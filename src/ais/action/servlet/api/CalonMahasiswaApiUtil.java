package ais.action.servlet.api;

import java.io.File;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Tbmuser;

public class CalonMahasiswaApiUtil {
	public static JSONObject biodata_calon_mahasiswa(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
						.ambil(BiodataCalonMahasiswa.class.getName(), Long.parseLong(request.get("id").toString()));

				if (biodataCalonMahasiswa == null) {
					jsonObject.put("status", "97");
					jsonObject.put("description", "Data tidak ditemukan");
				} else {

					File file = CommonReportHelper.onCetakBiodataCalonMahasiswa(biodataCalonMahasiswa, false);
					if (file == null) {
						jsonObject.put("status", "97");
						jsonObject.put("description", "File laporan tidak bisa di cetak");
					} else {
						String path = !Common.pakaiDirReportTergabung() ? Common.CURRENT_URL+"/report/"+URLEncoder.encode(file.getName(), "UTF-8") :  Common.CURRENT_URL+"/pdf?p=" + URLEncoder.encode(Common.desEncrypter.get().encrypt(file.getName()), "UTF-8");;
						jsonObject.put("url", path);
						jsonObject.put("status", "00");
						jsonObject.put("description", "OK");
					}

				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/CalonMahasiswaApiUtil.java:52");
			}
		}
		return jsonObject;
	}
}
