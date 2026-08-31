package ais.common;

import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Negara;
import ais.database.model.Pegawai;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;

/**
 * Generator UI dinamis untuk jsp curd generator. Kelas ini menerjemahkan metadata/konfigurasi
 * menjadi form, tabel, tab, atau wizard sehingga action tidak membangun ulang struktur komponen
 * yang sama.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code renderUploadFile()}, {@code
 * renderUploadFile()}, {@code renderUploadFile()}, {@code uploadFile()}); validasi/perhitungan ({@code
 * validation()}); pelaporan/ekspor ({@code renderText()}); operasi domain lain ({@code parseDateSafe()}, {@code
 * textEditor()}, {@code generateSingleInput()}, {@code pilihanValSemuaWizard()}, {@code pilihanValSemuaTab()},
 * {@code pilihanValSemua()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
 * <p><b>Efek samping:</b> operasi membuat/mengubah komponen UI dan dapat membaca metadata atau data persistence.
 * Jalankan dengan desktop/session aktif dan gunakan generator ini sebagai satu sumber struktur dinamis, bukan
 * menyalin konstruksi widget ke action.</p>
 */
public class JSPCurdGenerator {

	// --- HELPER METHODS UNTUK MENGURANGI DUPLIKASI ---

	/**
	 * Helper untuk memparsing tanggal dengan beberapa format fallback.
	 */
	private static Date parseDateSafe(Object val) {
		if (val == null)
			return null;
		if (val instanceof Date)
			return (Date) val;

		String dateStr = val.toString();
		try {
			return Common.timeFormat2.get().parse(dateStr);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/JSPCurdGenerator.java:58");
		}
		try {
			return Common.timeFormat.get().parse(dateStr);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/JSPCurdGenerator.java:62");
		}
		try {
			return Common.dateFormat1.get().parse(dateStr);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/JSPCurdGenerator.java:66");
		}

		return null;
	}

	
	
	public static void renderUploadFile(StringBuilder sb, String type, String col, String randomID) throws Exception {
		String baseUrl = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=common";
		String linkUpload = baseUrl + "&s=upload_component";

		// PERBAIKAN 1: Buka Async Function (IIFE)
		sb.append("(async () => {");

		sb.append("    try {\r\n" + "        let col = \"" + col + "\";\r\n" + "        let type = \"" + type
				+ "\";\r\n" + "        const json = (typeof type === 'string') ? JSON.parse(type) : type;\r\n" + "\r\n"
				+ "        // 1. Siapkan Parameter\r\n"
				+ "        const clazzVal = (json.clazz !== null && json.clazz !== undefined) ? String(json.clazz) : \"\";\r\n"
				+ "        const jenisVal = (json.jenis !== null && json.jenis !== undefined) ? String(json.jenis) : \"\";\r\n"
				+ "        let idVal      = !isEmpty" + randomID + "(item.id) ? item.id : '';\r\n" + "\r\n"
				+ "        // Logika IF untuk ID\r\n" + "        if (idVal && idVal.trim() !== \"\") {\r\n"
				+ "            idVal = \"&id=\" + idVal.trim();\r\n" + "        } else {\r\n"
				+ "            idVal = \"\"; \r\n" + "        }\r\n" + "\r\n" + "        // 2. Susun URL Path\r\n"
				+ "        const jspUrl = \"" + linkUpload + "&\" + \r\n"
				+ "                       \"clazz=\" + encodeURIComponent(clazzVal) + \r\n"
				+ "                       \"&jenis=\" + encodeURIComponent(jenisVal) + \r\n"
				+ "                       idVal + \r\n"
				+ "                       \"&col=\" + encodeURIComponent(col) + \r\n"
				+ "                       \"&rnd=\" + encodeURIComponent('" + randomID + "');\r\n" + "\r\n"
				+ "        console.log(\"URL:\", jspUrl);\r\n" + "\r\n");

		// Bagian ini sekarang aman karena berada di dalam async function
		sb.append("        const response" + randomID + " = await fetch(jspUrl);");
		sb.append("        const html" + randomID + " = await response" + randomID + ".text();");
		sb.append("        const targetDiv = document.getElementById('kolom_div_'" + col + "'_" + randomID + "');\r\n");
		sb.append("        if (targetDiv) {\r\n");
		sb.append("             // Kosongkan container dulu\r\n" + "	                targetDiv.innerHTML = ''; \r\n"
				+ "\r\n" + "	                // Teknik agar <script> di dalam 'html' bisa jalan\r\n"
				+ "	                const range = document.createRange();\r\n" + "	                \r\n"
				+ "	                // Set range ke dalam container agar konteksnya benar\r\n"
				+ "	                range.selectNode(targetDiv); // atau document.body\r\n" + "\r\n"
				+ "	                // Parse string HTML menjadi elemen DOM yang 'hidup'\r\n"
				+ "	                const fragment = range.createContextualFragment(html" + randomID + ");\r\n"
				+ "	                \r\n" + "	                //console.info(html" + randomID + ");\r\n" + "\r\n"
				+ "	                // Masukkan ke dalam modal\r\n"
				+ "	                targetDiv.appendChild(fragment);\r\n");

		sb.append("        }\r\n");

		sb.append("    } catch (e) {\r\n"
				+ "        console.error(\"Terjadi error saat parsing JSON atau menyusun URL:\", e);\r\n" + "    }");

		// PERBAIKAN 2: Tutup Async Function dan jalankan langsung
		sb.append("})();");
	}
	
	
	
	
	public static void renderText(StringBuilder sb, String randomID) throws Exception {
	    String baseUrl = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=common";
	    String linkUpload = baseUrl + "&s=text_area";

	    // PERBAIKAN 1: Buka Async Function (IIFE)
	    sb.append("(async () => {");

	    sb.append("    try {"
	            // PERBAIKAN 2: Pembuatan URL dirapikan
	            + "        const jspUrl = \"" + linkUpload + "&\" + \r\n"
	            + "                       \"idtextarea=\" + encodeURIComponent('input_C'+i+'_'+f+'_" + randomID + "') + \r\n"
	            + "                       \"&namatextarea=\" + encodeURIComponent('input_C'+i+'_'+f+'_" + randomID + "') + \r\n"
	            + "                       \"&rnd=\" + encodeURIComponent('" + randomID + "');\r\n"
	            + "        \r\n"
	            + "        console.log(\"URL:\", jspUrl);\r\n");

	    // PERBAIKAN 3: Hapus randomID dari nama variabel JS ('response' dan 'html')
	    // Karena sudah di dalam scope async function, nama 'response' aman digunakan dan tidak akan bentrok.
	    sb.append("        const response = await fetch(jspUrl + '&isi=' + encodeURIComponent(dataItem[f]));\r\n");
	    sb.append("        const html = await response.text();\r\n");
	    
	    // Selector div tetap menggunakan randomID karena ID elemen HTML harus unik
	    sb.append("        const targetDiv = document.getElementById('kolom_div_'+f+'_" + randomID + "');\r\n");
	    sb.append("        console.log('targetDiv', targetDiv);\r\n");
	    sb.append("        console.log('nama kolom div ', 'kolom_div_'+f+'_" + randomID + "');\r\n");
	    sb.append("        if (targetDiv) {\r\n");
	    sb.append("             // Kosongkan container dulu\r\n"
	            + "             targetDiv.innerHTML = ''; \r\n"
	            + "             const range = document.createRange();\r\n"
	            + "             \r\n"
	            + "             // Set range ke dalam container agar konteksnya benar\r\n"
	            + "             range.selectNode(targetDiv);\r\n"
	            + "             \r\n"
	            + "             // Parse string HTML menjadi elemen DOM\r\n"
	            // Gunakan variabel 'html' yang bersih
	            + "             const fragment = range.createContextualFragment(html);\r\n"
	            + "             \r\n"
	            + "             // Masukkan ke dalam modal\r\n"
	            + "             targetDiv.appendChild(fragment);\r\n");

	    sb.append("        }\r\n");

	    sb.append("    } catch (e) {\r\n"
	            + "        console.error(\"Terjadi error saat parsing atau fetch (" + randomID + "):\", e);\r\n" 
	            + "    }");

	    // PERBAIKAN 4: Tutup Async Function
	    sb.append("})();");
	}

	public static void renderUploadFile(StringBuilder sb, String randomID) throws Exception {
		String rs = Common.getGeneratedBarCode(7);
		String baseUrl = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=common";
		String linkUpload = baseUrl + "&s=upload_component";
        sb.append("                    // Ambil config JSON yang disimpan di wizardConfigs_\n");
        sb.append("                    const fileConfig"+rs+" = conf.columnConfigs[f];\n");
		// --- Buka Async Function Wrapper ---
		sb.append("(async () => {\r\n");

		// Bagian 1: Parsing string col dan type
		sb.append("    let type = fileConfig"+rs+";\r\n");
		// Bagian 2: Parsing JSON dan Fetch URL
		sb.append("    try {\r\n" + "        // --- PERBAIKAN DI SINI ---\r\n"
				+ "        // Inisialisasi json sebagai object kosong default\r\n"
				+ "        //console.log(\"type:\", type);\r\n" // Uncomment jika butuh debug
				+ "        let json = {};\r\n" 
				+ "        \r\n"
				+ "        // Hanya lakukan parsing jika 'type' memiliki isi (tidak kosong)\r\n"
				+ "        if (type) {\r\n" 
				+ "            try {\r\n"
				+ "                json = (typeof type === 'string') ? JSON.parse(type) : type;\r\n"
				+ "            } catch (jsonErr) {\r\n"
				+ "                console.warn(\"Format type bukan JSON valid, menggunakan default.\", jsonErr);\r\n"
				+ "            }\r\n" + "        }\r\n" + "\r\n" + "        console.log(\"json:\", json);\r\n" // Uncomment
																												// jika
																												// butuh
																												// debug
				+ "        // 1. Siapkan Parameter\r\n"
				+ "        // Menggunakan trik (val || \"\") untuk menangani null/undefined\r\n"
				+ "        const clazzVal = (json.clazz !== null && json.clazz !== undefined) ? String(json.clazz) : \"\";\r\n"
				+ "        const jenisVal = (json.jenis !== null && json.jenis !== undefined) ? String(json.jenis) : \"\";\r\n"
				+ "        let idVal      = !isEmpty" + randomID + "(dataItem.id) ? dataItem.id : '';\r\n" + "\r\n"
				+ "        // Logika IF untuk ID\r\n" + "        if (idVal && idVal.trim() !== \"\") {\r\n"
				+ "            idVal = \"&id=\" + idVal.trim();\r\n" + "        } else {\r\n"
				+ "            idVal = \"\"; \r\n" + "        }\r\n" + "\r\n"
				+ "        // 2. Susun URL Path beserta Query String\r\n" 
				+ "        const jspUrl = \"" + linkUpload+ "&\" + \r\n" + "                       \"clazz=\" + encodeURIComponent(clazzVal) + \r\n"
				+ "                       \"&jenis=\" + encodeURIComponent(jenisVal) + \r\n"
				+ "                       idVal + \r\n"
				+ "                       \"&col=\" + encodeURIComponent(col) + \r\n"
				+ "                       \"&rnd=\" + encodeURIComponent('" + randomID + "');\r\n" + "\r\n"
				+ "        //console.log(\"URL:\", jspUrl);\r\n" // Uncomment jika butuh debug
				+ "\r\n");

		// Bagian 3: Fetch dan Render
		sb.append("        const response" + randomID + " = await fetch(jspUrl);\r\n");
		sb.append("        const html" + randomID + " = await response" + randomID + ".text();\r\n");
		sb.append("        \r\n");
		sb.append(
				"        // Pastikan elemen tujuan ada sebelum set innerHTML agar tidak error null pointer di JS\r\n");
		sb.append("        const targetDiv = document.getElementById('kolom_div_'+col+'_" + randomID + "');\r\n");
		sb.append("        if (targetDiv) {\r\n");
		sb.append("             // Kosongkan container dulu\r\n" + "	                targetDiv.innerHTML = ''; \r\n"
				+ "\r\n" + "	                // Teknik agar <script> di dalam 'html' bisa jalan\r\n"
				+ "	                const range = document.createRange();\r\n" + "	                \r\n"
				+ "	                // Set range ke dalam container agar konteksnya benar\r\n"
				+ "	                range.selectNode(targetDiv); // atau document.body\r\n" + "\r\n"
				+ "	                // Parse string HTML menjadi elemen DOM yang 'hidup'\r\n"
				+ "	                const fragment = range.createContextualFragment(html" + randomID + ");\r\n"
				+ "	                \r\n" + "	                //console.info(html" + randomID + ");\r\n" + "\r\n"
				+ "	                // Masukkan ke dalam modal\r\n"
				+ "	                targetDiv.appendChild(fragment);\r\n");

		sb.append("        }\r\n");

		sb.append("    } catch (e) {\r\n"
				+ "        console.error(\"Terjadi error saat parsing JSON atau menyusun URL:\", e);\r\n"
				+ "    }\r\n");

		// --- Tutup Async Function Wrapper ---
		sb.append("})();");
	}

	public static void renderUploadFile(StringBuilder sb, String[] formCols, String randomID) throws Exception {
		for (String col : formCols) {
			String type = "";
			try {
				String[] ss = col.split(";");
				col = ss.length > 0 ? ss[0] : "";
				type = ss.length > 1 ? ss[1] : "";
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/JSPCurdGenerator.java:256");
				// TODO: handle exception
			}
			if (col != null && !col.trim().isEmpty()) {
				if (col.startsWith("file_") && Common.isValidJsonObject(type)) {
					JSONObject json = new JSONObject(type);
					System.out.println("json -> " + json);
					String clazzVal = json.isNull("clazz") ? "" : json.get("clazz") + "";
					String jenisVal = json.isNull("jenis") ? "" : json.get("jenis") + "";
					String urlFile = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=common&s=upload_component?clazz="
							+ URLEncoder.encode(clazzVal, "UTF-8") + "&jenis=" + URLEncoder.encode(jenisVal, "UTF-8")
							+ "&col=" + URLEncoder.encode(col, "UTF-8") + "&rnd="
							+ URLEncoder.encode(randomID, "UTF-8");

					sb.append(" const  response" + randomID + " = await fetch('" + urlFile + "?id=item.id');");
					sb.append(" const html" + randomID + " = await response" + randomID + ".text();");
					sb.append(" const targetDiv = document.getElementById('kolom_div_" + col + "_" + randomID + "');");

					sb.append("        if (targetDiv) {\r\n");
					sb.append("             // Kosongkan container dulu\r\n"
							+ "	                targetDiv.innerHTML = ''; \r\n" + "\r\n"
							+ "	                // Teknik agar <script> di dalam 'html' bisa jalan\r\n"
							+ "	                const range = document.createRange();\r\n" + "	                \r\n"
							+ "	                // Set range ke dalam container agar konteksnya benar\r\n"
							+ "	                range.selectNode(targetDiv); // atau document.body\r\n" + "\r\n"
							+ "	                // Parse string HTML menjadi elemen DOM yang 'hidup'\r\n"
							+ "	                const fragment = range.createContextualFragment(html" + randomID
							+ ");\r\n" + "	                \r\n" + "	                //console.info(html" + randomID
							+ ");\r\n" + "\r\n" + "	                // Masukkan ke dalam modal\r\n"
							+ "	                targetDiv.appendChild(fragment);\r\n");
					sb.append("        }\r\n");

				}
			}
		}
	}

	public static void uploadFile(StringBuilder sb, String col, String label, String type, boolean isRequired,
			String randomID) {
		try {

			String asterisk = isRequired ? " <span class='text-danger'>*</span>" : "";

			sb.append("        <label class='form-label fw-semibold' for='")
					.append("input_dofile_" + col + "_" + randomID).append("'>").append(label).append(asterisk)
					.append("</label>\n");

			JSONObject json = new JSONObject(type);
//			System.out.println("json -> " + json);
			// 1. Siapkan Parameter
			String clazzVal = json.isNull("clazz") ? "" : json.get("clazz") + "";
			String jenisVal = json.isNull("jenis") ? "" : json.get("jenis") + "";
			String idVal = json.isNull("id") ? "" : json.get("id") + "";

			// Logika IF untuk ID (sesuai request Anda)
			if (idVal != null && !idVal.trim().isEmpty()) {
				idVal = "&id=" + idVal.trim();
			}

			// 2. Susun URL Path beserta Query String (karena ini pengganti <jsp:param>)
			// Pastikan path JSP dimulai dengan "/"
			String jspUrl = "/WEB-INF/baru/modul/common/upload_component.jsp?" + "clazz="
					+ URLEncoder.encode(clazzVal, "UTF-8") + "&jenis=" + URLEncoder.encode(jenisVal, "UTF-8") + idVal
					+ "&col=" + URLEncoder.encode(col, "UTF-8") + "&rnd=" + URLEncoder.encode(randomID, "UTF-8"); // idVal
																													// sudah
																													// ada
																													// '&'
																													// nya
																													// atau
																													// kosong

			try {
				// 3. Panggil Helper untuk render JSP ke String
				String hasilRender = JspRenderer.renderJsp(RequestContext.get(), ResponseContext.get(), jspUrl);

				// 4. Masukkan ke StringBuilder
				sb.append(hasilRender);

				// Test output di console
//				System.out.println("Hasil Capture JSP: " + sb.toString());

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/JSPCurdGenerator.java:338");
				sb.append("Gagal meload komponen upload: " + e.getMessage());
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/JSPCurdGenerator.java:342");
		}
	}
	
	
	public static void textEditor(StringBuilder sb, String col, String label, String uniqueInputID, boolean isRequired,
			String randomID, String val) {
		try {

			String asterisk = isRequired ? " <span class='text-danger'>*</span>" : "";
			
			//String namatextarea ="input_C" + col + "_" + randomID;
			sb.append("        <label class='form-label fw-semibold' for='")
					.append(uniqueInputID).append("'>").append(label).append(asterisk)
					.append("</label>\n");
			
			
			// 2. Susun URL Path beserta Query String (karena ini pengganti <jsp:param>)
			// Pastikan path JSP dimulai dengan "/"
			String jspUrl = "/WEB-INF/baru/modul/common/text_area.jsp?idtextarea="
					+ URLEncoder.encode(uniqueInputID, "UTF-8") + "&namatextarea=" + URLEncoder.encode(uniqueInputID, "UTF-8") 
					+ "&isi=" + URLEncoder.encode(val == null ? "" : val, "UTF-8") 
					+ "&isRequired=" + isRequired + "&rnd=" + URLEncoder.encode(randomID, "UTF-8"); // idVal
																													// sudah
																													// ada
																													// '&'
																													// nya
																													// atau
																													// kosong
			//System.out.println("jspUrl: " + jspUrl);
			try {
				// 3. Panggil Helper untuk render JSP ke String
				String hasilRender = JspRenderer.renderJsp(RequestContext.get(), ResponseContext.get(), jspUrl);

				// 4. Masukkan ke StringBuilder
				sb.append(hasilRender);

				// Test output di console
				//System.out.println("Hasil Capture JSP: " + sb.toString());

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/JSPCurdGenerator.java:383");
				sb.append("Gagal meload komponen upload: " + e.getMessage());
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/JSPCurdGenerator.java:387");
		}
	}

	/**
	 * Helper utama untuk generate HTML Input. Digunakan oleh Wizard, Tab, dan Form
	 * Biasa.
	 */
	private static void generateSingleInput(StringBuilder sb, String colRaw, String labelOverride,
			ClassMetadata classMetadata, GeneralValueObject generalValueObject, Set<String> paramRequired,
			String randomID, boolean useBootstrapGrid) {

		if (colRaw == null || colRaw.trim().isEmpty())
			return;

		String[] parts = colRaw.split(";");
		String col = parts[0];
		String type = (parts.length > 1) ? parts[1] : "";
		String editor = (parts.length > 2) ? parts[2] : "";

		// Tentukan Label
		String label = labelOverride;
		if (label == null || label.equals(col)) {
			label = col.replaceAll("([a-z])([A-Z]+)", "$1 $2");
			label = (label.length() > 0) ? label.substring(0, 1).toUpperCase() + label.substring(1) : label;
		}
		label = Common.getBahasaConfig(label);
//		System.out.println("col -> " + col + ", type -> " + type);
		boolean isRequired = paramRequired.contains(col);

		if (col.startsWith("file_") && Common.isValidJsonObject(type)) {
			// Wrapper Column
			if (useBootstrapGrid) {
				sb.append("      <div class='").append("col-12").append("'>\n");
			}

			JSPCurdGenerator.uploadFile(sb, col, label, type, isRequired, randomID);
		} else {
			String returnName = null;
			// Ambil Value & Tipe Data
			Object val = null;
			try {
				val = classMetadata.getPropertyValue(generalValueObject, col, EntityMode.POJO);
				returnName = classMetadata.getPropertyType(col).getReturnedClass().getName();
			} catch (Exception e) {
				return;
//				e.printStackTrace();
			}
			String requiredAttr = isRequired ? "required" : "";
			String asterisk = isRequired ? " <span class='text-danger'>*</span>" : "";

			String inputId = "input_" + col + "_" + randomID;

			// Tentukan Layout (Full width atau Half)
			boolean isFullWidth = (type.equalsIgnoreCase("text") || type.equalsIgnoreCase("editor")) 
					|| col.toLowerCase().matches(".*(keterangan|catatan|deskripsi|alamat).*");

			// Wrapper Column
			if (useBootstrapGrid) {
				sb.append("      <div class='").append(isFullWidth ? "col-12" : "col-md-6").append("'>\n");
			}

			// --- LOGIKA RENDERING INPUT ---

			// 1. Entity / Foreign Key (Dropdown Pencarian)
			if (returnName != null && returnName.startsWith("ais.database.model")) {
				GeneralValueObject vo = (val instanceof GeneralValueObject) ? (GeneralValueObject) val : null;
				String valId = (vo != null && vo.getId() != null) ? vo.getId().toString() : "";
				String valNama = (vo != null && vo.getNama() != null) ? vo.getNama() : "";

				sb.append("        <label class='form-label small fw-bold text-uppercase text-secondary'>")
						.append(label).append(asterisk).append("</label>\n");

				String whereTambahan = "";
				try {
					if (parts.length > 1 && Common.isValidJsonObject(parts[1])) {
						JSONObject jsonObject = new JSONObject(parts[1]);
						if (!jsonObject.isNull("where")) {
							whereTambahan = " AND " + jsonObject.get("where");
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/JSPCurdGenerator.java:468");
				}

				try {
					sb.append(JSPCurdGenerator.pilihanVal(randomID, col, label, returnName, isRequired, "id", "nama",
							whereTambahan, valNama, valId));
				} catch (Exception e) {
					sb.append("");
				}

				// 2. Boolean (Switch)
			} else if (returnName != null && returnName.equals(Boolean.class.getName())) {
				boolean isChecked = (val != null && val.equals(true));
				sb.append("        <div class='form-check form-switch mt-4'>\n");
				sb.append("          <input class='form-check-input' type='checkbox' id='").append(inputId).append("' ")
						.append(isChecked ? "checked" : "").append(">\n");
				sb.append("          <label class='form-check-label fw-bold' for='").append(inputId).append("'>")
						.append(label).append("</label>\n");
				sb.append("        </div>\n");

				// 3. Time
			} else if (type.equalsIgnoreCase("time")) {
				Date s = parseDateSafe(val);
				sb.append("        <label class='form-label fw-semibold' for='").append(inputId).append("'>")
						.append(label).append(asterisk).append("</label>\n");
				sb.append("        <input type='time' class='form-control' value=\"")
						.append(s == null ? "" : Common.timeFormat.get().format(s)).append("\" id='").append(inputId)
						.append("' ").append(requiredAttr).append(" >\n");

				// 4. Date
			} else if (type.equalsIgnoreCase("date")
					|| (returnName != null && returnName.equals(Date.class.getName()))) {
				sb.append("        <label class='form-label fw-semibold' for='").append(inputId).append("'>")
						.append(label).append(asterisk).append("</label>\n");
				sb.append("        <input type='date' class='form-control' value=\"")
						.append(val == null ? "" : Common.databaseDateFormat.get().format(val)).append("\" id='")
						.append(inputId).append("' ").append(requiredAttr).append(" >\n");

				// 5. Text Area with Editor (Summernote)
			} else if ((type.equalsIgnoreCase("text") || type.equalsIgnoreCase("editor"))  && editor.equalsIgnoreCase("editor")) {
				
				
				JSPCurdGenerator.textEditor(sb, col, label, inputId, isRequired, randomID, val == null ? "" : val.toString());

				// 6. Text Area Biasa
			} else if (isFullWidth && !type.equalsIgnoreCase("text_short")) {
				sb.append("        <label class='form-label small fw-bold text-uppercase text-secondary'>")
						.append(label).append(asterisk).append("</label>\n");
				sb.append("        <textarea class='form-control' rows='3' id='").append(inputId).append("' ")
						.append(requiredAttr).append(">").append(val == null ? "" : val).append("</textarea>\n");

				// 7. Default Text Input
			} else {
				sb.append("        <label class='form-label small fw-bold text-uppercase text-secondary'>")
						.append(label).append(asterisk).append("</label>\n");
				sb.append("        <input type='text' class='form-control' id='").append(inputId).append("' value=\"")
						.append(val == null ? "" : val).append("\" ").append(requiredAttr).append(">\n");
			}
		}

		if (useBootstrapGrid) {
			sb.append("      </div>\n");
		}
	}

	// --- MAIN METHODS ---

	public static String pilihanValSemuaWizard(String randomID, String[][] formCols, Long objId,
			Map<String, String> hiddenValues, String[][] labels, ClassMetadata classMetadata,
			GeneralValueObject generalValueObject, Set<String> paramRequired) throws Exception {

		StringBuilder sb = new StringBuilder();

		sb.append("    <form id='dataForm_").append(randomID).append("'>\n");
		sb.append("      <input type='hidden' value=\"").append(objId).append("\" id='inputId_").append(randomID)
				.append("'>\n");

		if (hiddenValues != null) {
			for (Map.Entry<String, String> entry : hiddenValues.entrySet()) {
				sb.append("      <input type='hidden' value=\"").append(entry.getValue()).append("\" id='input_")
						.append(entry.getKey()).append("_").append(randomID).append("'>\n");
			}
		}

		for (int stepIndex = 0; stepIndex < formCols.length; stepIndex++) {
			sb.append("    <div id='stepContent_").append(stepIndex).append("_").append(randomID)
					.append("' class='wiz-content ").append(stepIndex == 0 ? "active" : "").append("'>\n");
			sb.append("      <div class='row g-3'>\n");

			String[] cols = formCols[stepIndex];
			String[] lbls = (labels.length > stepIndex) ? labels[stepIndex] : new String[0];

			for (int colIndex = 0; colIndex < cols.length; colIndex++) {
				String label = (lbls.length > colIndex) ? lbls[colIndex] : null;
				generateSingleInput(sb, cols[colIndex], label, classMetadata, generalValueObject, paramRequired,
						randomID, true);
			}
			sb.append("      </div>\n");
			sb.append("    </div>\n");
		}
		sb.append("    </form>\n");
		return sb.toString();
	}

	public static String pilihanValSemuaTab(String randomID, String[][] formCols, Long objId,
			Map<String, String> hiddenValues, String[][] labels, ClassMetadata classMetadata,
			GeneralValueObject generalValueObject, Set<String> paramRequired) throws Exception {

		StringBuilder sb = new StringBuilder();

		sb.append("    <form id='dataForm_").append(randomID).append("'>\n");
		sb.append("      <input type='hidden' value=\"").append(objId).append("\" id='inputId_").append(randomID)
				.append("'>\n");

		if (hiddenValues != null) {
			for (Map.Entry<String, String> entry : hiddenValues.entrySet()) {
				sb.append("      <input type='hidden' value=\"").append(entry.getValue()).append("\" id='input_")
						.append(entry.getKey()).append("_").append(randomID).append("'>\n");
			}
		}

		sb.append("      <div class='tab-content' id='myTabContent_").append(randomID).append("'>\n");

		for (int tabIndex = 0; tabIndex < formCols.length; tabIndex++) {
			boolean isActive = (tabIndex == 0);
			sb.append("        <div class='tab-pane fade ").append(isActive ? "show active" : "")
					.append("' id='tabPane_").append(tabIndex).append("_").append(randomID)
					.append("' role='tabpanel'>\n");
			sb.append("          <div class='row g-3'>\n");

			String[] cols = formCols[tabIndex];
			String[] lbls = (labels.length > tabIndex) ? labels[tabIndex] : new String[0];

			for (int colIndex = 0; colIndex < cols.length; colIndex++) {
				String label = (lbls.length > colIndex) ? lbls[colIndex] : null;
				generateSingleInput(sb, cols[colIndex], label, classMetadata, generalValueObject, paramRequired,
						randomID, true);
			}
			sb.append("          </div>\n");
			sb.append("        </div>\n");
		}
		sb.append("      </div>\n"); // End Tab Content
		sb.append("    </form>\n");
		return sb.toString();
	}

	public static String pilihanValSemua(String randomID, String[] formCols, Set<String> paramRequired,
			GeneralValueObject generalValueObject, Map<String, String> hiddenValues, String[] labels,
			ClassMetadata classMetadata) throws Exception {

		StringBuilder sb = new StringBuilder();
		Long currentId = (generalValueObject == null || generalValueObject.getId() == null) ? -1L
				: generalValueObject.getId();

		sb.append("    <form class='row g-3' id='dataForm_").append(randomID).append("'>\n");
		sb.append("      <input type='hidden' value=\"").append(currentId).append("\" id='inputId_").append(randomID)
				.append("'>\n");

		if (hiddenValues != null) {
			for (Map.Entry<String, String> entry : hiddenValues.entrySet()) {
				sb.append("      <input type='hidden' value=\"").append(entry.getValue()).append("\" id='input_")
						.append(entry.getKey()).append("_").append(randomID).append("'>\n");
			}
		}

		// Refresh object session safely
		if (generalValueObject != null && generalValueObject.getId() != null) {
			Session session = null;
			try {
				session = HibernateUtil.currentNativeSession();
				session.refresh(generalValueObject);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/JSPCurdGenerator.java:640");
			} finally {
				if (session != null && session.isOpen()) {
					try {
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/JSPCurdGenerator.java:646");
					}
				}
				HibernateUtil.closeSession();
			}
		}

		for (int i = 0; i < formCols.length; i++) {
			String label = (labels != null && labels.length > i) ? labels[i] : null;
			generateSingleInput(sb, formCols[i], label, classMetadata, generalValueObject, paramRequired, randomID,
					true);
		}

		sb.append("    </form>\n");
		return sb.toString();
	}

	public static String validation(String randomID, String[] formCols, String[] labels, Set<String> paramRequired) {
		StringBuilder sb = new StringBuilder();
		int index = 0;
		for (String col : formCols) {
			String tempCol = col; // final for processing
			String[] parts = tempCol.split(";");
			String colName = (parts.length > 0) ? parts[0] : "";

			String label = colName;
			if (labels != null && labels.length > index) {
				label = labels[index];
			} else {
				String textWithSpace = colName.replaceAll("([a-z])([A-Z]+)", "$1 $2");
				label = textWithSpace.substring(0, 1).toUpperCase() + textWithSpace.substring(1);
			}
			index++;

			if (colName != null && !colName.trim().isEmpty() && paramRequired.contains(colName)) {
				sb.append("    if(isEmpty").append(randomID).append("(data.").append(colName)
						.append(")) { tampilkanToast('").append(Common.getBahasaConfig(label)).append(" ")
						.append(Common.getBahasaConfig("wajib diisi")).append("!', 'bg-danger'); return; }");
			}
		}
		return sb.toString();
	}

	// --- OVERLOADS FOR PILIHAN ---
	public static String pilihan(String randomID, String f, String c, String returnNameFull, boolean required,
			String kolomId, String kolomNama, String whereTambahan) throws Exception {
		return pilihan(randomID, f, c, returnNameFull, required, kolomId, kolomNama, whereTambahan, "input", "");
	}

	private static String pilihanVal(String randomID, String f, String c, String returnNameFull, boolean required,
			String kolomId, String kolomNama, String whereTambahan, String valName, String valId) throws Exception {
		return pilihan(randomID, f, c, returnNameFull, required, kolomId, kolomNama, whereTambahan, "input", "",
				valName, valId);
	}

	public static String pilihan(String randomID, String f, String c, String returnNameFull, boolean required,
			String kolomId, String kolomNama, String whereTambahan, String inputName, String onEvent) throws Exception {
		return pilihan(randomID, f, c, returnNameFull, required, kolomId, kolomNama, whereTambahan, inputName, onEvent,
				"", "");
	}

	/**
	 * Metode rumit untuk generate komponen pencarian entitas (Dropdown with
	 * Search).
	 */
	public static String pilihan(String randomID, String f, String c, String returnNameFull, boolean required,
			String kolomId, String kolomNama, String whereTambahan, String inputName, String onEvent, String valName,
			String valId) throws Exception {

//		System.out.println("whereTambahan -> " + whereTambahan);
		Tbmuser tbmuser = Common.getCurrentUser(RequestContext.get());

		// --- SPECIAL HANDLERS FOR CURRENT USER RELATIONSHIPS ---
		// Refactored slightly to reduce boilerplate string building

		GeneralValueObject linkedObj = null;
		if (tbmuser != null) {
			if (returnNameFull.equals(Tbmuser.class.getName()))
				linkedObj = tbmuser;
			else if (returnNameFull.equals(BiodataCalonMahasiswa.class.getName()))
				linkedObj = tbmuser.getBiodataCalonMahasiswa();
			else if (returnNameFull.equals(Pegawai.class.getName()))
				linkedObj = tbmuser.ambilPegawai();
			else if (returnNameFull.equals(Mahasiswa.class.getName()))
				linkedObj = tbmuser.getMahasiswa();
			else if (returnNameFull.equals(Siswa.class.getName()) && tbmuser.getSiswa() != null)
				linkedObj = tbmuser.getSiswa(); // Note: Original code checks getSiswa logic separately
			else if (returnNameFull.equals(Dosen.class.getName()))
				linkedObj = tbmuser.ambilDosen();
			else if (returnNameFull.equals(Guru.class.getName()))
				linkedObj = tbmuser.ambilGuru();
		}

		// Render User/Profile Card
		if (linkedObj != null) {
			String displayNama = (linkedObj instanceof Tbmuser) ? ((Tbmuser) linkedObj).getUserNama()
					: tbmuser.getUserNama();
			String displayId = (linkedObj instanceof Tbmuser) ? ((Tbmuser) linkedObj).getUserId() : tbmuser.getUserId();
			String hiddenId = linkedObj.getId().toString();
			// Special case for Tbmuser which uses userId (String) not Id (Long) usually?
			// Following original code logic: tbmuser.getUserId() for Tbmuser, getId() for
			// others.
			if (linkedObj instanceof Tbmuser)
				hiddenId = ((Tbmuser) linkedObj).getUserId();

			String url = CommonMedia.getUrlFotoPengguna(tbmuser);

			StringBuilder sb = new StringBuilder();
			sb.append("        <input type='hidden' value='").append(hiddenId).append("' id='").append(inputName)
					.append("_").append(f).append("_").append(randomID).append("' name='").append(f).append("'>\n");
			sb.append(
					"<div class='d-flex align-items-center me-3 mb-2 bg-white rounded-pill pe-3 border' style='width:fit-content;'>\n")
					.append("   <img onclick='tampilGambar(this)' onmouseover='this.style.transform=\"scale(2.1)\"' onmouseout='this.style.transform=\"scale(1)\"' style='object-fit: cover; cursor: pointer;' src='")
					.append(url).append("' class='avatar-img me-2' alt='").append(displayNama).append("'>\n")
					.append("   <div>\n")
					.append("       <small class='d-block fw-bold text-dark' style='font-size: 0.8rem; line-height:1;'>")
					.append(displayNama).append("</small>\n")
					.append("       <small class='text-muted' style='font-size: 0.7rem;'>").append(displayId)
					.append("</small>\n").append("   </div>\n").append("</div>");
			return sb.toString();
		}

		// --- SPECIAL HANDLERS FOR STATIC RELATIONSHIPS (Simple Text Display) ---
		if (tbmuser != null) {
			GeneralValueObject staticObj = null;
			if (returnNameFull.equals(Jurusan.class.getName()))
				staticObj = tbmuser.ambilJurusan();
			else if (returnNameFull.equals(Fakultas.class.getName()))
				staticObj = tbmuser.ambilFakultas();
			else if (returnNameFull.equals(Sekolah.class.getName()))
				staticObj = tbmuser.ambilSekolah();
			else if (returnNameFull.equals(Yayasan.class.getName()))
				staticObj = tbmuser.ambilYayasan();
			else if (returnNameFull.equals(SatuanKerja.class.getName()))
				staticObj = tbmuser.ambilSatuanKerja();

			if (staticObj != null) {
				return "        <input type='hidden' value='" + staticObj.getId() + "' id='" + inputName + "_" + f + "_"
						+ randomID + "' name='" + f + "'>\n" + "<small class='fw-bold text-primary'>"
						+ staticObj.getNama() + "</small>";
			}
		}

		// --- STANDARD SEARCH DROPDOWN ---
		String colTable = kolomNama;
		if (returnNameFull.equals(Negara.class.getName())) {
			kolomNama = "namaNegara";
			colTable = "nama_negara";
		} else if (returnNameFull.equals(JenisKegiatan.class.getName())) {
			kolomNama = "namaKegiatan";
			colTable = "nama_kegiatan";
		} else if (returnNameFull.equals(Siswa.class.getName()) || returnNameFull.equals(CalonSiswa.class.getName())) {
			kolomNama = "namaSiswa";
			colTable = "nama_siswa";
		} else if (returnNameFull.equals(Tbmuser.class.getName())) {
			kolomNama = "userNama";
			colTable = "usernama";
		} else if (returnNameFull.equals(Tbmrole.class.getName())) {
			kolomNama = "roleName";
			colTable = "rolename";
		}

		String rand = Common.getGeneratedBarCode(5);
		boolean adaAktif = Common.isFieldExist(Class.forName(returnNameFull), "aktif");
		String activeClause = adaAktif ? " and (aktif=true or aktif is null) " : "";

		// Construct HTML & JS using StringBuilder for performance
		StringBuilder out = new StringBuilder();
		out.append("<div class='search-box' id='").append(f).append("Bandbox").append(rand).append("'>\n")
				.append("  <input ").append(required ? "required" : "").append(" type='text' value=\"").append(valName)
				.append("\" ").append("class='form-control search-input fuzzy-search' id='").append(inputName)
				.append("_nama_").append(f).append("_").append(randomID).append("' placeholder='-- ")
				.append(Common.getBahasaConfig("Pilih")).append(" ").append(c).append(" --' readonly>\n")
				.append("  <span class='fas fa-search search-box-icon'></span>")
				.append("  <input type='hidden' value=\"").append(valId).append("\" id='").append(inputName).append("_")
				.append(f).append("_").append(randomID).append("' name='").append(f).append("'>\n")
				.append("  <div class='bandbox-dropdown' id='dropdownContainer").append(rand).append("'>\n")
				.append("    <div class='search-container'>\n")
				.append("       <div class='input-group justify-content-center'>\n")
				.append("           <input type='text' class='form-input' id='searchInput").append(rand)
				.append("' placeholder='Cari ").append(c).append("...' autocomplete='off'>\n")
				.append("           <button id='btnSearch").append(rand)
				.append("' type='button' class='btn btn-outline-secondary'><span class='fas fa-search'></span></button>\n")
				.append("           <button id='btnReset").append(rand)
				.append("' type='button' class='btn btn-outline-secondary'><span class='fas fa-undo-alt'></span></button>\n")
				.append("       </div>\n").append("    </div>\n").append("    <ul class='result-list' id='resultList")
				.append(rand).append("'></ul>\n").append("    <div class='loading-text' id='loadingIndicator")
				.append(rand).append("'>").append(Common.getBahasaConfig("Memuat data")).append("...</div>\n")
				.append("  </div></div>");

		// Javascript Generation
		out.append("<script>\n").append(" (function() {\n") // Wrap in IIFE to avoid global scope pollution if possible
				.append("    const displayInput = document.getElementById('").append(inputName).append("_nama_")
				.append(f).append("_").append(randomID).append("');\n")
				.append("    const valueInput = document.getElementById('").append(inputName).append("_").append(f)
				.append("_").append(randomID).append("');\n")
				.append("    const dropdown = document.getElementById('dropdownContainer").append(rand).append("');\n")
				.append("    const searchInput = document.getElementById('searchInput").append(rand).append("');\n")
				.append("    const resultList = document.getElementById('resultList").append(rand).append("');\n")
				.append("    const loadingIndicator = document.getElementById('loadingIndicator").append(rand)
				.append("');\n")
				.append("    let currentPage = 1; const limit = 25; let currentSearch = ''; let isLoading = false; let hasMoreData = true;\n")
				.append("    let debounceTimer;\n")

				// Toggle Dropdown
				.append("    displayInput.addEventListener('click', () => {\n")
				.append("        if(!dropdown.classList.contains('active')) { dropdown.classList.add('active'); searchInput.focus(); if(resultList.children.length===0) loadData(true); }\n")
				.append("        else { dropdown.classList.remove('active'); }\n").append("    });\n")

				// Close on outside click
				.append("    document.addEventListener('click', (e) => { try { if (!document.getElementById('")
				.append(f).append("Bandbox").append(rand)
				.append("').contains(e.target)) dropdown.classList.remove('active'); } catch(e){} });\n")

				// Search Input
				.append("    searchInput.addEventListener('input', (e) => { clearTimeout(debounceTimer); debounceTimer = setTimeout(() => { currentSearch = e.target.value; loadData(true); }, 300); });\n")

				// Buttons
				.append("    document.getElementById('btnReset").append(rand)
				.append("').onclick = () => { valueInput.value=''; displayInput.value=''; ").append(onEvent)
				.append(" };\n").append("    document.getElementById('btnSearch").append(rand)
				.append("').onclick = () => { loadData(false); };\n")

				// Load Data Function
				.append("    async function loadData(reset=false) {\n")
				.append("        if(isLoading) return; isLoading=true; loadingIndicator.style.display='block';\n")
				.append("        if(reset) { currentPage=1; resultList.innerHTML=''; hasMoreData=true; }\n")
				.append("        const start = (currentPage-1)*limit;\n")
				.append("        const cleanSearch = currentSearch.replace(/[^a-zA-Z0-9 ]/g, \"\");\n")
				.append("        const req = JSON.stringify({ 'action':'daftar', 'class':'").append(returnNameFull)
				.append("', ").append("'projection':'").append(kolomId).append(";").append(kolomNama)
				.append("', 'deep':'1', 'max':limit, 'halaman':start, ").append("'where1':`").append(colTable)
				.append(" ilike '%${cleanSearch}%' ").append(whereTambahan).append(activeClause).append("`, ")
				.append("'order1':'asc', 'sort1':'").append(kolomNama).append("', 'order2':'asc', 'sort2':'")
				.append(kolomId).append("', 'count':'true' });\n").append("        const servletUrl = '")
				.append(Common.ROOT).append("/Data?datasearch='+encodeURIComponent(req);\n")

				.append("        try {\n")
				.append("           const response = await fetch(servletUrl); if(!response.ok) throw new Error(response.status);\n")
				.append("           const dataResponse = await response.json();\n")
				.append("           const hasMore = (start+limit) < dataResponse.count;\n")
				.append("           loadingIndicator.style.display='none'; isLoading=false;\n")

				.append("           if(dataResponse.data.length===0 && reset) { resultList.innerHTML='<li class=\"no-data\">")
				.append(Common.getBahasaConfig("Data tidak ditemukan")).append("</li>'; hasMoreData=false; return; }\n")

				.append("           dataResponse.data.forEach(prov => {\n")
				.append("               const li = document.createElement('li'); li.className='result-item'; li.textContent=prov.")
				.append(kolomNama).append(";\n")
				.append("               li.addEventListener('click', () => { displayInput.value=prov.")
				.append(kolomNama).append("; valueInput.value=prov.").append(kolomId)
				.append("; dropdown.classList.remove('active'); ").append(onEvent).append(" });\n")
				.append("               resultList.appendChild(li);\n").append("           });\n")
				.append("           hasMoreData = hasMore; if(hasMoreData) currentPage++;\n")

				.append("        } catch(err) { isLoading=false; loadingIndicator.style.display='none'; }\n")
				.append("    }\n")

				// Infinite Scroll
				.append("    resultList.addEventListener('scroll', () => { if(resultList.scrollTop + resultList.clientHeight >= resultList.scrollHeight - 10) { if(hasMoreData && !isLoading) loadData(false); } });\n")
				.append(" })();\n").append("</script>");

		return out.toString();
	}

	// --- LEGACY PILIHAN METHOD (Request Object) ---
	public static String pilihan(Object o, String f, String c, HttpServletRequest request, String returnNameFull,
			boolean required, String kolomId, String kolomNama, String whereTambahan) {

		GeneralValueObject oo = (o instanceof GeneralValueObject) ? (GeneralValueObject) o : null;
		String valName = (oo != null) ? oo.getNama() : "";
		String valId = (oo != null && oo.getId() != null) ? oo.getId().toString() : "";
		String rand = Common.getGeneratedBarCode();

		StringBuilder out = new StringBuilder();
		out.append("<div class=\"search-box\" id=\"").append(f).append("Bandbox").append(rand).append("\">\n")
				.append("  <input ").append(required ? "required" : "").append(" type=\"text\" value=\"")
				.append(valName).append("\" ").append("class=\"form-control search-input fuzzy-search\" id=\"input")
				.append(f).append("\" placeholder=\"-- Pilih ").append(c).append(" --\" readonly>\n")
				.append("  <span class=\"fas fa-search search-box-icon\"></span>")
				.append("  <input type=\"hidden\" value=\"").append(valId).append("\" id=\"").append(f)
				.append("_hidden_").append(rand).append("\" name=\"").append(f).append("\">\n")
				.append("  <div class=\"bandbox-dropdown\" id=\"dropdownContainer").append(rand).append("\">\n")
				.append("     <div class=\"search-container\"><input type=\"text\" class=\"search-input\" id=\"searchInput")
				.append(rand).append("\" placeholder=\"Cari ").append(c).append("...\" autocomplete=\"off\"></div>\n")
				.append("     <ul class=\"result-list\" id=\"resultList").append(rand).append("\"></ul>\n")
				.append("     <div class=\"loading-text\" id=\"loadingIndicator").append(rand)
				.append("\">Memuat data...</div>\n").append("  </div></div>");

		// Keep JS simple for this legacy method
		out.append("<script>\n").append("(function(){\n")
				.append("   const displayInput = document.getElementById('input").append(f).append("');\n")
				.append("   const valueInput = document.getElementById('").append(f).append("_hidden_").append(rand)
				.append("');\n").append("   const dropdown = document.getElementById('dropdownContainer").append(rand)
				.append("');\n").append("   const searchInput = document.getElementById('searchInput").append(rand)
				.append("');\n").append("   const resultList = document.getElementById('resultList").append(rand)
				.append("');\n")
				.append("   let currentPage=1; const limit=25; let currentSearch=''; let isLoading=false; let hasMoreData=true; let debounceTimer;\n")

				.append("   displayInput.onclick = () => { if(!dropdown.classList.contains('active')){ dropdown.classList.add('active'); searchInput.focus(); if(resultList.children.length===0) loadData(true); } else { dropdown.classList.remove('active'); }};\n")
				.append("   document.addEventListener('click', (e) => { try { if(!document.getElementById('").append(f)
				.append("Bandbox").append(rand)
				.append("').contains(e.target)) dropdown.classList.remove('active'); }catch(e){} });\n")
				.append("   searchInput.oninput = (e) => { clearTimeout(debounceTimer); debounceTimer = setTimeout(() => { currentSearch=e.target.value; loadData(true); }, 300); };\n")

				.append("   async function loadData(reset){ if(isLoading)return; isLoading=true; document.getElementById('loadingIndicator")
				.append(rand).append("').style.display='block';\n")
				.append("       if(reset){ currentPage=1; resultList.innerHTML=''; hasMoreData=true; }\n")
				.append("       const start=(currentPage-1)*limit;\n")
				.append("       const cleanSearch = currentSearch.replace(/[^a-zA-Z0-9 ]/g, \"\");\n")
				.append("       const req=JSON.stringify({'action':'daftar','class':'").append(returnNameFull)
				.append("','projection':'").append(kolomId).append(";").append(kolomNama)
				.append("','deep':'1','max':limit,'halaman':start,'where1':`").append(kolomNama)
				.append(" ilike '%${cleanSearch}%' ").append(whereTambahan).append("`,'count':'true'});\n")
				.append("       const response = await fetch('").append(request.getContextPath())
				.append("/Data?datasearch='+encodeURIComponent(req));\n")
				.append("       const json = await response.json();\n")
				.append("       document.getElementById('loadingIndicator").append(rand)
				.append("').style.display='none'; isLoading=false;\n")
				.append("       if(json.data.length===0 && reset) { resultList.innerHTML='<li class=\"no-data\">Data tidak ditemukan</li>'; return; }\n")
				.append("       json.data.forEach(p => { const li=document.createElement('li'); li.className='result-item'; li.textContent=p.")
				.append(kolomNama).append("; li.onclick=()=>{ displayInput.value=p.").append(kolomNama)
				.append("; valueInput.value=p.").append(kolomId)
				.append("; dropdown.classList.remove('active'); }; resultList.appendChild(li); });\n")
				.append("       hasMoreData = (start+limit) < json.count; if(hasMoreData) currentPage++;\n")
				.append("   }\n")
				.append("   resultList.onscroll = () => { if(resultList.scrollTop+resultList.clientHeight >= resultList.scrollHeight-10 && hasMoreData && !isLoading) loadData(false); };\n")
				.append("})();\n").append("</script>");

		return out.toString();
	}

	// --- MODIF METHOD ---
	@SuppressWarnings({ "rawtypes" })
	public static String modif(Class clazz, String[] fields, String[] columns, String[] tidakBolehSama, String judul,
			Set<String> paramRequired, HttpServletRequest request, HttpServletResponse response) {

		StringBuilder out = new StringBuilder();

		try {
			String iddata = request.getParameter("iddata") == null ? "" : request.getParameter("iddata").trim();
			String simpan = request.getParameter("simpan") == null ? "" : request.getParameter("simpan").trim();
			String view = request.getParameter("view") == null ? "" : request.getParameter("view").trim();
			String p = request.getParameter("p") == null ? "" : request.getParameter("p").trim();

			Session mySession = null;
			GeneralValueObject generalValueObject = null;
			String warning = "";

			try {
				mySession = HibernateUtil.currentNativeSession();
				ClassMetadata classMetadata = HibernateUtil.getClassMetadata(clazz);

				if (iddata.isEmpty()) {
					generalValueObject = (GeneralValueObject) clazz.newInstance();
				} else {
					generalValueObject = (GeneralValueObject) mySession.createCriteria(clazz)
							.add(Restrictions.idEq(Long.parseLong(iddata))).uniqueResult();
				}

				if (simpan.equalsIgnoreCase("true") && view.trim().isEmpty()) {
					int count = 0;
					for (String tidakBoleh : tidakBolehSama) {
						String s = request.getParameter(tidakBoleh);
						if (s != null && !s.trim().isEmpty()) {
							Criteria crit = mySession.createCriteria(clazz).setProjection(Projections.rowCount())
									.add(Restrictions.ilike(tidakBoleh, s.trim(), MatchMode.EXACT));
							if (generalValueObject.getId() != null) {
								crit.add(Restrictions.ne("id", generalValueObject.getId()));
							}
							count = ((Number) crit.uniqueResult()).intValue();
							if (count > 0) {
								warning = Common.getBahasaConfig(tidakBoleh) + " "
										+ Common.getBahasaConfig("sudah ada di database");
								break;
							}
						}
					}

					if (count == 0) {
						Common.setObjectValues(classMetadata, generalValueObject, fields, 0, request);
						mySession.getTransaction().begin();
						Common.refreshSaveOrUpdate(mySession, generalValueObject);
						mySession.getTransaction().commit();
						view = "true";
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/JSPCurdGenerator.java:1034");
			} finally {
				if (mySession != null) {
					try {
						// mySession.disconnect();
						if (mySession.isOpen()) {mySession.disconnect();mySession.close();}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/JSPCurdGenerator.java:1040");
					}
				}
				HibernateUtil.closeSession();
			}

			// --- HTML GENERATION FOR MODIF ---
			out.append(
					"<div class='card mb-3'><div class='card-body position-relative'><div class='row'><div class='col-lg-12'>")
					.append("<h3>").append(Common.getBahasaConfig(judul)).append("</h3>");

			if (!warning.isEmpty()) {
				out.append("<div class='alert alert-danger border-0 d-flex align-items-center' role='alert'>").append(
						"<div class='bg-danger me-3 icon-item'><span class='fas fa-times-circle text-white fs-6'></span></div>")
						.append("<p class='mb-0 flex-1'>").append(warning).append("</p>")
						.append("<button class='btn-close' type='button' data-bs-dismiss='alert' aria-label='Close'></button></div>");
			}

			out.append("<form class='row g-3 needs-validation' novalidate action='").append(request.getContextPath())
					.append("/baru?p=").append(p).append("&s=modif' method='post'>")
					.append("<input type='hidden' name='iddata' value='")
					.append(generalValueObject != null && generalValueObject.getId() != null
							? generalValueObject.getId()
							: "")
					.append("'/>").append("<input type='hidden' name='simpan' value='true'/>");

			ClassMetadata classMetadata = HibernateUtil.getClassMetadata(clazz); // re-fetch metadata (safe mostly
																					// static)

			for (int i = 0; i < columns.length; i++) {
				String c = columns[i];
				String f = fields[i];
				boolean required = paramRequired.contains(f);

				Object o = null;
				String returnName = "";
				String returnNameFull = "";
				try {
					returnName = classMetadata.getPropertyType(f).getReturnedClass().getSimpleName();
					returnNameFull = classMetadata.getPropertyType(f).getReturnedClass().getName();
					o = classMetadata.getPropertyValue(generalValueObject, f, EntityMode.POJO);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/JSPCurdGenerator.java:1081");
				}

				out.append("<div class='col-md-4'><label class='form-label' for='input").append(f).append("'>")
						.append(Common.getBahasaConfig(c)).append("</label>");

				if ("true".equalsIgnoreCase(view)) {
					String displayVal = (o == null) ? "" : o.toString();
					if ("Boolean".equals(returnName))
						displayVal = (o != null && o.equals(true)) ? "Ya" : "Tidak";
					else if ("Date".equals(returnName) && o != null)
						displayVal = Common.dateFormat51.get().format(o);
					else if (o instanceof GeneralValueObject)
						displayVal = ((GeneralValueObject) o).getNama();
					else if (o instanceof Number)
						displayVal = Common.numberFormat.get().format(o);

					out.append("<input class='form-control-plaintext outline-none' type='text' readonly value=\"")
							.append(displayVal).append("\" />");
				} else {
					// EDIT MODE
					if ("String".equals(returnName)) {
						if (f.toLowerCase().contains("keterangan")) {
							out.append("<textarea class='form-control' id='input").append(f).append("' name='")
									.append(f).append("' rows='3'>").append(o == null ? "" : o).append("</textarea>");
						} else {
							out.append("<input class='form-control' name='").append(f).append("' id='input").append(f)
									.append("' type='text' ").append(required ? "required" : "").append(" value=\"")
									.append(o == null ? "" : o).append("\" />");
						}
					} else if ("Date".equals(returnName)) {
						out.append("<input name='").append(f).append("' class='form-control datetimepicker' ")
								.append(required ? "required" : "").append(" value=\"")
								.append(o == null ? "" : Common.dateFormat53.get().format(o))
								.append("\" type='text' data-options='{\"enableTime\":true,\"dateFormat\":\"d/m/y H:i\",\"disableMobile\":true}' />");
					} else if ("Boolean".equals(returnName)) {
						out.append("<div class='form-check form-switch'><input name='").append(f)
								.append("' class='form-check-input' type='checkbox' ")
								.append(o != null && o.equals(true) ? "checked" : "")
								.append(" /><label class='form-check-label'>")
								.append(Common.getBahasaConfig("Data ini aktif")).append("</label></div>");
					} else if ("Double".equals(returnName) || "Integer".equals(returnName)
							|| "Long".equals(returnName)) {
						out.append("<input class='form-control' name='").append(f).append("' type='number' ")
								.append(required ? "required" : "").append(" value=\"").append(o == null ? "" : o)
								.append("\" />");
					} else {
						// Foreign Key
						out.append(pilihan(o, f, c, request, returnNameFull, required, "id", "nama", ""));
					}
				}
				out.append("</div>");
			}

			// Buttons
			out.append("<div class='col-12'><div class='btn-group' role='group'>");
			if ("true".equalsIgnoreCase(view)) {
				out.append("<button class='btn btn-primary' onclick=\"window.location.href='")
						.append(request.getContextPath()).append("/baru?p=").append(p).append("'; return false;\">")
						.append(Common.getBahasaConfig("Kembali")).append("</button>")
						.append("<button class='btn btn-warning' onclick=\"window.location.href='")
						.append(request.getContextPath()).append("/baru?p=").append(p).append("&s=modif&iddata=")
						.append(generalValueObject.getId()).append("'; return false;\">")
						.append(Common.getBahasaConfig("Ubah")).append("</button>");
			} else {
				out.append("<button class='btn btn-primary' type='submit'>").append(Common.getBahasaConfig("Simpan"))
						.append("</button>").append("<button class='btn btn-warning' onclick=\"window.location.href='")
						.append(request.getContextPath()).append("/baru?p=").append(p).append("'; return false;\">")
						.append(Common.getBahasaConfig("Batal")).append("</button>");
			}
			out.append("</div></div></form></div></div></div></div>");

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/JSPCurdGenerator.java:1154");
		}
		return out.toString();
	}

	// --- INDEX METHOD ---
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String index(Class clazz, String[] fields, String[] columns, String[] pencarian, String judul,
			HttpServletRequest request, HttpServletResponse response) {

		StringBuilder out = new StringBuilder();

		try {
			String iddata = request.getParameter("iddata") == null ? "" : request.getParameter("iddata").trim();
			String hapus = request.getParameter("hapus") == null ? "" : request.getParameter("hapus").trim();
			String search = request.getParameter("search") == null ? "" : request.getParameter("search").trim();
			String p = request.getParameter("p") == null ? "" : request.getParameter("p").trim();

			Session mySession = null;
			List<GeneralValueObject> generalValueObjects = null;
			boolean berhasilHapus = false;
			ClassMetadata classMetadata = null;

			try {
				mySession = HibernateUtil.currentNativeSession();
				classMetadata = HibernateUtil.getClassMetadata(clazz);

				if ("true".equalsIgnoreCase(hapus) && !iddata.isEmpty()) {
					try {
						GeneralValueObject obj = (GeneralValueObject) mySession.get(clazz, Long.parseLong(iddata));
						if (obj != null) {
							mySession.getTransaction().begin();
							Common.refreshDelete(mySession, obj);
							mySession.getTransaction().commit();
							berhasilHapus = true;
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/JSPCurdGenerator.java:1191");
					}
				}

				Criteria criteria = mySession.createCriteria(clazz).addOrder(Order.asc("nama"));
				if (!search.isEmpty()) {
					Criterion criterion = Restrictions.sqlRestriction("1=0"); // False default
					for (String s : pencarian) {
						criterion = Restrictions.or(criterion, Restrictions.ilike(s, search, MatchMode.ANYWHERE));
					}
					criteria.add(criterion);
				}
				generalValueObjects = ConstantValues.simpleList(criteria, clazz);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/JSPCurdGenerator.java:1205");
			} finally {
				if (mySession != null) {
					try {
						// mySession.disconnect();
						if (mySession.isOpen()) {mySession.disconnect();mySession.close();}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/JSPCurdGenerator.java:1211");
					}
				}
				HibernateUtil.closeSession();
			}

			out.append(
					"<div class='card mb-3'><div class='card-body position-relative'><div class='row'><div class='col-lg-12'>")
					.append("<h3>").append(Common.getBahasaConfig(judul)).append("</h3>");

			if (berhasilHapus) {
				out.append("<div class='alert alert-success border-0 d-flex align-items-center' role='alert'>").append(
						"<div class='bg-success me-3 icon-item'><span class='fas fa-check-circle text-white fs-6'></span></div>")
						.append("<p class='mb-0 flex-1'>").append(Common.getBahasaConfig("Data berhasil terhapus"))
						.append("</p>")
						.append("<button class='btn-close' type='button' data-bs-dismiss='alert'></button></div>");
			}

			// Search Bar & Add Button
			out.append("<div class='row justify-content-end px-3'><div class='col-auto col-sm-5 mb-3'>")
					.append("<form action='").append(request.getContextPath()).append("/baru?")
					.append(request.getQueryString()).append("' method='post'>").append("<div class='input-group'>")
					.append("<input class='form-control form-control-sm shadow-none search' type='search' value=\"")
					.append(search).append("\" name='search' placeholder='Cari...' />")
					.append("<div class='input-group-text bg-transparent'><span class='fa fa-search fs-10 text-600'></span></div>")
					.append("<button class='btn btn-falcon-default' onclick=\"window.location.href='")
					.append(request.getContextPath()).append("/baru?p=").append(p).append("&s=modif'; return false;\">")
					.append(Common.getBahasaConfig("Tambah Baru")).append("</button>")
					.append("</div></form></div></div>");

			// Table
			out.append(
					"<div class='table-responsive scrollbar'><table class='table table-hover table-striped overflow-hidden'><thead><tr>");
			for (String col : columns)
				out.append("<th scope='col'>").append(Common.getBahasaConfig(col)).append("</th>");
			out.append("<th class='text-end'></th></tr></thead><tbody>");

			if (generalValueObjects != null) {
				for (GeneralValueObject obj : generalValueObjects) {
					out.append("<tr class='align-middle'>");
					for (String f : fields) {
						Object o = null;
						String type = "";
						try {
							o = classMetadata.getPropertyValue(obj, f, EntityMode.POJO);
							type = classMetadata.getPropertyType(f).getReturnedClass().getSimpleName();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/JSPCurdGenerator.java:1258");
						}

						out.append("<td class='text-nowrap'>");
						if (o != null) {
							if ("Date".equals(type))
								out.append(Common.dateFormat51.get().format(o));
							else if ("Boolean".equals(type))
								out.append(o.equals(true) ? "Ya" : "Tidak");
							else if (o instanceof GeneralValueObject)
								out.append(((GeneralValueObject) o).getNama());
							else if (o instanceof Number)
								out.append(Common.numberFormat.get().format(o));
							else
								out.append(o);
						}
						out.append("</td>");
					}

					// Action Dropdown
					out.append("<td class='text-end'><div class='dropdown font-sans-serif position-static'>").append(
							"<button class='btn btn-link text-600 btn-sm dropdown-toggle btn-reveal' type='button' title='" + Common.getBahasaConfig("Aksi") + "' aria-label='" + Common.getBahasaConfig("Aksi") + "' data-bs-toggle='dropdown'><span class='fas fa-ellipsis-h fs-10'></span></button>")
							.append("<div class='dropdown-menu dropdown-menu-end border py-0'><div class='py-2'>")
							.append("<a class='dropdown-item' href='").append(request.getContextPath())
							.append("/baru?p=").append(p).append("&s=modif&view=true&iddata=").append(obj.getId())
							.append("'>").append(Common.getBahasaConfig("Lihat")).append("</a>")
							.append("<a class='dropdown-item text-danger' onclick=\"return confirm('")
							.append(Common.getBahasaConfig("Apakah Anda yakin ingin menghapus data ini ?"))
							.append("');\" href='").append(request.getContextPath()).append("/baru?p=").append(p)
							.append("&hapus=true&iddata=").append(obj.getId()).append("'>")
							.append(Common.getBahasaConfig("Hapus")).append("</a>")
							.append("</div></div></div></td></tr>");
				}
			}
			out.append("</tbody></table></div></div></div></div></div>");

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/JSPCurdGenerator.java:1295");
		}
		return out.toString();
	}
}