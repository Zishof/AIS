package ais.action.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.file.LampiranLain;
import ais.database.model.library.Item;
import ais.database.model.library.PeminjamanPengadaanItemDetail;

// Import tambahan yang diperlukan untuk Kunjungan & Profile
import ais.database.model.library.Anggota;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

public class Pustaka extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public Pustaka() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	private JSONObject populate(Item item, Session session) throws Exception {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", item.getId());
		jsonObject.put("cover", LibraryUtil.generateImageString(item));
		jsonObject.put("title", item.getNama());
		jsonObject.put("isbn", item.getIsbn().trim().isEmpty() ? item.getIsbn10() : item.getIsbn());
		jsonObject.put("author", item.getPengarangs());
		jsonObject.put("author_simple", Common.simpleString(item.getPengarangs(), 30));
		jsonObject.put("publisher", item.getPenerbit() == null ? "" : item.getPenerbit().getNama());
		jsonObject.put("classification", item.getKategories());
		jsonObject.put("year", item.getTahun());
		jsonObject.put("description", item.getAbstrak().isEmpty() ? item.getKeterangan() : item.getAbstrak());

		LampiranLain lampiranLain = LampiranLain.ambil(item.getId(), LampiranLain.ITEM);
		if (lampiranLain != null && lampiranLain.getId() != null) {
			jsonObject.put("downloadLink", lampiranLain.createLinkUri());
		}

		try {
			if (item.getGoogleBookId() != null && !item.getGoogleBookId().trim().isEmpty()) {
				JSONObject volumeInfo = new JSONObject(item.getInfoLain()).getJSONObject("volumeInfo");
				jsonObject.put("readLink", volumeInfo.getString("previewLink"));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Pustaka.java:80");
		}

		List<PeminjamanPengadaanItemDetail> detailTransaksis = session
				.createCriteria(PeminjamanPengadaanItemDetail.class).add(Restrictions.eq("item", item))
				.add(Restrictions.isNotNull("peminjamanPengadaanItem")).addOrder(Order.desc("id")).list();

		JSONArray loanHistory = new JSONArray();
		for (PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail : detailTransaksis) {
			JSONObject jsonObjectHistory = new JSONObject();
			jsonObjectHistory.put("id", peminjamanPengadaanItemDetail.getId());
			jsonObjectHistory.put("barcode", peminjamanPengadaanItemDetail.getItemPunyaBarcode() == null ? ""
					: peminjamanPengadaanItemDetail.getItemPunyaBarcode().getBarcode());

			jsonObjectHistory.put("date",
					peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem().getTanggalPersetujuan() == null ? ""
							: Common.dateFormat61.get().format(peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem()
									.getTanggalPersetujuan()));
			jsonObjectHistory.put("returnDate", peminjamanPengadaanItemDetail.getTanggalKembali() == null ? ""
					: Common.dateFormat61.get().format(peminjamanPengadaanItemDetail.getTanggalKembali()));
			loanHistory.put(jsonObjectHistory);
		}
		detailTransaksis.clear();
		detailTransaksis = null;
		jsonObject.put("borrowHistory", loanHistory);
		return jsonObject;
	}

	@SuppressWarnings({ })
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		// =========================================================================================
		// AWAL: Endpoint Khusus untuk Stream Gambar Profile Anggota (Kunjungan)
		// =========================================================================================
		if ("getFoto".equals(request.getParameter("action"))) {
			String anggotaIdStr = request.getParameter("anggotaId");
			boolean fotoBerhasilDiStream = false;

			if (anggotaIdStr != null && !anggotaIdStr.trim().isEmpty()) {
				try {
					Anggota a = (Anggota) GeneralValueObject.ambilData(Anggota.class, anggotaIdStr, true);
					if (a != null) {
						Tbmuser targetUser = null;

						// Menerapkan Logika Hierarki Kasta Pengguna
						if (a.getMahasiswa() != null) targetUser = new Tbmuser(a.getMahasiswa());
						else if (a.getSiswa() != null) targetUser = new Tbmuser(a.getSiswa());
						else if (a.getGuru() != null) targetUser = new Tbmuser(a.getGuru());
						else if (a.getDosen() != null) targetUser = new Tbmuser(a.getDosen());
						else if (a.getPegawai() != null) targetUser = new Tbmuser(a.getPegawai());
						else if (a.getTbmuser() != null) targetUser = a.getTbmuser();

						if (targetUser != null) {
							java.io.Serializable targetId = null;
							String targetJenis = null;
							Class<?> targetClass = null;

							// Menentukan variabel dinamis (id, jenis, class) untuk pencarian FileFotoLain
							if (targetUser.getMahasiswa() != null && targetUser.getMahasiswa().getId() != null) {
								targetId = targetUser.getMahasiswa().getId();
								targetJenis = ais.database.model.file.FotoMahasiswa.DEFAULT_JENIS;
								targetClass = ais.database.model.file.FotoMahasiswa.class;
							} else if (targetUser.getSiswa() != null && targetUser.getSiswa().getId() != null) {
								targetId = targetUser.getSiswa().getId();
								targetJenis = ais.database.model.file.FotoSiswa.DEFAULT_JENIS;
								targetClass = ais.database.model.file.FotoSiswa.class;
							} else if (targetUser.getDosen() != null && targetUser.getDosen().getId() != null) {
								targetId = targetUser.getDosen().getId();
								targetJenis = ais.database.model.file.FotoDosen.DEFAULT_JENIS;
								targetClass = ais.database.model.file.FotoDosen.class;
							} else if (targetUser.getGuru() != null && targetUser.getGuru().getId() != null) {
								targetId = targetUser.getGuru().getId();
								targetJenis = ais.database.model.file.FotoGuru.DEFAULT_JENIS;
								targetClass = ais.database.model.file.FotoGuru.class;
							} else if (targetUser.getPegawai() != null && targetUser.getPegawai().getId() != null) {
								targetId = targetUser.getPegawai().getId();
								targetJenis = ais.database.model.file.FotoPegawai.DEFAULT_JENIS;
								targetClass = ais.database.model.file.FotoPegawai.class;
							} else if (targetUser.getUserId() != null) {
								targetId = targetUser.getUserId();
								targetJenis = ais.database.model.file.FotoAdmin.DEFAULT_JENIS;
								targetClass = ais.database.model.file.FotoAdmin.class;
							}

							// Eksekusi Pengambilan File
							if (targetId != null && targetJenis != null && targetClass != null) {
								ais.database.model.file.FileFotoLain fileFotoLain = ais.database.model.file.FileFotoLain.ambil(targetId, targetJenis, targetClass);
								
								if (fileFotoLain != null) {
									java.io.File fileGambar = fileFotoLain.ambilFile();

									// Jika file fisik benar-benar ada di dalam server
									if (fileGambar != null && fileGambar.exists()) {

										// Deteksi MimeType (bisa disesuaikan, default image/jpeg)
										String mimeType = getServletContext().getMimeType(fileGambar.getName());
										if (mimeType == null) { mimeType = "image/jpeg"; }

										response.setContentType(mimeType);
										response.setContentLength((int) fileGambar.length());
										response.setHeader("Content-Disposition", "inline; filename=\"" + fileGambar.getName() + "\"");

										// Proses Streaming File Gambar Langsung ke Layar Browser
										java.io.FileInputStream inStream = new java.io.FileInputStream(fileGambar);
										java.io.OutputStream outStream = response.getOutputStream();
										byte[] buffer = new byte[4096];
										int bytesRead = -1;

										while ((bytesRead = inStream.read(buffer)) != -1) {
											outStream.write(buffer, 0, bytesRead);
										}

										inStream.close();
										outStream.flush();
										outStream.close();

										fotoBerhasilDiStream = true;
										return; // Keluar dari Servlet sepenuhnya
									}
								}
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Pustaka.java:203");
					// Error file handling diabaikan, akan masuk ke blok fallback avatar
				}
			}

			// Fallback: Jika ID tidak ada atau foto fisik tidak ditemukan, alihkan ke layanan avatar
			if (!fotoBerhasilDiStream) {
				response.sendRedirect("https://ui-avatars.com/api/?name=User&background=random");
				return;
			}
		}
		// =========================================================================================
		// AKHIR: Endpoint Khusus untuk Stream Gambar Profile Anggota
		// =========================================================================================

		Konfigurasi config = Common.getKonfigurasi("default_pustaka_gunakan_versi_baru", Konfigurasi.AKTIF);

		boolean isVersiBaruAktif = config != null && Konfigurasi.AKTIF.equalsIgnoreCase(config.getNilai());

		if (isVersiBaruAktif) {
			// Menangani link Pustaka Detail (SEO / Standalone View)
			String idBuku = request.getParameter("id");
			if (idBuku != null && !idBuku.trim().isEmpty()) {
				request.getRequestDispatcher("/WEB-INF/baru/pustaka.jsp?id=" + idBuku).forward(request, response);
				return;
			}
			// Tampilan halaman awal pustaka
			request.getRequestDispatcher("/WEB-INF/baru/pustaka.jsp").forward(request, response);
		} else {
			Session session = HibernateUtil.getSessionFactory().openSession();

			JSONArray books = new JSONArray();
			try {
				List<Item> items = ConstantValues.simpleList(
						session.createCriteria(Item.class).addOrder(Order.desc("id")).setMaxResults(500).setFirstResult(0),
						Item.class);

				for (Item item : items) {
					books.put(populate(item, session));
				}
			} finally {
				if (session != null) {
					try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Pustaka.java:245");}
					try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Pustaka.java:246");}
					try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Pustaka.java:247");}
				}
			}

			request.setAttribute("books", books.toString());

			request.getRequestDispatcher("/WEB-INF/u/pustaka.jsp").forward(request, response);
		}
	}
}