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
			} else if ("sesi_kas_buka".equals(action)) {
				KantinHelper.sesiKasBuka(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "sesi_kas");
			} else if ("sesi_kas_tutup".equals(action)) {
				KantinHelper.sesiKasTutup(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "sesi_kas");
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
			} else if ("akun_list".equals(action)) {
				ais.action.servlet.api.JenisProdukApiHelper.akunList(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "akun_list");
			} else if ("mutasi_tabungan_list".equals(action)) {
				KantinHelper.mutasiTabunganList(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "mutasi_tabungan_list");
			} else if ("mutasi_hutang_list".equals(action)) {
				KantinHelper.mutasiHutangList(payload, hasil);
				normalisasiStatusKantinHelper(hasil, "mutasi_hutang_list");
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
			} else if ("kulakan_faktur_detail".equals(action)) {
				KantinHelper.kulakanFakturDetail(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "kulakan_faktur_detail");
			} else if ("penyedia_list".equals(action)) {
				KantinHelper.penyediaList(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "penyedia_list");
			} else if ("penyedia_simpan".equals(action)) {
				KantinHelper.penyediaSimpan(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "penyedia_simpan");
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
			} else if ("diskon_evaluasi".equals(action)) {
				KantinHelper.diskonEvaluasi(tbmuser, payload, hasil);
				normalisasiStatusKantinHelper(hasil, "diskon_evaluasi");
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
			} else if ("laporan_jalankan".equals(action)) {
				prosesLaporanJalankan(request, tbmuser, payload, hasil);
			} else if ("laporan_pdf".equals(action)) {
				prosesLaporanPdf(request, tbmuser, payload, hasil);
			} else if ("detail_transaksi".equals(action)) {
				prosesDetailTransaksi(tbmuser, payload, hasil);
			} else if ("laporan_order_list".equals(action)) {
				prosesLaporanOrderList(tbmuser, payload, hasil);
			} else if ("laporan_sesi_list".equals(action)) {
				prosesLaporanSesiList(tbmuser, payload, hasil);
			} else if ("laporan_payment_list".equals(action)) {
				prosesLaporanPaymentList(tbmuser, payload, hasil);
			} else if (!prosesAksiTambahan(action, tbmuser, payload, hasil, request, response)) {
				hasil.put("status", "error");
				hasil.put("message", "Aksi tidak dikenal: " + action);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit PosApi.proses");
			hasil = new JSONObject();
			try {
				hasil.put("status", "error");
				hasil.put("message", "Terjadi kesalahan pada sistem. Silakan hubungi administrator.");
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

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			// Kategori diturunkan dari produk yang BENAR-BENAR ditampilkan (grup per jenisProduk),
			// BUKAN dari seluruh baris JenisProduk di sistem -- sebelumnya query kategori terpisah
			// (Criteria polos tanpa filter) sehingga menampilkan kategori yang tidak dipakai produk
			// mana pun di toko ini (mis. kategori milik toko lain, atau kategori tanpa produk aktif
			// sama sekali) -- membingungkan kasir krn pill kategori itu bila diklik selalu kosong.
			// LinkedHashMap sekadar dedup by id; urutan akhir tetap diseragamkan alfabetis di bawah.
			java.util.Map<Long, String> petaKategori = new java.util.LinkedHashMap<Long, String>();

			JSONArray produkArr = new JSONArray();
			for (Object o : PriceTagUtil.listProduk(session, tokoId, keyword, semuaTokoDiminta, adminGlobal, jenisItemFilter)) {
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
				// Gap-closure "Cetak PDF" (layar Produk) -- perlu Satuan/UOM & Pemasok per baris utk
				// cetakan detail, field ini SEBELUMNYA tak pernah dikirim aksi katalog sama sekali.
				j.put("satuanNama", p.getSatuan() == null ? "" : str(p.getSatuan().getNama()));
				j.put("pemasokNama", p.getPemasok() == null ? "" : str(p.getPemasok().getNama()));
				j.put("gambarUrl", Boolean.TRUE.equals(p.getAdaFileGambar()) ? buildUrlGambarProduk(request, p.getId()) : JSONObject.NULL);
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
		// WAJIB PERMANEN utk SEMUA toko (2026-08-11) -- lihat catatan sama di KantinHelper.bayar()/
		// PosKantinAction.onBayar(); field ini TETAP dikirim (bukan dihapus) supaya klien lama/baru
		// selalu tahu utk menampilkan gerbang Buka Kas proaktif, bukan cuma menemukan gerbangnya
		// setelah percobaan checkout gagal.
		hasil.put("wajibSesiKas", true);
		hasil.put("cegahOversell", Common.bolehKonfigurasi(Konfigurasi.KANTIN_POS_CEGAH_OVERSELL, Konfigurasi.TIDAK_AKTIF));
		hasil.put("isAdmin", toko == null);
		hasil.put("tokoId", toko == null ? JSONObject.NULL : toko.getId());
		hasil.put("tokoNama", toko == null ? "" : str(toko.getNama()));
		hasil.put("tokoAktifId", toko == null ? JSONObject.NULL : toko.getId());
		hasil.put("daftarToko", daftarTokoJson);
		hasil.put("multiToko", daftarTokoJson.length() > 1);
		// Ucapan penutup struk & Layar Pelanggan (fitur "Konfigurasi", per-toko) -- toko==null (admin
		// global, tanpa toko) memakai teks formal default yg sama, lihat Toko.PESAN_TERIMA_KASIH_DEFAULT.
		hasil.put("pesanTerimaKasih", toko == null ? Toko.PESAN_TERIMA_KASIH_DEFAULT : str(toko.getPesanTerimaKasih()));
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
		// Fitur "Hak Akses Menu per Akun" (gap-closure Toko Al-Bahjah). Admin global (pedagang==null, TIDAK terikat satu toko) SELALU
		// akses semua menu -- flag akses per-menu HANYA berlaku utk akun Pedagang toko biasa. Gerbang
		// SEBENARNYA (sidebar disembunyikan) ada di klien; ini murni sumber kebenarannya dari server
		// supaya admin bisa mengatur dari layar Konfigurasi/web tanpa klien bisa memalsukannya sendiri
		// (klien tak pernah punya cara menulis field ini kecuali lewat aksi pedagang_ubah/akun_tambah).
		// Sumber tunggal: JSON konsolidasi Tbmrole.ebisnisMenu (bukan lagi 26 kolom Boolean terpisah --
		// lihat ais.common.EbisnisMenuKatalog). roleAksesMenu==null (admin global/tanpa role) -> semua
		// menu default true (perilaku sama spt sebelumnya, EbisnisMenuKatalog.urai(null) sudah begitu).
		org.json.JSONObject ebisnisMenuRole = ais.common.EbisnisMenuKatalog
				.urai(roleAksesMenu == null ? null : roleAksesMenu.getEbisnisMenu());
		org.json.JSONObject menuTersimpan = ebisnisMenuRole.getJSONObject("menu");
		JSONObject aksesMenu = new JSONObject();
		aksesMenu.put("supervisor", ebisnisMenuRole.optBoolean("supervisor", false));
		aksesMenu.put("kasir", menuTersimpan.optBoolean("kasir", true));
		aksesMenu.put("ringkasan", menuTersimpan.optBoolean("ringkasan", true));
		aksesMenu.put("pesanan", menuTersimpan.optBoolean("pesanan", true));
		aksesMenu.put("anggota", menuTersimpan.optBoolean("anggota", true));
		aksesMenu.put("produk", menuTersimpan.optBoolean("produk", true));
		aksesMenu.put("barang", menuTersimpan.optBoolean("produk", true));
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
			aksesMenu.put(kunciVarian, menuTersimpan.optBoolean(kunciVarian, false));
		}
		hasil.put("aksesMenu", aksesMenu);
		JSONObject aksesMenuCrud = new JSONObject();
		JSONObject crudTersimpan = ebisnisMenuRole.optJSONObject("crud");
		for (String kunciCrud : ais.common.EbisnisMenuKatalog.KUNCI_CRUD) {
			JSONObject baris = new JSONObject();
			JSONObject barisTersimpan = crudTersimpan == null ? null : crudTersimpan.optJSONObject(kunciCrud);
			for (String aksiCrud : ais.common.EbisnisMenuKatalog.AKSI_CRUD) {
				baris.put(aksiCrud, barisTersimpan == null || barisTersimpan.optBoolean(aksiCrud, true));
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
		JSONObject roleEbisnisMenu = ais.common.EbisnisMenuKatalog.urai(role == null ? null : role.getEbisnisMenu());
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

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Criteria c = session.createCriteria(DraftPembelianAnggotaKoperasi.class).addOrder(Order.desc("id"));
			boolean butuhAliasToko = tokoId != null || !payload.optString("pedagang", "").trim().isEmpty();
			if (butuhAliasToko) {
				c.createAlias("toko", "tk");
			}
			if (tokoId != null) {
				c.add(Restrictions.eq("tk.id", tokoId));
			}

			// Filter tambahan (gap-closure -- padanan filter Mulai/Akhir/Kode/Pembeli/Pedagang di JSP
			// _draft_pesanan_anggota.jsp) -- SEMUA opsional, TIDAK mengubah perilaku lama saat kosong
			// (aplikasi lama yg belum di-update tetap dapat 100 baris terbaru tanpa filter apa pun).
			String sejak = payload.optString("sejak", "").trim();
			String sampai = payload.optString("sampai", "").trim();
			if (!sejak.isEmpty() || !sampai.isEmpty()) {
				java.text.SimpleDateFormat fmtTgl = new java.text.SimpleDateFormat("yyyy-MM-dd");
				if (!sejak.isEmpty()) {
					c.add(Restrictions.ge("tanggalPembayaran", fmtTgl.parse(sejak)));
				}
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
			if (!kode.isEmpty()) {
				c.add(Restrictions.ilike("kode", kode, org.hibernate.criterion.MatchMode.ANYWHERE));
			}
			String pembeli = payload.optString("pembeli", "").trim();
			if (!pembeli.isEmpty()) {
				c.createAlias("anggotaKoperasi", "ak").add(Restrictions.ilike("ak.nama", pembeli, org.hibernate.criterion.MatchMode.ANYWHERE));
			}
			String pedagang = payload.optString("pedagang", "").trim();
			if (!pedagang.isEmpty()) {
				c.add(Restrictions.ilike("tk.nama", pedagang, org.hibernate.criterion.MatchMode.ANYWHERE));
			}
			if (payload.optBoolean("hanya_belum_lunas", false)) {
				c.add(Restrictions.isNull("lunas"));
			}

			int batas = payload.optInt("limit", 100);
			if (batas <= 0 || batas > 500) {
				batas = 100;
			}
			c.setMaxResults(batas);

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
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
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
		if ("detail_transaksi".equals(action)) {
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
		if (!bolehSupervisorAtauAdmin(tbmuser) && !bolehAksiCrudPesanan(tbmuser, "reject")) {
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
		try {
			DraftPembelianAnggotaKoperasi draft = (DraftPembelianAnggotaKoperasi) session
					.get(DraftPembelianAnggotaKoperasi.class, draftId);
			if (draft == null) {
				hasil.put("status", "error");
				hasil.put("message", "Pesanan tidak ditemukan (mungkin sudah dibatalkan sebelumnya).");
				return;
			}
			if (draft.getLunas() != null) {
				hasil.put("status", "error");
				hasil.put("message", "Pesanan yang sudah lunas tidak bisa dibatalkan dari sini.");
				return;
			}

			System.out.println("[BATAL-PESANAN] draftId=" + draftId + ", kode=" + draft.getKode()
					+ ", total=" + draft.getTotalBiaya() + ", oleh=" + (tbmuser == null ? "?" : tbmuser.getUserId())
					+ ", alasan=" + alasan);

			session.beginTransaction();
			Criteria ci = session.createCriteria(DraftPembelian.class)
					.add(Restrictions.eq("draftPembelianAnggotaKoperasi", draft));
			for (Object oi : ci.list()) {
				session.delete(oi);
			}
			session.delete(draft);
			session.getTransaction().commit();

			hasil.put("status", "success");
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
	 * ⚠️ Menyeragamkan balasan {@link KantinHelper#bayar}/{@code draft_bayar}/{@code checkBayar}/
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
		String pesan;
		String kode;

		if (asli.length() == 0) {
			kode = "DATA_TIDAK_LENGKAP";
			pesan = "Data transaksi yang dikirim tidak lengkap atau toko tidak valid -- transaksi TIDAK diproses sama sekali (tidak ada perubahan di database).";
		} else if ("checkBayar".equals(konteks) && "01".equals(asli)) {
			kode = "TIDAK_DITEMUKAN";
			pesan = desc.length() > 0 ? desc : "Pembayaran belum terkonfirmasi.";
		} else {
			String descLower = desc.toLowerCase();
			if (descLower.indexOf("duplicate key") >= 0 || descLower.indexOf("unique constraint") >= 0) {
				kode = "DUPLIKAT_KODE_TRANSAKSI";
				pesan = "Kode transaksi ini SUDAH tercatat sebelumnya di server -- kemungkinan besar transaksi ini SUDAH BERHASIL pada percobaan sebelumnya.";
			} else {
				kode = "SERVER_ERROR";
				pesan = desc.length() > 0 ? desc : ("Transaksi ditolak server (kode status internal: " + asli + ").");
			}
		}

		hasil.put("status", "error");
		hasil.put("message", pesan);
		hasil.put("kode", kode);
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
	private static String petaIntervalPeriode(String periode) {
		if ("harian".equals(periode)) return "DATE(a.waktu) = CURRENT_DATE";
		if ("mingguan".equals(periode)) return "DATE(a.waktu) >= CURRENT_DATE - INTERVAL '7 days'";
		if ("semester".equals(periode)) return "DATE(a.waktu) >= CURRENT_DATE - INTERVAL '6 months'";
		if ("tahunan".equals(periode)) return "DATE(a.waktu) >= CURRENT_DATE - INTERVAL '1 year'";
		return "DATE(a.waktu) >= CURRENT_DATE - INTERVAL '1 month'"; // bulanan, default
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
			java.sql.PreparedStatement psKpi = conn.prepareStatement(
					"SELECT "
							+ "COALESCE(SUM(CASE WHEN DATE(waktu)=CURRENT_DATE THEN total ELSE 0 END),0), "
							+ "COUNT(DISTINCT CASE WHEN DATE(waktu)=CURRENT_DATE THEN id END), "
							+ "COALESCE(SUM(CASE WHEN DATE(waktu)>=DATE_TRUNC('week',CURRENT_DATE) THEN total ELSE 0 END),0), "
							+ "COUNT(DISTINCT CASE WHEN DATE(waktu)>=DATE_TRUNC('week',CURRENT_DATE) THEN id END), "
							+ "COALESCE(SUM(CASE WHEN DATE(waktu)>=DATE_TRUNC('month',CURRENT_DATE) THEN total ELSE 0 END),0), "
							+ "COUNT(DISTINCT CASE WHEN DATE(waktu)>=DATE_TRUNC('month',CURRENT_DATE) THEN id END), "
							+ "COALESCE(SUM(CASE WHEN waktu>=NOW()-INTERVAL '6 months' THEN total ELSE 0 END),0), "
							+ "COUNT(DISTINCT CASE WHEN waktu>=NOW()-INTERVAL '6 months' THEN id END) "
							+ "FROM koperasi.pembelian WHERE aktif = true" + kondisiToko);
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

			// ---- Tren transaksi (harian/mingguan/bulanan) ----
			String intervalSql, groupSql, labelFmt;
			if ("bulanan".equals(periodeTren)) { intervalSql = "12 months"; groupSql = "DATE_TRUNC('month', waktu)"; labelFmt = "Mon YYYY"; }
			else if ("mingguan".equals(periodeTren)) { intervalSql = "8 weeks"; groupSql = "DATE_TRUNC('week', waktu)"; labelFmt = "DD Mon"; }
			else { intervalSql = "14 days"; groupSql = "DATE(waktu)"; labelFmt = "DD Mon"; }
			java.sql.PreparedStatement psTren = conn.prepareStatement(
					"SELECT TO_CHAR(" + groupSql + ",'" + labelFmt + "') AS lbl, COUNT(*) FROM koperasi.pembelian "
							+ "WHERE waktu >= NOW() - INTERVAL '" + intervalSql + "'" + kondisiToko + " GROUP BY " + groupSql + " ORDER BY " + groupSql + " ASC");
			if (!semuaToko) psTren.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsTren = psTren.executeQuery();
			JSONArray tren = new JSONArray();
			while (rsTren.next()) {
				JSONObject t = new JSONObject();
				t.put("label", str(rsTren.getString(1)));
				t.put("jumlah", rsTren.getLong(2));
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
				whereChart.append(" AND p.waktu >= NOW() - INTERVAL '30 days'");
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
					"SELECT COALESCE(jp.nama,'Lainnya') lbl, COALESCE(SUM(p.total),0) nilai FROM koperasi.pembelian p "
							+ "LEFT JOIN koperasi.produk c ON c.id = p.produk LEFT JOIN koperasi.jenis_produk jp ON jp.id = c.jenis_produk "
							+ "WHERE " + kondisiChart + " GROUP BY 1 ORDER BY 2 DESC LIMIT 8");
			idx = 1;
			for (Object p : paramsChart) ikatParam(psKategori, idx++, p);
			java.sql.ResultSet rsKategori = psKategori.executeQuery();
			while (rsKategori.next()) {
				JSONObject o = new JSONObject();
				o.put("label", str(rsKategori.getString(1)));
				o.put("nilai", rsKategori.getDouble(2));
				omzetKategori.put(o);
			}
			rsKategori.close(); psKategori.close();

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

			hasil.put("status", "success");
			hasil.put("semuaToko", semuaToko);
			hasil.put("kpi", kpi);
			hasil.put("tren", tren);
			hasil.put("metodeBayar", metodeBayar);
			hasil.put("omzetKategori", omzetKategori);
			hasil.put("jamSibuk", jamSibuk);
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
							+ "COALESCE(t.nama,''), COALESCE(NULLIF(t.pesan_terima_kasih,''), ?) "
							+ "FROM koperasi.pembelian_anggota_koperasi pak LEFT JOIN koperasi.toko t ON pak.toko = t.id "
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
			}
			rsHeader.close(); psHeader.close();

			if (punyaHeaderKelompok) {
				// Gap-closure "Produk Ekstra" -- ORDER BY COALESCE(induk_id,id),id mengelompokkan baris
				// induk lalu ekstra-nya tepat sesudahnya (BUKAN urutan id mentah, yang bisa salah urut
				// krn baris ekstra bisa punya id lebih kecil dari baris induk LAIN yg tak terkait) --
				// klien (struk/riwayat) cukup render indent saat indukId != null, tanpa perlu grouping
				// sendiri.
				java.sql.PreparedStatement psItem = conn.prepareStatement(
						"SELECT COALESCE(pr.nama,''), p.qty, COALESCE(p.hargasatuan, (p.total / NULLIF(p.qty,0)), 0), COALESCE(p.diskon,0), COALESCE(p.cashback,0), p.produk, p.induk_id "
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
	 * entity) -- keduanya dicocokkan HANYA lewat identitas kasir ({@code oleh}/{@code olehid}) +
	 * rentang waktu {@code waktubuka..waktututup}, PERSIS pola yg sudah dipakai
	 * {@code SesiKasUtil.hitungPenjualan()} utk menghitung total tunai/non-tunai per sesi saat tutup
	 * kas. Query di bawah mereproduksi pencocokan yg SAMA lewat {@code LEFT JOIN LATERAL}, supaya
	 * "sesi ke berapa" bisa dihitung per-baris tanpa mengubah skema database sama sekali.</p>
	 */
	private void prosesLaporanOrderList(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoId(tbmuser, payload);
		if (tokoId == null) { hasil.put("status", "error"); hasil.put("message", "Toko tidak diketahui utk akun ini."); return; }

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			String tokoKode = toko == null || toko.getKode() == null || toko.getKode().trim().isEmpty()
					? ("toko" + tokoId) : toko.getKode().trim();

			JSONObject hasilQuery = daftarOrderDenganSesi(session, tokoId, tokoKode, payload);
			hasil.put("status", "success");
			hasil.put("data", hasilQuery.getJSONArray("data"));
			hasil.put("total", hasilQuery.getLong("total"));
			hasil.put("page", hasilQuery.getInt("page"));
			hasil.put("pageSize", hasilQuery.getInt("pageSize"));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
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
		int page = Math.max(1, payload.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(5, payload.optInt("pageSize", 10)));
		int offset = (page - 1) * pageSize;

		java.sql.Connection conn = session.connection();

		StringBuilder whereTrx = new StringBuilder("a.toko = ?");
		java.util.List<Object> paramsTrx = new java.util.ArrayList<Object>();
		paramsTrx.add(tokoId);
		if (tglMulai.length() > 0) { whereTrx.append(" AND DATE(a.waktu) >= ?"); paramsTrx.add(tglMulai); }
		if (tglSampai.length() > 0) { whereTrx.append(" AND DATE(a.waktu) <= ?"); paramsTrx.add(tglSampai); }
		String havingCari = cariPembeli.length() > 0 ? " HAVING MAX(a.member) ILIKE ? OR COALESCE(MAX(pak.kode),'') ILIKE ?" : "";

		// ---- Total baris (utk paginasi) ----
		java.sql.PreparedStatement psCount = conn.prepareStatement(
				"SELECT COUNT(*) FROM (SELECT COALESCE(a.pembelian_anggota_koperasi, a.id) FROM koperasi.pembelian a "
						+ "LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON pak.id = a.pembelian_anggota_koperasi WHERE "
						+ whereTrx + " GROUP BY 1" + havingCari + ") x");
		int idx = 1;
		for (Object p : paramsTrx) ikatParam(psCount, idx++, p);
		if (cariPembeli.length() > 0) {
			String kw = "%" + cariPembeli + "%";
			psCount.setString(idx++, kw);
			psCount.setString(idx++, kw);
		}
		java.sql.ResultSet rsCount = psCount.executeQuery();
		long total = rsCount.next() ? rsCount.getLong(1) : 0;
		rsCount.close(); psCount.close();

		// ---- Data (dikelompokkan per transaksi, dicocokkan ke sesi via LATERAL, dipaginasi) ----
		String sql = "WITH sesi_bertingkat AS ("
				+ "  SELECT id, oleh, olehid, waktubuka, waktututup,"
				+ "         ROW_NUMBER() OVER (ORDER BY waktubuka) AS nomor_sesi"
				+ "  FROM koperasi.sesi_kas_kasir WHERE toko = ?"
				+ "), order_dasar AS ("
				+ "  SELECT COALESCE(a.pembelian_anggota_koperasi, a.id) AS id_transaksi,"
				+ "         MAX(pak.kode) AS kode_nota, MAX(a.waktu) AS waktu, MAX(a.member) AS pembeli,"
				// Gap-closure "kolom KASIR selalu 'external_update'": a.oleh/a.olehid adalah metadata
				// audit generik yang SELALU ditimpa AuditTimestampInterceptor ke "external_update" utk
				// permintaan lewat PosApi (tak ada sesi HttpServletRequest standar) -- lihat JavaDoc
				// PembelianAnggotaKoperasi.getKasirLoginNama(). Field baru itu (pak.kasir_login_nama)
				// dipakai lebih dulu; fallback ke a.oleh HANYA utk baris transaksi LAMA (sebelum kolom
				// ini ada) supaya histori lama tidak mendadak kosong.
				+ "         COALESCE(MAX(pak.kasir_login_nama), MAX(a.oleh)) AS kasir,"
				+ "         MAX(a.olehid) AS kasir_id, MAX(pak.nama_mesin) AS nama_mesin, MAX(a.carabayar) AS metode,"
				+ "         SUM(a.qty) AS qty, SUM(a.total) AS subtotal_barang,"
				+ "         COALESCE(MAX(pak.total_diskon), SUM(COALESCE(a.diskon,0))) AS total_diskon,"
				+ "         COALESCE(MAX(pak.pajak),0) AS pajak,"
				+ "         COALESCE(MAX(pak.total_biaya), SUM(a.total)) AS total_biaya,"
				+ "         COALESCE(MAX(pak.bayar_tunai),0) AS bayar_tunai,"
				+ "         COALESCE(MAX(pak.bayar_non_tunai),0) AS bayar_non_tunai"
				+ "  FROM koperasi.pembelian a LEFT JOIN koperasi.pembelian_anggota_koperasi pak"
				+ "       ON pak.id = a.pembelian_anggota_koperasi"
				+ "  WHERE " + whereTrx + " GROUP BY 1" + havingCari
				+ "), order_dgn_sesi AS ("
				+ "  SELECT od.*, sb.id AS sesi_id, sb.nomor_sesi FROM order_dasar od"
				+ "  LEFT JOIN LATERAL ("
				+ "    SELECT * FROM sesi_bertingkat s"
				+ "    WHERE (s.oleh = od.kasir OR s.olehid = od.kasir_id)"
				+ "      AND od.waktu >= s.waktubuka AND od.waktu <= COALESCE(s.waktututup, NOW())"
				+ "    ORDER BY s.waktubuka DESC LIMIT 1"
				+ "  ) sb ON true"
				+ ") "
				+ "SELECT id_transaksi, kode_nota, waktu, pembeli, kasir, metode, qty, subtotal_barang,"
				+ "       total_diskon, pajak, total_biaya, bayar_tunai, bayar_non_tunai, sesi_id, nomor_sesi,"
				+ "       ROW_NUMBER() OVER (PARTITION BY sesi_id ORDER BY waktu) AS nomor_order_dalam_sesi, nama_mesin"
				+ " FROM order_dgn_sesi ORDER BY waktu DESC LIMIT ? OFFSET ?";
		java.sql.PreparedStatement psData = conn.prepareStatement(sql);
		idx = 1;
		psData.setLong(idx++, tokoId.longValue()); // sesi_bertingkat.toko
		for (Object p : paramsTrx) ikatParam(psData, idx++, p); // order_dasar WHERE (termasuk a.toko lagi)
		if (cariPembeli.length() > 0) {
			String kw = "%" + cariPembeli + "%";
			psData.setString(idx++, kw);
			psData.setString(idx++, kw);
		}
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
			o.put("bayarTunai", rs.getDouble(12));
			o.put("bayarNonTunai", rs.getDouble(13));
			rs.getLong(14); boolean adaSesi = !rs.wasNull();
			long nomorSesi = rs.getLong(15); boolean adaNomorSesi = !rs.wasNull();
			long nomorDalamSesi = rs.getLong(16);
			String sesiKode = adaSesi && adaNomorSesi ? (tokoKode + "/" + lpad(nomorSesi, 4)) : "-";
			o.put("sesiKode", sesiKode);
			o.put("nomorIdOrder", sesiKode);
			o.put("nomorNota", "Order " + tokoKode + " - " + (adaNomorSesi ? lpad(nomorSesi, 4) : "0000") + " - " + lpad(nomorDalamSesi, 3)
					+ (kodeNota.length() > 0 ? " (" + kodeNota + ")" : ""));
			String namaMesin = rs.getString(17);
			o.put("namaMesin", namaMesin == null || namaMesin.trim().isEmpty() ? JSONObject.NULL : str(namaMesin));
			arr.put(o);
		}
		rs.close(); psData.close();

		JSONObject out = new JSONObject();
		out.put("data", arr);
		out.put("total", total);
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

			String sql = "SELECT sk.id, sk.oleh, sk.waktubuka, sk.waktututup, sk.modalawal, sk.uangfisik, sk.status,"
					+ "       COALESCE((SELECT SUM(COALESCE(pak.bayar_tunai,0)) FROM koperasi.pembelian_anggota_koperasi pak"
					+ "                 WHERE pak.toko = sk.toko AND (pak.oleh = sk.oleh OR pak.olehid = sk.olehid)"
					+ "                 AND pak.tanggal_pembayaran >= sk.waktubuka AND pak.tanggal_pembayaran <= COALESCE(sk.waktututup, NOW())),0) AS total_tunai_live,"
					+ "       COALESCE((SELECT SUM(COALESCE(pak.bayar_non_tunai,0)) FROM koperasi.pembelian_anggota_koperasi pak"
					+ "                 WHERE pak.toko = sk.toko AND (pak.oleh = sk.oleh OR pak.olehid = sk.olehid)"
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

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			java.util.Map<Long, Double> peta = petaHargaPokok(conn, tokoId);

			// ---- Tren laba 14 hari (agregat qty*hpp per produk per hari) ----
			java.sql.PreparedStatement psLaba = conn.prepareStatement(
					"SELECT DATE(a.waktu), a.produk, SUM(a.total), SUM(a.qty) FROM koperasi.pembelian a "
							+ "WHERE a.toko = ? AND DATE(a.waktu) >= CURRENT_DATE - INTERVAL '14 days' GROUP BY DATE(a.waktu), a.produk ORDER BY 1");
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
							+ "WHERE pb.toko = ? AND pb.waktu >= CURRENT_DATE - INTERVAL '30 days' GROUP BY pr.nama ORDER BY 2 DESC");
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
			performaToko.put("harian", performaPeriode(conn, tokoId, "DATE(a.waktu)=CURRENT_DATE"));
			performaToko.put("mingguan", performaPeriode(conn, tokoId, "DATE(a.waktu) BETWEEN CURRENT_DATE - INTERVAL '7 days' AND CURRENT_DATE"));
			performaToko.put("bulanan", performaPeriode(conn, tokoId, "DATE(a.waktu) BETWEEN CURRENT_DATE - INTERVAL '1 month' AND CURRENT_DATE"));

			hasil.put("status", "success");
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
		String periode = payload.optString("periode", "bulanan");
		String intervalRekap = petaIntervalPeriode(periode);

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
							+ "WHERE pb.toko = ? AND pb.waktu >= CURRENT_DATE - INTERVAL '30 days' GROUP BY pr.id, pr.nama, pr.stok ORDER BY 3 DESC");
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
							+ "WHERE DATE(a.waktu) >= CURRENT_DATE - INTERVAL '1 month' AND a.toko = ? GROUP BY c.id, c.nama ORDER BY 2 DESC LIMIT 10");
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
							+ "WHERE DATE(a.waktu) >= CURRENT_DATE - INTERVAL '1 month' AND a.toko = ? GROUP BY a.carabayar ORDER BY 2 DESC");
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
							+ "WHERE a.toko = ? AND DATE(a.waktu) >= CURRENT_DATE - INTERVAL '30 days' "
							+ "AND c.id IN (SELECT produk FROM koperasi.pembelian WHERE toko = ? AND DATE(waktu) >= CURRENT_DATE - INTERVAL '30 days' "
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
							+ "WHERE toko = ? AND waktu >= CURRENT_DATE - INTERVAL '30 days'");
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
							+ "LEFT JOIN koperasi.pembelian a ON (a.produk = c.id AND DATE(a.waktu) >= CURRENT_DATE - INTERVAL '30 days') "
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
		String periode = payload.optString("periode", "bulanan");
		String intervalRekap = petaIntervalPeriode(periode);

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();

			java.sql.PreparedStatement psJam = conn.prepareStatement(
					"SELECT EXTRACT(HOUR FROM a.waktu), COUNT(a.id) FROM koperasi.pembelian a "
							+ "WHERE DATE(a.waktu) >= CURRENT_DATE - INTERVAL '1 month' AND a.toko = ? GROUP BY 1 ORDER BY 1");
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
							+ "WHERE DATE(a.waktu) >= CURRENT_DATE - INTERVAL '1 month' AND a.anggota_koperasi IS NOT NULL AND a.toko = ? "
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
