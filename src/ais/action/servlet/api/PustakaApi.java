package ais.action.servlet.api;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.json.JSONObject;

import ais.action.master.library.modern.LibraryCatalogSearchRequest;
import ais.action.master.library.modern.LibraryCatalogSearchResult;
import ais.action.master.library.modern.LibraryCatalogSearchService;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.library.Item;

/**
 * <h3>API Pustaka untuk klien mobile/desktop — BACA SAJA.</h3>
 *
 * <p>Menyajikan koleksi perpustakaan yang sudah terbit agar dapat dibaca di
 * dalam aplikasi. Karena koleksi terikat hak cipta, API ini <b>tidak pernah</b>
 * mengirimkan berkas dokumen utuh: isi disajikan per halaman sebagai gambar
 * ber-watermark identitas pembaca (lihat {@link PembacaTerlindungiUtil}).</p>
 *
 * <p>Kriteria "terbit" TIDAK ditulis ulang di sini melainkan memakai
 * {@code LibraryCatalogSearchService} yang sudah dipakai katalog web, supaya
 * aturan mana koleksi yang boleh tampil hanya punya SATU sumber kebenaran.</p>
 *
 * <p>Aksi:
 * <ul>
 *   <li>{@code pustaka_daftar} — {q, halaman, jumlahDataDalamSatuHalaman, urut}</li>
 *   <li>{@code pustaka_detail} — {id} → metadata + jumlah halaman bila ada berkas</li>
 *   <li>{@code pustaka_halaman} — {id, halaman} → gambar JPEG base64 ber-watermark</li>
 * </ul>
 * Ketiganya wajib token pengguna yang sah.</p>
 */
public final class PustakaApi {

	private PustakaApi() {
	}

	/** Batas atas halaman per permintaan; menahan penarikan massal isi buku. */
	private static final int MAKS_UKURAN_HALAMAN = 50;

	/** Baca id sebagai angka; nilai tak terbaca dianggap tidak ada. */
	private static Long angka(JSONObject request, String kunci) {
		try {
			String mentah = ApiHelperSupport.optString(request, kunci);
			if (mentah == null || mentah.trim().isEmpty()) {
				return null;
			}
			return Long.valueOf(mentah.trim());
		} catch (Exception abaikan) {
			return null;
		}
	}

	private static JSONObject tolakTanpaToken() throws Exception {
		return ApiHelperSupport.status("97",
				"Sesi tidak valid atau sudah berakhir. Silakan masuk kembali.");
	}

	/** Identitas yang dicap pada tiap halaman; melekat pada pembacanya. */
	private static String identitasPembaca(Tbmuser pengguna) {
		if (pengguna == null) {
			return "Pengguna tidak dikenal";
		}
		String nama = pengguna.getNama() == null ? pengguna.getUserId() : pengguna.getNama();
		String nomor = null;
		try {
			if (pengguna.getMahasiswa() != null) {
				nomor = pengguna.getMahasiswa().getNim();
			} else if (pengguna.getSiswa() != null) {
				nomor = pengguna.getSiswa().getNis();
			} else if (pengguna.getPegawai() != null) {
				nomor = pengguna.getPegawai().getNipLama();
			}
		} catch (Exception abaikan) {
			// Nomor induk hanya pelengkap; nama sudah cukup menandai pembaca.
		}
		return nomor == null || nomor.trim().isEmpty() ? String.valueOf(nama)
				: nama + " · " + nomor;
	}

	/** Daftar koleksi terbit, dengan pencarian dan paging. */
	public static JSONObject daftar(HttpServletRequest req, JSONObject request) throws Exception {
		Tbmuser pengguna = ApiUtil.currentUser(request, req);
		if (pengguna == null) {
			return tolakTanpaToken();
		}
		try {
			LibraryCatalogSearchRequest cari = new LibraryCatalogSearchRequest();
			cari.setQuery(ApiHelperSupport.optString(request, "q"));
			cari.setPage(request.optInt("halaman", 1));
			cari.setPageSize(Math.min(MAKS_UKURAN_HALAMAN,
					request.optInt("jumlahDataDalamSatuHalaman", 20)));
			String urut = ApiHelperSupport.optString(request, "urut");
			if (urut != null && !urut.trim().isEmpty()) {
				cari.setSort(urut);
			}

			LibraryCatalogSearchResult hasil = new LibraryCatalogSearchService().search(cari);
			JSONObject jawaban = ApiHelperSupport.status("00", "Koleksi pustaka berhasil dimuat.");
			JSONObject isi = hasil.toJson();
			jawaban.put("data", isi.opt("data"));
			jawaban.put("size", isi.opt("total"));
			jawaban.put("halaman", isi.opt("page"));
			// Ditegaskan ke klien supaya UI tidak menawarkan unduh/bagikan berkas.
			jawaban.put("hanyaBaca", true);
			return jawaban;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return ApiHelperSupport.errorResponse("Koleksi pustaka gagal dimuat.");
		}
	}

	/**
	 * Rincian satu koleksi. Hanya koleksi TERBIT yang boleh dibuka; koleksi
	 * yang belum terbit dijawab seolah tidak ada, bukan "ditolak", supaya
	 * keberadaannya tidak bocor lewat perbedaan pesan.
	 */
	public static JSONObject detail(HttpServletRequest req, JSONObject request) throws Exception {
		Tbmuser pengguna = ApiUtil.currentUser(request, req);
		if (pengguna == null) {
			return tolakTanpaToken();
		}
		Long id = angka(request, "id");
		if (id == null) {
			return ApiHelperSupport.status("91", "Parameter id wajib diisi.");
		}
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Item item = (Item) session.get(Item.class, id);
			if (item == null || !terbit(item)) {
				return ApiHelperSupport.status("94", "Koleksi tidak ditemukan.");
			}

			JSONObject jawaban = ApiHelperSupport.status("00", "Rincian koleksi berhasil dimuat.");
			jawaban.put("id", item.getId());
			jawaban.put("nama", item.getNama());
			jawaban.put("pengarangs", item.getPengarangs());
			jawaban.put("penerbit", item.getPenerbit() == null ? null : item.getPenerbit().getNama());
			jawaban.put("tahun", item.getTahun());
			jawaban.put("isbn", item.getIsbn());
			jawaban.put("issn", item.getIssn());
			jawaban.put("bahasa", item.getBahasa());
			jawaban.put("kategories", item.getKategories());
			jawaban.put("ringkasan", item.getAbstrak());
			jawaban.put("callNumber", item.getCallnumber());
			jawaban.put("imageUrl", item.getImageUrl());
			jawaban.put("hanyaBaca", true);

			int jumlahHalaman = 0;
			InputStream aliran = null;
			try {
				aliran = aliranBerkas(item);
				if (aliran != null) {
					jumlahHalaman = PembacaTerlindungiUtil.jumlahHalaman(aliran);
				}
			} catch (Exception e) {
				// Berkas rusak/bukan PDF: koleksi tetap tampil, hanya tidak
				// dapat dibaca di aplikasi. Lebih jujur daripada layar kosong.
				Common.tampilErrorJikaAdmin(e);
			} finally {
				tutup(aliran);
			}
			jawaban.put("jumlahHalaman", jumlahHalaman);
			jawaban.put("dapatDibaca", jumlahHalaman > 0);
			return jawaban;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return ApiHelperSupport.errorResponse("Rincian koleksi gagal dimuat.");
		} finally {
			ApiHelperSupport.closeOpenedSession(session);
		}
	}

	/** Satu halaman koleksi sebagai gambar ber-watermark identitas pembaca. */
	public static JSONObject halaman(HttpServletRequest req, JSONObject request) throws Exception {
		Tbmuser pengguna = ApiUtil.currentUser(request, req);
		if (pengguna == null) {
			return tolakTanpaToken();
		}
		Long id = angka(request, "id");
		int halaman = request.optInt("halaman", 1);
		if (id == null || halaman < 1) {
			return ApiHelperSupport.status("91", "Parameter id dan halaman wajib benar.");
		}
		Session session = null;
		InputStream aliran = null;
		try {
			session = HibernateUtil.openSession();
			Item item = (Item) session.get(Item.class, id);
			if (item == null || !terbit(item)) {
				return ApiHelperSupport.status("94", "Koleksi tidak ditemukan.");
			}
			aliran = aliranBerkas(item);
			if (aliran == null) {
				return ApiHelperSupport.status("94", "Koleksi ini belum memiliki berkas digital.");
			}
			String gambar = PembacaTerlindungiUtil.halamanSebagaiBase64(aliran, halaman,
					identitasPembaca(pengguna));
			if (gambar == null) {
				return ApiHelperSupport.status("94", "Halaman tidak tersedia.");
			}
			JSONObject jawaban = ApiHelperSupport.status("00", "Halaman berhasil dimuat.");
			jawaban.put("halaman", halaman);
			jawaban.put("gambarBase64", gambar);
			jawaban.put("mime", "image/jpeg");
			jawaban.put("hanyaBaca", true);
			return jawaban;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return ApiHelperSupport.errorResponse("Halaman gagal dimuat.");
		} finally {
			tutup(aliran);
			ApiHelperSupport.closeOpenedSession(session);
		}
	}

	/**
	 * Kriteria terbit yang SAMA dengan katalog web
	 * ({@code LibraryCatalogSearchService}): aktif (null dianggap aktif) dan
	 * status terbitnya termasuk terbit/publish/disetujui — status kosong pun
	 * dianggap terbit, mengikuti perilaku katalog yang sudah berjalan.
	 */
	private static boolean terbit(Item item) {
		if (Boolean.FALSE.equals(item.getAktif())) {
			return false;
		}
		try {
			if (item.getStatusTerbitItem() == null) {
				return true;
			}
			String nama = item.getStatusTerbitItem().getNama();
			if (nama == null) {
				return true;
			}
			String n = nama.trim().toLowerCase(java.util.Locale.ENGLISH);
			return "terbit".equals(n) || "publish".equals(n) || "disetujui".equals(n);
		} catch (Exception e) {
			// Relasi tidak dapat dibaca -> jangan tampilkan; menutup lebih aman
			// daripada membocorkan koleksi yang belum tentu boleh terbit.
			Common.tampilErrorJikaAdmin(e);
			return false;
		}
	}

	/** Aliran berkas digital koleksi (Blob di basis data), null bila tidak ada. */
	private static InputStream aliranBerkas(Item item) {
		try {
			LampiranLain lampiran = LampiranLain.ambil(item.getId(), LampiranLain.ITEM);
			if (lampiran == null || lampiran.getFoto() == null) {
				return null;
			}
			return lampiran.getFoto().getBinaryStream();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	private static void tutup(InputStream aliran) {
		if (aliran != null) {
			try {
				aliran.close();
			} catch (Exception abaikan) {
				// Tidak ada lagi yang bisa dilakukan.
			}
		}
	}
}
