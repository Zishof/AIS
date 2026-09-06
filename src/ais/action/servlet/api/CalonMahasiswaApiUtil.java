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

/**
 * Utilitas endpoint servlet API untuk kebutuhan biodata calon mahasiswa (PMB). Saat ini hanya
 * berisi satu operasi: menghasilkan/menyediakan URL berkas PDF cetak biodata calon mahasiswa bagi
 * klien API (mis. aplikasi mobile PMB) yang sudah terautentikasi lewat token.
 */
public class CalonMahasiswaApiUtil {
	/**
	 * Menangani permintaan API untuk mencetak/mengambil URL biodata calon mahasiswa dalam bentuk
	 * PDF. Memvalidasi token pemanggil lewat {@link ApiUtil#currentUser(JSONObject, HttpServletRequest)},
	 * memuat {@link BiodataCalonMahasiswa} berdasarkan {@code id} pada {@code request}, mencetak
	 * laporan biodata lewat {@link CommonReportHelper#onCetakBiodataCalonMahasiswa}, lalu
	 * mengembalikan URL berkas hasil cetak (dienkripsi bila konfigurasi direktori laporan
	 * tergabung aktif). Seluruh kegagalan ditangkap dan diterjemahkan menjadi respons JSON
	 * berkode status non-{@code "00"} (tidak melempar exception ke pemanggil servlet).
	 *
	 * @param req     permintaan HTTP asli (dipakai untuk resolusi token/user)
	 * @param request payload JSON permintaan, wajib memuat {@code id} (id
	 *                {@link BiodataCalonMahasiswa})
	 * @return objek JSON respons dengan kunci {@code status} ({@code "00"} sukses, {@code "97"}
	 *         data/token tidak valid, {@code "90"} error tak terduga), {@code description}, dan
	 *         {@code url} (bila sukses) berisi tautan berkas PDF biodata
	 */
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
						String path = !Common.pakaiDirReportTergabung()
								? ApiHelperSupport.absoluteUrl(req, "/report/" + URLEncoder.encode(file.getName(), "UTF-8"))
								: ApiHelperSupport.absoluteUrl(req, "/pdf?p=" + URLEncoder.encode(Common.desEncrypter.get().encrypt(file.getName()), "UTF-8"));
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
