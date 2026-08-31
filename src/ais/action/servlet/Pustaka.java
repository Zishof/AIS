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

/**
 * Servlet publik "Pustaka" (katalog perpustakaan): menyajikan halaman katalog buku/koleksi
 * perpustakaan, baik sebagai JSP standalone versi baru ({@code /WEB-INF/baru/pustaka.jsp}, dipilih
 * lewat konfigurasi {@code default_pustaka_gunakan_versi_baru}, default AKTIF) maupun versi lama
 * yang menyuntikkan data JSON 500 {@link Item} terbaru sebagai atribut request ke
 * {@code /WEB-INF/u/pustaka.jsp}. Juga menyediakan endpoint tersembunyi {@code ?action=getFoto}
 * yang men-stream foto profil {@link Anggota} perpustakaan (mahasiswa/siswa/dosen/guru/pegawai/admin)
 * langsung sebagai gambar.
 *
 * <p>
 * Endpoint {@code getFoto} menerapkan kontrol akses eksplisit: hanya admin, staf perpustakaan
 * ({@code LibraryPermissionGuard.isStaff}), atau pemilik profil sendiri yang boleh mengambil foto —
 * ditolak dengan 401 (belum login) atau 403 (bukan pemilik/staf/admin) bila tidak memenuhi syarat.
 * Bila foto fisik tidak ditemukan, servlet mengalihkan (redirect) ke gambar avatar default, bukan
 * mengembalikan error.
 * </p>
 */
public class Pustaka extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** Konstruktor default tanpa inisialisasi khusus. */
	public Pustaka() {
		super();
	}

	/** Menangani permintaan GET dengan mendelegasikan ke {@link #process}; kegagalan ditangkap dan dilaporkan, tidak dilempar ke container. */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Menangani permintaan POST dengan perilaku identik {@link #doGet}. */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	/**
	 * Menyusun representasi JSON satu {@link Item} untuk katalog versi lama: metadata dasar (judul,
	 * ISBN/ISBN10 fallback, pengarang, penerbit, klasifikasi, tahun, deskripsi), tautan unduh
	 * lampiran ({@link LampiranLain#ITEM}) bila ada, tautan pratinjau Google Books bila item punya
	 * {@code googleBookId}, dan riwayat peminjaman ({@link PeminjamanPengadaanItemDetail}, terbaru
	 * lebih dulu).
	 */
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
	/**
	 * Implementasi kanonik: (1) bila {@code action=getFoto}, memvalidasi wewenang lalu men-stream
	 * foto profil {@link Anggota} target (diresolusi ke entitas peran spesifik —
	 * mahasiswa/siswa/dosen/guru/pegawai/admin — untuk menentukan sumber {@code FileFotoLain} yang
	 * tepat) langsung ke response, atau redirect ke avatar default bila tidak ditemukan; (2) bila
	 * tidak, meneruskan ke JSP katalog versi baru atau lama sesuai konfigurasi
	 * {@code default_pustaka_gunakan_versi_baru} (lihat dokumentasi kelas).
	 */
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		// =========================================================================================
		// AWAL: Endpoint Khusus untuk Stream Gambar Profile Anggota (Kunjungan)
		// =========================================================================================
		if ("getFoto".equals(request.getParameter("action"))) {
			String anggotaIdStr = request.getParameter("anggotaId");
			boolean fotoBerhasilDiStream = false;
			Tbmuser currentUser = Common.getCurrentUser(request);
			Long requestedMemberId = null;
			try { requestedMemberId = Long.valueOf(anggotaIdStr == null ? "" : anggotaIdStr.trim()); } catch (Exception ignored) { }
			Anggota ownMember = currentUser == null ? null : Anggota.buatAtauAmbilAnggota(currentUser, false);
			boolean mayReadPhoto = currentUser != null && requestedMemberId != null
					&& (Common.getApakahAdmin() || ais.action.master.library.modern.LibraryPermissionGuard.isStaff(request)
							|| (ownMember != null && requestedMemberId.equals(ownMember.getId())));
			if (!mayReadPhoto) {
				response.sendError(currentUser == null ? HttpServletResponse.SC_UNAUTHORIZED : HttpServletResponse.SC_FORBIDDEN);
				return;
			}

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
				response.sendRedirect(Common.ROOT + "/img/user_default.png");
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
