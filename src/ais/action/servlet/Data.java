package ais.action.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Paging;

import ais.action.master.TampilanELearningAction;
import ais.action.master.helper.RekapitulasiPerkuliahanHelper;
import ais.action.master.helper.generic.AmbilDataLampiranFileLain;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.Report;
import ais.action.servlet.api.DaftarDataService;
import ais.action.servlet.api.ElearningApiUtil;
import ais.action.servlet.api.KantinHelper;
import ais.action.servlet.api.LinimasaApi;
import ais.action.servlet.api.TopupHelper;
import ais.common.Common;
import ais.common.ExcelExporter;
import ais.common.JsonUtil;
import ais.common.PdfGenerator;
import ais.common.WordExporter;
import ais.common.gdrive.GDriveUtilPerPengguna;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GDriveCode;
import ais.database.model.GeneralValueObject;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoAdmin;
import ais.database.model.file.LampiranLain;

/**
 * Komponen batas HTTP/servlet untuk data. Tipe ini menerima input dari luar aplikasi,
 * meneruskannya ke layanan domain, lalu membentuk respons tanpa menduplikasi aturan bisnis.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * HttpServlet}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code doGet()}, {@code ambil()}, {@code
 * processCari()}, {@code processDownload()}); validasi/perhitungan ({@code checkGDriveConnectionInternal()});
 * mutasi data ({@code simpanBatchDataRinci()}, {@code updateFile()}); penghapusan/pembatalan ({@code
 * hapusFile()}, {@code hapusFileById()}); pelaporan/ekspor ({@code renderFileDirectly()}); operasi domain lain
 * ({@code rollbackQuietly()}, {@code doPost()}, {@code processRequest()}, {@code pushToDriveInternal()}, {@code
 * sendJsonStreaming()}, {@code processFile()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see HttpServlet
 */
public class Data extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public Data() {
		super();
	}

	/** Rollback transaksi aktif pada session lokal tanpa melempar exception. */
	private static void rollbackQuietly(Session session) {
		try {
			if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Data.java:67");
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	private void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setHeader("Access-Control-Allow-Origin", "*");

		String actionParam = request.getParameter("action");
		String renderParam = request.getParameter("render");

		if ("checkGDriveConnection".equals(actionParam)) {
			response.setContentType("application/json; charset=UTF-8");
			sendJsonStreaming(response, checkGDriveConnectionInternal(request));
			return;
		}

		if ("pushToDrive".equals(actionParam)) {
			response.setContentType("application/json; charset=UTF-8");
			sendJsonStreaming(response, pushToDriveInternal(request));
			return;
		}

		if ("file".equals(actionParam) && "true".equals(renderParam)) {
			renderFileDirectly(request, response);
			return;
		}

		response.setContentType("application/json; charset=UTF-8");
		JSONObject resultJson = Data.ambil(request, response);
		if (resultJson == null) {
			resultJson = new JSONObject();
			try {
				resultJson.put("status", "99");
				resultJson.put("description", "Tidak ada respon server");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Data.java:113");
			}
		}
		sendJsonStreaming(response, resultJson);
	}

	private JSONObject checkGDriveConnectionInternal(HttpServletRequest request) {
		JSONObject res = new JSONObject();
		boolean isConnected = false;

		try {
			res.put("status", "Sukses");
			Tbmuser user = Common.getCurrentUser(request);

			if (user != null && user.getUserId() != null) {
				String username = user.getUserId();
				Session session = null;
				try {
					session = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();

					if (ais.common.GoogleCommon.codes.containsKey(username)) {
						String kode = ais.common.GoogleCommon.codes.get(username);
						GDriveCode gdriveCode = (GDriveCode) session.createCriteria(GDriveCode.class)
								.add(Restrictions.eq("nama", username)).setMaxResults(1).uniqueResult();
						if (gdriveCode == null)
							gdriveCode = new GDriveCode();

						gdriveCode.setNama(username);
						gdriveCode.setKeterangan(kode.trim());

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, gdriveCode);
						session.getTransaction().commit();

						ais.common.GoogleCommon.codes.remove(username);
					}

					GDriveCode gc = (GDriveCode) session.createCriteria(GDriveCode.class)
							.add(Restrictions.eq("nama", username)).setMaxResults(1).uniqueResult();
					if (gc != null && gc.getKeterangan() != null && !gc.getKeterangan().trim().isEmpty())
						isConnected = true;
				} catch (Exception e) {
					if (session != null && session.getTransaction().isActive())
						session.getTransaction().rollback();
				} finally {
					if (session != null && session.isOpen()) {
						session.clear();
						session.close();
					}
				}
			}
			res.put("connected", isConnected);
		} catch (JSONException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Data.java:165");
		}
		return res;
	}

	private JSONObject pushToDriveInternal(HttpServletRequest request) {
		JSONObject res = new JSONObject();
		try {
			res.put("status", "Gagal");
			final Tbmuser user = Common.getCurrentUser(request);
			String idStr = request.getParameter("id");
			String clazzStr = request.getParameter("clazz");

			if (user != null && idStr != null && clazzStr != null) {
				final Long finalId = Long.parseLong(idStr);
				@SuppressWarnings("rawtypes")
				final Class finalC = Class.forName(clazzStr);
				final PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);

				Session s2 = null;
				try {
					s2 = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
					FileFotoLain fFoto = (FileFotoLain) s2.get(finalC, finalId);

					// Ambil dan hapus referensi dari RAM Map
					File extractedFile = DoUpload.filesPending.remove(finalId.toString());

					if (extractedFile != null && extractedFile.exists()) {
						final File fileTempFisik = extractedFile;
						GDriveUtilPerPengguna driveUtil = new GDriveUtilPerPengguna(user);

						// EKSEKUSI LANGSUNG (SINKRONUS): Menunggu file selesai diupload
						com.google.api.services.drive.model.File fUp = driveUtil.kirimBackupLangsung(null,
								fileTempFisik, pt, finalC.getSimpleName(), null);

						if (fUp != null && fUp.getId() != null) {
							s2.refresh(fFoto);
							fFoto.setFoto(null);
							fFoto.setGdrive(fUp.getId());
							fFoto.setGdriveUsername(user.getUserId());

							s2.getTransaction().begin();
							s2.update(fFoto);
							s2.getTransaction().commit();

							if (fileTempFisik.exists()) {
								fileTempFisik.delete();
							}

							// SUSUN DATA BALIKAN UNTUK FUNGSI PREVIEW DI UI
							JSONObject objectData = new JSONObject();
							objectData.put("gdrive", fFoto.getGdrive());
							objectData.put("url", fFoto.createLinkUri(false));
							objectData.put("nama", fFoto.getNama());
							objectData.put("id", fFoto.getId());
							objectData.put("mime", fFoto.getKeterangan());

							res.put("data", objectData);
							res.put("status", "Sukses");
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Data.java:227");
				} finally {
					if (s2 != null && s2.isOpen()) {
						s2.clear();
						s2.close();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Data.java:236");
		}
		return res;
	}

	private void renderFileDirectly(HttpServletRequest request, HttpServletResponse response) {
		FileInputStream fileInputStream = null;
		OutputStream out = null;
		try {
			Class<?> clazz = Class.forName((request.getParameter("class") + "").trim());
			Serializable ref = clazz.getName().equalsIgnoreCase(FotoAdmin.class.getName()) ? request.getParameter("ref")
					: Long.parseLong((request.getParameter("ref") + "").trim());
			String jenis = (request.getParameter("jenis") + "").trim();
			boolean usingId = request.getParameter("usingId") != null
					&& Boolean.parseBoolean(request.getParameter("usingId").trim());
			boolean refresh = request.getParameter("refresh") != null
					&& Boolean.parseBoolean(request.getParameter("refresh").trim());
			String kondisiTambahan = request.getParameter("kondisiTambahan") != null
					? request.getParameter("kondisiTambahan").trim()
					: "";

			FileFotoLain fileFotoLain = FileFotoLain.ambil(usingId, ref, jenis, 0, clazz, refresh, kondisiTambahan);
			File file = (fileFotoLain != null) ? fileFotoLain.ambilFile() : null;
			if (file == null || !file.exists())
				file = new File(getServletContext().getRealPath("/img/administrator-icon_default.png"));

			String filename = file.getName();
			String mimeType = getServletContext().getMimeType(filename);
			if (mimeType == null)
				mimeType = filename.toLowerCase().endsWith("png") ? "image/png"
						: (filename.toLowerCase().endsWith("gif") ? "image/gif" : "image/jpeg");

			response.setContentType(mimeType);
			response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
			response.setContentLength((int) file.length());

			fileInputStream = new FileInputStream(file);
			out = response.getOutputStream();
			byte[] buffer = new byte[8192];
			int bytesRead;
			while ((bytesRead = fileInputStream.read(buffer)) != -1)
				out.write(buffer, 0, bytesRead);
			out.flush();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Data.java:280");
		} finally {
			if (fileInputStream != null)
				try {
					fileInputStream.close();
				} catch (IOException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Data.java:285");
				}
			if (out != null)
				try {
					out.close();
				} catch (IOException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Data.java:290");
				}
		}
	}

	private void sendJsonStreaming(HttpServletResponse response, JSONObject jsonObject) {
		PrintWriter writer = null;
		try {
			writer = response.getWriter();
			writer.write(jsonObject.toString());
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Data.java:302");
		} finally {
			if (writer != null)
				writer.close();
		}
	}

	/**
	 * Menyimpan banyak baris sekaligus (unggah/import Excel) dengan MEMAKAI ULANG logika simpan
	 * satuan {@link ElearningApiUtil#simpanDataRinci} untuk setiap baris — pola server-side seperti
	 * upload pada master ZK (AgamaAction). Payload: {@code {class:"...", dataBatch:[{...}, {...}]}};
	 * tiap elemen dataBatch berisi properti=nilai (boleh memuat {@code id} untuk update). Dipakai oleh
	 * aksi {@code simpanBatchProduk} &amp; {@code simpanBatchDataRinci}. Mengembalikan status "00" bila
	 * minimal satu baris tersimpan, beserta ringkasan jumlah berhasil/gagal.
	 */
	private static JSONObject simpanBatchDataRinci(HttpServletRequest request, JSONObject jsonObject) {
		JSONObject hasil = new JSONObject();
		int sukses = 0;
		int gagal = 0;
		StringBuilder err = new StringBuilder();
		try {
			String clazz = jsonObject.optString("class", "");
			JSONArray dataBatch = jsonObject.isNull("dataBatch") ? new JSONArray()
					: jsonObject.getJSONArray("dataBatch");
			for (int i = 0; i < dataBatch.length(); i++) {
				try {
					JSONObject row = dataBatch.getJSONObject(i);
					JSONObject perRow = new JSONObject();
					perRow.put("class", clazz);
					perRow.put("data", row);
					JSONObject res = ElearningApiUtil.simpanDataRinci(request, perRow, false);
					String st = res == null ? "" : res.optString("status", "");
					if ("00".equals(st) || "success".equalsIgnoreCase(st)) {
						sukses++;
					} else {
						gagal++;
						if (err.length() < 400 && res != null) {
							err.append(res.optString("description", "")).append("; ");
						}
					}
				} catch (Exception exRow) {
					gagal++;
				}
			}
			hasil.put("status", sukses > 0 ? "00" : "90");
			hasil.put("data", sukses);
			hasil.put("description",
					"Berhasil " + sukses + ", gagal " + gagal + (err.length() > 0 ? (". " + err.toString()) : ""));
		} catch (Exception e) {
			try {
				hasil.put("status", "90");
				hasil.put("description", "Error: " + e.getMessage());
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/servlet/Data.java:354");
			}
		}
		return hasil;
	}

	public static JSONObject ambil(HttpServletRequest request, HttpServletResponse response) {
		JSONObject hasil = new JSONObject();
		String log = "";
		BufferedReader reader = null;
		try {
			hasil.put("status", "99");
			hasil.put("description", "");
			String data = null;
			StringBuilder buffer = new StringBuilder();

			try {
				reader = request.getReader();
				String line;
				while ((line = reader.readLine()) != null) {
					buffer.append(line);
				}
				data = buffer.toString();
			} catch (Exception e) {
				System.err.println("Gagal membaca request reader: " + e.getMessage());
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Data.java:384");
					}
				}
			}

			String datasearch = (data != null && !data.trim().isEmpty()) ? data : request.getParameter("datasearch");
			if (datasearch == null || datasearch.trim().isEmpty()) {
				hasil.put("description", "Data search kosong");
				return hasil;
			}

			JSONObject jsonObject = new JSONObject(datasearch.trim());
			log = jsonObject.optString("log", "");
			if ("true".equalsIgnoreCase(log)) {
				System.out.println("request -> " + datasearch);
			}
			log = jsonObject.optString("log_response", "");
			Tbmuser tbmuser = Common.getCurrentUser(request);
			String tanpaLogin = jsonObject.optString("tanpaLogin", "");
			String action = jsonObject.optString("action", "");
			boolean belumLogin = tbmuser == null || tbmuser.getUserId() == null;

			// Aksi yang menjalankan SQL TULIS bebas dari klien TIDAK PERNAH boleh anonim.
			//
			// "tanpaLogin" adalah penanda yang dikirim halaman itu sendiri, jadi pemanggil mana
			// pun dapat menyetelnya -- termasuk yang tidak pernah membuka halaman kita. Untuk
			// aksi baca, penanda itu memang dipakai halaman publik yang sah (landing page,
			// pendaftaran calon anggota, toko online). Untuk aksi TULIS tidak ada satu pun
			// halaman yang memakainya (diperiksa atas seluruh JSP), sehingga menutupnya tidak
			// memutus fungsi apa pun -- sementara membiarkannya berarti siapa saja yang dapat
			// menjangkau endpoint ini bisa menjalankan UPDATE/DELETE tanpa pernah masuk.
			//
			// Ini pertahanan LAPIS PERTAMA yang tidak bergantung pada konfigurasi; lapis
			// keduanya, SqlSecurityGuard, hanya berlaku bila pemilik menyalakannya (dok. 70).
			boolean aksiSqlTulis = "update_data".equals(action) || "update_file_data".equals(action);
			if (aksiSqlTulis && belumLogin) {
				JSONObject jerr = new JSONObject();
				jerr.put("status", "90");
				jerr.put("description", "Perintah tulis memerlukan pengguna yang sudah masuk.");
				return jerr;
			}

			if (tanpaLogin.trim().isEmpty() || !tanpaLogin.equalsIgnoreCase("true")) {
				if (belumLogin) {
					JSONObject jerr = new JSONObject();
					jerr.put("status", "90");
					jerr.put("description", "Pengguna tidak boleh akses");
					return jerr;
				}
			}

			if ("file".equals(action)) {
				processFile(jsonObject, hasil);
			} else if ("load".equals(action)) {
				hasil = DaftarDataService.load(request, jsonObject, false);
			} else if ("update_file".equals(action)) {
				updateFile(jsonObject, hasil);
			} else if ("hapus_file".equals(action)) {
				hapusFile(jsonObject, hasil);
			} else if ("hapus_file_by_id".equals(action)) {
				hapusFileById(jsonObject, hasil);
			} else if ("cari".equals(action)) {
				processCari(jsonObject, hasil);
			} else if ("daftar".equals(action)) {
				hasil = DaftarDataService.daftar(request, jsonObject, false);
			} else if ("sql".equals(action)) {
				// Lapis pertahanan: action=sql wajib read-only (lihat ais.common.SqlSecurityGuard;
				// dikendalikan konfigurasi mode_proteksi_sql_endpoint, default off=tanpa efek).
				ais.common.SqlSecurityGuard.Result penjaga = ais.common.SqlSecurityGuard
						.checkReadSql(jsonObject.optString("sql", ""));
				if (!penjaga.allowed) {
					hasil.put("status", "90");
					hasil.put("description", penjaga.reason);
					return hasil;
				}
				JSONArray dataHasil = DaftarDataService.sql(jsonObject);
				hasil.put("data", dataHasil);
				hasil.put("status", "00");
			} else if ("update_data".equals(action)) {
				String sql = jsonObject.optString("sql", "");
				ais.common.SqlSecurityGuard.Result penjaga = ais.common.SqlSecurityGuard.checkWriteSql(sql);
				if (!penjaga.allowed) {
					hasil.put("status", "90");
					hasil.put("description", penjaga.reason);
					return hasil;
				}
				int hasildata = Common.updateSql(sql);
				hasil.put("data", hasildata);
				hasil.put("status", "00");
			} else if ("update_file_data".equals(action)) {
				String sql = jsonObject.optString("sql", "");
				ais.common.SqlSecurityGuard.Result penjaga = ais.common.SqlSecurityGuard.checkWriteSql(sql);
				if (!penjaga.allowed) {
					hasil.put("status", "90");
					hasil.put("description", penjaga.reason);
					return hasil;
				}
				int hasildata = Common.updateSqlStreaming(sql);
				hasil.put("data", hasildata);
				hasil.put("status", "00");
			} else if ("simpanDataRinci".equals(action)) {
				hasil = ElearningApiUtil.simpanDataRinci(request, jsonObject, false);
			} else if ("simpanBatchProduk".equals(action) || "simpanBatchDataRinci".equals(action)) {
				hasil = simpanBatchDataRinci(request, jsonObject);
			} else if ("hapusDataRinci".equals(action)) {
				hasil = ElearningApiUtil.hapusDataRinci(request, jsonObject, false);
			} else if ("downloadExcel".equals(action) || "downloadPdf".equals(action)
					|| "downloadDocx".equals(action)) {
				processDownload(request, jsonObject, action, hasil);
			} else if ("linimasa".equals(action)) {
				processLinimasa(request, tbmuser, jsonObject, hasil);
			} else if ("ringkasan".equals(action)) {
				processRingkasan(tbmuser, jsonObject, hasil);
			} else if ("ujian".equals(action) || "tugas".equals(action) || "materi".equals(action)
					|| "audio".equals(action) || "video".equals(action) || "tugas_kelompok".equals(action)) {
				processGenericLinimasaItem(request, tbmuser, jsonObject, action, hasil);
			} else if ("bayar".equals(action)) {
				KantinHelper.bayar(tbmuser, jsonObject, hasil);
			} else if (action != null && action.startsWith("grup_produk_")) {
				// Grup Produk (harga terpusat lintas toko) -- handler self-guard menu key +
				// aksi CRUD granular, lihat GrupProdukApiHelper.
				ais.action.servlet.api.GrupProdukApiHelper.proses(action, tbmuser, jsonObject, hasil);
			} else if (action != null && (action.startsWith("toko_kelola_") || "unit_usaha_katalog".equals(action))) {
				// CRUD Toko + katalog unit usaha (JSP memakai aksi yang sama dgn
				// Desktop/Android) -- admin-only, self-guarded di TokoApiHelper.
				ais.action.servlet.api.TokoApiHelper.proses(action, tbmuser, jsonObject, hasil);
			} else if ("pos_demo_status".equals(action)) {
				// Generator data contoh utk JSP/ZK -- tiga gerbang self-guarded di
				// helper (konfigurasi data_sample_ebisnis + admin + toko_demo).
				ais.action.servlet.api.PosDemoProvisionHelper.status(tbmuser, jsonObject, hasil);
			} else if ("pos_demo_seed_products_unit_usaha".equals(action)) {
				ais.action.servlet.api.PosDemoProvisionHelper.mulaiProdukUnitUsaha(tbmuser, jsonObject, hasil);
			} else if ("draft_bayar".equals(action)) {
				KantinHelper.draft_bayar(tbmuser, jsonObject, hasil);
			} else if ("checkBayar".equals(action)) {
				KantinHelper.checkBayar(jsonObject, hasil);
			} else if ("mutasi_stok_simpan".equals(action)) {
				KantinHelper.mutasiStokSimpan(tbmuser, jsonObject, hasil);
			} else if ("mutasi_stok_list".equals(action)) {
				KantinHelper.mutasiStokList(tbmuser, jsonObject, hasil);
			} else if (action != null && action.startsWith("anggaran_")) {
				// Anggaran/RAB bulanan -- helper yang SAMA dipakai PosApi (Desktop/Android),
				// jadi angka dan aturan bisnisnya satu sumber untuk ketiga kanal.
				ais.action.servlet.api.AnggaranApiHelper.proses(action, tbmuser, jsonObject, hasil);
			} else if (action != null && action.startsWith("pengadaan_")) {
				// Modul Pengadaan POS -- helper yang SAMA dipakai PosApi (Desktop/Android),
				// jadi aturan bisnisnya satu sumber. Helper self-guard kunci menu pengadaan_pr.
				ais.action.servlet.api.PengadaanPosApiHelper.proses(action, tbmuser, jsonObject, hasil);
			} else if ("kulakan_faktur_simpan".equals(action)) {
				KantinHelper.kulakanFakturSimpan(tbmuser, jsonObject, hasil);
			} else if ("kedaluwarsa_list".equals(action)) {
				KantinHelper.kedaluwarsaList(tbmuser, jsonObject, hasil);
			} else if ("produk_batch_simpan".equals(action)) {
				KantinHelper.produkBatchSimpan(tbmuser, jsonObject, hasil);
			} else if ("produk_batch_produk_list".equals(action)) {
				KantinHelper.produkBatchProdukList(tbmuser, jsonObject, hasil);
			} else if ("kulakan_faktur_list".equals(action)) {
				KantinHelper.kulakanFakturList(tbmuser, jsonObject, hasil);
			} else if ("kulakan_faktur_detail".equals(action)) {
				KantinHelper.kulakanFakturDetail(tbmuser, jsonObject, hasil);
			} else if ("penyedia_list".equals(action)) {
				KantinHelper.penyediaList(tbmuser, jsonObject, hasil);
			} else if ("penyedia_simpan".equals(action)) {
				KantinHelper.penyediaSimpan(tbmuser, jsonObject, hasil);
			} else if ("retur_pembelian_simpan".equals(action)) {
				KantinHelper.returPembelianSimpan(tbmuser, jsonObject, hasil);
			} else if ("retur_pembelian_list".equals(action)) {
				KantinHelper.returPembelianList(tbmuser, jsonObject, hasil);
			} else if ("retur_pembelian_hapus".equals(action)) {
				KantinHelper.returPembelianHapus(tbmuser, jsonObject, hasil);
			} else if ("sinkron_stok_toko".equals(action)) {
				KantinHelper.sinkronStokToko(tbmuser, jsonObject, hasil);
			} else if ("tabungan".equals(action)) {
				KantinHelper.tabungan(jsonObject, hasil);
			} else if ("sesi_kas_status".equals(action)) {
				KantinHelper.sesiKasStatus(tbmuser, jsonObject, hasil);
			} else if ("sesi_kas_buka".equals(action)) {
				KantinHelper.sesiKasBuka(tbmuser, jsonObject, hasil);
			} else if ("sesi_kas_tutup".equals(action)) {
				KantinHelper.sesiKasTutup(tbmuser, jsonObject, hasil);
			} else if ("topup_saldo".equals(action)) {
				KantinHelper.topupSaldo(tbmuser, jsonObject, hasil);
			} else if ("penyesuaian_saldo_cek".equals(action)
					|| "penyesuaian_saldo_simpan".equals(action)
					|| "penyesuaian_saldo_list".equals(action)) {
				// Opname saldo voucher/deposit dari halaman web memakai MESIN YANG SAMA dengan POS
				// Desktop/Android (PenyesuaianSaldoHelper), bukan salinan aturan baru -- termasuk
				// gerbang hak aksesnya (Tbmrole.bolehEntryTopup), pembacaan ulang saldo sistem di
				// server, dan penulisan satu mutasi koreksi dalam satu transaksi. Bila aturannya
				// ditulis ulang di JSP, dua kanal akan berbeda persis pada hal yang paling perlu
				// konsisten: siapa yang boleh membetulkan saldo orang lain.
				ais.action.servlet.api.PenyesuaianSaldoHelper.proses(action, tbmuser, jsonObject, hasil);
			} else if ("retur_penjualan_list".equals(action)) {
				KantinHelper.returPenjualanList(tbmuser, jsonObject, hasil);
			} else if ("retur_penjualan_simpan".equals(action)) {
				KantinHelper.returPenjualanSimpan(tbmuser, jsonObject, hasil);
			} else if ("pesanan_online_baru".equals(action)) {
				KantinHelper.pesananOnlineBaru(jsonObject, hasil);
			} else if ("topup".equals(action)) {
				hasil = TopupHelper.topup(jsonObject, request, tbmuser);
			} else if ("bayarOnline".equals(action)) {
				hasil = TopupHelper.bayarOnline(jsonObject, request, tbmuser);
			} else if ("laporan".equals(action)) {
				try {
					String formatLaporan = jsonObject.optString("formatLaporan", Report.PDF);
					JSONObject params = jsonObject.getJSONObject("params");
					Map<String, Object> parameters = JsonUtil.toMap(params);
					File file = new File(jsonObject.getString("file"));
					File fileHasil = Report.generateCompileFileReport(formatLaporan, parameters, file.getAbsolutePath(),
							ais.ui.util.WaktuUtil.getDate(), false);
					String url = Common.ROOT + "/report/" + fileHasil.getName();
					hasil.put("data", url);
					hasil.put("file", fileHasil.getAbsolutePath());
					hasil.put("status", "00");
				} catch (Exception e) {
					hasil.put("status", "99");
					hasil.put("description", "Error: " + e.getMessage());
				}
			} else {
				hasil.put("description", "Action tidak dikenali: " + action);
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Data.java:513");
			try {
				hasil.put("status", "99");
				hasil.put("description", "Error: " + e.getMessage());
			} catch (JSONException ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/servlet/Data.java:517");
			}
		}

		if ("true".equalsIgnoreCase(log)) {
			System.out.println("response -> " + hasil.toString());
		}

		return hasil;
	}

	private static void processFile(JSONObject jsonObject, JSONObject hasil) throws Exception {
		if (!jsonObject.isNull("class") && !jsonObject.isNull("ref") && !jsonObject.isNull("jenis")) {
			String refStr = jsonObject.optString("ref", "").trim();
			if (!Common.isNumber(refStr))
				return;

			Class<?> clazz = Class.forName(jsonObject.optString("class", "").trim());
			Serializable ref = (clazz.getName().equalsIgnoreCase(Tbmuser.class.getName())
					|| clazz.getName().equalsIgnoreCase(Tbmrole.class.getName())) ? refStr : Long.parseLong(refStr);

			String jenis = jsonObject.optString("jenis", "").trim();
			boolean usingId = jsonObject.optBoolean("usingId", false);
			boolean refresh = jsonObject.optBoolean("refresh", false);
			String kondisiTambahan = !jsonObject.isNull("kondisiTambahan")
					? jsonObject.optString("kondisiTambahan").trim()
					: "";

			FileFotoLain fileFotoLain = FileFotoLain.ambil(usingId, ref, jenis, 0, clazz, refresh, kondisiTambahan);

			if (fileFotoLain != null) {
				JSONObject objectData = new JSONObject();
				if (fileFotoLain.getGdrive() != null && !fileFotoLain.getGdrive().isEmpty()) {
					objectData.put("gdrive", fileFotoLain.getGdrive());
				}
				objectData.put("url", fileFotoLain.createLinkUri());
				objectData.put("nama", fileFotoLain.getNama());
				objectData.put("id", fileFotoLain.getId());
				objectData.put("mime", fileFotoLain.getKeterangan());

				hasil.put("data", objectData);
				hasil.put("status", "00");
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private static void updateFile(JSONObject jsonObject, JSONObject hasil) throws Exception {
		String idStr = jsonObject.optString("id", "").trim();
		String refStr = jsonObject.optString("ref", "").trim();

		if (Common.isNumber(idStr) && Common.isNumber(refStr)) {
			Serializable id = Long.parseLong(idStr);
			String jenis = jsonObject.optString("jenis", "").trim();
			String classa = jsonObject.optString("class", "").trim();
			Class clazz = LampiranLain.class;
			try {
				if (!classa.isEmpty()) {
					clazz = Class.forName(classa);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Data.java:578");
			}

			FileFotoLain fileFotoLain = (FileFotoLain) AmbilDataLampiranFileLain.ambilFile(id, clazz);
			System.out.println("fileFotoLain: " + fileFotoLain);
			if (fileFotoLain != null) {
				Session streamingSession = null;
				try {
					streamingSession = StreamingHibernateUtil.getInstance().openSession();
					streamingSession.refresh(fileFotoLain);
					AmbilDataLampiranFileLain.mappingInstanceData(fileFotoLain, Long.parseLong(refStr), null, jenis,
							fileFotoLain.getLink(), fileFotoLain.getNama(), fileFotoLain.getKeterangan(),
							fileFotoLain.getOlehId(), fileFotoLain.getOleh());
					streamingSession.getTransaction().begin();
					streamingSession.update(fileFotoLain);
					streamingSession.getTransaction().commit();
				} catch (Exception e1) {
					rollbackQuietly(streamingSession);
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/servlet/Data.java:596");
				} finally {
					ais.database.hibernate.HibernateUtil.closeSessionQuietly(streamingSession);
				}

				fileFotoLain = FileFotoLain.ambil(Long.parseLong(refStr), jenis, clazz, true);
				
				JSONObject objectData = new JSONObject();
				objectData.put("url", fileFotoLain.createLinkUri());
				objectData.put("nama", fileFotoLain.getNama());
				objectData.put("id", fileFotoLain.getId());
				objectData.put("mime", fileFotoLain.getKeterangan());
				hasil.put("data", objectData);
				hasil.put("status", "00");
			}
		}
	}

	private static void hapusFile(JSONObject jsonObject, JSONObject hasil) throws Exception {
		String refStr = jsonObject.optString("ref", "").trim();

		if (!jsonObject.isNull("jenis") && !jsonObject.isNull("clazz") && Common.isNumber(refStr)) {
			Serializable ref = Long.parseLong(refStr);
			String jenis = jsonObject.optString("jenis", "").trim();
			Class<?> clazz = Class.forName(jsonObject.optString("clazz", "").trim());

			FileFotoLain fileFotoLain = (FileFotoLain) LampiranLain.ambil(false, ref, jenis, clazz);

			if (fileFotoLain != null) {
				JSONObject objectData = new JSONObject();
				objectData.put("url", fileFotoLain.createLinkUri());
				objectData.put("nama", fileFotoLain.getNama());
				objectData.put("id", fileFotoLain.getId());
				objectData.put("mime", fileFotoLain.getKeterangan());
				hasil.put("data", objectData);
				hasil.put("status", "00");

				Session streamingSession = null;
				try {
					streamingSession = StreamingHibernateUtil.getInstance().openSession();
					streamingSession.getTransaction().begin();
					streamingSession.delete(fileFotoLain);
					streamingSession.getTransaction().commit();
				} catch (Exception e1) {
					rollbackQuietly(streamingSession);
				} finally {
					ais.database.hibernate.HibernateUtil.closeSessionQuietly(streamingSession);
				}
			}
		}
	}

	private static void hapusFileById(JSONObject jsonObject, JSONObject hasil) throws Exception {
		String idStr = jsonObject.optString("id", "").trim();

		if (!jsonObject.isNull("jenis") && !jsonObject.isNull("clazz") && Common.isNumber(idStr)) {
			Serializable id = Long.parseLong(idStr);
			String jenis = jsonObject.optString("jenis", "").trim();
			Class<?> clazz = Class.forName(jsonObject.optString("clazz", "").trim());

			FileFotoLain fileFotoLain = (FileFotoLain) LampiranLain.ambil(true, id, jenis, clazz);

			if (fileFotoLain != null) {
				JSONObject objectData = new JSONObject();
				objectData.put("url", fileFotoLain.createLinkUri());
				objectData.put("nama", fileFotoLain.getNama());
				objectData.put("id", fileFotoLain.getId());
				objectData.put("mime", fileFotoLain.getKeterangan());
				hasil.put("data", objectData);
				hasil.put("status", "00");

				Session streamingSession = null;
				try {
					streamingSession = StreamingHibernateUtil.getInstance().openSession();
					streamingSession.getTransaction().begin();
					streamingSession.delete(fileFotoLain);
					streamingSession.getTransaction().commit();
				} catch (Exception e1) {
					rollbackQuietly(streamingSession);
				} finally {
					ais.database.hibernate.HibernateUtil.closeSessionQuietly(streamingSession);
				}
			}
		}
	}

	private static void processCari(JSONObject jsonObject, JSONObject hasil) throws Exception {
		if (!jsonObject.isNull("class") && !jsonObject.isNull("id")) {
			Class<?> clazz = Class.forName(jsonObject.optString("class", "").trim());
			String id = jsonObject.optString("id", "").trim();

			GeneralValueObject generalValueObject = (GeneralValueObject) GeneralValueObject.ambilData(clazz, id, true);

			if (generalValueObject != null) {
				JSONObject objectData = new JSONObject();
				Common.insertProperty(clazz, generalValueObject, objectData, "", 1);
				JSONArray array = new JSONArray();
				array.put(objectData);
				hasil.put("data", array);
				hasil.put("status", "00");
			}
		}
	}

	private static void processDownload(HttpServletRequest request, JSONObject jsonObject, String type,
			JSONObject hasil) throws Exception {
		jsonObject.put("action", "daftar");
		JSONObject responseDaftar = DaftarDataService.daftar(request, jsonObject, false);

		if (responseDaftar.has("status"))
			hasil.put("status", responseDaftar.get("status"));
		if (responseDaftar.has("description"))
			hasil.put("description", responseDaftar.get("description"));

		if (!responseDaftar.has("data"))
			return;

		JSONArray object = responseDaftar.getJSONArray("data");
		String fileName = Common.getGeneratedBarCode();
		String link = "";
		File file = null;

		if ("downloadExcel".equals(type)) {
			file = new File(Common.REAL_PATH + "/f/" + fileName + ".xlsx");
			ExcelExporter.exportJsonToExcel(object, file);
		} else if ("downloadPdf".equals(type)) {
			file = new File(Common.REAL_PATH + "/f/" + fileName + ".pdf");
			PdfGenerator.convertJsonToPdf(object, file);
		} else if ("downloadDocx".equals(type)) {
			file = new File(Common.REAL_PATH + "/f/" + fileName + ".docx");
			WordExporter.exportJsonToWordTable(object, file);
		}

		if (file != null)
			link = Common.ROOT + "/f/" + file.getName();
		hasil.put("link", link);
	}

	private static void processLinimasa(HttpServletRequest request, Tbmuser tbmuser, JSONObject jsonObject,
			JSONObject hasil) throws Exception {
		jsonObject.put("token", tbmuser.getToken());
		JSONObject apiResult = LinimasaApi.linimasa(tbmuser, jsonObject, request);
		JSONArray sourceArray = apiResult.optJSONArray("data");
		JSONArray arrayBaru = new JSONArray();

		if (sourceArray != null) {
			for (int i = 0; i < sourceArray.length(); i++) {
				JSONObject obj = sourceArray.optJSONObject(i);
				if (obj != null && obj.has("id"))
					arrayBaru.put(obj.get("id"));
			}
		}
		hasil.put("totalSize", apiResult.opt("totalSize"));
		hasil.put("data", arrayBaru);
		hasil.put("status", "00");
	}

	private static void processGenericLinimasaItem(HttpServletRequest request, Tbmuser tbmuser, JSONObject jsonObject,
			String actionType, JSONObject hasil) throws Exception {
		jsonObject.put("token", tbmuser.getToken());
		jsonObject.put("hanyaIdSaja", "true");
		JSONObject apiResult = new JSONObject();

		if ("ujian".equals(actionType))
			apiResult = LinimasaApi.daftar_ujian(tbmuser, jsonObject, request);
		else if ("tugas".equals(actionType))
			apiResult = LinimasaApi.daftar_tugas(tbmuser, jsonObject, request);
		else if ("materi".equals(actionType))
			apiResult = LinimasaApi.daftar_materi(tbmuser, jsonObject, request);
		else if ("audio".equals(actionType))
			apiResult = LinimasaApi.daftar_audio(tbmuser, jsonObject, request);
		else if ("video".equals(actionType))
			apiResult = LinimasaApi.daftar_video(tbmuser, jsonObject, request);
		else if ("tugas_kelompok".equals(actionType))
			apiResult = LinimasaApi.daftar_tugas_kelompok(tbmuser, jsonObject, request);

		JSONArray sourceArray = apiResult.optJSONArray("data");
		JSONArray arrayBaru = new JSONArray();
		if (sourceArray != null) {
			for (int i = 0; i < sourceArray.length(); i++) {
				JSONObject obj = sourceArray.optJSONObject(i);
				if (obj != null && obj.has("id"))
					arrayBaru.put(obj.get("id"));
			}
		}

		hasil.put("totalSize", apiResult.opt("totalSize"));
		hasil.put("data", arrayBaru);
		hasil.put("status", "00");
	}

	private static void processRingkasan(Tbmuser tbmuser, JSONObject jsonObject, JSONObject hasil) throws Exception {
		String ta = jsonObject.optString("ta", null);
		String smt = jsonObject.optString("smt", null);
		String cari = jsonObject.optString("cari", "");
		boolean refresh = jsonObject.optBoolean("refresh", false);

		if (ta != null && ta.trim().isEmpty())
			ta = null;
		if (smt != null && smt.trim().isEmpty())
			smt = null;

		int ditampilkanHanya = jsonObject.isNull("ditampilkanHanya") ? TampilanELearningAction.PERKULIAHAN
				: Integer.parseInt(jsonObject
						.optString("ditampilkanHanya", String.valueOf(TampilanELearningAction.PERKULIAHAN)).trim());
		int activePage = jsonObject.optInt("activePage", 0);

		Paging paging = new Paging();
		List<VOPembelajaran> perkuliahans = RekapitulasiPerkuliahanHelper.ambilPembelajaran(tbmuser, ta, smt, cari,
				refresh, activePage, false, ditampilkanHanya, paging, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
					}
				}, null);

		JSONArray arrayBaru = new JSONArray();
		if (perkuliahans != null) {
			for (VOPembelajaran iddata : perkuliahans)
				arrayBaru.put(iddata.getId().toString());
		}

		hasil.put("totalSize", paging.getTotalSize());
		hasil.put("data", arrayBaru);
		hasil.put("status", "00");
	}
}
