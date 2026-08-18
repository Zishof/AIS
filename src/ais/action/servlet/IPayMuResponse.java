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
import ais.database.model.ipaymu.IpaymuRequest;
import ais.database.model.ipaymu.IpaymuRequestDetail;
import ais.database.model.ipaymu.IpaymuResponse;

/**
 * Servlet implementation class CheckISBN
 */
public class IPayMuResponse extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public IPayMuResponse() {
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
	public static void prosesResponse(IpaymuResponse ipaymuResponse) {
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();
		IpaymuRequest ipaymuRequest = (IpaymuRequest) session.createCriteria(IpaymuRequest.class)
				.add(Restrictions.ilike("nama", ipaymuResponse.getNama(), MatchMode.EXACT)).setMaxResults(1)
				.uniqueResult();
		// System.out.println("ipaymuRequest==>" + ipaymuRequest);
		if (ipaymuRequest != null && ipaymuRequest.getIpaymuResponse() == null) {
			ipaymuRequest.setIpaymuResponse(ipaymuResponse);
			session.getTransaction().begin();
			session.update(ipaymuRequest);
			session.getTransaction().commit();
		}
		if (ipaymuRequest != null && ipaymuResponse.getStatus().trim().equalsIgnoreCase(IpaymuResponse.BERHASIL)) {

			Double nilaiBiayaHarusDiBayars = ipaymuRequest.getNilaiBiayaHarusDiBayars();

			Kegiatan kegiatan = null;

			Mahasiswa mhs = ipaymuRequest.getMahasiswa();
			BiodataCalonMahasiswa bio = ipaymuRequest.getBiodataCalonMahasiswa();

			Integer semester = ipaymuRequest.getSemester();
			JadwalPembayaran jadwalPembayaran = ipaymuRequest.getJadwalPembayaran();
			JenisKegiatan jenisKegiatan = ipaymuRequest.getJenisKegiatan();
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
					+ ipaymuRequest.getPengurangan());

			if (kegiatan == null || kegiatan.getId() == null) {
				kegiatan = new Kegiatan();
				kegiatan.setJenisKegiatan(jenisKegiatan);
				kegiatan.setJadwalPembayaran(jadwalPembayaran);
				kegiatan.setMahasiswa(mhs);
				kegiatan.setCalonMahasiswa(bio);
				kegiatan.setSemster(semester);
				if (mhs != null) {
					kegiatan.setStatusMahasiswa(
							ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mhs, kegiatan.getTahunAkademik(), kegiatan.getSemster())
									.getStatusMahasiswa());
				} else if (bio != null) {
					kegiatan.setStatusMahasiswa(ConstantValues.AKTIF);
				}
				kegiatan.setTahunAkademik(ipaymuRequest.getTahunAkademik());
				kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
				kegiatan.setValidated(1);
				kegiatan.setValidator("iPaymu");
				kegiatan.setPengurangan(ipaymuRequest.getPengurangan());
				kegiatan.setKeterangan(ipaymuRequest.getKeterangan());
				kegiatan.setAmount(nilaiBiayaHarusDiBayars);

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, kegiatan);
				session.getTransaction().commit();
			}

			if (ipaymuRequest.getAmount() > 0.1) {
				LogPembayaran logPembayaran = new LogPembayaran();
				logPembayaran.setKegiatan(kegiatan);
				logPembayaran.setNominal(ipaymuRequest.getAmount());
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, logPembayaran);
				session.getTransaction().commit();
			}

			List<IpaymuRequestDetail> ipaymuRequestDetails = session.createCriteria(IpaymuRequestDetail.class)
					.add(Restrictions.eq("ipaymuRequest", ipaymuRequest)).addOrder(Order.asc("ke"))
					.add(Restrictions.gt("ke", 0)).list();
			System.out.println("ipaymuRequestDetails==>" + ipaymuRequestDetails);
			if (!ipaymuRequestDetails.isEmpty()) {

				for (IpaymuRequestDetail ipaymuRequestDetail : ipaymuRequestDetails) {

					String ref = "ipaymuRequestDetail-" + ipaymuRequestDetail.getId();

					CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
							.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref)).setMaxResults(1)
							.uniqueResult();
					if (cicilanPembayaran == null) {
						cicilanPembayaran = new CicilanPembayaran(ipaymuRequestDetail.getDetailBiaya());

					}
					cicilanPembayaran.setRef(ref);
					cicilanPembayaran.setValidator("iPaymu");
					cicilanPembayaran.setKe(ipaymuRequestDetail.getKe());
					cicilanPembayaran.setKegiatan(kegiatan);
					cicilanPembayaran.setItemBiaya(ipaymuRequestDetail.getItemBiaya());
					cicilanPembayaran
							.setPengaturanPembayaranBulanan(ipaymuRequestDetail.getPengaturanPembayaranBulanan());
					cicilanPembayaran.setNilai(ipaymuRequestDetail.getNilai());
					cicilanPembayaran.setTanggal(ipaymuRequestDetail.getTanggal());
					cicilanPembayaran.setJenisPembayaran(ConstantValues.TUNAI);
					cicilanPembayaran.setDenda(ipaymuRequestDetail.getDenda());
					cicilanPembayaran.setNilaiAsli(ipaymuRequestDetail.getNilaiAsli());
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

				kegiatan.setValidator("iPaymu");

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, kegiatan);
				session.getTransaction().commit();
			}

			pembayaranUtil.updateTunggakan(kegiatan, session);

			if (mhs != null) {
				CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);
			} else if (bio != null) {
				CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);
			}
		}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/IPayMuResponse.java:216");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/IPayMuResponse.java:217");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/IPayMuResponse.java:218");}
			}
		}
	}

	public static void prosesTransaksi(Map<String, String> param) {
		IpaymuResponse ipaymuResponse = new IpaymuResponse();
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();
		ipaymuResponse.setNama(param.get("sid"));
		ipaymuResponse.setStatus(param.get("status"));
		ipaymuResponse.setMerchant(param.get("merchant"));
		ipaymuResponse.setTrxId(param.get("trx_id"));
		ipaymuResponse.setProduct(param.get("product"));
		ipaymuResponse.setBuyer(param.get("buyer"));
		ipaymuResponse.setNoRekeningDeposit(param.get("no_rekening_deposit"));
		ipaymuResponse.setComments(param.get("comments"));
		session.getTransaction().begin();
		session.save(ipaymuResponse);
		session.getTransaction().commit();
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/IPayMuResponse.java:241");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/IPayMuResponse.java:242");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/IPayMuResponse.java:243");}
			}
		}

		prosesResponse(ipaymuResponse);
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
		System.out.println("==> IPayMuResponse data => " + data);

		String[] splt = StringUtils.split(data, "&");
		Map<String, String> param = new HashMap<String, String>();
		for (String s : splt) {
			try {
				String[] v = StringUtils.split(s, "=");
				param.put(v[0], v[1]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/IPayMuResponse.java:269");

			}
		}

		System.out.println("==> param => " + param);

		IPayMuResponse.prosesTransaksi(param);

		response.setHeader("Content-Type", "text/plain");

		PrintWriter writer = response.getWriter();
		writer.write("00");
	}

}
