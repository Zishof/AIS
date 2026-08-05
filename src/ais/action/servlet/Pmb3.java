package ais.action.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.pmb.TampilanPengumumanPMBAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DiskusiPengumumanAkademis;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PerguruanTinggi;
import ais.database.model.file.LampiranLain;
import ais.ui.util.WaktuUtil;

/**
 * Servlet implementation class CheckISBN
 */
public class Pmb3 extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Pmb3() {
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

	private JSONObject populate(PengumumanAkademis pengumumanAkademis, HttpServletRequest request, Session session)
			throws Exception {

		String html = pengumumanAkademis.getCatatan();
		List<String> urlGambar = Common.ambilUrlGambarDariHtml(html);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", pengumumanAkademis.getId());
		jsonObject.put("cover", urlGambar.isEmpty() ? Common.getRequestHostWithProtocol(request) + "/img/pengumuman.png"
				: urlGambar.get(0));
		jsonObject.put("title", pengumumanAkademis.getNama());
		jsonObject.put("content", pengumumanAkademis.getCatatan());

		jsonObject.put("link", "#");
		jsonObject.put("lampiran", "#");
		return jsonObject;
	}

	private JSONObject populate(GelombangPendaftaran gelombangPendaftaran, HttpServletRequest request, Session session)
			throws Exception {

		LampiranLain lampiranLain = LampiranLain.ambil(gelombangPendaftaran.getId(), LampiranLain.ICON_GELOMBANG_PMB);
		LampiranLain lampiranLainInfo = LampiranLain.ambil(gelombangPendaftaran.getId(), "INFO_PMB");

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", gelombangPendaftaran.getId());
		jsonObject.put("cover",
				lampiranLain == null || lampiranLain.getId() == null
						? Common.getRequestHostWithProtocol(request) + "/img/jalur.png"
						: lampiranLain.createLinkUri());
		jsonObject.put("nama", gelombangPendaftaran.getNama());
		jsonObject.put("info", gelombangPendaftaran.getInfo());
		jsonObject.put("keterangan", gelombangPendaftaran.getKeterangan());

//		jsonObject.put("biaya", gelombangPendaftaran.getNama());

		if (lampiranLainInfo != null) {
			jsonObject.put("brosur", lampiranLainInfo.createLinkUri());
		}

		return jsonObject;
	}

	private JSONObject populate(Jurusan jurusan, HttpServletRequest request, Session session) throws Exception {

		LampiranLain lampiranLain = LampiranLain.ambil(jurusan.getId(), LampiranLain.ICON_JURUSAN);
		LampiranLain infoProdi = LampiranLain.ambil(jurusan.getId(), LampiranLain.INFO_JURUSAN);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", jurusan.getId());
		jsonObject.put("cover",
				lampiranLain == null || lampiranLain.getId() == null
						? Common.getRequestHostWithProtocol(request) + "/img/prodi.png"
						: lampiranLain.createLinkUri());
		jsonObject.put("nama", jurusan.getNama());
		jsonObject.put("deskripsi", Common.simpleString(jurusan.getDeskripsi(), 50));
		jsonObject.put("infoRinci", jurusan.getDeskripsi());

		jsonObject.put("keterangan", jurusan.getKeterangan());
		jsonObject.put("link", "#");
		if (infoProdi != null) {
			jsonObject.put("lampiran", infoProdi.createLinkUri());
		}

		return jsonObject;
	}

	@SuppressWarnings({ "unchecked" })
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		Session session = HibernateUtil.getSessionFactory().openSession();
		PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);

		JSONArray pengumumanData = new JSONArray();
		JSONArray jalurData = new JSONArray();
		JSONArray prodiData = new JSONArray();
		JSONArray forumData = new JSONArray();
		String bannerUrl = Common.getRequestHostWithProtocol(request) + "/img/banner_pmb.jpg";
		try {
			List<PengumumanAkademis> listPengumumanAkademis = ConstantValues.simpleList(TampilanPengumumanPMBAction
					.initCriteriaStatic(true, selectedPerguruanTinggi).setMaxResults(Common.ROWS_COUNT_ON_PAGE_100),
					PengumumanAkademis.class);

			for (PengumumanAkademis pengumumanAkademis : listPengumumanAkademis) {
				pengumumanData.put(populate(pengumumanAkademis, request, session));
			}

			Date sekarang = WaktuUtil.getDate();
			List<GelombangPendaftaran> gelombangPendaftaransData = new ArrayList<GelombangPendaftaran>();
			Map<Long, GeneralValueObject> gelombangsAktif = ConstantValues.ambilBerdasarClass(GelombangPendaftaran.class);
			for (Long gelId : gelombangsAktif.keySet()) {
				GelombangPendaftaran gelombangPendaftaran1 = (GelombangPendaftaran) ConstantValues
						.ambil(GelombangPendaftaran.class.getName(), gelId);
				if (gelombangPendaftaran1 != null && gelombangPendaftaran1.getBisaDipilihPendaftarOnline()
						&& gelombangPendaftaran1.getAktif()
						&& (gelombangPendaftaran1.getMulai().before(sekarang) || Common.dateFormat8.get()
								.format(gelombangPendaftaran1.getMulai()).equals(Common.dateFormat8.get().format(sekarang)))
						&& (gelombangPendaftaran1.getSampai().after(sekarang) || Common.dateFormat8.get()
								.format(gelombangPendaftaran1.getSampai()).equals(Common.dateFormat8.get().format(sekarang)))) {
					gelombangPendaftaransData.add(gelombangPendaftaran1);

				}
			}

			Collections.sort(gelombangPendaftaransData);

			for (GelombangPendaftaran gelombangPendaftaran : gelombangPendaftaransData) {
				jalurData.put(populate(gelombangPendaftaran, request, session));
			}

			List<Jurusan> jurusans = ConstantValues.simpleList(session.createCriteria(Jurusan.class)
					.createAlias("fakultas", "fakultas")
					.add(Restrictions.eq("fakultas.perguruanTinggi", selectedPerguruanTinggi))
					.addOrder(Order.asc("fakultas.nama")).addOrder(Order.asc("nama")).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
					Jurusan.class);

			for (Jurusan jurusan : jurusans) {
				prodiData.put(populate(jurusan, request, session));
			}

			List<DiskusiPengumumanAkademis> diskusiPengumumanAkademis = listPengumumanAkademis.isEmpty()
					? new ArrayList<DiskusiPengumumanAkademis>()
					: session.createCriteria(DiskusiPengumumanAkademis.class)
							.add(Restrictions.in("pengumumanAkademis", listPengumumanAkademis)).addOrder(Order.desc("id"))
							.list();

			for (DiskusiPengumumanAkademis peminjamanPengadaanItemDetail : diskusiPengumumanAkademis) {
				JSONObject jsonObjectHistory = new JSONObject();
				jsonObjectHistory.put("pengumumanId", peminjamanPengadaanItemDetail.getId());
				jsonObjectHistory.put("user", peminjamanPengadaanItemDetail.getOleh());

				jsonObjectHistory.put("catatan", peminjamanPengadaanItemDetail.getCatatan());
				jsonObjectHistory.put("comment", Common.dateFormat33.get().format(peminjamanPengadaanItemDetail.getTanggal()));

				forumData.put(jsonObjectHistory);

			}
			diskusiPengumumanAkademis.clear();
			diskusiPengumumanAkademis = null;

			LampiranLain lampiranLain = LampiranLain.ambil(selectedPerguruanTinggi.getId(), LampiranLain.KOP_PMB_PT);
			if (lampiranLain != null && lampiranLain.getId() != null) {
				bannerUrl = lampiranLain.createLinkUri();
			}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Pmb3.java:222");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Pmb3.java:223");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Pmb3.java:224");}
			}
		}

		request.setAttribute("pengumumanData", pengumumanData.toString());
		request.setAttribute("jalurData", jalurData.toString());
		request.setAttribute("prodiData", prodiData.toString());
		request.setAttribute("forumData", forumData.toString());
		request.setAttribute("banner", bannerUrl);

		request.getRequestDispatcher("/WEB-INF/u/pmb3.jsp").forward(request, response);
	}

}
