package ais.action.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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

import ais.action.master.PengumumanAkademisAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoDosen;
import ais.ui.util.WaktuUtil;

/**
 * Servlet implementation class CheckISBN
 */
public class Jadwal extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Jadwal() {
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

	private JSONObject populate(Perkuliahan perkuliahan, Dosen dosen, HttpServletRequest request) throws Exception {
		FileFotoLain fotodosen = dosen == null ? null
				: FileFotoLain.ambil(dosen.getId(), FotoDosen.DEFAULT_JENIS, FotoDosen.class);
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("foto",
				dosen == null ? (Common.getRequestHostWithProtocol(request) + "/img/graduated-icon.png")
						: (fotodosen == null || fotodosen.getId() == null
								? Common.getRequestHostWithProtocol(request) + "/img/graduated-icon.png"
								: fotodosen.createLinkUri()));
		jsonObject.put("nama", dosen == null ? "Tanpa Dosen" : dosen.getNama());
		jsonObject.put("nidn", dosen == null ? "" : dosen.getNidn());
		jsonObject.put("matakuliah", (perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama())
				+ " (" + perkuliahan.getSemester() + " " + perkuliahan.getKelas() + ")");
		jsonObject.put("hari", perkuliahan.getHari());
		jsonObject.put("jam", perkuliahan.getWaktuMulai() == null ? ""
				: (perkuliahan.getWaktuMulai() + " sd " + perkuliahan.getWaktuSelesai()));
		jsonObject.put("ruangan", perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama());

		return jsonObject;
	}

	@SuppressWarnings({ "unchecked" })
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		Session session = HibernateUtil.getSessionFactory().openSession();
		JSONArray weeklySchedulesData = new JSONArray();
		JSONArray dailySchedulesData = new JSONArray();
		try {
			List<Perkuliahan> perkuliahans = ConstantValues.simpleList(session.createCriteria(Perkuliahan.class)
					.add(Restrictions.eq("tahunAjaran", Common.getCurrentTahunAkademik()))
					.add(Restrictions.eq("ganjilGenap",
							Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP))

					.add(Restrictions.ne("hari", "")).add(Restrictions.isNotNull("hari"))

					.add(Restrictions.ge("waktuMulaiD", 0.1))

					.add(Restrictions.sqlRestriction(
							"1=1 order by case hari when 'Senin' then 1 when 'Selasa' then 2 when 'Rabu' then 3 when 'Kamis' then 4 when 'Jumat' then 5  when 'Sabtu' then 6 when 'Minggu' then 7 else 5 end, waktu_mulai_d"))

					, Perkuliahan.class);

			for (Perkuliahan perkuliahan : perkuliahans) {
				List<Dosen> dosens = perkuliahan.populateDosenBuNama();
				if (dosens.isEmpty()) {
					Dosen dosen = null;
					weeklySchedulesData.put(populate(perkuliahan, dosen, request));
				} else {
					for (Dosen dosen : perkuliahan.populateDosenBuNama()) {
						weeklySchedulesData.put(populate(perkuliahan, dosen, request));
					}
				}
			}

			request.setAttribute("weeklySchedulesData", weeklySchedulesData.toString());

			String sekarang = Common.dateFormat8.get().format(WaktuUtil.getDate());
			Map<Long, Pertemuan> pertemuans = PengumumanAkademisAction.pertemuansHarian.get(sekarang);
			if (pertemuans == null) {

				List<Pertemuan> pertemuansData = session.createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("waktuMulai")).add(Restrictions.eq("tanggal", WaktuUtil.getDate()))
						.add(Restrictions.or(Restrictions.isNotNull("jadwalPelajaran"),
								Restrictions.isNotNull("perkuliahan")))
						.list();
				pertemuans = new java.util.concurrent.ConcurrentHashMap<Long, Pertemuan>();
				for (Pertemuan pertemuan : pertemuansData) {
					pertemuans.put(pertemuan.getId(), pertemuan);
				}
				pertemuansData = null;
				PengumumanAkademisAction.pertemuansHarian.put(sekarang, pertemuans);
			}

			for (Map<Long, Pertemuan> pertemuansD : new ArrayList<Map<Long, Pertemuan>>(
					PengumumanAkademisAction.pertemuansHarian.values())) {
				for (Pertemuan pertemuan : new ArrayList<Pertemuan>(pertemuansD.values())) {
					if (pertemuan != null && pertemuan.getPerkuliahan() != null) {
						Perkuliahan perkuliahan = pertemuan.getPerkuliahan();
						List<Dosen> dosens = perkuliahan.populateDosenBuNama();
						if (dosens.isEmpty()) {
							Dosen dosen = null;
							dailySchedulesData.put(populate(perkuliahan, dosen, request));
						} else {
							for (Dosen dosen : perkuliahan.populateDosenBuNama()) {
								dailySchedulesData.put(populate(perkuliahan, dosen, request));
							}
						}
					}
				}
			}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Jadwal.java:163");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Jadwal.java:164");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Jadwal.java:165");}
			}
		}
		request.setAttribute("weeklySchedulesData", weeklySchedulesData.toString());
		request.setAttribute("dailySchedulesData", dailySchedulesData.toString());
		request.setAttribute("tanggal", Common.dateFormat6.get().format(WaktuUtil.getDate()));

		request.getRequestDispatcher("/WEB-INF/u/jadwal.jsp").forward(request, response);
	}

}
