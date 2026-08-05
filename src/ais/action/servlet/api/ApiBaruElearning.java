package ais.action.servlet.api;

import java.util.List;

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

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

public class ApiBaruElearning {

	@SuppressWarnings({ "unchecked" })
	public static JSONObject daftarJadwalPelajaran(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				final String keyword = request.isNull("keyword") ? null : request.getString("keyword").trim();
				final String ta = request.isNull("ta") ? null : request.getString("ta");
				final Integer smt = request.isNull("smt") ? null : request.getInt("smt");
				Integer tampil_per_halaman = request.isNull("tampil_per_halaman") ? 10
						: Integer.parseInt(request.get("tampil_per_halaman") + "");
				Integer halaman = request.isNull("halaman") ? 0 : Integer.parseInt(request.get("halaman") + "");

				Session session = HibernateUtil.openSession();
				try {

				class CriteriaData {

					public Criteria buat(Session session, boolean order, Tbmuser tbmuser) {
						Yayasan yayasan = tbmuser.ambilYayasan();
						Sekolah sekolah = tbmuser.ambilSekolah();
						Guru guru = tbmuser.ambilGuru();

						Criterion criterion = Restrictions.or(Restrictions.eq("guru", guru),
								Restrictions.eq("guru2", guru));

						criterion = Restrictions.or(criterion, Restrictions.eq("guru3", guru));
						criterion = Restrictions.or(criterion, Restrictions.eq("guru4", guru));
						criterion = Restrictions.or(criterion, Restrictions.eq("guru5", guru));
						criterion = Restrictions.or(criterion, Restrictions.eq("guru6", guru));
						criterion = Restrictions.or(criterion, Restrictions.eq("guru7", guru));
						criterion = Restrictions.or(criterion, Restrictions.eq("guru8", guru));
						criterion = Restrictions.or(criterion, Restrictions.eq("guru9", guru));
						criterion = Restrictions.or(criterion, Restrictions.eq("guru10", guru));
						criterion = Restrictions.or(criterion, Restrictions.eq("guru11", guru));
						criterion = Restrictions.or(criterion, Restrictions.eq("guru12", guru));

						Criteria criteria = session.createCriteria(JadwalPelajaran.class)
								.add(Restrictions.isNotNull("tahunAjaran"))

								.add(ta == null || ta.trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("tahunAjaran", ta))

								.add(smt == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.sqlRestriction("this_.semester % 2 = " + smt))

								.createAlias("matapelajaran", "matapelajaran")

								.add(guru == null ? Restrictions.sqlRestriction("1=1") : criterion)
								.add(yayasan == null || yayasan.getId() == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("yayasan", yayasan))
								.add(sekolah == null || sekolah.getId() == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("sekolah", sekolah));

						if (order) {
							criteria.addOrder(Order.desc("tahunAjaran")).addOrder(Order.desc("semester"))
									.addOrder(Order.desc("matapelajaran.nama"));
						}

						if (!keyword.trim().isEmpty()) {
							criteria.add(Restrictions.or(
									Restrictions.ilike("matapelajaran.kode", keyword.trim(), MatchMode.ANYWHERE),
									Restrictions.ilike("matapelajaran.nama", keyword.trim(), MatchMode.ANYWHERE)));
						}

						return criteria;
					}

				}

				CriteriaData criteriaData = new CriteriaData();

				int count = ((Number) criteriaData.buat(session, false, tbmuser).setProjection(Projections.rowCount())
						.uniqueResult()).intValue();

				List<JadwalPelajaran> jadwalPelajarans = ConstantValues
						.simpleList(criteriaData.buat(session, true, tbmuser).setMaxResults(tampil_per_halaman)
								.setFirstResult(tampil_per_halaman * halaman), JadwalPelajaran.class);

				JSONArray data = new JSONArray();

				for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {
					JSONObject jsonObjectData = new JSONObject();
					Common.insertProperty(JadwalPelajaran.class, jadwalPelajaran, jsonObjectData, "", 1, "sekolah",
							"yayasan");
					data.put(jsonObjectData);
				}

				jsonObject.put("data", data);
				jsonObject.put("jumlah", count);

				// session.disconnect();
				ApiHelperSupport.closeOpenedSession(session);

				jsonObject.put("status", "00");
				jsonObject.put("description", "Ambil data berhasil");
				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ApiBaruElearning.java:137");
			}
		}
		return jsonObject;
	}

}
