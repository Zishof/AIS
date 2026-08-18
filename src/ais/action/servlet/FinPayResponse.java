package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.action.report.CommonReportHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.LogPembayaran;
import ais.database.model.Mahasiswa;
import ais.database.model.finpay.FinpayRequest;
import ais.database.model.finpay.FinpayRequestDetail;
import ais.database.model.finpay.FinpayResponse;

/**
 * Servlet implementation class CheckISBN
 */
public class FinPayResponse extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public FinPayResponse() {
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

	@SuppressWarnings("unchecked")
	public static void prosesResponse(FinpayResponse finpayResponse) {
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();
		FinpayRequest finpayRequest = (FinpayRequest) session.createCriteria(FinpayRequest.class)
				.add(Restrictions.ilike("paymentCode", finpayResponse.getPaymentCode(), MatchMode.EXACT))
				.add(Restrictions.ilike("invoice", finpayResponse.getInvoice(), MatchMode.EXACT)).setMaxResults(1)
				.uniqueResult();
		System.out.println("finpayRequest==>" + finpayRequest);
		if (finpayRequest != null) {
			finpayRequest.setFinpayResponse(finpayResponse);
			session.getTransaction().begin();
			session.update(finpayRequest);
			session.getTransaction().commit();

			Double nilaiBiayaHarusDiBayars = finpayRequest.getNilaiBiayaHarusDiBayars();

			Kegiatan kegiatan = createKegiatan(finpayRequest, finpayResponse, session);

			if (finpayRequest.getAmount() > 0.1) {
				LogPembayaran logPembayaran = new LogPembayaran();
				logPembayaran.setKegiatan(kegiatan);
				logPembayaran.setNominal(finpayRequest.getAmount());
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, logPembayaran);
				session.getTransaction().commit();
			}

			List<FinpayRequestDetail> finpayRequestDetails = session.createCriteria(FinpayRequestDetail.class)
					.add(Restrictions.eq("finpayRequest", finpayRequest)).addOrder(Order.asc("ke"))
					.add(Restrictions.gt("ke", 0)).list();
			System.out.println("finpayRequestDetails==>" + finpayRequestDetails);
			if (!finpayRequestDetails.isEmpty()) {

				for (FinpayRequestDetail finpayRequestDetail : finpayRequestDetails) {

					String ref = "finpayRequestDetail-" + finpayRequestDetail.getId();

					CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
							.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref)).setMaxResults(1)
							.uniqueResult();
					if (cicilanPembayaran == null) {
						cicilanPembayaran = new CicilanPembayaran(finpayRequestDetail.getDetailBiaya());

					}

					cicilanPembayaran.setRef(ref);
					cicilanPembayaran.setValidator(finpayResponse.getPaymentSource());
					cicilanPembayaran.setKe(finpayRequestDetail.getKe());
					cicilanPembayaran.setKegiatan(kegiatan);
					cicilanPembayaran.setItemBiaya(finpayRequestDetail.getItemBiaya());
					cicilanPembayaran
							.setPengaturanPembayaranBulanan(finpayRequestDetail.getPengaturanPembayaranBulanan());
					cicilanPembayaran.setNilai(finpayRequestDetail.getNilai());
					cicilanPembayaran.setTanggal(finpayRequestDetail.getTanggal());
					cicilanPembayaran.setJenisPembayaran(ConstantValues.TUNAI);
					cicilanPembayaran.setDenda(finpayRequestDetail.getDenda());
					cicilanPembayaran.setNilaiAsli(finpayRequestDetail.getNilaiAsli());
					session.getTransaction().begin();
					if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
					session.getTransaction().commit();
				}

				Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
				Double jumlah = d[0];
				Double denda = d[1];
				kegiatan.setDenda(denda.doubleValue());
				kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));
				kegiatan.setAmount(jumlah.doubleValue());

				kegiatan.setValidator(finpayResponse.getPaymentSource());

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, kegiatan);
				session.getTransaction().commit();
			}

			pembayaranUtil.updateTunggakan(kegiatan, session);

			Mahasiswa mhs = finpayRequest.getMahasiswa();
			BiodataCalonMahasiswa bio = finpayRequest.getBiodataCalonMahasiswa();
			if (mhs != null) {
				CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);
			} else if (bio != null) {
				CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);
			}
		}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FinPayResponse.java:170");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FinPayResponse.java:171");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FinPayResponse.java:172");}
			}
		}
	}

	public static void prosesTransaksi(Map<String, String> param) {
		FinpayResponse finpayResponse = new FinpayResponse();
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();
		finpayResponse.setNama(param.get("mer_signature"));
		finpayResponse.setTipe(param.get("trax_type"));
		finpayResponse.setMerchant(param.get("merchant_id"));
		finpayResponse.setInvoice(param.get("invoice"));
		finpayResponse.setPaymentCode(param.get("payment_code"));
		finpayResponse.setResultCode(param.get("result_code"));
		finpayResponse.setResultDesc(param.get("result_desc"));
		finpayResponse.setLogNo(param.get("log_no"));
		finpayResponse.setPaymentSource(param.get("payment_source"));
		session.getTransaction().begin();
		session.save(finpayResponse);
		session.getTransaction().commit();
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FinPayResponse.java:196");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FinPayResponse.java:197");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FinPayResponse.java:198");}
			}
		}

		prosesResponse(finpayResponse);
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
		System.out.println("==> FinPayResponse data => " + data);

		String[] splt = StringUtils.split(data, "&");
		Map<String, String> param = new HashMap<String, String>();
		for (String s : splt) {
			try {
				String[] v = StringUtils.split(s, "=");
				param.put(v[0], v[1]);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		for (Object p : request.getParameterMap().keySet()) {
			param.put(p.toString(), request.getParameter(p.toString()));
		}

		System.out.println("==> param => " + param);
		FinPayResponse.prosesTransaksi(param);
		response.setHeader("Content-Type", "text/plain");

		PrintWriter writer = response.getWriter();
		writer.write("00");
	}

	public static Kegiatan createKegiatan(FinpayRequest finpayRequest, FinpayResponse finpayResponse, Session session) {
		Mahasiswa mhs = finpayRequest.getMahasiswa();
		BiodataCalonMahasiswa bio = finpayRequest.getBiodataCalonMahasiswa();

		Integer semester = finpayRequest.getSemester();
		JadwalPembayaran jadwalPembayaran = finpayRequest.getJadwalPembayaran();
		JenisKegiatan jenisKegiatan = finpayRequest.getJenisKegiatan();

		Kegiatan kegiatan = null;
		if (mhs != null) {
			kegiatan = mhs.ambilKegiatans(semester, jenisKegiatan);
		} else if (bio != null) {
			kegiatan = bio.ambilKegiatans(semester, jenisKegiatan);
			if (kegiatan == null && semester <= 1) {
				kegiatan = bio.ambilKegiatans(jenisKegiatan);
			}
		}

		Double nilaiBiayaHarusDiBayars = finpayRequest.getNilaiBiayaHarusDiBayars();
		System.out.println("mhs==>" + mhs + ",bio==>" + bio + ", semester==>" + semester + ",jadwalPembayaran==>"
				+ jadwalPembayaran + ",jenisKegiatan==>" + jenisKegiatan + ",kegiatan==>" + kegiatan
				+ ",nilaiBiayaHarusDiBayars==>" + nilaiBiayaHarusDiBayars + ",pengurangan==>"
				+ finpayRequest.getPengurangan());

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
			kegiatan.setTahunAkademik(finpayRequest.getTahunAkademik());
			kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
			kegiatan.setValidated(1);
			kegiatan.setValidator(finpayResponse == null ? "Finpay" : finpayResponse.getPaymentSource());
			kegiatan.setPengurangan(finpayRequest.getPengurangan());
			kegiatan.setKeterangan(finpayRequest.getKeterangan());

			kegiatan.setAmount(nilaiBiayaHarusDiBayars);

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, kegiatan);
			session.getTransaction().commit();
		} else {
			session.refresh(kegiatan); 
		}

		return kegiatan;
	}

}
