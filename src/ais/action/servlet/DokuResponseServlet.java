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
import ais.database.model.doku.DokuRequest;
import ais.database.model.doku.DokuRequestDetail;
import ais.database.model.doku.DokuResponse;

/**
 * Servlet implementation class CheckISBN
 */
public class DokuResponseServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public DokuResponseServlet() {
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
	public static String prosesResponse(DokuResponse dokuResponse) {
		if (dokuResponse.getNama() == null || dokuResponse.getNama().trim().isEmpty()) {
			return "Stop";
		}
		String hasil = "Continue";
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();
		DokuRequest dokuRequest = (DokuRequest) session.createCriteria(DokuRequest.class)
				.add(Restrictions.ilike("nama", dokuResponse.getNama(), MatchMode.EXACT)).setMaxResults(1)
				.uniqueResult();
		System.out.println("dokuRequest==>" + dokuRequest);
		if (dokuRequest != null && dokuRequest.getDokuResponse() == null) {
			dokuRequest.setDokuResponse(dokuResponse);
			session.getTransaction().begin();
			session.update(dokuRequest);
			session.getTransaction().commit();
		}
		if (dokuRequest != null && dokuResponse.getStatus().trim().equalsIgnoreCase(DokuResponse.BERHASIL)) {

			Double nilaiBiayaHarusDiBayars = dokuRequest.getNilaiBiayaHarusDiBayars();

			Kegiatan kegiatan = null;

			Mahasiswa mhs = dokuRequest.getMahasiswa();
			BiodataCalonMahasiswa bio = dokuRequest.getBiodataCalonMahasiswa();

			Integer semester = dokuRequest.getSemester();
			JadwalPembayaran jadwalPembayaran = dokuRequest.getJadwalPembayaran();
			JenisKegiatan jenisKegiatan = dokuRequest.getJenisKegiatan();
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
					+ dokuRequest.getPengurangan());

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
				kegiatan.setTahunAkademik(dokuRequest.getTahunAkademik());
				kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
				kegiatan.setValidated(1);
				kegiatan.setValidator("Doku");
				kegiatan.setPengurangan(dokuRequest.getPengurangan());
				kegiatan.setKeterangan(dokuRequest.getKeterangan());
				kegiatan.setAmount(nilaiBiayaHarusDiBayars);

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, kegiatan);
				session.getTransaction().commit();
			}

			if (dokuRequest.getAmount() > 0.1) {
				LogPembayaran logPembayaran = new LogPembayaran();
				logPembayaran.setKegiatan(kegiatan);
				logPembayaran.setNominal(dokuRequest.getAmount());
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, logPembayaran);
				session.getTransaction().commit();
			}

			List<DokuRequestDetail> dokuRequestDetails = session.createCriteria(DokuRequestDetail.class)
					.add(Restrictions.eq("dokuRequest", dokuRequest)).addOrder(Order.asc("ke"))
					.add(Restrictions.gt("ke", 0)).list();
			System.out.println("dokuRequestDetails==>" + dokuRequestDetails);
			if (!dokuRequestDetails.isEmpty()) {

				for (DokuRequestDetail dokuRequestDetail : dokuRequestDetails) {

					String ref = "dokuRequestDetail-" + dokuRequestDetail.getId();

					CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
							.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref)).setMaxResults(1)
							.uniqueResult();
					if (cicilanPembayaran == null) {
						cicilanPembayaran = new CicilanPembayaran(dokuRequestDetail.getDetailBiaya());

					}

					cicilanPembayaran.setRef(ref);
					cicilanPembayaran.setCicilanSebelumnya(dokuRequestDetail.getIdCicilan());
					cicilanPembayaran.setValidator("Doku");
					cicilanPembayaran.setKe(dokuRequestDetail.getKe());
					cicilanPembayaran.setKegiatan(kegiatan);
					cicilanPembayaran.setItemBiaya(dokuRequestDetail.getItemBiaya());
					cicilanPembayaran
							.setPengaturanPembayaranBulanan(dokuRequestDetail.getPengaturanPembayaranBulanan());
					cicilanPembayaran.setNilai(dokuRequestDetail.getNilai());
					cicilanPembayaran.setTanggal(dokuRequestDetail.getTanggal());
					cicilanPembayaran.setJenisPembayaran(ConstantValues.TUNAI);

					cicilanPembayaran.setDenda(dokuRequestDetail.getDenda());
					cicilanPembayaran.setNilaiAsli(dokuRequestDetail.getNilaiAsli());

					session.getTransaction().begin();
					if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
					session.getTransaction().commit();

					System.out.println("cicilanPembayaran==>" + cicilanPembayaran);
				}

				Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
				Double jumlah = d[0];
				Double denda = d[1];
				kegiatan.setDenda(denda.doubleValue());
				kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));
				kegiatan.setAmount(jumlah.doubleValue());

				kegiatan.setValidator("Doku");

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
		} else {
			hasil = "Stop";
		}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuResponseServlet.java:227");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuResponseServlet.java:228");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuResponseServlet.java:229");}
			}
		}
		return hasil;
	}

	public static String prosesTransaksi(Map<String, String> param) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			DokuResponse dokuResponse = new DokuResponse();
			dokuResponse.setNama(param.get("TRANSIDMERCHANT"));
			dokuResponse.setStatus(param.get("RESULT"));
			session.getTransaction().begin();
			session.save(dokuResponse);
			session.getTransaction().commit();

			return prosesResponse(dokuResponse);
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuResponseServlet.java:250");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuResponseServlet.java:251");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuResponseServlet.java:252");}
			}
		}
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
		System.out.println("==> DokuResponseServlet data => " + data);

		String[] splt = StringUtils.split(data, "&");
		Map<String, String> param = new HashMap<String, String>();
		for (String s : splt) {
			try {
				String[] v = StringUtils.split(s, "=");
				param.put(v[0], v[1]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/DokuResponseServlet.java:276");

			}
		}

		System.out.println("==> param => " + param + ", " + request.getQueryString());

		String hasil = DokuResponseServlet.prosesTransaksi(param);

		response.setHeader("Content-Type", "text/plain");

		PrintWriter writer = response.getWriter();
		writer.write(hasil);
	}

}
