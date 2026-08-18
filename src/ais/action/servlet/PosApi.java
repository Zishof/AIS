package ais.action.servlet;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.json.JSONArray;
import org.json.JSONObject;

import org.hibernate.criterion.Restrictions;

import ais.action.master.inventory.PriceTagUtil;
import ais.action.servlet.api.KantinHelper;
import ais.action.servlet.api.PosDeviceAuthApi;
import ais.action.servlet.api.PosDemoProvisionHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.DraftPembelian;
import ais.database.model.inventory.JenisProduk;
import ais.database.model.inventory.Pedagang;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.DraftPembelianAnggotaKoperasi;
import ais.database.model.koperasi.JenisAnggotaKoperasi;
import org.hibernate.criterion.Disjunction;

/**
 * <b>PosApi</b> -- endpoint JSON BERTIPE (bukan SQL mentah dari klien seperti {@code /Data}) khusus
 * untuk layar Kasir (POS) LOKAL aplikasi desktop Electron (dimuat sebagai berkas {@code file://},
 * lihat {@code pos.html} di proyek {@code desktop-pos-electron/}).
 *
 * <h3>Kenapa servlet BARU, bukan menambah action ke {@code /Data} yang sudah ada</h3>
 * <p>{@code /Data} mengasumsikan pemanggilnya sudah login lewat cookie {@code HttpSession}
 * ({@code Common.getCurrentUser(request)}) -- asumsi itu TIDAK berlaku untuk halaman {@code file://}
 * (tidak ada cookie apa pun untuk domain manapun). Otentikasi di sini memakai TOKEN
 * ({@code Authorization: Bearer <token>}, lihat {@link PosDeviceAuthApi}) yang SENGAJA diisolasi ke
 * servlet baru ini -- {@code Common.getCurrentUser}/{@code CommonCurrentSessionHelper}/{@code FilterJSP}
 * (dipakai LUAS di seluruh modul lain, bukan cuma Kantin) TIDAK disentuh sama sekali oleh fitur ini.</p>
 *
 * <h3>Aksi yang didukung (field {@code action} di body JSON POST)</h3>
 * <ul>
 *   <li>{@code login} -- SATU-SATUNYA aksi yang TIDAK butuh header {@code Authorization} (belum
 *       punya token). Body: {@code {username, password, labelPerangkat?}}. Balasan sukses membawa
 *       {@code token} mentah -- lihat JavaDoc {@link PosDeviceAuthApi#terbitkanToken}.</li>
 *   <li>{@code logout} -- mencabut token yang dipakai memanggil aksi ini (lihat
 *       {@link PosDeviceAuthApi#cabutToken}).</li>
 *   <li>{@code katalog} -- daftar kategori ({@code jenis_produk}) + produk aktif milik toko kasir yang
 *       sedang login (dikunci ke tokonya sendiri, TIDAK bisa dipalsukan lewat parameter -- sama seperti
 *       pola di {@code _pos.jsp}/{@code pos_offline_service.jsp}). Data ini yang di-CACHE lokal oleh
 *       aplikasi desktop (SQLite, {@code local-db.js}) supaya kasir tetap bisa jualan saat offline
 *       memakai data terakhir yang berhasil diambil saat online.</li>
 *   <li>{@code konfigurasi} -- persentase pajak, gerbang "wajib Sesi Kas", gerbang "cegah oversell",
 *       info toko aktif -- nilai yang sama yang dibaca {@code _pos.jsp} dari scriptlet server-side.</li>
 *   <li>{@code bayar}/{@code draft_bayar}/{@code checkBayar} -- DIDELEGASIKAN LANGSUNG ke
 *       {@link KantinHelper}, TANPA logika bisnis baru sama sekali -- method-method itu SUDAH menerima
 *       {@code Tbmuser} sebagai parameter eksplisit (bukan menariknya sendiri dari session), jadi
 *       cukup dioper hasil resolusi token di sini. Ini memastikan aturan checkout (validasi stok,
 *       diskon, saldo member, dst.) PERSIS SAMA antara jalur cookie (web/ZK) dan jalur token (POS
 *       lokal) -- satu sumber kebenaran, tidak diduplikasi. Balasannya (konvensi kode angka lama
 *       "00"/"91" milik {@code KantinHelper}) DISERAGAMKAN ke {@code status:"success"|"error"} lewat
 *       {@link #normalisasiStatusKantinHelper} SEBELUM dikirim ke klien -- lihat JavaDoc method itu
 *       soal bug nyata yang dicegahnya (checkout sukses terbaca "ditolak" tanpa normalisasi ini).</li>
 *   <li>{@code pesanan_list}/{@code batal_pesanan} -- daftar pesanan online (draft) toko kasir dan
 *       pembatalannya; verifikasi/penyelesaian pesanan reuse aksi {@code bayar} yang sudah ada.</li>
 *   <li>{@code ringkasan} -- ringkasan penjualan HARI INI (omzet, jumlah transaksi, produk terlaris)
 *       milik toko kasir -- lihat JavaDoc {@link #prosesRingkasan}.</li>
 *   <li>{@code cari_member}/{@code saldo_member}/{@code verifikasi_pin} -- dukungan pembayaran pakai
 *       saldo member dgn gerbang PIN di Layar Pelanggan (jendela monitor kedua) -- lihat JavaDoc
 *       {@link #prosesCariMember}/{@link #prosesVerifikasiPin}.</li>
 *   <li>{@code dashboard_umum}/{@code dashboard_keuangan}/{@code dashboard_produk}/{@code
 *       dashboard_pelanggan}/{@code layani_transaksi}/{@code layani_semua_transaksi} -- dasbor
 *       "Ringkasan" 4 tab (port dari {@code dashboard.jsp} web, di-scope selalu ke toko kasir yang
 *       login) -- lihat JavaDoc {@link #prosesDashboardUmum} dst.</li>
 * </ul>
 *
 * <h3>CORS</h3>
 * <p>Permintaan dari {@code file://} mengirim header {@code Origin: null}, dan karena memakai header
 * kustom {@code Authorization}, Chromium akan mengirim preflight {@code OPTIONS} lebih dulu -- karena
 * itu {@link #doOptions} WAJIB membalas header CORS yang sesuai (lihat {@link #terapkanHeaderCors}).
 * Wildcard {@code Access-Control-Allow-Origin: *} AMAN dipakai di sini (berbeda dari jalur cookie di
 * {@code Data.java}/{@code FilterJSP}) karena permintaan TIDAK memakai kredensial browser
 * ({@code credentials:'include'}) -- otentikasinya lewat header {@code Authorization}, bukan cookie,
 * jadi tidak kena batasan "wildcard ACAO tidak kompatibel dengan kredensial".</p>
 */
public class PosApi extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		terapkanHeaderCors(response);
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		proses(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		proses(request, response);
	}

	private static void terapkanHeaderCors(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
		response.setHeader("Access-Control-Max-Age", "600");
	}

	private void proses(HttpServletRequest request, HttpServletResponse response) throws IOException {
		terapkanHeaderCors(response);
		response.setContentType("application/json; charset=UTF-8");
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");

		JSONObject hasil = new JSONObject();
		try {
			JSONObject payload = bacaJsonBody(request);
			String action = payload.optString("action", "");

			if ("login".equals(action)) {
				hasil = PosDeviceAuthApi.terbitkanToken(payload.optString("username", ""),
						payload.optString("password", ""),
						payload.has("labelPerangkat") ? payload.optString("labelPerangkat", null) : null,
						request, response);
				tulisJson(response, hasil);
				return;
			}

			if ("logout".equals(action)) {
				String tokenHeader = request.getHeader("Authorization");
				if (tokenHeader != null && tokenHeader.startsWith("Bearer ")) {
					PosDeviceAuthApi.cabutToken(tokenHeader.substring(7).trim());
				}
				hasil.put("status", "success");
				tulisJson(response, hasil);
				return;
			}

			// Fitur "Alih Bahasa" -- DIKECUALIKAN dari gerbang token (bersama login/logout) karena
			// layar Login & Pengaturan Server sendiri butuh diterjemahkan SEBELUM kasir punya sesi
			// token sama sekali. Aman: kamus terjemahan bukan data sensitif/bisnis (tidak ada info
			// toko/transaksi/pengguna di baliknya), semata teks UI statis.
			if ("i18n_kamus".equals(action)) {
				prosesI18nKamus(request, payload, hasil);
				tulisJson(response, hasil);
				return;
			}

			// SEMUA aksi selain login/logout/i18n_kamus wajib token valid.
			Tbmuser tbmuser = PosDeviceAuthApi.resolveDariRequest(request);
			// Workspace JSP berjalan same-origin dengan sesi AIS. Token perangkat tetap wajib untuk
			// klien Flutter/Desktop; fallback cookie ini sengaja dibatasi ke aksi si_* dan laporan
			// analitik yang memang dipanggil oleh partial dashboard.jsp. Gerbang hak akses di bawah
			// tetap berlaku, sehingga fallback ini bukan jalan pintas otorisasi.
			boolean aksiJspDenganSesi = action.startsWith("si_")
					|| "laporan_riwayat_penjualan_analitik".equals(action);
			if ((tbmuser == null || tbmuser.getUserId() == null) && aksiJspDenganSesi) {
				tbmuser = Common.getCurrentUser(request);
			}
			if (tbmuser == null || tbmuser.getUserId() == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				hasil.put("status", "error");
				hasil.put("message", "Sesi tidak valid/kedaluwarsa. Silakan masuk kembali.");
				tulisJson(response, hasil);
				return;
			}
			if (!bolehAksesActionKantin(tbmuser, action)) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				hasil.put("status", "error");
				hasil.put("message", "Akses menu ini tidak diizinkan untuk grup pengguna Anda.");
				tulisJson(response, hasil);
				return;
			}

			// Audit oleh/olehId: klien POS Desktop/Android tidak punya sesi web, sehingga
			// AuditTimestampInterceptor tadinya mencatat "external_update". Identitas hasil
			// autentikasi token perangkat ditaruh sebagai atribut request agar SEMUA
			// simpanan pada permintaan ini tercatat atas nama kasir yang sedang login.
			request.setAttribute(ais.database.hibernate.AuditTimestampInterceptor.ATTR_PENGGUNA_POS, tbmuser);

			// Idempotensi antrean master offline (eBisnis Flutter): client_mutation_id
			// hanya disertakan MasterOffline pada mutasi master yang diantre saat offline.
			// Kiriman ulang mengembalikan respons tersimpan -- create yang di-replay tidak
			// menciptakan data ganda. Fail-open (lihat MutasiIdempotenEBisnisUtil).
			String clientMutationId = payload.optString("client_mutation_id", "").trim();
			boolean mutasiIdempoten = clientMutationId.length() > 0
					&& ais.action.servlet.api.MutasiIdempotenEBisnisUtil.aksiMasterAntrean(action);
			if (mutasiIdempoten) {
				JSONObject replay = ais.action.servlet.api.MutasiIdempotenEBisnisUtil.ambil(tbmuser, action,
						clientMutationId);
				if (replay != null) {
					tulisJson(response, replay);
					return;
				}
			}

			if ("katalog".equals(action)) {
				prosesKatalog(tbmuser, payload, hasil, request);
			} else if ("konfigurasi".equals(action)) {
				prosesKonfigurasi(tbmuser, hasil);
			} else if ("ebisnis_menu_tree".equals(action)) {
				prosesEbisnisMenuTree(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "ebisnis_menu_tree");
			} else if ("daftar_toko_saya".equals(action)) {
				KantinHelper.daftarTokoSaya(tbmuser, hasil);
				normalisasiStatusKantinHelper(hasil, "daftar_toko_saya");
			} else if ("pilih_toko_aktif".equals(action)) {
				KantinHelper.pilihTokoAktif(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "pilih_toko_aktif");
			} else if ("ebisnis_role_list".equals(action)) {
				KantinHelper.ebisnisRoleList(tbmuser, hasil);
				normalisasiStatusKantinHelper(hasil, "ebisnis_role_list");
			} else if ("ebisnis_role_menu_ambil".equals(action)) {
				KantinHelper.ebisnisRoleMenuAmbil(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "ebisnis_role_menu_ambil");
			} else if ("ebisnis_role_menu_simpan".equals(action)) {
				KantinHelper.ebisnisRoleMenuSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "ebisnis_role_menu_simpan");
			} else if ("bayar".equals(action)) {
				KantinHelper.bayar(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "checkout");
			} else if ("draft_bayar".equals(action)) {
				KantinHelper.draft_bayar(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "checkout");
			} else if ("checkBayar".equals(action)) {
				KantinHelper.checkBayar(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "checkBayar");
			} else if ("pesanan_list".equals(action)) {
				prosesPesananList(tbmuser, payload, hasil);
			} else if ("batal_pesanan".equals(action)) {
				prosesBatalPesanan(tbmuser, payload, hasil);
			} else if ("pesanan_hitung_ulang".equals(action)) {
				KantinHelper.pesananHitungUlang(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "pesanan_hitung_ulang");
			} else if ("ringkasan".equals(action)) {
				prosesRingkasan(tbmuser, payload, hasil);
			} else if ("cari_member".equals(action)) {
				prosesCariMember(payload, hasil);
			} else if ("cara_bayar_list".equals(action)) {
				prosesCaraBayarList(payload, hasil);
			} else if ("cara_bayar_list_semua".equals(action)) {
				KantinHelper.caraBayarListSemua(hasil);
				normalisasiStatusKantinHelper(hasil, "cara_bayar_list_semua");
			} else if ("saldo_member".equals(action)) {
				KantinHelper.tabungan(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "saldo_member");
			} else if ("sesi_kas_status".equals(action)) {
				KantinHelper.sesiKasStatus(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "sesi_kas");
			} else if ("sesi_kas_list".equals(action)) {
				KantinHelper.sesiKasDaftar(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "sesi_kas");
			} else if ("sesi_kas_koreksi".equals(action)) {
				if (!bolehSupervisorAtauAdmin(tbmuser)) {
					hasil.put("status", "91");
					hasil.put("description", "Koreksi sesi kas hanya dapat dilakukan oleh supervisor.");
				} else {
					KantinHelper.sesiKasKoreksi(tbmuser, payload, hasil);
					normalisasiStatusKantinHelper(hasil, "sesi_kas");
				}
			} else if ("sesi_kas_rekonsiliasi_simpan".equals(action)) {
				if (!bolehSupervisorAtauAdmin(tbmuser)) {
					hasil.put("status", "91");
					hasil.put("description", "Rekonsiliasi sesi kas hanya dapat dilakukan oleh supervisor.");
				} else {
					KantinHelper.sesiKasRekonsiliasiSimpan(tbmuser, payload, hasil);
					normalisasiStatusKantinHelper(hasil, "sesi_kas");
				}
			} else if ("sesi_kas_buka".equals(action)) {
				KantinHelper.sesiKasBuka(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "sesi_kas");
			} else if ("sesi_kas_tutup".equals(action)) {
				// Koreksi modal awal adalah kewenangan supervisor/admin. Gerbang ini
				// wajib berada di server agar tidak dapat dilewati dengan memanggil API
				// secara langsung dari klien yang dimodifikasi.
				if ((!payload.isNull("modal_awal_koreksi") || !payload.isNull("penjualan_tunai_koreksi")) && !bolehSupervisorAtauAdmin(tbmuser)) {
					hasil.put("status", "91");
					hasil.put("description", "Koreksi nominal sesi kas hanya dapat dilakukan oleh supervisor.");
				} else {
					KantinHelper.sesiKasTutup(tbmuser, payload, hasil);
					normalisasiStatusKantinHelper(hasil, "sesi_kas");
				}
			} else if ("topup_saldo".equals(action)) {
				KantinHelper.topupSaldo(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "topup_saldo");
			} else if ("akun_ganti_password".equals(action)) {
				KantinHelper.gantiPasswordSendiri(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "akun_ganti_password");
			} else if ("akun_tambah".equals(action)) {
				KantinHelper.tambahAkunKasir(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "akun_tambah");
			} else if ("pedagang_list".equals(action)) {
				KantinHelper.pedagangList(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "pedagang_list");
			} else if ("pedagang_ubah".equals(action)) {
				KantinHelper.pedagangUbah(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "pedagang_ubah");
			} else if ("toko_profil_ambil".equals(action)) {
				KantinHelper.tokoProfilAmbil(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "toko_profil_ambil");
			} else if ("toko_profil_simpan".equals(action)) {
				KantinHelper.tokoProfilSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "toko_profil_simpan");
			} else if (action.startsWith("grup_produk_")) {
				// Grup Produk (harga terpusat lintas toko) -- gate menu di
				// bolehAksesActionKantin + aksi CRUD granular di helper (dua lapis).
				ais.action.servlet.api.GrupProdukApiHelper.proses(action, tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, action);
			} else if (action.startsWith("revisi_")) {
				// Riwayat revisi Envers per baris (padanan GenericRevisiHelper):
				// daftar/detail semua user login; pulihkan admin-only (self-guarded).
				ais.action.servlet.api.RevisiApiHelper.proses(action, tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, action);
			} else if (action.startsWith("toko_kelola_") || "unit_usaha_katalog".equals(action)) {
				// CRUD Toko utk Desktop/Android + katalog unit usaha -- admin-only,
				// self-guarded di TokoApiHelper (padanan gate isAdmin JSP / admin ZK).
				ais.action.servlet.api.TokoApiHelper.proses(action, tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, action);
			} else if ("pos_demo_status".equals(action)) {
				PosDemoProvisionHelper.status(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "pos_demo_status");
			} else if ("pos_demo_seed_products".equals(action)) {
				PosDemoProvisionHelper.mulaiProduk(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "pos_demo_seed_products");
			} else if ("pos_demo_seed_products_unit_usaha".equals(action)) {
				PosDemoProvisionHelper.mulaiProdukUnitUsaha(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "pos_demo_seed_products_unit_usaha");
			} else if ("pos_demo_seed_transactions".equals(action)) {
				PosDemoProvisionHelper.mulaiTransaksi(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "pos_demo_seed_transactions");
			} else if ("pos_demo_seed_customers".equals(action)) {
				PosDemoProvisionHelper.mulaiPelanggan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "pos_demo_seed_customers");
			} else if ("produk_simpan".equals(action)) {
				KantinHelper.produkSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "produk_simpan");
			} else if ("produk_ekspor_excel".equals(action)) {
				KantinHelper.produkEksporExcel(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "produk_ekspor_excel");
			} else if ("produk_impor_excel_preview".equals(action)) {
				KantinHelper.produkImporExcelPreview(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "produk_impor_excel_preview");
			} else if ("produk_impor_excel_komit".equals(action)) {
				KantinHelper.produkImporExcelKomit(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "produk_impor_excel_komit");
			} else if ("stok_hitung_ulang".equals(action)) {
				KantinHelper.stokHitungUlang(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "stok_hitung_ulang");
			} else if ("produk_nonaktifkan_tak_diimpor".equals(action)) {
				KantinHelper.produkNonaktifkanTakDiimpor(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "produk_nonaktifkan_tak_diimpor");
			} else if ("produk_hapus_nonaktif_tak_terpakai".equals(action)) {
				KantinHelper.produkHapusNonaktifTakTerpakai(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "produk_hapus_nonaktif_tak_terpakai");
			} else if ("produk_hapus_tak_ada_transaksi".equals(action)) {
				KantinHelper.produkHapusTakAdaTransaksi(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "produk_hapus_tak_ada_transaksi");
			} else if ("produk_grid_ekspor_excel".equals(action)) {
				KantinHelper.produkGridEksporExcel(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "produk_grid_ekspor_excel");
			} else if ("produk_impor_excel".equals(action)) {
				KantinHelper.produkImporExcel(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "produk_impor_excel");
			} else if ("produk_foto_list".equals(action)) {
				KantinHelper.produkFotoList(tbmuser, payload, hasil);
				if (hasil.has("data")) {
					JSONArray daftarFoto = hasil.getJSONArray("data");
					for (int iFoto = 0; iFoto < daftarFoto.length(); iFoto++) {
						JSONObject jFoto = daftarFoto.getJSONObject(iFoto);
						jFoto.put("urlGambar", buildUrlGambarFotoProduk(request, jFoto.getLong("id")));
					}
				}
				normalisasiStatusKantinHelper(hasil, "produk_foto_list");
			} else if ("produk_foto_upload".equals(action)) {
				KantinHelper.produkFotoUpload(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "produk_foto_upload");
			} else if ("produk_foto_hapus".equals(action)) {
				KantinHelper.produkFotoHapus(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "produk_foto_hapus");
			} else if ("so_sesi_list".equals(action)) {
				KantinHelper.soSesiList(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "so_sesi_list");
			} else if ("so_sesi_mulai".equals(action)) {
				KantinHelper.soSesiMulai(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "so_sesi_mulai");
			} else if ("so_sesi_selesai".equals(action)) {
				KantinHelper.soSesiSelesai(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "so_sesi_selesai");
			} else if ("so_produk_scan".equals(action)) {
				KantinHelper.soProdukScan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "so_produk_scan");
			} else if ("so_simpan".equals(action)) {
				KantinHelper.soSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "so_simpan");
			} else if ("so_ringkasan".equals(action)) {
				KantinHelper.soRingkasan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "so_ringkasan");
			} else if ("kedaluwarsa_list".equals(action)) {
				KantinHelper.kedaluwarsaList(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "kedaluwarsa_list");
			} else if ("produk_batch_simpan".equals(action)) {
				KantinHelper.produkBatchSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "produk_batch_simpan");
			} else if ("produk_batch_produk_list".equals(action)) {
				KantinHelper.produkBatchProdukList(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "produk_batch_produk_list");
			} else if ("so_riwayat".equals(action)) {
				KantinHelper.soRiwayat(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "so_riwayat");
			} else if ("so_ekspor_excel".equals(action)) {
				KantinHelper.soEksporExcel(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "so_ekspor_excel");
			} else if ("so_impor_excel".equals(action)) {
				KantinHelper.soImporExcel(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "so_impor_excel");
			} else if ("mutasi_stok_simpan".equals(action)) {
				KantinHelper.mutasiStokSimpan(tbmuser, payload, hasil);
				// Gap-closure: method ini py KONTRAK 3-STATE ("00"=sukses, "91"=gagal keras,
				// "92"=butuh pilih manual, lihat JavaDoc KantinHelper.mutasiStokSimpan) -- BEDA dari
				// checkout yg cuma py 2 state (normalisasiStatusKantinHelper dirancang utk itu, akan
				// meratakan "92" jadi status:"error" generik & MEMBUANG field data spt kandidat[]/
				// butuhPilihManual krn panggilPosApi/ApiClient.aksi di klien HANYA meneruskan body saat
				// status:"success"). "92" BUKAN error sistem -- klien harus tetap terima body-nya utuh
				// utk menampilkan picker manual, jadi dipetakan ke "success" jg (bukan diklasifikasi).
				// "91"/status tak dikenal TETAP lewat classifier standar spt sebelumnya.
				String statusAsliMutasi = hasil.optString("status", "");
				if ("00".equals(statusAsliMutasi) || "92".equals(statusAsliMutasi)) {
					hasil.put("status", "success");
				} else {
					normalisasiStatusKantinHelper(hasil, "mutasi_stok_simpan");
				}
			} else if ("mutasi_stok_list".equals(action)) {
				KantinHelper.mutasiStokList(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "mutasi_stok_list");
			} else if ("mutasi_stok_toko_list".equals(action)) {
				KantinHelper.mutasiStokTokoList(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "mutasi_stok_toko_list");
			} else if ("mutasi_stok_produk_list".equals(action)) {
				KantinHelper.mutasiStokProdukList(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "mutasi_stok_produk_list");
			} else if ("stok_mutasi_ledger".equals(action)) {
				KantinHelper.stokMutasiLedger(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "stok_mutasi_ledger");
			} else if ("produk_rekonsiliasi_ledger".equals(action)) {
				KantinHelper.produkRekonsiliasiLedger(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "produk_rekonsiliasi_ledger");
			} else if ("produk_mutasi_ringkasan".equals(action)) {
				KantinHelper.produkMutasiRingkasan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "produk_mutasi_ringkasan");
			} else if ("stok_dashboard".equals(action)) {
				KantinHelper.stokDashboard(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "stok_dashboard");
			} else if ("price_tag_list_produk".equals(action)) {
				KantinHelper.priceTagListProduk(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "price_tag_list_produk");
			} else if ("produk_statistik".equals(action)) {
				KantinHelper.produkStatistik(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "produk_statistik");
			} else if ("produk_statistik_detail".equals(action)) {
				KantinHelper.produkStatistikDetail(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "produk_statistik_detail");
			} else if ("produk_duplikat_cari".equals(action)) {
				if (!bolehSupervisorAtauAdmin(tbmuser)) {
					hasil.put("status", "error");
					hasil.put("message", "Hanya supervisor/admin yang boleh membersihkan produk duplikat.");
				} else {
					KantinHelper.produkDuplikatCari(tbmuser, payload, hasil);
					normalisasiStatusKantinHelper(hasil, "produk_duplikat_cari");
				}
			} else if ("produk_duplikat_hapus".equals(action)) {
				if (!bolehSupervisorAtauAdmin(tbmuser)) {
					hasil.put("status", "error");
					hasil.put("message", "Hanya supervisor/admin yang boleh membersihkan produk duplikat.");
				} else {
					KantinHelper.produkDuplikatHapus(tbmuser, payload, hasil);
					normalisasiStatusKantinHelper(hasil, "produk_duplikat_hapus");
				}
			} else if ("anggota_statistik".equals(action)) {
				KantinHelper.anggotaStatistik(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "anggota_statistik");
			} else if ("transaksi_statistik".equals(action)) {
				KantinHelper.transaksiStatistik(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "transaksi_statistik");
			} else if ("peringkat_mitra".equals(action)) {
				KantinHelper.peringkatMitra(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "peringkat_mitra");
			} else if ("resep_hpp_margin".equals(action)) {
				KantinHelper.resepHppMargin(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "resep_hpp_margin");
			} else if ("ramalan_penjualan".equals(action)) {
				KantinHelper.ramalanPenjualan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "ramalan_penjualan");
			} else if ("monitor_promo_cashback".equals(action)) {
				KantinHelper.monitorPromoCashback(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "monitor_promo_cashback");
			} else if ("kepatuhan_operasional".equals(action)) {
				KantinHelper.kepatuhanOperasional(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "kepatuhan_operasional");
			} else if ("error_log_kirim".equals(action)) {
				KantinHelper.errorLogKirim(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "error_log_kirim");
			} else if ("layar_pelanggan_kirim".equals(action)) {
				KantinHelper.layarPelangganKirim(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "layar_pelanggan_kirim");
			} else if ("layar_pelanggan_ambil".equals(action)) {
				KantinHelper.layarPelangganAmbil(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "layar_pelanggan_ambil");
			} else if ("layar_pelanggan_slide_list".equals(action)) {
				KantinHelper.layarPelangganSlideList(tbmuser, payload, hasil);
				if (hasil.has("data")) {
					JSONArray daftarSlide = hasil.getJSONArray("data");
					for (int iSlide = 0; iSlide < daftarSlide.length(); iSlide++) {
						JSONObject jSlide = daftarSlide.getJSONObject(iSlide);
						jSlide.put("urlGambar", buildUrlGambarLayarPelangganSlide(request, jSlide.getLong("id")));
					}
				}
				normalisasiStatusKantinHelper(hasil, "layar_pelanggan_slide_list");
			} else if ("layar_pelanggan_slide_upload".equals(action)) {
				KantinHelper.layarPelangganSlideUpload(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "layar_pelanggan_slide_upload");
			} else if ("layar_pelanggan_slide_ubah".equals(action)) {
				KantinHelper.layarPelangganSlideUbah(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "layar_pelanggan_slide_ubah");
			} else if ("layar_pelanggan_slide_hapus".equals(action)) {
				KantinHelper.layarPelangganSlideHapus(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "layar_pelanggan_slide_hapus");
			} else if ("layar_pelanggan_screensaver_config_ambil".equals(action)) {
				KantinHelper.layarPelangganScreensaverConfigAmbil(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "layar_pelanggan_screensaver_config_ambil");
			} else if ("layar_pelanggan_screensaver_config_simpan".equals(action)) {
				KantinHelper.layarPelangganScreensaverConfigSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "layar_pelanggan_screensaver_config_simpan");
			} else if ("layar_pelanggan_slide_untuk_tampil".equals(action)) {
				KantinHelper.layarPelangganSlideUntukTampil(tbmuser, payload, hasil);
				if (hasil.has("slides")) {
					JSONArray daftarSlide = hasil.getJSONArray("slides");
					for (int iSlide = 0; iSlide < daftarSlide.length(); iSlide++) {
						JSONObject jSlide = daftarSlide.getJSONObject(iSlide);
						jSlide.put("urlGambar", buildUrlGambarLayarPelangganSlide(request, jSlide.getLong("id")));
					}
				}
				normalisasiStatusKantinHelper(hasil, "layar_pelanggan_slide_untuk_tampil");
			} else if ("survey_kepuasan_simpan".equals(action)) {
				KantinHelper.surveyKepuasanSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "survey_kepuasan_simpan");
			} else if ("anggota_list".equals(action)) {
				KantinHelper.anggotaList(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "anggota_list");
			} else if ("anggota_sync_list".equals(action)) {
				KantinHelper.anggotaSyncList(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "anggota_sync_list");
			} else if ("anggota_transaksi_terbaru".equals(action)) {
				KantinHelper.anggotaTransaksiTerbaru(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "anggota_transaksi_terbaru");
			} else if ("anggota_simpan".equals(action)) {
				KantinHelper.anggotaSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "anggota_simpan");
			} else if ("anggota_simpan_cepat".equals(action)) {
				KantinHelper.anggotaSimpanCepat(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "anggota_simpan_cepat");
			} else if ("anggota_hapus".equals(action)) {
				KantinHelper.anggotaHapus(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "anggota_hapus");
			} else if ("jenis_anggota_list".equals(action)) {
				KantinHelper.jenisAnggotaList(hasil);
				normalisasiStatusKantinHelper(hasil, "jenis_anggota_list");
			} else if ("jenis_anggota_list_admin".equals(action)) {
				KantinHelper.jenisAnggotaListAdmin(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "jenis_anggota_list_admin");
			} else if ("jenis_anggota_simpan".equals(action)) {
				KantinHelper.jenisAnggotaSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "jenis_anggota_simpan");
			} else if ("jenis_anggota_hapus".equals(action)) {
				KantinHelper.jenisAnggotaHapus(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "jenis_anggota_hapus");
			} else if ("tipe_anggota_list".equals(action)) {
				KantinHelper.tipeAnggotaList(hasil);
				normalisasiStatusKantinHelper(hasil, "tipe_anggota_list");
			} else if ("tipe_anggota_list_admin".equals(action)) {
				KantinHelper.tipeAnggotaListAdmin(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "tipe_anggota_list_admin");
			} else if ("tipe_anggota_simpan".equals(action)) {
				KantinHelper.tipeAnggotaSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "tipe_anggota_simpan");
			} else if ("tipe_anggota_hapus".equals(action)) {
				KantinHelper.tipeAnggotaHapus(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "tipe_anggota_hapus");
			} else if ("deposit_list".equals(action)) {
				KantinHelper.depositList(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "deposit_list");
			} else if ("deposit_ubah".equals(action)) {
				KantinHelper.depositUbah(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "deposit_ubah");
			} else if ("deposit_hapus".equals(action)) {
				KantinHelper.depositHapus(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "deposit_hapus");
			} else if ("cara_bayar_list_admin".equals(action)) {
				KantinHelper.caraBayarListAdmin(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "cara_bayar_list_admin");
			} else if ("cara_bayar_simpan".equals(action)) {
				KantinHelper.caraBayarSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "cara_bayar_simpan");
			} else if ("cara_bayar_hapus".equals(action)) {
				KantinHelper.caraBayarHapus(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "cara_bayar_hapus");
			} else if ("jenis_produk_list".equals(action)) {
				ais.action.servlet.api.JenisProdukApiHelper.jenisProdukList(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "jenis_produk_list");
			} else if ("jenis_produk_simpan".equals(action)) {
				ais.action.servlet.api.JenisProdukApiHelper.jenisProdukSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "jenis_produk_simpan");
			} else if ("jenis_produk_hapus".equals(action)) {
				ais.action.servlet.api.JenisProdukApiHelper.jenisProdukHapus(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "jenis_produk_hapus");
			} else if ("kebijakan_retur_list".equals(action)) {
				ais.action.servlet.api.KebijakanReturApiHelper.list(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "kebijakan_retur_list");
			} else if ("kebijakan_retur_simpan".equals(action)) {
				ais.action.servlet.api.KebijakanReturApiHelper.simpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "kebijakan_retur_simpan");
			} else if ("kebijakan_retur_hapus".equals(action)) {
				ais.action.servlet.api.KebijakanReturApiHelper.hapus(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "kebijakan_retur_hapus");
			} else if ("akun_list".equals(action)) {
				ais.action.servlet.api.JenisProdukApiHelper.akunList(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "akun_list");
			} else if ("mutasi_tabungan_list".equals(action)) {
				KantinHelper.mutasiTabunganList(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "mutasi_tabungan_list");
			} else if ("mutasi_hutang_list".equals(action)) {
				KantinHelper.mutasiHutangList(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "mutasi_hutang_list");
			} else if ("pembantu_piutang_list".equals(action)) {
				KantinHelper.pembantuPiutangList(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "pembantu_piutang_list");
			} else if ("hutang_bayar_simpan".equals(action)) {
				KantinHelper.hutangBayarSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "hutang_bayar_simpan");
			} else if ("hutang_bayar_hapus".equals(action)) {
				KantinHelper.hutangBayarHapus(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "hutang_bayar_hapus");
			} else if ("notifikasi_list".equals(action)) {
				KantinHelper.notifikasiList(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "notifikasi_list");
			} else if ("notifikasi_hapus".equals(action)) {
				KantinHelper.notifikasiHapus(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "notifikasi_hapus");
			} else if ("sinkron_referensi".equals(action)) {
				KantinHelper.sinkronReferensi(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "sinkron_referensi");
			} else if ("sinkron_mahasiswa".equals(action)) {
				KantinHelper.sinkronMahasiswa(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "sinkron_mahasiswa");
			} else if ("sinkron_siswa".equals(action)) {
				KantinHelper.sinkronSiswa(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "sinkron_siswa");
			} else if ("sinkron_dosen".equals(action)) {
				KantinHelper.sinkronDosen(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "sinkron_dosen");
			} else if ("sinkron_guru".equals(action)) {
				KantinHelper.sinkronGuru(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "sinkron_guru");
			} else if ("sinkron_pegawai".equals(action)) {
				KantinHelper.sinkronPegawai(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "sinkron_pegawai");
			} else if ("diskon_list".equals(action)) {
				KantinHelper.diskonList(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "diskon_list");
			} else if ("diskon_simpan".equals(action)) {
				KantinHelper.diskonSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "diskon_simpan");
			} else if ("pencairan_diskon_list".equals(action)) {
				KantinHelper.pencairanDiskonList(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "pencairan_diskon_list");
			} else if ("pencairan_diskon_simpan".equals(action)) {
				KantinHelper.pencairanDiskonSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "pencairan_diskon_simpan");
			} else if ("pencairan_diskon_hapus".equals(action)) {
				KantinHelper.pencairanDiskonHapus(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "pencairan_diskon_hapus");
			} else if ("pencairan_diskon_saldo_member".equals(action)) {
				KantinHelper.pencairanDiskonSaldoMember(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "pencairan_diskon_saldo_member");
			} else if ("kulakan_list".equals(action)) {
				KantinHelper.kulakanList(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "kulakan_list");
			} else if ("kulakan_simpan".equals(action)) {
				KantinHelper.kulakanSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "kulakan_simpan");
			} else if ("kulakan_faktur_simpan".equals(action)) {
				KantinHelper.kulakanFakturSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "kulakan_faktur_simpan");
			} else if ("kulakan_faktur_list".equals(action)) {
				KantinHelper.kulakanFakturList(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "kulakan_faktur_list");
			} else if ("kulakan_faktur_batal".equals(action)) {
				// Pembatalan faktur kulakan -- supervisor / hak hapus kulakan
				// (self-guarded bolehAksiCrud di helper); stok & batch dibalikkan.
				KantinHelper.kulakanFakturBatal(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "kulakan_faktur_batal");
			} else if ("kulakan_faktur_detail".equals(action)) {
				KantinHelper.kulakanFakturDetail(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "kulakan_faktur_detail");
			} else if ("penyedia_list".equals(action)) {
				KantinHelper.penyediaList(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "penyedia_list");
			} else if ("penyedia_simpan".equals(action)) {
				KantinHelper.penyediaSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "penyedia_simpan");
			} else if ("penyedia_list_admin".equals(action)) {
				KantinHelper.penyediaListAdmin(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "penyedia_list_admin");
			} else if ("penyedia_hapus".equals(action)) {
				KantinHelper.penyediaHapus(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "penyedia_hapus");
			} else if ("retur_pembelian_list".equals(action)) {
				KantinHelper.returPembelianList(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "retur_pembelian_list");
			} else if ("retur_pembelian_simpan".equals(action)) {
				KantinHelper.returPembelianSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "retur_pembelian_simpan");
			} else if ("retur_pembelian_hapus".equals(action)) {
				KantinHelper.returPembelianHapus(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "retur_pembelian_hapus");
			} else if ("retur_penjualan_list".equals(action)) {
				KantinHelper.returPenjualanList(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "retur_penjualan_list");
			} else if ("retur_penjualan_simpan".equals(action)) {
				KantinHelper.returPenjualanSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "retur_penjualan_simpan");
			} else if ("retur_penjualan_ubah".equals(action)) {
				KantinHelper.returPenjualanUbah(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "retur_penjualan_ubah");
			} else if ("retur_penjualan_hapus".equals(action)) {
				KantinHelper.returPenjualanHapus(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "retur_penjualan_hapus");
			} else if ("batalkan_transaksi".equals(action)) {
				KantinHelper.batalkanTransaksi(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "batalkan_transaksi");
			} else if ("edit_transaksi".equals(action)) {
				KantinHelper.editTransaksi(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "edit_transaksi");
			} else if ("edit_transaksi_kasir_cari".equals(action)) {
				KantinHelper.editTransaksiKasirCari(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "edit_transaksi_kasir_cari");
			} else if ("diskon_evaluasi".equals(action)) {
				KantinHelper.diskonEvaluasi(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "diskon_evaluasi");
			} else if ("diskon_grup_list".equals(action)) {
				ais.action.servlet.api.DiskonGrupHelper.list(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, action);
			} else if ("diskon_grup_detail".equals(action)) {
				ais.action.servlet.api.DiskonGrupHelper.detail(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, action);
			} else if ("diskon_grup_opsi_member".equals(action)) {
				ais.action.servlet.api.DiskonGrupHelper.opsiMember(hasil);
				normalisasiStatusKantinHelper(hasil, action);
			} else if ("diskon_grup_produk_cari".equals(action)) {
				ais.action.servlet.api.DiskonGrupHelper.cariProduk(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, action);
			} else if ("diskon_grup_produk_resolve".equals(action)) {
				ais.action.servlet.api.DiskonGrupHelper.resolveProduk(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, action);
			} else if ("diskon_grup_simpan".equals(action)) {
				ais.action.servlet.api.DiskonGrupHelper.simpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, action);
			} else if ("diskon_manual_list".equals(action)) {
				KantinHelper.diskonManualList(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "diskon_manual_list");
			} else if ("pesanan_online_baru".equals(action)) {
				KantinHelper.pesananOnlineBaru(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "pesanan_online_baru");
			} else if ("verifikasi_pin".equals(action)) {
				prosesVerifikasiPin(payload, hasil);
			} else if ("dashboard_umum".equals(action)) {
				prosesDashboardUmum(tbmuser, payload, hasil);
			} else if ("layani_transaksi".equals(action)) {
				prosesLayaniTransaksi(tbmuser, payload, hasil);
			} else if ("layani_semua_transaksi".equals(action)) {
				prosesLayaniSemuaTransaksi(tbmuser, payload, hasil);
			} else if ("dashboard_keuangan".equals(action)) {
				prosesDashboardKeuangan(tbmuser, payload, hasil);
			} else if ("dashboard_produk".equals(action)) {
				prosesDashboardProduk(tbmuser, payload, hasil);
			} else if ("dashboard_pelanggan".equals(action)) {
				prosesDashboardPelanggan(tbmuser, payload, hasil);
			} else if ("laporan_katalog".equals(action)) {
				hasil.put("status", "success");
				hasil.put("kategori", ais.action.master.koperasi.helper.LaporanKatalogData.katalog());
			} else if ("laporan_keuangan_katalog".equals(action)) {
				hasil.put("status", "success");
				hasil.put("kategori", ais.action.master.koperasi.helper.LaporanKatalogData.katalogKeuangan());
				JSONArray pendukung = new JSONArray();
				JSONObject akun = new JSONObject();
				akun.put("id", "akun_perkiraan");
				akun.put("judul", "Akun / Perkiraan");
				akun.put("keterangan", "Bagan akun berjenjang dalam bentuk pohon.");
				akun.put("url", Common.ROOT + "/pages/master/akunting/akun.zul");
				pendukung.put(akun);
				JSONObject hpp = new JSONObject();
				hpp.put("id", "posting_hpp");
				hpp.put("judul", "Posting HPP");
				hpp.put("keterangan", "Pratinjau dan posting beban pokok penjualan ke jurnal.");
				hpp.put("url", Common.ROOT + "/pages/master/koperasi/posting_hpp_kantin.zul");
				pendukung.put(hpp);
				JSONObject penjualan = new JSONObject();
				penjualan.put("id", "posting_penjualan");
				penjualan.put("judul", "Posting Penjualan");
				penjualan.put("keterangan", "Pratinjau dan posting pendapatan penjualan ke jurnal.");
				penjualan.put("url", Common.ROOT + "/pages/master/koperasi/posting_penjualan_kantin.zul");
				pendukung.put(penjualan);
				hasil.put("pendukung", pendukung);
			} else if ("laporan_keuangan_pendukung".equals(action)) {
				prosesLaporanKeuanganPendukung(payload, hasil);
			} else if ("laporan_jalankan".equals(action)) {
				prosesLaporanJalankan(request, tbmuser, payload, hasil);
			} else if ("laporan_pdf".equals(action)) {
				prosesLaporanPdf(request, tbmuser, payload, hasil);
			} else if ("detail_transaksi".equals(action)) {
				prosesDetailTransaksi(tbmuser, payload, hasil);
			} else if ("laporan_order_list".equals(action)) {
				prosesLaporanOrderList(tbmuser, payload, hasil);
			} else if ("transaksi_backup_toko_list".equals(action)) {
				prosesTransaksiBackupTokoList(tbmuser, payload, hasil);
			} else if ("transaksi_backup_ack".equals(action)) {
				prosesTransaksiBackupAck(tbmuser, payload, hasil);
			} else if ("transaksi_backup_status".equals(action)) {
				prosesTransaksiBackupStatus(tbmuser, payload, hasil);
			} else if ("laporan_riwayat_penjualan_analitik".equals(action)) {
				prosesLaporanRiwayatPenjualanAnalitik(tbmuser, payload, hasil);
			} else if ("laporan_sesi_list".equals(action)) {
				prosesLaporanSesiList(tbmuser, payload, hasil);
			} else if ("laporan_payment_list".equals(action)) {
				prosesLaporanPaymentList(tbmuser, payload, hasil);
			} else if ("laporan_penjualan_kasir_list".equals(action)) {
				prosesLaporanPenjualanKasirList(tbmuser, payload, hasil);
			} else if ("laporan_penjualan_kasir_detail".equals(action)) {
				prosesLaporanPenjualanKasirDetail(tbmuser, payload, hasil);
			} else if ("laporan_penerimaan_kasir_list".equals(action)) {
				prosesLaporanPenerimaanKasirList(tbmuser, payload, hasil);
			} else if ("laporan_penerimaan_kasir_detail".equals(action)) {
				prosesLaporanPenerimaanKasirDetail(tbmuser, payload, hasil);
			} else if ("laporan_transaksi_per_kasir".equals(action)) {
				prosesLaporanTransaksiPerKasir(tbmuser, payload, hasil);
			} else if ("laporan_transaksi_per_kasir_detail".equals(action)) {
				prosesLaporanTransaksiPerKasirDetail(tbmuser, payload, hasil);
			} else if (!prosesAksiTambahan(action, tbmuser, payload, hasil, request, response)) {
				hasil.put("status", "error");
				hasil.put("message", "Aksi tidak dikenal: " + action);
			}

			// Eksekusi pertama sukses -> simpan responsnya utk replay kiriman ulang.
			if (mutasiIdempoten && ais.action.servlet.api.MutasiIdempotenEBisnisUtil.responsSukses(hasil)) {
				ais.action.servlet.api.MutasiIdempotenEBisnisUtil.simpan(tbmuser, action, clientMutationId, hasil);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit PosApi.proses");
			hasil = new JSONObject();
			try {
				hasil.put("status", "error");
				hasil.put("judul", "Proses belum dapat diselesaikan");
				hasil.put("message", "Data belum berubah. Silakan muat ulang halaman, periksa kembali data yang diisi, lalu coba sekali lagi.");
				hasil.put("kode", "KESALAHAN_SISTEM");
				hasil.put("teknis", detailTeknis(e));
				hasil.put("solusi", new JSONArray()
						.put("Muat ulang halaman dan periksa kembali data yang akan diproses.")
						.put("Jangan mengulangi pembayaran sebelum memastikan transaksi sebelumnya belum tercatat.")
						.put("Jika kendala berulang, buka Detail Error lalu salin informasinya untuk admin/developer."));
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) PosApi.proses"); }
		}
		tulisJson(response, hasil);
	}

	/**
	 * Katalog kategori+produk milik toko kasir yang sedang login -- lihat JavaDoc kelas soal peran
	 * data ini sebagai sumber cache offline aplikasi desktop. Sesi Hibernate dedicated (POLA B, lihat
	 * {@link KantinHelper}) karena dipanggil dari konteks servlet murni, bukan siklus hidup ZK.
	 *
	 * <p>Field {@code gambarUrl} per produk HANYA diisi bila {@link Produk#getAdaFileGambar()}
	 * bernilai true (produk memang punya foto ter-upload, lihat {@code ProdukPunyaGambarFotoHelper})
	 * -- disengaja utk menghindari mengunduh placeholder generik server (mis. ikon "tidak ada gambar")
	 * sbg gambar produk di aplikasi desktop; produk tanpa foto asli dibiarkan {@code null} supaya
	 * klien menampilkan avatar warna+inisial-nya sendiri, bukan placeholder generik yang tak menarik.
	 * URL dibangun MANUAL dari {@code request} (bukan {@code CommonMedia.getMediaProduk}, yang
	 * bergantung pada state request-scoped yang belum tentu terisi di konteks servlet BARU ini) --
	 * lihat {@link #buildUrlGambarProduk}. Endpoint {@code /AmbilMediaProduk} yang dirujuk TIDAK
	 * butuh otentikasi apa pun (sudah diverifikasi publik), jadi aman dipakai langsung sbg
	 * {@code <img src>} atau diunduh manual oleh aplikasi desktop tanpa header token.
	 */
	private void prosesKatalog(Tbmuser tbmuser, JSONObject payload, JSONObject hasil, HttpServletRequest request) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		String keyword = payload.optString("keyword", null);
		if (keyword != null && keyword.trim().isEmpty()) keyword = null;
		// Gap-closure "opsi tampilkan barang milik semua toko atau toko == null" (layar admin Produk) --
		// lihat JavaDoc PriceTagUtil.listProduk(session,tokoId,q,semuaToko,adminGlobal) soal batasan
		// keamanan (akun terikat toko TIDAK PERNAH melihat data toko LAIN, hanya orphan toko=null).
		boolean semuaTokoDiminta = payload.optBoolean("semuaToko", false);
		boolean adminGlobal = tbmuser.getPedagang() == null;
		// Gap-closure "Jenis Item" (Produk vs Bahan Baku) -- "JUAL"/"BAHAN" menyaring server-side
		// (lihat PriceTagUtil.listProduk 6-argumen), kosong/tak dikirim = tanpa filter (perilaku
		// lama, dipakai layar admin Produk yang harus melihat SEMUA baris).
		String jenisItemFilter = payload.optString("jenisItem", "").trim().toUpperCase();
		if (jenisItemFilter.isEmpty()) jenisItemFilter = null;
		int page = Math.max(1, payload.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(1, payload.optInt("page_size", 100)));
		boolean berpaginasi = payload.has("page") || payload.has("page_size");
		Long kategoriId = payload.isNull("kategori_id") ? null
				: Long.valueOf((payload.get("kategori_id") + "").trim());

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			// Kategori diturunkan dari produk yang BENAR-BENAR ditampilkan (grup per jenisProduk),
			// BUKAN dari seluruh baris JenisProduk di sistem -- sebelumnya query kategori terpisah
			// (Criteria polos tanpa filter) sehingga menampilkan kategori yang tidak dipakai produk
			// mana pun di toko ini (mis. kategori milik toko lain, atau kategori tanpa produk aktif
			// sama sekali) -- membingungkan kasir krn pill kategori itu bila diklik selalu kosong.
			// LinkedHashMap sekadar dedup by id; urutan akhir tetap diseragamkan alfabetis di bawah.
			java.util.Map<Long, String> petaKategori = new java.util.LinkedHashMap<Long, String>();
			// Gap-closure "Foto Produk" (banyak foto per produk) -- dikumpulkan selama loop utama di
			// bawah, dipakai SETELAH loop selesai utk satu query batch ke DB streaming (lihat di bawah
			// loop) supaya tak jadi N+1 (satu query per produk).
			java.util.Map<Long, JSONObject> jsonPorProdukId = new java.util.LinkedHashMap<Long, JSONObject>();

			JSONArray produkArr = new JSONArray();
			long totalProduk = berpaginasi
					? PriceTagUtil.countProduk(session, tokoId, keyword, semuaTokoDiminta, adminGlobal, jenisItemFilter, kategoriId)
					: 0L;
			java.util.List<Produk> daftarProduk = berpaginasi
					? PriceTagUtil.listProduk(session, tokoId, keyword, semuaTokoDiminta, adminGlobal,
							jenisItemFilter, kategoriId, (page - 1) * pageSize, pageSize)
					: PriceTagUtil.listProduk(session, tokoId, keyword, semuaTokoDiminta, adminGlobal, jenisItemFilter);
			for (Object o : daftarProduk) {
				Produk p = (Produk) o;
				JSONObject j = new JSONObject();
				j.put("id", p.getId());
				j.put("kode", str(p.getKode()));
				j.put("barcode", str(p.getBarcode()));
				j.put("nama", str(p.getNama()));
				// Gap-closure "Jenis Item" (Produk vs Bahan Baku) -- selalu dikirim (bukan hanya saat
				// difilter) supaya klien bisa membangun picker "Pilih Bahan Baku" dari list yang sudah
				// di memori (mis. daftar hasil katalog tanpa filter di layar admin Produk) tanpa perlu
				// call server kedua.
				j.put("jenisItem", str(p.getJenisItem()));
				// Ditampilkan hanya relevan saat semuaToko aktif (baris bisa berasal dari toko lain/null)
				// -- tetap dikirim selalu supaya klien tak perlu tahu mode aktifnya utk merender kolom ini.
				j.put("tokoIdProduk", p.getToko() == null ? JSONObject.NULL : p.getToko().getId());
				j.put("tokoNamaProduk", p.getToko() == null ? "(Tanpa Toko)" : str(p.getToko().getNama()));
				j.put("hargaJual", p.getHargaJual() == null ? 0 : p.getHargaJual());
				j.put("hargaBeli", p.getHargaBeli() == null ? 0 : p.getHargaBeli());
				// Stok LIVE (bukan cache) -- ditampilkan sbg badge di kartu produk kasir, pola sama
				// persis dgn _pos.jsp ("Habis" merah stok<=0, "Stok N" kuning stok<=5) supaya kasir
				// sadar barang mau habis TANPA harus buka tab "Produk & Inventaris" terpisah dulu.
				j.put("stok", p.getStok() == null ? 0 : p.getStok());
				// Dipakai layar admin Produk (Desktop/Android) mengisi ulang toggle "boleh dijual walau
				// stok minus" saat form Ubah dibuka -- sebelumnya field ini TIDAK PERNAH dikirim di sini
				// sehingga form Ubah selalu menganggapnya false, diam-diam mereset nilai asli tiap produk
				// disimpan ulang. Kasir non-admin (bukan bolehKelolaProduk()) tidak memakai field ini.
				j.put("izinkanJualMinusStok", Boolean.TRUE.equals(p.getIzinkanJualMinusStok()));
				j.put("keterangan", str(p.getKeterangan()));
				JenisProduk jp = p.getJenisProduk();
				j.put("kategoriId", jp == null || jp.getId() == null ? JSONObject.NULL : jp.getId());
				j.put("kategoriNama", jp == null ? "" : str(jp.getNama()));
				ais.database.model.inventory.KebijakanRetur kr = p.getKebijakanRetur();
				j.put("kebijakanReturId", kr == null || kr.getId() == null ? JSONObject.NULL : kr.getId());
				j.put("kebijakanReturNama", kr == null ? ais.database.model.inventory.KebijakanRetur.TANPA_KEBIJAKAN : str(kr.getNama()));
				// Gap-closure "Cetak PDF" (layar Produk) -- perlu Satuan/UOM & Pemasok per baris utk
				// cetakan detail, field ini SEBELUMNYA tak pernah dikirim aksi katalog sama sekali.
				j.put("satuanNama", p.getSatuan() == null ? "" : str(p.getSatuan().getNama()));
				j.put("pemasokNama", p.getPemasok() == null ? "" : str(p.getPemasok().getNama()));
				j.put("gambarUrl", Boolean.TRUE.equals(p.getAdaFileGambar()) ? buildUrlGambarProduk(request, p.getId()) : JSONObject.NULL);
				// Diisi ulang (bila ada) SETELAH loop ini lewat batch query -- default kosong dulu di
				// sini supaya baris tanpa foto tetap konsisten kirim array (bukan null/hilang).
				j.put("fotoUrls", new JSONArray());
				jsonPorProdukId.put(p.getId(), j);
				// Resep Bahan Baku (gap-closure Desktop/Android, padanan JSP barang/index.jsp) -- dipakai
				// mengisi ulang daftar bahan saat form "Ubah" produk ber-resep dibuka kembali. Kolom
				// {@code Produk.bahanBaku} sudah berupa string JSON persis bentuk array (lihat JavaDoc
				// entity), jadi cukup di-parse ulang jadi JSONArray asli (BUKAN dikutip mentah sbg
				// string) supaya klien terima array beneran, bukan string-berisi-JSON.
				String bahanBakuMentah = str(p.getBahanBaku()).trim();
				if (!bahanBakuMentah.isEmpty() && !bahanBakuMentah.equals("[]")) {
					try {
						j.put("bahanBaku", new JSONArray(bahanBakuMentah));
					} catch (Exception eResepParse) {
						j.put("bahanBaku", new JSONArray());
					}
				} else {
					j.put("bahanBaku", new JSONArray());
				}
				// Gap-closure "Produk Ekstra" -- SELALU dikirim (pola sama persis bahanBaku/jenisItem
				// di atas) supaya klien bisa resolve/bangun picker "Pilih Ekstra" dari list yang sudah
				// di memori tanpa call server kedua.
				String ekstraPilihanMentah = str(p.getEkstraPilihan()).trim();
				if (!ekstraPilihanMentah.isEmpty() && !ekstraPilihanMentah.equals("[]")) {
					try {
						j.put("ekstraPilihan", new JSONArray(ekstraPilihanMentah));
					} catch (Exception eEkstraParse) {
						j.put("ekstraPilihan", new JSONArray());
					}
				} else {
					j.put("ekstraPilihan", new JSONArray());
				}
				produkArr.put(j);

				if (jp != null && jp.getId() != null) {
					petaKategori.put(jp.getId(), str(jp.getNama()));
				}
			}

			// Gap-closure "Foto Produk" (banyak foto per produk, carousel Kasir tiap 3 detik bila >1) --
			// SATU query batch ke DB streaming terpisah utk SELURUH produk di halaman ini (bukan N+1),
			// lalu suntikkan array fotoUrls per baris (urut lama->baru = urutan unggah). Kosong/tak ada
			// baris FotoGambarProduk = tetap array kosong (sudah di-default di loop atas).
			if (!jsonPorProdukId.isEmpty()) {
				Session streamSession = ais.database.hibernate.StreamingHibernateUtil.getInstance().openSession();
				try {
					java.util.Map<Long, JSONArray> fotoPorProduk = new java.util.HashMap<Long, JSONArray>();
					// PostgreSQL JDBC lama menyimpan jumlah parameter Parse/Bind dalam integer 2 byte.
					// Satu Restrictions.in berisi 50.000 id (katalog data demo) membuat driver gagal
					// SEBELUM SQL dikirim: "out-of-range integer as a 2-byte value: 50000". Pecah
					// id menjadi batch tetap agar seluruh foto tetap termuat tanpa batas tersembunyi.
					final int ukuranBatchFoto = 500;
					java.util.List<Long> seluruhProdukId = new java.util.ArrayList<Long>(jsonPorProdukId.keySet());
					for (int awal = 0; awal < seluruhProdukId.size(); awal += ukuranBatchFoto) {
						int akhir = Math.min(awal + ukuranBatchFoto, seluruhProdukId.size());
						java.util.List<Long> produkIdBatch = new java.util.ArrayList<Long>(seluruhProdukId.subList(awal, akhir));
						@SuppressWarnings("unchecked")
						java.util.List<Object[]> barisFoto = streamSession
								.createCriteria(ais.database.model.file.FotoGambarProduk.class)
								.add(Restrictions.in("produk", produkIdBatch))
								.setProjection(org.hibernate.criterion.Projections.projectionList()
										.add(org.hibernate.criterion.Projections.property("produk"))
										.add(org.hibernate.criterion.Projections.property("id")))
								.addOrder(Order.asc("id")).list();
						for (Object[] baris : barisFoto) {
							Long produkIdBaris = (Long) baris[0];
							Long fotoId = (Long) baris[1];
							JSONArray arr = fotoPorProduk.get(produkIdBaris);
							if (arr == null) {
								arr = new JSONArray();
								fotoPorProduk.put(produkIdBaris, arr);
							}
							arr.put(buildUrlGambarFotoProduk(request, fotoId));
						}
						// Query hanya memproyeksikan scalar, tetapi clear tetap menjaga footprint session
						// konstan bila katalog sangat besar dan listener Hibernate menempelkan state lain.
						streamSession.clear();
					}
					for (java.util.Map.Entry<Long, JSONArray> e : fotoPorProduk.entrySet()) {
						JSONObject jTarget = jsonPorProdukId.get(e.getKey());
						if (jTarget != null) jTarget.put("fotoUrls", e.getValue());
					}
				} finally {
					ais.database.hibernate.HibernateUtil.closeSessionQuietly(streamSession);
				}
			}

			java.util.List<java.util.Map.Entry<Long, String>> entriesKategori = new java.util.ArrayList<java.util.Map.Entry<Long, String>>(petaKategori.entrySet());
			java.util.Collections.sort(entriesKategori, new java.util.Comparator<java.util.Map.Entry<Long, String>>() {
				public int compare(java.util.Map.Entry<Long, String> a, java.util.Map.Entry<Long, String> b) {
					return a.getValue().compareToIgnoreCase(b.getValue());
				}
			});
			JSONArray kategoriArr = new JSONArray();
			for (java.util.Map.Entry<Long, String> e : entriesKategori) {
				JSONObject j = new JSONObject();
				j.put("id", e.getKey());
				j.put("nama", e.getValue());
				kategoriArr.put(j);
			}

			hasil.put("status", "success");
			hasil.put("kategori", kategoriArr);
			hasil.put("produk", produkArr);
			hasil.put("tokoId", tokoId == null ? JSONObject.NULL : tokoId);
			hasil.put("semuaToko", semuaTokoDiminta);
			if (berpaginasi) {
				hasil.put("total", totalProduk);
				hasil.put("page", page);
				hasil.put("pageSize", pageSize);
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Menyusun URL absolut ke {@code /AmbilMediaProduk} (servlet publik/tanpa-auth yang sudah ada,
	 * lihat JavaDoc {@link #prosesKatalog}) dari komponen {@code request} secara MANUAL -- sengaja
	 * tidak memakai {@code ais.common.CommonMedia.getMediaProduk} yang secara internal membaca state
	 * request-scoped ({@code CommonCurrentSessionHelper.getRequestHostWithProtocol()}) yang belum
	 * tentu terisi benar di konteks servlet BARU ini (berbeda siklus hidup dari JSP/ZK yang sudah
	 * lama memakai helper itu) -- membangun manual dari {@code HttpServletRequest} yang SUDAH pasti
	 * tersedia lebih aman dan tidak bergantung pada asumsi tersembunyi apa pun.
	 */
	private static String buildUrlGambarProduk(HttpServletRequest request, Long produkId) {
		String scheme = request.getScheme();
		int port = request.getServerPort();
		boolean portDefault = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
		String basis = scheme + "://" + request.getServerName() + (portDefault ? "" : (":" + port)) + request.getContextPath();
		return basis + "/AmbilMediaProduk?id=" + produkId + "&height=200&width=200&img=.jpg";
	}

	/**
	 * Padanan {@link #buildUrlGambarProduk} utk SATU foto tertentu (gap-closure "Foto Produk" banyak
	 * foto) -- pakai param BARU {@code fotoId} (id baris {@code FotoGambarProduk} itu sendiri, lihat
	 * javadoc {@code AmbilMediaProduk.loadFile}), BUKAN {@code id} (yang berarti "produk ini, ambil
	 * foto TERBARUnya" -- dipakai {@link #buildUrlGambarProduk}). TIDAK dikecilkan (tanpa
	 * height/width) -- dipakai galeri form Ubah Produk & carousel Kasir, keduanya sudah mengatur
	 * ukuran tampil sendiri di sisi UI.
	 */
	private static String buildUrlGambarFotoProduk(HttpServletRequest request, Long fotoId) {
		String scheme = request.getScheme();
		int port = request.getServerPort();
		boolean portDefault = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
		String basis = scheme + "://" + request.getServerName() + (portDefault ? "" : (":" + port)) + request.getContextPath();
		return basis + "/AmbilMediaProduk?fotoId=" + fotoId;
	}

	/**
	 * Padanan {@link #buildUrlGambarProduk} utk slide screensaver Layar Pelanggan -- endpoint
	 * {@code /AmbilMediaLayarPelangganSlide} SAMA publik/tanpa otentikasi (lihat javadoc servlet).
	 * TIDAK dikecilkan (tanpa height/width) -- gambar screensaver ditampilkan penuh/fullscreen,
	 * beda kebutuhan dgn thumbnail katalog produk.
	 */
	private static String buildUrlGambarLayarPelangganSlide(HttpServletRequest request, Long slideId) {
		String scheme = request.getScheme();
		int port = request.getServerPort();
		boolean portDefault = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
		String basis = scheme + "://" + request.getServerName() + (portDefault ? "" : (":" + port)) + request.getContextPath();
		return basis + "/AmbilMediaLayarPelangganSlide?id=" + slideId;
	}

	/**
	 * Konfigurasi POS yang sama dibaca {@code _pos.jsp} dari scriptlet server-side (pajak, gerbang
	 * Sesi Kas/cegah-oversell, info toko) -- dipanggil layar lokal SEKALI saat masuk supaya nilainya
	 * konsisten dengan versi JSP tanpa menduplikasi aturan/nilai default di sisi klien.
	 */
	private void prosesKonfigurasi(Tbmuser tbmuser, JSONObject hasil) throws Exception {
		Pedagang pedagang = tbmuser.getPedagang();
		Tbmrole roleAksesMenu = tbmuser.hakAkses();
		boolean roleMultiToko = roleAksesMenu != null && roleAksesMenu.getTokoAksesJson() != null;
		Toko toko = null;
		JSONArray daftarTokoJson = new JSONArray();
		Session sesiToko = HibernateUtil.getSessionFactory().openSession();
		try {
			java.util.List<Toko> daftarTokoBoleh = KantinHelper.daftarTokoBolehDiakses(sesiToko, tbmuser);
			Long aktif = tbmuser.getTokoAktifMultiToko();
			for (Toko t : daftarTokoBoleh) {
				JSONObject j = new JSONObject();
				j.put("id", t.getId());
				j.put("nama", str(t.getNama()));
				j.put("tokoDemo", Boolean.TRUE.equals(t.getTokoDemo()));
				daftarTokoJson.put(j);
				if (aktif != null && t.getId() != null && t.getId().equals(aktif)) toko = t;
			}
			if (toko == null) {
				if (roleMultiToko) {
					// Kalau role sudah mengatur multi-toko, abaikan tbmuser.getPedagang(): default aman
					// adalah toko pertama yang memang ada di JSON role, bukan toko lama yang mungkin
					// melekat di akun.
					if (!daftarTokoBoleh.isEmpty()) toko = daftarTokoBoleh.get(0);
				} else if (pedagang != null && pedagang.getToko() != null) {
					toko = pedagang.getToko();
				}
			}
		} finally {
			HibernateUtil.closeSessionQuietly(sesiToko);
		}

		double pajakPersen = 0;
		try {
			String v = Common.getKonfigurasi("pajak_persen_kantin", "0").getNilai();
			if (v != null) v = v.replaceAll("[^0-9.]", "");
			if (v != null && v.length() > 0) pajakPersen = Double.parseDouble(v);
		} catch (Exception e) { pajakPersen = 0; }
		if (pajakPersen < 0) pajakPersen = 0;

		hasil.put("status", "success");
		hasil.put("pajakPersen", pajakPersen);
		// Satu sumber kebenaran untuk Desktop/Android/JSP/API. OPT-IN (default MATI,
		// selaras gerbang server KantinHelper.bayar): kewajiban sesi kas hanya
		// berlaku bila konfigurasi diaktifkan eksplisit.
		hasil.put("wajibSesiKas", Common.bolehKonfigurasi(
				Konfigurasi.KANTIN_POS_WAJIB_SESI_KAS, Konfigurasi.TIDAK_AKTIF));
		hasil.put("cegahOversell", Common.bolehKonfigurasi(Konfigurasi.KANTIN_POS_CEGAH_OVERSELL, Konfigurasi.TIDAK_AKTIF));
		hasil.put("bolehTransaksiStokHabis", toko != null
				&& Boolean.TRUE.equals(toko.getBolehTransaksiStokHabis()));
		hasil.put("tokoDemo", toko != null && Boolean.TRUE.equals(toko.getTokoDemo()));
		hasil.put("dataSampleEbisnis", Common.bolehKonfigurasi(
				Konfigurasi.DATA_SAMPLE_EBISNIS, Konfigurasi.TIDAK_AKTIF));
		// Jangan menyimpulkan admin dari scope toko. Administrator tetap dapat terikat ke
		// Pedagang/Toko (mis. akun demo Apotik) dan harus tetap menerima seluruh menu.
		// Sumber kebenaran yang sama dipakai modul ZK/JSP: role ADMINISTRATOR aktif.
		final boolean admin = Common.getApakahAdminLain(tbmuser);
		hasil.put("isAdmin", admin);
		hasil.put("tokoId", toko == null ? JSONObject.NULL : toko.getId());
		hasil.put("tokoNama", toko == null ? "" : str(toko.getNama()));
		hasil.put("tokoAktifId", toko == null ? JSONObject.NULL : toko.getId());
		hasil.put("daftarToko", daftarTokoJson);
		hasil.put("multiToko", daftarTokoJson.length() > 1);
		// Ucapan penutup struk & Layar Pelanggan (fitur "Konfigurasi", per-toko) -- toko==null (admin
		// global, tanpa toko) memakai teks formal default yg sama, lihat Toko.PESAN_TERIMA_KASIH_DEFAULT.
		hasil.put("pesanTerimaKasih", toko == null ? Toko.PESAN_TERIMA_KASIH_DEFAULT : str(toko.getPesanTerimaKasih()));
		hasil.put("alasanTahan", KantinHelper.alasanTahanUntukToko(toko));
		hasil.put("userId", str(tbmuser.getUserId()));
		// Fitur "Akun Pedagang"/"Konfigurasi" (menu Konfigurasi Kasir Desktop): kasir toko berstatus
		// SUPERVISOR (Pedagang.getSupervisor()) boleh kelola akun pedagang lain + ubah profil toko di
		// tokonya sendiri -- gerbang SEBENARNYA tetap dicek ulang server-side di
		// KantinHelper.pedagangUbah/tokoProfilSimpan (flag ini murni utk gating tampilan tombol).
		hasil.put("supervisorPedagang", (pedagang != null && Boolean.TRUE.equals(pedagang.getSupervisor()))
				|| (roleAksesMenu != null
						&& ais.common.EbisnisMenuKatalog.urai(roleAksesMenu.getEbisnisMenu()).optBoolean("supervisor", false)));
		// Fitur "Topup" (tab Pelanggan) -- gerbang SAMA dgn Tbmrole.bolehEntryTopup yg SUDAH ditegakkan
		// server-side di KantinHelper.topupSaldo/depositUbah/depositHapus; flag ini murni utk gating
		// tampilan tombol Topup di Flutter (mirror pola isAdmin/supervisorPedagang di atas).
		hasil.put("bolehEntryTopup", roleAksesMenu != null && roleAksesMenu.getBolehEntryTopup() != null
				&& roleAksesMenu.getBolehEntryTopup().booleanValue());
		hasil.put("bolehHapusPesanan", bolehSupervisorAtauAdmin(tbmuser)
				|| bolehAksiCrudPesanan(tbmuser, "delete") || bolehAksiCrudPesanan(tbmuser, "reject"));
		// Fitur "Hak Akses Menu per Akun" (gap-closure Toko Al-Bahjah). Admin global (pedagang==null, TIDAK terikat satu toko) SELALU
		// akses semua menu -- flag akses per-menu HANYA berlaku utk akun Pedagang toko biasa. Gerbang
		// SEBENARNYA (sidebar disembunyikan) ada di klien; ini murni sumber kebenarannya dari server
		// supaya admin bisa mengatur dari layar Konfigurasi/web tanpa klien bisa memalsukannya sendiri
		// (klien tak pernah punya cara menulis field ini kecuali lewat aksi pedagang_ubah/akun_tambah).
		// Sumber tunggal: JSON konsolidasi Tbmrole.ebisnisMenu (bukan lagi 26 kolom Boolean terpisah --
		// lihat ais.common.EbisnisMenuKatalog). roleAksesMenu==null (admin global/tanpa role) -> semua
		// menu default true (perilaku sama spt sebelumnya, EbisnisMenuKatalog.urai(null) sudah begitu).
		org.json.JSONObject ebisnisMenuRole = ais.common.EbisnisMenuKatalog
				.urai(admin || roleAksesMenu == null ? null : roleAksesMenu.getEbisnisMenu());
		org.json.JSONObject menuTersimpan = ebisnisMenuRole.getJSONObject("menu");
		JSONObject aksesMenu = new JSONObject();
		aksesMenu.put("supervisor", ebisnisMenuRole.optBoolean("supervisor", false));
		aksesMenu.put("kasir", menuTersimpan.optBoolean("kasir", true));
		aksesMenu.put("ringkasan", menuTersimpan.optBoolean("ringkasan", true));
		aksesMenu.put("pesanan", menuTersimpan.optBoolean("pesanan", true));
		aksesMenu.put("anggota", menuTersimpan.optBoolean("anggota", true));
		aksesMenu.put("produk", menuTersimpan.optBoolean("produk", true));
		aksesMenu.put("barang", menuTersimpan.optBoolean("produk", true));
		// Fail-closed (default false) -- lihat gate grup_produk_ di bolehAksesActionKantin.
		aksesMenu.put("grup_produk", menuTersimpan.optBoolean("grup_produk", false));
		aksesMenu.put("hotel_properti", menuTersimpan.optBoolean("hotel_properti", false));
		aksesMenu.put("hotel_kamar", menuTersimpan.optBoolean("hotel_kamar", false));
		aksesMenu.put("hotel_reservasi", menuTersimpan.optBoolean("hotel_reservasi", false));
		aksesMenu.put("hotel_checkin", menuTersimpan.optBoolean("hotel_checkin", false));
		aksesMenu.put("hotel_folio", menuTersimpan.optBoolean("hotel_folio", false));
		aksesMenu.put("hotel_tiket_dapur", menuTersimpan.optBoolean("hotel_tiket_dapur", false));
		aksesMenu.put("hotel_kontrak_pemilik", menuTersimpan.optBoolean("hotel_kontrak_pemilik", false));
		aksesMenu.put("hotel_laporan_pemilik", menuTersimpan.optBoolean("hotel_laporan_pemilik", false));
		aksesMenu.put("stokopname", menuTersimpan.optBoolean("stokopname", true));
		aksesMenu.put("stok", menuTersimpan.optBoolean("stokopname", true));
		aksesMenu.put("kulakan", menuTersimpan.optBoolean("kulakan", true));
		aksesMenu.put("diskon", menuTersimpan.optBoolean("diskon", true));
		aksesMenu.put("pembayaran", menuTersimpan.optBoolean("pembayaran", true));
		aksesMenu.put("pedagang", menuTersimpan.optBoolean("pedagang", true));
		aksesMenu.put("meja", menuTersimpan.optBoolean("meja", true));
		aksesMenu.put("penyedia", menuTersimpan.optBoolean("penyedia", true));
		aksesMenu.put("vendor", menuTersimpan.optBoolean("penyedia", true));
		aksesMenu.put("kaskasir", menuTersimpan.optBoolean("kaskasir", true));
		aksesMenu.put("kas", menuTersimpan.optBoolean("kaskasir", true));
		aksesMenu.put("setorantenant", menuTersimpan.optBoolean("setorantenant", true));
		aksesMenu.put("tenant", menuTersimpan.optBoolean("setorantenant", true));
		aksesMenu.put("jadwalopname", menuTersimpan.optBoolean("jadwalopname", true));
		aksesMenu.put("opname", menuTersimpan.optBoolean("jadwalopname", true));
		aksesMenu.put("stokexpired", menuTersimpan.optBoolean("stokexpired", true));
		aksesMenu.put("stok_expired", menuTersimpan.optBoolean("stokexpired", true));
		aksesMenu.put("limitkredit", menuTersimpan.optBoolean("limitkredit", true));
		aksesMenu.put("limit_kredit", menuTersimpan.optBoolean("limitkredit", true));
		aksesMenu.put("rekeningkoran", menuTersimpan.optBoolean("mutasirekening", true));
		aksesMenu.put("mutasi_rekening", menuTersimpan.optBoolean("mutasirekening", true));
		aksesMenu.put("produksi", menuTersimpan.optBoolean("produksi", true));
		aksesMenu.put("laporantransaksi", menuTersimpan.optBoolean("laporantransaksi", true));
		aksesMenu.put("laporan_transaksi", menuTersimpan.optBoolean("laporantransaksi", true));
		aksesMenu.put("laporan", menuTersimpan.optBoolean("laporan", true));
		aksesMenu.put("laporankeuangan", menuTersimpan.optBoolean("laporankeuangan", true));
		aksesMenu.put("pengaturanlaporan", menuTersimpan.optBoolean("pengaturanlaporan", true));
		aksesMenu.put("pengaturan_laporan", menuTersimpan.optBoolean("pengaturanlaporan", true));
		aksesMenu.put("riwayatsinkronisasi", menuTersimpan.optBoolean("riwayatsinkronisasi", true));
		aksesMenu.put("sinkronisasi", menuTersimpan.optBoolean("riwayatsinkronisasi", true));
		aksesMenu.put("logerror", menuTersimpan.optBoolean("logerror", true));
		aksesMenu.put("log_error", menuTersimpan.optBoolean("logerror", true));
		aksesMenu.put("konfigurasi", menuTersimpan.optBoolean("konfigurasi", true));
		aksesMenu.put("pengaturan", menuTersimpan.optBoolean("konfigurasi", true));
		aksesMenu.put("riwayatpenjualan", menuTersimpan.optBoolean("riwayatpenjualan", true));
		aksesMenu.put("returpenjualan", menuTersimpan.optBoolean("returpenjualan", true));
		// Kunci varian fail-closed (Inventory & Sales, POS Apotik, POS eMedik) -- SATU jalur
		// terpusat dari KUNCI_DEFAULT_NONAKTIF, default EKSPLISIT false: klien lama
		// (Sesi.bolehMenu) menganggap kunci yang HILANG = boleh, jadi kunci baru wajib selalu
		// hadir di respons ini bernilai false bila role belum pernah menyimpannya. Kunci varian
		// baru berikutnya otomatis ikut tanpa menambah baris manual di sini.
		for (String kunciVarian : ais.common.EbisnisMenuKatalog.KUNCI_DEFAULT_NONAKTIF) {
			aksesMenu.put(kunciVarian, admin || menuTersimpan.optBoolean(kunciVarian, false));
		}
		hasil.put("aksesMenu", aksesMenu);
		JSONObject aksesMenuCrud = new JSONObject();
		JSONObject crudTersimpan = ebisnisMenuRole.optJSONObject("crud");
		for (String kunciCrud : ais.common.EbisnisMenuKatalog.KUNCI_CRUD) {
			JSONObject baris = new JSONObject();
			JSONObject barisTersimpan = crudTersimpan == null ? null : crudTersimpan.optJSONObject(kunciCrud);
			for (String aksiCrud : ais.common.EbisnisMenuKatalog.AKSI_CRUD) {
				baris.put(aksiCrud, admin || barisTersimpan == null || barisTersimpan.optBoolean(aksiCrud, true));
			}
			aksesMenuCrud.put(kunciCrud, baris);
		}
		hasil.put("aksesMenuCrud", aksesMenuCrud);
		// Konteks aktor varian "eBisnis Inventory & Sales" (P1 FND-006/FND-008) -- ADITIF: klien
		// POS lama mengabaikan field baru ini; klien varian inventory_sales memakainya menentukan
		// landing + menu per aktor. Dibungkus try/catch: kegagalan resolusi (mis. tabel
		// sales_inventory belum tercipta pada start pertama) TIDAK boleh merusak aksi konfigurasi.
		try {
			ais.action.servlet.api.SalesInventoryHelper.isiKonteksAktor(tbmuser, hasil);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PosApi.prosesKonfigurasi: isiKonteksAktor (aditif, dilewati)");
		}
		// Fitur "Top Up Saldo lewat POS": tombolnya hanya boleh tampil bila kasir ini punya hak
		// Tbmrole.getBolehEntryTopup() -- gerbang YANG SAMA dgn menu "Manajemen Saldo (Deposit)"
		// (bukan gerbang baru); gerbang SEBENARNYA tetap dicek ulang di server saat topup_saldo
		// dipanggil (lihat KantinHelper.topupSaldo) -- flag ini murni utk sembunyikan tombol di UI.
		ais.database.model.Tbmrole roleTopup = tbmuser.hakAkses();
		hasil.put("bolehTopup", roleTopup != null && roleTopup.getBolehEntryTopup() != null
				&& roleTopup.getBolehEntryTopup().booleanValue());

		// Fitur "Format Unggah/Unduh Excel" (gap-closure, permintaan user: file yang diunggah toko
		// biasanya adalah HASIL EKSPOR software akuntansi "Accurate" -- klien wajib meminta konfirmasi
		// format sebelum Unggah/Unduh, walau utk sekarang hanya SATU format tersedia). Daftar HARDCODE
		// dulu di sini (server = satu sumber kebenaran, bukan diam-diam berbeda antar Desktop/Android) --
		// TIAP format baru di masa depan cukup ditambah SATU baris di sini, klien otomatis menampilkannya
		// di modal pilihan tanpa perlu rilis ulang. "aktif" murni default awal; enable/disable per-toko
		// masih murni preferensi lokal klien (localStorage) krn baru ada satu format, belum ada tabel DB.
		JSONArray formatImporEksporArr = new JSONArray();
		JSONObject formatAccurate = new JSONObject();
		formatAccurate.put("id", "accurate");
		formatAccurate.put("nama", "Format Accurate");
		formatAccurate.put("aktif", true);
		formatImporEksporArr.put(formatAccurate);
		hasil.put("formatImporEkspor", formatImporEksporArr);

		// Daftar cara bayar aktif (id+nama+manual) -- dibutuhkan pos.html membangun pilihan metode
		// pembayaran, dgn ID yg VALID di server ini (beda tiap instalasi -- tidak boleh dihardcode
		// di klien). "manual" membedakan cara bayar yg butuh verifikasi tambahan (mis. transfer
		// bank) dari yg langsung tuntas (mis. tunai) -- sama seperti dibaca _pos.jsp dari SQL.
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JSONArray caraBayarArr = new JSONArray();
			Criteria c = session.createCriteria(CaraPembayaranKoperasi.class)
					.add(Restrictions.eq("aktif", true)).addOrder(Order.asc("nama"));
			for (Object o : c.list()) {
				CaraPembayaranKoperasi cb = (CaraPembayaranKoperasi) o;
				JSONObject j = new JSONObject();
				j.put("id", cb.getId());
				j.put("nama", str(cb.getNama()));
				j.put("manual", cb.getManual() != null && cb.getManual());
				caraBayarArr.put(j);
			}
			hasil.put("caraBayar", caraBayarArr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * <h3>Menu "Sidebar/Drawer Tree" -- pohon navigasi ERP eBisnis lengkap (gap-closure dokumen
	 * STRUKTUR_MENU_LENGKAP_EBISNIS_ID.md), khusus utk Desktop/Android.</h3>
	 *
	 * <p>Sumber taksonominya {@code ais.common.EbisnisMenuKatalog.treeUntukKlien} (berkas JSON
	 * global {@code ebisnis_menu_master.json}, BUKAN database) -- HANYA node {@code tersedia=true}
	 * (sudah ada layar sungguhan) yang dikembalikan, digabung dgn status aktif/nonaktif per Grup
	 * Pengguna dari {@code Tbmrole.ebisnisMenu} (field JSON yang SAMA dipakai {@code aksesMenu} di
	 * {@link #prosesKonfigurasi}). Klien lama yang masih pakai daftar datar {@code aksesMenu} TIDAK
	 * terdampak -- aksi ini aditif, tidak menggantikan apa pun.</p>
	 *
	 * @param request payload: {@code platform} ({@code "desktop"} atau {@code "android"}, wajib).
	 * @param hasil   diisi {@code status="00"}, {@code tree} (array node bersarang).
	 */
	private void prosesEbisnisMenuTree(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String platform = request == null ? "" : request.optString("platform", "").trim().toLowerCase();
		if (!"desktop".equals(platform) && !"android".equals(platform)) {
			hasil.put("status", "91");
			hasil.put("description", "Parameter platform wajib diisi \"desktop\" atau \"android\".");
			return;
		}
		Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		JSONObject roleEbisnisMenu = Common.getApakahAdminLain(tbmuser) ? null
				: ais.common.EbisnisMenuKatalog.urai(role == null ? null : role.getEbisnisMenu());
		hasil.put("status", "00");
		hasil.put("tree", ais.common.EbisnisMenuKatalog.treeUntukKlien(platform, roleEbisnisMenu));
	}

	/**
	 * Daftar pesanan (draft pembelian anggota koperasi) milik toko kasir yang sedang login -- 100
	 * baris terbaru, belum-lunas ditampilkan lebih dulu (pola urutan sama dgn
	 * {@code _draft_pesanan_anggota.jsp}). Setiap baris membawa daftar item lengkapnya (bukan cuma
	 * ringkasan teks) supaya klien bisa membangun ULANG payload {@code {action:"bayar", ...}} persis
	 * spt yg dibutuhkan {@link KantinHelper#bayar} saat kasir memverifikasi/menuntaskan pesanan --
	 * TIDAK ADA method baru dibutuhkan utk aksi "verifikasi/selesaikan", cukup pakai ulang aksi
	 * {@code bayar} yang SUDAH ADA dgn {@code draftPembelianAnggotaKoperasi} diisi id pesanan ini.
	 *
	 * <p>Status pesanan disimpulkan dari {@link DraftPembelianAnggotaKoperasi#getLunas()} (bukan
	 * kolom status terpisah -- tabel ini memang tidak punya itu): {@code null} = belum
	 * dibayar/diverifikasi, terisi = sudah lunas.</p>
	 */
	private void prosesPesananList(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		int page = Math.max(1, payload.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(1, payload.optInt("page_size", payload.optInt("limit", 15))));

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Criteria c = buatCriteriaPesanan(session, tokoId, payload).addOrder(Order.desc("id"));
			c.setFirstResult((page - 1) * pageSize).setMaxResults(pageSize);

			JSONArray arr = new JSONArray();
			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			for (Object o : c.list()) {
				DraftPembelianAnggotaKoperasi d = (DraftPembelianAnggotaKoperasi) o;
				JSONObject j = new JSONObject();
				j.put("id", d.getId());
				j.put("kode", str(d.getKode()));
				j.put("pemesan", d.getAnggotaKoperasi() == null ? "" : str(d.getAnggotaKoperasi().getNama()));
				j.put("anggotaId", d.getAnggotaKoperasi() == null ? JSONObject.NULL : d.getAnggotaKoperasi().getId());
				j.put("totalBiaya", d.getTotalBiaya() == null ? 0 : d.getTotalBiaya());
				j.put("lunas", d.getLunas() != null);
				// id PembelianAnggotaKoperasi terkait bila sudah lunas -- gap-closure, dipakai klien
				// utk "Cetak Struk" (aksi detail_transaksi) & "Hitung Ulang" pada baris yg sudah final.
				j.put("lunasId", d.getLunas() == null ? JSONObject.NULL : d.getLunas().getId());
				j.put("totalDiskon", d.getTotalDiskon() == null ? 0 : d.getTotalDiskon());
				j.put("totalCashback", d.getTotalCashback() == null ? 0 : d.getTotalCashback());
				j.put("tokoNama", d.getToko() == null ? "" : str(d.getToko().getNama()));
				j.put("keterangan", str(d.getKeterangan()));
				j.put("tanggalPembayaran", d.getTanggalPembayaran() == null ? "" : fmt.format(d.getTanggalPembayaran()));
				j.put("caraBayarId", d.getCaraPembayaranKoperasi() == null ? JSONObject.NULL : d.getCaraPembayaranKoperasi().getId());
				// Pembeda "Pesanan Online" (dibuat pembeli sendiri) vs "Keranjang Tertahan" (ditahan
				// kasir) -- lihat JavaDoc lengkap KantinHelper.pesananOnlineBaru soal kenapa sinyal
				// SATU-SATUNYA yang bisa diandalkan adalah tbmuser pengirim draft ini.
				j.put("dariPembeliOnline", d.getTbmuser() != null && d.getTbmuser().getAnggotaKoperasi() != null);
				// Gap-closure "banyak mesin POS satu toko" + "nama kasir salah" -- lihat JavaDoc
				// DraftPembelianAnggotaKoperasi.getKasirLoginNama()/getNamaMesin().
				j.put("kasirLoginNama", str(d.getKasirLoginNama()));
				j.put("namaMesin", d.getNamaMesin() == null || d.getNamaMesin().trim().isEmpty() ? JSONObject.NULL : str(d.getNamaMesin()));

				JSONArray items = new JSONArray();
				Criteria ci = session.createCriteria(DraftPembelian.class)
						.add(Restrictions.eq("draftPembelianAnggotaKoperasi", d));
				for (Object oi : ci.list()) {
					DraftPembelian dp = (DraftPembelian) oi;
					JSONObject ji = new JSONObject();
					ji.put("id", dp.getProduk() == null ? JSONObject.NULL : dp.getProduk().getId());
					ji.put("kode", dp.getProduk() == null ? "" : str(dp.getProduk().getKode()));
					ji.put("nama", str(dp.getNama()));
					ji.put("harga", dp.getHargaSatuan() == null ? 0 : dp.getHargaSatuan());
					ji.put("jumlah", dp.getQty() == null ? 0 : dp.getQty());
					// Fase 5 -- dulu hilang (Keranjang Tertahan resume di Desktop/Android mengirim
					// diskon:0/aturanDiskon:null hardcode); sekarang dipulihkan APA ADANYA dari draft,
					// walau nilainya akan langsung dihitung ULANG begitu keranjang dievaluasi lagi
					// (Fase 4, diskon_evaluasi) -- disertakan di sini murni supaya konsisten dgn JSP.
					ji.put("diskon", dp.getDiskon() == null ? 0 : dp.getDiskon());
					ji.put("cashback", dp.getCashback() == null ? 0 : dp.getCashback());
					ji.put("aturanDiskon", dp.getAturanDiskon() == null ? JSONObject.NULL : dp.getAturanDiskon().getId());
					// Gap-closure "Produk Ekstra" -- draftItemId = id baris DraftPembelian INI SENDIRI
					// (BEDA dari "id" di atas yg id Produk), dibutuhkan klien utk mencocokkan "indukId"
					// baris lain ke baris mana saat merekonstruksi keranjang ber-ekstra dari draft yang
					// ditahan. indukId null = baris dasar, terisi = baris ekstra (nilainya draftItemId
					// baris induknya).
					ji.put("draftItemId", dp.getId());
					ji.put("indukId", dp.getIndukId() == null ? JSONObject.NULL : dp.getIndukId());
					items.put(ji);
				}
				j.put("items", items);
				arr.put(j);
			}

			hasil.put("status", "success");
			hasil.put("pesanan", arr);
			Object total = buatCriteriaPesanan(session, tokoId, payload)
					.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
			hasil.put("total", total instanceof Number ? ((Number) total).longValue() : 0L);
			hasil.put("page", page);
			hasil.put("pageSize", pageSize);
			// Pilihan kasir berasal dari draft existing pada toko aktif. Identitas yang dikirim
			// adalah nilai kasir tersimpan, sehingga dropdown tidak bergantung pada nama akun
			// yang sedang login dan tetap dapat menelusuri transaksi shift sebelumnya.
			JSONArray daftarKasir = new JSONArray();
			Criteria ck = session.createCriteria(DraftPembelianAnggotaKoperasi.class)
					.add(Restrictions.isNotNull("kasirLoginNama"));
			if (tokoId != null) {
				ck.createAlias("toko", "tkk").add(Restrictions.eq("tkk.id", tokoId));
			}
			ck.setProjection(org.hibernate.criterion.Projections.distinct(
					org.hibernate.criterion.Projections.property("kasirLoginNama")));
			for (Object namaKasir : ck.list()) {
				String nama = str(namaKasir).trim();
				if (!nama.isEmpty()) daftarKasir.put(nama);
			}
			hasil.put("daftarKasir", daftarKasir);
			// KPI dihitung oleh DB dengan agregasi, bukan dengan mengambil semua baris ke JVM.
			JSONObject ringkasanPayload = new JSONObject(payload.toString());
			ringkasanPayload.remove("asal");
			Criteria semuaKpi = buatCriteriaPesanan(session, tokoId, ringkasanPayload);
			Object jumlahSemua = semuaKpi.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
			Criteria lunasKpi = buatCriteriaPesanan(session, tokoId, ringkasanPayload)
					.add(Restrictions.isNotNull("lunas"));
			Object jumlahLunas = lunasKpi.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
			Criteria belumKpi = buatCriteriaPesanan(session, tokoId, ringkasanPayload)
					.add(Restrictions.isNull("lunas"));
			Object jumlahBelum = belumKpi.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
			Criteria onlineKpi = buatCriteriaPesanan(session, tokoId, ringkasanPayload)
					.add(Restrictions.isNull("lunas"))
					.createAlias("tbmuser", "kpu", org.hibernate.sql.JoinFragment.LEFT_OUTER_JOIN)
					.createAlias("kpu.anggotaKoperasi", "kpak", org.hibernate.sql.JoinFragment.LEFT_OUTER_JOIN)
					.add(Restrictions.isNotNull("kpak.id"));
			Object jumlahOnline = onlineKpi.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
			Criteria nilaiKpi = buatCriteriaPesanan(session, tokoId, ringkasanPayload)
					.add(Restrictions.isNull("lunas"));
			Object nilaiMenunggu = nilaiKpi.setProjection(org.hibernate.criterion.Projections.sum("totalBiaya")).uniqueResult();
			long nSemua = jumlahSemua instanceof Number ? ((Number) jumlahSemua).longValue() : 0L;
			long nLunas = jumlahLunas instanceof Number ? ((Number) jumlahLunas).longValue() : 0L;
			long nBelum = jumlahBelum instanceof Number ? ((Number) jumlahBelum).longValue() : 0L;
			long nOnline = jumlahOnline instanceof Number ? ((Number) jumlahOnline).longValue() : 0L;
			JSONObject ringkasan = new JSONObject();
			ringkasan.put("total", nSemua);
			ringkasan.put("online", nOnline);
			ringkasan.put("tertahan", Math.max(0L, nBelum - nOnline));
			ringkasan.put("terbayar", nLunas);
			ringkasan.put("nilaiMenunggu", nilaiMenunggu instanceof Number ? ((Number) nilaiMenunggu).doubleValue() : 0D);
			hasil.put("ringkasan", ringkasan);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Criteria tunggal agar filter data dan COUNT paging selalu identik. */
	private static Criteria buatCriteriaPesanan(Session session, Long tokoId, JSONObject payload) throws Exception {
		Criteria c = session.createCriteria(DraftPembelianAnggotaKoperasi.class);
		boolean butuhAliasToko = tokoId != null || !payload.optString("pedagang", "").trim().isEmpty();
		if (butuhAliasToko) c.createAlias("toko", "tk");
		if (tokoId != null) c.add(Restrictions.eq("tk.id", tokoId));
		String sejak = payload.optString("sejak", "").trim();
		String sampai = payload.optString("sampai", "").trim();
		if (!sejak.isEmpty() || !sampai.isEmpty()) {
			java.text.SimpleDateFormat fmtTgl = new java.text.SimpleDateFormat("yyyy-MM-dd");
			if (!sejak.isEmpty()) c.add(Restrictions.ge("tanggalPembayaran", fmtTgl.parse(sejak)));
			if (!sampai.isEmpty()) {
				java.util.Calendar akhirHari = java.util.Calendar.getInstance();
				akhirHari.setTime(fmtTgl.parse(sampai));
				akhirHari.set(java.util.Calendar.HOUR_OF_DAY, 23);
				akhirHari.set(java.util.Calendar.MINUTE, 59);
				akhirHari.set(java.util.Calendar.SECOND, 59);
				c.add(Restrictions.le("tanggalPembayaran", akhirHari.getTime()));
			}
		}
		String kode = payload.optString("kode", "").trim();
		if (!kode.isEmpty()) c.add(Restrictions.ilike("kode", kode, org.hibernate.criterion.MatchMode.ANYWHERE));
		String pembeli = payload.optString("pembeli", "").trim();
		if (!pembeli.isEmpty()) {
			c.createAlias("anggotaKoperasi", "ak")
					.add(Restrictions.ilike("ak.nama", pembeli, org.hibernate.criterion.MatchMode.ANYWHERE));
		}
		String pedagang = payload.optString("pedagang", "").trim();
		if (!pedagang.isEmpty()) c.add(Restrictions.ilike("tk.nama", pedagang, org.hibernate.criterion.MatchMode.ANYWHERE));
		String kasir = payload.optString("kasir", "").trim();
		if (!kasir.isEmpty()) c.add(Restrictions.eq("kasirLoginNama", kasir));
		if (payload.optBoolean("hanya_belum_lunas", false)) c.add(Restrictions.isNull("lunas"));
		String asal = payload.optString("asal", "").trim().toLowerCase();
		if ("online".equals(asal) || "tertahan".equals(asal)) {
			c.createAlias("tbmuser", "pu", org.hibernate.sql.JoinFragment.LEFT_OUTER_JOIN)
					.createAlias("pu.anggotaKoperasi", "pak", org.hibernate.sql.JoinFragment.LEFT_OUTER_JOIN);
			c.add("online".equals(asal) ? Restrictions.isNotNull("pak.id") : Restrictions.isNull("pak.id"));
		}
		return c;
	}

	/**
	 * Membatalkan (HARD DELETE header+seluruh baris item) satu pesanan yang BELUM lunas -- dipanggil
	 * dari tombol "Batalkan" di layar Pesanan lokal. Sengaja diimplementasikan LANGSUNG di sini
	 * (bukan method baru di {@code KantinHelper}) -- jalur existing utk aksi ini di web
	 * ({@code _draft_pesanan_anggota.jsp}) memakai framework delete generik ({@code /Data
	 * action:"deleteData"}) yang bergantung pada konteks cookie-session, TIDAK cocok dipanggil dari
	 * servlet berbasis token ini; logika penggantinya di sini sengaja dibuat SESEDERHANA mungkin
	 * (tanpa cascade/orphan-removal otomatis dari mapping entity, jadi baris item dihapus manual
	 * dulu baru header) drpd menambah method baru ke {@code KantinHelper} yang sudah diaudit rapi.
	 *
	 * <p>Pesanan yang SUDAH lunas TIDAK boleh dibatalkan lewat sini (sudah jadi transaksi final,
	 * pembatalannya berarti retur/void -- di luar cakupan Fase 3 ini, tetap harus lewat "Buka
	 * Aplikasi Lengkap").</p>
	 */
	/**
	 * Gerbang "supervisor-only" (gap-closure permintaan "edit/hapus/batal hanya supervisor") --
	 * admin global (tanpa Pedagang) ATAU supervisor toko ({@code Pedagang.getSupervisor()==true})
	 * boleh; kasir biasa DITOLAK. Logika SAMA PERSIS dgn flag {@code isAdmin}/{@code supervisorPedagang}
	 * yg sudah dikembalikan aksi {@code konfigurasi} (dipakai UI Desktop/Android menyembunyikan tombol)
	 * -- di sini gerbang SEBENARNYA ditegakkan, bukan cuma UI.
	 */
	private static boolean bolehSupervisorAtauAdmin(Tbmuser tbmuser) {
		Pedagang pedagang = tbmuser == null ? null : tbmuser.getPedagang();
		Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		return pedagang == null || Boolean.TRUE.equals(pedagang.getSupervisor())
				|| (role != null && ais.common.EbisnisMenuKatalog.urai(role.getEbisnisMenu()).optBoolean("supervisor", false));
	}

	/**
	 * Gerbang CRUD granular ({@code Tbmrole.ebisnisMenu.crud}) khusus menu "Pesanan" -- "layani
	 * transaksi" (tandai pesanan sudah dilayani) = aksi {@code approve}, "batal pesanan" = aksi
	 * {@code reject}. Default {@code true} (boleh) selama role belum pernah menyetel grid CRUD-nya --
	 * TIDAK mengubah perilaku akun yang sudah ada, hanya menambah cara baru utk MEMBATASI role tertentu.
	 */
	private static boolean bolehAksiCrudPesanan(Tbmuser tbmuser, String aksi) {
		Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		return ais.common.EbisnisMenuKatalog.bolehAksi(
				ais.common.EbisnisMenuKatalog.urai(role.getEbisnisMenu()), "pesanan", aksi);
	}

	/**
	 * <h3>Hook ekstensi aksi per-lini-produk (P1 varian "eBisnis Inventory &amp; Sales").</h3>
	 *
	 * <p>Dipanggil TEPAT sebelum fallback "Aksi tidak dikenal" -- SETELAH autentikasi token dan
	 * gate {@link #bolehAksesActionKantin} lolos, jadi implementasi override menerima
	 * {@code tbmuser} yang sudah terverifikasi. Default di {@code PosApi} ini SELALU {@code false}
	 * (tidak menangani apa pun): endpoint {@code /PosApi} lama TIDAK berubah perilakunya sama
	 * sekali. Subclass per lini produk ({@link ApiEBisnis}) meng-override utk mendaftarkan aksi
	 * barunya sendiri TANPA menggandakan autentikasi/CORS/parsing/normalisasi.</p>
	 *
	 * @return {@code true} bila aksi sudah ditangani (hasil terisi); {@code false} bila bukan
	 *         aksi milik subclass -- pemanggil menampilkan pesan "Aksi tidak dikenal" existing.
	 */
	protected boolean prosesAksiTambahan(String action, Tbmuser tbmuser, JSONObject payload, JSONObject hasil,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		return false;
	}

	private static boolean bolehAksesActionKantin(Tbmuser tbmuser, String action) {
		if (action == null || action.length() == 0) return true;
		// Administrator resmi selalu boleh mengakses seluruh permukaan menu/API,
		// sekalipun akun tersebut juga terikat ke Pedagang/Toko.
		if (Common.getApakahAdminLain(tbmuser)) return true;
		Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		if (role == null) return true;
		// Sumber tunggal: JSON konsolidasi Tbmrole.ebisnisMenu -- lihat ais.common.EbisnisMenuKatalog
		// (menggantikan 26 kolom Boolean akses* terpisah yang sebelumnya dibaca satu-satu di sini).
		org.json.JSONObject ebisnisMenuRole = ais.common.EbisnisMenuKatalog.urai(role.getEbisnisMenu());
		org.json.JSONObject menu = ebisnisMenuRole.optJSONObject("menu");
		ais.database.model.inventory.Pedagang pedagang = tbmuser.getPedagang();
		if (ebisnisMenuRole.optBoolean("supervisor", false)
				|| (pedagang != null && Boolean.TRUE.equals(pedagang.getSupervisor()))) return true;
		if ("konfigurasi".equals(action) || "daftar_toko_saya".equals(action)
				|| "pilih_toko_aktif".equals(action)
				|| "akun_ganti_password".equals(action)) return true;

		if ("katalog".equals(action)) {
			return menu.optBoolean("kasir", true) || menu.optBoolean("produk", true);
		}
		if ("bayar".equals(action) || "draft_bayar".equals(action)
				|| "checkBayar".equals(action) || "cari_member".equals(action)
				|| "cara_bayar_list".equals(action) || "saldo_member".equals(action)
				|| "topup_saldo".equals(action) || "verifikasi_pin".equals(action)
				|| "layar_pelanggan_kirim".equals(action) || "layar_pelanggan_ambil".equals(action)
				|| "survey_kepuasan_simpan".equals(action)) {
			return menu.optBoolean("kasir", true);
		}
		if ("cara_bayar_list_semua".equals(action)) {
			return menu.optBoolean("kasir", true) || menu.optBoolean("anggota", true);
		}
		if (action.startsWith("pesanan_") || "batal_pesanan".equals(action)
				|| "pesanan_list".equals(action) || "pesanan_hitung_ulang".equals(action)
				|| "layani_transaksi".equals(action) || "layani_semua_transaksi".equals(action)) {
			return menu.optBoolean("pesanan", true);
		}
		if ("ringkasan".equals(action) || "dashboard_umum".equals(action)
				|| "dashboard_keuangan".equals(action) || "dashboard_produk".equals(action)
				|| "dashboard_pelanggan".equals(action) || "transaksi_statistik".equals(action)
				|| "peringkat_mitra".equals(action) || "resep_hpp_margin".equals(action)
				|| "ramalan_penjualan".equals(action) || "monitor_promo_cashback".equals(action)
				|| "kepatuhan_operasional".equals(action)) {
			return menu.optBoolean("ringkasan", true);
		}
		if (action.startsWith("hotel_")) {
			// MitraInap fail-closed per-area (default false semua). Aksi granular
			// create/update tetap dicek lapis kedua di HotelApiHelper.boleh().
			//
			// KECUALI lookup room-charge: gerbang menu "kasir" (LANGKAH 4) -- kasir
			// OUTLET perlu memilih tamu in-house utk menagih penjualan ke folio tanpa
			// diberi menu front-desk; datanya minimal (nama tamu, nomor kamar).
			if ("hotel_room_charge_lookup".equals(action)) return menu.optBoolean("kasir", true);
			if (action.startsWith("hotel_properti_")) return menu.optBoolean("hotel_properti", false);
			if (action.startsWith("hotel_tipe_kamar_") || action.startsWith("hotel_kamar_")) return menu.optBoolean("hotel_kamar", false);
			if (action.startsWith("hotel_tamu_") || action.startsWith("hotel_reservasi_")) return menu.optBoolean("hotel_reservasi", false);
			// LANGKAH 6: konfirmasi manual pembayaran booking online -- area reservasi.
			if (action.startsWith("hotel_booking_")) return menu.optBoolean("hotel_reservasi", false);
			if ("hotel_checkin".equals(action) || "hotel_checkout".equals(action) || "hotel_pindah_kamar".equals(action)) return menu.optBoolean("hotel_checkin", false);
			if (action.startsWith("hotel_menginap_")) return menu.optBoolean("hotel_checkin", false);
			if (action.startsWith("hotel_folio_")) return menu.optBoolean("hotel_folio", false);
			// LANGKAH 5: layar dapur & kontrak/laporan pemilik -- fail-closed per-kunci.
			if (action.startsWith("hotel_kitchen_ticket_")) return menu.optBoolean("hotel_tiket_dapur", false);
			if (action.startsWith("hotel_kontrak_pemilik_")) return menu.optBoolean("hotel_kontrak_pemilik", false);
			if (action.startsWith("hotel_laporan_pemilik_")) return menu.optBoolean("hotel_laporan_pemilik", false);
			return false;
		}
		if (action.startsWith("grup_produk_")) {
			// FAIL-CLOSED (default false, selaras KUNCI_DEFAULT_NONAKTIF): menyimpan grup
			// mengubah harga di SEMUA toko -- role existing tidak boleh mendadak memilikinya.
			return menu.optBoolean("grup_produk", false);
		}
		if (action.startsWith("produk_") || action.startsWith("price_tag_")) {
			return menu.optBoolean("produk", true);
		}
		if (action.startsWith("stok_") || action.startsWith("so_")) {
			return menu.optBoolean("stokopname", true);
		}
		if (action.startsWith("anggota_") || action.startsWith("jenis_anggota_")
				|| action.startsWith("tipe_anggota_") || action.startsWith("deposit_")
				|| action.startsWith("notifikasi_") || action.startsWith("sinkron_")) {
			return menu.optBoolean("anggota", true);
		}
		if (action.startsWith("diskon_") || action.startsWith("pencairan_diskon_")) {
			return menu.optBoolean("diskon", true) || menu.optBoolean("kasir", true);
		}
		if (action.startsWith("kulakan_")) {
			return menu.optBoolean("kulakan", true);
		}
		if (action.startsWith("sesi_kas_")) {
			return menu.optBoolean("kaskasir", true) || menu.optBoolean("kasir", true);
		}
		if (action.startsWith("pedagang_") || "akun_tambah".equals(action)) {
			return menu.optBoolean("pedagang", true);
		}
		if (action.startsWith("toko_profil_")) {
			return menu.optBoolean("konfigurasi", true);
		}
		// "detail_transaksi" (Cetak Struk) dipakai dari DUA layar berbeda -- Laporan (menu "laporan")
		// DAN tombol "Cetak Struk" per-baris di dasbor Ringkasan (menu "ringkasan", lihat
		// prosesPesananList/prosesRingkasan) -- gap-closure: sebelumnya HANYA dicek via menu "laporan",
		// jadi kasir/pedagang yang punya akses "ringkasan" tapi bukan "laporan" bisa MELIHAT transaksi
		// di dasbor Ringkasan tapi ditolak server begitu menekan "Cetak Struk" pada baris yang sama.
		if ("detail_transaksi".equals(action) || "edit_transaksi".equals(action) || "edit_transaksi_kasir_cari".equals(action)) {
			return menu.optBoolean("laporan", true) || menu.optBoolean("ringkasan", true)
					|| menu.optBoolean("returpenjualan", true);
		}
		if ("laporan_order_list".equals(action)) {
			return menu.optBoolean("laporan", true) || menu.optBoolean("returpenjualan", true);
		}
		if (action.startsWith("laporan_")) {
			return menu.optBoolean("laporan", true);
		}
		if (action.startsWith("error_log_")) {
			return menu.optBoolean("logerror", true);
		}
		// Grup Produk (harga terpusat lintas toko): kunci default NONAKTIF
		// (KUNCI_DEFAULT_NONAKTIF) -- fail-closed, optBoolean(..., false). Aksi CRUD granular
		// dicek LAGI di GrupProdukApiHelper (dua lapis, pola si_/apotik_).
		if (action.startsWith("grup_produk_")) {
			return menu.optBoolean("grup_produk", false);
		}
		// -- Varian "eBisnis Inventory & Sales" (prefix si_): SELURUH kunci menu barunya default
		// NONAKTIF (EbisnisMenuKatalog.KUNCI_DEFAULT_NONAKTIF), jadi optBoolean(..., false) --
		// fail-closed: role lama yang belum pernah diaktifkan adminnya TIDAK bisa memanggil aksi
		// si_ apa pun. Prefix LEBIH SPESIFIK wajib dicek lebih dulu (si_supplier_price_ sebelum
		// si_supplier_, dst.). Aktor-level (ADMIN/PEMILIK/SALES) dicek LAGI di
		// SalesInventoryApiDispatcher -- dua lapis, bukan satu.
		if ("si_actor_context".equals(action)) {
			return true; // konteks diri sendiri -- setara "konfigurasi", tanpa data bisnis.
		}
		// -- Varian "POS Apotik" (prefix apotik_): kunci menu default NONAKTIF (fail-closed),
		// pola sama blok si_ di bawah -- optBoolean(..., false), prefix spesifik lebih dulu.
		// Provisioning demo UAT: lolos gate menu (self-guarded di handler: admin + token
		// konfirmasi + hanya server tanpa data SIRS) -- pola sama si_actor_context, supaya
		// tidak chicken-and-egg (butuh apotik utk menyiapkan apotik).
		if ("apotik_provision_demo".equals(action)) {
			return true;
		}
		if ("apotik_item_profil_simpan".equals(action)) {
			return menu.optBoolean("apotik_formularium", false);
		}
		if (action.startsWith("apotik_resep_")) {
			return menu.optBoolean("apotik_resep", false) || menu.optBoolean("apotik_kasir", false);
		}
		if (action.startsWith("apotik_item_")) {
			return menu.optBoolean("apotik_kasir", false) || menu.optBoolean("apotik_formularium", false)
					|| menu.optBoolean("apotik_stok_opname", false) || menu.optBoolean("apotik_batch", false);
		}
		if (action.startsWith("apotik_terima_")) {
			return menu.optBoolean("apotik_pengadaan", false);
		}
		if (action.startsWith("apotik_opname_")) {
			return menu.optBoolean("apotik_stok_opname", false);
		}
		if (action.startsWith("apotik_retur_")) {
			return menu.optBoolean("apotik_retur", false);
		}
		if (action.startsWith("apotik_batch_")) {
			return menu.optBoolean("apotik_batch", false) || menu.optBoolean("apotik_stok_opname", false)
					|| menu.optBoolean("apotik_kasir", false);
		}
		if (action.startsWith("apotik_laporan_")) {
			return menu.optBoolean("apotik_laporan", false);
		}
		if (action.startsWith("apotik_")) {
			return menu.optBoolean("apotik_kasir", false);
		}
		if (action.startsWith("si_supplier_price_") || action.startsWith("si_customer_price_")
				|| action.startsWith("si_price_") || action.startsWith("si_selling_price_")) {
			return menu.optBoolean("harga", false);
		}
		if (action.startsWith("si_supplier_")) {
			return menu.optBoolean("master_supplier", false);
		}
		if (action.startsWith("si_customer_")) {
			return menu.optBoolean("master_customer", false);
		}
		if (action.startsWith("si_sales_order_")) {
			return menu.optBoolean("penjualan_sales", false);
		}
		if (action.startsWith("si_sales_")) {
			return menu.optBoolean("master_sales", false);
		}
		if (action.startsWith("si_inventory_")) {
			return menu.optBoolean("persediaan", false);
		}
		if (action.startsWith("si_stock_count_")) {
			// Layar 9-10 reuse Stok Opname existing -- gate paritas dgn layar so_* lama.
			return menu.optBoolean("stokopname", true);
		}
		if (action.startsWith("si_stock_")) {
			return menu.optBoolean("persediaan", false);
		}
		if (action.startsWith("si_purchase_")) {
			// Layar 20/28/29 adalah perluasan Kulakan existing -- gate paritas dgn kulakan_* lama.
			return menu.optBoolean("kulakan", true);
		}
		if (action.startsWith("si_payable_")) {
			return menu.optBoolean("hutang", false);
		}
		if (action.startsWith("si_receivable_") || action.startsWith("si_collection_")) {
			return menu.optBoolean("piutang", false);
		}
		if (action.startsWith("si_spj_")) {
			return menu.optBoolean("surat_perintah_sales", false);
		}
		if (action.startsWith("si_trip_purchase_")) {
			return menu.optBoolean("pembelian_sales", false);
		}
		if (action.startsWith("si_trip_")) {
			return menu.optBoolean("nota_sales", false);
		}
		if (action.startsWith("si_expense_")) {
			return menu.optBoolean("biaya_sales", false);
		}
		if (action.startsWith("si_coa_") || action.startsWith("si_cash_journal_")) {
			return menu.optBoolean("kas_jurnal", false);
		}
		if (action.startsWith("si_profit_loss_") || action.startsWith("si_gross_profit_")) {
			return menu.optBoolean("laba_rugi", false);
		}
		if (action.startsWith("si_sync_")) {
			return menu.optBoolean("nota_sales", false) || menu.optBoolean("penjualan_sales", false);
		}
		if (action.startsWith("si_print_log_")) {
			// Register riwayat cetak: create boleh semua aktor varian (mencatat cetakan
			// miliknya); list dijaga Pemilik/Admin di helper.
			return true;
		}
		if (action.startsWith("si_audit_")) {
			// Riwayat Audit per record: gerbang menu detail per-entity ditegakkan helper
			// (petaAudit) -- di lapis menu cukup salah satu kunci varian aktif.
			return menu.optBoolean("master_supplier", false) || menu.optBoolean("master_customer", false)
					|| menu.optBoolean("master_sales", false) || menu.optBoolean("piutang", false)
					|| menu.optBoolean("penjualan_sales", false)
					|| menu.optBoolean("surat_perintah_sales", false);
		}
		if ("si_import_legacy".equals(action)) {
			// Impor DBF: gerbang aktor sesungguhnya (PEMILIK/ADMIN) di helper -- di lapis menu
			// cukup salah satu kunci master varian aktif.
			return menu.optBoolean("master_supplier", false) || menu.optBoolean("master_customer", false)
					|| menu.optBoolean("master_sales", false) || menu.optBoolean("harga", false);
		}
		if (action.startsWith("si_")) {
			return false; // prefix si_ tak dikenal = TOLAK (fail-closed), bukan jatuh ke default true.
		}
		return true;
	}

	private void prosesBatalPesanan(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		if (!bolehSupervisorAtauAdmin(tbmuser) && !bolehAksiCrudPesanan(tbmuser, "delete")
				&& !bolehAksiCrudPesanan(tbmuser, "reject")) {
			hasil.put("status", "error");
			hasil.put("message", "Hanya supervisor/admin yang boleh membatalkan pesanan.");
			return;
		}
		if (payload.isNull("id")) {
			hasil.put("status", "error");
			hasil.put("message", "ID pesanan wajib diisi.");
			return;
		}
		// Alasan wajib -- gap-closure: sebelumnya draft dihapus permanen TANPA jejak apa pun (bukan
		// cuma tak diarsipkan ke koperasi.pembatalan_transaksi spt batalkanTransaksi, malah tak
		// tercatat sama sekali di mana pun). PembatalanTransaksiUtil.batalkan HANYA menerima
		// PembelianAnggotaKoperasi (transaksi LUNAS), draft yg belum lunas ini di luar cakupannya --
		// cukup dicetak ke log server sblm dihapus (println, pola sama dgn diagnostik
		// [SESI-KAS-STATUS] di KantinHelper) drpd membangun tabel arsip baru utk draft yg toh belum
		// pernah berdampak stok/keuangan.
		String alasan = payload.isNull("alasan") ? "" : payload.optString("alasan", "").trim();
		if (alasan.isEmpty()) {
			hasil.put("status", "error");
			hasil.put("message", "Alasan pembatalan wajib diisi.");
			return;
		}
		Long draftId;
		try {
			draftId = Long.valueOf(Long.parseLong((payload.get("id") + "").trim()));
		} catch (Exception e) {
			hasil.put("status", "error");
			hasil.put("message", "ID pesanan tidak valid.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		org.hibernate.Transaction txBatal = null;
		try {
			DraftPembelianAnggotaKoperasi draft = (DraftPembelianAnggotaKoperasi) session
					.get(DraftPembelianAnggotaKoperasi.class, draftId);
			if (draft == null) {
				hasil.put("status", "error");
				hasil.put("message", "Pesanan tidak ditemukan (mungkin sudah dibatalkan sebelumnya).");
				return;
			}
			Long tokoLoginId = resolveTokoId(tbmuser, payload);
			if (tokoLoginId != null && (draft.getToko() == null || draft.getToko().getId() == null
					|| !tokoLoginId.equals(draft.getToko().getId()))) {
				hasil.put("status", "error");
				hasil.put("message", "Pesanan bukan milik toko yang sedang login.");
				return;
			}
			System.out.println("[BATAL-PESANAN] draftId=" + draftId + ", kode=" + draft.getKode()
					+ ", total=" + draft.getTotalBiaya() + ", oleh=" + (tbmuser == null ? "?" : tbmuser.getUserId())
					+ ", alasan=" + alasan);

			txBatal = session.beginTransaction();
			boolean sudahLunas = draft.getLunas() != null;
			if (sudahLunas) {
				ais.database.model.koperasi.PembelianAnggotaKoperasi transaksi = draft.getLunas();
				draft.setLunas(null);
				session.update(draft);
				session.flush();
				ais.action.master.koperasi.helper.PembatalanTransaksiUtil.batalkan(session, transaksi, alasan);
			}
			Criteria ci = session.createCriteria(DraftPembelian.class)
					.add(Restrictions.eq("draftPembelianAnggotaKoperasi", draft));
			for (Object oi : ci.list()) {
				session.delete(oi);
			}
			session.delete(draft);
			txBatal.commit();

			hasil.put("status", "success");
			hasil.put("description", sudahLunas
					? "Pembelian berhasil dibatalkan; stok dan saldo terkait telah dikoreksi serta jejak audit disimpan."
					: "Pesanan berhasil dibatalkan.");
		} catch (Exception e) {
			if (txBatal != null && txBatal.isActive()) txBatal.rollback();
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Ringkasan penjualan HARI INI (omzet, jumlah transaksi, qty terjual, 5 produk terlaris) milik
	 * toko kasir yang sedang login -- dipakai layar "Ringkasan" (Fase 2) sbg pengganti RINGAN dari
	 * dasbor Kantin penuh ({@code DashboardKantinAction}, 6 tab/14 panel) yang terlalu berat/kompleks
	 * utk direplikasi ke aplikasi lokal; cukup 4 angka+daftar singkat yg paling relevan buat kasir
	 * memantau performa hari berjalan tanpa harus buka "Aplikasi Lengkap (Online)".
	 *
	 * <p>Query diadaptasi LANGSUNG dari {@code DashboardKantinAction.buildRingkasan()}/
	 * {@code buildProdukStok()} (SQL native via JDBC, BUKAN HQL/Criteria -- pola yg sama dipakai
	 * krn Hibernate 3.6 native-SQL autodiscovery pernah salah tebak tipe kolom teks jadi double,
	 * lihat komentar {@code DashboardKantinAction.rows()}), tapi filter periode diganti dari
	 * "sejak cache" jadi HARI INI saja ({@code CURRENT_DATE}), dan filter toko WAJIB (bukan opsional
	 * spt dasbor admin) -- konsisten dgn {@link #resolveTokoId} yg mengunci kasir ke tokonya sendiri.
	 * Sengaja TIDAK pakai layer cache L1/L3 dasbor itu (query ringan, sekali-jalan per buka halaman,
	 * hasil pun sudah di-cache aplikasi desktop sendiri lewat {@code cache_referensi}).</p>
	 */
	private void prosesRingkasan(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		String andToko = tokoId == null ? "" : (" AND p.toko = " + tokoId);
		String where = "p.aktif = true AND p.waktu >= CURRENT_DATE AND p.waktu < CURRENT_DATE + INTERVAL '1 day'" + andToko;

		Session session = HibernateUtil.getSessionFactory().openSession();
		java.sql.Statement st = null;
		java.sql.ResultSet rs = null;
		try {
			java.sql.Connection conn = session.connection();
			st = conn.createStatement();

			double omzet = 0;
			long jumlahTransaksi = 0;
			double qtyTerjual = 0;
			rs = st.executeQuery("SELECT COALESCE(SUM(p.total),0), COUNT(DISTINCT p.id), COALESCE(SUM(p.qty),0) "
					+ "FROM koperasi.pembelian p WHERE " + where);
			if (rs.next()) {
				omzet = rs.getDouble(1);
				jumlahTransaksi = rs.getLong(2);
				qtyTerjual = rs.getDouble(3);
			}
			rs.close();
			rs = null;

			JSONArray terlaris = new JSONArray();
			rs = st.executeQuery("SELECT c.nama, COALESCE(SUM(p.qty),0) q FROM koperasi.pembelian p "
					+ "JOIN koperasi.produk c ON c.id = p.produk WHERE " + where
					+ " GROUP BY c.nama ORDER BY q DESC LIMIT 5");
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("nama", rs.getString(1) == null ? "" : rs.getString(1));
				j.put("qty", rs.getDouble(2));
				terlaris.put(j);
			}

			hasil.put("status", "success");
			hasil.put("omzetHariIni", omzet);
			hasil.put("transaksiHariIni", jumlahTransaksi);
			hasil.put("qtyTerjualHariIni", qtyTerjual);
			hasil.put("produkTerlaris", terlaris);
		} finally {
			if (rs != null) { try { rs.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) PosApi.prosesRingkasan.rs"); } }
			if (st != null) { try { st.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) PosApi.prosesRingkasan.st"); } }
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Pencarian anggota koperasi (member) aktif berdasar nama/kode identitas -- dipakai kasir memilih
	 * pembeli SEBELUM checkout pakai metode pembayaran "saldo" ({@code CaraPembayaranKoperasi.manual
	 * == false} berarti dipotong dari saldo terhitung member, BUKAN dibayar tunai/manual -- makna ini
	 * dikonfirmasi dari kode {@code _pos.jsp}, bukan tebakan). Versi web memuat SELURUH member sekaligus
	 * ke datalist browser
	 * (dataset kecil, terjamin sama-origin); versi lokal (token, jaringan lebih mahal) sengaja
	 * pencarian-per-kueri dgn {@code LIMIT 20} drpd meniru pendekatan itu.
	 *
	 * <p>{@code wajibPin}/{@code minSaldo} diambil dari {@link AnggotaKoperasi#getJenisAnggotaKoperasi()}
	 * (getter, BUKAN join Criteria) -- getter itu SUDAH menangani fallback bila kolom FK null (default
	 * ke jenis "Reguler"), lebih aman drpd join eksplisit yang bisa keliru exclude baris ber-FK null.
	 * N+1 query per member dibiarkan (bukan masalah performa nyata -- hasil dibatasi 20 baris).</p>
	 */
	/** Bentuk JSON member yg SAMA dipakai {@code cari_member} (baik jalur keyword maupun jalur exact-id di bawah) -- satu-satunya tempat field {id,nama,kodeIdentitas,wajibPin,minSaldo} dirakit, supaya kedua jalur selalu sinkron. */
	private JSONObject jsonMember(AnggotaKoperasi a) throws Exception {
		JenisAnggotaKoperasi jenis = a.getJenisAnggotaKoperasi();
		JSONObject j = new JSONObject();
		j.put("id", a.getId());
		j.put("nama", str(a.getNama()));
		j.put("kodeIdentitas", str(a.getKodeIdentitas()));
		j.put("wajibPin", jenis != null && Boolean.TRUE.equals(jenis.getWajibPin()));
		j.put("minSaldo", jenis == null || jenis.getMinimalSaldo() == null ? 0 : jenis.getMinimalSaldo());
		return j;
	}

	/**
	 * Cari member -- dua jalur: {@code keyword} (nama/kode identitas, MAKS 20 baris, dipakai picker
	 * member) ATAU {@code id} (exact lookup SATU baris, dipakai memulihkan member terpilih saat
	 * "Muat" Keranjang Tertahan -- Fase 5, lihat JavaDoc {@code prosesPesananList} soal {@code
	 * anggotaId} yg dikembalikan di sana). Kedua jalur berbagi bentuk JSON yg SAMA lewat {@link
	 * #jsonMember}.
	 */
	private void prosesCariMember(JSONObject payload, JSONObject hasil) throws Exception {
		String keyword = payload.optString("keyword", "").trim();
		Long idExact = payload.isNull("id") ? null : Long.valueOf((payload.get("id") + "").trim());
		if (keyword.isEmpty() && idExact == null) {
			hasil.put("status", "success");
			hasil.put("member", new JSONArray());
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JSONArray arr = new JSONArray();
			if (idExact != null) {
				AnggotaKoperasi a = (AnggotaKoperasi) session.get(AnggotaKoperasi.class, idExact);
				if (a != null && Boolean.TRUE.equals(a.getAktif())) {
					arr.put(jsonMember(a));
				}
			} else {
				Disjunction atau = Restrictions.disjunction();
				atau.add(Restrictions.ilike("nama", keyword, org.hibernate.criterion.MatchMode.ANYWHERE));
				atau.add(Restrictions.ilike("kodeIdentitas", keyword, org.hibernate.criterion.MatchMode.ANYWHERE));

				Criteria c = session.createCriteria(AnggotaKoperasi.class)
						.add(Restrictions.eq("aktif", true))
						.add(atau)
						.addOrder(Order.asc("nama"))
						.setMaxResults(20);

				for (Object o : c.list()) {
					arr.put(jsonMember((AnggotaKoperasi) o));
				}
			}

			hasil.put("status", "success");
			hasil.put("member", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * <h3>Fase 5 -- daftar metode bayar, TERFILTER per jenis-anggota bila {@code id_member} dikirim.</h3>
	 *
	 * <p>Porting 1:1 dari {@code _pos.jsp} fungsi {@code loadMetodePembayaranPOS} (lihat JavaDoc di
	 * sana): tanpa {@code id_member} (atau member itu tak punya {@code jenisAnggotaKoperasi}),
	 * kembalikan SEMUA metode bayar aktif (SAMA dgn {@code caraBayar} di {@code prosesKonfigurasi}) --
	 * dengan {@code id_member}, filter memakai kolom {@code jenis_anggota_koperasi.
	 * daftar_cara_pembayaran_yang_boleh_di_pilih} (teks berpembatas koma, dicek via {@code LIKE},
	 * IDENTIK query-nya dgn JSP -- bukan logika baru). Desktop/Android memanggil ini ULANG setiap kali
	 * member dipilih/dihapus di layar Kasir (sebelumnya kedua app itu hanya memuat {@code caraBayar}
	 * SEKALI saat start lewat {@code konfigurasi}, tak pernah menyaring ulang per member -- gap ini
	 * yang diperbaiki).</p>
	 *
	 * @param payload berisi {@code id_member} (opsional).
	 * @param hasil   diisi {@code status="success"}, {@code caraBayar} (array {@code {id, nama, manual}}).
	 */
	private void prosesCaraBayarList(JSONObject payload, JSONObject hasil) throws Exception {
		Long idMember = payload.isNull("id_member") ? null : Long.valueOf((payload.get("id_member") + "").trim());
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Long idJenisAnggota = null;
			if (idMember != null) {
				AnggotaKoperasi a = (AnggotaKoperasi) session.get(AnggotaKoperasi.class, idMember);
				if (a != null && a.getJenisAnggotaKoperasi() != null) {
					idJenisAnggota = a.getJenisAnggotaKoperasi().getId();
				}
			}

			JSONArray arr = new JSONArray();
			if (idJenisAnggota != null) {
				java.sql.PreparedStatement ps = session.connection().prepareStatement(
						"SELECT cpk.id, cpk.nama, cpk.manual, COALESCE(cpk.ada_kembalian, cpk.nama ILIKE '%tunai%') "
								+ "FROM koperasi.cara_pembayaran_koperasi cpk "
								+ "WHERE cpk.aktif = true AND (SELECT jak.daftar_cara_pembayaran_yang_boleh_di_pilih "
								+ "FROM koperasi.jenis_anggota_koperasi jak WHERE jak.id = ?) LIKE '%,' || cpk.id || ',%' "
								+ "ORDER BY cpk.nama ASC");
				ps.setLong(1, idJenisAnggota);
				java.sql.ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					JSONObject j = new JSONObject();
					j.put("id", rs.getLong(1));
					j.put("nama", rs.getString(2));
					j.put("manual", rs.getBoolean(3));
					j.put("adaKembalian", rs.getBoolean(4));
					arr.put(j);
				}
				rs.close();
				ps.close();
			} else {
				Criteria c = session.createCriteria(CaraPembayaranKoperasi.class)
						.add(Restrictions.eq("aktif", true)).addOrder(Order.asc("nama"));
				for (Object o : c.list()) {
					CaraPembayaranKoperasi cb = (CaraPembayaranKoperasi) o;
					JSONObject j = new JSONObject();
					j.put("id", cb.getId());
					j.put("nama", str(cb.getNama()));
					j.put("manual", cb.getManual() != null && cb.getManual());
					j.put("adaKembalian", cb.getAdaKembalian());
					arr.put(j);
				}
			}

			hasil.put("status", "success");
			hasil.put("caraBayar", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Verifikasi PIN transaksi member -- dipanggil dari jendela Layar Pelanggan lokal (lewat proses
	 * utama Electron, BUKAN langsung dari jendela itu -- lihat JavaDoc {@code main.js} handler
	 * {@code pos:verifikasi-pin}) SETELAH pembeli mengetik PIN di layar kedua. Logika PERSIS
	 * {@code verifikasi_pin_service.jsp} (bandingkan {@link AnggotaKoperasi#getPin()} plain, TIDAK
	 * di-hash -- konsisten dgn versi web, bukan pelemahan baru) -- PIN TIDAK PERNAH disimpan/dicatat
	 * di sini, hanya dibandingkan sekali lalu dibuang bersama akhir method.
	 */
	private void prosesVerifikasiPin(JSONObject payload, JSONObject hasil) throws Exception {
		if (payload.isNull("memberId") || payload.isNull("pin")) {
			hasil.put("status", "error");
			hasil.put("ok", false);
			hasil.put("message", "Member dan PIN wajib diisi.");
			return;
		}
		String pinInput = payload.optString("pin", "").trim();

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			AnggotaKoperasi a;
			try {
				Long memberId = Long.valueOf(Long.parseLong((payload.get("memberId") + "").trim()));
				a = (AnggotaKoperasi) session.get(AnggotaKoperasi.class, memberId);
			} catch (Exception e) {
				a = null;
			}

			if (a == null) {
				hasil.put("status", "error");
				hasil.put("ok", false);
				hasil.put("message", "Member tidak ditemukan.");
				return;
			}

			boolean ok = a.getPin() != null && a.getPin().equals(pinInput);
			hasil.put("status", "success");
			hasil.put("ok", ok);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Menyeragamkan balasan {@link KantinHelper#bayar}/{@code draft_bayar}/{@code checkBayar}/
	 * {@code tabungan} -- SEMUA method itu diwarisi apa adanya dari servlet {@code /Data} yang jauh
	 * lebih lama, memakai konvensi kode angka ({@code "00"}=sukses, {@code "91"}/{@code "90"}=exception,
	 * {@code "01"}=tidak ditemukan/parameter kurang, field pesan bernama {@code description}) -- BUKAN
	 * konvensi {@code status:"success"|"error"} + {@code message} yang dipakai method PosApi lain di
	 * kelas ini DAN yang diasumsikan {@code panggilPosApi()} di {@code main.js} (mengecek {@code
	 * json.status !== 'success'}). TANPA normalisasi ini, checkout yang BENAR-BENAR BERHASIL di
	 * database tetap dibaca klien sebagai "ditolak server" -- bug nyata yang ditemukan dari laporan
	 * pengguna (toast "Permintaan ditolak server." muncul walau transaksi sudah tersimpan), BUKAN
	 * sekadar soal pesan kurang jelas.
	 *
	 * <p>Kode ASLI tetap disimpan di {@code statusAsli} (tidak dibuang, berguna utk dukungan teknis).
	 * Field {@code kode} (BARU, hanya diisi saat gagal) berisi alasan singkat yang bisa dikenali klien
	 * secara PASTI (bukan menebak dari teks bebas) utk memilih panduan yang tepat di
	 * {@code pesan-detail.js} sisi Electron:</p>
	 * <ul>
	 *   <li>{@code DUPLIKAT_KODE_TRANSAKSI} -- pesan exception mengandung penanda pelanggaran UNIQUE
	 *       constraint kolom {@code kode} (migrasi idempotency Fase 1.5) -- BUKTI KUAT transaksi dgn
	 *       {@code kodeUnik} yang SAMA sudah berhasil tersimpan SEBELUMNYA (mis. retry manual kasir
	 *       setelah salah kira transaksi pertama gagal) -- klien harus diberi tahu utk MENGECEK
	 *       riwayat, BUKAN mengulang lagi.</li>
	 *   <li>{@code TIDAK_DITEMUKAN} -- khusus {@code checkBayar} status "01" -- kode pembayaran belum
	 *       terkonfirmasi gateway, ini KEADAAN NORMAL selagi menunggu (bukan error sungguhan).</li>
	 *   <li>{@code DATA_TIDAK_LENGKAP} -- {@code hasil} kosong TOTAL (validasi field wajib gagal DI
	 *       DALAM {@code KantinHelper} SEBELUM try/catch-nya sendiri terbuka -- method itu tidak
	 *       sempat mengisi {@code status} apa pun dlm kasus ini, lihat kondisi {@code toko==null} di
	 *       {@code bayar()}/{@code draft_bayar()}).</li>
	 *   <li>{@code SERVER_ERROR} -- fallback umum (exception tak terduga, deskripsi asli Java tetap
	 *       disertakan apa adanya di {@code message} utk konteks teknis).</li>
	 * </ul>
	 *
	 * @param hasil   balasan yang SUDAH diisi method KantinHelper terkait (dimodifikasi in-place).
	 * @param konteks {@code "checkout"} (bayar/draft_bayar), {@code "checkBayar"}, atau
	 *                {@code "saldo_member"} -- menentukan arti kode "01" (beda antara checkBayar dan
	 *                lainnya -- checkBayar TIDAK PERNAH mengembalikan "01" dari validasi payload PosApi
	 *                sendiri, jadi tak butuh cabang DATA_TIDAK_LENGKAP terpisah utk konteks itu).
	 */
	private static void normalisasiStatusKantinHelper(JSONObject hasil, String konteks) throws Exception {
		String asli = hasil.optString("status", "");
		hasil.put("statusAsli", asli);

		if ("00".equals(asli)) {
			hasil.put("status", "success");
			return;
		}

		String desc = hasil.optString("description", "");
		String teknis = hasil.optString("teknis", "");
		if (teknis.trim().length() == 0 && desc.trim().length() > 0) teknis = desc;
		String pesan;
		String kode;
		String judul = "Proses belum berhasil";
		JSONArray solusi = new JSONArray();

		if (asli.length() == 0) {
			kode = "DATA_TIDAK_LENGKAP";
			judul = "Data belum lengkap";
			pesan = "Beberapa data yang dibutuhkan belum tersedia. Tidak ada perubahan yang disimpan.";
			solusi.put("Periksa toko, produk, jumlah, dan metode pembayaran yang dipilih.")
					.put("Muat ulang halaman, lalu ulangi proses setelah seluruh data tampil.");
		} else if ("checkBayar".equals(konteks) && "01".equals(asli)) {
			kode = "TIDAK_DITEMUKAN";
			pesan = desc.length() > 0 ? desc : "Pembayaran belum terkonfirmasi.";
		} else {
			// Gunakan detail teknis juga untuk klasifikasi. KantinHelper sengaja
			// menyimpan pesan kasir yang aman di description dan exception asli di
			// teknis, sehingga pemetaan tetap akurat tanpa membocorkan stack trace.
			String descLower = (desc + "\n" + teknis).toLowerCase();
			if ((descLower.indexOf("rincian pesanan") >= 0 && descLower.indexOf("keranjang") >= 0)
					|| descLower.indexOf("produk rincian pesanan tidak sama") >= 0) {
				kode = "PESANAN_PERLU_DIMUAT_ULANG";
				judul = "Pesanan perlu dimuat ulang";
				pesan = "Isi pesanan di server berbeda dengan keranjang yang sedang tampil. Pembayaran dihentikan agar barang atau jumlah yang salah tidak tersimpan.";
				solusi.put("Tutup jendela pembayaran, lalu muat ulang daftar pesanan.")
						.put("Buka kembali pesanan tersebut dan periksa nama produk serta jumlahnya.")
						.put("Jika masih berbeda, jangan membuat transaksi pengganti. Salin Detail Error dan hubungi supervisor/admin.");
			} else if (descLower.indexOf("duplicate key") >= 0 || descLower.indexOf("unique constraint") >= 0) {
				kode = "DUPLIKAT_KODE_TRANSAKSI";
				judul = "Transaksi mungkin sudah tercatat";
				pesan = "Kode transaksi yang sama sudah ada di server. Pembayaran tidak diulang untuk mencegah transaksi ganda.";
				solusi.put("Periksa Riwayat Penjualan dan Riwayat Sinkronisasi.")
						.put("Jangan menekan Bayar kembali jika transaksi sudah tercatat.");
			} else if (descLower.indexOf("stok") >= 0 || descLower.indexOf("kadaluarsa") >= 0) {
				kode = descLower.indexOf("kadaluarsa") >= 0 ? "PRODUK_KADALUARSA" : "STOK_TIDAK_CUKUP";
				judul = descLower.indexOf("kadaluarsa") >= 0 ? "Produk tidak boleh dijual" : "Stok belum mencukupi";
				pesan = desc;
				solusi.put("Periksa produk dan jumlah di keranjang.")
						.put("Minta petugas stok melakukan pemeriksaan fisik atau stok opname bila jumlah di layar tidak sesuai.");
			} else {
				kode = "SERVER_ERROR";
				pesan = "Server belum dapat menyelesaikan proses ini. Tidak ada perubahan parsial yang dipertahankan.";
				solusi.put("Muat ulang halaman dan periksa kembali data yang diisi.")
						.put("Coba sekali lagi setelah beberapa saat.")
						.put("Jika berulang, buka Detail Error lalu salin informasinya untuk admin/developer.");
			}
		}

		hasil.put("status", "error");
		hasil.put("judul", judul);
		hasil.put("message", pesan);
		hasil.put("kode", kode);
		hasil.put("solusi", solusi);
		if (hasil.optString("referensi", "").trim().length() == 0) {
			hasil.put("referensi", "API-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase());
		}
		hasil.put("teknis", teknis.length() == 0
				? "konteks=" + konteks + "; statusInternal=" + asli
				: teknis);
	}

	private static String detailTeknis(Throwable error) {
		if (error == null) return "Tidak ada detail exception.";
		java.io.StringWriter sw = new java.io.StringWriter();
		java.io.PrintWriter pw = new java.io.PrintWriter(sw);
		error.printStackTrace(pw);
		pw.flush();
		return sw.toString();
	}

	/**
	 * Mengikat satu parameter {@code PreparedStatement} sesuai tipe Java-nya (Long->BIGINT,
	 * Integer->INT, string tanggal ISO {@code YYYY-MM-DD}->DATE, selain itu->teks). Binding tanggal
	 * sebagai {@code setDate()} penting utk kondisi seperti {@code DATE(a.waktu) >= ?}; kalau dikirim
	 * sebagai varchar, PostgreSQL bisa menolak dgn error operator {@code date >= character varying}.
	 * Dipakai method dashboard/laporan di bawah yg membangun WHERE clause dinamis (jumlah parameter
	 * berubah tergantung filter yg dikirim klien).
	 */
	private static void ikatParam(java.sql.PreparedStatement ps, int idx, Object v) throws Exception {
		if (v instanceof Long) ps.setLong(idx, ((Long) v).longValue());
		else if (v instanceof Integer) ps.setInt(idx, ((Integer) v).intValue());
		// Double/Float/BigDecimal WAJIB dibind numerik (setDouble), bukan lewat cabang String di bawah --
		// kalau lewat situ, angka spt 5000.0 terkirim sbg VARCHAR dan PostgreSQL menolak dgn error
		// "operator does not exist: double precision >= character varying" (lihat totalMinimal/
		// totalMaksimal/qtyMinimal/qtyMaksimal di daftarOrderDenganSesi -- keduanya dikirim ke sini
		// sbg Double.valueOf(...), jadi HARUS tertangkap di cabang ini, bukan jatuh ke setString()).
		else if (v instanceof java.math.BigDecimal) ps.setBigDecimal(idx, (java.math.BigDecimal) v);
		else if (v instanceof Double) ps.setDouble(idx, ((Double) v).doubleValue());
		else if (v instanceof Float) ps.setDouble(idx, ((Float) v).doubleValue());
		else if (v instanceof java.util.Date) ps.setDate(idx, new java.sql.Date(((java.util.Date) v).getTime()));
		else {
			String s = String.valueOf(v);
			if (s.matches("\\d{4}-\\d{2}-\\d{2}")) {
				java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
				fmt.setLenient(false);
				ps.setDate(idx, new java.sql.Date(fmt.parse(s).getTime()));
			} else {
				ps.setString(idx, s);
			}
		}
	}

	/** @return fragmen kondisi waktu utk kolom {@code a.waktu} sesuai kode periode dropdown (dipakai rekap produk terlaris/pelanggan terloyal). */
	private static String petaIntervalPeriode(String periode, String acuanSql) {
		if ("harian".equals(periode)) return "DATE(a.waktu) = " + acuanSql;
		if ("mingguan".equals(periode)) return "DATE(a.waktu) >= " + acuanSql + " - INTERVAL '7 days' AND DATE(a.waktu) <= " + acuanSql;
		if ("semester".equals(periode)) return "DATE(a.waktu) >= " + acuanSql + " - INTERVAL '6 months' AND DATE(a.waktu) <= " + acuanSql;
		if ("tahunan".equals(periode)) return "DATE(a.waktu) >= " + acuanSql + " - INTERVAL '1 year' AND DATE(a.waktu) <= " + acuanSql;
		return "DATE(a.waktu) >= " + acuanSql + " - INTERVAL '1 month' AND DATE(a.waktu) <= " + acuanSql; // bulanan, default
	}

	/**
	 * Tanggal acuan dashboard dari payload (yyyy-MM-dd; jatuh ke hari ini bila kosong/
	 * tidak valid). Divalidasi regex ketat karena nilainya disisipkan sebagai literal
	 * {@code DATE '...'} supaya urutan binding param toko yang sudah ada tidak berubah.
	 */
	private static String tanggalAcuanDariPayload(JSONObject payload) {
		String tanggalAcuan = payload.optString("tanggalAcuan", "").trim();
		if (!tanggalAcuan.matches("\\d{4}-\\d{2}-\\d{2}")) {
			tanggalAcuan = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
		}
		return tanggalAcuan;
	}

	/**
	 * Peta id produk -&gt; HPP (harga pokok penjualan) per unit: produk ber-resep (kolom {@code
	 * bahanbaku} JSON terisi) dihitung rollup (&Sigma; qty resep &times; hargabeli tiap bahan baku,
	 * parsing sama persis {@code BahanBakuUtil.konsumsiBahanBaku}); produk tanpa resep pakai
	 * {@code hargabeli} miliknya sendiri langsung. Port dari {@code DashboardKantinAction.petaHargaPokok()}
	 * (dasbor ZK admin) ke JDBC murni krn method aslinya terikat helper `qShared`/`rows` privat kelas
	 * itu -- BUKAN diekstrak jadi utilitas bersama supaya tidak menambah risiko/cakupan di luar
	 * servlet ini (lihat catatan file itu kalau suatu saat ingin disatukan).
	 */
	private static java.util.Map<Long, Double> petaHargaPokok(java.sql.Connection conn, Long tokoId) throws Exception {
		java.util.Map<Long, Double> peta = new java.util.HashMap<Long, Double>();
		java.util.List<Object[]> perluResep = new java.util.ArrayList<Object[]>();
		java.util.Set<Long> idBahan = new java.util.HashSet<Long>();

		java.sql.PreparedStatement psSemua = conn.prepareStatement(
				"SELECT id, COALESCE(bahanbaku,''), COALESCE(hargabeli,0) FROM koperasi.produk WHERE toko = ?");
		psSemua.setLong(1, tokoId.longValue());
		java.sql.ResultSet rsSemua = psSemua.executeQuery();
		while (rsSemua.next()) {
			long id = rsSemua.getLong(1);
			String resepJson = rsSemua.getString(2).trim();
			double beli = rsSemua.getDouble(3);
			JSONArray items = null;
			if (!resepJson.isEmpty() && !resepJson.equals("[]")) {
				try {
					JSONArray parsed = new JSONArray(resepJson);
					if (parsed.length() > 0) items = parsed;
				} catch (Exception e) { items = null; }
			}
			if (items == null) {
				peta.put(Long.valueOf(id), Double.valueOf(beli));
				continue;
			}
			perluResep.add(new Object[] { Long.valueOf(id), items });
			for (int i = 0; i < items.length(); i++) {
				JSONObject b = items.optJSONObject(i);
				long pid = b == null ? 0 : b.optLong("produk", 0);
				if (pid > 0) idBahan.add(Long.valueOf(pid));
			}
		}
		rsSemua.close();
		psSemua.close();

		java.util.Map<Long, Double> hargaBahan = new java.util.HashMap<Long, Double>();
		if (!idBahan.isEmpty()) {
			StringBuilder ids = new StringBuilder();
			for (Long id : idBahan) {
				if (ids.length() > 0) ids.append(',');
				ids.append(id);
			}
			java.sql.Statement stBahan = conn.createStatement();
			java.sql.ResultSet rsBahan = stBahan.executeQuery("SELECT id, COALESCE(hargabeli,0) FROM koperasi.produk WHERE id IN (" + ids + ")");
			while (rsBahan.next()) hargaBahan.put(Long.valueOf(rsBahan.getLong(1)), Double.valueOf(rsBahan.getDouble(2)));
			rsBahan.close();
			stBahan.close();
		}

		for (Object[] pr : perluResep) {
			long id = ((Long) pr[0]).longValue();
			JSONArray items = (JSONArray) pr[1];
			double hpp = 0;
			for (int j = 0; j < items.length(); j++) {
				JSONObject b = items.optJSONObject(j);
				if (b == null) continue;
				Double harga = hargaBahan.get(Long.valueOf(b.optLong("produk", 0)));
				hpp += b.optDouble("qty", 0) * (harga == null ? 0 : harga.doubleValue());
			}
			peta.put(Long.valueOf(id), Double.valueOf(hpp));
		}
		return peta;
	}

	/** @return {qty, pembeli(DISTINCT anggota), total} utk satu toko pada rentang waktu {@code kondisiWaktu} (fragmen SQL siap pakai, mis. {@code "DATE(a.waktu)=CURRENT_DATE"}). */
	private static JSONObject performaPeriode(java.sql.Connection conn, Long tokoId, String kondisiWaktu) throws Exception {
		java.sql.PreparedStatement ps = conn.prepareStatement(
				"SELECT COALESCE(SUM(a.qty),0), COUNT(DISTINCT a.anggota_koperasi), COALESCE(SUM(a.total),0) "
						+ "FROM koperasi.pembelian a WHERE a.toko = ? AND " + kondisiWaktu);
		ps.setLong(1, tokoId.longValue());
		java.sql.ResultSet rs = ps.executeQuery();
		JSONObject o = new JSONObject();
		if (rs.next()) { o.put("qty", rs.getDouble(1)); o.put("pembeli", rs.getLong(2)); o.put("total", rs.getDouble(3)); }
		else { o.put("qty", 0); o.put("pembeli", 0); o.put("total", 0); }
		rs.close();
		ps.close();
		return o;
	}

	/**
	 * <h3>Tab "Ringkasan Umum"</h3> -- port dari {@code dashboard.jsp} versi web (`_info_transaksi_header.jsp`
	 * + `_forecast.jsp` + `_riwayat_transaksi_terbaru.jsp`), SQL ditulis ulang sbg native JDBC
	 * ber-parameter (BUKAN string SQL dari klien spt `/Data action:"sql"` yg dipakai versi asli --
	 * pola itu SENGAJA tidak ditiru krn rawan SQL-injection, lihat riset yg jadi dasar port ini).
	 *
	 * <p>Payload: {@code periodeTren} ("harian"/"mingguan"/"bulanan", default harian), {@code
	 * tglMulai}/{@code tglSampai} (filter riwayat transaksi, opsional), {@code cariPembeli}
	 * (opsional), {@code kodeTransaksi}/{@code kode} (opsional, cari nomor transaksi/nota),
	 * {@code page}/{@code pageSize} (paginasi riwayat transaksi).</p>
	 */
	private void prosesDashboardUmum(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		// Gap-closure paritas dgn ZK DashboardKantinAction.buildRingkasan() -- admin global (pedagang
		// == null) BOLEH melihat SEMUA toko sekaligus bila tak eksplisit memilih satu (tokoId tetap
		// null), BUKAN error "Toko tidak diketahui". Pedagang/kasir toko TETAP wajib toko diketahui
		// (resolveTokoId sudah mengunci ke toko pedagang itu sendiri, jadi tokoId==null di sini HANYA
		// mungkin terjadi utk admin -- lihat javadoc resolveTokoId).
		boolean semuaToko = (tokoId == null);
		if (semuaToko && tbmuser.getPedagang() != null) {
			// Tak seharusnya tercapai (pedagang selalu punya toko terkunci) -- fail-safe murni.
			hasil.put("status", "error"); hasil.put("message", "Toko tidak diketahui utk akun ini."); return;
		}
		String kondisiToko = semuaToko ? "" : " AND toko = ?";

		String periodeTren = payload.optString("periodeTren", "harian");
		String tglMulai = payload.optString("tglMulai", "");
		String tglSampai = payload.optString("tglSampai", "");
		// Tanggal Acuan: SELURUH kartu KPI dihitung SAMPAI tanggal ini (bukan
		// selalu CURRENT_DATE) -- sebelumnya param ini diabaikan sehingga mundur
		// ke kemarin menampilkan omzet 0 (kartu berlabel acuan, angka hari ini).
		// Divalidasi regex ketat lalu disisipkan sbg literal DATE agar urutan
		// binding param toko yang sudah ada tidak berubah.
		String tanggalAcuan = tanggalAcuanDariPayload(payload);
		String acuanSql = "DATE '" + tanggalAcuan + "'";
		String cariPembeli = payload.optString("cariPembeli", "").trim();
		String kodeTransaksi = payload.optString("kodeTransaksi", "").trim();
		if (kodeTransaksi.length() == 0) kodeTransaksi = payload.optString("kode", "").trim();
		int page = Math.max(1, payload.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(5, payload.optInt("pageSize", 10)));
		int offset = (page - 1) * pageSize;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();

			// ---- 4 kartu KPI: Hari Ini / Minggu Ini / Bulan Ini / Semester Ini ----
			// Satu baris subquery = satu NOTA, bukan satu item. Sebelumnya COUNT(DISTINCT id)
			// menghitung baris detail sehingga Dashboard bisa menampilkan 724 sementara
			// Riwayat Penjualan menampilkan 361 nota. Nilai resmi memakai total header bila
			// tersedia dan fallback ke jumlah detail untuk transaksi legacy.
			String subKpi = "SELECT COALESCE(a.pembelian_anggota_koperasi,a.id) id_trx,MAX(a.waktu) waktu,"
					+ "COALESCE(MAX(h.total_biaya),SUM(a.total)) total_nota FROM koperasi.pembelian a "
					+ "LEFT JOIN koperasi.pembelian_anggota_koperasi h ON h.id=a.pembelian_anggota_koperasi "
					+ "WHERE COALESCE(a.aktif,true)=true" + (semuaToko ? "" : " AND a.toko=?") + " GROUP BY 1";
			// Semua jendela dihitung SAMPAI tanggal acuan (hari = acuan itu sendiri;
			// minggu/bulan = awal periode acuan s.d. acuan; 6 bulan = acuan-6bln s.d.
			// acuan) -- selaras teks UI "dihitung sampai tanggal ini".
			String hariAcuan = "DATE(waktu)=" + acuanSql;
			String mingguAcuan = "DATE(waktu)>=DATE_TRUNC('week'," + acuanSql + ") AND DATE(waktu)<=" + acuanSql;
			String bulanAcuan = "DATE(waktu)>=DATE_TRUNC('month'," + acuanSql + ") AND DATE(waktu)<=" + acuanSql;
			String semesterAcuan = "waktu>=" + acuanSql + "-INTERVAL '6 months' AND DATE(waktu)<=" + acuanSql;
			java.sql.PreparedStatement psKpi = conn.prepareStatement(
					"SELECT "
							+ "COALESCE(SUM(CASE WHEN " + hariAcuan + " THEN total_nota ELSE 0 END),0), "
							+ "COALESCE(SUM(CASE WHEN " + hariAcuan + " THEN 1 ELSE 0 END),0), "
							+ "COALESCE(SUM(CASE WHEN " + mingguAcuan + " THEN total_nota ELSE 0 END),0), "
							+ "COALESCE(SUM(CASE WHEN " + mingguAcuan + " THEN 1 ELSE 0 END),0), "
							+ "COALESCE(SUM(CASE WHEN " + bulanAcuan + " THEN total_nota ELSE 0 END),0), "
							+ "COALESCE(SUM(CASE WHEN " + bulanAcuan + " THEN 1 ELSE 0 END),0), "
							+ "COALESCE(SUM(CASE WHEN " + semesterAcuan + " THEN total_nota ELSE 0 END),0), "
							+ "COALESCE(SUM(CASE WHEN " + semesterAcuan + " THEN 1 ELSE 0 END),0) "
							+ "FROM (" + subKpi + ") trx");
			if (!semuaToko) psKpi.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsKpi = psKpi.executeQuery();
			JSONObject kpi = new JSONObject();
			if (rsKpi.next()) {
				kpi.put("hariIni", kpiRpTrx(rsKpi.getDouble(1), rsKpi.getLong(2)));
				kpi.put("mingguIni", kpiRpTrx(rsKpi.getDouble(3), rsKpi.getLong(4)));
				kpi.put("bulanIni", kpiRpTrx(rsKpi.getDouble(5), rsKpi.getLong(6)));
				kpi.put("semesterIni", kpiRpTrx(rsKpi.getDouble(7), rsKpi.getLong(8)));
			}
			rsKpi.close();
			psKpi.close();

			// ---- Rekap 7 hari terakhir s.d. tanggal acuan ----
			// Klien (tab_umum "Rekap 7 Hari Terakhir") merender field rekap7Hari:
			// [{tanggal:'yyyy-MM-dd', trx, rp, tanggalAcuan:bool}] -- SEMUA 7 hari
			// dikirim (hari tanpa transaksi = 0) supaya grid-nya utuh.
			java.sql.PreparedStatement psRekap = conn.prepareStatement(
					"SELECT TO_CHAR(DATE(waktu),'YYYY-MM-DD'),COUNT(*),COALESCE(SUM(total_nota),0) "
							+ "FROM (" + subKpi + ") r WHERE DATE(waktu)>" + acuanSql + "-INTERVAL '7 days'"
							+ " AND DATE(waktu)<=" + acuanSql + " GROUP BY 1");
			if (!semuaToko) psRekap.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsRekap = psRekap.executeQuery();
			java.util.Map<String, double[]> rekapPerTanggal = new java.util.HashMap<String, double[]>();
			while (rsRekap.next()) {
				rekapPerTanggal.put(rsRekap.getString(1),
						new double[] { rsRekap.getLong(2), rsRekap.getDouble(3) });
			}
			rsRekap.close();
			psRekap.close();
			JSONArray rekap7Hari = new JSONArray();
			java.text.SimpleDateFormat fmtRekap = new java.text.SimpleDateFormat("yyyy-MM-dd");
			java.util.Calendar kalRekap = java.util.Calendar.getInstance();
			kalRekap.setTime(fmtRekap.parse(tanggalAcuan));
			kalRekap.add(java.util.Calendar.DAY_OF_MONTH, -6);
			for (int i = 0; i < 7; i++) {
				String tgl = fmtRekap.format(kalRekap.getTime());
				double[] nilai = rekapPerTanggal.get(tgl);
				JSONObject baris = new JSONObject();
				baris.put("tanggal", tgl);
				baris.put("trx", nilai == null ? 0 : (long) nilai[0]);
				baris.put("rp", nilai == null ? 0.0 : nilai[1]);
				baris.put("tanggalAcuan", tgl.equals(tanggalAcuan));
				rekap7Hari.put(baris);
				kalRekap.add(java.util.Calendar.DAY_OF_MONTH, 1);
			}

			// ---- Tren transaksi (harian/mingguan/bulanan) ----
			String intervalSql, groupSql, labelFmt;
			if ("bulanan".equals(periodeTren)) { intervalSql = "12 months"; groupSql = "DATE_TRUNC('month', waktu)"; labelFmt = "Mon YYYY"; }
			else if ("mingguan".equals(periodeTren)) { intervalSql = "8 weeks"; groupSql = "DATE_TRUNC('week', waktu)"; labelFmt = "DD Mon"; }
			else { intervalSql = "14 days"; groupSql = "DATE(waktu)"; labelFmt = "DD Mon"; }
			String subTren = "SELECT COALESCE(a.pembelian_anggota_koperasi,a.id) id_trx,MAX(a.waktu) waktu,"
					+ "COALESCE(MAX(h.total_biaya),SUM(a.total)) nilai FROM koperasi.pembelian a "
					+ "LEFT JOIN koperasi.pembelian_anggota_koperasi h ON h.id=a.pembelian_anggota_koperasi "
					+ "WHERE COALESCE(a.aktif,true)=true AND a.waktu>=" + acuanSql + "-INTERVAL '" + intervalSql + "' AND DATE(a.waktu)<=" + acuanSql
					+ (semuaToko ? "" : " AND a.toko=?") + " GROUP BY 1";
			java.sql.PreparedStatement psTren = conn.prepareStatement(
					"SELECT TO_CHAR(" + groupSql + ",'" + labelFmt + "') AS lbl,COUNT(*),COALESCE(SUM(nilai),0),"
							+ "COALESCE(AVG(nilai),0),COALESCE(MIN(nilai),0),COALESCE(MAX(nilai),0),"
							+ "COALESCE((ARRAY_AGG(nilai ORDER BY waktu ASC))[1],0),COALESCE((ARRAY_AGG(nilai ORDER BY waktu DESC))[1],0) "
							+ "FROM (" + subTren + ") tren GROUP BY " + groupSql + " ORDER BY " + groupSql + " ASC");
			if (!semuaToko) psTren.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsTren = psTren.executeQuery();
			JSONArray tren = new JSONArray();
			while (rsTren.next()) {
				JSONObject t = new JSONObject();
				t.put("label", str(rsTren.getString(1)));
				t.put("jumlah", rsTren.getLong(2));
				t.put("transaksi", rsTren.getLong(2));
				t.put("omzet", rsTren.getDouble(3));
				t.put("rataRata", rsTren.getDouble(4));
				t.put("low", rsTren.getDouble(5));
				t.put("high", rsTren.getDouble(6));
				t.put("open", rsTren.getDouble(7));
				t.put("close", rsTren.getDouble(8));
				tren.put(t);
			}
			rsTren.close();
			psTren.close();

			// ---- Riwayat transaksi (dikelompokkan per transaksi, difilter+dipaginasi) ----
			StringBuilder whereTrx = new StringBuilder(semuaToko ? "1=1" : "a.toko = ?");
			java.util.List<Object> paramsTrx = new java.util.ArrayList<Object>();
			if (!semuaToko) paramsTrx.add(tokoId);
			if (tglMulai.length() > 0) { whereTrx.append(" AND DATE(a.waktu) >= ?"); paramsTrx.add(tglMulai); }
			if (tglSampai.length() > 0) { whereTrx.append(" AND DATE(a.waktu) <= ?"); paramsTrx.add(tglSampai); }
			if (kodeTransaksi.length() > 0) {
				whereTrx.append(" AND (CAST(COALESCE(a.pembelian_anggota_koperasi, a.id) AS varchar) ILIKE ? "
						+ "OR COALESCE(a.kode,'') ILIKE ? "
						+ "OR EXISTS (SELECT 1 FROM koperasi.pembelian_anggota_koperasi pak "
						+ "WHERE pak.id = a.pembelian_anggota_koperasi AND COALESCE(pak.kode,'') ILIKE ?))");
				String qKode = "%" + kodeTransaksi + "%";
				paramsTrx.add(qKode);
				paramsTrx.add(qKode);
				paramsTrx.add(qKode);
			}
			String havingCari = cariPembeli.length() > 0 ? " HAVING MAX(a.member) ILIKE ?" : "";

			java.sql.PreparedStatement psCount = conn.prepareStatement(
					"SELECT COUNT(*) FROM (SELECT COALESCE(a.pembelian_anggota_koperasi, a.id) FROM koperasi.pembelian a WHERE "
							+ whereTrx + " GROUP BY 1" + havingCari + ") x");
			int idx = 1;
			for (Object p : paramsTrx) ikatParam(psCount, idx++, p);
			if (cariPembeli.length() > 0) psCount.setString(idx++, "%" + cariPembeli + "%");
			java.sql.ResultSet rsCount = psCount.executeQuery();
			long totalTrx = rsCount.next() ? rsCount.getLong(1) : 0;
			rsCount.close();
			psCount.close();

			java.sql.PreparedStatement psData = conn.prepareStatement(
					"SELECT COALESCE(a.pembelian_anggota_koperasi, a.id) AS id_transaksi, MAX(a.waktu) AS waktu, "
							+ "STRING_AGG(COALESCE(c.nama,'(Produk Dihapus)') || ' (' || a.qty || ')', ', ') AS namabarang, "
							+ "MAX(a.member) AS member, MAX(a.jenismember) AS jenismember, MAX(a.carabayar) AS carabayar, "
							+ "SUM(a.qty) AS qty, SUM(a.total) AS total, BOOL_AND(COALESCE(a.terlayani,false)) AS terlayani "
							+ "FROM koperasi.pembelian a LEFT JOIN koperasi.produk c ON c.id = a.produk "
							+ "WHERE " + whereTrx + " GROUP BY 1" + havingCari + " ORDER BY waktu DESC LIMIT ? OFFSET ?");
			idx = 1;
			for (Object p : paramsTrx) ikatParam(psData, idx++, p);
			if (cariPembeli.length() > 0) psData.setString(idx++, "%" + cariPembeli + "%");
			psData.setInt(idx++, pageSize);
			psData.setInt(idx++, offset);
			java.sql.ResultSet rsData = psData.executeQuery();
			JSONArray transaksiArr = new JSONArray();
			while (rsData.next()) {
				JSONObject t = new JSONObject();
				t.put("idTransaksi", rsData.getLong(1));
				java.sql.Timestamp w = rsData.getTimestamp(2);
				t.put("waktu", w == null ? "" : w.toString());
				t.put("barang", str(rsData.getString(3)));
				t.put("pembeli", str(rsData.getString(4)));
				t.put("tipeAnggota", str(rsData.getString(5)));
				t.put("metode", str(rsData.getString(6)));
				t.put("qty", rsData.getDouble(7));
				t.put("total", rsData.getDouble(8));
				t.put("terlayani", rsData.getBoolean(9));
				transaksiArr.put(t);
			}
			rsData.close();
			psData.close();

			// Gap-closure paritas dgn ZK buildRingkasan() -- 3 chart tambahan (sebaran metode bayar,
			// omzet per kategori produk, jam sibuk), di-scope periode YANG SAMA dgn riwayat transaksi
			// di atas (tglMulai/tglSampai bila diisi; default 30 hari terakhir bila keduanya kosong --
			// TANPA batas ini query bisa memindai SELURUH histori toko, mahal & tak relevan utk chart
			// ringkasan "kondisi terkini").
			StringBuilder whereChart = new StringBuilder(semuaToko ? "1=1" : "p.toko = ?");
			java.util.List<Object> paramsChart = new java.util.ArrayList<Object>();
			if (!semuaToko) paramsChart.add(tokoId);
			if (tglMulai.length() > 0 || tglSampai.length() > 0) {
				if (tglMulai.length() > 0) { whereChart.append(" AND DATE(p.waktu) >= ?"); paramsChart.add(tglMulai); }
				if (tglSampai.length() > 0) { whereChart.append(" AND DATE(p.waktu) <= ?"); paramsChart.add(tglSampai); }
			} else {
				// Default 30 hari mengikuti TANGGAL ACUAN (bukan selalu hari ini) --
				// selaras kartu KPI; mundur acuan = seluruh rekap detail ikut mundur.
				whereChart.append(" AND DATE(p.waktu) > " + acuanSql + " - INTERVAL '30 days'"
						+ " AND DATE(p.waktu) <= " + acuanSql);
			}
			String kondisiChart = whereChart.toString();

			JSONArray metodeBayar = new JSONArray();
			java.sql.PreparedStatement psBayar = conn.prepareStatement(
					"SELECT COALESCE(NULLIF(TRIM(CAST(p.carabayar AS varchar)),''),'Lainnya') lbl, COALESCE(SUM(p.total),0) nilai "
							+ "FROM koperasi.pembelian p WHERE " + kondisiChart + " GROUP BY 1 ORDER BY 2 DESC");
			idx = 1;
			for (Object p : paramsChart) ikatParam(psBayar, idx++, p);
			java.sql.ResultSet rsBayar = psBayar.executeQuery();
			while (rsBayar.next()) {
				JSONObject o = new JSONObject();
				o.put("label", str(rsBayar.getString(1)));
				o.put("nilai", rsBayar.getDouble(2));
				metodeBayar.put(o);
			}
			rsBayar.close(); psBayar.close();

			JSONArray omzetKategori = new JSONArray();
			java.sql.PreparedStatement psKategori = conn.prepareStatement(
					"SELECT COALESCE(jp.nama,'Lainnya') lbl, COALESCE(SUM(p.total),0) nilai, COALESCE(SUM(p.qty),0) qty FROM koperasi.pembelian p "
							+ "LEFT JOIN koperasi.produk c ON c.id = p.produk LEFT JOIN koperasi.jenis_produk jp ON jp.id = c.jenis_produk "
							+ "WHERE " + kondisiChart + " GROUP BY 1 ORDER BY 2 DESC LIMIT 8");
			idx = 1;
			for (Object p : paramsChart) ikatParam(psKategori, idx++, p);
			java.sql.ResultSet rsKategori = psKategori.executeQuery();
			while (rsKategori.next()) {
				JSONObject o = new JSONObject();
				o.put("label", str(rsKategori.getString(1)));
				o.put("nilai", rsKategori.getDouble(2));
				o.put("qty", rsKategori.getDouble(3));
				omzetKategori.put(o);
			}
			rsKategori.close(); psKategori.close();

			// Kartu keputusan tambahan memakai periode dan scope toko yang SAMA dengan chart.
			// Nilai per kasir/toko/produk dijumlahkan dari baris item (p.total), sedangkan jumlah
			// transaksi memakai id header agar satu nota berisi banyak item tetap dihitung sekali.
			JSONArray omzetKasir = new JSONArray();
			java.sql.PreparedStatement psKasir = conn.prepareStatement(
					"SELECT COALESCE(NULLIF(TRIM(pak.kasir_login_nama),''),NULLIF(TRIM(u.usernama),''),'Tidak diketahui') lbl, "
							+ "COALESCE(SUM(p.total),0) nilai, COUNT(DISTINCT COALESCE(p.pembelian_anggota_koperasi,p.id)) trx "
							+ "FROM koperasi.pembelian p "
							+ "LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON pak.id=p.pembelian_anggota_koperasi "
							+ "LEFT JOIN public.tbmuser u ON u.userid=pak.tbmuser "
							+ "WHERE " + kondisiChart + " GROUP BY 1 ORDER BY 2 DESC LIMIT 8");
			idx = 1;
			for (Object p : paramsChart) ikatParam(psKasir, idx++, p);
			java.sql.ResultSet rsKasir = psKasir.executeQuery();
			while (rsKasir.next()) {
				JSONObject o = new JSONObject();
				o.put("label", str(rsKasir.getString(1)));
				o.put("nilai", rsKasir.getDouble(2));
				o.put("trx", rsKasir.getLong(3));
				omzetKasir.put(o);
			}
			rsKasir.close(); psKasir.close();

			JSONArray produkTerlaris = new JSONArray();
			java.sql.PreparedStatement psProduk = conn.prepareStatement(
					"SELECT COALESCE(NULLIF(TRIM(c.nama),''),'Produk dihapus') lbl, COALESCE(SUM(p.total),0) nilai, "
							+ "COALESCE(SUM(p.qty),0) qty FROM koperasi.pembelian p LEFT JOIN koperasi.produk c ON c.id=p.produk "
							+ "WHERE " + kondisiChart + " GROUP BY 1 ORDER BY 3 DESC,2 DESC LIMIT 8");
			idx = 1;
			for (Object p : paramsChart) ikatParam(psProduk, idx++, p);
			java.sql.ResultSet rsProduk = psProduk.executeQuery();
			while (rsProduk.next()) {
				JSONObject o = new JSONObject();
				o.put("label", str(rsProduk.getString(1)));
				o.put("nilai", rsProduk.getDouble(2));
				o.put("qty", rsProduk.getDouble(3));
				produkTerlaris.put(o);
			}
			rsProduk.close(); psProduk.close();

			JSONArray omzetToko = new JSONArray();
			if (semuaToko) {
				java.sql.PreparedStatement psToko = conn.prepareStatement(
						"SELECT COALESCE(NULLIF(TRIM(t.nama),''),'Toko tidak diketahui') lbl, COALESCE(SUM(p.total),0) nilai, "
								+ "COUNT(DISTINCT COALESCE(p.pembelian_anggota_koperasi,p.id)) trx FROM koperasi.pembelian p "
								+ "LEFT JOIN koperasi.toko t ON t.id=p.toko WHERE " + kondisiChart
								+ " GROUP BY 1 ORDER BY 2 DESC LIMIT 8");
				idx = 1;
				for (Object p : paramsChart) ikatParam(psToko, idx++, p);
				java.sql.ResultSet rsToko = psToko.executeQuery();
				while (rsToko.next()) {
					JSONObject o = new JSONObject();
					o.put("label", str(rsToko.getString(1)));
					o.put("nilai", rsToko.getDouble(2));
					o.put("trx", rsToko.getLong(3));
					omzetToko.put(o);
				}
				rsToko.close(); psToko.close();
			}

			JSONArray jamSibuk = new JSONArray();
			java.sql.PreparedStatement psJam = conn.prepareStatement(
					"SELECT CAST(EXTRACT(HOUR FROM p.waktu) AS integer) jam, COUNT(DISTINCT p.id) jumlah "
							+ "FROM koperasi.pembelian p WHERE " + kondisiChart + " GROUP BY 1 ORDER BY 1");
			idx = 1;
			for (Object p : paramsChart) ikatParam(psJam, idx++, p);
			java.sql.ResultSet rsJam = psJam.executeQuery();
			while (rsJam.next()) {
				JSONObject o = new JSONObject();
				o.put("label", String.format("%02d:00", rsJam.getInt(1)));
				o.put("nilai", rsJam.getLong(2));
				jamSibuk.put(o);
			}
			rsJam.close(); psJam.close();

			// Heatmap hari x jam memakai SATU baris per nota, bukan per item, agar
			// kepadatan operasional tidak bias pada transaksi yang banyak produknya.
			String subHeatmap = "SELECT COALESCE(p.pembelian_anggota_koperasi,p.id) id_trx,MAX(p.waktu) waktu,"
					+ "COALESCE(MAX(h.total_biaya),SUM(p.total)) nilai FROM koperasi.pembelian p "
					+ "LEFT JOIN koperasi.pembelian_anggota_koperasi h ON h.id=p.pembelian_anggota_koperasi "
					+ "WHERE " + kondisiChart + " GROUP BY 1";
			java.sql.PreparedStatement psHeatmap = conn.prepareStatement(
					"SELECT CAST(EXTRACT(ISODOW FROM waktu) AS integer),CAST(EXTRACT(HOUR FROM waktu) AS integer),"
							+ "COUNT(*),COALESCE(SUM(nilai),0) FROM (" + subHeatmap + ") trx GROUP BY 1,2 ORDER BY 1,2");
			idx = 1;
			for (Object p : paramsChart) ikatParam(psHeatmap, idx++, p);
			java.sql.ResultSet rsHeatmap = psHeatmap.executeQuery();
			JSONArray heatmap = new JSONArray();
			while (rsHeatmap.next()) {
				JSONObject o = new JSONObject(); o.put("hari", rsHeatmap.getInt(1)); o.put("jam", rsHeatmap.getInt(2));
				o.put("transaksi", rsHeatmap.getLong(3)); o.put("omzet", rsHeatmap.getDouble(4)); heatmap.put(o);
			}
			rsHeatmap.close(); psHeatmap.close();

			hasil.put("status", "success");
			hasil.put("semuaToko", semuaToko);
			hasil.put("kpi", kpi);
			hasil.put("tanggalAcuan", tanggalAcuan);
			hasil.put("rekap7Hari", rekap7Hari);
			hasil.put("tren", tren);
			hasil.put("metodeBayar", metodeBayar);
			hasil.put("omzetKategori", omzetKategori);
			hasil.put("omzetKasir", omzetKasir);
			hasil.put("produkTerlaris", produkTerlaris);
			hasil.put("omzetToko", omzetToko);
			hasil.put("jamSibuk", jamSibuk);
			hasil.put("heatmap", heatmap);
			JSONObject transaksiObj = new JSONObject();
			transaksiObj.put("data", transaksiArr);
			transaksiObj.put("total", totalTrx);
			transaksiObj.put("page", page);
			transaksiObj.put("pageSize", pageSize);
			hasil.put("transaksi", transaksiObj);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static JSONObject kpiRpTrx(double rp, long trx) throws Exception {
		JSONObject o = new JSONObject();
		o.put("rp", rp);
		o.put("trx", trx);
		return o;
	}

	/**
	 * Menandai SATU transaksi (dikelompokkan per {@code pembelian_anggota_koperasi} ATAU {@code id}
	 * berdiri sendiri) sebagai terlayani -- tombol "Layani" per baris di tab Ringkasan Umum.
	 * <b>Perbaikan keamanan dari versi asli</b>: query UPDATE versi web ({@code _riwayat_transaksi_terbaru.jsp})
	 * TIDAK memfilter {@code toko} sama sekali (celah IDOR -- kasir toko A bisa menandai transaksi
	 * toko B terlayani kalau tahu ID-nya) -- WAJIB ditambah {@code AND toko = ?} di sini.
	 */
	private void prosesLayaniTransaksi(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		if (!bolehAksiCrudPesanan(tbmuser, "approve")) {
			hasil.put("status", "error");
			hasil.put("message", "Grup pengguna Anda tidak diizinkan menyetujui/melayani pesanan.");
			return;
		}
		Long tokoId = resolveTokoId(tbmuser, payload);
		if (tokoId == null) { hasil.put("status", "error"); hasil.put("message", "Toko tidak diketahui utk akun ini."); return; }
		if (payload.isNull("id")) { hasil.put("status", "error"); hasil.put("message", "ID transaksi wajib diisi."); return; }
		long idTransaksi;
		try { idTransaksi = Long.parseLong((payload.get("id") + "").trim()); }
		catch (Exception e) { hasil.put("status", "error"); hasil.put("message", "ID transaksi tidak valid."); return; }

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"UPDATE koperasi.pembelian SET terlayani = true WHERE (pembelian_anggota_koperasi = ? OR id = ?) AND toko = ?");
			ps.setLong(1, idTransaksi);
			ps.setLong(2, idTransaksi);
			ps.setLong(3, tokoId.longValue());
			int jumlah = ps.executeUpdate();
			ps.close();
			session.getTransaction().commit();

			hasil.put("status", "success");
			hasil.put("jumlahBarisDiperbarui", jumlah);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Rincian SATU transaksi (header + baris item) utk tombol "Cetak Struk" di Desktop (layar
	 * Ringkasan, yang hanya punya data agregat per baris riwayat -- bukan rincian item -- lihat
	 * {@code struk.js}). Struktur data acuan sama persis dgn yang dipakai {@code cetak_struk.jsp}
	 * versi web (header dari {@code koperasi.pembelian_anggota_koperasi}, baris item dari
	 * {@code koperasi.pembelian} JOIN {@code koperasi.produk}), TAPI diporting ke query
	 * terparameterisasi (bukan {@code /Data action:"sql"} dgn string ter-concat yang dipakai versi
	 * JSP -- pola itu SUDAH ditandai rawan SQL-injection, jangan ditiru di jalur baru manapun).
	 *
	 * <p><b>IDOR guard</b>: SELALU disaring {@code AND toko = ?} pakai {@code tokoId} hasil
	 * {@link #resolveTokoId}, TIDAK PERNAH dari input klien -- kasir toko A tidak boleh bisa mencetak
	 * ulang struk milik toko B walau tahu ID transaksinya. Header tak ditemukan (id salah ATAU toko
	 * tak cocok) dibalas sbg "not found" polos, bukan data toko lain.</p>
	 */
	private void prosesDetailTransaksi(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		if (tokoId == null) { hasil.put("status", "error"); hasil.put("message", "Toko tidak diketahui utk akun ini."); return; }
		if (payload.isNull("id")) { hasil.put("status", "error"); hasil.put("message", "ID transaksi wajib diisi."); return; }
		long idTransaksi;
		try { idTransaksi = Long.parseLong((payload.get("id") + "").trim()); }
		catch (Exception e) { hasil.put("status", "error"); hasil.put("message", "ID transaksi tidak valid."); return; }

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

			java.sql.PreparedStatement psHeader = conn.prepareStatement(
					"SELECT pak.kode, pak.tanggal_pembayaran, COALESCE(pak.bayar_tunai,0), COALESCE(pak.bayar_non_tunai,0), "
							+ "COALESCE(pak.kembalian,0), COALESCE(pak.total_biaya,0), COALESCE(pak.total_diskon,0), COALESCE(pak.totalcashback,0), "
							+ "COALESCE(t.nama,''), COALESCE(NULLIF(t.pesan_terima_kasih,''), ?), pak.tbmuser, pak.kasir_login_nama, "
							+ "pak.cara_pembayaran_koperasi, COALESCE(cb.nama,'') "
							+ "FROM koperasi.pembelian_anggota_koperasi pak LEFT JOIN koperasi.toko t ON pak.toko = t.id "
							+ "LEFT JOIN koperasi.cara_pembayaran_koperasi cb ON pak.cara_pembayaran_koperasi = cb.id "
							+ "WHERE pak.id = ? AND pak.toko = ?");
			psHeader.setString(1, Toko.PESAN_TERIMA_KASIH_DEFAULT);
			psHeader.setLong(2, idTransaksi);
			psHeader.setLong(3, tokoId.longValue());
			java.sql.ResultSet rsHeader = psHeader.executeQuery();
			boolean punyaHeaderKelompok = rsHeader.next();
			if (punyaHeaderKelompok) {
				hasil.put("kode", str(rsHeader.getString(1)));
				hasil.put("waktu", rsHeader.getTimestamp(2) == null ? "" : fmt.format(rsHeader.getTimestamp(2)));
				hasil.put("bayarTunai", rsHeader.getDouble(3));
				hasil.put("bayarNonTunai", rsHeader.getDouble(4));
				hasil.put("kembalian", rsHeader.getDouble(5));
				hasil.put("totalBiaya", rsHeader.getDouble(6));
				hasil.put("totalDiskon", rsHeader.getDouble(7));
				hasil.put("totalCashback", rsHeader.getDouble(8));
				hasil.put("tokoNama", str(rsHeader.getString(9)));
				hasil.put("pesanTerimaKasih", str(rsHeader.getString(10)));
				hasil.put("kasirUserId", rsHeader.getString(11) == null ? JSONObject.NULL : rsHeader.getString(11));
				hasil.put("kasirNama", rsHeader.getString(12) == null ? "" : rsHeader.getString(12));
				long caraBayarId = rsHeader.getLong(13);
				hasil.put("caraBayarId", rsHeader.wasNull() ? JSONObject.NULL : caraBayarId);
				hasil.put("caraBayarNama", rsHeader.getString(14) == null ? "" : rsHeader.getString(14));
			}
			rsHeader.close(); psHeader.close();

			if (punyaHeaderKelompok) {
				// Gap-closure "Produk Ekstra" -- ORDER BY COALESCE(induk_id,id),id mengelompokkan baris
				// induk lalu ekstra-nya tepat sesudahnya (BUKAN urutan id mentah, yang bisa salah urut
				// krn baris ekstra bisa punya id lebih kecil dari baris induk LAIN yg tak terkait) --
				// klien (struk/riwayat) cukup render indent saat indukId != null, tanpa perlu grouping
				// sendiri.
				java.sql.PreparedStatement psItem = conn.prepareStatement(
						"SELECT COALESCE(pr.nama,''), p.qty, COALESCE(p.hargasatuan, (p.total / NULLIF(p.qty,0)), 0), COALESCE(p.diskon,0), COALESCE(p.cashback,0), p.produk, p.induk_id, p.id "
								+ "FROM koperasi.pembelian p LEFT JOIN koperasi.produk pr ON p.produk = pr.id "
								+ "WHERE p.pembelian_anggota_koperasi = ? AND p.toko = ? "
								+ "ORDER BY COALESCE(p.induk_id, p.id) ASC, p.id ASC");
				psItem.setLong(1, idTransaksi);
				psItem.setLong(2, tokoId.longValue());
				java.sql.ResultSet rsItem = psItem.executeQuery();
				JSONArray item = new JSONArray();
				while (rsItem.next()) {
					JSONObject j = new JSONObject();
					j.put("nama", str(rsItem.getString(1)));
					j.put("qty", rsItem.getDouble(2));
					j.put("harga", rsItem.getDouble(3));
					j.put("diskon", rsItem.getDouble(4));
					j.put("cashback", rsItem.getDouble(5));
					long produkIdItem = rsItem.getLong(6);
					j.put("produkId", rsItem.wasNull() ? JSONObject.NULL : produkIdItem);
					long indukIdItem = rsItem.getLong(7);
					j.put("indukId", rsItem.wasNull() ? JSONObject.NULL : indukIdItem);
					j.put("pembelianId", rsItem.getLong(8));
					item.put(j);
				}
				rsItem.close(); psItem.close();
				hasil.put("item", item);
			} else {
				// Baris pembelian LEGACY/berdiri sendiri (pembelian_anggota_koperasi NULL, tak pernah
				// dikelompokkan ke header) -- pola id sama dgn prosesDashboardUmum:
				// COALESCE(a.pembelian_anggota_koperasi, a.id), jadi idTransaksi di sini bisa jadi
				// langsung id baris koperasi.pembelian itu sendiri. Susun header SINTETIS dari SATU
				// baris itu (tak ada rincian tunai/non-tunai/kembalian utk kasus ini -- data itu memang
				// tak pernah tercatat tanpa header kelompok).
				java.sql.PreparedStatement psSatuan = conn.prepareStatement(
						"SELECT p.kode, p.waktu, COALESCE(p.total,0), COALESCE(p.diskon,0), COALESCE(p.cashback,0), "
								+ "COALESCE(pr.nama,''), p.qty, COALESCE(p.hargasatuan, (p.total / NULLIF(p.qty,0)), 0), COALESCE(t.nama,''), "
								+ "COALESCE(NULLIF(t.pesan_terima_kasih,''), ?), p.produk "
								+ "FROM koperasi.pembelian p LEFT JOIN koperasi.produk pr ON p.produk = pr.id "
								+ "LEFT JOIN koperasi.toko t ON p.toko = t.id "
								+ "WHERE p.id = ? AND p.toko = ? AND p.pembelian_anggota_koperasi IS NULL");
				psSatuan.setString(1, Toko.PESAN_TERIMA_KASIH_DEFAULT);
				psSatuan.setLong(2, idTransaksi);
				psSatuan.setLong(3, tokoId.longValue());
				java.sql.ResultSet rsSatuan = psSatuan.executeQuery();
				if (!rsSatuan.next()) {
					rsSatuan.close(); psSatuan.close();
					hasil.put("status", "error");
					hasil.put("message", "Transaksi tidak ditemukan.");
					return;
				}
				hasil.put("kode", str(rsSatuan.getString(1)));
				hasil.put("waktu", rsSatuan.getTimestamp(2) == null ? "" : fmt.format(rsSatuan.getTimestamp(2)));
				hasil.put("bayarTunai", 0);
				hasil.put("bayarNonTunai", 0);
				hasil.put("kembalian", 0);
				hasil.put("totalBiaya", rsSatuan.getDouble(3));
				hasil.put("totalDiskon", rsSatuan.getDouble(4));
				hasil.put("totalCashback", rsSatuan.getDouble(5));
				hasil.put("tokoNama", str(rsSatuan.getString(9)));
				hasil.put("pesanTerimaKasih", str(rsSatuan.getString(10)));

				JSONArray item = new JSONArray();
				JSONObject j = new JSONObject();
				j.put("nama", str(rsSatuan.getString(6)));
				j.put("qty", rsSatuan.getDouble(7));
				j.put("harga", rsSatuan.getDouble(8));
				j.put("diskon", rsSatuan.getDouble(4));
				j.put("cashback", rsSatuan.getDouble(5));
				long produkIdSatuan = rsSatuan.getLong(11);
				j.put("produkId", rsSatuan.wasNull() ? JSONObject.NULL : produkIdSatuan);
				item.put(j);
				rsSatuan.close(); psSatuan.close();
				hasil.put("item", item);
			}

			hasil.put("bolehEditTransaksi", KantinHelper.bolehEditTransaksi(tbmuser) && punyaHeaderKelompok);
			hasil.put("status", "success");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * <h3>Report Order / Report Sesi / Report Payment</h3> -- 3 laporan yang diminta klien lewat
	 * dokumen spesifikasi "Flow Kasir" (nomor ID order format {@code toko3/0001}, nomor nota format
	 * {@code Order toko3 - 0001 - 001}, dst). Ketiganya BUKAN tabel baru -- semua data direkonstruksi
	 * dari {@code koperasi.pembelian} (baris item, sudah punya kolom {@code member}/{@code oleh}/
	 * {@code carabayar} ter-denormalisasi, dipakai persis sama oleh {@link #prosesDashboardUmum}) +
	 * {@code koperasi.pembelian_anggota_koperasi} (header, utk total_diskon/pajak/total_biaya/
	 * bayar_tunai/bayar_non_tunai) + {@code koperasi.sesi_kas_kasir} (utk nomor sesi).
	 *
	 * <p><b>Kenapa "sesi" harus DIREKONSTRUKSI, bukan dibaca dari FK</b>: tidak ada kolom
	 * {@code sesi_kas_kasir} di {@code pembelian_anggota_koperasi} (dikonfirmasi lewat pembacaan
	 * entity) -- keduanya dicocokkan HANYA lewat identitas kasir ({@code sesi_kas_kasir.kasir_nama}/
	 * {@code kasir_user_id}, BUKAN {@code oleh}/{@code olehid} -- itu audit generik, lihat javadoc
	 * {@code SesiKasKasir.getKasirNama()}) + rentang waktu {@code waktubuka..waktututup}, PERSIS pola
	 * yg sudah dipakai {@code SesiKasUtil.hitungPenjualan()} utk menghitung total tunai/non-tunai per
	 * sesi saat tutup
	 * kas. Query di bawah mereproduksi pencocokan yg SAMA lewat {@code LEFT JOIN LATERAL}, supaya
	 * "sesi ke berapa" bisa dihitung per-baris tanpa mengubah skema database sama sekali.</p>
	 */
	private void prosesLaporanOrderList(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		if (tokoId == null) { hasil.put("status", "error"); hasil.put("message", "Toko tidak diketahui utk akun ini."); return; }

		JSONObject payloadAman = new JSONObject(payload.toString());
		if (!bolehSupervisorAtauAdmin(tbmuser)) {
			String namaKasirLogin = tbmuser == null ? "" : str(tbmuser.getUserNama()).trim();
			if (namaKasirLogin.length() == 0 && tbmuser != null && tbmuser.getPedagang() != null)
				namaKasirLogin = str(tbmuser.getPedagang().getNama()).trim();
			if (namaKasirLogin.length() == 0 && tbmuser != null)
				namaKasirLogin = str(tbmuser.getUserId()).trim();
			payloadAman.put("kasirExact", namaKasirLogin.length() == 0
					? "__AKUN_KASIR_TIDAK_DIKENAL__" : namaKasirLogin);
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			String tokoKode = toko == null || toko.getKode() == null || toko.getKode().trim().isEmpty()
					? ("toko" + tokoId) : toko.getKode().trim();

			JSONObject hasilQuery = daftarOrderDenganSesi(session, tokoId, tokoKode, payloadAman);
			hasil.put("status", "success");
			hasil.put("data", hasilQuery.getJSONArray("data"));
			hasil.put("total", hasilQuery.getLong("total"));
			hasil.put("totalNilai", hasilQuery.optDouble("totalNilai", 0));
			hasil.put("totalQty", hasilQuery.optDouble("totalQty", 0));
			hasil.put("page", hasilQuery.getInt("page"));
			hasil.put("pageSize", hasilQuery.getInt("pageSize"));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Analitik keputusan utk Riwayat Penjualan. Seluruh agregasi dihitung di
	 * database atas periode lengkap (bukan dari 15 baris halaman klien), dengan
	 * pembanding periode sebelumnya yang panjang harinya sama. Rentang dibatasi
	 * maksimum 366 hari agar grafik tetap responsif dan aman utk database toko.
	 */
	private static JSONObject hitungAnalitikLabaKotor(java.sql.Connection conn,
			java.util.Map<Long, Double> petaHpp, Long tokoId, String mulai, String sampai,
			String batasKasir, boolean bolehSemuaKasir, String namaKasirLogin, boolean sertakanRincian)
			throws Exception {
		String sql = "SELECT COALESCE(a.pembelian_anggota_koperasi,a.id),DATE(a.waktu),MAX(a.waktu),"
				+ "a.produk,COALESCE(NULLIF(TRIM(MAX(a.nama)),''),NULLIF(TRIM(MAX(p.nama)),''),'Produk tanpa nama'),"
				+ "COALESCE(SUM(a.qty),0),COALESCE(SUM(a.total),0) FROM koperasi.pembelian a "
				+ "LEFT JOIN koperasi.produk p ON p.id=a.produk LEFT JOIN koperasi.pembelian_anggota_koperasi pak "
				+ "ON pak.id=a.pembelian_anggota_koperasi WHERE a.toko=? AND COALESCE(a.aktif,true)=true "
				+ "AND DATE(a.waktu)>=?::date AND DATE(a.waktu)<=?::date" + batasKasir
				+ " GROUP BY 1,2,a.produk ORDER BY 2,3,1";
		java.sql.PreparedStatement ps = conn.prepareStatement(sql);
		ps.setLong(1, tokoId.longValue()); ps.setString(2, mulai); ps.setString(3, sampai);
		if (!bolehSemuaKasir) ps.setString(4, namaKasirLogin);
		java.sql.ResultSet rs = ps.executeQuery();
		java.util.Map<String, double[]> perHari = new java.util.TreeMap<String, double[]>();
		java.util.Map<String, Object[]> perProduk = new java.util.LinkedHashMap<String, Object[]>();
		java.util.Map<String, Object[]> perTransaksi = new java.util.LinkedHashMap<String, Object[]>();
		double omzet = 0, hpp = 0, qtyTanpaHpp = 0;
		java.util.Set<String> produkTanpaHpp = new java.util.HashSet<String>();
		while (rs.next()) {
			String idTransaksi = rs.getString(1), tanggal = rs.getDate(2).toString();
			java.sql.Timestamp waktu = rs.getTimestamp(3);
			long produkId = rs.getLong(4); boolean produkNull = rs.wasNull();
			String namaProduk = rs.getString(5);
			double qty = rs.getDouble(6), penjualan = rs.getDouble(7);
			Double hppUnit = produkNull ? null : petaHpp.get(Long.valueOf(produkId));
			double modal = hppUnit == null ? 0 : qty * hppUnit.doubleValue();
			if (hppUnit == null || hppUnit.doubleValue() <= 0) {
				qtyTanpaHpp += qty; produkTanpaHpp.add(produkNull ? namaProduk : String.valueOf(produkId));
			}
			omzet += penjualan; hpp += modal;
			double[] hari = perHari.get(tanggal);
			if (hari == null) { hari = new double[] { 0, 0 }; perHari.put(tanggal, hari); }
			hari[0] += penjualan; hari[1] += modal;
			String kunciProduk = produkNull ? ("N:" + namaProduk) : ("I:" + produkId);
			Object[] produk = perProduk.get(kunciProduk);
			if (produk == null) {
				produk = new Object[] { namaProduk, Double.valueOf(0), Double.valueOf(0), Double.valueOf(0) };
				perProduk.put(kunciProduk, produk);
			}
			produk[1] = Double.valueOf(((Double) produk[1]).doubleValue() + qty);
			produk[2] = Double.valueOf(((Double) produk[2]).doubleValue() + penjualan);
			produk[3] = Double.valueOf(((Double) produk[3]).doubleValue() + modal);
			Object[] transaksi = perTransaksi.get(idTransaksi);
			if (transaksi == null) {
				transaksi = new Object[] { tanggal, Long.valueOf(waktu == null ? 0 : waktu.getTime()),
						Double.valueOf(0), Double.valueOf(0) };
				perTransaksi.put(idTransaksi, transaksi);
			}
			transaksi[2] = Double.valueOf(((Double) transaksi[2]).doubleValue() + penjualan);
			transaksi[3] = Double.valueOf(((Double) transaksi[3]).doubleValue() + modal);
		}
		rs.close(); ps.close();
		double laba = omzet - hpp;
		JSONObject ringkasan = new JSONObject();
		ringkasan.put("omzet", omzet); ringkasan.put("hpp", hpp); ringkasan.put("labaKotor", laba);
		ringkasan.put("marginPersen", omzet == 0 ? 0 : laba / omzet * 100.0);
		ringkasan.put("qtyTanpaHpp", qtyTanpaHpp); ringkasan.put("produkTanpaHpp", produkTanpaHpp.size());
		java.util.List<JSONObject> daftarProduk = new java.util.ArrayList<JSONObject>();
		int produkMarginNegatif = 0;
		for (Object[] p : perProduk.values()) {
			double penjualan = ((Double) p[2]).doubleValue(), modal = ((Double) p[3]).doubleValue();
			double labaProduk = penjualan - modal; if (labaProduk < 0) produkMarginNegatif++;
			JSONObject o = new JSONObject(); o.put("nama", p[0]); o.put("qty", p[1]); o.put("omzet", penjualan);
			o.put("hpp", modal); o.put("labaKotor", labaProduk);
			o.put("marginPersen", penjualan == 0 ? 0 : labaProduk / penjualan * 100.0); daftarProduk.add(o);
		}
		ringkasan.put("produkMarginNegatif", produkMarginNegatif);
		JSONObject hasilLaba = new JSONObject(); hasilLaba.put("ringkasan", ringkasan);
		if (!sertakanRincian) return hasilLaba;
		java.util.Collections.sort(daftarProduk, new java.util.Comparator<JSONObject>() {
			public int compare(JSONObject a, JSONObject b) {
				return Double.compare(b.optDouble("labaKotor", 0), a.optDouble("labaKotor", 0));
			}
		});
		JSONArray produkJson = new JSONArray();
		for (int i = 0; i < Math.min(15, daftarProduk.size()); i++) produkJson.put(daftarProduk.get(i));
		hasilLaba.put("produk", produkJson);
		JSONArray tren = new JSONArray();
		for (java.util.Map.Entry<String, double[]> e : perHari.entrySet()) {
			double penjualan = e.getValue()[0], modal = e.getValue()[1], labaHari = penjualan - modal;
			JSONObject o = new JSONObject(); o.put("tanggal", e.getKey()); o.put("omzet", penjualan);
			o.put("hpp", modal); o.put("labaKotor", labaHari);
			o.put("marginPersen", penjualan == 0 ? 0 : labaHari / penjualan * 100.0); tren.put(o);
		}
		hasilLaba.put("tren", tren);
		java.util.Map<String, java.util.List<Double>> nilaiHari =
				new java.util.TreeMap<String, java.util.List<Double>>();
		for (Object[] transaksi : perTransaksi.values()) {
			String tanggal = (String) transaksi[0]; java.util.List<Double> nilai = nilaiHari.get(tanggal);
			if (nilai == null) { nilai = new java.util.ArrayList<Double>(); nilaiHari.put(tanggal, nilai); }
			nilai.add(Double.valueOf(((Double) transaksi[2]).doubleValue() - ((Double) transaksi[3]).doubleValue()));
		}
		JSONArray candle = new JSONArray();
		for (java.util.Map.Entry<String, java.util.List<Double>> e : nilaiHari.entrySet()) {
			java.util.List<Double> nilai = e.getValue(); if (nilai.isEmpty()) continue;
			double tinggi = -Double.MAX_VALUE, rendah = Double.MAX_VALUE;
			for (Double n : nilai) { tinggi = Math.max(tinggi, n.doubleValue()); rendah = Math.min(rendah, n.doubleValue()); }
			JSONObject o = new JSONObject(); o.put("tanggal", e.getKey()); o.put("open", nilai.get(0));
			o.put("high", tinggi); o.put("low", rendah); o.put("close", nilai.get(nilai.size() - 1));
			o.put("transaksi", nilai.size()); candle.put(o);
		}
		hasilLaba.put("candle", candle); return hasilLaba;
	}

	/** Menjalankan pratinjau/posting HPP atau penjualan tanpa berpindah ke halaman ZK. */
	private void prosesLaporanKeuanganPendukung(JSONObject payload, JSONObject hasil) throws Exception {
		String jenis = payload.optString("jenis", "").trim().toLowerCase();
		boolean posting = payload.optBoolean("posting", false);
		java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
		fmt.setLenient(false);
		java.util.Date sampai = fmt.parse(payload.optString("sampai", fmt.format(new java.util.Date())));
		java.util.Calendar awal = java.util.Calendar.getInstance();
		awal.setTime(sampai);
		awal.set(java.util.Calendar.DAY_OF_MONTH, 1);
		java.util.Date mulai = fmt.parse(payload.optString("mulai", fmt.format(awal.getTime())));
		JSONObject data;
		if ("hpp".equals(jenis)) {
			data = new ais.action.master.koperasi.PostingHppKantinAction().prosesApi(mulai, sampai, posting);
		} else if ("penjualan".equals(jenis)) {
			data = new ais.action.master.koperasi.PostingPenjualanKantinAction().prosesApi(mulai, sampai, posting);
		} else {
			throw new IllegalArgumentException("Jenis pendukung laporan keuangan tidak dikenal: " + jenis);
		}
		hasil.put("status", "success");
		hasil.put("data", data);
	}

	/**
	 * Sumber replikasi cadangan POS antarkasir. Berbeda dari laporan operasional,
	 * endpoint ini sengaja tidak membatasi nama kasir karena seluruh perangkat pada
	 * toko yang sama harus mempunyai salinan pemulihan yang identik. Batas toko tetap
	 * dihitung oleh {@link #resolveTokoId(Tbmuser, JSONObject)} sehingga kasir tidak
	 * dapat meminta transaksi toko lain dengan memalsukan payload.
	 */
	private void prosesTransaksiBackupTokoList(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		if (tokoId == null) {
			hasil.put("status", "error");
			hasil.put("message", "Toko tidak diketahui utk akun ini.");
			return;
		}
		JSONObject payloadAman = new JSONObject(payload.toString());
		payloadAman.remove("kasirExact");
		payloadAman.remove("kasir");
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			String tokoKode = toko == null || toko.getKode() == null || toko.getKode().trim().isEmpty()
					? ("toko" + tokoId) : toko.getKode().trim();
			JSONObject hasilQuery = daftarOrderDenganSesi(session, tokoId, tokoKode, payloadAman);
			hasil.put("status", "success");
			hasil.put("data", hasilQuery.getJSONArray("data"));
			hasil.put("total", hasilQuery.getLong("total"));
			hasil.put("totalNilai", hasilQuery.optDouble("totalNilai", 0));
			hasil.put("totalQty", hasilQuery.optDouble("totalQty", 0));
			hasil.put("page", hasilQuery.getInt("page"));
			hasil.put("pageSize", hasilQuery.getInt("pageSize"));
			hasil.put("tokoId", tokoId);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Catat perangkat yang benar-benar sudah menyimpan salinan transaksi. */
	private void prosesTransaksiBackupAck(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		JSONArray kodeArray = payload.optJSONArray("kode_transaksi");
		String idPerangkat = payload.optString("id_perangkat", "").trim();
		String namaMesin = payload.optString("nama_mesin", "").trim();
		if (tokoId == null || kodeArray == null || kodeArray.length() == 0 || idPerangkat.length() == 0) {
			hasil.put("status", "error");
			hasil.put("message", "Toko, kode transaksi, dan identitas perangkat wajib diisi.");
			return;
		}
		if (idPerangkat.length() > 255) idPerangkat = idPerangkat.substring(0, 255);
		if (namaMesin.length() > 255) namaMesin = namaMesin.substring(0, 255);
		String userId = tbmuser == null ? "" : str(tbmuser.getUserId()).trim();
		String userNama = tbmuser == null ? "" : str(tbmuser.getUserNama()).trim();
		org.hibernate.Session session = null;
		org.hibernate.Transaction tx = null;
		java.sql.PreparedStatement ps = null;
		int tersimpan = 0;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			ps = session.connection().prepareStatement("INSERT INTO koperasi.transaksi_backup_ack "
					+ "(toko,kode_transaksi,id_perangkat,nama_mesin,kasir_user_id,kasir_nama,waktu) "
					+ "VALUES (?,?,?,?,?,?,now()) ON CONFLICT (toko,lower(kode_transaksi),id_perangkat) "
					+ "DO UPDATE SET nama_mesin=EXCLUDED.nama_mesin,kasir_user_id=EXCLUDED.kasir_user_id,"
					+ "kasir_nama=EXCLUDED.kasir_nama,waktu=now()");
			int batas = Math.min(kodeArray.length(), 500);
			for (int i = 0; i < batas; i++) {
				String kode = kodeArray.optString(i, "").trim();
				if (kode.length() == 0) continue;
				if (kode.length() > 160) kode = kode.substring(0, 160);
				ps.setLong(1, tokoId.longValue());
				ps.setString(2, kode);
				ps.setString(3, idPerangkat);
				ps.setString(4, namaMesin);
				ps.setString(5, userId);
				ps.setString(6, userNama);
				ps.addBatch();
				tersimpan++;
			}
			if (tersimpan > 0) ps.executeBatch();
			tx.commit();
			hasil.put("status", "success");
			hasil.put("jumlah", tersimpan);
		} catch (Exception e) {
			if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception rollback) {
				ais.common.ErrorAuditUtil.record(rollback, "prosesTransaksiBackupAck-rollback");
			}
			throw e;
		} finally {
			try { if (ps != null) ps.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "prosesTransaksiBackupAck-ps-close"); }
			if (session != null) {
				try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "prosesTransaksiBackupAck-clear"); }
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "prosesTransaksiBackupAck-disconnect"); }
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "prosesTransaksiBackupAck-close"); }
			}
		}
	}

	/** Ringkasan perangkat/kasir lain yang telah mengakui salinan per transaksi. */
	private void prosesTransaksiBackupStatus(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		JSONArray kodeArray = payload.optJSONArray("kode_transaksi");
		String perangkatIni = payload.optString("id_perangkat", "").trim();
		JSONArray data = new JSONArray();
		if (tokoId == null || kodeArray == null || kodeArray.length() == 0) {
			hasil.put("status", "success"); hasil.put("data", data); return;
		}
		java.util.List<String> kode = new java.util.ArrayList<String>();
		int batas = Math.min(kodeArray.length(), 100);
		for (int i = 0; i < batas; i++) {
			String nilai = kodeArray.optString(i, "").trim();
			if (nilai.length() > 0) kode.add(nilai.toLowerCase());
		}
		if (kode.isEmpty()) { hasil.put("status", "success"); hasil.put("data", data); return; }
		StringBuilder sql = new StringBuilder("SELECT lower(kode_transaksi) kode,"
				+ "COUNT(DISTINCT NULLIF(kasir_user_id,'')) jumlah_kasir,COUNT(DISTINCT id_perangkat) jumlah_mesin,"
				+ "string_agg(DISTINCT (COALESCE(NULLIF(kasir_nama,''),NULLIF(kasir_user_id,''),'-')"
				+ " || ' [' || COALESCE(NULLIF(nama_mesin,''),id_perangkat) || ']'),', ') penerima "
				+ "FROM koperasi.transaksi_backup_ack WHERE toko=? AND lower(kode_transaksi) IN (");
		for (int i = 0; i < kode.size(); i++) { if (i > 0) sql.append(','); sql.append('?'); }
		sql.append(")");
		if (perangkatIni.length() > 0) sql.append(" AND id_perangkat<>?");
		sql.append(" GROUP BY lower(kode_transaksi)");
		org.hibernate.Session session = null;
		java.sql.PreparedStatement ps = null;
		java.sql.ResultSet rs = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			ps = session.connection().prepareStatement(sql.toString());
			int no = 1; ps.setLong(no++, tokoId.longValue());
			for (int i = 0; i < kode.size(); i++) ps.setString(no++, kode.get(i));
			if (perangkatIni.length() > 0) ps.setString(no++, perangkatIni);
			rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject row = new JSONObject();
				row.put("kodeTransaksi", rs.getString("kode"));
				row.put("jumlahKasir", rs.getInt("jumlah_kasir"));
				row.put("jumlahMesin", rs.getInt("jumlah_mesin"));
				row.put("penerima", rs.getString("penerima"));
				data.put(row);
			}
			hasil.put("status", "success"); hasil.put("data", data);
		} finally {
			try { if (rs != null) rs.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "prosesTransaksiBackupStatus-rs-close"); }
			try { if (ps != null) ps.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "prosesTransaksiBackupStatus-ps-close"); }
			if (session != null) {
				try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "prosesTransaksiBackupStatus-clear"); }
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "prosesTransaksiBackupStatus-disconnect"); }
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "prosesTransaksiBackupStatus-close"); }
			}
		}
	}

	private void prosesLaporanRiwayatPenjualanAnalitik(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		if (tokoId == null) { hasil.put("status", "error"); hasil.put("message", "Toko tidak diketahui utk akun ini."); return; }
		boolean bolehSemuaKasir = bolehSupervisorAtauAdmin(tbmuser);
		String namaKasirLogin = tbmuser == null ? "" : str(tbmuser.getUserNama()).trim();
		if (namaKasirLogin.length() == 0 && tbmuser != null && tbmuser.getPedagang() != null)
			namaKasirLogin = str(tbmuser.getPedagang().getNama()).trim();
		if (namaKasirLogin.length() == 0 && tbmuser != null) namaKasirLogin = str(tbmuser.getUserId()).trim();
		// Fail closed: kasir biasa tidak pernah memperoleh agregat kasir lain bila
		// identitas login lama ternyata kosong/tidak konsisten.
		if (!bolehSemuaKasir && namaKasirLogin.length() == 0) namaKasirLogin = "__AKUN_KASIR_TIDAK_DIKENAL__";
		String batasKasir = bolehSemuaKasir ? "" :
				" AND LOWER(NULLIF(TRIM(pak.kasir_login_nama),''))=LOWER(?)";
		java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
		fmt.setLenient(false);
		java.util.Date akhir;
		try { akhir = fmt.parse(payload.optString("tglSampai", fmt.format(new java.util.Date()))); }
		catch (Exception e) { akhir = new java.util.Date(); }
		java.util.Calendar cal = java.util.Calendar.getInstance();
		cal.setTime(akhir); cal.add(java.util.Calendar.DATE, -29);
		java.util.Date awalDefault = cal.getTime();
		java.util.Date awal;
		try { awal = fmt.parse(payload.optString("tglMulai", fmt.format(awalDefault))); }
		catch (Exception e) { awal = awalDefault; }
		if (awal.after(akhir)) { java.util.Date tmp = awal; awal = akhir; akhir = tmp; }
		long jumlahHari = ((akhir.getTime() - awal.getTime()) / 86400000L) + 1L;
		if (jumlahHari > 366L) { jumlahHari = 366L; cal.setTime(akhir); cal.add(java.util.Calendar.DATE, -365); awal = cal.getTime(); }
		cal.setTime(awal); cal.add(java.util.Calendar.DATE, -1); java.util.Date akhirLalu = cal.getTime();
		cal.add(java.util.Calendar.DATE, -(int)jumlahHari + 1); java.util.Date awalLalu = cal.getTime();
		String mulai = fmt.format(awal), sampai = fmt.format(akhir);
		String mulaiLalu = fmt.format(awalLalu), sampaiLalu = fmt.format(akhirLalu);

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			java.util.Map<Long, Double> petaHppAnalitik = petaHargaPokok(conn, tokoId);
			String trx = "SELECT COALESCE(a.pembelian_anggota_koperasi,a.id) id_trx,DATE(MAX(a.waktu)) tanggal,"
					+ " EXTRACT(HOUR FROM MAX(a.waktu)) jam,COALESCE(MAX(pak.total_biaya),SUM(a.total)) total_nilai,"
					+ " SUM(COALESCE(a.qty,0)) qty,MAX(pak.total_biaya) total_master,COALESCE(SUM(a.total),0) total_detail,"
					+ " COALESCE(MAX(pak.total_diskon),SUM(COALESCE(a.diskon,0)),0) total_diskon,"
					+ " COALESCE(MAX(pak.totalcashback),SUM(COALESCE(a.cashback,0)),0) total_cashback,"
					+ " COALESCE(NULLIF(TRIM(MAX(a.carabayar)),''),'-') metode,"
					+ " COALESCE(NULLIF(TRIM(MAX(pak.kasir_login_nama)),''),'Kasir tidak tercatat') kasir,"
					+ " MAX(a.member) member FROM koperasi.pembelian a LEFT JOIN koperasi.pembelian_anggota_koperasi pak"
					+ " ON pak.id=a.pembelian_anggota_koperasi WHERE a.toko=? AND COALESCE(a.aktif,true)=true AND DATE(a.waktu)>=?::date AND DATE(a.waktu)<=?::date"
					+ batasKasir + " GROUP BY 1";

			java.sql.PreparedStatement pk = conn.prepareStatement("SELECT COUNT(*),COALESCE(SUM(total_nilai),0),COALESCE(AVG(total_nilai),0),"
					+ "COALESCE(SUM(qty),0),COALESCE(SUM(CASE WHEN total_master IS NOT NULL AND ABS(total_master-total_detail)>0.01 THEN 1 ELSE 0 END),0),"
					+ "COALESCE(SUM(CASE WHEN NULLIF(TRIM(member),'') IS NOT NULL THEN 1 ELSE 0 END),0),"
					+ "COALESCE(SUM(total_diskon),0),COALESCE(SUM(total_cashback),0),"
					+ "COALESCE(SUM(CASE WHEN total_master IS NOT NULL AND ABS(total_master-total_detail)>0.01 THEN ABS(total_master-total_detail) ELSE 0 END),0)"
					+ " FROM (" + trx + ") x");
			pk.setLong(1, tokoId.longValue()); pk.setString(2, mulai); pk.setString(3, sampai);
			if (!bolehSemuaKasir) pk.setString(4, namaKasirLogin);
			java.sql.ResultSet rk = pk.executeQuery(); JSONObject kpi = new JSONObject();
			if (rk.next()) { kpi.put("transaksi", rk.getLong(1)); kpi.put("omzet", rk.getDouble(2)); kpi.put("rataRata", rk.getDouble(3)); kpi.put("qty", rk.getDouble(4)); kpi.put("tidakValid", rk.getLong(5)); kpi.put("transaksiMember", rk.getLong(6)); kpi.put("diskon", rk.getDouble(7)); kpi.put("cashback", rk.getDouble(8)); kpi.put("nilaiTidakValid", rk.getDouble(9)); }
			rk.close(); pk.close();

			java.sql.PreparedStatement pp = conn.prepareStatement("SELECT COUNT(*),COALESCE(SUM(total_nilai),0),COALESCE(AVG(total_nilai),0),COALESCE(SUM(qty),0) FROM (" + trx + ") x");
			pp.setLong(1, tokoId.longValue()); pp.setString(2, mulaiLalu); pp.setString(3, sampaiLalu);
			if (!bolehSemuaKasir) pp.setString(4, namaKasirLogin);
			java.sql.ResultSet rp = pp.executeQuery(); JSONObject pembanding = new JSONObject();
			if (rp.next()) { pembanding.put("transaksi", rp.getLong(1)); pembanding.put("omzet", rp.getDouble(2)); pembanding.put("rataRata", rp.getDouble(3)); pembanding.put("qty", rp.getDouble(4)); }
			rp.close(); pp.close();

			java.sql.PreparedStatement pt = conn.prepareStatement("SELECT tanggal,COUNT(*),COALESCE(SUM(total_nilai),0),COALESCE(AVG(total_nilai),0) FROM (" + trx + ") x GROUP BY tanggal ORDER BY tanggal");
			pt.setLong(1, tokoId.longValue()); pt.setString(2, mulai); pt.setString(3, sampai);
			if (!bolehSemuaKasir) pt.setString(4, namaKasirLogin);
			java.sql.ResultSet rt = pt.executeQuery(); JSONArray tren = new JSONArray();
			while (rt.next()) { JSONObject o = new JSONObject(); o.put("tanggal", rt.getDate(1).toString()); o.put("transaksi", rt.getLong(2)); o.put("omzet", rt.getDouble(3)); o.put("rataRata", rt.getDouble(4)); tren.put(o); }
			rt.close(); pt.close();

			java.sql.PreparedStatement pm = conn.prepareStatement("SELECT metode,COUNT(*),COALESCE(SUM(total_nilai),0) FROM (" + trx + ") x GROUP BY metode ORDER BY 3 DESC LIMIT 10");
			pm.setLong(1, tokoId.longValue()); pm.setString(2, mulai); pm.setString(3, sampai);
			if (!bolehSemuaKasir) pm.setString(4, namaKasirLogin);
			java.sql.ResultSet rm = pm.executeQuery(); JSONArray metode = new JSONArray();
			while (rm.next()) { JSONObject o = new JSONObject(); o.put("nama", rm.getString(1)); o.put("transaksi", rm.getLong(2)); o.put("omzet", rm.getDouble(3)); metode.put(o); }
			rm.close(); pm.close();

			java.sql.PreparedStatement pj = conn.prepareStatement("SELECT CAST(jam AS integer),COUNT(*),COALESCE(SUM(total_nilai),0) FROM (" + trx + ") x GROUP BY jam ORDER BY jam");
			pj.setLong(1, tokoId.longValue()); pj.setString(2, mulai); pj.setString(3, sampai);
			if (!bolehSemuaKasir) pj.setString(4, namaKasirLogin);
			java.sql.ResultSet rj = pj.executeQuery(); JSONArray jam = new JSONArray();
			while (rj.next()) { JSONObject o = new JSONObject(); o.put("jam", rj.getInt(1)); o.put("transaksi", rj.getLong(2)); o.put("omzet", rj.getDouble(3)); jam.put(o); }
			rj.close(); pj.close();

			java.sql.PreparedStatement pHari = conn.prepareStatement("SELECT EXTRACT(ISODOW FROM tanggal)::integer,COUNT(*),COALESCE(SUM(total_nilai),0),COALESCE(AVG(total_nilai),0) FROM (" + trx + ") x GROUP BY 1 ORDER BY 1");
			pHari.setLong(1, tokoId.longValue()); pHari.setString(2, mulai); pHari.setString(3, sampai);
			if (!bolehSemuaKasir) pHari.setString(4, namaKasirLogin);
			java.sql.ResultSet rHari = pHari.executeQuery(); JSONArray hari = new JSONArray();
			String[] namaHari = { "", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu" };
			while (rHari.next()) { JSONObject o = new JSONObject(); int nomorHari = rHari.getInt(1); o.put("nomor", nomorHari); o.put("nama", nomorHari >= 1 && nomorHari <= 7 ? namaHari[nomorHari] : "-"); o.put("transaksi", rHari.getLong(2)); o.put("omzet", rHari.getDouble(3)); o.put("rataRata", rHari.getDouble(4)); hari.put(o); }
			rHari.close(); pHari.close();

			java.sql.PreparedStatement pKeranjang = conn.prepareStatement("SELECT CASE WHEN total_nilai<25000 THEN '< Rp25 ribu' WHEN total_nilai<50000 THEN 'Rp25–50 ribu' WHEN total_nilai<100000 THEN 'Rp50–100 ribu' WHEN total_nilai<250000 THEN 'Rp100–250 ribu' ELSE '>= Rp250 ribu' END rentang,COUNT(*),COALESCE(SUM(total_nilai),0),MIN(total_nilai),MAX(total_nilai) FROM (" + trx + ") x GROUP BY 1 ORDER BY MIN(total_nilai)");
			pKeranjang.setLong(1, tokoId.longValue()); pKeranjang.setString(2, mulai); pKeranjang.setString(3, sampai);
			if (!bolehSemuaKasir) pKeranjang.setString(4, namaKasirLogin);
			java.sql.ResultSet rKeranjang = pKeranjang.executeQuery(); JSONArray keranjang = new JSONArray();
			while (rKeranjang.next()) { JSONObject o = new JSONObject(); o.put("rentang", rKeranjang.getString(1)); o.put("transaksi", rKeranjang.getLong(2)); o.put("omzet", rKeranjang.getDouble(3)); o.put("minimum", rKeranjang.getDouble(4)); o.put("maksimum", rKeranjang.getDouble(5)); keranjang.put(o); }
			rKeranjang.close(); pKeranjang.close();

			java.sql.PreparedStatement pKasir = conn.prepareStatement("SELECT kasir,COUNT(*),COALESCE(SUM(total_nilai),0),COALESCE(AVG(total_nilai),0) FROM (" + trx + ") x GROUP BY kasir ORDER BY 3 DESC LIMIT 10");
			pKasir.setLong(1, tokoId.longValue()); pKasir.setString(2, mulai); pKasir.setString(3, sampai);
			if (!bolehSemuaKasir) pKasir.setString(4, namaKasirLogin);
			java.sql.ResultSet rKasir = pKasir.executeQuery(); JSONArray kasir = new JSONArray();
			while (rKasir.next()) { JSONObject o = new JSONObject(); o.put("nama", rKasir.getString(1)); o.put("transaksi", rKasir.getLong(2)); o.put("omzet", rKasir.getDouble(3)); o.put("rataRata", rKasir.getDouble(4)); kasir.put(o); }
			rKasir.close(); pKasir.close();

			String sqlProduk = "SELECT COALESCE(NULLIF(TRIM(MAX(a.nama)),''),NULLIF(TRIM(MAX(p.nama)),''),'Produk tanpa nama') nama,"
					+ "COALESCE(SUM(a.qty),0),COALESCE(SUM(a.total),0),COUNT(DISTINCT COALESCE(a.pembelian_anggota_koperasi,a.id))"
					+ " FROM koperasi.pembelian a LEFT JOIN koperasi.produk p ON p.id=a.produk"
					+ " LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON pak.id=a.pembelian_anggota_koperasi"
					+ " WHERE a.toko=? AND COALESCE(a.aktif,true)=true AND DATE(a.waktu)>=?::date AND DATE(a.waktu)<=?::date" + batasKasir
					+ " GROUP BY a.produk ORDER BY 3 DESC LIMIT 15";
			java.sql.PreparedStatement pProduk = conn.prepareStatement(sqlProduk);
			pProduk.setLong(1, tokoId.longValue()); pProduk.setString(2, mulai); pProduk.setString(3, sampai);
			if (!bolehSemuaKasir) pProduk.setString(4, namaKasirLogin);
			java.sql.ResultSet rProduk = pProduk.executeQuery(); JSONArray produk = new JSONArray();
			while (rProduk.next()) { JSONObject o = new JSONObject(); o.put("nama", rProduk.getString(1)); o.put("qty", rProduk.getDouble(2)); o.put("omzet", rProduk.getDouble(3)); o.put("transaksi", rProduk.getLong(4)); produk.put(o); }
			rProduk.close(); pProduk.close();

			String sqlRetur = "SELECT COUNT(DISTINCT rp.pembelian_anggota_koperasi_id),COALESCE(SUM(rp.qty),0),COALESCE(SUM(rp.totalnilai),0)"
					+ " FROM koperasi.retur_penjualan rp LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON pak.id=rp.pembelian_anggota_koperasi_id"
					+ " WHERE rp.toko=? AND DATE(rp.waktu)>=?::date AND DATE(rp.waktu)<=?::date";
			if (!bolehSemuaKasir) sqlRetur += " AND LOWER(NULLIF(TRIM(pak.kasir_login_nama),''))=LOWER(?)";
			java.sql.PreparedStatement pRetur = conn.prepareStatement(sqlRetur);
			pRetur.setLong(1, tokoId.longValue()); pRetur.setString(2, mulai); pRetur.setString(3, sampai);
			if (!bolehSemuaKasir) pRetur.setString(4, namaKasirLogin);
			java.sql.ResultSet rRetur = pRetur.executeQuery(); JSONObject retur = new JSONObject();
			if (rRetur.next()) { retur.put("transaksi", rRetur.getLong(1)); retur.put("qty", rRetur.getDouble(2)); retur.put("nilai", rRetur.getDouble(3)); }
			rRetur.close(); pRetur.close();
			JSONObject labaKotor = hitungAnalitikLabaKotor(conn, petaHppAnalitik, tokoId, mulai, sampai,
					batasKasir, bolehSemuaKasir, namaKasirLogin, true);
			JSONObject labaKotorLalu = hitungAnalitikLabaKotor(conn, petaHppAnalitik, tokoId, mulaiLalu, sampaiLalu,
					batasKasir, bolehSemuaKasir, namaKasirLogin, false);

			hasil.put("status", "success"); hasil.put("tglMulai", mulai); hasil.put("tglSampai", sampai);
			hasil.put("tglMulaiPembanding", mulaiLalu); hasil.put("tglSampaiPembanding", sampaiLalu);
			hasil.put("kpi", kpi); hasil.put("pembanding", pembanding); hasil.put("tren", tren);
			hasil.put("metode", metode); hasil.put("jam", jam); hasil.put("hari", hari); hasil.put("keranjang", keranjang);
			hasil.put("kasir", kasir); hasil.put("produk", produk); hasil.put("retur", retur);
			hasil.put("labaKotor", labaKotor);
			hasil.put("labaKotorPembanding", labaKotorLalu.getJSONObject("ringkasan"));
			hasil.put("cakupan", bolehSemuaKasir ? "TOKO" : "KASIR");
			hasil.put("kasirAktif", bolehSemuaKasir ? "" : namaKasirLogin);
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	/**
	 * Grid Penjualan per Kasir. Kasir biasa selalu dikunci server-side ke nama
	 * akun login; hanya supervisor/admin yang boleh memilih kasir lain. Daftar
	 * pilihan kasir berasal dari transaksi aktual pada toko dan periode terpilih,
	 * bukan dari master akun, sehingga tidak menampilkan akun tanpa penjualan.
	 */
	private void prosesLaporanPenjualanKasirList(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		if (tokoId == null) { hasil.put("status", "error"); hasil.put("message", "Toko tidak diketahui utk akun ini."); return; }
		boolean bolehSemua = bolehSupervisorAtauAdmin(tbmuser);
		String hariIni = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
		String tglMulai = payload.optString("tglMulai", "").trim();
		String tglSampai = payload.optString("tglSampai", "").trim();
		if (tglMulai.length() == 0) tglMulai = hariIni;
		if (tglSampai.length() == 0) tglSampai = hariIni;
		String namaLogin = tbmuser == null ? "" : str(tbmuser.getUserNama()).trim();
		if (namaLogin.length() == 0 && tbmuser != null && tbmuser.getPedagang() != null) namaLogin = str(tbmuser.getPedagang().getNama()).trim();
		if (namaLogin.length() == 0 && tbmuser != null) namaLogin = str(tbmuser.getUserId()).trim();
		String kasirDipilih = bolehSemua ? payload.optString("kasir", "").trim() : namaLogin;

		JSONObject aman = new JSONObject(payload.toString());
		aman.put("tglMulai", tglMulai);
		aman.put("tglSampai", tglSampai);
		if (kasirDipilih.length() > 0) aman.put("kasirExact", kasirDipilih);
		else aman.remove("kasirExact");

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			String tokoKode = toko == null || toko.getKode() == null || toko.getKode().trim().isEmpty()
					? ("toko" + tokoId) : toko.getKode().trim();
			JSONObject daftar = daftarOrderDenganSesi(session, tokoId, tokoKode, aman);
			java.sql.Connection conn = session.connection();
			String dasar = " FROM koperasi.pembelian a LEFT JOIN koperasi.pembelian_anggota_koperasi pak"
					+ " ON pak.id=a.pembelian_anggota_koperasi WHERE a.toko=? AND DATE(a.waktu)>=?::date AND DATE(a.waktu)<=?::date";
			String sub = "SELECT COALESCE(a.pembelian_anggota_koperasi,a.id) id_trx,"
					+ " COALESCE(NULLIF(TRIM(MAX(pak.kasir_login_nama)),''),'Kasir tidak tercatat') kasir,"
					+ " COALESCE(MAX(pak.total_biaya),SUM(a.total)) nilai" + dasar + " GROUP BY 1";
			String batasKasir = kasirDipilih.length() == 0 ? "" : " WHERE LOWER(TRIM(kasir))=LOWER(?)";
			java.sql.PreparedStatement ps = conn.prepareStatement(
					"SELECT kasir,COUNT(*),COALESCE(SUM(nilai),0) FROM (" + sub + ") trx" + batasKasir + " GROUP BY kasir ORDER BY kasir");
			int ix = 1;
			ps.setLong(ix++, tokoId.longValue()); ps.setString(ix++, tglMulai); ps.setString(ix++, tglSampai);
			if (kasirDipilih.length() > 0) ps.setString(ix++, kasirDipilih);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray ringkasan = new JSONArray();
			while (rs.next()) {
				JSONObject o = new JSONObject(); o.put("kasir", rs.getString(1)); o.put("jumlahTransaksi", rs.getLong(2)); o.put("total", rs.getDouble(3)); ringkasan.put(o);
			}
			rs.close(); ps.close();

			JSONArray pilihan = new JSONArray();
			if (bolehSemua) {
				java.sql.PreparedStatement psPilihan = conn.prepareStatement("SELECT kasir FROM (" + sub + ") trx GROUP BY kasir ORDER BY kasir");
				psPilihan.setLong(1, tokoId.longValue()); psPilihan.setString(2, tglMulai); psPilihan.setString(3, tglSampai);
				java.sql.ResultSet rp = psPilihan.executeQuery(); while (rp.next()) pilihan.put(rp.getString(1)); rp.close(); psPilihan.close();
			} else if (namaLogin.length() > 0) pilihan.put(namaLogin);

			hasil.put("status", "success");
			hasil.put("data", daftar.getJSONArray("data")); hasil.put("total", daftar.getLong("total"));
			hasil.put("page", daftar.getInt("page")); hasil.put("pageSize", daftar.getInt("pageSize"));
			hasil.put("tglMulai", tglMulai); hasil.put("tglSampai", tglSampai);
			hasil.put("bolehFilterKasir", bolehSemua); hasil.put("kasirAktif", kasirDipilih);
			hasil.put("daftarKasir", pilihan); hasil.put("ringkasanKasir", ringkasan);
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	/** Detail baris laporan dengan pemeriksaan kepemilikan kasir di server. */
	private void prosesLaporanPenjualanKasirDetail(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		if (!bolehSupervisorAtauAdmin(tbmuser)) {
			Long tokoId = resolveTokoId(tbmuser, payload);
			if (tokoId == null || payload.isNull("id")) { hasil.put("status", "error"); hasil.put("message", "Transaksi tidak ditemukan."); return; }
			String namaLogin = str(tbmuser.getUserNama()).trim();
			if (namaLogin.length() == 0 && tbmuser.getPedagang() != null) namaLogin = str(tbmuser.getPedagang().getNama()).trim();
			if (namaLogin.length() == 0) namaLogin = str(tbmuser.getUserId()).trim();
			Session cek = HibernateUtil.getSessionFactory().openSession();
			try {
				java.sql.PreparedStatement ps = cek.connection().prepareStatement(
						"SELECT COUNT(*) FROM koperasi.pembelian a LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON pak.id=a.pembelian_anggota_koperasi "
						+ "WHERE COALESCE(a.pembelian_anggota_koperasi,a.id)=? AND a.toko=? AND LOWER(NULLIF(TRIM(pak.kasir_login_nama),''))=LOWER(?)");
				ps.setLong(1, Long.parseLong(String.valueOf(payload.get("id")))); ps.setLong(2, tokoId.longValue()); ps.setString(3, namaLogin);
				java.sql.ResultSet rs = ps.executeQuery(); boolean milik = rs.next() && rs.getLong(1) > 0; rs.close(); ps.close();
				if (!milik) { hasil.put("status", "error"); hasil.put("message", "Kasir hanya boleh melihat rincian penjualan miliknya sendiri."); return; }
			} finally { HibernateUtil.closeSessionQuietly(cek); }
		}
		prosesDetailTransaksi(tbmuser, payload, hasil);
	}

	/** Ringkasan penerimaan dikelompokkan per tanggal, kasir, dan metode pembayaran. */
	private void prosesLaporanPenerimaanKasirList(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		if (tokoId == null) { hasil.put("status", "error"); hasil.put("message", "Toko tidak diketahui utk akun ini."); return; }
		boolean bolehSemua = bolehSupervisorAtauAdmin(tbmuser);
		String hariIni = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
		String mulai = payload.optString("tglMulai", "").trim(); if (mulai.length() == 0) mulai = hariIni;
		String sampai = payload.optString("tglSampai", "").trim(); if (sampai.length() == 0) sampai = hariIni;
		String namaLogin = tbmuser == null ? "" : str(tbmuser.getUserNama()).trim();
		if (namaLogin.length() == 0 && tbmuser != null && tbmuser.getPedagang() != null) namaLogin = str(tbmuser.getPedagang().getNama()).trim();
		if (namaLogin.length() == 0 && tbmuser != null) namaLogin = str(tbmuser.getUserId()).trim();
		String kasir = bolehSemua ? payload.optString("kasir", "").trim() : namaLogin;
		int page = Math.max(1, payload.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(5, payload.optInt("pageSize", 15)));
		int offset = (page - 1) * pageSize;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			String trx = "SELECT COALESCE(a.pembelian_anggota_koperasi,a.id) id_trx,DATE(MAX(a.waktu)) tanggal,"
					+ " COALESCE(NULLIF(TRIM(MAX(pak.kasir_login_nama)),''),'Kasir tidak tercatat') kasir,"
					+ " COALESCE(NULLIF(TRIM(MAX(a.carabayar)),''),'-') metode,"
					+ " COALESCE(MAX(pak.total_biaya),SUM(a.total)) nilai"
					+ " FROM koperasi.pembelian a LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON pak.id=a.pembelian_anggota_koperasi"
					+ " WHERE a.toko=? AND DATE(a.waktu)>=?::date AND DATE(a.waktu)<=?::date GROUP BY 1";
			String filter = kasir.length() == 0 ? "" : " WHERE LOWER(TRIM(kasir))=LOWER(?)";
			String group = " SELECT tanggal,kasir,metode,COUNT(*) jumlah,COALESCE(SUM(nilai),0) total FROM (" + trx + ") t" + filter
					+ " GROUP BY tanggal,kasir,metode";
			java.sql.PreparedStatement pc = conn.prepareStatement("SELECT COUNT(*) FROM (" + group + ") g");
			int ix = 1; pc.setLong(ix++, tokoId.longValue()); pc.setString(ix++, mulai); pc.setString(ix++, sampai); if (kasir.length() > 0) pc.setString(ix++, kasir);
			java.sql.ResultSet rc = pc.executeQuery(); long total = rc.next() ? rc.getLong(1) : 0; rc.close(); pc.close();
			java.sql.PreparedStatement pd = conn.prepareStatement(group + " ORDER BY tanggal DESC,kasir,metode LIMIT ? OFFSET ?");
			ix = 1; pd.setLong(ix++, tokoId.longValue()); pd.setString(ix++, mulai); pd.setString(ix++, sampai); if (kasir.length() > 0) pd.setString(ix++, kasir); pd.setInt(ix++, pageSize); pd.setInt(ix++, offset);
			java.sql.ResultSet rd = pd.executeQuery(); JSONArray data = new JSONArray();
			while (rd.next()) { JSONObject o = new JSONObject(); o.put("tanggal", rd.getDate(1).toString()); o.put("kasir", rd.getString(2)); o.put("metode", rd.getString(3)); o.put("jumlahTransaksi", rd.getLong(4)); o.put("total", rd.getDouble(5)); data.put(o); }
			rd.close(); pd.close();

			JSONArray pilihan = new JSONArray();
			if (bolehSemua) {
				java.sql.PreparedStatement pp = conn.prepareStatement("SELECT kasir FROM (" + trx + ") t GROUP BY kasir ORDER BY kasir");
				pp.setLong(1, tokoId.longValue()); pp.setString(2, mulai); pp.setString(3, sampai);
				java.sql.ResultSet rp = pp.executeQuery(); while (rp.next()) pilihan.put(rp.getString(1)); rp.close(); pp.close();
			} else if (namaLogin.length() > 0) pilihan.put(namaLogin);

			hasil.put("status", "success"); hasil.put("data", data); hasil.put("total", total); hasil.put("page", page); hasil.put("pageSize", pageSize);
			hasil.put("tglMulai", mulai); hasil.put("tglSampai", sampai); hasil.put("bolehFilterKasir", bolehSemua); hasil.put("kasirAktif", kasir); hasil.put("daftarKasir", pilihan);
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	/** Daftar transaksi penyusun satu baris ringkasan penerimaan. */
	private void prosesLaporanPenerimaanKasirDetail(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		if (tokoId == null) { hasil.put("status", "error"); hasil.put("message", "Toko tidak diketahui utk akun ini."); return; }
		boolean bolehSemua = bolehSupervisorAtauAdmin(tbmuser);
		String namaLogin = tbmuser == null ? "" : str(tbmuser.getUserNama()).trim();
		if (namaLogin.length() == 0 && tbmuser != null && tbmuser.getPedagang() != null) namaLogin = str(tbmuser.getPedagang().getNama()).trim();
		if (namaLogin.length() == 0 && tbmuser != null) namaLogin = str(tbmuser.getUserId()).trim();
		String kasir = bolehSemua ? payload.optString("kasir", "").trim() : namaLogin;
		String tanggal = payload.optString("tanggal", "").trim();
		String metode = payload.optString("metode", "").trim();
		if (tanggal.length() == 0 || kasir.length() == 0) { hasil.put("status", "error"); hasil.put("message", "Tanggal dan kasir wajib diisi."); return; }
		JSONObject aman = new JSONObject(); aman.put("tglMulai", tanggal); aman.put("tglSampai", tanggal); aman.put("kasirExact", kasir); aman.put("metodeExact", metode); aman.put("page", 1); aman.put("pageSize", 100);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			String kode = toko == null || toko.getKode() == null || toko.getKode().trim().isEmpty() ? "toko" + tokoId : toko.getKode().trim();
			JSONObject q = daftarOrderDenganSesi(session, tokoId, kode, aman);
			hasil.put("status", "success"); hasil.put("data", q.getJSONArray("data")); hasil.put("total", q.getLong("total"));
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	/**
	 * Laporan rekonsiliasi transaksi per kasir: modal buka, penerimaan per metode,
	 * kas closing, dan selisih. Transaksi dikelompokkan dari master pembayaran agar
	 * satu nota yang memiliki banyak rincian produk tidak dihitung berulang.
	 */
	private void prosesLaporanTransaksiPerKasir(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		if (tokoId == null) { hasil.put("status", "error"); hasil.put("message", "Toko tidak diketahui utk akun ini."); return; }
		boolean bolehSemua = bolehSupervisorAtauAdmin(tbmuser);
		String hariIni = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
		String mulai = payload.optString("tglMulai", "").trim(); if (mulai.length() == 0) mulai = hariIni;
		String sampai = payload.optString("tglSampai", "").trim(); if (sampai.length() == 0) sampai = hariIni;
		String namaLogin = tbmuser == null ? "" : str(tbmuser.getUserNama()).trim();
		if (namaLogin.length() == 0 && tbmuser != null && tbmuser.getPedagang() != null) namaLogin = str(tbmuser.getPedagang().getNama()).trim();
		if (namaLogin.length() == 0 && tbmuser != null) namaLogin = str(tbmuser.getUserId()).trim();
		String kasirDipilih = bolehSemua ? payload.optString("kasir", "").trim() : namaLogin;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			java.util.Map<String, JSONObject> perKasir = new java.util.LinkedHashMap<String, JSONObject>();
			String transaksi = "SELECT COALESCE(a.pembelian_anggota_koperasi,a.id) id_trx,"
					+ " COALESCE(NULLIF(TRIM(MAX(pak.kasir_login_nama)),''),'Kasir tidak tercatat') kasir,"
					+ " COALESCE(NULLIF(TRIM(MAX(a.carabayar)),''),'-') metode,"
					+ " COALESCE(MAX(pak.total_biaya),SUM(a.total)) nilai,"
					+ " COALESCE(MAX(pak.bayar_tunai),0) tunai,COALESCE(MAX(pak.bayar_non_tunai),0) non_tunai"
					+ " FROM koperasi.pembelian a LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON pak.id=a.pembelian_anggota_koperasi"
					+ " WHERE a.toko=? AND COALESCE(a.aktif,true)=true AND DATE(a.waktu)>=?::date AND DATE(a.waktu)<=?::date GROUP BY 1";
			String filterKasir = kasirDipilih.length() == 0 ? "" : " WHERE LOWER(TRIM(kasir))=LOWER(?)";
			java.sql.PreparedStatement pt = conn.prepareStatement("SELECT kasir,metode,COUNT(*),COALESCE(SUM(nilai),0),COALESCE(SUM(tunai),0),COALESCE(SUM(non_tunai),0) FROM (" + transaksi + ") trx" + filterKasir + " GROUP BY kasir,metode ORDER BY kasir,metode");
			int ix = 1; pt.setLong(ix++, tokoId.longValue()); pt.setString(ix++, mulai); pt.setString(ix++, sampai); if (kasirDipilih.length() > 0) pt.setString(ix++, kasirDipilih);
			java.sql.ResultSet rt = pt.executeQuery();
			while (rt.next()) {
				String namaKasir = rt.getString(1);
				JSONObject kasir = perKasir.get(namaKasir);
				if (kasir == null) {
					kasir = laporanTransaksiKasirBaru(namaKasir);
					perKasir.put(namaKasir, kasir);
				}
				JSONObject metode = new JSONObject();
				metode.put("nama", rt.getString(2)); metode.put("jumlahTransaksi", rt.getLong(3)); metode.put("total", rt.getDouble(4));
				kasir.getJSONArray("metodePembayaran").put(metode);
				kasir.put("jumlahTransaksi", kasir.getLong("jumlahTransaksi") + rt.getLong(3));
				kasir.put("totalTransaksi", kasir.getDouble("totalTransaksi") + rt.getDouble(4));
				kasir.put("totalTunai", kasir.getDouble("totalTunai") + rt.getDouble(5));
				kasir.put("totalNonTunai", kasir.getDouble("totalNonTunai") + rt.getDouble(6));
			}
			rt.close(); pt.close();

			String filterSesi = kasirDipilih.length() == 0 ? "" : " AND LOWER(TRIM(sk.kasir_nama))=LOWER(?)";
			String sesiSql = "SELECT COALESCE(NULLIF(TRIM(sk.kasir_nama),''),'Kasir tidak tercatat'),COUNT(*),COALESCE(SUM(sk.modalawal),0),"
					+ " COALESCE(SUM(CASE WHEN sk.status='TUTUP' THEN COALESCE(sk.uangfisik,0) ELSE COALESCE(sk.modalawal,0)+COALESCE((SELECT SUM(COALESCE(pak.bayar_tunai,0)) FROM koperasi.pembelian_anggota_koperasi pak WHERE pak.toko=sk.toko AND (pak.sesi_kas_kasir=sk.id OR (pak.sesi_kas_kasir IS NULL AND pak.kasir_login_nama=sk.kasir_nama)) AND pak.tanggal_pembayaran>=sk.waktubuka AND pak.tanggal_pembayaran<=NOW()),0) END),0),"
					+ " COALESCE(SUM(CASE WHEN sk.status='TUTUP' THEN 1 ELSE 0 END),0)"
					+ " FROM koperasi.sesi_kas_kasir sk WHERE sk.toko=? AND DATE(sk.waktubuka)>=?::date AND DATE(sk.waktubuka)<=?::date" + filterSesi + " GROUP BY 1 ORDER BY 1";
			java.sql.PreparedStatement ps = conn.prepareStatement(sesiSql);
			ix = 1; ps.setLong(ix++, tokoId.longValue()); ps.setString(ix++, mulai); ps.setString(ix++, sampai); if (kasirDipilih.length() > 0) ps.setString(ix++, kasirDipilih);
			java.sql.ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String namaKasir = rs.getString(1);
				JSONObject kasir = perKasir.get(namaKasir);
				if (kasir == null) {
					kasir = laporanTransaksiKasirBaru(namaKasir);
					perKasir.put(namaKasir, kasir);
				}
				kasir.put("jumlahSesi", rs.getLong(2)); kasir.put("modalAwal", rs.getDouble(3)); kasir.put("kasClosing", rs.getDouble(4));
				kasir.put("closingDikonfirmasi", rs.getLong(5) == rs.getLong(2) && rs.getLong(2) > 0);
			}
			rs.close(); ps.close();

			JSONArray data = new JSONArray(); double totalTransaksi = 0; double totalSelisih = 0; long jumlahTransaksi = 0;
			java.util.Iterator<JSONObject> it = perKasir.values().iterator();
			while (it.hasNext()) {
				JSONObject kasir = it.next();
				double kasSeharusnya = kasir.getDouble("modalAwal") + kasir.getDouble("totalTunai");
				double selisih = kasir.getDouble("kasClosing") - kasSeharusnya;
				kasir.put("kasSeharusnya", kasSeharusnya); kasir.put("selisih", selisih);
				totalTransaksi += kasir.getDouble("totalTransaksi"); totalSelisih += selisih; jumlahTransaksi += kasir.getLong("jumlahTransaksi"); data.put(kasir);
			}

			hasil.put("status", "success"); hasil.put("data", data); hasil.put("tglMulai", mulai); hasil.put("tglSampai", sampai);
			hasil.put("totalTransaksi", totalTransaksi); hasil.put("totalSelisih", totalSelisih); hasil.put("jumlahTransaksi", jumlahTransaksi);
			hasil.put("bolehFilterKasir", bolehSemua); hasil.put("kasirAktif", kasirDipilih);
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	private JSONObject laporanTransaksiKasirBaru(String namaKasir) throws Exception {
		JSONObject o = new JSONObject(); o.put("kasir", namaKasir == null ? "Kasir tidak tercatat" : namaKasir);
		o.put("jumlahSesi", 0); o.put("jumlahTransaksi", 0); o.put("modalAwal", 0); o.put("totalTransaksi", 0);
		o.put("totalTunai", 0); o.put("totalNonTunai", 0); o.put("kasClosing", 0); o.put("kasSeharusnya", 0);
		o.put("selisih", 0); o.put("closingDikonfirmasi", false); o.put("metodePembayaran", new JSONArray()); return o;
	}

	/**
	 * Rincian penyusun setiap angka pada laporan transaksi per kasir. Endpoint ini
	 * sengaja menggunakan sumber master pembayaran dan sesi kas yang sama dengan
	 * ringkasannya, sehingga popup dapat direkonsiliasi kembali sampai ke transaksi
	 * dan sesi asal. Kasir biasa tetap dikunci server-side ke identitas login.
	 */
	private void prosesLaporanTransaksiPerKasirDetail(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		if (tokoId == null) { hasil.put("status", "error"); hasil.put("message", "Toko tidak diketahui utk akun ini."); return; }
		boolean bolehSemua = bolehSupervisorAtauAdmin(tbmuser);
		String hariIni = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
		String mulai = payload.optString("tglMulai", "").trim(); if (mulai.length() == 0) mulai = hariIni;
		String sampai = payload.optString("tglSampai", "").trim(); if (sampai.length() == 0) sampai = hariIni;
		if (!mulai.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") || !sampai.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
			hasil.put("status", "error"); hasil.put("message", "Format tanggal laporan tidak valid."); return;
		}
		String namaLogin = tbmuser == null ? "" : str(tbmuser.getUserNama()).trim();
		if (namaLogin.length() == 0 && tbmuser != null && tbmuser.getPedagang() != null) namaLogin = str(tbmuser.getPedagang().getNama()).trim();
		if (namaLogin.length() == 0 && tbmuser != null) namaLogin = str(tbmuser.getUserId()).trim();
		String kasir = bolehSemua ? payload.optString("kasir", "").trim() : namaLogin;
		String komponen = payload.optString("komponen", "semua").trim().toLowerCase(java.util.Locale.ENGLISH);
		String metodeDipilih = payload.optString("metode", "").trim();
		boolean rincianTransaksi = "semua".equals(komponen) || "jumlah_transaksi".equals(komponen)
				|| "total_transaksi".equals(komponen) || "metode".equals(komponen);
		boolean rincianSesi = "semua".equals(komponen) || "modal_awal".equals(komponen)
				|| "kas_seharusnya".equals(komponen) || "kas_closing".equals(komponen) || "selisih".equals(komponen);
		if (!rincianTransaksi && !rincianSesi) { hasil.put("status", "error"); hasil.put("message", "Komponen laporan tidak dikenal."); return; }

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			JSONArray data = new JSONArray();
			JSONObject total = new JSONObject();
			total.put("jumlahSesi", 0); total.put("jumlahTransaksi", 0); total.put("qty", 0);
			total.put("totalTransaksi", 0); total.put("tunai", 0); total.put("nonTunai", 0);
			total.put("modalAwal", 0); total.put("kasSeharusnya", 0); total.put("kasClosing", 0); total.put("selisih", 0);
			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			if (rincianTransaksi) {
				String filterKasir = kasir.length() == 0 ? "" : " AND LOWER(NULLIF(TRIM(pak.kasir_login_nama),''))=LOWER(?)";
				String filterMetode = metodeDipilih.length() == 0 ? "" : " AND LOWER(COALESCE(a.carabayar,''))=LOWER(?)";
				String sql = "SELECT MAX(a.waktu),COALESCE(NULLIF(TRIM(MAX(pak.kode)),''),CAST(COALESCE(a.pembelian_anggota_koperasi,a.id) AS varchar)),"
						+ " COALESCE(NULLIF(TRIM(MAX(pak.kasir_login_nama)),''),'Kasir tidak tercatat'),COALESCE(NULLIF(TRIM(MAX(a.carabayar)),''),'-'),"
						+ " COALESCE(SUM(a.qty),0),COALESCE(MAX(pak.total_biaya),SUM(a.total)),COALESCE(MAX(pak.bayar_tunai),0),COALESCE(MAX(pak.bayar_non_tunai),0)"
						+ " FROM koperasi.pembelian a LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON pak.id=a.pembelian_anggota_koperasi"
						+ " WHERE a.toko=? AND COALESCE(a.aktif,true)=true AND DATE(a.waktu)>=?::date AND DATE(a.waktu)<=?::date"
						+ filterKasir + filterMetode + " GROUP BY COALESCE(a.pembelian_anggota_koperasi,a.id) ORDER BY MAX(a.waktu) DESC";
				java.sql.PreparedStatement ps = conn.prepareStatement(sql);
				int ix = 1; ps.setLong(ix++, tokoId.longValue()); ps.setString(ix++, mulai); ps.setString(ix++, sampai);
				if (kasir.length() > 0) ps.setString(ix++, kasir);
				if (metodeDipilih.length() > 0) ps.setString(ix++, metodeDipilih);
				java.sql.ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					JSONObject row = new JSONObject();
					java.sql.Timestamp waktu = rs.getTimestamp(1);
					row.put("waktu", waktu == null ? "" : fmt.format(waktu)); row.put("jenis", "Transaksi"); row.put("referensi", rs.getString(2));
					row.put("kasir", rs.getString(3)); row.put("metode", rs.getString(4)); row.put("jumlahTransaksi", 1); row.put("qty", rs.getDouble(5));
					row.put("totalTransaksi", rs.getDouble(6)); row.put("tunai", rs.getDouble(7)); row.put("nonTunai", rs.getDouble(8));
					row.put("modalAwal", 0); row.put("kasSeharusnya", 0); row.put("kasClosing", 0); row.put("selisih", 0); data.put(row);
					total.put("jumlahTransaksi", total.getLong("jumlahTransaksi") + 1); total.put("qty", total.getDouble("qty") + rs.getDouble(5));
					total.put("totalTransaksi", total.getDouble("totalTransaksi") + rs.getDouble(6));
					total.put("tunai", total.getDouble("tunai") + rs.getDouble(7)); total.put("nonTunai", total.getDouble("nonTunai") + rs.getDouble(8));
				}
				rs.close(); ps.close();
			}

			if (rincianSesi) {
				String filterKasir = kasir.length() == 0 ? "" : " AND LOWER(TRIM(sk.kasir_nama))=LOWER(?)";
				java.sql.PreparedStatement ps = conn.prepareStatement(
						"SELECT sk.id,sk.waktubuka,sk.waktututup,sk.status,COALESCE(NULLIF(TRIM(sk.kasir_nama),''),'Kasir tidak tercatat'),COALESCE(sk.modalawal,0),COALESCE(sk.uangfisik,0)"
						+ " FROM koperasi.sesi_kas_kasir sk WHERE sk.toko=? AND DATE(sk.waktubuka)>=?::date AND DATE(sk.waktubuka)<=?::date" + filterKasir + " ORDER BY sk.waktubuka DESC");
				int ix = 1; ps.setLong(ix++, tokoId.longValue()); ps.setString(ix++, mulai); ps.setString(ix++, sampai); if (kasir.length() > 0) ps.setString(ix++, kasir);
				java.sql.ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					long sesiId = rs.getLong(1); java.sql.Timestamp buka = rs.getTimestamp(2); java.sql.Timestamp tutup = rs.getTimestamp(3);
					String status = rs.getString(4); String namaKasir = rs.getString(5); double modal = rs.getDouble(6); double uangFisik = rs.getDouble(7);
					java.sql.PreparedStatement pd = conn.prepareStatement(
							"SELECT COUNT(*),COALESCE(SUM(total_biaya),0),COALESCE(SUM(bayar_tunai),0),COALESCE(SUM(bayar_non_tunai),0)"
							+ " FROM koperasi.pembelian_anggota_koperasi WHERE toko=? AND (sesi_kas_kasir=? OR (sesi_kas_kasir IS NULL"
							+ " AND LOWER(NULLIF(TRIM(kasir_login_nama),''))=LOWER(?) AND tanggal_pembayaran>=? AND tanggal_pembayaran<=?))");
					pd.setLong(1, tokoId.longValue()); pd.setLong(2, sesiId); pd.setString(3, namaKasir); pd.setTimestamp(4, buka);
					pd.setTimestamp(5, tutup == null ? new java.sql.Timestamp(System.currentTimeMillis()) : tutup);
					java.sql.ResultSet rd = pd.executeQuery(); long jumlah = 0; double nilai = 0; double tunai = 0; double nonTunai = 0;
					if (rd.next()) { jumlah = rd.getLong(1); nilai = rd.getDouble(2); tunai = rd.getDouble(3); nonTunai = rd.getDouble(4); }
					rd.close(); pd.close();
					double seharusnya = modal + tunai; double closing = "TUTUP".equalsIgnoreCase(status) ? uangFisik : seharusnya; double selisih = closing - seharusnya;
					JSONObject row = new JSONObject(); row.put("waktu", buka == null ? "" : fmt.format(buka)); row.put("jenis", "Sesi Kas " + str(status));
					row.put("referensi", "Sesi #" + sesiId); row.put("kasir", namaKasir); row.put("metode", "Rekonsiliasi"); row.put("jumlahTransaksi", jumlah); row.put("qty", 0);
					row.put("totalTransaksi", nilai); row.put("tunai", tunai); row.put("nonTunai", nonTunai); row.put("modalAwal", modal);
					row.put("kasSeharusnya", seharusnya); row.put("kasClosing", closing); row.put("selisih", selisih); data.put(row);
					total.put("jumlahSesi", total.getLong("jumlahSesi") + 1); total.put("jumlahTransaksi", total.getLong("jumlahTransaksi") + jumlah);
					total.put("totalTransaksi", total.getDouble("totalTransaksi") + nilai); total.put("tunai", total.getDouble("tunai") + tunai);
					total.put("nonTunai", total.getDouble("nonTunai") + nonTunai); total.put("modalAwal", total.getDouble("modalAwal") + modal);
					total.put("kasSeharusnya", total.getDouble("kasSeharusnya") + seharusnya); total.put("kasClosing", total.getDouble("kasClosing") + closing);
					total.put("selisih", total.getDouble("selisih") + selisih);
				}
				rs.close(); ps.close();
			}
			hasil.put("status", "success"); hasil.put("data", data); hasil.put("total", total);
			hasil.put("tglMulai", mulai); hasil.put("tglSampai", sampai); hasil.put("kasirAktif", kasir); hasil.put("komponen", komponen);
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	/**
	 * Sama persis sumber data dgn {@link #prosesLaporanOrderList} -- HANYA beda bentuk output
	 * (fokus kolom pembayaran: tanggal, metode, referensi order, jumlah), sesuai "Report Payment"
	 * di spesifikasi. Sengaja TIDAK query ulang dgn SQL terpisah supaya kedua laporan selalu 100%
	 * konsisten satu sama lain (satu sumber kebenaran).
	 */
	private void prosesLaporanPaymentList(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		if (tokoId == null) { hasil.put("status", "error"); hasil.put("message", "Toko tidak diketahui utk akun ini."); return; }

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			String tokoKode = toko == null || toko.getKode() == null || toko.getKode().trim().isEmpty()
					? ("toko" + tokoId) : toko.getKode().trim();

			JSONObject hasilQuery = daftarOrderDenganSesi(session, tokoId, tokoKode, payload);
			JSONArray orderArr = hasilQuery.getJSONArray("data");
			JSONArray paymentArr = new JSONArray();
			for (int i = 0; i < orderArr.length(); i++) {
				JSONObject o = orderArr.getJSONObject(i);
				JSONObject p = new JSONObject();
				p.put("waktu", o.get("waktu"));
				p.put("metode", o.get("metode"));
				p.put("orderKode", o.get("nomorNota"));
				p.put("sesiKode", o.get("sesiKode"));
				p.put("jumlah", o.get("totalBiaya"));
				p.put("bayarTunai", o.get("bayarTunai"));
				p.put("bayarNonTunai", o.get("bayarNonTunai"));
				paymentArr.put(p);
			}
			hasil.put("status", "success");
			hasil.put("data", paymentArr);
			hasil.put("total", hasilQuery.getLong("total"));
			hasil.put("page", hasilQuery.getInt("page"));
			hasil.put("pageSize", hasilQuery.getInt("pageSize"));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Query bersama Report Order + Report Payment (lihat JavaDoc {@link #prosesLaporanOrderList}).
	 * Payload: {@code tglMulai}/{@code tglSampai} (opsional, format {@code yyyy-MM-dd}),
	 * {@code cariPembeli} (opsional, cari nama pembeli), {@code page}/{@code pageSize}
	 * (paginasi, pola SAMA dgn {@link #prosesDashboardUmum}).
	 */
	private JSONObject daftarOrderDenganSesi(Session session, Long tokoId, String tokoKode, JSONObject payload) throws Exception {
		String tglMulai = payload.optString("tglMulai", "");
		String tglSampai = payload.optString("tglSampai", "");
		String cariPembeli = payload.optString("cariPembeli", payload.optString("keyword", "")).trim();
		String kasirExact = payload.optString("kasirExact", "").trim();
		String kasir = payload.optString("kasir", "").trim();
		String mesin = payload.optString("mesin", "").trim();
		String nomorNota = payload.optString("nomorNota", "").trim();
		String produk = payload.optString("produk", "").trim();
		String waktuMulai = payload.optString("waktuMulai", "").trim();
		String waktuSampai = payload.optString("waktuSampai", "").trim();
		String metodeExact = payload.optString("metodeExact", "").trim();
		double totalMinimal = Math.max(0, payload.optDouble("totalMinimal", 0));
		double totalMaksimal = Math.max(0, payload.optDouble("totalMaksimal", 0));
		double qtyMinimal = Math.max(0, payload.optDouble("qtyMinimal", 0));
		double qtyMaksimal = Math.max(0, payload.optDouble("qtyMaksimal", 0));
		boolean transaksiTidakValid = payload.optBoolean("transaksiTidakValid", false)
				|| "true".equalsIgnoreCase(payload.optString("transaksiTidakValid", ""))
				|| "1".equals(payload.optString("transaksiTidakValid", ""));
		int page = Math.max(1, payload.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(5, payload.optInt("pageSize", 10)));
		int offset = (page - 1) * pageSize;

		java.sql.Connection conn = session.connection();

		StringBuilder whereTrx = new StringBuilder("a.toko = ? AND COALESCE(a.aktif,true)=true");
		java.util.List<Object> paramsTrx = new java.util.ArrayList<Object>();
		paramsTrx.add(tokoId);
		if (tglMulai.length() > 0) { whereTrx.append(" AND DATE(a.waktu) >= CAST(? AS date)"); paramsTrx.add(tglMulai); }
		if (tglSampai.length() > 0) { whereTrx.append(" AND DATE(a.waktu) <= CAST(? AS date)"); paramsTrx.add(tglSampai); }
		if (waktuMulai.matches("[0-2][0-9]:[0-5][0-9]")) { whereTrx.append(" AND a.waktu::time >= CAST(? AS time)"); paramsTrx.add(waktuMulai); }
		if (waktuSampai.matches("[0-2][0-9]:[0-5][0-9]")) { whereTrx.append(" AND a.waktu::time <= CAST(? AS time)"); paramsTrx.add(waktuSampai); }
		if (kasirExact.length() > 0) {
			whereTrx.append(" AND LOWER(NULLIF(TRIM(pak.kasir_login_nama),'')) = LOWER(?)");
			paramsTrx.add(kasirExact);
		}
		if (kasirExact.length() == 0 && kasir.length() > 0) {
			whereTrx.append(" AND COALESCE(pak.kasir_login_nama,'') ILIKE ?");
			paramsTrx.add("%" + kasir + "%");
		}
		if (mesin.length() > 0) { whereTrx.append(" AND COALESCE(pak.nama_mesin,'') ILIKE ?"); paramsTrx.add("%" + mesin + "%"); }
		if (nomorNota.length() > 0) { whereTrx.append(" AND COALESCE(pak.kode,'') ILIKE ?"); paramsTrx.add("%" + nomorNota + "%"); }
		if (metodeExact.length() > 0) {
			whereTrx.append(" AND COALESCE(a.carabayar,'') ILIKE ?");
			paramsTrx.add("%" + metodeExact + "%");
		}
		if (produk.length() > 0) {
			whereTrx.append(" AND EXISTS (SELECT 1 FROM koperasi.pembelian af LEFT JOIN koperasi.produk pf ON pf.id=af.produk "
					+ "WHERE COALESCE(af.pembelian_anggota_koperasi,af.id)=COALESCE(a.pembelian_anggota_koperasi,a.id) "
					+ "AND (COALESCE(af.nama,'') ILIKE ? OR COALESCE(pf.kode,'') ILIKE ? OR COALESCE(pf.barcode,'') ILIKE ?))");
			String kwProduk = "%" + produk + "%";
			paramsTrx.add(kwProduk); paramsTrx.add(kwProduk); paramsTrx.add(kwProduk);
		}
		StringBuilder syaratHaving = new StringBuilder();
		java.util.List<Object> paramsHaving = new java.util.ArrayList<Object>();
		if (cariPembeli.length() > 0) {
			syaratHaving.append("(MAX(a.member) ILIKE ? OR COALESCE(MAX(pak.kode),'') ILIKE ?)");
			String kw = "%" + cariPembeli + "%";
			paramsHaving.add(kw); paramsHaving.add(kw);
		}
		if (totalMinimal > 0) { if (syaratHaving.length() > 0) syaratHaving.append(" AND "); syaratHaving.append("COALESCE(MAX(pak.total_biaya),SUM(a.total)) >= ?"); paramsHaving.add(Double.valueOf(totalMinimal)); }
		if (totalMaksimal > 0) { if (syaratHaving.length() > 0) syaratHaving.append(" AND "); syaratHaving.append("COALESCE(MAX(pak.total_biaya),SUM(a.total)) <= ?"); paramsHaving.add(Double.valueOf(totalMaksimal)); }
		if (qtyMinimal > 0) { if (syaratHaving.length() > 0) syaratHaving.append(" AND "); syaratHaving.append("COALESCE(SUM(a.qty),0) >= ?"); paramsHaving.add(Double.valueOf(qtyMinimal)); }
		if (qtyMaksimal > 0) { if (syaratHaving.length() > 0) syaratHaving.append(" AND "); syaratHaving.append("COALESCE(SUM(a.qty),0) <= ?"); paramsHaving.add(Double.valueOf(qtyMaksimal)); }
		if (transaksiTidakValid) {
			if (syaratHaving.length() > 0) syaratHaving.append(" AND ");
			// Hanya bandingkan transaksi yang mempunyai header/master. Selisih di atas
			// satu sen dianggap tidak valid; toleransi ini mencegah false-positive dari
			// pembulatan floating point tanpa menyembunyikan selisih nominal rupiah.
			syaratHaving.append("MAX(pak.id) IS NOT NULL AND ABS(COALESCE(MAX(pak.total_biaya),0) - COALESCE(SUM(a.total),0)) > 0.01");
		}
		String havingFilter = syaratHaving.length() == 0 ? "" : " HAVING " + syaratHaving.toString();

		// ---- Total baris (utk paginasi) ----
		java.sql.PreparedStatement psCount = conn.prepareStatement(
				"SELECT COUNT(*),COALESCE(SUM(nilai),0),COALESCE(SUM(qty),0) FROM (SELECT COALESCE(a.pembelian_anggota_koperasi, a.id),"
						+ " COALESCE(MAX(pak.total_biaya),SUM(a.total)) nilai,COALESCE(SUM(a.qty),0) qty FROM koperasi.pembelian a "
						+ "LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON pak.id = a.pembelian_anggota_koperasi WHERE "
						+ whereTrx + " GROUP BY 1" + havingFilter + ") x");
		int idx = 1;
		for (Object p : paramsTrx) ikatParam(psCount, idx++, p);
		for (Object p : paramsHaving) ikatParam(psCount, idx++, p);
		java.sql.ResultSet rsCount = psCount.executeQuery();
		long total = 0; double totalNilai = 0; double totalQty = 0;
		if (rsCount.next()) { total = rsCount.getLong(1); totalNilai = rsCount.getDouble(2); totalQty = rsCount.getDouble(3); }
		rsCount.close(); psCount.close();

		// ---- Data (dikelompokkan per transaksi, dicocokkan ke sesi via LATERAL, dipaginasi) ----
		String sql = "WITH sesi_bertingkat AS ("
				+ "  SELECT id, kasir_nama, kasir_user_id, waktubuka, waktututup,"
				+ "         ROW_NUMBER() OVER (ORDER BY waktubuka) AS nomor_sesi"
				+ "  FROM koperasi.sesi_kas_kasir WHERE toko = ?"
				+ "), order_dasar AS ("
				+ "  SELECT COALESCE(a.pembelian_anggota_koperasi, a.id) AS id_transaksi,"
				+ "         MAX(pak.kode) AS kode_nota, MAX(a.waktu) AS waktu, MAX(a.member) AS pembeli,"
				// Snapshot kasir transaksi adalah satu-satunya sumber identitas. oleh/olehId
				// merupakan metadata audit dan dapat berisi external_update.
				+ "         COALESCE(NULLIF(TRIM(MAX(pak.kasir_login_nama)),''),'Kasir tidak tercatat') AS kasir,"
				+ "         MAX(pak.sesi_kas_kasir) AS sesi_kas_kasir_id, MAX(pak.nama_mesin) AS nama_mesin,"
				+ "         MAX(pak.id_perangkat) AS id_perangkat, MAX(a.carabayar) AS metode,"
				+ "         SUM(a.qty) AS qty, SUM(a.total) AS subtotal_barang,"
				+ "         COALESCE(MAX(pak.total_diskon), SUM(COALESCE(a.diskon,0))) AS total_diskon,"
				+ "         COALESCE(MAX(pak.pajak),0) AS pajak,"
				+ "         COALESCE(MAX(pak.total_biaya), SUM(a.total)) AS total_biaya,"
				+ "         MAX(pak.total_biaya) AS total_master, COALESCE(SUM(a.total),0) AS total_detail,"
				+ "         ABS(COALESCE(MAX(pak.total_biaya),0) - COALESCE(SUM(a.total),0)) AS selisih_total,"
				+ "         COALESCE(MAX(pak.bayar_tunai),0) AS bayar_tunai,"
				+ "         COALESCE(MAX(pak.bayar_non_tunai),0) AS bayar_non_tunai"
				+ "  FROM koperasi.pembelian a LEFT JOIN koperasi.pembelian_anggota_koperasi pak"
				+ "       ON pak.id = a.pembelian_anggota_koperasi"
				+ "  WHERE " + whereTrx + " GROUP BY 1" + havingFilter
				+ "), order_dgn_sesi AS ("
				+ "  SELECT od.*, sb.id AS sesi_id, sb.nomor_sesi, sb.kasir_user_id FROM order_dasar od"
				+ "  LEFT JOIN LATERAL ("
				+ "    SELECT * FROM sesi_bertingkat s"
				+ "    WHERE (s.id = od.sesi_kas_kasir_id OR (od.sesi_kas_kasir_id IS NULL AND s.kasir_nama = od.kasir))"
				+ "      AND od.waktu >= s.waktubuka AND od.waktu <= COALESCE(s.waktututup, NOW())"
				+ "    ORDER BY s.waktubuka DESC LIMIT 1"
				+ "  ) sb ON true"
				+ ") "
				+ "SELECT id_transaksi, kode_nota, waktu, pembeli, kasir, metode, qty, subtotal_barang,"
				+ "       total_diskon, pajak, total_biaya, total_master, total_detail, selisih_total,"
				+ "       bayar_tunai, bayar_non_tunai, sesi_id, nomor_sesi,"
				+ "       ROW_NUMBER() OVER (PARTITION BY sesi_id ORDER BY waktu) AS nomor_order_dalam_sesi,"
				+ "       nama_mesin, id_perangkat, kasir_user_id"
				+ " FROM order_dgn_sesi ORDER BY waktu DESC LIMIT ? OFFSET ?";
		java.sql.PreparedStatement psData = conn.prepareStatement(sql);
		idx = 1;
		psData.setLong(idx++, tokoId.longValue()); // sesi_bertingkat.toko
		for (Object p : paramsTrx) ikatParam(psData, idx++, p); // order_dasar WHERE (termasuk a.toko lagi)
		for (Object p : paramsHaving) ikatParam(psData, idx++, p);
		psData.setInt(idx++, pageSize);
		psData.setInt(idx++, offset);
		java.sql.ResultSet rs = psData.executeQuery();
		JSONArray arr = new JSONArray();
		while (rs.next()) {
			JSONObject o = new JSONObject();
			o.put("idTransaksi", rs.getLong(1));
			String kodeNota = rs.getString(2) == null ? "" : rs.getString(2);
			java.sql.Timestamp w = rs.getTimestamp(3);
			o.put("waktu", w == null ? "" : w.toString());
			o.put("pembeli", rs.getString(4) == null || rs.getString(4).trim().isEmpty() ? "Umum" : rs.getString(4));
			o.put("kasir", rs.getString(5) == null ? "" : rs.getString(5));
			o.put("metode", rs.getString(6) == null || rs.getString(6).trim().isEmpty() ? "-" : rs.getString(6));
			o.put("qty", rs.getDouble(7));
			o.put("subtotalBarang", rs.getDouble(8));
			o.put("totalDiskon", rs.getDouble(9));
			o.put("pajak", rs.getDouble(10));
			o.put("totalBiaya", rs.getDouble(11));
			double totalMaster = rs.getDouble(12); boolean adaMaster = !rs.wasNull();
			double totalDetail = rs.getDouble(13);
			double selisihTotal = rs.getDouble(14);
			o.put("totalMaster", adaMaster ? Double.valueOf(totalMaster) : JSONObject.NULL);
			o.put("totalDetail", totalDetail);
			o.put("selisihTotal", selisihTotal);
			o.put("transaksiTidakValid", adaMaster && selisihTotal > 0.01d);
			o.put("bayarTunai", rs.getDouble(15));
			o.put("bayarNonTunai", rs.getDouble(16));
			rs.getLong(17); boolean adaSesi = !rs.wasNull();
			long nomorSesi = rs.getLong(18); boolean adaNomorSesi = !rs.wasNull();
			long nomorDalamSesi = rs.getLong(19);
			String sesiKode = adaSesi && adaNomorSesi ? (tokoKode + "/" + lpad(nomorSesi, 4)) : "-";
			o.put("sesiKode", sesiKode);
			o.put("nomorIdOrder", sesiKode);
			// Kode idempotensi mentah wajib ikut dikirim. Klien memakai nilai ini
			// untuk rekonsiliasi backup lokal <-> server; nomorNota hanyalah label
			// presentasi dan tidak boleh dijadikan identitas transaksi.
			o.put("kodeUnik", kodeNota);
			o.put("clientTrxId", kodeNota);
			o.put("nomorNota", "Order " + tokoKode + " - " + (adaNomorSesi ? lpad(nomorSesi, 4) : "0000") + " - " + lpad(nomorDalamSesi, 3)
					+ (kodeNota.length() > 0 ? " (" + kodeNota + ")" : ""));
			String namaMesin = rs.getString(20);
			o.put("namaMesin", namaMesin == null || namaMesin.trim().isEmpty() ? JSONObject.NULL : str(namaMesin));
			String idPerangkat = rs.getString(21);
			o.put("idPerangkat", idPerangkat == null || idPerangkat.trim().isEmpty() ? JSONObject.NULL : str(idPerangkat));
			String kasirUserId = rs.getString(22);
			if (kasirUserId == null || kasirUserId.trim().isEmpty()) kasirUserId = rs.getString(5);
			o.put("kasirUserId", kasirUserId == null || kasirUserId.trim().isEmpty() ? JSONObject.NULL : str(kasirUserId));
			arr.put(o);
		}
		rs.close(); psData.close();

		JSONObject out = new JSONObject();
		out.put("data", arr);
		out.put("total", total);
		out.put("totalNilai", totalNilai);
		out.put("totalQty", totalQty);
		out.put("page", page);
		out.put("pageSize", pageSize);
		return out;
	}

	/** Nol-padding kiri sederhana ({@code lpad(3, 4)} -> {@code "0003"}) -- dipakai membangun format nomor sesi/order/nota di {@link #daftarOrderDenganSesi}. */
	private static String lpad(long angka, int panjang) {
		String s = String.valueOf(Math.max(0, angka));
		StringBuilder sb = new StringBuilder();
		for (int i = s.length(); i < panjang; i++) sb.append('0');
		return sb.append(s).toString();
	}

	/**
	 * <h3>Report Sesi</h3> -- daftar sesi kas kasir milik toko, dgn total tunai/non-tunai DIHITUNG
	 * LIVE (bukan dibaca dari kolom {@code totaltunai}/{@code totalnontunai} yg cuma diisi SEKALI
	 * saat kasir menutup sesi) via subquery berkorelasi yg PERSIS mereproduksi rumus
	 * {@code SesiKasUtil.hitungPenjualan()} -- supaya sesi yg MASIH TERBUKA (kasir belum tutup kas)
	 * tetap menampilkan angka real-time, bukan nol.
	 */
	private void prosesLaporanSesiList(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		if (tokoId == null) { hasil.put("status", "error"); hasil.put("message", "Toko tidak diketahui utk akun ini."); return; }
		String tglMulai = payload.optString("tglMulai", "");
		String tglSampai = payload.optString("tglSampai", "");
		int page = Math.max(1, payload.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(5, payload.optInt("pageSize", 10)));
		int offset = (page - 1) * pageSize;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			String tokoKode = toko == null || toko.getKode() == null || toko.getKode().trim().isEmpty()
					? ("toko" + tokoId) : toko.getKode().trim();
			String tokoNama = toko == null ? "" : str(toko.getNama());
			java.sql.Connection conn = session.connection();

			StringBuilder where = new StringBuilder("toko = ?");
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			params.add(tokoId);
			if (tglMulai.length() > 0) { where.append(" AND DATE(waktubuka) >= ?"); params.add(tglMulai); }
			if (tglSampai.length() > 0) { where.append(" AND DATE(waktubuka) <= ?"); params.add(tglSampai); }

			java.sql.PreparedStatement psCount = conn.prepareStatement(
					"SELECT COUNT(*) FROM koperasi.sesi_kas_kasir WHERE " + where);
			int idx = 1;
			for (Object p : params) ikatParam(psCount, idx++, p);
			java.sql.ResultSet rsCount = psCount.executeQuery();
			long total = rsCount.next() ? rsCount.getLong(1) : 0;
			rsCount.close(); psCount.close();

			// Hubungkan transaksi ke sesi melalui foreign key sesi yang disimpan saat
			// checkout. Fallback hanya memakai snapshot kasir transaksi, tidak pernah
			// memakai oleh/olehId karena keduanya metadata audit.
			String sql = "SELECT sk.id, sk.kasir_nama, sk.waktubuka, sk.waktututup, sk.modalawal, sk.uangfisik, sk.status,"
					+ "       COALESCE((SELECT SUM(COALESCE(pak.bayar_tunai,0)) FROM koperasi.pembelian_anggota_koperasi pak"
					+ "                 WHERE pak.toko = sk.toko AND (pak.sesi_kas_kasir = sk.id OR (pak.sesi_kas_kasir IS NULL AND pak.kasir_login_nama = sk.kasir_nama))"
					+ "                 AND pak.tanggal_pembayaran >= sk.waktubuka AND pak.tanggal_pembayaran <= COALESCE(sk.waktututup, NOW())),0) AS total_tunai_live,"
					+ "       COALESCE((SELECT SUM(COALESCE(pak.bayar_non_tunai,0)) FROM koperasi.pembelian_anggota_koperasi pak"
					+ "                 WHERE pak.toko = sk.toko AND (pak.sesi_kas_kasir = sk.id OR (pak.sesi_kas_kasir IS NULL AND pak.kasir_login_nama = sk.kasir_nama))"
					+ "                 AND pak.tanggal_pembayaran >= sk.waktubuka AND pak.tanggal_pembayaran <= COALESCE(sk.waktututup, NOW())),0) AS total_nontunai_live,"
					+ "       ROW_NUMBER() OVER (ORDER BY sk.waktubuka) AS nomor_sesi"
					+ " FROM koperasi.sesi_kas_kasir sk WHERE " + where + " ORDER BY sk.waktubuka DESC LIMIT ? OFFSET ?";
			java.sql.PreparedStatement psData = conn.prepareStatement(sql);
			idx = 1;
			for (Object p : params) ikatParam(psData, idx++, p);
			psData.setInt(idx++, pageSize);
			psData.setInt(idx++, offset);
			java.sql.ResultSet rs = psData.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject o = new JSONObject();
				o.put("id", rs.getLong(1));
				o.put("kasir", rs.getString(2) == null ? "" : rs.getString(2));
				o.put("namaToko", tokoNama);
				java.sql.Timestamp wb = rs.getTimestamp(3);
				o.put("waktuBuka", wb == null ? "" : wb.toString());
				java.sql.Timestamp wt = rs.getTimestamp(4);
				o.put("waktuTutup", wt == null ? "" : wt.toString());
				double modalAwal = rs.getDouble(5);
				double uangFisik = rs.getDouble(6);
				String status = rs.getString(7) == null ? "" : rs.getString(7);
				double totalTunaiLive = rs.getDouble(8);
				double totalNonTunaiLive = rs.getDouble(9);
				long nomorSesi = rs.getLong(10);
				o.put("modalAwal", modalAwal);
				o.put("status", status);
				o.put("totalTunai", totalTunaiLive);
				o.put("totalNonTunai", totalNonTunaiLive);
				// "Saldo akhir": sesi sudah TUTUP -> uang fisik yg benar2 dihitung/dikonfirmasi kasir;
				// sesi MASIH BUKA -> proyeksi (modal awal + tunai live), krn uang fisik belum diisi.
				boolean tutup = "TUTUP".equals(status);
				o.put("saldoAkhir", tutup ? uangFisik : (modalAwal + totalTunaiLive));
				o.put("saldoAkhirDikonfirmasi", tutup);
				o.put("sesiKode", tokoKode + "/" + lpad(nomorSesi, 4));
				arr.put(o);
			}
			rs.close(); psData.close();

			hasil.put("status", "success");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("pageSize", pageSize);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Menandai SEMUA transaksi milik toko kasir yang belum terlayani (dalam rentang tanggal filter yg
	 * sedang aktif di layar, opsional) sbg terlayani sekaligus -- tombol "Layani Semua". Satu UPDATE
	 * bermassal (bukan loop per-baris seperti versi web) -- lebih cepat & atomik.
	 */
	private void prosesLayaniSemuaTransaksi(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		if (!bolehAksiCrudPesanan(tbmuser, "approve")) {
			hasil.put("status", "error");
			hasil.put("message", "Grup pengguna Anda tidak diizinkan menyetujui/melayani pesanan.");
			return;
		}
		Long tokoId = resolveTokoId(tbmuser, payload);
		if (tokoId == null) { hasil.put("status", "error"); hasil.put("message", "Toko tidak diketahui utk akun ini."); return; }
		String tglMulai = payload.optString("tglMulai", "");
		String tglSampai = payload.optString("tglSampai", "");

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder sql = new StringBuilder("UPDATE koperasi.pembelian SET terlayani = true WHERE toko = ? AND (terlayani IS NULL OR terlayani = false)");
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			params.add(tokoId);
			if (tglMulai.length() > 0) { sql.append(" AND DATE(waktu) >= ?"); params.add(tglMulai); }
			if (tglSampai.length() > 0) { sql.append(" AND DATE(waktu) <= ?"); params.add(tglSampai); }

			session.beginTransaction();
			java.sql.PreparedStatement ps = session.connection().prepareStatement(sql.toString());
			for (int i = 0; i < params.size(); i++) ikatParam(ps, i + 1, params.get(i));
			int jumlah = ps.executeUpdate();
			ps.close();
			session.getTransaction().commit();

			hasil.put("status", "success");
			hasil.put("jumlahBarisDiperbarui", jumlah);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * <h3>Tab "Keuangan &amp; Kinerja"</h3> -- port `_analisis_laba.jsp` (tren laba 14 hari, HPP dari
	 * {@link #petaHargaPokok}) + `_resep_hpp.jsp` (rekap margin per menu + pemakaian bahan baku 30
	 * hari) + `_leaderboard.jsp` (performa toko -- DISEDERHANAKAN jadi 3 kartu 1-baris per periode,
	 * BUKAN tabel multi-toko spt aslinya, krn versi kasir ini selalu single-toko).
	 */
	private void prosesDashboardKeuangan(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		if (tokoId == null) { hasil.put("status", "error"); hasil.put("message", "Toko tidak diketahui utk akun ini."); return; }
		String tanggalAcuan = tanggalAcuanDariPayload(payload);
		String acuanSql = "DATE '" + tanggalAcuan + "'";

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			java.util.Map<Long, Double> peta = petaHargaPokok(conn, tokoId);

			// ---- Tren laba 14 hari (agregat qty*hpp per produk per hari) ----
			java.sql.PreparedStatement psLaba = conn.prepareStatement(
					"SELECT DATE(a.waktu), a.produk, SUM(a.total), SUM(a.qty) FROM koperasi.pembelian a "
							+ "WHERE a.toko = ? AND DATE(a.waktu) >= " + acuanSql + " - INTERVAL '14 days'"
							+ " AND DATE(a.waktu) <= " + acuanSql + " GROUP BY DATE(a.waktu), a.produk ORDER BY 1");
			psLaba.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsLaba = psLaba.executeQuery();
			java.util.LinkedHashMap<String, double[]> perHari = new java.util.LinkedHashMap<String, double[]>();
			while (rsLaba.next()) {
				String tgl = str(rsLaba.getDate(1));
				long produkId = rsLaba.getLong(2);
				double omzet = rsLaba.getDouble(3);
				double qty = rsLaba.getDouble(4);
				Double hppUnit = peta.get(Long.valueOf(produkId));
				double modal = qty * (hppUnit == null ? 0 : hppUnit.doubleValue());
				double[] agg = perHari.get(tgl);
				if (agg == null) { agg = new double[] { 0, 0 }; perHari.put(tgl, agg); }
				agg[0] += omzet;
				agg[1] += modal;
			}
			rsLaba.close();
			psLaba.close();

			JSONArray tren = new JSONArray();
			double totalOmzet = 0, totalModal = 0;
			for (java.util.Map.Entry<String, double[]> e : perHari.entrySet()) {
				JSONObject t = new JSONObject();
				t.put("tanggal", e.getKey());
				t.put("omzet", e.getValue()[0]);
				t.put("modal", e.getValue()[1]);
				t.put("laba", e.getValue()[0] - e.getValue()[1]);
				tren.put(t);
				totalOmzet += e.getValue()[0];
				totalModal += e.getValue()[1];
			}
			JSONObject labaKpi = new JSONObject();
			labaKpi.put("omzet", totalOmzet);
			labaKpi.put("modal", totalModal);
			double labaKotor = totalOmzet - totalModal;
			labaKpi.put("labaKotor", labaKotor);
			labaKpi.put("marginPersen", totalOmzet > 0 ? (labaKotor / totalOmzet * 100.0) : 0);
			JSONObject laba = new JSONObject();
			laba.put("kpi", labaKpi);
			laba.put("tren", tren);

			// ---- Resep, HPP & Margin per menu ----
			java.sql.PreparedStatement psResep = conn.prepareStatement(
					"SELECT p.id, COALESCE(t.nama,'-'), p.nama, COALESCE(p.hargajual,0) FROM koperasi.produk p "
							+ "LEFT JOIN koperasi.toko t ON t.id = p.toko "
							+ "WHERE p.bahanbaku IS NOT NULL AND TRIM(p.bahanbaku) NOT IN ('','[]') AND p.toko = ? ORDER BY p.nama");
			psResep.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsResep = psResep.executeQuery();
			JSONArray rekapMenu = new JSONArray();
			double sumMargin = 0; int jumlahMenu = 0;
			String marginTertipisNama = null; double marginTertipisPersen = Double.MAX_VALUE;
			while (rsResep.next()) {
				long id = rsResep.getLong(1);
				double jual = rsResep.getDouble(4);
				Double hppObj = peta.get(Long.valueOf(id));
				double hpp = hppObj == null ? 0 : hppObj.doubleValue();
				double untung = jual - hpp;
				double marginPersen = jual > 0 ? (untung / jual * 100.0) : 0;
				JSONObject m = new JSONObject();
				m.put("tenant", str(rsResep.getString(2)));
				m.put("menu", str(rsResep.getString(3)));
				m.put("hpp", hpp);
				m.put("jual", jual);
				m.put("untung", untung);
				m.put("marginPersen", marginPersen);
				rekapMenu.put(m);
				sumMargin += marginPersen;
				jumlahMenu++;
				if (marginPersen < marginTertipisPersen) { marginTertipisPersen = marginPersen; marginTertipisNama = str(rsResep.getString(3)); }
			}
			rsResep.close();
			psResep.close();

			java.sql.PreparedStatement psBahan = conn.prepareStatement(
					"SELECT COALESCE(pr.nama,'-'), COALESCE(SUM(pb.qty),0), COALESCE(SUM(pb.qty * COALESCE(pr.hargabeli,0)),0) "
							+ "FROM koperasi.pemakaian_bahan_baku pb LEFT JOIN koperasi.produk pr ON pr.id = pb.produk "
							+ "WHERE pb.toko = ? AND pb.waktu >= " + acuanSql + " - INTERVAL '30 days'"
							+ " AND DATE(pb.waktu) <= " + acuanSql + " GROUP BY pr.nama ORDER BY 2 DESC");
			psBahan.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsBahan = psBahan.executeQuery();
			JSONArray rekapBahan = new JSONArray();
			double nilaiBahanTerpakai = 0;
			while (rsBahan.next()) {
				JSONObject b = new JSONObject();
				b.put("nama", str(rsBahan.getString(1)));
				b.put("qty", rsBahan.getDouble(2));
				double nilai = rsBahan.getDouble(3);
				b.put("nilai", nilai);
				nilaiBahanTerpakai += nilai;
				rekapBahan.put(b);
			}
			rsBahan.close();
			psBahan.close();

			JSONObject resepKpi = new JSONObject();
			resepKpi.put("menuBerResep", jumlahMenu);
			resepKpi.put("rataMargin", jumlahMenu > 0 ? (sumMargin / jumlahMenu) : 0);
			resepKpi.put("marginTertipisNama", marginTertipisNama == null ? "-" : marginTertipisNama);
			resepKpi.put("marginTertipisPersen", marginTertipisNama == null ? 0 : marginTertipisPersen);
			resepKpi.put("nilaiBahanTerpakai", nilaiBahanTerpakai);
			JSONObject resepHpp = new JSONObject();
			resepHpp.put("kpi", resepKpi);
			resepHpp.put("rekapMenu", rekapMenu);
			resepHpp.put("rekapBahan", rekapBahan);

			JSONObject performaToko = new JSONObject();
			performaToko.put("harian", performaPeriode(conn, tokoId, "DATE(a.waktu)=" + acuanSql));
			performaToko.put("mingguan", performaPeriode(conn, tokoId, "DATE(a.waktu) BETWEEN " + acuanSql + " - INTERVAL '7 days' AND " + acuanSql));
			performaToko.put("bulanan", performaPeriode(conn, tokoId, "DATE(a.waktu) BETWEEN " + acuanSql + " - INTERVAL '1 month' AND " + acuanSql));

			hasil.put("status", "success");
			hasil.put("tanggalAcuan", tanggalAcuan);
			hasil.put("laba", laba);
			hasil.put("resepHpp", resepHpp);
			hasil.put("performaToko", performaToko);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * <h3>Tab "Produk &amp; Inventaris"</h3> -- port `_stok.jsp` + `_bahan_baku.jsp` +
	 * `_rekonsiliasi_aset.jsp` + `_produk_terlaris.jsp` + `_rekap_produk_terlaris.jsp` +
	 * `_produk_paling_kurang_laku.jsp`. Panel rekonsiliasi aset DIBUNGKUS try/catch tersendiri --
	 * bergantung skema {@code asset}/{@code library} yang belum tentu terpasang/relevan di semua
	 * instalasi; kegagalannya TIDAK boleh menggagalkan seluruh tab, cukup kosongkan panel itu saja.
	 */
	private void prosesDashboardProduk(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		if (tokoId == null) { hasil.put("status", "error"); hasil.put("message", "Toko tidak diketahui utk akun ini."); return; }
		String tanggalAcuan = tanggalAcuanDariPayload(payload);
		String acuanSql = "DATE '" + tanggalAcuan + "'";
		String periode = payload.optString("periode", "bulanan");
		String intervalRekap = petaIntervalPeriode(periode, acuanSql);

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();

			java.sql.PreparedStatement psStok = conn.prepareStatement(
					"SELECT c.nama, COALESCE(c.stok,0) FROM koperasi.produk c WHERE c.toko = ? AND (c.aktif = true OR c.aktif IS NULL) ORDER BY c.stok ASC LIMIT 100");
			psStok.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsStok = psStok.executeQuery();
			JSONArray stokArr = new JSONArray();
			while (rsStok.next()) {
				double stok = rsStok.getDouble(2);
				JSONObject s = new JSONObject();
				s.put("namaProduk", str(rsStok.getString(1)));
				s.put("sisaStok", stok);
				s.put("status", stok <= 0 ? "HABIS" : (stok <= 5 ? "KRITIS" : "MENIPIS"));
				stokArr.put(s);
			}
			rsStok.close();
			psStok.close();

			java.sql.PreparedStatement psBahan = conn.prepareStatement(
					"SELECT COALESCE(pr.nama,'-'), COALESCE(pr.stok,0), COALESCE(SUM(pb.qty),0) "
							+ "FROM koperasi.pemakaian_bahan_baku pb LEFT JOIN koperasi.produk pr ON pr.id = pb.produk "
							+ "WHERE pb.toko = ? AND pb.waktu >= " + acuanSql + " - INTERVAL '30 days'"
							+ " AND DATE(pb.waktu) <= " + acuanSql + " GROUP BY pr.id, pr.nama, pr.stok ORDER BY 3 DESC");
			psBahan.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsBahan = psBahan.executeQuery();
			JSONArray bahanList = new JSONArray();
			int segeraHabis = 0; String palingMendesakNama = null; double palingMendesakHari = Double.MAX_VALUE;
			while (rsBahan.next()) {
				String nama = str(rsBahan.getString(1));
				double sisa = rsBahan.getDouble(2);
				double terpakai = rsBahan.getDouble(3);
				double perHari = terpakai / 30.0;
				JSONObject b = new JSONObject();
				b.put("nama", nama);
				b.put("sisa", sisa);
				b.put("terpakai", terpakai);
				if (perHari > 0) {
					double estimasiHari = sisa / perHari;
					b.put("estimasiHari", estimasiHari);
					if (estimasiHari < 7) segeraHabis++;
					if (estimasiHari < palingMendesakHari) { palingMendesakHari = estimasiHari; palingMendesakNama = nama; }
				} else {
					b.put("estimasiHari", JSONObject.NULL);
				}
				bahanList.put(b);
			}
			rsBahan.close();
			psBahan.close();

			java.sql.PreparedStatement psNilaiStok = conn.prepareStatement(
					"SELECT COALESCE(SUM(COALESCE(stok,0) * COALESCE(hargabeli,0)),0) FROM koperasi.produk "
							+ "WHERE toko = ? AND id IN (SELECT DISTINCT produk FROM koperasi.pemakaian_bahan_baku WHERE toko = ?)");
			psNilaiStok.setLong(1, tokoId.longValue());
			psNilaiStok.setLong(2, tokoId.longValue());
			java.sql.ResultSet rsNilaiStok = psNilaiStok.executeQuery();
			double nilaiStokBahan = rsNilaiStok.next() ? rsNilaiStok.getDouble(1) : 0;
			rsNilaiStok.close();
			psNilaiStok.close();

			JSONObject bahanKpi = new JSONObject();
			bahanKpi.put("dipantau", bahanList.length());
			bahanKpi.put("segeraHabis", segeraHabis);
			bahanKpi.put("palingMendesakNama", palingMendesakNama == null ? "-" : palingMendesakNama);
			bahanKpi.put("nilaiStok", nilaiStokBahan);
			JSONObject bahanBaku = new JSONObject();
			bahanBaku.put("kpi", bahanKpi);
			bahanBaku.put("list", bahanList);

			// ---- Rekonsiliasi Aset (opsional, modul terpisah -- gagal diam-diam bila skema tak ada) ----
			JSONArray asetList = new JSONArray();
			int tertaut = 0, stokCocok = 0, perluDicek = 0;
			try {
				java.sql.PreparedStatement psAset = conn.prepareStatement(
						"SELECT COALESCE(t.nama,'-'), p.nama, COALESCE(ma.kode,'') || ' - ' || COALESCE(ma.nama,''), COALESCE(p.stok,0), "
								+ "COALESCE((SELECT SUM((a.qty + a.qtybonus) * b.jenis) FROM asset.detail_transaksi_asset a "
								+ "INNER JOIN library.kode_transaksi b ON a.kode_transaksi = b.id WHERE a.master_asset = p.master_asset), 0) "
								+ "FROM koperasi.produk p INNER JOIN asset.master_asset ma ON ma.id = p.master_asset "
								+ "LEFT JOIN koperasi.toko t ON t.id = p.toko WHERE p.master_asset IS NOT NULL AND p.toko = ? ORDER BY p.nama");
				psAset.setLong(1, tokoId.longValue());
				java.sql.ResultSet rsAset = psAset.executeQuery();
				while (rsAset.next()) {
					double stokKantin = rsAset.getDouble(4);
					double stokAset = rsAset.getDouble(5);
					double selisih = stokKantin - stokAset;
					JSONObject a = new JSONObject();
					a.put("tenant", str(rsAset.getString(1)));
					a.put("produk", str(rsAset.getString(2)));
					a.put("aset", str(rsAset.getString(3)));
					a.put("stokKantin", stokKantin);
					a.put("stokAset", stokAset);
					a.put("selisih", selisih);
					asetList.put(a);
					tertaut++;
					if (Math.abs(selisih) <= 0.001) stokCocok++; else perluDicek++;
				}
				rsAset.close();
				psAset.close();
			} catch (Exception exAset) {
				ais.common.ErrorAuditUtil.record(exAset, "auto-audit PosApi.prosesDashboardProduk.rekonsiliasiAset");
			}
			JSONObject asetKpi = new JSONObject();
			asetKpi.put("tertaut", tertaut);
			asetKpi.put("stokCocok", stokCocok);
			asetKpi.put("perluDicek", perluDicek);
			JSONObject rekonsiliasiAset = new JSONObject();
			rekonsiliasiAset.put("kpi", asetKpi);
			rekonsiliasiAset.put("list", asetList);

			java.sql.PreparedStatement psTerlaris = conn.prepareStatement(
					"SELECT c.nama, SUM(a.qty) FROM koperasi.pembelian a INNER JOIN koperasi.produk c ON a.produk = c.id "
							+ "WHERE DATE(a.waktu) >= " + acuanSql + " - INTERVAL '1 month' AND DATE(a.waktu) <= " + acuanSql
							+ " AND a.toko = ? GROUP BY c.id, c.nama ORDER BY 2 DESC LIMIT 10");
			psTerlaris.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsTerlaris = psTerlaris.executeQuery();
			JSONArray produkTerlaris = new JSONArray();
			while (rsTerlaris.next()) {
				JSONObject p = new JSONObject();
				p.put("nama", str(rsTerlaris.getString(1)));
				p.put("qty", rsTerlaris.getDouble(2));
				produkTerlaris.put(p);
			}
			rsTerlaris.close();
			psTerlaris.close();

			java.sql.PreparedStatement psMetode = conn.prepareStatement(
					"SELECT COALESCE(a.carabayar,'-'), SUM(a.total) FROM koperasi.pembelian a "
							+ "WHERE DATE(a.waktu) >= " + acuanSql + " - INTERVAL '1 month' AND DATE(a.waktu) <= " + acuanSql
							+ " AND a.toko = ? GROUP BY a.carabayar ORDER BY 2 DESC");
			psMetode.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsMetode = psMetode.executeQuery();
			JSONArray metodeBayar = new JSONArray();
			while (rsMetode.next()) {
				JSONObject m = new JSONObject();
				m.put("nama", str(rsMetode.getString(1)));
				m.put("total", rsMetode.getDouble(2));
				metodeBayar.put(m);
			}
			rsMetode.close();
			psMetode.close();

			java.sql.PreparedStatement psRekapTerlaris = conn.prepareStatement(
					"SELECT c.nama, SUM(a.qty), SUM(a.total) FROM koperasi.pembelian a INNER JOIN koperasi.produk c ON a.produk = c.id "
							+ "WHERE a.toko = ? AND " + intervalRekap + " GROUP BY c.id, c.nama ORDER BY 2 DESC");
			psRekapTerlaris.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsRekapTerlaris = psRekapTerlaris.executeQuery();
			JSONArray rekapProdukTerlaris = new JSONArray();
			while (rsRekapTerlaris.next()) {
				JSONObject r = new JSONObject();
				r.put("nama", str(rsRekapTerlaris.getString(1)));
				r.put("qty", rsRekapTerlaris.getDouble(2));
				r.put("total", rsRekapTerlaris.getDouble(3));
				rekapProdukTerlaris.put(r);
			}
			rsRekapTerlaris.close();
			psRekapTerlaris.close();

			// Gap-closure "Analisa Produk: jam sibuk per-produk" -- jamSibuk yang sudah ada
			// (prosesDashboardUmum/prosesDashboardPelanggan) selalu agregat SELURUH toko, tidak per
			// item. Dibatasi TOP 5 produk (30 hari terakhir) supaya query murah & chart tetap terbaca.
			java.sql.PreparedStatement psJamSibukProduk = conn.prepareStatement(
					"SELECT c.id, c.nama, EXTRACT(HOUR FROM a.waktu) AS jam, SUM(a.qty) "
							+ "FROM koperasi.pembelian a INNER JOIN koperasi.produk c ON a.produk = c.id "
							+ "WHERE a.toko = ? AND DATE(a.waktu) >= " + acuanSql + " - INTERVAL '30 days' AND DATE(a.waktu) <= " + acuanSql + " "
							+ "AND c.id IN (SELECT produk FROM koperasi.pembelian WHERE toko = ? AND DATE(waktu) >= " + acuanSql
							+ " - INTERVAL '30 days' AND DATE(waktu) <= " + acuanSql + " "
							+ "GROUP BY produk ORDER BY SUM(qty) DESC LIMIT 5) "
							+ "GROUP BY c.id, c.nama, jam ORDER BY c.nama, jam");
			psJamSibukProduk.setLong(1, tokoId.longValue());
			psJamSibukProduk.setLong(2, tokoId.longValue());
			java.sql.ResultSet rsJamSibukProduk = psJamSibukProduk.executeQuery();
			java.util.LinkedHashMap<Long, JSONObject> petaJamSibukProduk = new java.util.LinkedHashMap<Long, JSONObject>();
			while (rsJamSibukProduk.next()) {
				long idProduk = rsJamSibukProduk.getLong(1);
				JSONObject entri = petaJamSibukProduk.get(idProduk);
				if (entri == null) {
					entri = new JSONObject();
					entri.put("produkId", idProduk);
					entri.put("nama", str(rsJamSibukProduk.getString(2)));
					double[] jamArr = new double[24];
					JSONArray jamJson = new JSONArray();
					for (int j = 0; j < 24; j++) jamJson.put(0);
					entri.put("jam", jamJson);
					petaJamSibukProduk.put(idProduk, entri);
				}
				int jamIdx = (int) rsJamSibukProduk.getDouble(3);
				if (jamIdx >= 0 && jamIdx < 24) {
					entri.getJSONArray("jam").put(jamIdx, rsJamSibukProduk.getDouble(4));
				}
			}
			rsJamSibukProduk.close();
			psJamSibukProduk.close();
			JSONArray jamSibukPerProduk = new JSONArray();
			for (JSONObject entri : petaJamSibukProduk.values()) jamSibukPerProduk.put(entri);

			// Gap-closure "Analisa Produk: perputaran stok (turnover)" -- formula SAMA PERSIS dgn
			// laporan JSP ad-hoc "Perputaran Stok (Turnover)" (ais.action.master.koperasi.helper.
			// LaporanKantinUtil, kode "perputaran_stok") -- diport apa adanya, BUKAN dihitung ulang
			// beda rumus, supaya kedua tampilan selalu sama persis.
			java.sql.PreparedStatement psTurnover = conn.prepareStatement(
					"SELECT pr.kode, pr.nama, SUM(COALESCE(a.qty,0)), COALESCE(pr.stok,0), "
							+ "(CASE WHEN COALESCE(pr.stok,0) > 0 THEN SUM(COALESCE(a.qty,0))/COALESCE(pr.stok,0) ELSE 0 END) "
							+ "FROM koperasi.pembelian a JOIN koperasi.produk pr ON pr.id = a.produk "
							+ "WHERE a.toko = ? AND " + intervalRekap + " GROUP BY pr.id, pr.kode, pr.nama, pr.stok ORDER BY 5 DESC");
			psTurnover.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsTurnover = psTurnover.executeQuery();
			JSONArray perputaranStok = new JSONArray();
			while (rsTurnover.next()) {
				JSONObject t = new JSONObject();
				t.put("kode", str(rsTurnover.getString(1)));
				t.put("nama", str(rsTurnover.getString(2)));
				t.put("qtyTerjual", rsTurnover.getDouble(3));
				t.put("stokKini", rsTurnover.getDouble(4));
				t.put("perputaran", rsTurnover.getDouble(5));
				perputaranStok.put(t);
			}
			rsTurnover.close();
			psTurnover.close();

			// Gap-closure "Survey Kepuasan Pelanggan" -- lingkup TOKO (bukan per-produk, lihat
			// JavaDoc entity SurveyKepuasanPos), rata-rata + jumlah responden 30 hari terakhir.
			java.sql.PreparedStatement psKepuasan = conn.prepareStatement(
					"SELECT COALESCE(AVG(rating),0), COUNT(*) FROM koperasi.survey_kepuasan_pos "
							+ "WHERE toko = ? AND waktu >= " + acuanSql + " - INTERVAL '30 days' AND DATE(waktu) <= " + acuanSql);
			psKepuasan.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsKepuasan = psKepuasan.executeQuery();
			JSONObject kepuasanPelanggan = new JSONObject();
			if (rsKepuasan.next()) {
				kepuasanPelanggan.put("rataRating", rsKepuasan.getDouble(1));
				kepuasanPelanggan.put("jumlahResponden", rsKepuasan.getInt(2));
			} else {
				kepuasanPelanggan.put("rataRating", 0);
				kepuasanPelanggan.put("jumlahResponden", 0);
			}
			rsKepuasan.close();
			psKepuasan.close();

			java.sql.PreparedStatement psKurang = conn.prepareStatement(
					"SELECT c.nama, COALESCE(SUM(a.qty),0) FROM koperasi.produk c "
							+ "LEFT JOIN koperasi.pembelian a ON (a.produk = c.id AND DATE(a.waktu) >= " + acuanSql
							+ " - INTERVAL '30 days' AND DATE(a.waktu) <= " + acuanSql + ") "
							+ "WHERE (c.aktif = true OR c.aktif IS NULL) AND c.toko = ? GROUP BY c.id, c.nama HAVING COALESCE(SUM(a.qty),0) <= 5 ORDER BY 2 ASC, c.nama ASC LIMIT 20");
			psKurang.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsKurang = psKurang.executeQuery();
			JSONArray produkKurangLaku = new JSONArray();
			while (rsKurang.next()) {
				JSONObject k = new JSONObject();
				k.put("nama", str(rsKurang.getString(1)));
				k.put("terjual", rsKurang.getDouble(2));
				produkKurangLaku.put(k);
			}
			rsKurang.close();
			psKurang.close();

			hasil.put("status", "success");
			hasil.put("tanggalAcuan", tanggalAcuan);
			hasil.put("stok", stokArr);
			hasil.put("bahanBaku", bahanBaku);
			hasil.put("rekonsiliasiAset", rekonsiliasiAset);
			hasil.put("produkTerlaris", produkTerlaris);
			hasil.put("metodeBayar", metodeBayar);
			hasil.put("rekapProdukTerlaris", rekapProdukTerlaris);
			hasil.put("produkKurangLaku", produkKurangLaku);
			hasil.put("jamSibukPerProduk", jamSibukPerProduk);
			hasil.put("perputaranStok", perputaranStok);
			hasil.put("kepuasanPelanggan", kepuasanPelanggan);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * <h3>Tab "Perilaku Pelanggan"</h3> -- port `_peak_hour.jsp` + `_rekap_pelanggan_terloyal.jsp`.
	 */
	private void prosesDashboardPelanggan(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		if (tokoId == null) { hasil.put("status", "error"); hasil.put("message", "Toko tidak diketahui utk akun ini."); return; }
		String tanggalAcuan = tanggalAcuanDariPayload(payload);
		String acuanSql = "DATE '" + tanggalAcuan + "'";
		String periode = payload.optString("periode", "bulanan");
		String intervalRekap = petaIntervalPeriode(periode, acuanSql);

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();

			java.sql.PreparedStatement psJam = conn.prepareStatement(
					"SELECT EXTRACT(HOUR FROM a.waktu), COUNT(a.id) FROM koperasi.pembelian a "
							+ "WHERE DATE(a.waktu) >= " + acuanSql + " - INTERVAL '1 month' AND DATE(a.waktu) <= " + acuanSql
							+ " AND a.toko = ? GROUP BY 1 ORDER BY 1");
			psJam.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsJam = psJam.executeQuery();
			JSONArray jamSibuk = new JSONArray();
			while (rsJam.next()) {
				JSONObject j = new JSONObject();
				j.put("jam", rsJam.getInt(1));
				j.put("jumlah", rsJam.getLong(2));
				jamSibuk.put(j);
			}
			rsJam.close();
			psJam.close();

			java.sql.PreparedStatement psLoyal = conn.prepareStatement(
					"SELECT b.nama, COUNT(a.id), SUM(a.total) FROM koperasi.pembelian a "
							+ "INNER JOIN koperasi.anggota_koperasi b ON a.anggota_koperasi = b.id "
							+ "WHERE DATE(a.waktu) >= " + acuanSql + " - INTERVAL '1 month' AND DATE(a.waktu) <= " + acuanSql
							+ " AND a.anggota_koperasi IS NOT NULL AND a.toko = ? "
							+ "GROUP BY b.id, b.nama ORDER BY 3 DESC LIMIT 10");
			psLoyal.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsLoyal = psLoyal.executeQuery();
			JSONArray pembeliTerloyal = new JSONArray();
			while (rsLoyal.next()) {
				JSONObject p = new JSONObject();
				p.put("nama", str(rsLoyal.getString(1)));
				p.put("frekuensi", rsLoyal.getLong(2));
				p.put("total", rsLoyal.getDouble(3));
				pembeliTerloyal.put(p);
			}
			rsLoyal.close();
			psLoyal.close();

			java.sql.PreparedStatement psRekapLoyal = conn.prepareStatement(
					"SELECT ak.nama, COUNT(a.id), SUM(a.total) FROM koperasi.pembelian a "
							+ "INNER JOIN koperasi.anggota_koperasi ak ON a.anggota_koperasi = ak.id "
							+ "WHERE a.toko = ? AND a.anggota_koperasi IS NOT NULL AND " + intervalRekap + " GROUP BY ak.id, ak.nama ORDER BY 3 DESC");
			psRekapLoyal.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsRekapLoyal = psRekapLoyal.executeQuery();
			JSONArray rekapPelangganTerloyal = new JSONArray();
			while (rsRekapLoyal.next()) {
				JSONObject r = new JSONObject();
				r.put("nama", str(rsRekapLoyal.getString(1)));
				r.put("frekuensi", rsRekapLoyal.getLong(2));
				r.put("total", rsRekapLoyal.getDouble(3));
				rekapPelangganTerloyal.put(r);
			}
			rsRekapLoyal.close();
			psRekapLoyal.close();

			hasil.put("status", "success");
			hasil.put("tanggalAcuan", tanggalAcuan);
			hasil.put("jamSibuk", jamSibuk);
			hasil.put("pembeliTerloyal", pembeliTerloyal);
			hasil.put("rekapPelangganTerloyal", rekapPelangganTerloyal);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Toko yang berlaku utk kasir yang login -- pedagang DIKUNCI ke tokonya sendiri (TIDAK bisa
	 * dipalsukan lewat parameter {@code tokoId} di body), admin-kantin (tanpa relasi Pedagang) boleh
	 * memilih toko manapun lewat parameter. Pola SAMA PERSIS dgn resolusi toko di {@code _pos.jsp}.
	 */
	private Long resolveTokoId(Tbmuser tbmuser, JSONObject payload) {
		Long tokoDariPayload = ambilTokoIdPayload(payload);
		Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		boolean roleMultiToko = role != null && role.getTokoAksesJson() != null;
		if (roleMultiToko) {
			Session session = HibernateUtil.getSessionFactory().openSession();
			try {
				java.util.List<Toko> daftar = KantinHelper.daftarTokoBolehDiakses(session, tbmuser);
				Long kandidat = tokoDariPayload != null ? tokoDariPayload : (tbmuser == null ? null : tbmuser.getTokoAktifMultiToko());
				if (kandidat != null) {
					for (Toko t : daftar) {
						if (t.getId() != null && t.getId().equals(kandidat)) return kandidat;
					}
				}
				return daftar.isEmpty() ? null : daftar.get(0).getId();
			} finally {
				HibernateUtil.closeSessionQuietly(session);
			}
		}
		Pedagang pedagang = tbmuser.getPedagang();
		// Multi-toko (lihat JavaDoc Tbmuser.tokoAktifMultiToko di database.model) -- SELALU dipercaya
		// lebih dulu bila terisi; pengguna toko-tunggal biasa tidak pernah mengisi field ini.
		if (pedagang != null && tbmuser.getTokoAktifMultiToko() != null) {
			return tbmuser.getTokoAktifMultiToko();
		}
		Toko toko = pedagang == null ? null : pedagang.getToko();
		if (toko != null) return toko.getId();
		return tokoDariPayload;
	}

	private static Long ambilTokoIdPayload(JSONObject payload) {
		if (payload == null) return null;
		String[] kunci = new String[] { "tokoId", "id_toko", "idToko", "toko_id" };
		for (int i = 0; i < kunci.length; i++) {
			try {
				if (!payload.isNull(kunci[i])) {
					String v = (payload.get(kunci[i]) + "").trim();
					if (v.length() > 0 && Common.isNumber(v)) {
						Long id = Long.valueOf(v);
						if (id.longValue() > 0) return id;
					}
				}
			} catch (Exception e) { /* coba kunci berikutnya */ }
		}
		return null;
	}

	/**
	 * Membaca seluruh body permintaan sebagai teks lalu mem-parse-nya sebagai JSON. Body kosong/tidak
	 * valid → objek JSON kosong (bukan exception). Sengaja try/finally manual (BUKAN try-with-resources
	 * -- build Ant proyek ini memakai {@code -source 1.6}, yang tidak mendukung sintaks itu).
	 */
	/**
	 * <h3>Fitur "Laporan-Laporan e-Kantin" (versi Desktop) -- adaptor menuju {@code LaporanKantinUtil}.</h3>
	 *
	 * <p>{@link ais.action.master.koperasi.helper.LaporanKantinUtil#build} (mesin ~150 laporan yang
	 * SAMA dipakai versi web) menerima {@link HttpServletRequest} mentah dan membaca DUA hal dari
	 * situ yang TIDAK tersedia apa adanya di konteks token {@code PosApi}: (1)
	 * {@code request.getParameter(...)} -- padahal klien Desktop mengirim JSON body, bukan form params
	 * -- diselesaikan dengan {@link ParamRequestWrapper} yang membungkus request asli dan menjawab
	 * {@code getParameter} dari payload JSON; (2) {@code Common.getCurrentUser(request)} -- otentikasi
	 * cookie-session, sedangkan Desktop otentikasi TOKEN -- diselesaikan dengan MENITIPKAN
	 * {@code tbmuser} (SUDAH diresolusi dari token oleh {@link #proses}) ke atribut session
	 * {@code "mytbmuser"} SAMA PERSIS kunci yang dibaca {@code CommonCurrentSessionHelper.getCurrentUser}
	 * -- dipulihkan di {@code finally} agar tidak membocorkan identitas token ke request lain yang
	 * kebetulan berbagi {@code HttpSession} (mis. bila kasir yang sama juga login web di browser yang
	 * sama). TIDAK ADA perubahan pada {@code LaporanKantinUtil}/{@code Common} sama sekali -- adaptor
	 * murni di sisi pemanggil.</p>
	 *
	 * <p><b>Sesi Hibernate:</b> {@code LaporanKantinUtil.build()} memakai
	 * {@code HibernateUtil.currentSession()} yang (di luar eksekusi ZK) JATUH ke
	 * {@code currentNativeSession()} -- session ThreadLocal yang TIDAK PERNAH ditutup otomatis di
	 * konteks servlet POS API ini (tidak ada {@code FilterJSP}/{@code OpenSessionInView} yang menutupnya
	 * di akhir request). WAJIB ditutup manual di {@code finally} lewat
	 * {@link HibernateUtil#closeSession()} -- alpa di sini berarti kebocoran koneksi c3p0 pelan-pelan
	 * tiap kali laporan dijalankan dari Desktop (lihat COOKBOOK POLA B di {@code HibernateUtil}).</p>
	 */
	private void prosesLaporanJalankan(HttpServletRequest request, Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		HttpSession sesiHttp = request.getSession(true);
		Object userSebelumnya = sesiHttp.getAttribute("mytbmuser");
		sesiHttp.setAttribute("mytbmuser", tbmuser);
		try {
			HttpServletRequest wrapped = new ParamRequestWrapper(request, payload);
			ais.action.master.koperasi.helper.LaporanKantinUtil.Hasil H =
					ais.action.master.koperasi.helper.LaporanKantinUtil.build(wrapped);
			if (!"00".equals(H.status)) {
				hasil.put("status", H.status);
				hasil.put("message", H.message);
				return;
			}
			JSONArray kolom = new JSONArray();
			for (ais.action.master.koperasi.helper.LaporanKantinUtil.Kolom kl : H.kolom) {
				JSONObject o = new JSONObject();
				o.put("l", kl.label);
				o.put("t", kl.tipe);
				kolom.put(o);
			}
			java.text.SimpleDateFormat fmtTgl = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm");
			JSONArray baris = new JSONArray();
			for (Object[] row : H.baris) {
				JSONArray b = new JSONArray();
				for (int i = 0; i < H.tipe.length; i++) {
					Object v = i < row.length ? row[i] : null;
					if ("num".equals(H.tipe[i])) {
						if (v instanceof Number) { b.put(((Number) v).doubleValue()); }
						else if (v == null) { b.put(JSONObject.NULL); }
						else { b.put(0.0); }
					} else if ("tgl".equals(H.tipe[i])) {
						b.put(v instanceof java.util.Date ? fmtTgl.format((java.util.Date) v) : "");
					} else {
						b.put(v == null ? "" : v.toString());
					}
				}
				baris.put(b);
			}
			hasil.put("status", "success");
			hasil.put("judul", H.judul);
			hasil.put("kolom", kolom);
			hasil.put("baris", baris);
			hasil.put("catatan", H.catatan);
			hasil.put("lockToko", H.lockToko);
			hasil.put("grup", H.grup);
			hasil.put("grandTotal", H.grandTotal);
		} finally {
			if (userSebelumnya != null) { sesiHttp.setAttribute("mytbmuser", userSebelumnya); }
			else { sesiHttp.removeAttribute("mytbmuser"); }
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Unduh PDF laporan (Fitur "Laporan-Laporan e-Kantin", Desktop) -- mesin RENDER PDF (iText) SAMA
	 * dipakai versi web ({@link LaporanKantinPdf#generate}), hanya dituju ke
	 * {@link ByteArrayOutputStream} lalu di-base64 di JSON (bukan langsung ke
	 * {@code HttpServletResponse}, karena panggilan ini datang lewat {@code fetch} JSON biasa dari
	 * proses render Electron, bukan navigasi/unduhan browser langsung). Sisi Desktop yang menulis file
	 * fisik (lewat dialog simpan native, pola sama dgn ekspor CSV dasbor -- lihat {@code pos:simpan-file}
	 * di {@code main.js}).
	 */
	private void prosesLaporanPdf(HttpServletRequest request, Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		HttpSession sesiHttp = request.getSession(true);
		Object userSebelumnya = sesiHttp.getAttribute("mytbmuser");
		sesiHttp.setAttribute("mytbmuser", tbmuser);
		try {
			HttpServletRequest wrapped = new ParamRequestWrapper(request, payload);
			ais.action.master.koperasi.helper.LaporanKantinUtil.Hasil H =
					ais.action.master.koperasi.helper.LaporanKantinUtil.build(wrapped);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			new LaporanKantinPdf().generate(wrapped, H, bos);
			hasil.put("status", "success");
			hasil.put("pdfBase64", java.util.Base64.getEncoder().encodeToString(bos.toByteArray()));
			hasil.put("judul", H.judul);
		} finally {
			if (userSebelumnya != null) { sesiHttp.setAttribute("mytbmuser", userSebelumnya); }
			else { sesiHttp.removeAttribute("mytbmuser"); }
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Membungkus {@link HttpServletRequest} asli supaya {@code getParameter}/{@code getParameterMap}
	 * membaca dari payload JSON ({@code PosApi}) alih-alih form/query params asli -- SATU-SATUNYA
	 * alasan class ini ada: {@code LaporanKantinUtil.build(HttpServletRequest)} dipakai bersama versi
	 * JSP (form/query params asli) dan versi Desktop (JSON body) TANPA mengubah {@code build()} itu
	 * sendiri. Method lain (session, header, dst.) diteruskan apa adanya ke request asli lewat
	 * {@link HttpServletRequestWrapper}.
	 */
	private static final class ParamRequestWrapper extends HttpServletRequestWrapper {
		private final Map<String, String> params = new HashMap<String, String>();

		ParamRequestWrapper(HttpServletRequest request, JSONObject payload) {
			super(request);
			String[] kunci = { "r", "tokoId", "tglMulai", "tglSampai", "qProduk", "qPelanggan", "perToko" };
			for (String k : kunci) {
				if (payload.has(k) && !payload.isNull(k)) {
					params.put(k, String.valueOf(payload.opt(k)));
				}
			}
		}

		@Override
		public String getParameter(String name) {
			return params.containsKey(name) ? params.get(name) : super.getParameter(name);
		}

		@Override
		public Map<String, String[]> getParameterMap() {
			Map<String, String[]> m = new HashMap<String, String[]>();
			for (Map.Entry<String, String> e : params.entrySet()) {
				m.put(e.getKey(), new String[] { e.getValue() });
			}
			return m;
		}

		@Override
		public String[] getParameterValues(String name) {
			return params.containsKey(name) ? new String[] { params.get(name) } : super.getParameterValues(name);
		}
	}

	/**
	 * <h3>Fitur "Alih Bahasa" (Desktop) -- jembatan menuju kamus {@code Common.getBahasaConfig}.</h3>
	 *
	 * <p>{@code Common.getBahasaConfig(text)} (dipakai di SELURUH JSP/ZK utk terjemahan) SELALU
	 * membaca bahasa AMBIEN sesi ({@code HttpSession} atribut {@code "current_lang"}, lihat
	 * {@code Common.currentLang()}) -- tidak ada overload yang menerima kode bahasa eksplisit. Desktop
	 * TIDAK punya sesi ambien semacam itu (tiap layar bisa memakai bahasa berbeda dari yang dipilih
	 * kasir kapan saja, tidak terikat 1 sesi HTTP), jadi method ini MENITIPKAN kode bahasa yang diminta
	 * ke atribut session tsb SELAMA loop penerjemahan berlangsung, memanggil
	 * {@code Common.getBahasaConfig} apa adanya (reuse 100% mesin kamus/auto-insert/cache yang sama
	 * dipakai web -- BUKAN kamus terpisah utk Desktop), lalu memulihkan nilai session sebelumnya di
	 * {@code finally}.</p>
	 *
	 * <p><b>Payload:</b> {@code {lang: "id"|"en"|"ar"|"zh", teks: ["Kasir", "Ringkasan", ...]}} --
	 * daftar string SUMBER (Bahasa Indonesia asli, dipakai sbg KUNCI kamus, pola yg SAMA dgn
	 * {@code Common.getBahasaConfig("teks indonesia")} di JSP/ZK). Balasan: {@code {status:"success",
	 * kamus:{"Kasir":"Cashier", ...}}} -- Desktop meng-cache peta ini per-bahasa di
	 * {@code localStorage} supaya panggilan ini cukup sekali per bahasa per sesi aplikasi (bukan tiap
	 * ganti layar).</p>
	 *
	 * <p><b>Performa:</b> kunci yang BELUM pernah diterjemahkan tersimpan on-the-fly ke tabel
	 * {@code LabelBahasa} (satu simpan+commit per kunci baru) -- lambat HANYA di panggilan PERTAMA
	 * (ratusan kunci baru sekaligus); panggilan berikutnya sudah dari cache in-memory
	 * ({@code MemoryDbUtil}). Terjemahan EN/AR/ZH kunci baru memakai kamus statis lokal
	 * ({@code KamusBahasaInternal}, TANPA panggilan jaringan) -- aman dipanggil sinkron dari thread
	 * request ini; upgrade AI (Ollama) opsional berjalan di thread LATAR terpisah, tidak menahan
	 * balasan permintaan ini.</p>
	 */
	private void prosesI18nKamus(HttpServletRequest request, JSONObject payload, JSONObject hasil) throws Exception {
		String kodeBahasa = payload.optString("lang", "id");
		String bahasaTarget;
		if ("en".equalsIgnoreCase(kodeBahasa)) { bahasaTarget = Tbmuser.ENGLISH; }
		else if ("ar".equalsIgnoreCase(kodeBahasa)) { bahasaTarget = Tbmuser.ARAB; }
		else if ("zh".equalsIgnoreCase(kodeBahasa)) { bahasaTarget = Tbmuser.MANDARIN; }
		else { bahasaTarget = Tbmuser.INDONESIA; }

		JSONArray teks = payload.optJSONArray("teks");
		if (teks == null) { teks = new JSONArray(); }

		HttpSession sesiHttp = request.getSession(true);
		Object bahasaSebelumnya = sesiHttp.getAttribute("current_lang");
		sesiHttp.setAttribute("current_lang", bahasaTarget);
		try {
			JSONObject kamus = new JSONObject();
			for (int i = 0; i < teks.length(); i++) {
				String asli = teks.optString(i, "");
				if (asli.isEmpty()) { continue; }
				try {
					kamus.put(asli, Common.getBahasaConfig(asli));
				} catch (Exception eSatu) {
					kamus.put(asli, asli);
				}
			}
			hasil.put("status", "success");
			hasil.put("lang", kodeBahasa);
			hasil.put("kamus", kamus);
		} finally {
			if (bahasaSebelumnya != null) { sesiHttp.setAttribute("current_lang", bahasaSebelumnya); }
			else { sesiHttp.removeAttribute("current_lang"); }
		}
	}

	private static JSONObject bacaJsonBody(HttpServletRequest request) {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = request.getReader();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PosApi.bacaJsonBody"); }
		finally {
			if (reader != null) {
				try { reader.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) PosApi.bacaJsonBody.close"); }
			}
		}

		String teks = sb.toString().trim();
		if (teks.isEmpty()) return new JSONObject();
		try {
			return new JSONObject(teks);
		} catch (Exception e) {
			return new JSONObject();
		}
	}

	private static void tulisJson(HttpServletResponse response, JSONObject hasil) throws IOException {
		PrintWriter writer = response.getWriter();
		writer.print(hasil.toString());
		writer.flush();
	}

	/** {@code toString()} null-safe -- dipakai membangun JSON balasan supaya tidak pernah menulis literal "null". */
	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}


