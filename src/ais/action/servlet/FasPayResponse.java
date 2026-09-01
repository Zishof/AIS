package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.json.XML;

import ais.action.report.CommonReportHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.AeSimpleSHA1;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.FaspayCommon;
import ais.common.MD5;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
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
import ais.database.model.faspay.FaspayRequest;
import ais.database.model.faspay.FaspayRequestDetail;
import ais.database.model.faspay.FaspayRequestDetailBiaya;
import ais.database.model.faspay.FaspayResponse;

/**
 * Servlet callback/handler untuk integrasi payment gateway <b>Faspay</b>: menerima notifikasi
 * status transaksi dari server Faspay (parameter {@code data}/{@code querystring} berisi XML
 * status transaksi), mencocokkannya dengan {@link FaspayRequest} lokal berdasarkan {@code trx_id}/
 * {@code bill_no}, lalu menindaklanjuti pembayaran yang berhasil (pelunasan tagihan terkait via
 * {@link FaspayCommon}, pencatatan {@link LogPembayaran}/{@link LogHostToHost}). Servlet ini juga
 * menyediakan endpoint untuk memulai pembayaran Faspay langsung dari parameter request web service
 * (lihat {@link FaspayCommon#populateFaspayRequestDetail(HttpServletRequest, Mahasiswa, String, Integer)}).
 *
 * <p>
 * <b>Riwayat keamanan (DIPERBAIKI 2026-09-01)</b> — salah satu jalur pada servlet ini sebelumnya
 * mengambil kredensial merchant Faspay lewat {@link Common#getKonfigurasi(String, String)} dengan
 * nilai default RAHASIA tertanam langsung di kode sumber ({@code faspay_user_id} default
 * {@code "bot31503"}, {@code faspay_password} default {@code "W4TYRmO0"} — identik dengan default
 * lama pada {@link FaspayCommon} dan {@code ais.common.FaspayKeranjangPembayaran}, sudah diperbaiki
 * terpisah). Default itu sudah dihapus (kini string kosong). Baris {@code System.out.println} yang
 * sebelumnya mencetak signature transaksi (berpotensi membocorkan bahan autentikasi lewat log
 * server) juga sudah dihapus. Kredensial lama yang sebelumnya tertanam sudah lama berada di
 * riwayat SVN dan WAJIB dianggap bocor — perlu dirotasi di sisi Faspay bila masih aktif di
 * produksi.
 * </p>
 */
public class FasPayResponse extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public FasPayResponse() {
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

	public static Kegiatan createKegiatan(FaspayRequest faspayRequest, Session session) {
		Kegiatan kegiatan = null;
		Double nilaiBiayaHarusDiBayars = faspayRequest.getNilaiBiayaHarusDiBayars();

		Mahasiswa mhs = faspayRequest.getMahasiswa();
		BiodataCalonMahasiswa bio = faspayRequest.getBiodataCalonMahasiswa();

		Integer semester = faspayRequest.getSemester();
		JadwalPembayaran jadwalPembayaran = faspayRequest.getJadwalPembayaran();
		JenisKegiatan jenisKegiatan = faspayRequest.getJenisKegiatan();
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
				+ faspayRequest.getPengurangan() + ", hapusCicilanSebelumnya ==> "
				+ faspayRequest.getHapusCicilanSebelumnya());

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
			kegiatan.setTahunAkademik(faspayRequest.getTahunAkademik());
			kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
			kegiatan.setValidated(1);
			kegiatan.setValidator(faspayRequest.getPayment_channel_name());
			kegiatan.setPengurangan(faspayRequest.getPengurangan());
			kegiatan.setKeterangan(faspayRequest.getKeterangan());
			kegiatan.setAmount(nilaiBiayaHarusDiBayars);

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, kegiatan);
			session.getTransaction().commit();
		} else {
			session.refresh(kegiatan); 
		}
		return kegiatan;
	}


	private static boolean isRequestSudahDiproses(FaspayRequest faspayRequest) {
		if (faspayRequest == null) {
			return false;
		}
		try {
			Collection temporarys = faspayRequest.getKegiatanTemporarys();
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

	@SuppressWarnings("unchecked")
	public static void prosesResponse(FaspayRequest faspayRequest, FaspayResponse faspayResponse, String billNo) {
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();
		if (faspayRequest == null || faspayRequest.getId() == null) {
			faspayRequest = (FaspayRequest) session.createCriteria(FaspayRequest.class)
					.add(Restrictions.eq("trxId", faspayResponse.getTrxId())).add(Restrictions.eq("billNo", billNo))
					.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
		}

		if (faspayRequest != null && faspayResponse.getKodeStatus().toString().trim().equalsIgnoreCase("2")) {

			if (isRequestSudahDiproses(faspayRequest)) {
				System.out.println("Callback faspayRequest sudah pernah diproses, proses pembayaran dilewati: " + faspayRequest);
				return;
			}

			if (faspayResponse.getKodeStatus() != null && (faspayRequest.getKodeStatus() == null
					|| !faspayResponse.getKodeStatus().equalsIgnoreCase(faspayRequest.getKodeStatus()))) {
				faspayRequest.setFaspayResponse(faspayResponse);
				faspayRequest.setStatus("Payment Sukses");
				faspayRequest.setKodeStatus(faspayResponse.getKodeStatus());
				session.getTransaction().begin();
				Common.refreshUpdate(session, faspayRequest);
				session.getTransaction().commit();
			}

			String kodeAkun = Common.getKonfigurasi("kode_akun_faspay", "").getNilai();
			JenisPembayaran jenisPembayaran = JenisPembayaran.ambilJenisPembayaranBerdasarkanKodeAkun(session,
					kodeAkun);

			if (!faspayRequest.getKegiatanTemporarys().isEmpty()) {
				Kegiatan kegiatan = null;
				for (KegiatanTemporary temporary : faspayRequest.getKegiatanTemporarys()) {
					Session sessionLocalKeg = null;
					try {
						sessionLocalKeg = HibernateUtil.getSessionFactory().openSession();
						KegiatanTemporary kegiatanTemporary = (KegiatanTemporary) sessionLocalKeg
								.createCriteria(KegiatanTemporary.class).add(Restrictions.idEq(temporary.getId()))
								.uniqueResult();
						if (kegiatanTemporary != null) {

							Mahasiswa mahasiswa = kegiatanTemporary.getMahasiswa();
							Integer smt = kegiatanTemporary.getSemster();
							JenisKegiatan jenisKegiatan = kegiatanTemporary.getJenisKegiatan();
							kegiatan = mahasiswa.ambilKegiatans(smt, jenisKegiatan);

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
							kegiatan.setValidator(faspayRequest.getPayment_channel_name());
							kegiatan.setKeterangan(kegiatanTemporary.getKeterangan());

							sessionLocalKeg.getTransaction().begin();
							Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatan);
							sessionLocalKeg.getTransaction().commit();

							List<CicilanPembayaran> cicilanPembayarans = sessionLocalKeg
									.createCriteria(CicilanPembayaran.class)
									.add(Restrictions.eq("kegiatanTemporary.id", temporary.getId())).list();
							for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
								cicilanPembayaran.setKegiatan(kegiatan);
								cicilanPembayaran.setValidator(faspayRequest.getPayment_channel_name());
								cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
								sessionLocalKeg.getTransaction().begin();
								Common.refreshSaveOrUpdate(sessionLocalKeg, cicilanPembayaran);
								sessionLocalKeg.getTransaction().commit();
							}

							try {
								if (kegiatanTemporary.getKegiatan() == null || (kegiatanTemporary.getKegiatan() != null
										&& !kegiatanTemporary.getKegiatan().getId().equals(kegiatan.getId()))) {
									sessionLocalKeg.getTransaction().begin();
									kegiatanTemporary.setKegiatan(kegiatan);
									Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatanTemporary);
									sessionLocalKeg.getTransaction().commit();
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:267");
								// TODO: handle exception
							}

							Number jumlah = (Number) sessionLocalKeg.createCriteria(CicilanPembayaran.class)
									.add(Restrictions.isNotNull("itemBiaya")).add(Restrictions.eq("kegiatan", kegiatan))
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
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					} finally {
						if (sessionLocalKeg != null) {
							try { sessionLocalKeg.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:311");}
							try { sessionLocalKeg.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:312");}
							try { sessionLocalKeg.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:313");}
						}
					}
				}

				if (faspayRequest.getAmount() > 0.1) {
					LogPembayaran logPembayaran = (LogPembayaran) session.createCriteria(LogPembayaran.class)
							.add(Restrictions.eq("faspayRequest", faspayRequest)).setMaxResults(1).uniqueResult();
					if (logPembayaran == null) {
						logPembayaran = new LogPembayaran();
					}
					double biayaAdministrasi = 0.0;
					try {
						biayaAdministrasi = Double
								.parseDouble(Common.getKonfigurasi("faspay_biaya_administrasi", "0.0").getNilai());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:328");

					}
					logPembayaran.setBiayaAdministrasi(biayaAdministrasi);
					logPembayaran.setFaspayRequest(faspayRequest);
					logPembayaran.setNominal(faspayRequest.getAmount());
					logPembayaran.setKeterangan(faspayRequest.getResponse());
					logPembayaran.setKegiatan(kegiatan);
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, logPembayaran);
					session.getTransaction().commit();
				}

			} else {

				Kegiatan kegiatan = FasPayResponse.createKegiatan(faspayRequest, session);
				Double nilaiBiayaHarusDiBayars = faspayRequest.getNilaiBiayaHarusDiBayars();

				List<FaspayRequestDetail> faspayRequestDetails = session.createCriteria(FaspayRequestDetail.class)
						.add(Restrictions.isNull("idCicilan")).add(Restrictions.eq("faspayRequest", faspayRequest))
						.add(Restrictions.gt("ke", 0)).addOrder(Order.asc("ke")).add(Restrictions.gt("ke", 0)).list();
				System.out.println("faspayRequestDetails==>" + faspayRequestDetails);
				if (!faspayRequestDetails.isEmpty()) {

					for (FaspayRequestDetail faspayRequestDetail : faspayRequestDetails) {

						String ref = "faspayRequestDetail-" + faspayRequestDetail.getId();

						System.out.println("ref==>" + ref);

						CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
								.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref))
								.setMaxResults(1).uniqueResult();
						if (cicilanPembayaran == null) {
							cicilanPembayaran = new CicilanPembayaran(faspayRequestDetail.getDetailBiaya());
						}

						cicilanPembayaran.setRef(ref);
						cicilanPembayaran.setValidator(faspayRequest.getPayment_channel_name());
						cicilanPembayaran.setKe(faspayRequestDetail.getKe());
						cicilanPembayaran.setKegiatan(kegiatan);
						cicilanPembayaran.setItemBiaya(faspayRequestDetail.getItemBiaya());
						cicilanPembayaran
								.setPengaturanPembayaranBulanan(faspayRequestDetail.getPengaturanPembayaranBulanan());
						cicilanPembayaran.setNilai(faspayRequestDetail.getNilai());
						cicilanPembayaran.setTanggal(faspayRequestDetail.getTanggal());
						cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
						cicilanPembayaran.setDenda(faspayRequestDetail.getDenda());
						cicilanPembayaran.setNilaiAsli(faspayRequestDetail.getNilaiAsli());
						session.getTransaction().begin();
						if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
						session.getTransaction().commit();

						faspayRequestDetail.setCicilanref(cicilanPembayaran.getId());
						session.getTransaction().begin();
						session.update(faspayRequestDetail);
						session.getTransaction().commit();

					}

					Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
					Double jumlah = d[0];
					Double denda = d[1];
					kegiatan.setDenda(denda.doubleValue());
					kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));
					kegiatan.setAmount(jumlah.doubleValue());

					kegiatan.setValidator(faspayRequest.getPayment_channel_name());

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, kegiatan);
					session.getTransaction().commit();
				}

				if (faspayRequest.getAmount() > 0.1) {
					LogPembayaran logPembayaran = (LogPembayaran) session.createCriteria(LogPembayaran.class)
							.add(Restrictions.eq("faspayRequest", faspayRequest)).setMaxResults(1).uniqueResult();
					if (logPembayaran == null) {
						logPembayaran = new LogPembayaran();
					}
					double biayaAdministrasi = 0.0;
					try {
						biayaAdministrasi = Double
								.parseDouble(Common.getKonfigurasi("faspay_biaya_administrasi", "0.0").getNilai());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:412");

					}
					logPembayaran.setBiayaAdministrasi(biayaAdministrasi);
					logPembayaran.setFaspayRequest(faspayRequest);
					logPembayaran.setKegiatan(kegiatan);
					logPembayaran.setNominal(faspayRequest.getAmount());
					logPembayaran.setKeterangan(faspayRequest.getResponse());
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, logPembayaran);
					session.getTransaction().commit();
				}

				pembayaranUtil.updateTunggakan(kegiatan, session);

				Mahasiswa mhs = faspayRequest.getMahasiswa();
				BiodataCalonMahasiswa bio = faspayRequest.getBiodataCalonMahasiswa();
				if (mhs != null) {
					CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);
				} else if (bio != null) {
					CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);
				}
			}
		}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:438");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:439");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:440");}
			}
		}
	}

	public static FaspayResponse prosesTransaksi(JSONObject faspay) throws Exception {
		String trx_id = faspay.isNull("trx_id") ? "" : ais.common.CommonJSONUtil.ambilLong(faspay,"trx_id") + "";
		String bill_no = faspay.isNull("bill_no") ? "" : faspay.getString("bill_no") + "";
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();

		FaspayResponse faspayResponse = (FaspayResponse) session.createCriteria(FaspayResponse.class)
				.add(Restrictions.eq("trxId", trx_id))
				.add(Restrictions.ilike("keterangan", bill_no, MatchMode.ANYWHERE)).setMaxResults(1)
				.addOrder(Order.desc("id")).uniqueResult();
		if (faspayResponse == null) {
			faspayResponse = new FaspayResponse();
		}
		faspayResponse.setKeterangan(faspay.toString());
		faspayResponse.setNama(trx_id);
		faspayResponse.setStatus(FaspayResponse.SEDANG_DIPROSES);
		faspayResponse.setMerchant(faspay.isNull("merchant_id") ? "" : faspay.get("merchant_id").toString());
		faspayResponse.setTrxId(trx_id);
		session.getTransaction().begin();
		session.save(faspayResponse);
		session.getTransaction().commit();
		return faspayResponse;
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:470");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:471");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:472");}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void processRequestFaspay(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String payment_channel = request.getParameter("payment_channel") == null ? "402"
				: request.getParameter("payment_channel").trim();
		String payment_channel_name = request.getParameter("payment_channel_name") == null ? "Permata"
				: request.getParameter("payment_channel_name").trim();
		String tahunAkademik = request.getParameter("tahunAkademik");
		String keterangan = "Bayar via Android";
		Double pengurangan = 0.0;
		Integer semester = Integer.parseInt(request.getParameter("semester"));
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			JenisKegiatan jenisKegiatan = (JenisKegiatan) ConstantValues.simpleObject(
					session.createCriteria(JenisKegiatan.class)
							.add(Restrictions.idEq(Long.parseLong(request.getParameter("jenisKegiatan")))),
					JenisKegiatan.class);
			JadwalPembayaran jadwalPembayaran = PembayaranUtil.getInstance().getJadwalPembayaranTanpaDibatasiWaktu(
					jenisKegiatan, tahunAkademik, semester % 2 == 1, null, null, null);
			if (jadwalPembayaran == null) {
				response.setHeader("Content-Type", "text/plain");
				PrintWriter writer = response.getWriter();
				writer.write("Jadwal pembayaran tidak ada");
				return;
			}
			Mahasiswa mahasiswa = (Mahasiswa) ConstantValues
					.simpleObject(
							session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.idEq(Long.parseLong(request.getParameter("mahasiswa")))),
							Mahasiswa.class);
			if (mahasiswa != null && jenisKegiatan != null) {

				List<FaspayRequestDetail> faspayRequestDetails = FaspayCommon.populateFaspayRequestDetail(request,
						mahasiswa, payment_channel_name, semester);
				List<FaspayRequestDetailBiaya> faspayRequestDetailBiayas = new ArrayList<FaspayRequestDetailBiaya>();
				for (FaspayRequestDetail faspayRequestDetail : faspayRequestDetails) {
					FaspayRequestDetailBiaya faspayRequestDetailBiaya = new FaspayRequestDetailBiaya();
					faspayRequestDetailBiaya
							.setDetailBiaya(faspayRequestDetail.getPengaturanPembayaranBulanan().getDetailBiaya());
					faspayRequestDetailBiaya.setDetailBiaya(faspayRequestDetail.getDetailBiaya());
					faspayRequestDetailBiaya.setNilai(faspayRequestDetail.getNilai());
					faspayRequestDetailBiayas.add(faspayRequestDetailBiaya);
				}

				String merchant_id = Common.getKonfigurasi("faspay_merchant_id", "").getNilai().trim();
				String merchant = Common.getKonfigurasi("faspay_merchant_name", "eCampus").getNilai().trim();
				String UserID = Common.getKonfigurasi("faspay_user_id", "").getNilai().trim();
				String Password = Common.getKonfigurasi("faspay_password", "").getNilai().trim();

				String items = "";

				Kegiatan kegiatan = mahasiswa.ambilKegiatans(semester, jenisKegiatan);
				Map<Long, DetailBiaya> map = new java.util.HashMap<Long, DetailBiaya>();
				Collection<DetailBiaya> mydetailBiayas = pembayaranUtil.getDetailBiayaMahasiswa(mahasiswa, semester,
						jenisKegiatan, false);
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

				Double nilaiBiayaHarusDiBayars = 0.0;
				for (DetailBiaya detailBiaya : map.values()) {
					Double nilai = detailBiaya.hitungTotalKegiatan(kegiatan, session);
					nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
				}

				System.out
						.println("kegiatan => " + kegiatan + ", nilaiBiayaHarusDiBayars => " + nilaiBiayaHarusDiBayars);

				Double amn = 0.0;
				for (FaspayRequestDetail detail : faspayRequestDetails) {
					amn += detail.getNilai();
					if (detail.getPengaturanPembayaranBulanan() != null) {
						String item = "<item>\n<product>"
								+ detail.getPengaturanPembayaranBulanan().getDetailBiaya().getItemBiaya().getNama()
								+ " bulan " + detail.getPengaturanPembayaranBulanan().getNamaBulan()
								+ "</product>\n<qty>1</qty>\n<amount>" + detail.getNilai().intValue() + "00</amount>\n"
								+ "<payment_plan>01</payment_plan>\n<merchant_id></merchant_id>\n<tenor>00</tenor>\n"
								+ "</item>\n";
						items += item;
					} else {
						String item = "<item>\n<product>" + detail.getItemBiaya().getNama()
								+ "</product>\n<qty>1</qty>\n<amount>" + detail.getNilai().intValue() + "00</amount>\n"
								+ "<payment_plan>01</payment_plan>\n<merchant_id></merchant_id>\n<tenor>00</tenor>\n"
								+ "</item>\n";
						items += item;
					}

				}

				String bill_no = Common.getGeneratedBarCode();

				String signature = AeSimpleSHA1.SHA1(MD5.crypt(UserID + Password + bill_no));

				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR) + 12);

				String cust_name = "";
				String cust_no = "";
				String msisdn = "";
				String email = "";
				String address = "";
				String city = "";
				String region = "";
				String poscode = "";

				cust_name = mahasiswa.getNama();
				cust_no = mahasiswa.getNim();
				email = mahasiswa.getEmail().split(",")[0];
				address = mahasiswa.getAlamat();
				BiodataMahasiswa biodataMahasiswa = (BiodataMahasiswa) session.createCriteria(BiodataMahasiswa.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();
				if (biodataMahasiswa != null) {
					city = biodataMahasiswa.getKota() == null ? "" : biodataMahasiswa.getKota().getNama();
					region = biodataMahasiswa.getPropinsi() == null ? "" : biodataMahasiswa.getPropinsi().getNama();
					poscode = biodataMahasiswa.getKodepos();
					msisdn = biodataMahasiswa.getHp();
					if (!Common.isNumber(msisdn.replaceAll("\\+", "").trim())) {
						msisdn = "0810000000";
					}
				}

				String postData = "<faspay>\n<request>Post Data Transaksi</request>\n<merchant_id>" + merchant_id
						+ "</merchant_id>\n<merchant>" + merchant + "</merchant>\n<bill_no>" + bill_no + "</bill_no>\n"
						+ "<bill_reff>" + cust_no + "</bill_reff>\n<bill_date>"
						+ dateFormat.format(ais.ui.util.WaktuUtil.getDate()) + "</bill_date>\n<bill_expired>"
						+ dateFormat.format(calendar.getTime()) + "</bill_expired>\n" + "<bill_desc>"
						+ (jenisKegiatan == null ? "Pembayaran Mahasiswa" : jenisKegiatan.getNamaKegiatan())
						+ "</bill_desc>\n<bill_currency>IDR</bill_currency>\n<bill_gross>" + amn.intValue()
						+ "00</bill_gross>\n<bill_tax>0</bill_tax>\n<bill_miscfee>0</bill_miscfee>\n<bill_total>"
						+ amn.intValue() + "00</bill_total>\n<cust_no>" + cust_no + "</cust_no>\n<cust_name>"
						+ cust_name + "</cust_name>\n<payment_channel>" + payment_channel
						+ "</payment_channel>\n<pay_type>1</pay_type>\n<bank_userid>" + cust_no
						+ "</bank_userid>\n<msisdn>" + msisdn + "</msisdn>\n<email>" + email
						+ "</email>\n<terminal>10</terminal>\n<billing_address>" + address + "</billing_address>\n"
						+ "<billing_address_city>" + city + "</billing_address_city>\n<billing_address_region>" + region
						+ "</billing_address_region>\n<billing_address_state>Indonesia</billing_address_state>\n"
						+ "<billing_address_poscode>" + poscode + "</billing_address_poscode>\n"
						+ "<billing_address_country_code>ID</billing_address_country_code>\n<receiver_name_for_shipping>"
						+ cust_name + "</receiver_name_for_shipping>\n<shipping_address>" + address
						+ "</shipping_address>\n<shipping_address_city>" + city + "</shipping_address_city>\n"
						+ "<shipping_address_region>" + region + "</shipping_address_region>\n"
						+ "<shipping_address_state>Indonesia</shipping_address_state>\n<shipping_address_poscode>"
						+ poscode + "</shipping_address_poscode>\n" + items
						+ "\n<reserve1></reserve1>\n<reserve2></reserve2>\n<signature>" + signature
						+ "</signature>\n</faspay>";

				postData = org.apache.commons.lang3.StringUtils.replace(postData, "\\&", "dan");

				final FaspayRequest faspayRequest = FaspayCommon.sendRequest(postData, mahasiswa, null, jenisKegiatan,
						jadwalPembayaran, semester, tahunAkademik, keterangan, pengurangan, nilaiBiayaHarusDiBayars,
						amn, merchant_id, signature, bill_no, payment_channel_name, faspayRequestDetails,
						faspayRequestDetailBiayas, false);
				if (faspayRequest != null && faspayRequest.getUrl() != null
						&& !faspayRequest.getUrl().trim().isEmpty()) {

					// String url = faspayRequest.getUrl().trim() + "#topbar";
					// response.sendRedirect(url);
					// request.getRequestDispatcher(url).forward(request,
					// response);
					response.setHeader("Content-Type", "text/json");
					PrintWriter writer = response.getWriter();

					JSONObject jsonObject = new JSONObject();
					jsonObject.put("trxId", faspayRequest.getTrxId());
					jsonObject.put("nominal", faspayRequest.getAmount());
					jsonObject.put("url", faspayRequest.getUrl().trim() + "#topbar");
					// String body =
					// "<html>\n<head>\n<title>Pembayaran</title>\n</head>\n"
					// + "<frameset rows=\"*\" border=\"0\">\n<frame
					// name=\"main\" src=\"" + url + "\" />\n"
					// + "</frameset>\n</html>";
					writer.write(jsonObject.toString());
				} else {
					response.setHeader("Content-Type", "text/plain");
					PrintWriter writer = response.getWriter();
					writer.write("Terjadi kesalahan sistem.");
				}
			} else {
				response.setHeader("Content-Type", "text/plain");
				PrintWriter writer = response.getWriter();
				writer.write("Data tidak valid");
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			response.setHeader("Content-Type", "text/plain");
			PrintWriter writer = response.getWriter();
			writer.write("Terjadi kesalahan sistem " + e.getMessage());
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:676");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:677");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:678");}
			}
		}
	}

	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), "Faspay");
		// Read from request
		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String data = buffer.toString();
		String querystring = request.getQueryString();
		System.out.println("==> FasPayResponse data => " + data);
		System.out.println("==> FasPayResponse querystring => " + querystring);

		FaspayRequest faspayRequest = null;
		String trx_id = null;
		String bill_no = null;
		JSONObject faspay = null;
		String xml = null;
		// Jejak stack trace bila pemrosesan callback/pembayaran error; disimpan ke kolom log H2H.
		String h2hStackTrace = null;

		try {
		if (querystring != null && !querystring.trim().isEmpty()) {

			if (request.getParameter("mahasiswa") != null) {
				processRequestFaspay(request, response);
			} else {
				try {
					trx_id = request.getParameter("trx_id");
					bill_no = request.getParameter("bill_no");
					Session sessionInner = null;
					FaspayResponse faspayResponse = null;
					try {
						sessionInner = HibernateUtil.getSessionFactory().openSession();
						faspayResponse = (FaspayResponse) sessionInner.createCriteria(FaspayResponse.class)
								.add(Restrictions.eq("trxId", trx_id)).add(Restrictions.eq("billNo", bill_no))
								.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
						System.out
								.println("==> FasPayResponse trx_id => " + trx_id + ", faspayResponse = " + faspayResponse);

						if (faspayResponse == null && trx_id != null) {
							faspayResponse = new FaspayResponse();
							faspayResponse.setKeterangan(querystring);
							faspayResponse.setNama(trx_id);
							faspayResponse.setStatus(FaspayResponse.SEDANG_DIPROSES);
							faspayResponse.setMerchant(request.getParameter("merchant_id"));
							faspayResponse.setTrxId(trx_id);
							faspayResponse.setCallback(querystring);
							faspayResponse.setKodeStatus(request.getParameter("status"));

							sessionInner.getTransaction().begin();
							sessionInner.save(faspayResponse);
							sessionInner.getTransaction().commit();
						} else if (faspayResponse != null) {
							faspayResponse.setCallback(querystring);
							faspayResponse.setKodeStatus(request.getParameter("status"));
							sessionInner.getTransaction().begin();
							sessionInner.update(faspayResponse);
							sessionInner.getTransaction().commit();
						}
					} finally {
						if (sessionInner != null) {
							try { sessionInner.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:747");}
							try { sessionInner.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:748");}
							try { sessionInner.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:749");}
						}
					}
					if (faspayResponse != null) {
						prosesResponse(null, faspayResponse, bill_no);
						if (faspayResponse.getId() == null) {
							System.out.println("==> bikin baru FasPayResponse trx_id => " + trx_id + ", bill_no ==> "
									+ bill_no + ", faspayResponse = " + faspayResponse);
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				response.setHeader("Content-Type", "text/html");
				PrintWriter writer = response.getWriter();

				String body = "<html>\n<head>\n<title>Pembayaran</title>\n</head><body><h3>Lakukanlah pembayaran segera dengan memasukkan kode pembayaran "
						+ request.getParameter("trx_id")
						+ "</h3><br>Klik back atau tutup untuk kembali ke menu sebelumnya.</body></html>";

				writer.write(body);

			}

		} else {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			xml = "<faspay><response>Payment Notification</response><trx_id></trx_id>"
					+ "<merchant_id></merchant_id><bill_no></bill_no><response_code>01</response_code>"
					+ "<response_desc>Gagal</response_desc><response_date>"
					+ dateFormat.format(ais.ui.util.WaktuUtil.getDate()) + "</response_date></faspay>";

			if (bankHost != null) {

				try {

					JSONObject jSONObject = XML.toJSONObject(data);
					faspay = jSONObject.getJSONObject("faspay");

					trx_id = faspay.isNull("trx_id") ? "" : ais.common.CommonJSONUtil.ambilLong(faspay,"trx_id") + "";
					bill_no = faspay.isNull("bill_no") ? "" : faspay.getString("bill_no") + "";

					Session session = null;
					FaspayResponse faspayResponseForProsesResponse = null;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						faspayRequest = (FaspayRequest) session.createCriteria(FaspayRequest.class)
								.add(Restrictions.eq("trxId", trx_id)).add(Restrictions.eq("billNo", bill_no))
								.setMaxResults(1).uniqueResult();

						System.out.println(
								"trx_id = " + trx_id + ", bill_no = " + bill_no + ", faspayRequest = " + faspayRequest);
						if (faspayRequest != null) {
							FaspayResponse faspayResponse = prosesTransaksi(faspay);
							if (!faspay.isNull("payment_status_code") && !faspay.isNull("payment_status_desc")) {
								Object payment_status_code = faspay.get("payment_status_code");
								Object payment_status_desc = faspay.get("payment_status_desc");
								if (payment_status_code != null
										&& payment_status_code.toString().trim().equalsIgnoreCase("2")) {

									session.refresh(faspayResponse);
									faspayResponse.setKeterangan(faspay.toString());
									faspayResponse.setNama(faspayRequest.getTrxId());
									faspayResponse.setStatus(payment_status_desc == null ? "Belum diproses"
											: payment_status_desc.toString());
									faspayResponse.setMerchant(
											faspay.isNull("merchant_id") ? "" : faspay.get("merchant_id").toString());
									faspayResponse.setTrxId(faspayRequest.getTrxId());
									faspayResponse.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
									faspayResponse.setKodeStatus(
											payment_status_code == null ? "0" : payment_status_code.toString());
									session.getTransaction().begin();
									Common.refreshSaveOrUpdate(session, faspayResponse);
									session.getTransaction().commit();

									session.refresh(faspayRequest);
									faspayRequest
											.setStatus(payment_status_desc == null ? null : payment_status_desc.toString());
									faspayRequest.setKodeStatus(
											payment_status_code == null ? null : payment_status_code.toString());
									session.getTransaction().begin();
									Common.refreshSaveOrUpdate(session, faspayRequest);
									session.getTransaction().commit();

									faspayResponseForProsesResponse = faspayResponse;
								}
							}

							xml = "<faspay><response>Payment Notification</response><trx_id>" + trx_id + "</trx_id>"
									+ "<merchant_id>" + faspay.get("merchant_id") + "</merchant_id><bill_no>"
									+ faspay.get("bill_no") + "</bill_no><response_code>00</response_code>"
									+ "<response_desc>Sukses</response_desc><response_date>"
									+ dateFormat.format(ais.ui.util.WaktuUtil.getDate()) + "</response_date></faspay>";
						} else {
							xml = "<faspay><response>Payment Notification</response><trx_id></trx_id>"
									+ "<merchant_id></merchant_id><bill_no></bill_no><response_code>01</response_code>"
									+ "<response_desc>Gagal</response_desc><response_date>"
									+ dateFormat.format(ais.ui.util.WaktuUtil.getDate()) + "</response_date></faspay>";
						}
					} finally {
						if (session != null) {
							try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:850");}
							try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:851");}
							try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FasPayResponse.java:852");}
						}
					}
					if (faspayResponseForProsesResponse != null) {
						FasPayResponse.prosesResponse(null, faspayResponseForProsesResponse, bill_no);
					}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			response.setHeader("Content-Type", "text/xml");
			PrintWriter writer = response.getWriter();
			writer.write(xml);
		}

		System.out.println("==> trx_id => " + trx_id + " ==> bill_no => " + bill_no);

		} catch (Exception e) {
			h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
			throw e;
		} finally {
			// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception
			// (helper: session terdedikasi + commit + retry, tak pernah gagal).
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
				logHostToHost.setNim(faspayRequest == null ? ""
						: (faspayRequest.getMahasiswa() != null ? faspayRequest.getMahasiswa().getNim()
								: faspayRequest.getBiodataCalonMahasiswa() != null
										? faspayRequest.getBiodataCalonMahasiswa().getNoRegistrasi()
										: ""));
				logHostToHost.setKode(trx_id);
				logHostToHost.setNama(faspayRequest == null ? ""
						: (faspayRequest.getMahasiswa() != null ? faspayRequest.getMahasiswa().getNama()
								: faspayRequest.getBiodataCalonMahasiswa() != null
										? faspayRequest.getBiodataCalonMahasiswa().getNama()
										: ""));
				logHostToHost.setTanggal(ais.ui.util.WaktuUtil.getDate());
				logHostToHost.setResponseDescription(xml);
				logHostToHost.setNominal(
						faspayRequest == null ? 0.0 : (faspayRequest.getAmount() + faspayRequest.getBiayaAdministrasi()));
				logHostToHost.setItem(faspay == null ? "" : faspay.toString());
				logHostToHost.setStackTrace(h2hStackTrace);
				ais.action.ws.util.PembayaranGatewayHelper.simpanLogHostToHost(logHostToHost);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

}
