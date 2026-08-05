package ais.action.servlet.api;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.json.JSONObject;

import ais.action.master.sekolah.helper.TagihanUtilCalonSiswa;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.CalonSiswa;

public class SiswaBaruApi {

	public static JSONObject pendaftaran_siswa_baru(JSONObject request, HttpServletRequest req) {
		boolean refresh = request.optBoolean("refresh", false);
		return pendaftaran_siswa_baru(request, req, refresh);
	}

	@SuppressWarnings({ "rawtypes" })
	public static JSONObject pendaftaran_siswa_baru(JSONObject request, HttpServletRequest req, boolean refresh) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = Common.getCurrentUser(req);
			List<String> warnings = new ArrayList<String>();
			Session session = HibernateUtil.openSession();
			try {
			GeneralValueObject generalValueObject = null;
			Class clazz = CalonSiswa.class;
			try {
				generalValueObject = ElearningApiUtil.simpanData(clazz, session, request, tbmuser, warnings, req);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/SiswaBaruApi.java:38");
			}

			// session.disconnect();
			ApiHelperSupport.closeOpenedSession(session);

			if (!warnings.isEmpty()) {
				String w = "";
				for (String ww : warnings) {
					w += w.isEmpty() ? ww : "\n" + ww;
				}
				jsonObject.put("status", "90");
				jsonObject.put("description", w);
			} else if (generalValueObject != null) {
				JSONObject object = new JSONObject();
				Common.insertProperty(clazz, generalValueObject, object, "", 1);
				jsonObject.put("data", object);
				jsonObject.put("status", "00");
				jsonObject.put("description", "Simpan data berhasil");

				CalonSiswa calonSiswa = (CalonSiswa) generalValueObject;
				if (calonSiswa.getGelombangPendaftaranPsb() != null
						&& calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolah() != null) {

					TagihanUtilCalonSiswa.doGenerateTagihanInsendentil(calonSiswa,
							calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolah(), refresh);
				}
				if (calonSiswa.getGelombangPendaftaranPsb() != null
						&& calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahTerverifikasi() != null) {

					TagihanUtilCalonSiswa.doGenerateTagihanInsendentil(calonSiswa,
							calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahTerverifikasi(), refresh);
				}
				if (calonSiswa.getGelombangPendaftaranPsb() != null
						&& calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahLulus() != null) {

					TagihanUtilCalonSiswa.doGenerateTagihanInsendentil(calonSiswa,
							calonSiswa.getGelombangPendaftaranPsb().getJenisBiayaSekolahLulus(), refresh);
				}
				String link_bayar = Common.getRequestHostWithProtocol(req)
						+ "/pages/master/sekolah/pembayaran_online.zul?calon_siswa=" + calonSiswa.getId()
						+ "&langsungBayar=true";
				jsonObject.put("link_bayar", link_bayar);
			} else {
				jsonObject.put("status", "01");
				jsonObject.put("description", "Simpan data gagal");
			}

			} finally {
				HibernateUtil.closeSessionQuietly(session);
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/SiswaBaruApi.java:95");
			}
		}
		return jsonObject;
	}

}
