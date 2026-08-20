package ais.action.servlet.api;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.repository.RepositoryPublicService;
import ais.common.Common;
import ais.database.model.Tbmuser;
import ais.database.model.repository.RepoBitstream;

/**
 * <h3>API Repository digital untuk klien mobile/desktop — BACA SAJA.</h3>
 *
 * <p>Menyajikan artefak digital yang sudah terpublikasi (jurnal, skripsi, karya
 * ilmiah lain) agar dapat dibaca di dalam aplikasi. Seperti Pustaka, API ini
 * <b>tidak pernah</b> mengirim berkas utuh: isinya disajikan per halaman
 * sebagai gambar ber-watermark identitas pembaca.</p>
 *
 * <p><b>Aturan visibilitas memakai {@link RepositoryPublicService}</b> — yang
 * sudah menyaring item ditarik ({@code isWithdrawn}), status sinkron non-publik,
 * dan kebijakan akses berkas. Aksi API lama {@code repositoryMahasiswa}
 * SENGAJA tidak dipakai ulang: ia hanya menyaring kolom {@code aktif}, sehingga
 * ikut menampilkan artefak berstatus draft/gagal dan membuka field internal
 * (status sinkron, Turnitin) yang tidak layak dilihat pembaca umum.</p>
 *
 * <p>Aksi:
 * <ul>
 *   <li>{@code repository_daftar} — {q, halaman, jumlahDataDalamSatuHalaman, urut, jenis, tahun}</li>
 *   <li>{@code repository_detail} — {id} → metadata + daftar berkas yang boleh dibaca</li>
 *   <li>{@code repository_halaman} — {id, berkasId, halaman} → JPEG base64 ber-watermark</li>
 * </ul>
 * Ketiganya wajib token pengguna yang sah.</p>
 */
public final class RepositoryPublikApi {

	private RepositoryPublikApi() {
	}

	private static final int MAKS_UKURAN_HALAMAN = 50;

	private static JSONObject tolakTanpaToken() throws Exception {
		return ApiHelperSupport.status("97",
				"Sesi tidak valid atau sudah berakhir. Silakan masuk kembali.");
	}

	/**
	 * Izin membuka Repository (per pengguna, bawaan menyala). Diperiksa di
	 * SETIAP aksi: menyembunyikan menu saja tidak menghalangi pemanggilan API.
	 */
	private static boolean bolehMembaca(Tbmuser pengguna) {
		return pengguna != null
				&& Boolean.TRUE.equals(pengguna.getBolehBacaRepository());
	}

	private static JSONObject tolakTanpaIzin() throws Exception {
		return ApiHelperSupport.status("96",
				"Akses ke Repository tidak diaktifkan untuk akun Anda.");
	}

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

	/** Identitas pembaca yang dicap pada setiap halaman. */
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
			// Nomor induk hanya pelengkap.
		}
		return nomor == null || nomor.trim().isEmpty() ? String.valueOf(nama)
				: nama + " · " + nomor;
	}

	public static JSONObject daftar(HttpServletRequest req, JSONObject request) throws Exception {
		Tbmuser pengguna = ApiUtil.currentUser(request, req);
		if (pengguna == null) {
			return tolakTanpaToken();
		}
		if (!bolehMembaca(pengguna)) {
			return tolakTanpaIzin();
		}
		try {
			RepositoryPublicService layanan = new RepositoryPublicService();
			RepositoryPublicService.Query kueri = new RepositoryPublicService.Query();
			kueri.keyword = ApiHelperSupport.trimToEmpty(
					ApiHelperSupport.optString(request, "q"));
			kueri.documentType = ApiHelperSupport.trimToEmpty(
					ApiHelperSupport.optString(request, "jenis"));
			kueri.author = ApiHelperSupport.trimToEmpty(
					ApiHelperSupport.optString(request, "penulis"));
			kueri.subject = ApiHelperSupport.trimToEmpty(
					ApiHelperSupport.optString(request, "subjek"));
			String urut = ApiHelperSupport.optString(request, "urut");
			if (urut != null && !urut.trim().isEmpty()) {
				kueri.sort = urut.trim();
			}
			Long tahun = angka(request, "tahun");
			if (tahun != null) {
				kueri.year = Integer.valueOf(tahun.intValue());
			}
			Long koleksi = angka(request, "koleksiId");
			if (koleksi != null) {
				kueri.collectionId = koleksi;
			}
			kueri.page = Math.max(1, request.optInt("halaman", 1));
			kueri.pageSize = Math.min(MAKS_UKURAN_HALAMAN,
					Math.max(1, request.optInt("jumlahDataDalamSatuHalaman", 20)));

			RepositoryPublicService.SearchResult hasil = layanan.search(kueri);
			JSONArray data = new JSONArray();
			for (RepositoryPublicService.ItemCard kartu : hasil.items) {
				data.put(kartuSebagaiJson(kartu));
			}

			JSONObject jawaban = ApiHelperSupport.status("00", "Repository berhasil dimuat.");
			jawaban.put("data", data);
			jawaban.put("size", hasil.total);
			jawaban.put("halaman", kueri.page);
			jawaban.put("totalHalaman", hasil.totalPages);
			jawaban.put("hanyaBaca", true);
			return jawaban;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return ApiHelperSupport.errorResponse("Repository gagal dimuat.");
		}
	}

	public static JSONObject detail(HttpServletRequest req, JSONObject request) throws Exception {
		Tbmuser pengguna = ApiUtil.currentUser(request, req);
		if (pengguna == null) {
			return tolakTanpaToken();
		}
		if (!bolehMembaca(pengguna)) {
			return tolakTanpaIzin();
		}
		Long id = angka(request, "id");
		if (id == null) {
			return ApiHelperSupport.status("91", "Parameter id wajib diisi.");
		}
		try {
			RepositoryPublicService layanan = new RepositoryPublicService();
			RepositoryPublicService.ItemDetail rinci = layanan.findPublicItem(id);
			if (rinci == null) {
				// Item non-publik dijawab "tidak ditemukan" (bukan "ditolak")
				// supaya keberadaannya tidak bocor lewat perbedaan pesan.
				return ApiHelperSupport.status("94", "Artefak tidak ditemukan.");
			}

			JSONObject jawaban = ApiHelperSupport.status("00", "Rincian artefak berhasil dimuat.");
			JSONObject isi = kartuSebagaiJson(rinci);
			isi.put("ringkasan", rinci.abstractText);
			isi.put("penerbit", rinci.publisher);
			isi.put("lisensi", rinci.licenseUri);
			jawaban.put("data", isi);

			// Hanya berkas PDF yang dapat dibaca di aplikasi; berkas lain
			// disebutkan apa adanya sebagai "tidak dapat dibaca di aplikasi"
			// daripada memberi tombol yang tidak melakukan apa-apa.
			JSONArray berkas = new JSONArray();
			List<RepositoryPublicService.BitstreamView> daftarBerkas = rinci.files;
			if (daftarBerkas != null) {
				for (RepositoryPublicService.BitstreamView b : daftarBerkas) {
					JSONObject j = new JSONObject();
					j.put("id", b.id);
					j.put("nama", b.namaFile);
					j.put("mime", b.mimeType);
					j.put("ukuranByte", b.ukuranByte);
					boolean pdf = b.mimeType != null
							&& b.mimeType.toLowerCase(java.util.Locale.ENGLISH)
									.indexOf("pdf") >= 0;
					j.put("dapatDibaca", pdf);
					berkas.put(j);
				}
			}
			jawaban.put("berkas", berkas);
			jawaban.put("hanyaBaca", true);
			return jawaban;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return ApiHelperSupport.errorResponse("Rincian artefak gagal dimuat.");
		}
	}

	/** Satu halaman artefak sebagai gambar ber-watermark identitas pembaca. */
	public static JSONObject halaman(HttpServletRequest req, JSONObject request) throws Exception {
		Tbmuser pengguna = ApiUtil.currentUser(request, req);
		if (pengguna == null) {
			return tolakTanpaToken();
		}
		if (!bolehMembaca(pengguna)) {
			return tolakTanpaIzin();
		}
		Long berkasId = angka(request, "berkasId");
		int halaman = request.optInt("halaman", 1);
		if (berkasId == null || halaman < 1) {
			return ApiHelperSupport.status("91", "Parameter berkasId dan halaman wajib benar.");
		}
		try {
			RepositoryPublicService layanan = new RepositoryPublicService();
			// findDownloadableBitstream sudah memeriksa kebijakan akses berkas
			// DAN visibilitas publik item induknya -- pemeriksaan itu tidak
			// ditulis ulang di sini supaya aturannya tetap satu sumber.
			RepoBitstream bitstream = layanan.findDownloadableBitstream(berkasId);
			if (bitstream == null) {
				return ApiHelperSupport.status("94", "Berkas tidak ditemukan atau tidak terbuka.");
			}
			String mime = bitstream.getMimeType();
			if (mime == null || mime.toLowerCase(java.util.Locale.ENGLISH).indexOf("pdf") < 0) {
				return ApiHelperSupport.status("91",
						"Berkas ini bukan PDF sehingga belum dapat dibaca di aplikasi.");
			}
			File berkas = layanan.resolveBitstreamFile(bitstream);
			if (berkas == null || !berkas.exists()) {
				return ApiHelperSupport.status("94", "Berkas tidak tersedia di server.");
			}
			String gambar = PembacaTerlindungiUtil.halamanSebagaiBase64(berkas, halaman,
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
		}
	}

	/** Jumlah halaman satu berkas; dipakai klien untuk navigasi pembaca. */
	public static JSONObject jumlahHalaman(HttpServletRequest req, JSONObject request)
			throws Exception {
		Tbmuser pengguna = ApiUtil.currentUser(request, req);
		if (pengguna == null) {
			return tolakTanpaToken();
		}
		if (!bolehMembaca(pengguna)) {
			return tolakTanpaIzin();
		}
		Long berkasId = angka(request, "berkasId");
		if (berkasId == null) {
			return ApiHelperSupport.status("91", "Parameter berkasId wajib diisi.");
		}
		try {
			RepositoryPublicService layanan = new RepositoryPublicService();
			RepoBitstream bitstream = layanan.findDownloadableBitstream(berkasId);
			if (bitstream == null) {
				return ApiHelperSupport.status("94", "Berkas tidak ditemukan atau tidak terbuka.");
			}
			File berkas = layanan.resolveBitstreamFile(bitstream);
			if (berkas == null || !berkas.exists()) {
				return ApiHelperSupport.status("94", "Berkas tidak tersedia di server.");
			}
			JSONObject jawaban = ApiHelperSupport.status("00", "Informasi berkas berhasil dimuat.");
			jawaban.put("jumlahHalaman", PembacaTerlindungiUtil.jumlahHalaman(berkas));
			jawaban.put("hanyaBaca", true);
			return jawaban;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return ApiHelperSupport.errorResponse("Informasi berkas gagal dimuat.");
		}
	}

	private static JSONObject kartuSebagaiJson(RepositoryPublicService.ItemCard kartu)
			throws Exception {
		JSONObject j = new JSONObject();
		j.put("id", kartu.id);
		j.put("judul", kartu.title);
		j.put("penulis", kartu.authors);
		j.put("tahun", kartu.year);
		j.put("jenis", kartu.documentType);
		j.put("subjek", kartu.subjects);
		j.put("bahasa", kartu.language);
		j.put("doi", kartu.doi);
		j.put("koleksi", kartu.collectionName);
		// Tautan permanen SENGAJA disertakan: pengguna boleh membagikan
		// TAUTAN-nya, yang justru cara berbagi yang menghormati hak cipta.
		j.put("oaiIdentifier", kartu.oaiIdentifier);
		j.put("handle", kartu.dspaceHandle);
		return j;
	}
}
