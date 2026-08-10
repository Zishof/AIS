package ais.action.servlet.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;
import ais.database.model.inventory.JenisProduk;
import ais.database.model.inventory.PemasokProduk;
import ais.database.model.inventory.Pembelian;
import ais.database.model.inventory.PengadaanProduk;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.ReturPenjualan;
import ais.database.model.inventory.SatuanProduk;
import ais.database.model.inventory.SesiStokOpname;
import ais.database.model.inventory.StokOpname;
import ais.database.model.inventory.Toko;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.DraftPembelianAnggotaKoperasi;
import ais.database.model.koperasi.JenisAnggotaKoperasi;
import ais.database.model.koperasi.KodePembayaranOnline;
import ais.database.model.koperasi.MejaKantin;
import ais.database.model.koperasi.PembelianAnggotaKoperasi;
import ais.ui.util.WaktuUtil;

/**
 * <h3>Endpoint API kasir POS Kantin/Koperasi — checkout, draft, saldo, dan konfirmasi pembayaran online.</h3>
 *
 * <p>Dipanggil dari servlet {@code ais.action.servlet.Data} (rute {@code /Data}) sebagai handler
 * untuk aksi POS yang dipicu dari front-end kasir — baik versi JSP ({@code _pos.jsp}, termasuk jalur
 * offline-first lewat {@code pos_offline_service.jsp} yang memanggil ulang {@link #bayar} untuk tiap
 * transaksi tertunda saat sinkron) maupun versi ZK ({@code PosKantinAction}, yang memanggil
 * {@link #bayar} langsung/sinkron dari {@code eksekusiBayar()}). Class ini murni kumpulan method
 * statis (bukan servlet/action sendiri) — dipilih pendekatan prosedural sederhana karena setiap method
 * di sini independen satu sama lain (tidak berbagi state instance), konsisten dengan pola helper API
 * lain di paket {@code ais.action.servlet.api}.</p>
 *
 * <h3>Method publik &amp; kapan dipakai</h3>
 * <ul>
 *   <li>{@link #bayar} — checkout FINAL: menulis {@code PembelianAnggotaKoperasi} + baris
 *       {@code pembelian}, memicu pemakaian bahan baku resep, recompute stok, sinkron stok aset
 *       (opsional), dan menyelesaikan draft asal (bila checkout ini berasal dari draft tersimpan).</li>
 *   <li>{@link #draft_bayar} — SIMPAN SEMENTARA (belum final): menulis
 *       {@code DraftPembelianAnggotaKoperasi} + baris {@code draft_pembelian} SAJA, TIDAK menyentuh
 *       stok/bahan baku/aset sama sekali (efek samping itu baru terjadi saat draft ini kelak
 *       di-{@link #bayar} — lihat parameter {@code draftPembelianAnggotaKoperasi} di sana). Dipakai
 *       fitur "Simpan Keranjang"/pesanan online yang menunggu diproses kasir.</li>
 *   <li>{@link #topup}, {@link #tabungan} — baca saldo/deposit anggota koperasi (read-only, tidak
 *       membuka session Hibernate sendiri karena {@link ais.action.master.sekolah.util.DepositHelper}
 *       yang dipanggil sudah mengelola aksesnya sendiri).</li>
 *   <li>{@link #checkBayar} — konfirmasi status pembayaran ONLINE (VA/QRIS/e-wallet) berdasarkan
 *       {@code kodeUnik} yang di-generate saat transaksi dimulai; dipoll dari front-end kasir selagi
 *       menunggu pembeli menyelesaikan pembayaran di kanal eksternal.</li>
 * </ul>
 *
 * <h3>Pola desain yang konsisten dipertahankan di seluruh method</h3>
 * <ul>
 *   <li><b>Validasi input longgar-tapi-aman:</b> setiap field JSON opsional divalidasi dengan
 *       {@code isNull()} + {@link Common#isNumber(String)} SEBELUM di-parse ke tipe numerik — field
 *       yang tidak valid diperlakukan sebagai kosong (null), TIDAK melempar exception mentah yang
 *       lolos dari respons JSON standar {@code {status, description}}.</li>
 *   <li><b>Sesi Hibernate mandiri (POLA B):</b> {@link #bayar}, {@link #draft_bayar}, dan
 *       {@link #checkBayar} membuka session sendiri lewat
 *       {@code HibernateUtil.getSessionFactory().openSession()} (BUKAN
 *       {@code HibernateUtil.currentSession()}, karena class ini dipanggil dari konteks servlet
 *       {@code /Data}, bukan siklus hidup ZK) dan SELALU menutupnya tuntas (clear → disconnect →
 *       close) di blok {@code finally} — lihat dokumentasi lengkap POLA A/B/B-JSP di
 *       {@code HibernateUtil}.</li>
 *   <li><b>Transaksi kecil terpisah, bukan satu transaksi atomik:</b> {@link #bayar} memanggil
 *       {@code session.getTransaction().begin()/commit()} berkali-kali untuk langkah berbeda (simpan
 *       lokasi baru, simpan header, dst.) alih-alih membungkus semuanya dalam satu transaksi —
 *       trade-off yang diketahui &amp; didokumentasikan (lihat JavaDoc
 *       {@link #validasiStokCukupDenganLock}), bukan kelalaian.</li>
 *   <li><b>Efek samping pasca-simpan bersifat fail-safe berlapis:</b> pemakaian bahan baku, recompute
 *       stok, dan sinkron aset masing-masing dibungkus try/catch TERPISAH di {@link #bayar} — satu
 *       efek samping gagal tidak boleh menggagalkan efek samping lain ATAU transaksi utama yang sudah
 *       tersimpan.</li>
 * </ul>
 */
public class KantinHelper {

	/**
	 * Nilai bawaan (immutable) hasil {@link #hitungTotalDiskonCashback}: total tagihan (sudah termasuk
	 * pajak awal bila dikirim), total diskon, dan total cashback yang terakumulasi dari seluruh baris
	 * item transaksi. Sekadar pembawa 3 nilai terkait erat — dibuat sebagai kelas kecil (bukan
	 * {@code double[3]}) supaya pemanggil tidak perlu mengingat urutan indeks yang mudah tertukar.
	 */
	private static final class TotalHitung {
		final double total;
		final double totalDiskon;
		final double totalCashback;

		TotalHitung(double total, double totalDiskon, double totalCashback) {
			this.total = total;
			this.totalDiskon = totalDiskon;
			this.totalCashback = totalCashback;
		}
	}

	/**
	 * Menjumlahkan total tagihan, total diskon, dan total cashback dari seluruh baris {@code transaksi}
	 * — logika yang SEBELUMNYA ditulis dua kali secara identik (karakter demi karakter) di
	 * {@link #bayar} dan {@link #draft_bayar}, kini disatukan di sini supaya perubahan rumus
	 * (mis. menambah komponen biaya baru) cukup dilakukan sekali dan otomatis berlaku di kedua jalur
	 * checkout tanpa risiko salah satu salinan lupa diperbarui (drift).
	 *
	 * <p>{@code total} DIAWALI dari nominal pajak (PPN) di {@code jsonObject.pajak} bila dikirim POS
	 * (bukan dari nol) — nama variabel "total" di sini berarti "total tagihan akhir", bukan "total
	 * harga barang saja"; pajak ditambahkan LEBIH DULU, baru tiap baris item menambah
	 * {@code (harga &times; jumlah) − diskon} di atasnya. Baris item yang gagal diparse (mis. field
	 * numerik tidak valid) DILEWATI dan dicatat ke audit log — tidak menggagalkan seluruh perhitungan,
	 * karena satu baris keranjang yang cacat tidak boleh membatalkan transaksi kasir yang sedang
	 * berjalan di depan pembeli.</p>
	 *
	 * @param jsonObject     payload permintaan, dibaca field {@code pajak}-nya sebagai nilai awal total.
	 * @param transaksi      array baris item ({@code {harga, jumlah, diskon, cashback}}), tidak boleh
	 *                       null (pemanggil sudah memastikan ini lewat {@code jsonObject.getJSONArray}).
	 * @param auditTagSuffix akhiran label unik untuk {@code ErrorAuditUtil} agar log baris gagal parse
	 *                       bisa dibedakan asalnya (dipanggil dari {@link #bayar} atau
	 *                       {@link #draft_bayar}).
	 * @return {@link TotalHitung} berisi total/totalDiskon/totalCashback yang terakumulasi.
	 * @throws Exception merambat HANYA dari pembacaan {@code jsonObject.pajak} sebelum loop (mis.
	 *         {@code JSONException} bila field itu ada tapi tipenya tidak bisa dibaca) — kegagalan di
	 *         DALAM loop per baris item TIDAK merambat (ditangkap &amp; dicatat per baris, lihat di
	 *         atas), konsisten dengan perilaku method aslinya sebelum diekstrak.
	 */
	private static TotalHitung hitungTotalDiskonCashback(JSONObject jsonObject, JSONArray transaksi,
			String auditTagSuffix) throws Exception {
		double total = jsonObject.isNull("pajak") ? 0.0
				: Math.max(0.0, Double.parseDouble((jsonObject.get("pajak") + "").trim()));
		double totalDiskon = 0.0;
		double totalCashback = 0.0;
		for (int i = 0; i < transaksi.length(); i++) {
			try {
				JSONObject objectTransaksi = transaksi.getJSONObject(i);
				double hargaBarang = objectTransaksi.isNull("harga") ? 1.0
						: Double.parseDouble((objectTransaksi.get("harga") + "").trim());
				double jumlahBarang = objectTransaksi.isNull("jumlah") ? 1.0
						: Double.parseDouble((objectTransaksi.get("jumlah") + "").trim());
				double diskonBarang = objectTransaksi.isNull("diskon") ? 0.0
						: Double.parseDouble((objectTransaksi.get("diskon") + "").trim());
				double cashbackBarang = objectTransaksi.isNull("cashback") ? 0.0
						: Double.parseDouble((objectTransaksi.get("cashback") + "").trim());
				total += (hargaBarang * jumlahBarang) - diskonBarang;
				totalDiskon += diskonBarang;
				totalCashback += cashbackBarang;
			} catch (Exception e) {
				e.printStackTrace();
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit src/ais/action/servlet/api/KantinHelper.java:" + auditTagSuffix);
			}
		}
		return new TotalHitung(total, totalDiskon, totalCashback);
	}

	private static boolean dimintaLangsungTerlayani(JSONObject jsonObject) {
		return jsonObject.optBoolean("terlayani", false)
				|| jsonObject.optBoolean("langsungTerlayani", false)
				|| jsonObject.optBoolean("statusTerlayani", false)
				|| jsonObject.optBoolean("langsungDilayani", false)
				|| jsonObject.optBoolean("sudahTerlayani", false)
				|| jsonObject.optBoolean("dilayani", false)
				|| "TERLAYANI".equalsIgnoreCase(jsonObject.optString("statusPelayanan", ""));
	}

	@SuppressWarnings("unchecked")
	private static void tandaiRincianTerlayani(Session session,
			PembelianAnggotaKoperasi pembelianAnggotaKoperasi) {
		if (pembelianAnggotaKoperasi == null || pembelianAnggotaKoperasi.getId() == null) {
			return;
		}
		List<Pembelian> daftarPembelian = session.createCriteria(Pembelian.class)
				.add(Restrictions.eq("pembelianAnggotaKoperasi", pembelianAnggotaKoperasi)).list();
		if (daftarPembelian == null || daftarPembelian.isEmpty()) {
			return;
		}
		session.getTransaction().begin();
		for (Pembelian pembelian : daftarPembelian) {
			pembelian.setTerlayani(Boolean.TRUE);
			session.update(pembelian);
		}
		session.getTransaction().commit();
	}

	/**
	 * <h3>Checkout final POS Kantin: simpan transaksi penjualan sekaligus seluruh efek sampingnya.</h3>
	 *
	 * <p>Method inti alur kasir. Untuk satu payload {@code jsonObject} berisi keranjang belanja
	 * ({@code transaksi}), metode pembayaran, dan info toko/pembeli, method ini secara berurutan:</p>
	 * <ol>
	 *   <li>Memvalidasi field wajib (idToko, waktu, transaksi, caraBayar) dan menolak diam-diam
	 *       (tidak melakukan apa pun) bila salah satu tidak ada — pemanggil di front-end sudah
	 *       memastikan field ini terisi sebelum mengirim, jadi ini adalah lapisan pertahanan kedua,
	 *       bukan validasi utama.</li>
	 *   <li>Mengecek kecukupan stok server-side (lihat {@link #validasiStokCukupDenganLock}) — HANYA
	 *       untuk keperluan audit log sejak 2026-07-20 (tidak lagi memblokir transaksi).</li>
	 *   <li>Menghitung total tagihan/diskon/cashback lewat {@link #hitungTotalDiskonCashback}.</li>
	 *   <li>Menyimpan/memperbarui baris {@code Lokasi} toko (dibuat otomatis bila toko ini belum pernah
	 *       punya lokasi aktif — happens-once per toko, transparan bagi kasir).</li>
	 *   <li>Menyimpan header {@code PembelianAnggotaKoperasi} — TIGA kemungkinan jalur: (a) MENYELESAIKAN
	 *       draft yang sudah pernah dibayar-lunas-kan sebelumnya (baris {@code draftPembelianAnggotaKoperasi.getLunas()}
	 *       sudah ada), (b) memperbarui transaksi existing berdasarkan {@code id} yang dikirim, atau
	 *       (c) membuat transaksi baru sepenuhnya.</li>
	 *   <li>Menulis ulang baris {@code koperasi.pembelian} (hapus baris lama milik header ini, lalu
	 *       tulis ulang lewat {@code PembelianAnggotaKoperasi.simpanRinci}) — pola hapus-lalu-tulis-ulang
	 *       ini membuat method aman dipanggil ulang untuk transaksi {@code id} yang sama (idempoten
	 *       dari sisi baris item, walau bukan dari sisi efek samping stok/bahan-baku/aset di bawah).</li>
	 *   <li>Memicu TIGA efek samping fail-safe independen: pemakaian bahan baku resep
	 *       ({@link ais.action.master.inventory.BahanBakuUtil#konsumsiBahanBaku}) + recompute stok
	 *       bahan baku yang terpakai; recompute stok produk yang DIJUAL LANGSUNG (Fase 1, menutup celah
	 *       lama di mana hanya bahan baku yang direcompute); dan sinkron stok keluar ke modul Aset
	 *       ({@link ais.action.master.inventory.KantinAssetSyncUtil#konsumsiPenjualanKeAset}, di balik
	 *       gerbang konfigurasi, default mati).</li>
	 *   <li>Menandai draft asal (bila ada) sebagai {@code lunas} — menautkannya ke header yang baru
	 *       saja disimpan, sehingga UI "Keranjang Tertahan" tahu draft ini sudah selesai diproses.</li>
	 * </ol>
	 *
	 * <p><b>Kompatibilitas jalur offline-first:</b> method ini adalah target akhir baik dari checkout
	 * ONLINE langsung (kasir terhubung internet) MAUPUN dari flush antrian OFFLINE
	 * ({@code pos_offline_service.jsp} memanggil method ini untuk tiap transaksi yang sempat tertunda
	 * di IndexedDB perangkat kasir) — {@code kodeUnik} yang dikirim klien menjamin idempotensi lintas
	 * kedua jalur (percobaan kirim ulang transaksi yang sama tidak menggandakan baris).</p>
	 *
	 * @param tbmuser    pengguna (kasir) yang login, dicatat sebagai pemilik transaksi.
	 * @param jsonObject payload permintaan lengkap dari front-end kasir (JSP maupun ZK).
	 * @param hasil      objek keluaran yang DIISI oleh method ini ({@code status}, {@code description},
	 *                   {@code data}, {@code pembelianAnggotaKoperasi}) — bukan nilai kembali biasa,
	 *                   mengikuti konvensi seluruh servlet API {@code /Data} di aplikasi ini.
	 * @throws Exception hanya merambat untuk kegagalan di LUAR blok try/catch internal (mis. parsing
	 *                    field wajib yang gagal sebelum session dibuka) — kegagalan SETELAH session
	 *                    terbuka selalu ditangkap dan diterjemahkan menjadi {@code hasil.status = "91"},
	 *                    tidak pernah merambat sebagai exception mentah ke pemanggil.
	 */
	/**
	 * Hasil parse payload split-pembayaran (field opsional {@code caraBayarTambahan}, array berisi
	 * s/d 4 objek {@code {caraBayar, nominal}} -- slot 2-5; slot 1 tetap dari field {@code caraBayar}
	 * lama). Dipisah dari {@link #bayar(Tbmuser, JSONObject, JSONObject)} supaya method itu tidak
	 * makin gemuk dan supaya validasi "total semua slot != totalBiaya" hanya ditulis SEKALI, dipakai
	 * di kedua cabang if/else (draft vs baru) yang sebelumnya duplikat identik.
	 */
	private static class SplitPembayaran {
		CaraPembayaranKoperasi cara2, cara3, cara4, cara5;
		Double nominal2 = 0.0, nominal3 = 0.0, nominal4 = 0.0, nominal5 = 0.0;
		String pesanError;

		void terapkanKe(PembelianAnggotaKoperasi p) {
			p.setCaraPembayaranKoperasi2(cara2);
			p.setNominalBayar2(nominal2);
			p.setCaraPembayaranKoperasi3(cara3);
			p.setNominalBayar3(nominal3);
			p.setCaraPembayaranKoperasi4(cara4);
			p.setNominalBayar4(nominal4);
			p.setCaraPembayaranKoperasi5(cara5);
			p.setNominalBayar5(nominal5);
		}
	}

	private static SplitPembayaran resolveSplitPembayaran(JSONObject jsonObject, Double totalBiaya) {
		SplitPembayaran hasil = new SplitPembayaran();
		if (jsonObject.isNull("caraBayarTambahan")) {
			return hasil;
		}
		JSONArray tambahan;
		try {
			tambahan = jsonObject.getJSONArray("caraBayarTambahan");
		} catch (Exception e) {
			return hasil;
		}
		// Maks 4 slot tambahan (slot 2-5) -- elemen ke-5 dst diabaikan diam-diam, bukan error keras,
		// supaya klien yang (secara keliru) mengirim lebih dari batas tetap bisa checkout dgn 5
		// metode pertama drpd transaksi gagal total.
		CaraPembayaranKoperasi[] caraSlot = new CaraPembayaranKoperasi[4];
		Double[] nominalSlot = new Double[] { 0.0, 0.0, 0.0, 0.0 };
		double totalTambahan = 0.0;
		int batas = Math.min(tambahan.length(), 4);
		for (int i = 0; i < batas; i++) {
			try {
				JSONObject entri = tambahan.getJSONObject(i);
				if (entri.isNull("caraBayar") || !Common.isNumber((entri.get("caraBayar") + "").trim())) {
					continue;
				}
				double nominal = entri.isNull("nominal") ? 0.0 : Double.parseDouble((entri.get("nominal") + "").trim());
				if (nominal <= 0.0) {
					continue;
				}
				CaraPembayaranKoperasi cara = (CaraPembayaranKoperasi) GeneralValueObject
						.ambilData(CaraPembayaranKoperasi.class, (entri.get("caraBayar") + "").trim());
				if (cara == null) {
					hasil.pesanError = "Salah satu metode pembayaran tambahan tidak ditemukan.";
					return hasil;
				}
				caraSlot[i] = cara;
				nominalSlot[i] = nominal;
				totalTambahan += nominal;
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/KantinHelper.java:resolveSplitPembayaran");
			}
		}
		// Toleransi pembulatan kecil (0.5) -- nominal dari klien berupa pecahan hasil input manual kasir.
		if (totalBiaya != null && totalTambahan > totalBiaya.doubleValue() + 0.5) {
			hasil.pesanError = "Total nominal metode pembayaran tambahan (" + Common.numberFormat.get().format(totalTambahan)
					+ ") melebihi total tagihan (" + Common.numberFormat.get().format(totalBiaya) + ").";
			return hasil;
		}
		hasil.cara2 = caraSlot[0];
		hasil.nominal2 = nominalSlot[0];
		hasil.cara3 = caraSlot[1];
		hasil.nominal3 = nominalSlot[1];
		hasil.cara4 = caraSlot[2];
		hasil.nominal4 = nominalSlot[2];
		hasil.cara5 = caraSlot[3];
		hasil.nominal5 = nominalSlot[3];
		return hasil;
	}

	public static void bayar(Tbmuser tbmuser, JSONObject jsonObject, JSONObject hasil) throws Exception {
		if (!jsonObject.isNull("kodeUnik") && !jsonObject.isNull("idToko") && !jsonObject.isNull("waktu")
				&& !jsonObject.isNull("transaksi") && !jsonObject.isNull("caraBayar")
				&& Common.isNumber((jsonObject.get("idToko") + "").trim())
				&& Common.isNumber((jsonObject.get("caraBayar") + "").trim())) {

			// id/kodePembayaranOnline: divalidasi dgn Common.isNumber() dulu (sama seperti idToko/caraBayar
			// di atas) sebelum Long.parseLong() -- SEBELUMNYA langsung diparse mentah, sehingga nilai
			// bukan-angka yang terkirim dari klien akan melempar NumberFormatException di LUAR blok
			// try/catch method ini (baru dibuka di bawah), lolos tanpa respons JSON status=91 yang rapi
			// seperti kegagalan lain di method ini -- melainkan exception mentah merambat ke pemanggil.
			// Nilai tak valid kini diperlakukan sama seperti field kosong (null), bukan error fatal.
			Long id = (jsonObject.isNull("id") || !Common.isNumber((jsonObject.get("id") + "").trim())) ? null
					: Long.parseLong((jsonObject.get("id") + "").trim());
			Long kodePembayaranOnlineId = (jsonObject.isNull("kodePembayaranOnline")
					|| !Common.isNumber((jsonObject.get("kodePembayaranOnline") + "").trim())) ? null
							: Long.parseLong((jsonObject.get("kodePembayaranOnline") + "").trim());
			Toko toko = (Toko) GeneralValueObject.ambilData(Toko.class, (jsonObject.get("idToko") + "").trim());
			CaraPembayaranKoperasi caraPembayaranKoperasiOnline = (CaraPembayaranKoperasi) GeneralValueObject
					.ambilData(CaraPembayaranKoperasi.class, (jsonObject.get("caraBayar") + "").trim());
			if (toko != null) {
				String kodeUnik = (jsonObject.get("kodeUnik") + "").trim();
				Date currentWaktu = WaktuUtil.getDate();
				try {
					currentWaktu = Common.dateFormat3.get().parse((jsonObject.get("waktu") + "").trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:44");
					// TODO: handle exception
				}
				AnggotaKoperasi anggotaKoperasi = (AnggotaKoperasi) (jsonObject.isNull("id_member") ? null
						: GeneralValueObject.ambilData(AnggotaKoperasi.class,
								(jsonObject.get("id_member") + "").trim()));

				MejaKantin mejaKantin = (MejaKantin) (jsonObject.isNull("mejaKantin") ? null
						: GeneralValueObject.ambilData(MejaKantin.class, (jsonObject.get("mejaKantin") + "").trim()));

				Session session = HibernateUtil.getSessionFactory().openSession();

				try {
					// Fitur "Sesi Kasir": gerbang SERVER-SIDE (belt-and-suspenders) di titik SATU-SATUNYA
					// tempat checkout FINAL ditulis -- JSP dan Desktop (PosApi) sebelumnya TIDAK punya
					// gerbang sama sekali (hanya ZK PosKantinAction.onBayar() yg mengecek client-side
					// sebelum sampai ke sini). Dipasang di sini, ketiga platform otomatis konsisten tanpa
					// duplikasi logika. Sengaja TIDAK dipasang di draft_bayar() -- "Simpan/Tahan Keranjang"
					// bukan komitmen finansial, jadi tidak perlu sesi kas terbuka. OFF secara default
					// (lihat javadoc Konfigurasi.KANTIN_POS_WAJIB_SESI_KAS). Memakai session POLA B yang
					// SUDAH dibuka di atas (BUKAN HibernateUtil.currentSession() -- lihat javadoc kelas).
					if (Common.bolehKonfigurasi(ais.database.model.Konfigurasi.KANTIN_POS_WAJIB_SESI_KAS,
							ais.database.model.Konfigurasi.TIDAK_AKTIF)) {
						String[] idKasir = identitasKasir(tbmuser);
						if (ais.action.master.koperasi.helper.SesiKasUtil.idSesiTerbuka(session,
								idKasir[0], idKasir[1], toko.getId()) == null) {
							hasil.put("status", "91");
							hasil.put("description",
									"Belum ada Sesi Kas Kasir yang terbuka. Buka kas terlebih dahulu sebelum memproses pembayaran.");
							return;
						}
					}
					Long iddraftPembelianAnggotaKoperasi = (jsonObject.isNull("draftPembelianAnggotaKoperasi")
							|| !Common.isNumber((jsonObject.get("draftPembelianAnggotaKoperasi") + "").trim())) ? null
									: Long.parseLong((jsonObject.get("draftPembelianAnggotaKoperasi") + "").trim());
					DraftPembelianAnggotaKoperasi draftPembelianAnggotaKoperasi = (iddraftPembelianAnggotaKoperasi == null
							? null
							: (DraftPembelianAnggotaKoperasi) session
									.createCriteria(DraftPembelianAnggotaKoperasi.class)
									.add(Restrictions.idEq(iddraftPembelianAnggotaKoperasi)).uniqueResult());
					JSONArray transaksi = jsonObject.getJSONArray("transaksi");

					// Fase 0 (2026-07-26, gap analisis PDF klien "Kadaluarsa"): produk yang SUDAH lewat
					// tanggal kadaluarsa (Produk.tanggalExpired) WAJIB diblokir keras -- BEDA dgn kekurangan
					// stok biasa di bawah (yang sengaja fail-open per instruksi 2026-07-20), krn menjual
					// barang kadaluarsa adalah masalah keamanan pangan/kepatuhan, bukan sekadar masalah
					// data stok historis blm bersih. Field ini sudah ada (dipakai laporan "akan/sudah
					// kadaluarsa" di LaporanKantinUtil) tapi SEBELUM INI tidak pernah dicek sama sekali di
					// jalur checkout -- kasir bisa saja menjual produk kadaluarsa tanpa peringatan apa pun.
					List<String> produkKadaluarsa = cekProdukKadaluarsa(session, transaksi);
					if (!produkKadaluarsa.isEmpty()) {
						hasil.put("status", "91");
						hasil.put("description", "Produk berikut sudah melewati tanggal kadaluarsa dan tidak boleh dijual: "
								+ String.join(", ", produkKadaluarsa) + ". Segera pisahkan dari stok jual.");
						return;
					}

					// Fase 1: validasi stok server-side dengan row lock (SELECT...FOR UPDATE) -- sebelumnya
					// TIDAK ADA pengecekan stok di server sama sekali (hanya client-side lolosCekStok() di
					// PosKantinAction, yang bisa dilewati sepenuhnya lewat panggilan langsung ke
					// /Data?action=bayar).
					//
					// Per instruksi user 2026-07-20: SEMENTARA jangan blokir transaksi walau kekurangan
					// stok genuinely terdeteksi (banyak toko punya baseline stok historis yang belum
					// direkonsiliasi -- lihat [[cegah-oversell-default-blokir-toko-belum-opname]] --
					// sehingga blokir keras di sini menolak transaksi pelanggan yang sah). Kekurangan tetap
					// DICATAT ke audit log (agar tetap terlihat & bisa ditindaklanjuti stok opname), tapi
					// transaksi tetap diteruskan (fail-open). Fail-safe bila mekanisme cek itu sendiri
					// error tetap dipertahankan (validasiStokCukupDenganLock kembalikan null).
					//
					// PENGECUALIAN (2026-07-24, spesifikasi "dashboard kasir" butir 8): produk dengan
					// override per-produk Produk.izinkanJualMinusStok=false WAJIB diblokir keras di sini
					// -- admin sengaja mengunci produk itu, kekurangannya TIDAK boleh diam-diam dilewati
					// spt gerbang toko default.
					HasilValidasiStok stokKurang = validasiStokCukupDenganLock(transaksi);
					if (stokKurang != null && stokKurang.wajibBlokir != null && !stokKurang.wajibBlokir.isEmpty()) {
						StringBuilder pesanWajibBlokir = new StringBuilder();
						for (String s : stokKurang.wajibBlokir) {
							if (pesanWajibBlokir.length() > 0) {
								pesanWajibBlokir.append(", ");
							}
							pesanWajibBlokir.append(s);
						}
						hasil.put("status", "91");
						hasil.put("description", "Stok tidak mencukupi utk produk yang dikunci admin (tidak boleh dijual minus): " + pesanWajibBlokir);
						return;
					}
					if (stokKurang != null && stokKurang.semuaKurang != null && !stokKurang.semuaKurang.isEmpty()) {
						StringBuilder pesanKurang = new StringBuilder();
						for (String s : stokKurang.semuaKurang) {
							if (pesanKurang.length() > 0) {
								pesanKurang.append(", ");
							}
							pesanKurang.append(s);
						}
						ais.common.ErrorAuditUtil.record(
								new RuntimeException("Stok tidak mencukupi (transaksi TETAP diproses): " + pesanKurang),
								"auto-audit src/ais/action/servlet/api/KantinHelper.java:stokKurangDilewati");
					}

					TotalHitung th = hitungTotalDiskonCashback(jsonObject, transaksi, "bayarHitungTotal");
					Double total = Double.valueOf(th.total);
					Double totalDiskon = Double.valueOf(th.totalDiskon);
					Double totalCashback = Double.valueOf(th.totalCashback);

					// Split pembayaran (maks 5 metode/transaksi): "caraBayar" (di atas) TETAP wajib &
					// selalu jadi slot 1 -- payload lama tanpa "caraBayarTambahan" sama sekali (client
					// belum di-update) berperilaku 100% identik spt sebelum fitur ini ada. Elemen ke-5
					// dst pada array diabaikan (lihat JavaDoc SplitPembayaran di bawah).
					SplitPembayaran split = resolveSplitPembayaran(jsonObject, total);
					if (split.pesanError != null) {
						hasil.put("status", "91");
						hasil.put("description", split.pesanError);
						return;
					}

					KodePembayaranOnline kodePembayaranOnline = (KodePembayaranOnline) (kodePembayaranOnlineId == null
							? null
							: session.createCriteria(KodePembayaranOnline.class)
									.add(Restrictions.idEq(kodePembayaranOnlineId)).uniqueResult());

					Lokasi lokasi = (Lokasi) session.createCriteria(Lokasi.class).add(Restrictions.eq("toko", toko))
							.add(Restrictions.eq("aktif", true)).setMaxResults(1).uniqueResult();
					if (lokasi == null) {
						lokasi = new Lokasi();
						lokasi.setToko(toko);
						lokasi.setAktif(true);
						lokasi.setNama(toko.getNama());
						session.getTransaction().begin();
						session.save(lokasi);
						session.getTransaction().commit();
					}

					// Gap-closure "kasir tertera external_update" + "banyak mesin POS satu toko": KEDUANYA
					// dihitung SEKALI di sini, dipakai ulang di kedua cabang if/else bawah -- lihat JavaDoc
					// PembelianAnggotaKoperasi.getKasirLoginNama()/getNamaMesin() utk alasan lengkap kenapa
					// field baru ini TIDAK memakai oleh/olehId (audit generik, selalu "external_update"
					// utk request PosApi krn tak ada sesi browser).
					String kasirLoginNamaVal = identitasKasir(tbmuser)[0];
					String namaMesinVal = jsonObject.optString("nama_mesin", "").trim();
					if (namaMesinVal.isEmpty()) namaMesinVal = null;

					PembelianAnggotaKoperasi pembelianAnggotaKoperasi = null;
					if (draftPembelianAnggotaKoperasi != null && draftPembelianAnggotaKoperasi.getId() != null
							&& draftPembelianAnggotaKoperasi.getLunas() != null
							&& draftPembelianAnggotaKoperasi.getLunas().getId() != null) {
						pembelianAnggotaKoperasi = draftPembelianAnggotaKoperasi.getLunas();
						pembelianAnggotaKoperasi.setKeterangan(jsonObject.isNull("keterangan") ? null
								: (jsonObject.get("keterangan") + "").trim());
						pembelianAnggotaKoperasi.setMejaKantin(mejaKantin);
						pembelianAnggotaKoperasi.setTotalDiskon(totalDiskon);
						pembelianAnggotaKoperasi.setTotalCashback(totalCashback);
						pembelianAnggotaKoperasi.setKodePembayaranOnline(kodePembayaranOnline);
						pembelianAnggotaKoperasi.setKode(kodeUnik);
						pembelianAnggotaKoperasi.setAnggotaKoperasi(anggotaKoperasi);
						pembelianAnggotaKoperasi.setTanggalPembayaran(currentWaktu);
						pembelianAnggotaKoperasi.setCaraPembayaranKoperasi(caraPembayaranKoperasiOnline);
						pembelianAnggotaKoperasi.setTotalBiaya(total);
						pembelianAnggotaKoperasi.setBiaya(total);
						split.terapkanKe(pembelianAnggotaKoperasi);
						pembelianAnggotaKoperasi.setLokasi(lokasi);
						pembelianAnggotaKoperasi.setTbmuser(tbmuser);
						pembelianAnggotaKoperasi.setKasirLoginNama(kasirLoginNamaVal);
						pembelianAnggotaKoperasi.setNamaMesin(namaMesinVal);
						pembelianAnggotaKoperasi.setToko(toko);
						pembelianAnggotaKoperasi.setPajak(jsonObject.isNull("pajak") ? 0.0 : Math.max(0.0, Double.parseDouble((jsonObject.get("pajak") + "").trim())));
						pembelianAnggotaKoperasi.setDraftPembelianAnggotaKoperasi(draftPembelianAnggotaKoperasi);
						session.getTransaction().begin();
						session.saveOrUpdate(pembelianAnggotaKoperasi);
						session.getTransaction().commit();

					} else {

						pembelianAnggotaKoperasi = (PembelianAnggotaKoperasi) (id == null ? null
								: session.createCriteria(PembelianAnggotaKoperasi.class).add(Restrictions.idEq(id))
										.uniqueResult());
						if (pembelianAnggotaKoperasi == null) {
							pembelianAnggotaKoperasi = new PembelianAnggotaKoperasi();
						}
						pembelianAnggotaKoperasi.setKeterangan(jsonObject.isNull("keterangan") ? null
								: (jsonObject.get("keterangan") + "").trim());
						pembelianAnggotaKoperasi.setMejaKantin(mejaKantin);
						pembelianAnggotaKoperasi.setTotalDiskon(totalDiskon);
						pembelianAnggotaKoperasi.setTotalCashback(totalCashback);
						pembelianAnggotaKoperasi.setKodePembayaranOnline(kodePembayaranOnline);
						pembelianAnggotaKoperasi.setKode(kodeUnik);
						pembelianAnggotaKoperasi.setAnggotaKoperasi(anggotaKoperasi);
						pembelianAnggotaKoperasi.setTanggalPembayaran(currentWaktu);
						pembelianAnggotaKoperasi.setCaraPembayaranKoperasi(caraPembayaranKoperasiOnline);
						pembelianAnggotaKoperasi.setTotalBiaya(total);
						pembelianAnggotaKoperasi.setBiaya(total);
						split.terapkanKe(pembelianAnggotaKoperasi);
						pembelianAnggotaKoperasi.setLokasi(lokasi);
						pembelianAnggotaKoperasi.setTbmuser(tbmuser);
						pembelianAnggotaKoperasi.setKasirLoginNama(kasirLoginNamaVal);
						pembelianAnggotaKoperasi.setNamaMesin(namaMesinVal);
						pembelianAnggotaKoperasi.setToko(toko);
						pembelianAnggotaKoperasi.setPajak(jsonObject.isNull("pajak") ? 0.0 : Math.max(0.0, Double.parseDouble((jsonObject.get("pajak") + "").trim())));
						pembelianAnggotaKoperasi.setDraftPembelianAnggotaKoperasi(draftPembelianAnggotaKoperasi);
						session.getTransaction().begin();
						session.saveOrUpdate(pembelianAnggotaKoperasi);
						session.getTransaction().commit();

					}

					session.getTransaction().begin();
					if (draftPembelianAnggotaKoperasi != null && draftPembelianAnggotaKoperasi.getId() != null) {
						session.createSQLQuery(
								"update koperasi.draft_pembelian set lunas = null where draft_pembelian_anggota_koperasi = :draftId")
								.setParameter("draftId", draftPembelianAnggotaKoperasi.getId()).executeUpdate();
					}
					session.createSQLQuery(
							"delete from koperasi.pembelian where pembelian_anggota_koperasi = :id")
							.setParameter("id", pembelianAnggotaKoperasi.getId()).executeUpdate();
					session.getTransaction().commit();

					JSONArray arrayTransaksi = pembelianAnggotaKoperasi.simpanRinci(session, transaksi, kodeUnik,
							currentWaktu, toko, kodePembayaranOnline, draftPembelianAnggotaKoperasi);
					// Gap-closure -- padanan pengecekan di draft_bayar() (lihat JavaDoc di sana): kalau
					// draftPembelianAnggotaKoperasi != null, simpanRinci() menyalin item dari BARIS
					// {@code koperasi.draft_pembelian} di DB (BUKAN dari payload {@code transaksi} ini),
					// jadi kalau draft itu ternyata 0 baris (mis. gara-gara "Tahan" sebelumnya gagal diam-
					// diam), transaksi ini akan FINAL/lunas dgn total > 0 tapi TANPA satu pun baris item --
					// penjualan hantu yg tak tercermin di stok/laporan. WAJIB dihentikan di sini, SEBELUM
					// blok pengurangan stok/bahan baku di bawah sempat jalan (yg iterasi dari {@code
					// transaksi} milik klien, bukan dari arrayTransaksi -- kalau dibiarkan lanjut, stok
					// akan berkurang utk item yg toh tak pernah tercatat terjual).
					if (arrayTransaksi.length() == 0 && transaksi.length() > 0) {
						hasil.put("status", "91");
						hasil.put("description", "Gagal menyimpan rincian item transaksi (0 dari " + transaksi.length()
								+ " item tersimpan) -- transaksi DIBATALKAN, silakan coba bayar ulang. Jika berulang, hubungi admin.");
						ais.common.ErrorAuditUtil.record(
								new RuntimeException("bayar: 0 dari " + transaksi.length()
										+ " item tersimpan utk pembelianAnggotaKoperasi id="
										+ pembelianAnggotaKoperasi.getId()),
								"auto-audit src/ais/action/servlet/api/KantinHelper.java:bayarItemKosong");
						return;
					}

					// === Otomatis: kurangi stok bahan baku (resep/BOM) untuk produk ber-resep yang terjual.
					// Fail-safe: kegagalan di sini tidak boleh menggagalkan transaksi penjualan. ===
					try {
						java.util.Set<Long> bahanTerpakai = ais.action.master.inventory.BahanBakuUtil
								.konsumsiBahanBaku(session, transaksi, toko, currentWaktu, pembelianAnggotaKoperasi);
						for (Long bahanId : bahanTerpakai) {
							try {
								ais.action.master.inventory.StokKantinUtil.recomputeStokProduk(bahanId);
							} catch (Exception exStok) {
								exStok.printStackTrace(); ais.common.ErrorAuditUtil.record(exStok, "auto-audit src/ais/action/servlet/api/KantinHelper.java:179");
							}
						}
					} catch (Exception exBahan) {
						exBahan.printStackTrace(); ais.common.ErrorAuditUtil.record(exBahan, "auto-audit src/ais/action/servlet/api/KantinHelper.java:183");
					}

					// === FASE 1 (perbaikan celah lama): produk yang DIJUAL LANGSUNG juga wajib
					// direcompute stoknya di sini -- sebelumnya hanya bahan baku/resep (blok di atas) yang
					// direcompute, sehingga Produk.stok milik item yang dijual langsung (bukan bahan baku
					// siapa pun) tetap basi sampai proses lain (Pengadaan/StokOpname) kebetulan menyentuhnya.
					// Formula StokKantinUtil sudah mengurangi koperasi.pembelian.qty (baris penjualan yang
					// baru saja ditulis simpanRinci() di atas), jadi recompute di sini otomatis benar --
					// tidak perlu logika baru, cukup dipanggil. Jalur POS JSP offline
					// (pos_offline_service.jsp) sudah melakukan ini; disamakan di sini supaya kedua jalur
					// checkout konsisten. Fail-safe, pola identik dengan blok bahan baku di atas. ===
					try {
						java.util.Set<Long> produkTerjual = new java.util.HashSet<Long>();
						for (int i = 0; i < transaksi.length(); i++) {
							try {
								JSONObject t = transaksi.getJSONObject(i);
								if (!t.isNull("id")) {
									produkTerjual.add(Long.valueOf(Long.parseLong((t.get("id") + "").trim())));
								}
							} catch (Exception exParse) {
								ais.common.ErrorAuditUtil.record(exParse,
										"auto-audit src/ais/action/servlet/api/KantinHelper.java:produkTerjualParse");
							}
						}
						for (Long produkId : produkTerjual) {
							try {
								ais.action.master.inventory.StokKantinUtil.recomputeStokProduk(produkId);
							} catch (Exception exStok) {
								exStok.printStackTrace();
								ais.common.ErrorAuditUtil.record(exStok,
										"auto-audit src/ais/action/servlet/api/KantinHelper.java:produkTerjualRecompute");
							}
						}
					} catch (Exception exProduk) {
						exProduk.printStackTrace();
						ais.common.ErrorAuditUtil.record(exProduk,
								"auto-audit src/ais/action/servlet/api/KantinHelper.java:produkTerjualBlock");
					}

					// === FASE 2 (opsional, gerbang konfigurasi): penjualan kantin -> stok KELUAR aset
					// untuk produk yang tertaut ke barang persediaan. Fail-safe + idempoten per bill. ===
					try {
						ais.action.master.inventory.KantinAssetSyncUtil.konsumsiPenjualanKeAset(session, transaksi,
								pembelianAnggotaKoperasi, currentWaktu);
					} catch (Exception exAset) {
						exAset.printStackTrace(); ais.common.ErrorAuditUtil.record(exAset, "auto-audit src/ais/action/servlet/api/KantinHelper.java:192");
					}

					if (draftPembelianAnggotaKoperasi != null && draftPembelianAnggotaKoperasi.getId() != null) {
						draftPembelianAnggotaKoperasi.setLunas(pembelianAnggotaKoperasi);
						session.getTransaction().begin();
						session.update(draftPembelianAnggotaKoperasi);
						session.getTransaction().commit();
					}

					if (dimintaLangsungTerlayani(jsonObject)) {
						tandaiRincianTerlayani(session, pembelianAnggotaKoperasi);
					}

					hasil.put("data", arrayTransaksi);
					hasil.put("pembelianAnggotaKoperasi", pembelianAnggotaKoperasi.getId());
					hasil.put("idTransaksi", pembelianAnggotaKoperasi.getId());
					hasil.put("terlayani", dimintaLangsungTerlayani(jsonObject));
					hasil.put("status", "00");

				} catch (Exception e) {
					hasil.put("status", "91");
					hasil.put("description", "Error: " + e.getMessage());
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/KantinHelper.java:209");
				} finally {
					try {
						try { session.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:212");}
						session.disconnect();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:214");
						// TODO: handle exception
					}
					try {
						session.close();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:219");
						// TODO: handle exception
					}
				}
			}
		}
	}

	/**
	 * <h3>Fase 1: validasi stok server-side dengan row lock sebelum baris penjualan ditulis.</h3>
	 *
	 * <p>Sebelumnya {@link #bayar} sama sekali tidak memvalidasi stok di server -- pengecekan hanya
	 * ada di klien ({@code PosKantinAction.lolosCekStok}), yang bisa dilewati sepenuhnya oleh siapa
	 * pun yang memanggil {@code /Data?action=bayar} langsung. Method ini mengunci baris
	 * {@code koperasi.produk} tiap item terjual (urut id menaik untuk mengurangi risiko deadlock antar
	 * transaksi konkuren) memakai {@code SELECT ... FOR UPDATE}, lalu membaca stok LIVE lewat formula
	 * kanonik yang sama dengan {@link ais.action.master.inventory.StokKantinUtil} (bukan field
	 * {@code Produk.stok} yang bisa saja basi) sebelum membandingkannya dengan qty yang diminta.</p>
	 *
	 * <p><b>Gerbang konfigurasi:</b> hanya aktif bila {@code Konfigurasi.KANTIN_POS_CEGAH_OVERSELL}
	 * AKTIF -- default TIDAK_AKTIF (OFF), sama seperti {@code KANTIN_POS_WAJIB_SESI_KAS}. Sebab: rumus
	 * stok LIVE di sini hanya mengakui item masuk lewat {@code koperasi.pengadaan_produk}/{@code
	 * stok_opname}; banyak toko existing tidak pernah mencatat stok masuk lewat modul Pengadaan (form
	 * Produk kantin tidak punya kolom stok yang bisa diisi manual) sehingga stok LIVE hasil hitungan
	 * sudah negatif dari riwayat penjualan lama, jauh sebelum gerbang ini ada. Mengaktifkan gerbang ini
	 * secara default per 2026-07-20 sempat memblokir SELURUH penjualan produk semacam itu di toko yang
	 * belum pernah opname (termasuk "Layani Semua Pesanan" utk draft lama). Toko yang datanya sudah
	 * bersih (rutin Pengadaan/Stok Opname) dapat mengaktifkan gerbang ini secara eksplisit lewat
	 * Konfigurasi &gt; Kasir (POS).</p>
	 *
	 * <p><b>Batas cakupan (diketahui, bukan bug):</b> lock ini hanya dipegang selama pengecekan itu
	 * sendiri (transaksi pendek tersendiri), BUKAN sepanjang seluruh proses simpan baris penjualan --
	 * {@link #bayar} memakai beberapa transaksi kecil terpisah per langkah, bukan satu transaksi
	 * atomik. Ini menutup celah "tidak ada validasi server-side sama sekali" (perbaikan utama Fase 1),
	 * tapi masih menyisakan race window sempit secara teoritis antara commit pengecekan ini dan commit
	 * baris penjualan. Penutupan penuh butuh menyatukan seluruh alur {@link #bayar} ke satu transaksi
	 * atomik -- di luar cakupan Fase 1 ini, dicatat sebagai risiko terbuka.</p>
	 *
	 * <p><b>Gagal-aman &amp; ADVISORY-ONLY (per 2026-07-20):</b> bila mekanisme pengecekan ini sendiri
	 * melempar exception (mis. masalah koneksi DB sesaat), method mengembalikan {@code null}. Sejak
	 * 2026-07-20, hasil kekurangan stok yang GENUINELY terdeteksi juga TIDAK LAGI memblokir transaksi di
	 * {@link #bayar} -- pemanggil hanya mencatatnya ke audit log dan tetap meneruskan penjualan
	 * (fail-open penuh). Perubahan ini diminta eksplisit oleh pengguna karena blokir keras menolak
	 * transaksi pelanggan yang sah di toko dengan baseline stok historis belum bersih (lihat
	 * [[cegah-oversell-default-blokir-toko-belum-opname]]). Method ini masih berguna sebagai sumber
	 * deteksi/audit shortage, sekadar tidak lagi dipakai sebagai gerbang blokir.</p>
	 *
	 * <p><b>Override per-produk (2026-07-24):</b> {@link ais.database.model.inventory.Produk#getIzinkanJualMinusStok()}
	 * membiarkan admin mengunci PRODUK TERTENTU supaya WAJIB diblokir begitu stoknya tidak cukup,
	 * TERLEPAS dari gerbang toko di atas (mis. barang mahal/gampang basi) -- ditandai lewat
	 * {@link HasilValidasiStok#wajibBlokir} pada hasil balikan. Produk TANPA override (default,
	 * {@code null}) tetap sepenuhnya fail-open seperti sebelum field ini ada.</p>
	 *
	 * @param transaksi array item terjual dari payload POS ({@code {id, jumlah, ...}}).
	 * @return {@code null} bila stok semua item cukup (dan tak ada override wajib-blokir aktif);
	 *         selain itu {@link HasilValidasiStok} berisi {@code semuaKurang} (SELURUH kekurangan,
	 *         dipakai pemanggil utk audit log spt sebelumnya) dan {@code wajibBlokir} (subset yg
	 *         punya override per-produk {@code false} -- pemanggil WAJIB menolak transaksi bila
	 *         subset ini tidak kosong).
	 */
	/**
	 * Cek keras produk kadaluarsa dalam satu transaksi checkout (2026-07-26, gap analisis PDF klien
	 * "Kadaluarsa") -- {@link ais.database.model.inventory.Produk#getTanggalExpired()} sudah ada
	 * (dipakai laporan "akan/sudah kadaluarsa" di {@code LaporanKantinUtil}) tapi field itu SEBELUM
	 * INI tidak pernah dibaca di jalur checkout sama sekali. BEDA dgn kekurangan stok biasa (fail-open
	 * per instruksi 2026-07-20) -- ini SELALU memblokir keras, tidak ada gerbang konfigurasi utk
	 * mematikannya, krn menjual barang kadaluarsa adalah risiko keamanan pangan/kepatuhan, bukan
	 * sekadar data stok historis yang belum bersih.
	 *
	 * @return nama produk (dgn tanggal kadaluarsanya) yang ditemukan dalam {@code transaksi} dan SUDAH
	 *         lewat tanggal kadaluarsanya per hari ini; daftar kosong bila semua aman.
	 */
	private static List<String> cekProdukKadaluarsa(Session session, JSONArray transaksi) {
		List<String> hasil = new java.util.ArrayList<String>();
		if (transaksi == null || transaksi.length() == 0) {
			return hasil;
		}
		java.util.Date sekarang = ais.ui.util.WaktuUtil.getDate();
		for (int i = 0; i < transaksi.length(); i++) {
			try {
				JSONObject t = transaksi.getJSONObject(i);
				if (t.isNull("id")) {
					continue;
				}
				Long pid = Long.valueOf(Long.parseLong((t.get("id") + "").trim()));
				Produk p = (Produk) session.get(Produk.class, pid);
				if (p != null && p.getTanggalExpired() != null && p.getTanggalExpired().before(sekarang)) {
					hasil.add(p.getNama() + " (kadaluarsa " + Common.dateFormat6.get().format(p.getTanggalExpired()) + ")");
				}
			} catch (Exception exParse) {
				ais.common.ErrorAuditUtil.record(exParse,
						"auto-audit src/ais/action/servlet/api/KantinHelper.java:cekProdukKadaluarsaParse");
			}
		}
		return hasil;
	}

	private static HasilValidasiStok validasiStokCukupDenganLock(JSONArray transaksi) {
		if (transaksi == null || transaksi.length() == 0) {
			return null;
		}
		java.util.Map<Long, Double> diminta = new java.util.TreeMap<Long, Double>();
		for (int i = 0; i < transaksi.length(); i++) {
			try {
				JSONObject t = transaksi.getJSONObject(i);
				if (t.isNull("id")) {
					continue;
				}
				Long pid = Long.valueOf(Long.parseLong((t.get("id") + "").trim()));
				double qty = t.isNull("jumlah") ? 0 : Double.parseDouble((t.get("jumlah") + "").trim());
				if (qty <= 0) {
					continue;
				}
				Double prev = diminta.get(pid);
				diminta.put(pid, Double.valueOf((prev == null ? 0.0 : prev.doubleValue()) + qty));
			} catch (Exception exParse) {
				ais.common.ErrorAuditUtil.record(exParse,
						"auto-audit src/ais/action/servlet/api/KantinHelper.java:validasiStokParse");
			}
		}
		if (diminta.isEmpty()) {
			return null;
		}

		boolean gerbangToko = Common.bolehKonfigurasi(ais.database.model.Konfigurasi.KANTIN_POS_CEGAH_OVERSELL,
				ais.database.model.Konfigurasi.TIDAK_AKTIF);

		// Jalur cepat existing (gerbang toko OFF, kasus DEFAULT/paling umum): kalau tak satu pun produk
		// di transaksi ini punya override wajib-blokir, lewati SELURUH pengecekan (termasuk row lock)
		// spt sebelum fitur override ada -- TIDAK ada tambahan biaya performa utk toko yg tak memakainya.
		if (!gerbangToko) {
			Session cekSession = HibernateUtil.getSessionFactory().openSession();
			try {
				Long adaOverride = (Long) cekSession
						.createSQLQuery("SELECT COUNT(*) FROM koperasi.produk WHERE id IN ("
								+ idsDipisahKoma(diminta.keySet()) + ") AND izinkan_jual_minus_stok = false")
						.uniqueResult();
				if (adaOverride == null || adaOverride.longValue() == 0) {
					return null;
				}
			} catch (Exception ePreCek) {
				ais.common.ErrorAuditUtil.record(ePreCek,
						"auto-audit src/ais/action/servlet/api/KantinHelper.java:validasiStokPreCekOverride");
				return null; // gagal-aman: jangan blokir penjualan krn pre-cek ini sendiri error
			} finally {
				HibernateUtil.closeSessionQuietly(cekSession);
			}
		}

		java.util.List<String> kurang = new java.util.ArrayList<String>();
		java.util.List<String> wajibBlokir = new java.util.ArrayList<String>();
		Session lockSession = HibernateUtil.getSessionFactory().openSession();
		try {
			lockSession.getTransaction().begin();
			for (java.util.Map.Entry<Long, Double> en : diminta.entrySet()) {
				Long pid = en.getKey();
				double qtyDiminta = en.getValue().doubleValue();
				Object[] row = (Object[]) lockSession.createSQLQuery("SELECT nama, "
						+ "COALESCE((SELECT SUM(qty) FROM koperasi.pengadaan_produk WHERE produk=p.id),0)"
						+ " + COALESCE((SELECT SUM(selisih) FROM koperasi.stok_opname WHERE produk=p.id),0)"
						+ " - COALESCE((SELECT SUM(qty) FROM koperasi.pembelian WHERE produk=p.id),0)"
						+ " - COALESCE((SELECT SUM(qty) FROM koperasi.pemakaian_bahan_baku WHERE produk=p.id),0),"
						+ " izinkan_jual_minus_stok"
						+ " FROM koperasi.produk p WHERE p.id = " + pid + " FOR UPDATE").uniqueResult();
				if (row == null) {
					continue;
				}
				String nama = row[0] == null ? ("#" + pid) : row[0].toString();
				double stokLive = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
				Boolean overridePerItem = (row[2] instanceof Boolean) ? (Boolean) row[2] : null;
				if (stokLive < qtyDiminta) {
					String deskripsi = nama + " (sisa " + stokLive + ", diminta " + qtyDiminta + ")";
					kurang.add(deskripsi);
					if (Boolean.FALSE.equals(overridePerItem)) {
						wajibBlokir.add(deskripsi);
					}
				}
			}
			lockSession.getTransaction().commit();
		} catch (Exception e) {
			try {
				lockSession.getTransaction().rollback();
			} catch (Exception ignoreRollback) {
				ais.common.ErrorAuditUtil.record(ignoreRollback,
						"auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:validasiStokRollback");
			}
			e.printStackTrace();
			ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/KantinHelper.java:validasiStokCek");
			return null; // gagal-aman: jangan blokir penjualan krn util cek ini sendiri error
		} finally {
			try {
				lockSession.close();
			} catch (Exception ignoreClose) {
				ais.common.ErrorAuditUtil.record(ignoreClose,
						"auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:validasiStokClose");
			}
		}
		if (kurang.isEmpty()) {
			return null;
		}
		HasilValidasiStok hasil = new HasilValidasiStok();
		hasil.semuaKurang = kurang;
		hasil.wajibBlokir = wajibBlokir;
		return hasil;
	}

	/** Balikan {@link #validasiStokCukupDenganLock} -- lihat JavaDoc method tsb. */
	private static class HasilValidasiStok {
		java.util.List<String> semuaKurang;
		java.util.List<String> wajibBlokir;
	}

	/** Menggabungkan sekumpulan id produk jadi daftar angka dipisah koma siap ditempel ke klausa {@code IN (...)}. */
	private static String idsDipisahKoma(java.util.Set<Long> ids) {
		StringBuilder sb = new StringBuilder();
		for (Long id : ids) {
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(id.longValue());
		}
		return sb.toString();
	}

	/**
	 * <h3>Simpan keranjang belanja sebagai draft (BELUM final) — "Simpan Keranjang" / pesanan online.</h3>
	 *
	 * <p>Struktur mengikuti {@link #bayar} secara paralel (validasi field, resolusi Toko/CaraPembayaran/
	 * AnggotaKoperasi/MejaKantin/Lokasi, hitung total lewat {@link #hitungTotalDiskonCashback}), TAPI
	 * menyimpan ke entity yang BERBEDA: {@link DraftPembelianAnggotaKoperasi} dan baris
	 * {@code koperasi.draft_pembelian} — bukan {@link PembelianAnggotaKoperasi}/{@code pembelian}.
	 * Perbedaan ini disengaja dan penting: draft BUKAN transaksi final, jadi method ini SENGAJA TIDAK
	 * memicu efek samping apa pun yang berlaku untuk penjualan sungguhan — stok TIDAK berkurang,
	 * bahan baku TIDAK terpakai, sinkron aset TIDAK terjadi. Seluruh efek samping itu baru terjadi
	 * belakangan saat draft ini benar-benar di-{@link #bayar} (lihat parameter
	 * {@code draftPembelianAnggotaKoperasi} pada {@link #bayar}, yang menandai draft asal sebagai
	 * {@code lunas} setelah checkout final berhasil).</p>
	 *
	 * <p>Dipakai dua skenario: (1) kasir menekan "Simpan Keranjang" untuk menahan transaksi yang belum
	 * jadi dibayar pembeli (mis. menunggu konfirmasi item), dimunculkan lagi lewat panel "Keranjang
	 * Tertahan"; (2) pesanan online dari pembeli (anggota koperasi) yang MASIH menunggu diproses/
	 * dilayani kasir sebelum benar-benar dibayar.</p>
	 *
	 * @param tbmuser    pengguna yang menyimpan draft (kasir, atau sistem atas nama pembeli online).
	 * @param jsonObject payload keranjang, format sama dengan {@link #bayar}.
	 * @param hasil      objek keluaran ({@code status}, {@code description}, {@code data},
	 *                   {@code pembelianAnggotaKoperasi} — nama field terakhir dipertahankan sama
	 *                   dengan {@link #bayar} untuk konsistensi kontrak API, walau nilainya di sini
	 *                   adalah id {@link DraftPembelianAnggotaKoperasi}, bukan
	 *                   {@link PembelianAnggotaKoperasi}).
	 * @throws Exception lihat catatan yang sama pada {@link #bayar}.
	 */
	public static void draft_bayar(Tbmuser tbmuser, JSONObject jsonObject, JSONObject hasil) throws Exception {
		if (!jsonObject.isNull("kodeUnik") && !jsonObject.isNull("idToko") && !jsonObject.isNull("waktu")
				&& !jsonObject.isNull("transaksi") && !jsonObject.isNull("caraBayar")
				&& Common.isNumber((jsonObject.get("idToko") + "").trim())
				&& Common.isNumber((jsonObject.get("caraBayar") + "").trim())) {

			// Sama seperti bayar(): validasi Common.isNumber() dulu sebelum parseLong, supaya nilai
			// bukan-angka tidak melempar exception di luar try/catch method ini.
			Long id = (jsonObject.isNull("id") || !Common.isNumber((jsonObject.get("id") + "").trim())) ? null
					: Long.parseLong((jsonObject.get("id") + "").trim());
			Long kodePembayaranOnlineId = (jsonObject.isNull("kodePembayaranOnline")
					|| !Common.isNumber((jsonObject.get("kodePembayaranOnline") + "").trim())) ? null
							: Long.parseLong((jsonObject.get("kodePembayaranOnline") + "").trim());
			Toko toko = (Toko) GeneralValueObject.ambilData(Toko.class, (jsonObject.get("idToko") + "").trim());
			CaraPembayaranKoperasi caraPembayaranKoperasiOnline = (CaraPembayaranKoperasi) GeneralValueObject
					.ambilData(CaraPembayaranKoperasi.class, (jsonObject.get("caraBayar") + "").trim());
			if (toko != null) {
				String kodeUnik = (jsonObject.get("kodeUnik") + "").trim();
				Date currentWaktu = WaktuUtil.getDate();
				try {
					currentWaktu = Common.dateFormat3.get().parse((jsonObject.get("waktu") + "").trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:244");
					// TODO: handle exception
				}
				AnggotaKoperasi anggotaKoperasi = (AnggotaKoperasi) (jsonObject.isNull("id_member") ? null
						: GeneralValueObject.ambilData(AnggotaKoperasi.class,
								(jsonObject.get("id_member") + "").trim()));

				MejaKantin mejaKantin = (MejaKantin) (jsonObject.isNull("mejaKantin") ? null
						: GeneralValueObject.ambilData(MejaKantin.class, (jsonObject.get("mejaKantin") + "").trim()));

				Session session = HibernateUtil.getSessionFactory().openSession();

				try {

					KodePembayaranOnline kodePembayaranOnline = (KodePembayaranOnline) (kodePembayaranOnlineId == null
							? null
							: session.createCriteria(KodePembayaranOnline.class)
									.add(Restrictions.idEq(kodePembayaranOnlineId)).uniqueResult());

					Lokasi lokasi = (Lokasi) session.createCriteria(Lokasi.class).add(Restrictions.eq("toko", toko))
							.add(Restrictions.eq("aktif", true)).setMaxResults(1).uniqueResult();
					if (lokasi == null) {
						lokasi = new Lokasi();
						lokasi.setToko(toko);
						lokasi.setAktif(true);
						lokasi.setNama(toko.getNama());
						session.getTransaction().begin();
						session.save(lokasi);
						session.getTransaction().commit();
					}

					JSONArray transaksi = jsonObject.getJSONArray("transaksi");
					TotalHitung th = hitungTotalDiskonCashback(jsonObject, transaksi, "draftBayarHitungTotal");
					Double total = Double.valueOf(th.total);
					Double totalDiskon = Double.valueOf(th.totalDiskon);
					Double totalCashback = Double.valueOf(th.totalCashback);

					DraftPembelianAnggotaKoperasi pembelianAnggotaKoperasi = (DraftPembelianAnggotaKoperasi) (id == null
							? null
							: session.createCriteria(DraftPembelianAnggotaKoperasi.class).add(Restrictions.idEq(id))
									.uniqueResult());
					if (pembelianAnggotaKoperasi == null) {
						pembelianAnggotaKoperasi = new DraftPembelianAnggotaKoperasi();
					}
					pembelianAnggotaKoperasi.setKeterangan(jsonObject.isNull("keterangan") ? null
							: (jsonObject.get("keterangan") + "").trim());
					pembelianAnggotaKoperasi.setMejaKantin(mejaKantin);
					pembelianAnggotaKoperasi.setTotalDiskon(totalDiskon);
					pembelianAnggotaKoperasi.setTotalCashback(totalCashback);
					pembelianAnggotaKoperasi.setKodePembayaranOnline(kodePembayaranOnline);
					pembelianAnggotaKoperasi.setKode(kodeUnik);
					pembelianAnggotaKoperasi.setAnggotaKoperasi(anggotaKoperasi);
					pembelianAnggotaKoperasi.setTanggalPembayaran(currentWaktu);
					pembelianAnggotaKoperasi.setCaraPembayaranKoperasi(caraPembayaranKoperasiOnline);
					pembelianAnggotaKoperasi.setTotalBiaya(total);
					pembelianAnggotaKoperasi.setBiaya(total);
					pembelianAnggotaKoperasi.setLokasi(lokasi);
					pembelianAnggotaKoperasi.setTbmuser(tbmuser);
					// Gap-closure "kasir external_update" + identitas mesin POS -- lihat JavaDoc
					// PembelianAnggotaKoperasi.getKasirLoginNama()/getNamaMesin() (pola sama dipakai di
					// bayar()).
					pembelianAnggotaKoperasi.setKasirLoginNama(identitasKasir(tbmuser)[0]);
					String namaMesinDraftVal = jsonObject.optString("nama_mesin", "").trim();
					pembelianAnggotaKoperasi.setNamaMesin(namaMesinDraftVal.isEmpty() ? null : namaMesinDraftVal);
					pembelianAnggotaKoperasi.setToko(toko);
					session.getTransaction().begin();
					session.saveOrUpdate(pembelianAnggotaKoperasi);
					session.getTransaction().commit();

					session.createSQLQuery(
							"delete from koperasi.draft_pembelian where draft_pembelian_anggota_koperasi="
									+ pembelianAnggotaKoperasi.getId())
							.executeUpdate();

					JSONArray arrayTransaksi = pembelianAnggotaKoperasi.simpanRinci(session, transaksi, kodeUnik,
							currentWaktu, toko, kodePembayaranOnline);
					// Gap-closure "toast sukses tapi keranjang kosong saat Muat ke Keranjang": simpanRinci()
					// menelan exception per-item tanpa melempar ke atas, jadi SEBELUM INI method ini selalu
					// melapor status "00" walau seluruh item gagal tersimpan (mis. gara-gara error data satu
					// produk yg dulunya meracuni transaction seluruh loop -- lihat rollback fix di
					// DraftPembelianAnggotaKoperasi.simpanRinci). Deteksi eksplisit di sini supaya kasir
					// LANGSUNG tahu "Tahan" gagal (dan bisa coba lagi), bukan diam-diam kehilangan keranjang
					// yang baru ketahuan nanti saat "Muat ke Keranjang" ternyata kosong.
					if (arrayTransaksi.length() == 0 && transaksi.length() > 0) {
						hasil.put("status", "91");
						hasil.put("description", "Gagal menyimpan rincian keranjang (0 dari " + transaksi.length()
								+ " item tersimpan) -- silakan coba tekan \"Tahan\" lagi. Jika berulang, hubungi admin.");
						ais.common.ErrorAuditUtil.record(
								new RuntimeException("draft_bayar: 0 dari " + transaksi.length()
										+ " item tersimpan utk draftPembelianAnggotaKoperasi id="
										+ pembelianAnggotaKoperasi.getId()),
								"auto-audit src/ais/action/servlet/api/KantinHelper.java:draftBayarItemKosong");
						return;
					}
					hasil.put("data", arrayTransaksi);
					hasil.put("pembelianAnggotaKoperasi", pembelianAnggotaKoperasi.getId());
					hasil.put("status", "00");
				} catch (Exception e) {
					hasil.put("status", "91");
					hasil.put("description", "Error: " + e.getMessage());
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/KantinHelper.java:339");
				} finally {
					try {
						try { session.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:342");}
						session.disconnect();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:344");
						// TODO: handle exception
					}
					try {
						session.close();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:349");
						// TODO: handle exception
					}
				}
			}
		}
	}

	/**
	 * Membaca saldo/deposit terkini seorang anggota koperasi (dibulatkan ke bawah ke bilangan bulat
	 * lewat {@code Double.longValue()} — saldo rupiah tidak berdesimal di UI kasir). Nama method
	 * "topup" historis (isi ulang saldo) walau isinya murni pembacaan saldo, bukan operasi isi ulang
	 * itu sendiri — isi ulang saldo sungguhan ditangani modul pembayaran online terpisah, method ini
	 * hanya dipanggil front-end untuk menampilkan angka saldo terkini (mis. sebelum kasir memvalidasi
	 * cukup/tidaknya saldo member untuk metode bayar "Saldo").
	 *
	 * @param request payload berisi {@code id_member} (opsional); tanpa itu selalu mengembalikan 0.
	 * @param hasil   diisi {@code data} (saldo, {@code Long}) dan {@code status="00"} selalu (tidak
	 *                pernah gagal secara eksplisit — anggota tidak ditemukan pun tetap 0, bukan error).
	 */
	public static void topup(JSONObject request, JSONObject hasil) throws Exception {
		if (!request.isNull("id_member")) {
			AnggotaKoperasi anggotaKoperasi = (AnggotaKoperasi) (request.isNull("id_member") ? null
					: GeneralValueObject.ambilData(AnggotaKoperasi.class, (request.get("id_member") + "").trim()));
			Double tabungan = ais.action.master.sekolah.util.DepositHelper.hitungDeposit(anggotaKoperasi);
			Long longData = tabungan.longValue();
			hasil.put("data", longData);
			hasil.put("status", "00");
		} else {
			hasil.put("data", 0L);
			hasil.put("status", "00");
		}
	}

	/**
	 * Membaca saldo/deposit anggota koperasi seperti {@link #topup}, dengan tambahan opsi memotong
	 * perhitungan pada tanggal tertentu lewat {@code tanggal_sama_atau_sebelum} — dipakai laporan/
	 * rekonsiliasi yang perlu tahu "berapa saldo member ini PADA tanggal X" (mis. saat audit),
	 * bukan sekadar saldo saat ini.
	 *
	 * <p>Tanggal batas diterima dalam dua format: ISO 8601 dengan penanda {@code "T"}
	 * (mis. {@code 2026-03-06T14:30:00}, format standar payload JSON) atau format SQL polos
	 * (mis. {@code 2026-03-06 14:30:00}, dengan atau tanpa desimal milidetik yang dibuang otomatis).
	 * Tanggal yang gagal diparse diperlakukan sebagai "tanpa batas" (fail-safe: dicatat ke konsol dan
	 * jatuh ke saldo TERKINI), bukan error yang menggagalkan permintaan.</p>
	 *
	 * @param request payload berisi {@code id_member} (opsional) dan {@code tanggal_sama_atau_sebelum}
	 *                (opsional, kosong berarti saldo terkini tanpa batas tanggal).
	 * @param hasil   diisi {@code data} (saldo pada tanggal batas atau saldo terkini) dan
	 *                {@code status="00"} selalu.
	 */
	public static void tabungan(JSONObject request, JSONObject hasil) throws Exception {
		if (!request.isNull("id_member")) {
			AnggotaKoperasi anggotaKoperasi = (AnggotaKoperasi) (request.isNull("id_member") ? null
					: GeneralValueObject.ambilData(AnggotaKoperasi.class, (request.get("id_member") + "").trim()));

			String tanggal_sama_atau_sebelumStr = request.optString("tanggal_sama_atau_sebelum", "");
			Date tanggalBatas = null;

			// 1. Logika Parsing String ke java.util.Date
			if (tanggal_sama_atau_sebelumStr != null && !tanggal_sama_atau_sebelumStr.trim().isEmpty()) {
				try {
					if (tanggal_sama_atau_sebelumStr.contains("T")) {
						// Jika format JSON/ISO 8601 (Contoh: 2026-03-06T14:30:00)
						tanggalBatas = Common.dateFormatInput.get().parse(tanggal_sama_atau_sebelumStr);
					} else {
						// Jika format default SQL (Contoh: 2026-03-06 14:30:00)
						// Hapus desimal milisecond jika ada (misal: .000)
						if (tanggal_sama_atau_sebelumStr.contains(".")) {
							tanggal_sama_atau_sebelumStr = tanggal_sama_atau_sebelumStr.substring(0,
									tanggal_sama_atau_sebelumStr.indexOf("."));
						}
						SimpleDateFormat sdfSql = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						tanggalBatas = sdfSql.parse(tanggal_sama_atau_sebelumStr);
					}
				} catch (Exception e) {
					System.out.println("Gagal memparsing tanggal batas: " + e.getMessage());
				}
			}


			// 2. Hitung deposit -- overload dgn tanggalBatas dipakai bila diminta, jatuh ke saldo
			// terkini bila tidak ada batas tanggal.
			Double tabungan = (tanggalBatas != null)
					? ais.action.master.sekolah.util.DepositHelper.hitungDeposit(anggotaKoperasi, tanggalBatas)
					: ais.action.master.sekolah.util.DepositHelper.hitungDeposit(anggotaKoperasi);

			Long longData = tabungan.longValue();
			hasil.put("data", longData);
			hasil.put("status", "00");
		} else {
			hasil.put("data", 0L);
			hasil.put("status", "00");
		}
	}

	/**
	 * Identitas kasir untuk kecocokan Sesi Kas ({@link ais.action.master.koperasi.helper.SesiKasUtil})
	 * -- pola IDENTIK dengan {@code KasKasirZkAction}/{@code PosKantinAction}: nama pengguna (atau id
	 * bila nama kosong) sebagai {@code oleh}, id pengguna sebagai {@code olehId}. Disatukan di sini
	 * supaya versi JSP ({@code _pos.jsp}) mencocokkan sesi yang SAMA dengan versi ZK/menu Kas Kasir
	 * terpisah walau dipanggil lewat endpoint berbeda.
	 */
	private static String[] identitasKasir(Tbmuser tbmuser) {
		String oleh = (tbmuser != null && tbmuser.getUserNama() != null) ? tbmuser.getUserNama()
				: (tbmuser == null ? "-" : String.valueOf(tbmuser.getUserId()));
		String olehId = tbmuser == null ? "-" : String.valueOf(tbmuser.getUserId());
		return new String[] { oleh, olehId };
	}

	/**
	 * Fitur "Sesi Kasir" (versi JSP) -- status sesi kas TERBUKA milik kasir saat ini untuk toko
	 * tertentu: dipanggil saat {@code _pos.jsp} dimuat (dan tiap kali toko berpindah) untuk
	 * menggambar chip status ("Kas: Tertutup" / "Kas: Rp X sejak HH:mm") dan menentukan tombol mana
	 * (Buka/Tutup) yang ditampilkan. TIDAK menggerbang apa pun sendiri -- gerbang checkout dilakukan
	 * terpisah di {@link #bayar} lewat {@code Konfigurasi.KANTIN_POS_WAJIB_SESI_KAS}.
	 *
	 * @param request payload berisi {@code id_toko} (opsional -- null berarti tidak dibatasi toko).
	 * @param hasil   diisi {@code status="00"}, {@code terbuka} (boolean), dan bila terbuka:
	 *                {@code waktuBuka} (ISO), {@code modalAwal}, {@code totalTunai}, {@code totalNonTunai},
	 *                {@code kasSaatIni} (modalAwal + totalTunai berjalan).
	 */
	public static void sesiKasStatus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String[] id = identitasKasir(tbmuser);
		Long tokoId = request.isNull("id_toko") ? null : Long.valueOf(request.get("id_toko").toString());
		// Logging diagnostik (SAMA gaya dgn [SESI-KAS-BUKA]) -- ditambahkan setelah laporan lapangan
		// "sesi_kas_buka membalas sukses+commit terbukti di log, tapi sesi_kas_status BERULANG KALI
		// (bukan sekadar sesaat, dicoba ulang beberapa kali dgn jeda beberapa detik s/d menit) tetap
		// melaporkan tertutup". Sebelumnya method ini SAMA SEKALI tidak mencetak apa pun, jadi tidak
		// ada cara membandingkan identitas (oleh/olehId/toko) yang dipakai QUERY BACA ini terhadap yang
		// dipakai QUERY TULIS di sesiKasBuka -- kejadian berikutnya sekarang bisa dibandingkan LANGSUNG
		// dari catalina.out utk memastikan apakah keduanya benar-benar cocok atau ada perbedaan
		// (mis. spasi/kapitalisasi/nilai berbeda) yang selama ini luput dari pengamatan.
		System.out.println("[SESI-KAS-STATUS] cek -- kasir(oleh=" + id[0] + ", olehId=" + id[1] + "), toko=" + tokoId);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.inventory.SesiKasKasir sesi = ais.action.master.koperasi.helper.SesiKasUtil
					.sesiTerbuka(session, id[0], id[1], tokoId);
			System.out.println("[SESI-KAS-STATUS] hasil query -- " + (sesi == null ? "TIDAK DITEMUKAN (null)"
					: ("DITEMUKAN id=" + sesi.getId() + ", oleh=" + sesi.getOleh() + ", olehId=" + sesi.getOlehId()
							+ ", toko=" + (sesi.getToko() == null ? "null" : sesi.getToko().getId()) + ", status=" + sesi.getStatus())));
			// Diagnostik tambahan -- SAMA persis dgn [SESI-KAS-BUKA][DIAGNOSTIK]: cetak identitas fisik
			// koneksi database (current_database/inet_server_addr/inet_server_port) supaya bisa
			// dibandingkan LANGSUNG dgn log sisi tulis. Bila host/port/db BERBEDA antara kedua log,
			// itu bukti ada lebih dari satu database/replika yang terlibat di luar sepengetahuan kode
			// Hibernate (mis. proxy/load-balancer level TCP di depan Postgres).
			try {
				Object[] r = (Object[]) session
						.createSQLQuery("select cast(current_database() as text), cast(inet_server_addr() as text), cast(inet_server_port() as text)")
						.uniqueResult();
				System.out.println("[SESI-KAS-STATUS][DIAGNOSTIK] db=" + r[0] + ", host=" + r[1] + ", port=" + r[2]);
			} catch (Exception eInfo) {
				System.out.println("[SESI-KAS-STATUS][DIAGNOSTIK] gagal ambil info koneksi: " + eInfo);
			}
			hasil.put("status", "00");
			if (sesi == null) {
				hasil.put("terbuka", false);
			} else {
				double[] jual = ais.action.master.koperasi.helper.SesiKasUtil.hitungPenjualan(session, id[0], id[1],
						tokoId, sesi.getWaktuBuka(), new Date());
				double modalAwal = sesi.getModalAwal() == null ? 0.0 : sesi.getModalAwal().doubleValue();
				hasil.put("terbuka", true);
				hasil.put("waktuBuka", Common.dateFormatInput.get().format(sesi.getWaktuBuka()));
				hasil.put("modalAwal", modalAwal);
				hasil.put("totalTunai", jual[0]);
				hasil.put("totalNonTunai", jual[1]);
				hasil.put("kasSaatIni", modalAwal + jual[0]);
			}
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Penutupan session POLA B seragam (rollback transaksi tersisa bila ada -> clear -> disconnect ->
	 * close, masing-masing fail-safe).
	 *
	 * <p><b>Kenapa rollback ditambahkan (bukan sekadar clear/disconnect/close spt semula).</b> Banyak
	 * pemanggil method ini (mis. {@link #sesiKasStatus}) MURNI baca-saja -- TIDAK PERNAH memanggil
	 * {@code session.beginTransaction()}/{@code commit()} sendiri. Tapi konfigurasi Hibernate/JDBC yang
	 * lazim ({@code autocommit=false}) tetap membuka transaksi IMPLISIT begitu query pertama
	 * dijalankan. Bila transaksi implisit itu TIDAK PERNAH di-commit/rollback sebelum koneksi
	 * dikembalikan ke connection pool (c3p0), koneksi bisa tersangkut "idle in transaction" -- dan
	 * tergantung isolation level server, permintaan BERIKUTNYA yang meminjam koneksi yg SAMA dari pool
	 * bisa terus melihat SNAPSHOT DATA LAMA (dari saat transaksi implisit itu mulai), bukan data
	 * ter-commit terbaru. Ini cocok PERSIS dengan laporan lapangan "status sesi kas tak pernah ikut
	 * berubah walau baris baru sudah terbukti commit di log server" -- gejala yg TIDAK membaik meski
	 * dicoba ulang berkali-kali (retry klien tak bisa memperbaiki koneksi server yg tersangkut).
	 * Rollback eksplisit di sini AMAN utk SEMUA pemanggil (baik baca-saja maupun yg SUDAH commit
	 * sendiri lebih dulu -- {@code isActive()} otomatis false setelah commit, jadi tidak ada rollback
	 * ganda) dan memastikan koneksi SELALU kembali ke pool dalam keadaan transaksi bersih/tertutup.</p>
	 */
	private static void tutupSessionPolaB(Session session) {
		try {
			if (session != null && session.isOpen() && session.getTransaction() != null
					&& session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception eRollback) {
			ais.common.ErrorAuditUtil.record(eRollback,
					"auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:tutupSessionPolaB-rollback");
		}
		try {
			try {
				session.clear();
			} catch (Exception ignoreClear) {
				ais.common.ErrorAuditUtil.record(ignoreClear,
						"auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:tutupSessionPolaB-clear");
			}
			session.disconnect();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:tutupSessionPolaB-disconnect");
		}
		try {
			session.close();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:tutupSessionPolaB-close");
		}
	}

	/**
	 * Fitur "Sesi Kasir" (versi JSP) -- membuka sesi kas baru dengan modal awal. Menolak (status
	 * "91") bila kasir ini SUDAH punya sesi terbuka untuk toko yang sama, supaya tak ada dua sesi
	 * tumpang tindih yang membingungkan perhitungan {@code hitungPenjualan}.
	 *
	 * <p><b>Logging.</b> Method ini SENGAJA sangat cerewet (setiap langkah dicetak ke {@code
	 * System.out}/catalina.out dgn prefiks {@code [SESI-KAS-BUKA]}) -- akar masalah bug "Kas Belum
	 * Dibuka nyangkut" sebelumnya adalah kegagalan SENYAP (server membalas status="00" padahal baris
	 * {@code SesiKasKasir} tidak pernah benar-benar ter-commit, TANPA exception apa pun yg tercatat di
	 * log error), jadi tidak cukup hanya menangkap exception -- perlu jejak eksplisit di setiap
	 * tahapan supaya admin bisa memastikan LANGSUNG dari catalina.out bahwa baris benar-benar tersimpan
	 * (bukan menebak dari perilaku UI). Exception apa pun SEKARANG ditangkap eksplisit di sini (bukan
	 * hanya mengandalkan {@code PosApi.proses}'s catch-all) supaya pesannya spesifik ke aksi ini,
	 * lengkap dgn konteks kasir/toko, ketimbang pesan generik "Terjadi kesalahan pada sistem".
	 *
	 * @param request payload berisi {@code id_toko} (opsional), {@code modal_awal} (angka),
	 *                {@code keterangan} (opsional), {@code kode} (opsional -- idempotensi utk alur
	 *                "Sesi Kasir offline-first" Desktop/Android, lihat javadoc {@link
	 *                ais.database.model.inventory.SesiKasKasir#getKode()}), dan {@code waktu_buka}
	 *                (opsional, ISO -- waktu buka SEBENARNYA di klien bila permintaan ini adalah
	 *                sinkronisasi tertunda dari sesi yang sudah dibuka offline sebelumnya).
	 */
	public static void sesiKasBuka(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String[] id = identitasKasir(tbmuser);
		Long tokoId = request.isNull("id_toko") ? null : Long.valueOf(request.get("id_toko").toString());
		String kode = request.isNull("kode") ? null : request.optString("kode", null);
		System.out.println("[SESI-KAS-BUKA] mulai -- kasir(oleh=" + id[0] + ", olehId=" + id[1] + "), toko=" + tokoId
				+ ", kode=" + kode + ", payload=" + request.toString());
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			// Multi-toko (lihat JavaDoc Tbmuser.tokoAktifMultiToko/daftarTokoBolehDiakses) -- "Buka
			// Kas" adalah momen pengguna multi-toko WAJIB memilih toko yang mau dioperasikan sesi ini.
			// Pengguna toko-tunggal (kasus umum) SAMA SEKALI tidak kena cabang ini (daftar.size()<=1).
			java.util.List<Toko> daftarTokoBoleh = daftarTokoBolehDiakses(session, tbmuser);
			if (daftarTokoBoleh.size() > 1) {
				boolean tokoValid = false;
				for (Toko t : daftarTokoBoleh) {
					if (t.getId().equals(tokoId)) { tokoValid = true; break; }
				}
				if (!tokoValid) {
					hasil.put("status", "91");
					hasil.put("description", "Anda memiliki akses ke lebih dari satu toko -- pilih toko terlebih dahulu sebelum membuka kas.");
					return;
				}
				// Simpan pilihan supaya SEMUA aksi lain (katalog/bayar/produk/dst) ikut memakai toko
				// yang sama ini sampai pengguna membuka kas ulang dgn toko berbeda.
				Tbmuser tbmuserBaris = tbmuser == null || tbmuser.getUserId() == null ? null
						: (Tbmuser) session.get(Tbmuser.class, tbmuser.getUserId());
				if (tbmuserBaris != null) {
					tbmuserBaris.setTokoAktifMultiToko(tokoId);
					session.beginTransaction();
					session.update(tbmuserBaris);
					session.getTransaction().commit();
					tbmuser.setTokoAktifMultiToko(tokoId);
				}
			}

			// Idempotensi (alur offline-first): kalau klien SUDAH PERNAH mengirim kode yg sama dan
			// baris itu SUDAH ada, ini pasti percobaan-ulang sinkron (mis. respons sebelumnya hilang
			// di jalan) -- balas SUKSES apa adanya TANPA membuat baris baru & TANPA gerbang "sudah ada
			// sesi terbuka" di bawah (baris ini MEMANG sesi yg sedang disinkronkan itu sendiri).
			ais.database.model.inventory.SesiKasKasir sudahAda = kode == null ? null
					: ais.action.master.koperasi.helper.SesiKasUtil.cariByKode(session, kode);
			if (sudahAda != null) {
				System.out.println("[SESI-KAS-BUKA] idempotensi -- kode=" + kode + " SUDAH ada, id=" + sudahAda.getId()
						+ " -- balas sukses apa adanya (bukan retry gagal, bukan duplikat).");
				hasil.put("status", "00");
				hasil.put("id_server", sudahAda.getId());
				return;
			}

			Long idTerbuka = ais.action.master.koperasi.helper.SesiKasUtil.idSesiTerbuka(session, id[0], id[1], tokoId);
			if (idTerbuka != null) {
				System.out.println("[SESI-KAS-BUKA] ditolak -- sudah ada sesi terbuka id=" + idTerbuka
						+ " utk kasir/toko yg sama.");
				hasil.put("status", "91");
				hasil.put("description", "Sesi kas sudah terbuka. Tutup kas yang sedang berjalan sebelum membuka sesi baru.");
				return;
			}
			Toko toko = tokoId == null ? null : (Toko) session.get(Toko.class, tokoId);
			double modalAwal = request.optDouble("modal_awal", 0);
			String keterangan = request.optString("keterangan", "");
			java.util.Date waktuBukaKlien = null;
			if (!request.isNull("waktu_buka")) {
				try {
					waktuBukaKlien = Common.dateFormatInput.get().parse(request.optString("waktu_buka", null));
				} catch (Exception eParse) {
					waktuBukaKlien = null; // format tak dikenali -- jatuh ke waktu server, bukan gagal total.
				}
			}
			System.out.println("[SESI-KAS-BUKA] tidak ada sesi terbuka sebelumnya -- lanjut buka baru. toko="
					+ (toko == null ? "null(admin/global)" : (toko.getId() + " " + toko.getNama()))
					+ ", modalAwal=" + modalAwal);

			session.beginTransaction();
			System.out.println("[SESI-KAS-BUKA] transaksi Hibernate dimulai (session.beginTransaction() sukses).");
			ais.database.model.inventory.SesiKasKasir baru;
			try {
				baru = ais.action.master.koperasi.helper.SesiKasUtil.buka(session,
						toko, id[0], id[1], modalAwal, keterangan, kode, waktuBukaKlien);
				System.out.println("[SESI-KAS-BUKA] SesiKasUtil.buka() selesai -- id baris (sebelum commit)="
						+ (baru == null ? "null" : baru.getId()));
				session.getTransaction().commit();
			} catch (org.hibernate.exception.ConstraintViolationException eDup) {
				// FIX race idempotensi: dua request offline-first dgn `kode` SAMA (mis. retry sinkron
				// ganda) bisa sama-sama lolos cek cariByKode() di atas SEBELUM salah satunya commit
				// (klasik TOCTOU) -> yang kedua menabrak unique constraint sesi_kas_kasir_kode_key.
				// Sesuai niat idempotensi yg sudah didokumentasikan di atas, JANGAN anggap ini gagal --
				// baris dgn kode itu sudah ada (dibuat request "pemenang" race), kembalikan sukses apa
				// adanya seperti jalur cariByKode() normal, bukan status 91 (yang mengelabui klien
				// offline-first supaya retry lagi & bisa berulang-ulang menabrak constraint yg sama).
				try { if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				} } catch (Exception eRb) { ais.common.ErrorAuditUtil.record(eRb, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:sesiKasBuka-rollback-dup"); }
				try { session.clear(); } catch (Exception eClr) { ais.common.ErrorAuditUtil.record(eClr, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:sesiKasBuka-clear-dup"); }
				ais.database.model.inventory.SesiKasKasir menang = kode == null ? null
						: ais.action.master.koperasi.helper.SesiKasUtil.cariByKode(session, kode);
				if (menang == null) {
					throw eDup; // bukan soal duplikat `kode` -- constraint lain, tetap gagal apa adanya.
				}
				System.out.println("[SESI-KAS-BUKA] idempotensi (race dideteksi via ConstraintViolationException) -- "
						+ "kode=" + kode + " SUDAH ada (dibuat request lain), id=" + menang.getId());
				hasil.put("status", "00");
				hasil.put("id_server", menang.getId());
				return;
			}
			System.out.println("[SESI-KAS-BUKA] session.getTransaction().commit() SUKSES -- baris id="
					+ (baru == null ? "null" : baru.getId()) + " seharusnya sudah permanen di koperasi.sesi_kas_kasir.");
			hasil.put("id_server", baru == null ? JSONObject.NULL : baru.getId());
			// Diagnostik tambahan (lihat javadoc method) -- cek ulang KEBERADAAN baris yang BARU SAJA
			// di-commit, dulu pada session YANG SAMA (harus DITEMUKAN -- sekadar sanity check codepath
			// query itu sendiri benar), lalu pada session BARU yang dibuka+ditutup di SINI JUGA, di
			// thread/request yang SAMA persis dgn penulisan -- meniru apa yang dilakukan sesiKasStatus,
			// tapi TANPA melibatkan request HTTP terpisah sama sekali. Bila baris ke-2 (session baru,
			// thread sama) tetap "TIDAK DITEMUKAN", itu bukti kuat masalahnya BUKAN soal timing antar
			// request/thread -- mengarah ke sesuatu di level koneksi/database itu sendiri (mis. RLS,
			// replika baca, trigger) yang tak bisa didiagnosis lebih lanjut dari kode Java semata.
			cekUlangDiagnostikSesiKasBuka(session, id, tokoId, "SESSION SAMA (setelah commit)");
			hasil.put("status", "00");
		} catch (Exception e) {
			String pesanKonteks = "[SESI-KAS-BUKA] GAGAL -- kasir(oleh=" + id[0] + ", olehId=" + id[1] + "), toko=" + tokoId
					+ ", payload=" + request.toString();
			System.out.println(pesanKonteks + " -- exception: " + e);
			e.printStackTrace();
			ais.common.ErrorAuditUtil.record(e, "sesi-kas-buka-gagal src/ais/action/servlet/api/KantinHelper.java:sesiKasBuka -- " + pesanKonteks);
			hasil.put("status", "91");
			hasil.put("description", "Gagal membuka sesi kas karena kesalahan sistem: "
					+ (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())
					+ ". Sudah dicatat ke log error server untuk ditindaklanjuti admin.");
		} finally {
			tutupSessionPolaB(session);
			System.out.println("[SESI-KAS-BUKA] selesai, session ditutup.");
		}
		// Diagnostik tahap 2 -- SESSION BARU sepenuhnya (baru dibuka+ditutup sendiri di sini), tapi
		// MASIH di thread/request HTTP yang SAMA dgn penulisan di atas (bukan request terpisah spt
		// sesiKasStatus). Dijalankan SETELAH try/finally di atas supaya tidak mengganggu alur normal
		// (baik sukses maupun gagal) -- murni observasi tambahan, tak pernah melempar exception ke atas.
		if ("00".equals(hasil.optString("status", ""))) {
			Session sesiBaru = null;
			try {
				sesiBaru = HibernateUtil.getSessionFactory().openSession();
				cekUlangDiagnostikSesiKasBuka(sesiBaru, id, tokoId, "SESSION BARU (thread/request SAMA)");
			} catch (Exception eDiag) {
				ais.common.ErrorAuditUtil.record(eDiag,
						"auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:sesiKasBuka-diagnostik-sesibaru");
			} finally {
				tutupSessionPolaB(sesiBaru);
			}
		}
	}

	/**
	 * Diagnostik pemburu akar masalah bug "Kas Belum Dibuka tak mau hilang" (lihat javadoc {@link
	 * #sesiKasBuka}) -- mencetak apakah sesi kas TERBUKA milik kasir/toko ini BISA ditemukan lewat
	 * {@link ais.action.master.koperasi.helper.SesiKasUtil#idSesiTerbuka}, plus identitas fisik
	 * koneksi database yang dipakai ({@code current_database()}, {@code inet_server_addr()},
	 * {@code inet_server_port()}) -- supaya bila ada lebih dari satu database/replika yang terlibat
	 * (di luar dugaan kode Java, mis. load balancer level TCP/proxy DB), itu langsung kelihatan dari
	 * perbandingan log antara sesi tulis dan sesi baca, tanpa perlu akses langsung ke server database.
	 */
	private static void cekUlangDiagnostikSesiKasBuka(Session session, String[] id, Long tokoId, String label) {
		try {
			Long idTerbuka = ais.action.master.koperasi.helper.SesiKasUtil.idSesiTerbuka(session, id[0], id[1], tokoId);
			String infoKoneksi = "?";
			try {
				Object[] r = (Object[]) session
						.createSQLQuery("select cast(current_database() as text), cast(inet_server_addr() as text), cast(inet_server_port() as text)")
						.uniqueResult();
				infoKoneksi = "db=" + r[0] + ", host=" + r[1] + ", port=" + r[2];
			} catch (Exception eInfo) {
				infoKoneksi = "gagal ambil info koneksi: " + eInfo;
			}
			System.out.println("[SESI-KAS-BUKA][DIAGNOSTIK] " + label + " -- kasir(oleh=" + id[0] + ", olehId="
					+ id[1] + "), toko=" + tokoId + " -- hasil cek ulang: "
					+ (idTerbuka == null ? "TIDAK DITEMUKAN (null)" : ("DITEMUKAN id=" + idTerbuka)) + " -- " + infoKoneksi);
		} catch (Exception e) {
			System.out.println("[SESI-KAS-BUKA][DIAGNOSTIK] " + label + " -- GAGAL total: " + e);
		}
	}

	/**
	 * Fitur "Sesi Kasir" (versi JSP) -- menutup sesi kas TERBUKA milik kasir ini: menghitung
	 * tunai/non-tunai sepanjang sesi, menyimpan uang fisik yang dihitung kasir, dan mencatat selisih
	 * (uang fisik − (modal awal + tunai)) secara permanen. Menolak (status "91") bila tidak ada sesi
	 * terbuka untuk ditutup.
	 *
	 * @param request payload berisi {@code id_toko} (opsional), {@code uang_fisik} (angka),
	 *                {@code keterangan} (opsional), {@code kode} (opsional -- kode yg SAMA dipakai
	 *                saat buka, dipakai menemukan sesi yg TEPAT + idempotensi retry, lihat javadoc
	 *                {@link #sesiKasBuka}), dan {@code waktu_tutup} (opsional, ISO -- waktu tutup
	 *                SEBENARNYA di klien bila ini sinkronisasi tertunda).
	 * @param hasil   diisi {@code status="00"} + {@code selisih} bila berhasil.
	 */
	public static void sesiKasTutup(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String[] id = identitasKasir(tbmuser);
		Long tokoId = request.isNull("id_toko") ? null : Long.valueOf(request.get("id_toko").toString());
		String kode = request.isNull("kode") ? null : request.optString("kode", null);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			// Cari lewat kode dulu (lebih presisi, sesi PASTI yg dimaksud klien) -- jatuh ke pencarian
			// "sesi terbuka milik kasir ini" lama bila kode tak diisi/tak ditemukan (mis. buka belum
			// sempat tersinkron -- lihat javadoc method).
			ais.database.model.inventory.SesiKasKasir sesi = kode == null ? null
					: ais.action.master.koperasi.helper.SesiKasUtil.cariByKode(session, kode);
			if (sesi != null && ais.database.model.inventory.SesiKasKasir.STATUS_TUTUP.equals(sesi.getStatus())) {
				// Idempotensi: sesi ini SUDAH ditutup sebelumnya (retry sinkron krn respons hilang) --
				// balas hasil yg SUDAH tercatat apa adanya, JANGAN hitung ulang (waktu "sekarang" sudah
				// bukan waktu tutup sungguhan lagi, hasil hitung ulang akan keliru).
				hasil.put("status", "00");
				hasil.put("selisih", sesi.getSelisih());
				hasil.put("stokMenipis", daftarProdukStokMenipis(session, tokoId));
				return;
			}
			if (sesi == null) {
				sesi = ais.action.master.koperasi.helper.SesiKasUtil.sesiTerbuka(session, id[0], id[1], tokoId);
			}
			if (sesi == null) {
				hasil.put("status", "91");
				hasil.put("description", "Tidak ada sesi kas yang terbuka untuk ditutup.");
				return;
			}
			double uangFisik = request.optDouble("uang_fisik", 0);
			String keterangan = request.optString("keterangan", "");
			java.util.Date waktuTutupKlien = null;
			if (!request.isNull("waktu_tutup")) {
				try {
					waktuTutupKlien = Common.dateFormatInput.get().parse(request.optString("waktu_tutup", null));
				} catch (Exception eParse) {
					waktuTutupKlien = null;
				}
			}
			session.beginTransaction();
			double selisih = ais.action.master.koperasi.helper.SesiKasUtil.tutup(session, sesi, uangFisik, keterangan, waktuTutupKlien);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("selisih", selisih);
			hasil.put("stokMenipis", daftarProdukStokMenipis(session, tokoId));
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Butir 10 spesifikasi "dashboard kasir": "setelah close session stok item barang akan
	 * terkalkulasi secara otomatis... buatkan konfigurasi minimal stok barang yg mentriger jadi
	 * pemesanan barang ke gudang pusat". Dipanggil dari {@link #sesiKasTutup} supaya kasir langsung
	 * melihat daftar produk yang perlu direstok TEPAT setelah kas ditutup, tanpa langkah tambahan.
	 *
	 * <p>SENGAJA HANYA notifikasi (bukan otomatis membuat dokumen {@code asset.PengirimanGudang}) --
	 * itu ledger stok Gudang yang terpisah dari stok POS Kantin ({@code koperasi.produk.stok}) per
	 * keputusan user menunda penyatuan kedua sistem (lihat sesi Pengiriman Antar Gudang). Query ini
	 * memakai filter yang SAMA PERSIS dgn laporan "Stok Minimum / Reorder" yang sudah ada
	 * ({@code LaporanKantinUtil}, aksi laporan {@code stok_minimum}) supaya kedua tempat konsisten --
	 * di sini murni permukaan yang sama ditampilkan proaktif, bukan logika baru.</p>
	 *
	 * @param tokoId {@code null} berarti tidak ada toko diketahui -- balikan kosong (tak ada yg bisa dicek).
	 * @return array {@code {nama, stok, stokMinimum}}, maks 50 baris, terurut selisih terbesar dulu.
	 */
	private static JSONArray daftarProdukStokMenipis(Session session, Long tokoId) throws Exception {
		JSONArray arr = new JSONArray();
		if (tokoId == null) {
			return arr;
		}
		java.sql.PreparedStatement ps = session.connection().prepareStatement(
				"SELECT nama, COALESCE(stok,0), COALESCE(stok_minimum,0) FROM koperasi.produk "
						+ "WHERE toko = ? AND aktif = true AND COALESCE(stok_minimum,0) > 0 AND COALESCE(stok,0) <= COALESCE(stok_minimum,0) "
						+ "ORDER BY (COALESCE(stok_minimum,0) - COALESCE(stok,0)) DESC LIMIT 50");
		ps.setLong(1, tokoId.longValue());
		java.sql.ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			JSONObject j = new JSONObject();
			j.put("nama", rs.getString(1) == null ? "" : rs.getString(1));
			j.put("stok", rs.getDouble(2));
			j.put("stokMinimum", rs.getDouble(3));
			arr.put(j);
		}
		rs.close();
		ps.close();
		return arr;
	}

	/**
	 * Fitur "Popup Pesanan Online Baru" -- dipoll berkala (tiap ~15-20 detik) dari layar Kasir (JSP,
	 * ZK, Desktop) untuk mendeteksi pesanan yang dibuat PEMBELI SENDIRI lewat {@code toko_online.jsp}
	 * (bukan fisik di depan kasir), supaya kasir bisa langsung tahu tanpa harus berulang kali membuka
	 * tab "Pesanan" secara manual.
	 *
	 * <p><b>Cara membedakan "pesanan online dari pembeli" vs "keranjang yang ditahan kasir sendiri"
	 * (Fitur "Simpan Keranjang"):</b> KEDUANYA memakai baris {@link DraftPembelianAnggotaKoperasi}
	 * yang SAMA (tidak ada kolom "sumber" terpisah) -- satu-satunya sinyal yang bisa diandalkan adalah
	 * kolom {@code tbmuser} (siapa yang MENGIRIM draft ini): saat kasir menekan "Tahan", {@code
	 * tbmuser} adalah akun KASIR yang login; saat pembeli checkout online lewat {@code toko_online.jsp},
	 * {@code tbmuser} adalah akun PEMBELI sendiri (anggota koperasi yang login). Jadi filter di sini
	 * HANYA menyertakan draft yang {@code tbmuser}-nya punya {@code anggota_koperasi} terpasang (akun
	 * anggota, bukan akun staf/kasir) -- lihat {@code Tbmuser.getAnggotaKoperasi()}.</p>
	 *
	 * <p>Pemanggil (klien) yang menyimpan "id maksimum terakhir dilihat" dan mengirimnya balik lewat
	 * {@code sejak_id} -- method ini TIDAK menyimpan state apa pun sendiri (stateless, aman dipanggil
	 * dari banyak kasir/perangkat bersamaan tanpa saling mengganggu). Panggilan PERTAMA sebaiknya
	 * mengirim {@code sejak_id} kosong utk sekadar mengambil {@code maksId} sebagai baseline (klien
	 * TIDAK menampilkan popup utk baseline ini -- kalau ditampilkan, kasir akan dibanjiri popup utk
	 * SEMUA pesanan lama yang belum dilayani setiap kali aplikasi baru dibuka).</p>
	 *
	 * @param request payload berisi {@code id_toko} (opsional -- null = tidak dibatasi toko) dan
	 *                {@code sejak_id} (opsional -- hanya pesanan dgn id lebih besar dari ini yang
	 *                dikembalikan; kosong/0 = tidak ada yang difilter, hanya dipakai utk baseline).
	 * @param hasil   diisi {@code status="00"}, {@code maksId} (id tertinggi saat ini -- jadi
	 *                {@code sejak_id} pemanggil berikutnya), dan {@code pesanan} (array
	 *                {@code {id, kode, waktu, pembeli, barang}}, terurut id menaik, maks 30 baris).
	 */
	public static void pesananOnlineBaru(JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = request.isNull("id_toko") ? null : Long.valueOf(request.get("id_toko").toString());
		long sejakId = request.optLong("sejak_id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder sb = new StringBuilder(
					"select d.id, d.kode, to_char(d.tanggal_pembayaran,'DD-MM-YYYY HH24:MI:SS') as waktu, "
							+ "coalesce(ak.nama,'-') as pembeli, "
							+ "string_agg(coalesce(dp.nama,'-') || ' x' || CAST(coalesce(dp.qty,0) AS text), ', ') as barang "
							+ "from koperasi.draft_pembelian_anggota_koperasi d "
							+ "left join koperasi.anggota_koperasi ak on d.anggota_koperasi = ak.id "
							+ "left join public.tbmuser u on d.tbmuser = u.userid "
							+ "left join koperasi.draft_pembelian dp on dp.draft_pembelian_anggota_koperasi = d.id "
							+ "where d.lunas is null and u.anggota_koperasi is not null and d.id > :sejakId ");
			if (tokoId != null) {
				sb.append(" and d.toko = :tokoId ");
			}
			sb.append(" group by d.id, d.kode, d.tanggal_pembayaran, ak.nama order by d.id asc ");
			org.hibernate.SQLQuery q = session.createSQLQuery(sb.toString());
			q.setParameter("sejakId", Long.valueOf(sejakId));
			if (tokoId != null) {
				q.setParameter("tokoId", tokoId);
			}
			q.setMaxResults(30);

			JSONArray arr = new JSONArray();
			long maksId = sejakId;
			for (Object o : q.list()) {
				Object[] r = (Object[]) o;
				long idBaris = ((Number) r[0]).longValue();
				if (idBaris > maksId) {
					maksId = idBaris;
				}
				JSONObject j = new JSONObject();
				j.put("id", idBaris);
				j.put("kode", r[1] == null ? "" : r[1].toString());
				j.put("waktu", r[2] == null ? "" : r[2].toString());
				j.put("pembeli", r[3] == null ? "-" : r[3].toString());
				j.put("barang", r[4] == null ? "-" : r[4].toString());
				arr.put(j);
			}
			// CATATAN: bila pesanan baru yg BELUM ditampilkan melebihi batas 30 baris ini, maksId HANYA
			// maju sejauh baris yg benar-benar dikembalikan (BUKAN id tertinggi keseluruhan) -- supaya
			// baris yg "terpotong" tetap muncul di poll BERIKUTNYA (fail-safe: kasir melihat baris yg
			// sama berulang sampai backlog berkurang, bukan diam-diam kehilangan notifikasi pesanan).
			hasil.put("status", "00");
			hasil.put("maksId", maksId);
			hasil.put("pesanan", arr);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Top Up Saldo lewat POS" -- kasir mengisi saldo (tunai) member LANGSUNG dari layar Kasir,
	 * TANPA berpindah ke menu "Manajemen Saldo (Deposit)" terpisah ({@code _manajemen_topup.jsp}).
	 * Sengaja ditulis sebagai method BERTIPE eksplisit (bukan reflection-based {@code simpanDataRinci}
	 * yang dipakai layar admin itu) -- POS adalah titik masuk BARU dgn permukaan validasi lebih kecil
	 * &amp; lebih ketat drpd form admin generik, jadi field yg diterima dibatasi sengaja (member, nominal,
	 * keterangan) drpd meneruskan bentuk objek arbitrer dari klien.
	 *
	 * <p>Menghormati DUA gerbang otorisasi yang SAMA dgn layar admin (bukan gerbang baru): (1)
	 * {@link Tbmrole#getBolehEntryTopup()} milik kasir yang login -- kasir tanpa hak ini ditolak sama
	 * sekali; (2) {@link JenisAnggotaKoperasi#getBolehEntryTopupOlehAdmin()} milik jenis keanggotaan
	 * member yang dituju -- beberapa jenis anggota sengaja TIDAK boleh menerima topup lewat kasir
	 * (mis. tipe yang saldonya hanya boleh diisi lewat kanal resmi tertentu), konsisten dgn filter SQL
	 * {@code j.boleh_entry_topup_oleh_admin = true} yg membatasi datalist pencarian member di layar
	 * admin.</p>
	 *
	 * <p>Baris {@link Deposit} yang tersimpan OTOMATIS menambah saldo terhitung member (lihat
	 * {@link ais.action.master.sekolah.util.DepositHelper#hitungDeposit(AnggotaKoperasi)} -- saldo
	 * adalah SUM semua Deposit dikurangi pengeluaran, bukan kolom saldo terpisah yang perlu di-update
	 * manual) -- jadi begitu method ini sukses, panggilan {@code tabungan}/{@code saldo_member}
	 * berikutnya otomatis mencerminkan saldo baru tanpa langkah tambahan apa pun.</p>
	 *
	 * @param request payload berisi {@code id_member} (wajib), {@code nominal} (wajib, &gt; 0), dan
	 *                {@code keterangan} (opsional).
	 * @param hasil   diisi {@code status="00"} + {@code id} (id baris Deposit baru) bila berhasil;
	 *                {@code status="91"} + {@code description} penjelas bila ditolak gerbang otorisasi
	 *                atau validasi input.
	 */
	public static void topupSaldo(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		boolean bolehEntryTopup = role != null && role.getBolehEntryTopup() != null
				&& role.getBolehEntryTopup().booleanValue();
		if (!bolehEntryTopup) {
			hasil.put("status", "91");
			hasil.put("description",
					"Anda tidak memiliki hak akses untuk mengisi saldo (topup). Hubungi admin untuk mengaktifkan hak \"Boleh Entry Topup\".");
			return;
		}
		if (request.isNull("id_member") || !Common.isNumber((request.get("id_member") + "").trim())) {
			hasil.put("status", "91");
			hasil.put("description", "Member wajib dipilih.");
			return;
		}
		double nominal = request.optDouble("nominal", 0);
		if (nominal <= 0) {
			hasil.put("status", "91");
			hasil.put("description", "Nominal topup harus lebih dari 0.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			AnggotaKoperasi anggota = (AnggotaKoperasi) session.get(AnggotaKoperasi.class,
					Long.valueOf((request.get("id_member") + "").trim()));
			if (anggota == null) {
				hasil.put("status", "91");
				hasil.put("description", "Member tidak ditemukan.");
				return;
			}
			boolean bolehUntukJenisIni = anggota.getJenisAnggotaKoperasi() != null
					&& anggota.getJenisAnggotaKoperasi().getBolehEntryTopupOlehAdmin() != null
					&& anggota.getJenisAnggotaKoperasi().getBolehEntryTopupOlehAdmin().booleanValue();
			if (!bolehUntukJenisIni) {
				hasil.put("status", "91");
				hasil.put("description",
						"Jenis keanggotaan member ini tidak diizinkan menerima topup lewat kasir. Hubungi admin bila ini keliru.");
				return;
			}
			String[] idKasir = identitasKasir(tbmuser);
			ais.database.model.Deposit deposit = new ais.database.model.Deposit();
			deposit.setAnggotaKoperasi(anggota);
			deposit.setNominal(Double.valueOf(nominal));
			// Parity JSP "_manajemen_topup.jsp": waktu boleh backdate (opsional), default SEKARANG
			// spt perilaku lama bila tak dikirim -- TIDAK mengubah perilaku pemanggil existing.
			Date waktuTopup = new Date();
			if (!request.isNull("waktu") && !request.optString("waktu", "").trim().isEmpty()) {
				try {
					waktuTopup = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(request.optString("waktu"));
				} catch (Exception eParse) {
					ais.common.ErrorAuditUtil.record(eParse, "auto-audit topupSaldo-parse-waktu src/ais/action/servlet/api/KantinHelper.java");
				}
			}
			deposit.setWaktu(waktuTopup);
			if (!request.isNull("tanggal_expired") && !request.optString("tanggal_expired", "").trim().isEmpty()) {
				try {
					java.util.Date exp = new SimpleDateFormat("yyyy-MM-dd").parse(request.optString("tanggal_expired"));
					java.util.Calendar cal = java.util.Calendar.getInstance();
					cal.setTime(exp);
					cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
					cal.set(java.util.Calendar.MINUTE, 59);
					cal.set(java.util.Calendar.SECOND, 59);
					deposit.setTanggalExpired(cal.getTime());
				} catch (Exception eParse) {
					ais.common.ErrorAuditUtil.record(eParse, "auto-audit topupSaldo-parse-expired src/ais/action/servlet/api/KantinHelper.java");
				}
			}
			if (!request.isNull("jenis_pembayaran_id")) {
				deposit.setJenisPembayaran((ais.database.model.JenisPembayaran) session.get(ais.database.model.JenisPembayaran.class,
						Long.valueOf((request.get("jenis_pembayaran_id") + "").trim())));
			}
			if (!request.isNull("jenis_tabungan_id")) {
				deposit.setJenisTabungan((ais.database.model.JenisTabungan) session.get(ais.database.model.JenisTabungan.class,
						Long.valueOf((request.get("jenis_tabungan_id") + "").trim())));
			}
			deposit.setKeterangan(request.optString("keterangan", ""));
			deposit.setOleh(idKasir[0]);
			deposit.setOlehId(idKasir[1]);
			session.beginTransaction();
			session.saveOrUpdate(deposit);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", deposit.getId());
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Butir 11 spesifikasi "dashboard kasir": ganti kata sandi akun SENDIRI dari Kasir Desktop --
	 * tersedia untuk SIAPA PUN yang sedang login (kasir maupun admin), TIDAK butuh hak akses khusus
	 * (beda dari {@link #tambahAkunKasir} yg admin-only) karena ini hanya menyentuh akun milik
	 * pemanggil sendiri (diambil dari token, TIDAK PERNAH dari id yg dikirim klien -- desain IDOR-safe
	 * yg sama dgn {@code resolveTokoId}).
	 *
	 * <p><b>Cakupan saat ini</b>: hanya menangani akun kasir bertipe {@link Pedagang}
	 * ({@code tbmuser.getPedagang() != null}) -- account admin (tanpa Pedagang) belum didukung dari
	 * jalur ini (server belum punya mekanisme ganti password admin yg tercapai lewat token PosApi;
	 * yang ada baru {@code _do_ganti_password.jsp} berbasis sesi cookie web, bukan token). Admin yang
	 * perlu ganti kata sandi tetap memakai aplikasi web untuk saat ini.</p>
	 *
	 * <p><b>Penyimpanan kata sandi</b>: mengikuti persis pola existing {@code Pedagang.pass} (plaintext,
	 * sama seperti yg sudah dipakai layar web "Manajemen Akun Pedagang" -- BUKAN keputusan baru yg
	 * diperkenalkan di sini, sekadar konsisten dgn skema autentikasi yg sudah berjalan supaya kasir yg
	 * password-nya diganti dari Desktop tetap bisa login lewat jalur web/JSP yg SAMA).</p>
	 *
	 * @param request payload berisi {@code password_lama} dan {@code password_baru} (keduanya wajib).
	 * @param hasil   diisi {@code status="00"} bila berhasil; {@code status="91"} + {@code description}
	 *                bila kata sandi lama salah, input tidak lengkap, atau akun bukan tipe Pedagang.
	 */
	public static void gantiPasswordSendiri(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pedagang = tbmuser == null ? null : tbmuser.getPedagang();
		if (pedagang == null) {
			hasil.put("status", "91");
			hasil.put("description", "Ganti kata sandi akun admin belum didukung dari Kasir Desktop -- gunakan aplikasi web.");
			return;
		}
		String passwordLama = request.optString("password_lama", "");
		String passwordBaru = request.optString("password_baru", "");
		if (passwordLama.isEmpty() || passwordBaru.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Kata sandi lama dan kata sandi baru wajib diisi.");
			return;
		}
		if (passwordBaru.length() < 6) {
			hasil.put("status", "91");
			hasil.put("description", "Kata sandi baru minimal 6 karakter.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.inventory.Pedagang p = (ais.database.model.inventory.Pedagang) session
					.get(ais.database.model.inventory.Pedagang.class, pedagang.getId());
			if (p == null || p.getPass() == null || !p.getPass().equals(passwordLama)) {
				hasil.put("status", "91");
				hasil.put("description", "Kata sandi lama tidak cocok.");
				return;
			}
			session.beginTransaction();
			p.setPass(passwordBaru);
			session.saveOrUpdate(p);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Butir 11 spesifikasi "dashboard kasir": "yang dapat membuat akun adalah manager, sedangkan
	 * kasir hanya dapat melakukan login/update diri/transaksi" -- gerbang otorisasi ASALNYA memakai
	 * pola binari admin-vs-kasir ({@code tbmuser.getPedagang() == null}). DIPERLUAS (fitur "Akun
	 * Pedagang" menu Konfigurasi): kasir toko yang berstatus SUPERVISOR ({@link Pedagang#getSupervisor()})
	 * kini JUGA boleh menambah akun kasir baru -- tapi terkunci server-side ke tokonya SENDIRI
	 * (parameter {@code toko_id} dari client DIABAIKAN untuk supervisor, IDOR-safe, pola sama dgn
	 * {@code diskonSimpan}). Kasir non-supervisor tetap ditolak sama sekali, sama seperti sebelumnya.
	 *
	 * <p>Akun kasir baru = satu baris {@link Pedagang} baru (userid+pass+nama+toko+aktif) -- SAMA
	 * PERSIS dgn yg dibuat layar web "Manajemen Akun Pedagang" ({@code pedagang.jsp}), tidak ada baris
	 * {@code public.tbmuser} terpisah yg perlu dibuat (autentikasi akun Pedagang diselesaikan lewat
	 * resolusi dinamis {@code Tbmuser} dari {@code Pedagang} yg cocok saat login, bukan lewat baris
	 * tersimpan permanen -- lihat {@code Tbmuser.getUserId()}).</p>
	 *
	 * <p><b>Gap-closure "supervisor boleh membuat Supervisor lain juga, bukan cuma Kasir"</b>: field
	 * {@code supervisor} SEKARANG dihormati langsung di sini utk KEDUA jenis pemanggil (supervisor
	 * toko MAUPUN admin global) -- SEBELUMNYA selalu {@code false} apa pun permintaan client (admin
	 * harus mengangkat lewat {@link #pedagangUbah} belakangan sbg langkah terpisah). Ini AMAN thd
	 * eskalasi hak istimewa krn gerbang di atas SUDAH membatasi pemanggil hanya 2 jenis (supervisor
	 * toko ybs, ATAU admin global) -- keduanya memang SUDAH berwenang penuh atas toko yg sama (toko
	 * dikunci server-side, bukan dipercaya dari client), jadi mengizinkan mereka langsung menandai
	 * akun baru sbg supervisor bukan celah baru, hanya menghemat satu langkah terpisah.</p>
	 *
	 * @param request payload berisi {@code userid}, {@code password}, {@code nama} (semua wajib),
	 *                {@code toko_id} (wajib HANYA utk admin global -- diabaikan/dikunci utk supervisor
	 *                pedagang), {@code keterangan} (opsional), dan {@code supervisor} (opsional,
	 *                boolean -- true utk membuat akun baru INI langsung sbg Supervisor toko).
	 * @param hasil   diisi {@code status="00"} + {@code id} bila berhasil; {@code status="91"} +
	 *                {@code description} bila ditolak gerbang otorisasi, input tidak lengkap, atau
	 *                userid sudah dipakai akun lain.
	 */
	public static void tambahAkunKasir(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pedagangPembuat = tbmuser == null ? null : tbmuser.getPedagang();
		boolean supervisor = pedagangPembuat != null && Boolean.TRUE.equals(pedagangPembuat.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pedagangPembuat, pedagangPembuat == null, supervisor, "pedagang", "create")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat menambah akun kasir baru. Hubungi admin/supervisor toko Anda.");
			return;
		}
		String userid = request.optString("userid", "").trim();
		String password = request.optString("password", "");
		String nama = request.optString("nama", "").trim();
		String keterangan = request.optString("keterangan", "");
		Long tokoId;
		if (supervisor) {
			// Supervisor SELALU terkunci ke tokonya sendiri -- toko_id dari client diabaikan sama
			// sekali, tidak sekadar divalidasi, supaya tak ada celah IDOR walau client mengirim
			// toko_id toko lain.
			tokoId = pedagangPembuat.getToko() == null ? null : pedagangPembuat.getToko().getId();
		} else {
			tokoId = request.isNull("toko_id") ? null : Long.valueOf((request.get("toko_id") + "").trim());
		}
		if (userid.isEmpty() || password.isEmpty() || nama.isEmpty() || tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Userid, kata sandi, nama, dan toko wajib diisi.");
			return;
		}
		if (password.length() < 6) {
			hasil.put("status", "91");
			hasil.put("description", "Kata sandi minimal 6 karakter.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Long jumlahBentrok = (Long) session
					.createSQLQuery("SELECT COUNT(*) FROM koperasi.pedagang WHERE userid = :u")
					.setParameter("u", userid).uniqueResult();
			if (jumlahBentrok != null && jumlahBentrok.longValue() > 0) {
				hasil.put("status", "91");
				hasil.put("description", "Userid \"" + userid + "\" sudah dipakai akun lain -- pilih userid yang berbeda.");
				return;
			}
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			if (toko == null) {
				hasil.put("status", "91");
				hasil.put("description", "Toko tidak ditemukan.");
				return;
			}
			ais.database.model.inventory.Pedagang p = new ais.database.model.inventory.Pedagang();
			p.setUserid(userid);
			p.setPass(password);
			p.setNama(nama);
			p.setToko(toko);
			p.setAktif(true);
			p.setKeterangan(keterangan);
			// Lihat JavaDoc method ini soal alasan supervisor/admin boleh langsung menandai akun baru
			// sbg Supervisor di langkah ini juga (bukan lagi selalu false + langkah pedagangUbah terpisah).
			p.setSupervisor(Boolean.valueOf(request.optBoolean("supervisor", false)));
			session.beginTransaction();
			session.saveOrUpdate(p);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", p.getId());
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Akun Pedagang" (menu Konfigurasi, Kasir Desktop) -- daftar seluruh akun {@link Pedagang}
	 * milik SATU toko (toko sendiri utk kasir/supervisor, WAJIB {@code toko_id} eksplisit utk admin
	 * global -- tidak ada mode "semua toko sekaligus", konsisten dgn pola IDOR-safe lain di file ini).
	 * TIDAK ada gerbang tambahan utk method LIST ini -- siapa pun yang login (kasir biasa termasuk)
	 * boleh MELIHAT daftar akun tokonya sendiri; gerbang supervisor hanya berlaku utk aksi UBAH/TAMBAH
	 * (lihat {@link #tambahAkunKasir}/{@link #pedagangUbah}), sesuai permintaan "kalau bukan
	 * supervisor, hanya boleh lihat saja".
	 *
	 * @param hasil diisi {@code status="00"}, {@code data} (array {@code {id, userid, nama,
	 *              keterangan, aktif, supervisor}}), dan {@code bolehKelola} (boolean -- true bila
	 *              tbmuser pemanggil admin global ATAU supervisor toko ini, dipakai Desktop
	 *              menyembunyikan tombol Tambah/Ubah tanpa perlu menebak ulang aturannya di klien).
	 */
	public static void pedagangList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggil = tbmuser == null ? null : tbmuser.getPedagang();
		Long tokoId;
		if (pemanggil != null) {
			tokoId = pemanggil.getToko() == null ? null : pemanggil.getToko().getId();
		} else {
			tokoId = request.isNull("toko_id") ? null : Long.valueOf((request.get("toko_id") + "").trim());
		}
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		boolean bolehKelola = pemanggil == null || Boolean.TRUE.equals(pemanggil.getSupervisor());

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			// Fix bug: kolom akses_kasir/akses_ringkasan/dst di bawah PERNAH direncanakan
			// (lihat komentar lama yg menyebut "Pedagang.getAksesKasir()") tapi TIDAK PERNAH
			// benar-benar dibuat -- baik sbg kolom @Column di Pedagang.java maupun via
			// hbm2ddl -- krn desain akhirnya beralih ke Tbmrole.ebisnisMenu (menu "Hak Akses"
			// per grup pengguna, lihat EbisnisMenuKatalog). Query lama ini SELALU gagal
			// (Postgres "column does not exist") setiap kali tab "Akun Pengguna" dibuka,
			// tampil sbg "Terjadi kesalahan pada sistem" -- field-nya jg dikonfirmasi TIDAK
			// dipakai sama sekali oleh klien Flutter manapun (grep bersih), jadi dihapus
			// (bukan diperbaiki jadi kolom sungguhan) drpd menghidupkan lagi fitur yatim yg
			// tak pernah py jalur simpan (pedagangUbah tak pernah menulis kolom ini).
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT id, userid, nama, COALESCE(keterangan,''), COALESCE(aktif,true), COALESCE(supervisor,false) "
							+ "FROM koperasi.pedagang WHERE toko = ? ORDER BY nama ASC");
			ps.setLong(1, tokoId);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				// FIX: jangan panggil session.get() di dalam while(rs.next()) —
				// Hibernate menerbitkan SQL baru pada koneksi JDBC yang sama sehingga
				// menutup ResultSet aktif → PSQLException: This ResultSet is closed.
				// Nilai supervisor sudah tersedia dari kolom 6 SQL (COALESCE(supervisor,false)).
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("userid", rs.getString(2));
				j.put("nama", rs.getString(3));
				j.put("keterangan", rs.getString(4));
				j.put("aktif", rs.getBoolean(5));
				j.put("supervisor", rs.getBoolean(6));
				arr.put(j);
			}
			rs.close();
			ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("bolehKelola", bolehKelola);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Gerbang CRUD granular per grup pengguna ({@code Tbmrole.ebisnisMenu.crud}, lihat
	 * {@code ais.common.EbisnisMenuKatalog}) -- dipakai SEBAGAI JALUR TAMBAHAN (OR), BUKAN pengganti,
	 * gerbang {@code Pedagang.getSupervisor()}/admin-global yang sudah ada di setiap method di bawah:
	 * admin global atau akun yang di-flag supervisor di level toko TETAP selalu boleh (perilaku lama
	 * tidak berubah); yang BARU adalah role (Tbmrole) sekarang JUGA bisa memberi izin Create/Update/
	 * Delete/Approve/Reject per menu secara granular TANPA harus menjadikan akunnya supervisor toko
	 * penuh (mis. role "Staf Gudang" diberi Create+Update Produk tapi tidak Delete). "Supervisor" pada
	 * grid Tbmrole tetap SATU toggle blanket ({@code EbisnisMenuKatalog.bolehAksi} sudah menangani
	 * bypass ini) -- bukan baris grid tersendiri, sesuai keputusan "Supervisor = ALL Checked Akses".
	 */
	private static boolean bolehAksiCrud(Tbmuser tbmuser, ais.database.model.inventory.Pedagang pemanggil,
			boolean adminGlobal, boolean supervisorToko, String kunciMenu, String aksi) {
		if (adminGlobal || supervisorToko) {
			return true;
		}
		ais.database.model.Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		if (role == null) {
			return false;
		}
		return ais.common.EbisnisMenuKatalog.bolehAksi(
				ais.common.EbisnisMenuKatalog.urai(role.getEbisnisMenu()), kunciMenu, aksi);
	}

	/**
	 * Fitur "Akun Pedagang" -- ubah akun {@link Pedagang} yang SUDAH ADA (nama/keterangan/aktif,
	 * opsional kata sandi baru). Gerbang SAMA dgn {@link #tambahAkunKasir} (admin global atau
	 * supervisor toko), PLUS IDOR-guard tambahan: supervisor HANYA boleh mengubah pedagang di
	 * TOKONYA SENDIRI (dicek ulang di sini, bukan cuma dipercaya dari klien).
	 *
	 * <p>Field {@code supervisor} bisa diubah admin GLOBAL **atau** supervisor toko ybs (gap-closure
	 * "supervisor boleh kelola status Supervisor pedagang lain di tokonya sendiri") -- IDOR-guard toko
	 * di atas SUDAH memastikan supervisor cuma bisa menyentuh baris pedagang di TOKONYA SENDIRI, jadi
	 * mengizinkan mereka jg mengubah field ini bukan celah baru (toko sama = wewenang sama, sama spt
	 * field nama/keterangan/aktif/password_baru lain di method ini yg sudah bisa diubah supervisor).
	 * Kasir non-supervisor tetap sama sekali tidak bisa memanggil method ini (gerbang di atas).</p>
	 *
	 * @param request payload: {@code id} (wajib), {@code nama}, {@code keterangan}, {@code aktif},
	 *                {@code password_baru} (opsional, min 6 karakter bila diisi), {@code supervisor}
	 *                (opsional, boolean).
	 */
	public static void pedagangUbah(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggil = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobal = pemanggil == null;
		boolean supervisor = pemanggil != null && Boolean.TRUE.equals(pemanggil.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggil, adminGlobal, supervisor, "pedagang", "update")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengubah akun pedagang.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "Akun pedagang tidak ditemukan.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.inventory.Pedagang p = (ais.database.model.inventory.Pedagang) session
					.get(ais.database.model.inventory.Pedagang.class, id);
			if (p == null) {
				hasil.put("status", "91");
				hasil.put("description", "Akun pedagang tidak ditemukan.");
				return;
			}
			if (supervisor) {
				Long tokoSupervisor = pemanggil.getToko() == null ? null : pemanggil.getToko().getId();
				Long tokoTarget = p.getToko() == null ? null : p.getToko().getId();
				if (tokoSupervisor == null || !tokoSupervisor.equals(tokoTarget)) {
					hasil.put("status", "91");
					hasil.put("description", "Akun pedagang ini bukan milik toko Anda.");
					return;
				}
			}
			if (request.has("nama") && !request.isNull("nama")) {
				String nama = request.optString("nama", "").trim();
				if (!nama.isEmpty()) p.setNama(nama);
			}
			if (request.has("keterangan")) p.setKeterangan(request.optString("keterangan", ""));
			if (request.has("aktif")) p.setAktif(request.optBoolean("aktif", true));
			if (request.has("supervisor")) {
				p.setSupervisor(request.optBoolean("supervisor", false));
			}
			String passwordBaru = request.optString("password_baru", "");
			if (!passwordBaru.isEmpty()) {
				if (passwordBaru.length() < 6) {
					hasil.put("status", "91");
					hasil.put("description", "Kata sandi baru minimal 6 karakter.");
					return;
				}
				p.setPass(passwordBaru);
			}
			session.beginTransaction();
			session.saveOrUpdate(p);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Katalog Barang" (Kasir Desktop/Android) -- tambah/ubah {@link Produk}, dipakai layar
	 * yang mereplikasi field inti form modal produk di POS Online JSP ({@code
	 * WEB-INF/baru/modul/kantin/barang/index.jsp}). Gerbang SAMA dgn {@link #tokoProfilSimpan}
	 * (admin global ATAU supervisor toko), PLUS IDOR-guard: supervisor HANYA boleh menambah/mengubah
	 * produk milik TOKONYA SENDIRI (dicek ulang di sini, bukan cuma dipercaya dari klien) -- produk
	 * BARU otomatis dikunci ke toko supervisor, produk EXISTING diverifikasi ulang kepemilikannya.
	 *
	 * <p>SENGAJA hanya mencakup field INTI (kode, nama, keterangan, harga beli/jual, stok, aktif,
	 * boleh-jual-walau-minus, kategori) -- resep bahan baku, unggah gambar, dan penautan
	 * {@code masterAsset} TIDAK direplikasi di sini (di luar cakupan "tambah/edit katalog cepat" versi
	 * kasir; tetap dikelola lewat form JSP lengkap bila perlu).</p>
	 *
	 * @param request payload: {@code id} (opsional, kosong/null = produk baru), {@code kode}
	 *                (wajib), {@code barcode} (opsional, UPC/barcode fisik kemasan -- lihat JavaDoc
	 *                {@code Produk.getBarcode()}), {@code nama} (wajib), {@code keterangan}, {@code harga_beli},
	 *                {@code harga_jual}, {@code stok}, {@code aktif} (def:true),
	 *                {@code izinkan_jual_minus_stok} (def:false), {@code kategori_id} (opsional,
	 *                id {@link JenisProduk} -- lihat daftar kategori dari aksi {@code katalog}),
	 *                {@code toko_id} (wajib utk admin global saat membuat produk baru, diabaikan/
	 *                dikunci utk supervisor).
	 * @param hasil   diisi {@code status="00"} + {@code id} bila berhasil.
	 */
	public static void produkSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggil = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobal = pemanggil == null;
		boolean supervisor = pemanggil != null && Boolean.TRUE.equals(pemanggil.getSupervisor());
		String aksiProdukSimpan = request.isNull("id") ? "create" : "update";
		if (!bolehAksiCrud(tbmuser, pemanggil, adminGlobal, supervisor, "produk", aksiProdukSimpan)) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola katalog barang.");
			return;
		}
		String kode = request.optString("kode", "").trim();
		String nama = request.optString("nama", "").trim();
		if (kode.isEmpty() || nama.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Kode dan nama produk wajib diisi.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		String barcodeUntukKunci = request.optString("barcode", "").trim();
		// Gap-closure permintaan user eksplisit: tolak kalau Kode, Barcode, DAN Nama SEKALIGUS kosong
		// (lihat JavaDoc ProdukKunciUnikUtil.adaIdentitasProduk) -- SUDAH tertutup lewat gerbang
		// kode/nama wajib di atas, guard ini dipasang eksplisit sbg lapis kedua yg berdiri sendiri.
		if (!ais.common.ProdukKunciUnikUtil.adaIdentitasProduk(kode, barcodeUntukKunci, nama)) {
			hasil.put("status", "91");
			hasil.put("description", "Kode, Barcode, dan Nama produk tidak boleh kosong semuanya.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Produk p;
			boolean baru = (id == null);
			if (baru) {
				Long tokoId;
				if (supervisor) {
					tokoId = pemanggil.getToko() == null ? null : pemanggil.getToko().getId();
				} else {
					tokoId = request.isNull("toko_id") ? null : Long.valueOf((request.get("toko_id") + "").trim());
				}
				if (tokoId == null) {
					hasil.put("status", "91");
					hasil.put("description", "Toko tidak diketahui.");
					return;
				}
				Toko toko = (Toko) session.get(Toko.class, tokoId);
				if (toko == null) {
					hasil.put("status", "91");
					hasil.put("description", "Toko tidak ditemukan.");
					return;
				}
				// Gap-closure "insert produk dgn kunci unik yg sama harus jadi UPDATE, bukan error DB"
				// (lihat JavaDoc Produk.hitungKunciUnik + MIGRASI/InitIndex.initIndexProdukKunciUnik) --
				// SEBELUM insert baru, cek dulu apakah produk dgn kombinasi kode+barcode+nama+toko yg
				// SAMA (dinormalisasi) SUDAH ADA -- kalau ada, redirect ke UPDATE baris itu (self-heal,
				// SAMA PERSIS perilaku upsert yg sudah dipakai Impor Excel), supaya klik "Tambah Produk"
				// dgn data yg kebetulan identik tidak pernah menabrak UNIQUE INDEX kunci_unik.
				String kunciCekBaru = ais.common.ProdukKunciUnikUtil.hitung(kode,
						barcodeUntukKunci.isEmpty() ? null : barcodeUntukKunci, nama, tokoId);
				@SuppressWarnings("unchecked")
				List<Produk> calonSamaBaru = session.createCriteria(Produk.class)
						.add(Restrictions.eq("kunciUnik", kunciCekBaru))
						.addOrder(Order.asc("id")).setMaxResults(1).list();
				if (!calonSamaBaru.isEmpty()) {
					p = calonSamaBaru.get(0);
					baru = false;
				} else {
					p = new Produk();
					p.setToko(toko);
				}
			} else {
				p = (Produk) session.get(Produk.class, id);
				if (p == null) {
					hasil.put("status", "91");
					hasil.put("description", "Produk tidak ditemukan.");
					return;
				}
				if (supervisor) {
					Long tokoSupervisor = pemanggil.getToko() == null ? null : pemanggil.getToko().getId();
					Long tokoProduk = p.getToko() == null ? null : p.getToko().getId();
					if (tokoSupervisor == null || !tokoSupervisor.equals(tokoProduk)) {
						hasil.put("status", "91");
						hasil.put("description", "Produk ini bukan milik toko Anda.");
						return;
					}
				}
				// Gap-closure sama spt di atas, versi UPDATE: kalau perubahan kode/barcode/nama yg diminta
				// bikin kunci_unik-nya bentrok dgn produk LAIN (id berbeda) di toko ini, tolak dgn pesan
				// jelas SEBELUM commit -- drpd baris ini tetap disimpan lalu menabrak UNIQUE INDEX (error
				// DB mentah yg membingungkan) saat commit.
				Long tokoIdCekUpdate = p.getToko() == null ? null : p.getToko().getId();
				String kunciCekUpdate = ais.common.ProdukKunciUnikUtil.hitung(kode,
						barcodeUntukKunci.isEmpty() ? null : barcodeUntukKunci, nama, tokoIdCekUpdate);
				@SuppressWarnings("unchecked")
				List<Produk> calonSamaUpdate = session.createCriteria(Produk.class)
						.add(Restrictions.eq("kunciUnik", kunciCekUpdate))
						.add(Restrictions.ne("id", id))
						.addOrder(Order.asc("id")).setMaxResults(1).list();
				if (!calonSamaUpdate.isEmpty()) {
					hasil.put("status", "91");
					hasil.put("description", "Kombinasi Kode + Barcode + Nama ini sudah dipakai produk lain (ID "
							+ calonSamaUpdate.get(0).getId() + ") di toko ini. Ubah salah satu kolom agar berbeda, atau kelola produk itu langsung.");
					return;
				}
			}
			p.setKode(kode);
			// Barcode fisik kemasan (opsional, TERPISAH dari kode internal toko di atas) -- lihat
			// JavaDoc Produk.getBarcode(). Sebelumnya field ini hanya bisa diisi lewat impor Excel;
			// gap-closure ini menyambungkannya jg ke form Tambah/Ubah Produk Desktop/Android/ZK.
			String barcode = request.optString("barcode", "").trim();
			p.setBarcode(barcode.isEmpty() ? null : barcode);
			p.setNama(nama);
			// Gap-closure "kunci_unik tetap kosong walau baris baru disimpan" -- @PrePersist/@PreUpdate
			// (Produk.hitungKunciUnik) TERNYATA TIDAK diandalkan di setup Hibernate 3.6 native project ini
			// (bootstrap via AnnotationConfiguration polos di HibernateUtil, BUKAN JPA EntityManager --
			// callback lifecycle JPA tidak terjamin terpasang otomatis di sana). Diset EKSPLISIT di sini
			// sbg sumber kebenaran SEBENARNYA -- hook di entity dibiarkan sbg cadangan tak berbahaya.
			p.setKunciUnik(ais.common.ProdukKunciUnikUtil.hitung(kode, barcode.isEmpty() ? null : barcode, nama,
					p.getToko() == null ? null : p.getToko().getId()));
			p.setKeterangan(request.optString("keterangan", ""));
			double hargaBeliDiminta = request.has("harga_beli") ? request.optDouble("harga_beli", 0) : (p.getHargaBeli() == null ? 0 : p.getHargaBeli());
			p.setHargaJual(request.has("harga_jual") ? request.optDouble("harga_jual", 0) : (p.getHargaJual() == null ? 0 : p.getHargaJual()));
			p.setStok(request.has("stok") ? request.optDouble("stok", 0) : (p.getStok() == null ? 0 : p.getStok()));
			p.setAktif(!request.has("aktif") || request.optBoolean("aktif", true));
			p.setIzinkanJualMinusStok(request.optBoolean("izinkan_jual_minus_stok", false));
			if (request.has("kategori_id") && !request.isNull("kategori_id")) {
				Long kategoriId = Long.valueOf((request.get("kategori_id") + "").trim());
				ais.database.model.inventory.JenisProduk jenis = (ais.database.model.inventory.JenisProduk) session
						.get(ais.database.model.inventory.JenisProduk.class, kategoriId);
				p.setJenisProduk(jenis);
			} else if (request.has("kategori_id")) {
				p.setJenisProduk(null);
			}

			// Bahan Baku (Resep) & HPP otomatis -- gap-closure Desktop/Android, padanan JSP
			// barang/index.jsp (blok "Bahan Baku (Resep) & HPP"): SAMA PERSIS perilakunya -- bila resep
			// terisi, hargaBeli produk ini SELALU ditimpa oleh total HPP hasil hitung (Σ qty x harga
			// beku tiap bahan, BUKAN dihitung ulang live dari harga_beli bahan saat ini -- snapshot
			// "harga" di tiap baris resep dikunci sejak baris itu ditambahkan di klien, sama persis
			// field {@code Produk.bahanBaku} JavaDoc & alur bbAdd() JSP), TERLEPAS dari apa pun nilai
			// harga_beli yg (mungkin) ikut dikirim klien -- resep adalah sumber kebenaran begitu ada isi.
			if (request.has("bahan_baku") && !request.isNull("bahan_baku")) {
				JSONArray resep = request.getJSONArray("bahan_baku");
				if (resep.length() > 0) {
					double hpp = 0;
					for (int i = 0; i < resep.length(); i++) {
						JSONObject baris = resep.getJSONObject(i);
						hpp += baris.optDouble("qty", 0) * baris.optDouble("harga", 0);
					}
					p.setBahanBaku(resep.toString());
					p.setHargaBeli(hpp);
				} else {
					p.setBahanBaku(null);
					p.setHargaBeli(hargaBeliDiminta);
				}
			} else {
				p.setHargaBeli(hargaBeliDiminta);
			}

			session.beginTransaction();
			if (baru) {
				session.save(p);
			} else {
				session.saveOrUpdate(p);
			}
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", p.getId());
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** Urutan+label kolom Excel katalog barang dipakai BERSAMA oleh {@link #produkEksporExcel} (menulis)
	 * dan {@link #produkImporExcel} (mencari kolom berdasarkan label, bukan posisi tetap -- lihat
	 * {@link #cariIndeksKolom}) -- sengaja identik dengan format akunting umum ("Daftar Barang dan
	 * Jasa") yang sudah dipakai user supaya file yang sudah ada bisa diunggah tanpa diubah dulu. */
	private static final String[] KOLOM_EXCEL_PRODUK = { "No", "Kode", "UPC/Barcode", "Kategori", "Nama Barang",
			"Nama Pemasok Utama", "Satuan", "Kts", "Def. Hrg. Jual Sa", "Nilai Satuan", "Nilai Total" };

	/**
	 * Fitur "Unduh Excel" (layar Produk, khusus supervisor) -- mengekspor SELURUH katalog produk milik
	 * satu toko ke format {@code .xlsx} yang SAMA PERSIS dengan format "Daftar Barang dan Jasa" yang
	 * sudah dipakai user (kolom No/Kode/UPC/Kategori/Nama Barang/Nama Pemasok Utama/Satuan/Kts/Def.
	 * Hrg. Jual Sa/Nilai Satuan/Nilai Total) -- supaya file yang diunduh di sini bisa diedit lalu
	 * diunggah kembali lewat {@link #produkImporExcel} tanpa perlu menata ulang kolom.
	 *
	 * <p>Dikirim sebagai base64 di field JSON {@code fileBase64} (pola sama dengan
	 * {@code PosApi.prosesLaporanPdf}/{@code pdfBase64}) -- BUKAN langsung ke
	 * {@code HttpServletResponse}, karena panggilan ini lewat {@code fetch} JSON biasa dari proses
	 * Electron/Android, bukan navigasi/unduhan browser langsung. Sisi klien yang menulis file fisik
	 * (dialog simpan native di Desktop, Filesystem API di Android).</p>
	 *
	 * <p>Gerbang SAMA dengan {@link #produkSimpan} (admin global ATAU supervisor toko) -- unduh
	 * dianggap bagian dari "mengelola katalog", bukan sekadar melihat, supaya kasir biasa tidak bisa
	 * mengekspor daftar harga/stok toko diam-diam.</p>
	 */
	public static void produkEksporExcel(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggil = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobal = pemanggil == null;
		boolean supervisor = pemanggil != null && Boolean.TRUE.equals(pemanggil.getSupervisor());
		if (!adminGlobal && !supervisor) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengunduh katalog barang.");
			return;
		}
		Long tokoId;
		if (supervisor) {
			tokoId = pemanggil.getToko() == null ? null : pemanggil.getToko().getId();
		} else {
			tokoId = request.isNull("toko_id") ? null : Long.valueOf((request.get("toko_id") + "").trim());
		}
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			if (toko == null) {
				hasil.put("status", "91");
				hasil.put("description", "Toko tidak ditemukan.");
				return;
			}
			org.hibernate.Criteria kriteria = session.createCriteria(Produk.class).add(Restrictions.eq("toko", toko));
			// Checkbox "Hanya Aktif" (layar Katalog Barang, default tercentang) -- SEBELUMNYA ekspor
			// ini SELALU menyertakan produk Non-Aktif juga, sehingga file yg diunduh utk dicocokkan ke
			// Accurate ikut memuat barang yg sudah dinonaktifkan (bukan gambaran katalog "hidup" toko).
			if (request.optBoolean("hanya_aktif", false)) {
				kriteria.add(Restrictions.eq("aktif", true));
			}
			@SuppressWarnings("unchecked")
			List<Produk> daftar = kriteria.addOrder(Order.asc("kode")).list();

			XSSFWorkbook wb = new XSSFWorkbook();
			XSSFSheet sheet = wb.createSheet("Daftar Barang dan Jasa");
			sheet.setDefaultColumnWidth(20);
			// Kolom A/B SENGAJA dibiarkan kosong (margin) -- data ditulis mulai kolom C, PERSIS posisi yang
			// dibaca {@link #deteksiKolomExcelProdukFormatAccurate} (fixed C..M) -- supaya Unduh Excel lalu
			// Unggah Excel lagi (format Accurate) round-trip tanpa perlu menata ulang kolom.
			final int OFFSET_KOLOM_ACCURATE = 2;
			XSSFRow headerRow = sheet.createRow(0);
			for (int i = 0; i < KOLOM_EXCEL_PRODUK.length; i++) {
				headerRow.createCell(i + OFFSET_KOLOM_ACCURATE).setCellValue(KOLOM_EXCEL_PRODUK[i]);
			}
			int r = 1;
			int no = 1;
			for (Produk p : daftar) {
				XSSFRow row = sheet.createRow(r++);
				row.createCell(0 + OFFSET_KOLOM_ACCURATE).setCellValue(no++);
				row.createCell(1 + OFFSET_KOLOM_ACCURATE).setCellValue(p.getKode() == null ? "" : p.getKode());
				row.createCell(2 + OFFSET_KOLOM_ACCURATE).setCellValue(p.getBarcode() == null ? "" : p.getBarcode());
				row.createCell(3 + OFFSET_KOLOM_ACCURATE).setCellValue(p.getJenisProduk() == null ? "" : nvl(p.getJenisProduk().getNama()));
				row.createCell(4 + OFFSET_KOLOM_ACCURATE).setCellValue(p.getNama() == null ? "" : p.getNama());
				row.createCell(5 + OFFSET_KOLOM_ACCURATE).setCellValue(p.getPemasok() == null ? "" : nvl(p.getPemasok().getNama()));
				row.createCell(6 + OFFSET_KOLOM_ACCURATE).setCellValue(p.getSatuan() == null ? "" : nvl(p.getSatuan().getNama()));
				row.createCell(7 + OFFSET_KOLOM_ACCURATE).setCellValue(p.getStok() == null ? 0 : p.getStok());
				row.createCell(8 + OFFSET_KOLOM_ACCURATE).setCellValue(p.getHargaJual() == null ? 0 : p.getHargaJual());
				row.createCell(9 + OFFSET_KOLOM_ACCURATE).setCellValue(p.getHargaBeli() == null ? 0 : p.getHargaBeli());
				row.createCell(10 + OFFSET_KOLOM_ACCURATE)
						.setCellValue((p.getStok() == null ? 0 : p.getStok()) * (p.getHargaBeli() == null ? 0 : p.getHargaBeli()));
			}

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			wb.write(bos);
			hasil.put("status", "00");
			hasil.put("fileBase64", java.util.Base64.getEncoder().encodeToString(bos.toByteArray()));
			hasil.put("namaFile", "Katalog-" + (toko.getKode() == null ? toko.getId() : toko.getKode()) + ".xlsx");
			hasil.put("total", daftar.size());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static String nvl(String s) {
		return s == null ? "" : s;
	}

	/**
	 * Mencari indeks kolom (basis-0) di baris header berdasarkan LABEL (bukan posisi tetap) -- dicek
	 * dengan {@code contains} case-insensitive supaya varian label yang sedikit berbeda (mis. "Nama
	 * Barang" vs "Nama Produk") tetap terbaca, dan supaya file yang diunduh lewat
	 * {@link #produkEksporExcel} maupun file "Daftar Barang dan Jasa" asli (dengan baris judul/cover
	 * di atasnya) SAMA-SAMA bisa diunggah lewat {@link #produkImporExcel} tanpa penataan ulang.
	 * @return indeks kolom, atau -1 bila label tidak ditemukan di baris ini.
	 */
	private static int cariIndeksKolom(XSSFRow row, String label) {
		if (row == null) return -1;
		String labelUpper = label.toUpperCase();
		for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
			XSSFCell cell = row.getCell(c);
			String isi = Common.getCellContent(cell).trim().toUpperCase();
			if (isi.length() > 0 && isi.contains(labelUpper)) return c;
		}
		return -1;
	}

	/**
	 * Hasil deteksi baris header + posisi kolom pada file Excel katalog barang -- dipakai bersama oleh
	 * {@link #produkImporExcelPreview} (satu-satunya tempat file di-parse; {@link #produkImporExcelKomit}
	 * menerima baris yang SUDAH diedit user, bukan file lagi).
	 */
	private static final class IndeksKolomExcelProduk {
		int barisHeader = -1;
		int kode = -1, barcode = -1, kategori = -1, nama = -1, pemasok = -1, satuan = -1, stok = -1,
				hargaJual = -1, hargaBeli = -1;
		/** Kolom "No" urut -- HANYA diisi (>=0) oleh {@link #deteksiKolomExcelProdukFormatAccurate}, dipakai
		 * mendeteksi baris total ("Total No") supaya baris rekap di akhir file tidak ikut terimpor. -1
		 * berarti tak dipakai (format lain tak punya konsep "baris total" yang perlu dideteksi). */
		int kolomNomorUrut = -1;
	}

	/**
	 * Deteksi kolom KHUSUS "Format Accurate" (gap-closure lanjutan -- deteksi berbasis label+validasi
	 * angka {@link #deteksiKolomExcelProduk} MASIH gagal pada file nyata toko: Stok/Harga Jual/Harga
	 * Beli tetap tak terbaca walau kolom teks lain benar). Permintaan user EKSPLISIT: JANGAN deteksi
	 * per-kolom sama sekali -- struktur file "Daftar Barang dan Jasa" hasil ekspor Accurate SELALU
	 * SAMA (kolom A/B margin kosong, C..M data, urutan tetap), jadi field cukup dibaca dari POSISI
	 * ABSOLUT begitu baris header ditemukan, TANPA mencocokkan label per kolom atau memvalidasi isi
	 * angka apa pun. Satu-satunya hal yang masih "dicari" adalah BARIS header (posisinya bisa
	 * berbeda tergantung berapa baris judul/cover perusahaan di atasnya) -- ditandai baris yang
	 * punya sel PERSIS "Kode" DAN sel lain mengandung "Barcode" (menutupi varian "UPC/Barcode" dst).
	 *
	 * <p>Mapping kolom (0-basis, huruf Excel di komentar): C(2)=No (bukan disimpan, cuma dipakai
	 * mendeteksi baris total), D(3)=Kode, E(4)=Barcode, F(5)=Kategori, G(6)=Nama Barang, H(7)=Nama
	 * Pemasok Utama, I(8)=Satuan, J(9)=Kts (stok), K(10)=Def. Hrg. Jual Sa (harga jual), L(11)=Nilai
	 * Satuan (harga beli/nilai persediaan per satuan), M(12)=Nilai Total -- SENGAJA TIDAK dibaca,
	 * sistem ini tak pernah menyimpan nilai turunan (stok x harga beli dihitung ulang saat
	 * dibutuhkan, konsisten dgn seluruh kode lain).</p>
	 */
	private static IndeksKolomExcelProduk deteksiKolomExcelProdukFormatAccurate(XSSFSheet sheet) {
		IndeksKolomExcelProduk idx = new IndeksKolomExcelProduk();
		int batasBaris = Math.min(20, sheet.getLastRowNum());
		int barisHeader = -1;
		for (int r = 0; r <= batasBaris; r++) {
			XSSFRow row = sheet.getRow(r);
			if (row == null) continue;
			boolean adaKode = false, adaBarcode = false;
			for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
				String isi = Common.getCellContent(row.getCell(c)).trim().toUpperCase();
				if (isi.equals("KODE")) adaKode = true;
				if (isi.contains("BARCODE")) adaBarcode = true;
			}
			if (adaKode && adaBarcode) { barisHeader = r; break; }
		}
		if (barisHeader < 0) { idx.barisHeader = -1; return idx; }
		idx.barisHeader = barisHeader;
		idx.kolomNomorUrut = 2; // C
		idx.kode = 3;           // D
		idx.barcode = 4;        // E
		idx.kategori = 5;       // F
		idx.nama = 6;           // G
		idx.pemasok = 7;        // H
		idx.satuan = 8;         // I
		idx.stok = 9;           // J (Kts)
		idx.hargaJual = 10;     // K (Def. Hrg. Jual Sa)
		idx.hargaBeli = 11;     // L (Nilai Satuan)
		return idx;
	}

	// Daftar SINONIM label per kolom (urut prioritas) -- gap-closure permintaan "otomatis deteksi mana
	// kira-kira QTY/harga jual/harga beli/kode dst": makin banyak istilah dunia nyata yg ditampung
	// (Accurate, Excel ekspor sendiri, istilah Inggris umum), makin kecil peluang gagal krn sekadar
	// beda penyebutan. Array, BUKAN satu label -- {@link #cariIndeksKolomSinonim} mencoba SEMUA sampai
	// ketemu, {@link #cariUlangDanValidasiKolomAngka} malah mencoba SEMUA KANDIDAT yg cocok bertahap
	// sampai salah satu LOLOS validasi isi (lihat javadoc method itu kenapa validasi isi perlu).
	private static final String[] LABEL_KODE = { "KODE BARANG", "KODE", "SKU", "ITEM CODE" };
	private static final String[] LABEL_BARCODE = { "BARCODE/PLU", "UPC", "BARCODE", "PLU" };
	private static final String[] LABEL_KATEGORI = { "KATEGORI", "GOLONGAN", "JENIS BARANG", "CATEGORY" };
	private static final String[] LABEL_NAMA = { "NAMA BARANG", "NAMA PRODUK", "NAMA", "DESKRIPSI", "DESCRIPTION", "ITEM NAME" };
	private static final String[] LABEL_PEMASOK = { "NAMA PEMASOK", "PEMASOK", "SUPPLIER", "VENDOR" };
	private static final String[] LABEL_SATUAN = { "SATUAN", "UOM", "UNIT" };
	private static final String[] LABEL_STOK = { "KTS", "QTY", "QUANTITY", "KUANTITAS", "STOK", "STOCK", "JUMLAH" };
	private static final String[] LABEL_HARGA_JUAL = { "HRG. JUAL", "HARGA JUAL", "SELLING PRICE", "HRG JUAL", "JUAL" };
	private static final String[] LABEL_HARGA_BELI = { "NILAI SATUAN", "HARGA BELI", "HRG. BELI", "HRG BELI", "PURCHASE PRICE", "COST", "MODAL", "HPP", "BELI" };

	/** Coba SEMUA sinonim berurutan di SATU baris, kecocokan PERTAMA menang. @return indeks kolom, atau -1. */
	private static int cariIndeksKolomSinonim(XSSFRow row, String[] labelSinonim) {
		for (String label : labelSinonim) {
			int c = cariIndeksKolom(row, label);
			if (c >= 0) return c;
		}
		return -1;
	}

	/**
	 * Apakah kolom {@code kolom} pada baris {@code barisDataAwal..lastRow} SUNGGUHAN berisi angka --
	 * gap-closure "kolom KETEMU (index bukan -1, label cocok) tapi nilainya 0 semua stlh diimpor".
	 * Root cause SEBENARNYA kasus itu BUKAN label tak ditemukan (index tetap -1, sudah ditangani versi
	 * sebelumnya) -- melainkan label KEBETULAN cocok di kolom yg SALAH (mis. kolom tersembunyi berisi
	 * sisa teks lama, atau dua kolom berbeda kebetulan sama2 mengandung kata "Jual"), jadi index-nya
	 * valid (>=0) tapi menunjuk kolom yg SALAH -- makanya `kolomTidakDitemukan` (peringatan versi lama)
	 * tidak pernah terpicu meski hasilnya tetap salah. Validasi ini mengecek ISI SUNGGUHAN pada baris
	 * DATA (bukan header): kalau mayoritas sel yg terisi TIDAK berbentuk angka, kolom itu DITOLAK
	 * (pemanggil lalu mencoba kandidat/sinonim lain, lihat {@link #cariUlangDanValidasiKolomAngka}).
	 * @return true bila minimal 3 baris tersampel terisi DAN >=70% di antaranya berbentuk angka murni.
	 */
	private static boolean kolomBerisiAngkaValid(XSSFSheet sheet, int kolom, int barisDataAwal, int lastRow) {
		if (kolom < 0) return false;
		int terisi = 0, cocokAngka = 0;
		int batasSampel = Math.min(lastRow, barisDataAwal + 40);
		for (int r = barisDataAwal; r <= batasSampel; r++) {
			String isi = Common.getCellContent(Common.getCell(sheet, kolom, r)).trim();
			if (isi.isEmpty()) continue;
			terisi++;
			// Angka murni (boleh minus/desimal titik-atau-koma) -- SENGAJA tidak memakai parseAngkaAman
			// di sini (itu sudah "memaksa" jadi angka walau isinya teks campur, tidak cocok dipakai
			// validasi krn nyaris selalu "berhasil" walau isinya sebenarnya bukan angka).
			if (isi.matches("^-?[0-9]+([.,][0-9]+)?$")) cocokAngka++;
		}
		return terisi >= 3 && cocokAngka >= Math.ceil(terisi * 0.7);
	}

	/**
	 * Cari kolom ANGKA (Stok/Harga Jual/Harga Beli) dari daftar sinonim, MENCOBA SETIAP kandidat
	 * kolom yg cocok label-nya di baris manapun (0..batasBarisHeader) SAMPAI salah satu LOLOS
	 * {@link #kolomBerisiAngkaValid}. Beda dari kolom teks (Kode/Nama/dst) yg cukup kecocokan label
	 * PERTAMA (tidak ada "bentuk" universal utk memvalidasi isi teks bebas) -- kolom angka WAJIB
	 * divalidasi krn di sinilah gap-closure "Stok/Harga Jual selalu 0 walau kolomnya 'ketemu'" (lihat
	 * javadoc {@link #kolomBerisiAngkaValid}).
	 */
	private static int cariUlangDanValidasiKolomAngka(XSSFSheet sheet, int batasBarisHeader,
			int barisDataAwal, int lastRow, String[] labelSinonim) {
		java.util.LinkedHashSet<Integer> kandidat = new java.util.LinkedHashSet<Integer>();
		for (int r = 0; r <= batasBarisHeader; r++) {
			XSSFRow row = sheet.getRow(r);
			if (row == null) continue;
			for (String label : labelSinonim) {
				int c = cariIndeksKolom(row, label);
				if (c >= 0) kandidat.add(c);
			}
		}
		for (int kolom : kandidat) {
			if (kolomBerisiAngkaValid(sheet, kolom, barisDataAwal, lastRow)) return kolom;
		}
		return -1;
	}

	/**
	 * Mencari SETIAP label kolom SECARA INDEPENDEN di baris 0-15 (bukan cuma di SATU baris tempat
	 * "Kode" pertama ditemukan) -- gap-closure "Stok Baru selalu 0 stlh Unggah Excel walau file berisi
	 * angka yg benar" (kolom lain spt Kode/Nama/Kategori/Pemasok terbaca normal, cuma Stok yg selalu
	 * 0). Beberapa alat sumber file (Accurate, atau file yg pernah disunting manual di Excel/WPS -- lihat
	 * "Tinjau Impor Katalog" yg dilaporkan pengguna) kadang menaruh header dlm BARIS YANG TIDAK PERSIS
	 * SAMA satu sama lain (mis. kolom-kolom kiri di baris 4, kolom kuantitas/harga di baris 5 krn baris
	 * judul/cover di atasnya tidak rata) -- versi SEBELUMNYA mengunci SEMUA kolom ke baris "Kode"
	 * ditemukan pertama kali, jadi kolom yg headernya kebetulan ada di baris lain gagal total.
	 *
	 * <p><b>Ronde kedua (gap-closure lanjutan, laporan "Stok TETAP 0 walau header 1 baris rata")</b> --
	 * kolom TEKS (Kode/Barcode/Kategori/Nama/Pemasok/Satuan) dideteksi dulu spt biasa (kecocokan label
	 * pertama, tanpa validasi -- teks bebas tak punya "bentuk" utk divalidasi). Kolom ANGKA (Stok/Harga
	 * Jual/Harga Beli) BARU dicari SETELAH baris data diketahui, lewat daftar SINONIM lebih luas
	 * ({@link #LABEL_STOK} dst) DAN divalidasi isinya sungguhan angka ({@link
	 * #cariUlangDanValidasiKolomAngka}) -- menutup celah "label kebetulan cocok tapi kolomnya salah"
	 * yang TIDAK tertangkap oleh deteksi berbasis label semata.</p>
	 */
	private static IndeksKolomExcelProduk deteksiKolomExcelProduk(XSSFSheet sheet) {
		IndeksKolomExcelProduk idx = new IndeksKolomExcelProduk();
		int batasBaris = Math.min(15, sheet.getLastRowNum());
		int barisTerbawah = -1;
		for (int r = 0; r <= batasBaris; r++) {
			XSSFRow row = sheet.getRow(r);
			if (row == null) continue;
			int c;
			if (idx.kode < 0 && (c = cariIndeksKolomSinonim(row, LABEL_KODE)) >= 0) { idx.kode = c; barisTerbawah = r; }
			if (idx.barcode < 0 && (c = cariIndeksKolomSinonim(row, LABEL_BARCODE)) >= 0) { idx.barcode = c; barisTerbawah = Math.max(barisTerbawah, r); }
			if (idx.kategori < 0 && (c = cariIndeksKolomSinonim(row, LABEL_KATEGORI)) >= 0) { idx.kategori = c; barisTerbawah = Math.max(barisTerbawah, r); }
			if (idx.nama < 0 && (c = cariIndeksKolomSinonim(row, LABEL_NAMA)) >= 0) { idx.nama = c; barisTerbawah = Math.max(barisTerbawah, r); }
			if (idx.pemasok < 0 && (c = cariIndeksKolomSinonim(row, LABEL_PEMASOK)) >= 0) { idx.pemasok = c; barisTerbawah = Math.max(barisTerbawah, r); }
			if (idx.satuan < 0 && (c = cariIndeksKolomSinonim(row, LABEL_SATUAN)) >= 0) { idx.satuan = c; barisTerbawah = Math.max(barisTerbawah, r); }
		}
		if (idx.kode < 0 || idx.nama < 0) {
			idx.barisHeader = -1;
			return idx;
		}
		int lastRow = sheet.getLastRowNum();
		int barisDataAwal = barisTerbawah + 1;
		idx.stok = cariUlangDanValidasiKolomAngka(sheet, batasBaris, barisDataAwal, lastRow, LABEL_STOK);
		idx.hargaJual = cariUlangDanValidasiKolomAngka(sheet, batasBaris, barisDataAwal, lastRow, LABEL_HARGA_JUAL);
		idx.hargaBeli = cariUlangDanValidasiKolomAngka(sheet, batasBaris, barisDataAwal, lastRow, LABEL_HARGA_BELI);
		idx.barisHeader = barisTerbawah;
		return idx;
	}

	/**
	 * Gerbang bersama {@link #produkImporExcelPreview}/{@link #produkImporExcelKomit}/
	 * {@link #produkGridEksporExcel} (admin global ATAU supervisor toko) + resolusi {@code tokoId} --
	 * SAMA PERSIS dgn {@link #produkSimpan}.
	 * @return {@code tokoId} terkunci, atau {@code null} bila ditolak (hasil sudah diisi status="91").
	 */
	private static Long gerbangDanTokoImporProduk(Tbmuser tbmuser, JSONObject request, JSONObject hasil,
			String pesanTolak) throws Exception {
		ais.database.model.inventory.Pedagang pemanggil = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobal = pemanggil == null;
		boolean supervisor = pemanggil != null && Boolean.TRUE.equals(pemanggil.getSupervisor());
		if (!adminGlobal && !supervisor) {
			hasil.put("status", "91");
			hasil.put("description", pesanTolak);
			return null;
		}
		Long tokoId;
		if (supervisor) {
			tokoId = pemanggil.getToko() == null ? null : pemanggil.getToko().getId();
		} else {
			tokoId = request.isNull("toko_id") ? null : Long.valueOf((request.get("toko_id") + "").trim());
		}
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return null;
		}
		return tokoId;
	}

	/**
	 * Fitur "Unggah Excel" langkah 1/2 (layar Produk, khusus supervisor) -- PARSE SAJA, TIDAK menulis
	 * apa pun ke database. Membaca file {@code .xlsx} yang cocok format {@link #KOLOM_EXCEL_PRODUK},
	 * mencocokkan tiap baris terhadap katalog toko SAAT INI (stok lama, kategori/pemasok/satuan yang
	 * sudah dikenal) lalu mengembalikan array baris siap-ditinjau supaya supervisor bisa memeriksa/
	 * mengedit di layar sebelum benar-benar disimpan lewat {@link #produkImporExcelKomit} (permintaan
	 * user: "munculkan dulu popup konfirmasi... tabel... baru tombol Simpan/Batal").
	 *
	 * @param request payload: {@code file_base64} (wajib), {@code toko_id} (wajib utk admin global).
	 * @param hasil   diisi {@code status="00"} + {@code baris} (array
	 *                {@code {no,kode,barcode,nama,kategoriId,kategoriNama,pemasokId,pemasokNama,
	 *                satuanId,satuanNama,stokBaru,stokLama,hargaJual,hargaBeli,produkId,baru}}),
	 *                {@code daftarKategori}/{@code daftarPemasok}/{@code daftarSatuan} (array
	 *                {@code {id,nama}}, opsi utk dropdown/datalist di layar review), {@code tokoId}.
	 */
	public static void produkImporExcelPreview(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = gerbangDanTokoImporProduk(tbmuser, request, hasil,
				"Hanya admin/manager atau supervisor toko yang dapat mengunggah katalog barang.");
		if (tokoId == null) return;

		String fileBase64 = request.optString("file_base64", "");
		if (fileBase64.trim().isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "File Excel tidak dikirim.");
			return;
		}
		byte[] bytes;
		try {
			bytes = java.util.Base64.getDecoder().decode(fileBase64);
		} catch (Exception e) {
			hasil.put("status", "91");
			hasil.put("description", "File Excel tidak valid (gagal decode).");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			if (toko == null) {
				hasil.put("status", "91");
				hasil.put("description", "Toko tidak ditemukan.");
				return;
			}

			XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes));
			XSSFSheet sheet = wb.getSheetAt(0);
			// "format" (gap-closure fitur pilih-format Unggah/Unggah Excel) -- "accurate" (default, satu-
			// satunya format tersedia saat ini) memakai pemetaan kolom TETAP {@link
			// #deteksiKolomExcelProdukFormatAccurate} (permintaan user eksplisit: jangan deteksi per-kolom),
			// format lain (belum ada) tetap memakai deteksi sinonim+validasi angka lama sbg jalur cadangan.
			String formatExcel = request.optString("format", "accurate");
			IndeksKolomExcelProduk idx = "accurate".equalsIgnoreCase(formatExcel)
					? deteksiKolomExcelProdukFormatAccurate(sheet)
					: deteksiKolomExcelProduk(sheet);
			if (idx.barisHeader < 0 || idx.kode < 0 || idx.nama < 0) {
				hasil.put("status", "91");
				hasil.put("description",
						"Format Excel tidak dikenali -- kolom \"Kode\" dan \"Nama Barang\" wajib ada di salah satu baris pertama.");
				return;
			}
			// Gap-closure "Stok Baru selalu 0 tanpa pemberitahuan apa pun" -- kalau kolom Stok/Harga TIDAK
			// ditemukan sama sekali di file ini, SETIAP baris otomatis dibaca 0 utk kolom itu (lihat di
			// bawah) TANPA tanda apa pun sebelumnya -- kalau supervisor tidak jeli, klik "Simpan" bisa
			// menimpa stok/harga asli jadi 0 utk SELURUH katalog. Daftar ini dikirim ke klien (Desktop
			// MAUPUN Android, sama-sama memanggil aksi server ini) supaya bisa ditampilkan sbg peringatan
			// tegas SEBELUM tombol Simpan bisa ditekan, bukan baru ketahuan setelah data sudah tertimpa.
			JSONArray kolomTidakDitemukan = new JSONArray();
			if (idx.stok < 0) kolomTidakDitemukan.put("Stok/Kts");
			if (idx.hargaJual < 0) kolomTidakDitemukan.put("Harga Jual");
			if (idx.hargaBeli < 0) kolomTidakDitemukan.put("Harga Beli/Nilai Satuan");
			hasil.put("kolomTidakDitemukan", kolomTidakDitemukan);

			@SuppressWarnings("unchecked")
			List<JenisProduk> semuaKategori = session.createCriteria(JenisProduk.class).addOrder(Order.asc("nama")).list();
			Map<String, Long> petaKategori = new HashMap<String, Long>();
			JSONArray kategoriArr = new JSONArray();
			for (JenisProduk j : semuaKategori) {
				if (j.getNama() == null) continue;
				petaKategori.put(j.getNama().toUpperCase(), j.getId());
				JSONObject jo = new JSONObject(); jo.put("id", j.getId()); jo.put("nama", j.getNama()); kategoriArr.put(jo);
			}
			@SuppressWarnings("unchecked")
			List<PemasokProduk> semuaPemasok = session.createCriteria(PemasokProduk.class).addOrder(Order.asc("nama")).list();
			Map<String, Long> petaPemasok = new HashMap<String, Long>();
			JSONArray pemasokArr = new JSONArray();
			for (PemasokProduk p : semuaPemasok) {
				if (p.getNama() == null) continue;
				petaPemasok.put(p.getNama().toUpperCase(), p.getId());
				JSONObject jo = new JSONObject(); jo.put("id", p.getId()); jo.put("nama", p.getNama()); pemasokArr.put(jo);
			}
			@SuppressWarnings("unchecked")
			List<SatuanProduk> semuaSatuan = session.createCriteria(SatuanProduk.class).addOrder(Order.asc("nama")).list();
			Map<String, Long> petaSatuan = new HashMap<String, Long>();
			JSONArray satuanArr = new JSONArray();
			for (SatuanProduk s : semuaSatuan) {
				if (s.getNama() == null) continue;
				petaSatuan.put(s.getNama().toUpperCase(), s.getId());
				JSONObject jo = new JSONObject(); jo.put("id", s.getId()); jo.put("nama", s.getNama()); satuanArr.put(jo);
			}

			@SuppressWarnings("unchecked")
			List<Produk> semuaProdukToko = session.createCriteria(Produk.class).add(Restrictions.eq("toko", toko)).list();
			Map<String, Produk> petaProdukToko = new HashMap<String, Produk>();
			for (Produk p : semuaProdukToko) if (p.getKode() != null) petaProdukToko.put(p.getKode().trim().toUpperCase(), p);

			JSONArray barisArr = new JSONArray();
			int lastRow = sheet.getLastRowNum();
			int no = 0;
			for (int r = idx.barisHeader + 1; r <= lastRow; r++) {
				XSSFRow row = sheet.getRow(r);
				if (row == null) continue;
				// Baris "Total No" (rekap di akhir file "Daftar Barang dan Jasa") -- HANYA dicek utk format
				// Accurate (kolomNomorUrut>=0, lihat JavaDoc deteksiKolomExcelProdukFormatAccurate); begitu
				// ketemu, berhenti total (bukan skip) krn baris SETELAHNYA bukan lagi data barang/jasa.
				if (idx.kolomNomorUrut >= 0) {
					String noTeks = Common.getCellContent(Common.getCell(sheet, idx.kolomNomorUrut, r)).trim().toUpperCase();
					if (noTeks.contains("TOTAL")) break;
				}
				String kode = Common.getCellContent(Common.getCell(sheet, idx.kode, r)).trim();
				String nama = Common.getCellContent(Common.getCell(sheet, idx.nama, r)).trim();
				if (kode.isEmpty() || nama.isEmpty()) continue;
				no++;

				String barcode = idx.barcode >= 0 ? Common.getCellContent(Common.getCell(sheet, idx.barcode, r)).trim() : "";
				String kategoriNama = idx.kategori >= 0 ? Common.getCellContent(Common.getCell(sheet, idx.kategori, r)).trim() : "";
				String pemasokNama = idx.pemasok >= 0 ? Common.getCellContent(Common.getCell(sheet, idx.pemasok, r)).trim() : "";
				String satuanNama = idx.satuan >= 0 ? Common.getCellContent(Common.getCell(sheet, idx.satuan, r)).trim() : "";
				double stokBaru = idx.stok >= 0 ? parseAngkaAman(Common.getCellContent(Common.getCell(sheet, idx.stok, r))) : 0;
				double hargaJual = idx.hargaJual >= 0 ? parseAngkaAman(Common.getCellContent(Common.getCell(sheet, idx.hargaJual, r))) : 0;
				double hargaBeli = idx.hargaBeli >= 0 ? parseAngkaAman(Common.getCellContent(Common.getCell(sheet, idx.hargaBeli, r))) : 0;

				Produk existing = petaProdukToko.get(kode.toUpperCase());
				double stokLama = existing == null ? 0 : (existing.getStok() == null ? 0 : existing.getStok());

				JSONObject b = new JSONObject();
				b.put("no", no);
				b.put("kode", kode);
				b.put("barcode", barcode);
				b.put("nama", nama);
				b.put("kategoriNama", kategoriNama);
				b.put("kategoriId", petaKategori.get(kategoriNama.toUpperCase()));
				b.put("pemasokNama", pemasokNama);
				b.put("pemasokId", petaPemasok.get(pemasokNama.toUpperCase()));
				b.put("satuanNama", satuanNama);
				b.put("satuanId", petaSatuan.get(satuanNama.toUpperCase()));
				b.put("stokBaru", stokBaru);
				b.put("stokLama", stokLama);
				b.put("hargaJual", hargaJual);
				b.put("hargaBeli", hargaBeli);
				b.put("produkId", existing == null ? JSONObject.NULL : existing.getId());
				b.put("baru", existing == null);
				barisArr.put(b);
			}

			hasil.put("status", "00");
			hasil.put("baris", barisArr);
			hasil.put("daftarKategori", kategoriArr);
			hasil.put("daftarPemasok", pemasokArr);
			hasil.put("daftarSatuan", satuanArr);
			hasil.put("tokoId", tokoId);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Fitur "Unggah Excel" langkah 2/2 (layar Produk, khusus supervisor) -- KOMIT baris yang SUDAH
	 * ditinjau/diedit user di layar review (hasil {@link #produkImporExcelPreview}, TIDAK membaca
	 * file Excel lagi di sini). Upsert per baris dikunci per {@code kode} DALAM toko caller (bukan
	 * {@code produkId} yang dikirim klien -- SENGAJA diabaikan/dicari ulang server-side via kode+toko
	 * supaya supervisor toko A tidak bisa menimpa produk toko B walau id-nya ditebak/dipalsukan).
	 *
	 * <p><b>Gap-closure "impor Excel jangan sampai bikin duplikat lagi"</b>: SEBELUMNYA pencarian
	 * produk yang sudah ada HANYA lewat {@code kode} persis -- kalau baris Excel punya {@code kode}
	 * yang (sedikit) berbeda dari kode produk yang sudah tersimpan (mis. format ulang dari
	 * vendor/re-export) tapi {@code barcode} atau {@code nama}-nya SAMA, baris itu diam-diam dianggap
	 * PRODUK BARU -- persis skenario yang menghasilkan baris duplikat yang lalu harus dibersihkan
	 * manual lewat "Bersihkan Produk Duplikat". Sekarang pencarian jatuh bertingkat: {@code kode}
	 * persis dulu (paling otoritatif) -> kalau tak ketemu DAN barcode terisi, coba {@code barcode}
	 * persis -> kalau masih tak ketemu, coba {@code nama} persis (case/spasi-insensitive). Begitu
	 * ketemu lewat jalur mana pun, baris dianggap UPDATE ke produk itu (termasuk kode-nya ikut
	 * diperbarui ke nilai baru dari Excel), BUKAN insert baru -- stok pun ikut benar (ditimpa lewat
	 * StokOpname resmi ke baris yang sama, bukan tersebar ke baris baru). Peta kode/barcode/nama
	 * DIPERBARUI tiap baris diproses (bukan cuma dibaca sekali di awal) supaya baris-baris LAIN dalam
	 * batch impor yang SAMA juga saling mencegah duplikat satu sama lain, bukan cuma vs data lama.</p>
	 *
	 * <p>Perilaku upsert produk, cari-atau-buat kategori/pemasok/satuan, dan pencatatan kolom stok sbg
	 * {@link ais.database.model.inventory.StokOpname} resmi (BUKAN overwrite langsung) SAMA PERSIS dgn
	 * {@link #produkImporExcelPreview}'s pemanggil sebelumnya -- lihat catatan lengkap di situ.</p>
	 *
	 * <p><b>Opname HANYA dicatat bila ada selisih.</b> Sebelumnya method ini SELALU membuat baris
	 * {@code StokOpname} utk setiap baris Excel walau stok baru == stok lama (mis. baris yg cuma
	 * mengubah harga, stoknya tak berubah) -- membanjiri riwayat opname dgn baris selisih=0 yg tak
	 * berguna. Sekarang stok lama (dari {@code Produk.getStok()} SEBELUM baris ini disentuh, 0 utk
	 * produk baru) dibandingkan dgn stok baru dari Excel; opname HANYA dipanggil kalau keduanya
	 * berbeda -- kalau sama, baris ditandai {@code aksiStok="tidak_ada_perubahan"} dan dilewati tanpa
	 * membuat baris opname sama sekali.</p>
	 *
	 * <p><b>Laporan per-baris.</b> Selain ringkasan agregat (dibuat/diperbarui/dst, dipertahankan utk
	 * kompatibilitas mundur), {@code hasil.baris} SEKARANG berisi SATU entri utk SETIAP baris input
	 * (bukan cuma 50 baris error pertama spt {@code error} lama) -- {@code {no, kode, nama, status
	 * ("berhasil"/"gagal"/"dilewati"), pesan, teknis, produkBaru, kategoriBaru, pemasokBaru,
	 * satuanBaru, stokLama, stokBaru, selisih, aksiStok}} -- dipakai klien (Desktop) utk membangun
	 * laporan hasil impor yg bisa ditampilkan/diunduh (lihat JavaDoc {@code produk-renderer.js}
	 * fungsi tampilkanLaporanImpor).</p>
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin global), {@code baris} (wajib, array
	 *                {@code {kode,barcode,nama,kategoriNama,pemasokNama,satuanNama,stokBaru,hargaJual,
	 *                hargaBeli}} -- bentuk field SAMA dgn keluaran {@link #produkImporExcelPreview},
	 *                nilai boleh sudah diedit user).
	 * @param hasil   diisi {@code status="00"} + ringkasan {@code total, dibuat, diperbarui, dilewati,
	 *                kategoriBaru, pemasokBaru, satuanBaru, stokDiopname, error} DAN {@code baris}
	 *                (array detail per-baris, lihat di atas).
	 */
	private static final int BATCH_FLUSH_IMPOR_PRODUK = 300;

	/**
	 * Gap-closure "deadlock Postgres saat impor Excel" -- ditemukan dari log produksi:
	 * {@code ERROR: deadlock detected ... while updating tuple ... in relation "produk"} saat
	 * {@link #produkImporExcelKomitSatuPercobaan}'s {@code session.getTransaction().commit()}
	 * dipanggil. Akar masalahnya BUKAN bug logika (data tetap benar), tapi 2 transaksi yang SAMA-
	 * SAMA meng-UPDATE baris {@code koperasi.produk} yang tumpang tindih dalam URUTAN BERBEDA
	 * (mis. 2 impor Excel berjalan bersamaan, atau satu impor besar berbarengan dgn transaksi
	 * checkout Kasir yang jg menyentuh produk yg sama) -- Postgres MENDETEKSI siklus tunggu-menunggu
	 * ini dan SENGAJA membatalkan salah satu transaksi (SQLState {@code 40P01}) supaya sistem tidak
	 * macet selamanya; ini perilaku NORMAL/diharapkan di bawah konkurensi tinggi, BUKAN korupsi data.
	 * SEBELUMNYA exception ini polos menembus ke klien sbg "kesalahan sistem" generik dan SELURUH
	 * batch impor (bisa ribuan baris) gagal total tanpa ada percobaan ulang otomatis.
	 *
	 * <p>Solusi standar utk deadlock (bukan hanya di sini -- pola umum semua sistem transaksional
	 * berkonkurensi tinggi): DETEKSI kegagalan spesifik ini lalu ULANGI SELURUH transaksi dari awal
	 * (bukan sebagian -- transaksi yg dibatalkan Postgres TIDAK bisa dilanjutkan sebagian, harus
	 * mulai baru). Method ini membungkus {@link #produkImporExcelKomitSatuPercobaan} dgn hingga 3
	 * percobaan, jeda singkat (bertahap: 300ms/600ms/900ms) di antara percobaan supaya transaksi
	 * pesaing sempat selesai duluan -- pada percobaan berikutnya urutan lock biasanya sudah berbeda
	 * (transaksi lain sudah commit/rollback), jadi deadlock yg sama nyaris tidak pernah berulang.
	 * Kegagalan LAIN (bukan deadlock) TETAP langsung dilempar tanpa diulang, spt sebelumnya.</p>
	 */
	public static void produkImporExcelKomit(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		int percobaanMaks = 3;
		for (int percobaan = 1; percobaan <= percobaanMaks; percobaan++) {
			try {
				produkImporExcelKomitSatuPercobaan(tbmuser, request, hasil);
				return;
			} catch (Exception e) {
				if (!merupakanDeadlockPostgres(e) || percobaan >= percobaanMaks) {
					throw e;
				}
				ais.common.ErrorAuditUtil.record(e, "produkImporExcelKomit deadlock -- percobaan " + percobaan
						+ "/" + percobaanMaks + " gagal, mencoba ulang seluruh batch");
				try {
					Thread.sleep(300L * percobaan);
				} catch (InterruptedException eSela) {
					Thread.currentThread().interrupt();
					throw e;
				}
			}
		}
	}

	/** @return true bila {@code e} (atau salah satu cause-nya) adalah deadlock Postgres (SQLState {@code 40P01}). */
	private static boolean merupakanDeadlockPostgres(Throwable e) {
		Throwable cur = e;
		while (cur != null) {
			if (cur instanceof java.sql.SQLException && "40P01".equals(((java.sql.SQLException) cur).getSQLState())) {
				return true;
			}
			cur = cur.getCause();
		}
		return false;
	}

	/**
	 * Gap-closure "kenapa masih bisa error 'duplicate key kunci_unik', harusnya sudah dicegah?" --
	 * deteksi SPESIFIK pelanggaran UNIQUE INDEX {@code idx_produk_kunci_unik} (lihat
	 * {@code InitIndex.initIndexProdukKunciUnik}), BUKAN sembarang unique_violation (23505) lain --
	 * pola sama {@link #merupakanDeadlockPostgres} (jalan-terus sepanjang rantai cause krn Hibernate
	 * membungkus {@code PSQLException} asli dgn beberapa lapis {@code ConstraintViolationException}/
	 * {@code JDBCException}). Dicek by NAMA CONSTRAINT di pesan error (BUKAN cuma SQLState 23505 saja
	 * -- barcode/kode/dll bisa saja punya unique constraint lain di masa depan, jangan sampai
	 * tertukar dgn pelanggaran itu & disangka aman utk auto-redirect jadi update).
	 */
	private static boolean merupakanPelanggaranKunciUnikProduk(Throwable e) {
		Throwable cur = e;
		while (cur != null) {
			boolean sqlStateCocok = cur instanceof java.sql.SQLException && "23505".equals(((java.sql.SQLException) cur).getSQLState());
			String pesan = cur.getMessage();
			if (sqlStateCocok && pesan != null && pesan.contains("idx_produk_kunci_unik")) return true;
			cur = cur.getCause();
		}
		return false;
	}

	/**
	 * Query LANGSUNG ke database (BUKAN peta di memori, yg bisa basi -- lihat pemanggil) utk baris
	 * produk yg SUDAH memiliki {@code kunci_unik} tertentu di toko ini. Dipakai HANYA sbg jalur
	 * self-heal setelah percobaan insert bentrok dgn UNIQUE INDEX -- kalau ADA lebih dari satu baris
	 * (semestinya mustahil begitu index-nya aktif, tapi bisa terjadi SEMENTARA sebelum index berhasil
	 * dipasang krn data lama belum bersih) id TERKECIL yang dipilih, konsisten dgn aturan survivor di
	 * {@link #produkDuplikatHapus}.
	 * @return id baris yg cocok, atau {@code null} bila (anehnya) tak ketemu sama sekali.
	 */
	private static Long cariProdukIdByKunciUnik(Session session, long tokoId, String kunciUnik) throws Exception {
		@SuppressWarnings("unchecked")
		List<Produk> cocok = session.createCriteria(Produk.class)
				.add(Restrictions.eq("kunciUnik", kunciUnik))
				.addOrder(Order.asc("id")).setMaxResults(1).list();
		return cocok.isEmpty() ? null : cocok.get(0).getId();
	}

	private static void produkImporExcelKomitSatuPercobaan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = gerbangDanTokoImporProduk(tbmuser, request, hasil,
				"Hanya admin/manager atau supervisor toko yang dapat menyimpan katalog barang.");
		if (tokoId == null) return;

		JSONArray barisArr = request.optJSONArray("baris");
		if (barisArr == null || barisArr.length() == 0) {
			hasil.put("status", "91");
			hasil.put("description", "Tidak ada baris untuk disimpan.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			if (toko == null) {
				hasil.put("status", "91");
				hasil.put("description", "Toko tidak ditemukan.");
				return;
			}

			Map<String, Long> petaKategori = new HashMap<String, Long>();
			@SuppressWarnings("unchecked")
			List<JenisProduk> semuaKategori = session.createCriteria(JenisProduk.class).list();
			for (JenisProduk j : semuaKategori) if (j.getNama() != null) petaKategori.put(j.getNama().toUpperCase(), j.getId());

			Map<String, Long> petaPemasok = new HashMap<String, Long>();
			@SuppressWarnings("unchecked")
			List<PemasokProduk> semuaPemasok = session.createCriteria(PemasokProduk.class).list();
			for (PemasokProduk p : semuaPemasok) if (p.getNama() != null) petaPemasok.put(p.getNama().toUpperCase(), p.getId());

			Map<String, Long> petaSatuan = new HashMap<String, Long>();
			@SuppressWarnings("unchecked")
			List<SatuanProduk> semuaSatuan = session.createCriteria(SatuanProduk.class).list();
			for (SatuanProduk s : semuaSatuan) if (s.getNama() != null) petaSatuan.put(s.getNama().toUpperCase(), s.getId());

			Map<String, Long> petaProdukToko = new HashMap<String, Long>();
			Map<String, Long> petaProdukTokoBarcode = new HashMap<String, Long>();
			Map<String, Long> petaProdukTokoNama = new HashMap<String, Long>();
			// Gap-closure "baris impor Excel bikin duplikat walau kode/barcode/nama sekilas beda" (lihat
			// JavaDoc Produk.hitungKunciUnik) -- lapis KEEMPAT/terakhir cocokkan, dinormalisasi (tanda
			// baca/spasi/huruf besar-kecil diabaikan) supaya tetap menemukan baris yg SEBENARNYA sama tapi
			// lolos dari 3 lapis kode/barcode/nama persis di atas.
			//
			// PENTING (masukan user): kunci_unik DIHITUNG ULANG di sini dari kode/barcode/nama baris ITU
			// SENDIRI -- SENGAJA TIDAK membaca kolom p.getKunciUnik() tersimpan (yg bisa NULL/basi utk
			// baris LAMA yg belum pernah disimpan ulang sejak kolom ini ada, lihat JavaDoc Produk.kunciUnik).
			// Ini menjamin peta SELALU LENGKAP utk SELURUH baris toko ini SEJAK AWAL method (proaktif,
			// dicek SEBELUM percobaan insert), bukan cuma mengandalkan kolom DB yg mungkin kosong --
			// mengurangi drastis peluang percobaan insert benar2 menabrak UNIQUE INDEX (jalur self-heal
			// {@link #merupakanPelanggaranKunciUnikProduk} di bawah jadi murni jaring pengaman terakhir
			// utk kasus SANGAT jarang, bukan jalur utama). Disimpan sbg id (bukan objek Produk penuh) --
			// referensi entity bisa jadi TIDAK VALID lagi setelah session.clear() (dipanggil tiap 300 baris
			// di bawah), jadi tetap perlu session.get() ulang saat dipakai; menyimpan objek penuh cuma
			// menambah beban memori utk ribuan baris tanpa manfaat nyata.
			Map<String, Long> petaProdukTokoKunciUnik = new HashMap<String, Long>();
			@SuppressWarnings("unchecked")
			List<Produk> semuaProdukToko = session.createCriteria(Produk.class).add(Restrictions.eq("toko", toko)).list();
			for (Produk p : semuaProdukToko) {
				if (p.getKode() != null && !p.getKode().trim().isEmpty()) petaProdukToko.put(p.getKode().trim().toUpperCase(), p.getId());
				if (p.getBarcode() != null && !p.getBarcode().trim().isEmpty()) petaProdukTokoBarcode.put(p.getBarcode().trim().toUpperCase(), p.getId());
				if (p.getNama() != null && !p.getNama().trim().isEmpty()) petaProdukTokoNama.put(p.getNama().trim().toUpperCase(), p.getId());
				String kunciSegar = ais.common.ProdukKunciUnikUtil.hitung(p.getKode(), p.getBarcode(), p.getNama(), tokoId);
				if (!kunciSegar.isEmpty()) petaProdukTokoKunciUnik.put(kunciSegar, p.getId());
			}

			int dibuat = 0, diperbarui = 0, dilewati = 0, kategoriBaru = 0, pemasokBaru = 0, satuanBaru = 0, stokDiopname = 0;
			JSONArray errorArr = new JSONArray();
			JSONArray barisHasilArr = new JSONArray();
			String oleh = tbmuser == null ? "impor-excel-katalog" : tbmuser.getUserId();

			session.beginTransaction();
			for (int i = 0; i < barisArr.length(); i++) {
				JSONObject bh = new JSONObject();
				bh.put("no", i + 1);
				// Dihoist di luar try (bukan dideklarasikan di dalam spt sebelumnya) supaya blok catch
				// di bawah bisa membaca/membatalkan bookkeeping in-memory baris ini kalau baris ini
				// gagal -- lihat catatan lengkap di titik pembuatan savepoint & di blok catch.
				String kategoriNama = "", pemasokNama = "", satuanNama = "";
				boolean kategoriBaruBarisIni = false, pemasokBaruBarisIni = false, satuanBaruBarisIni = false;
				java.sql.Savepoint spBarisImpor = null;
				try {
					JSONObject b = barisArr.getJSONObject(i);
					String kode = b.optString("kode", "").trim();
					String nama = b.optString("nama", "").trim();
					String barcodeUntukCekIdentitas = b.optString("barcode", "").trim();
					bh.put("kode", kode);
					bh.put("nama", nama);
					if (kode.isEmpty() || nama.isEmpty()) {
						dilewati++;
						bh.put("status", "dilewati");
						bh.put("pesan", "Dilewati: kolom Kode dan/atau Nama Produk kosong.");
						bh.put("aksiStok", "-");
						barisHasilArr.put(bh);
						continue;
					}
					// Gap-closure permintaan user eksplisit: tolak kalau Kode, Barcode, DAN Nama SEKALIGUS
					// kosong (lihat JavaDoc ProdukKunciUnikUtil.adaIdentitasProduk) -- SUDAH tertutup lewat
					// gerbang kode/nama di atas, dipasang eksplisit di sini sbg lapis kedua yg berdiri sendiri.
					if (!ais.common.ProdukKunciUnikUtil.adaIdentitasProduk(kode, barcodeUntukCekIdentitas, nama)) {
						dilewati++;
						bh.put("status", "dilewati");
						bh.put("pesan", "Dilewati: Kode, Barcode, dan Nama Produk tidak boleh kosong semuanya.");
						bh.put("aksiStok", "-");
						barisHasilArr.put(bh);
						continue;
					}

					// Savepoint SETELAH gerbang kode/nama kosong (baris kosong tak pernah menyentuh DB,
					// tak perlu savepoint) -- WAJIB krn tanpa flush per-baris (lihat titik flush di
					// bawah), kegagalan DB (mis. barcode dobel) baru ketahuan saat commit() di akhir
					// SELURUH batch, meracuni transaksi Postgres shg SEMUA baris setelahnya ikut gagal
					// walau datanya sendiri benar. Rollback-ke-savepoint (bukan rollback transaksi utuh)
					// di blok catch mengisolasi kegagalan ke baris ini saja.
					spBarisImpor = session.connection().setSavepoint();

					String barcode = b.optString("barcode", "").trim();
					kategoriNama = b.optString("kategoriNama", "").trim();
					pemasokNama = b.optString("pemasokNama", "").trim();
					satuanNama = b.optString("satuanNama", "").trim();
					double stokBaru = b.optDouble("stokBaru", 0);
					double hargaJual = b.optDouble("hargaJual", 0);
					double hargaBeli = b.optDouble("hargaBeli", 0);

					Long kategoriId = null;
					if (!kategoriNama.isEmpty()) {
						kategoriId = petaKategori.get(kategoriNama.toUpperCase());
						if (kategoriId == null) {
							JenisProduk jBaru = new JenisProduk();
							jBaru.setNama(kategoriNama);
							jBaru.setAktif(true);
							session.save(jBaru);
							kategoriId = jBaru.getId();
							petaKategori.put(kategoriNama.toUpperCase(), kategoriId);
							kategoriBaru++;
							kategoriBaruBarisIni = true;
						}
					}
					Long pemasokId = null;
					if (!pemasokNama.isEmpty()) {
						pemasokId = petaPemasok.get(pemasokNama.toUpperCase());
						if (pemasokId == null) {
							PemasokProduk pBaru = new PemasokProduk();
							pBaru.setNama(pemasokNama);
							pBaru.setAktif(true);
							session.save(pBaru);
							pemasokId = pBaru.getId();
							petaPemasok.put(pemasokNama.toUpperCase(), pemasokId);
							pemasokBaru++;
							pemasokBaruBarisIni = true;
						}
					}
					Long satuanId = null;
					if (!satuanNama.isEmpty()) {
						satuanId = petaSatuan.get(satuanNama.toUpperCase());
						if (satuanId == null) {
							SatuanProduk sBaru = new SatuanProduk();
							sBaru.setNama(satuanNama);
							sBaru.setAktif(true);
							session.save(sBaru);
							satuanId = sBaru.getId();
							petaSatuan.put(satuanNama.toUpperCase(), satuanId);
							satuanBaru++;
							satuanBaruBarisIni = true;
						}
					}

					String kodeUpper = kode.toUpperCase();
					String barcodeUpper = barcode.isEmpty() ? null : barcode.toUpperCase();
					String namaUpper = nama.toUpperCase();
					String kunciUnikBaris = ais.common.ProdukKunciUnikUtil.hitung(kode, barcode.isEmpty() ? null : barcode, nama, tokoId);
					Long produkId = petaProdukToko.get(kodeUpper);
					String dicocokkanVia = produkId != null ? "kode" : null;
					if (produkId == null && barcodeUpper != null) {
						produkId = petaProdukTokoBarcode.get(barcodeUpper);
						if (produkId != null) dicocokkanVia = "barcode";
					}
					if (produkId == null) {
						produkId = petaProdukTokoNama.get(namaUpper);
						if (produkId != null) dicocokkanVia = "nama";
					}
					if (produkId == null) {
						produkId = petaProdukTokoKunciUnik.get(kunciUnikBaris);
						if (produkId != null) dicocokkanVia = "kunci_unik";
					}
					Produk p;
					boolean baru = (produkId == null);
					double stokLama = 0;
					String kodeLama = null;
					if (baru) {
						p = new Produk();
						p.setToko(toko);
					} else {
						p = (Produk) session.get(Produk.class, produkId);
						if (p == null) { p = new Produk(); p.setToko(toko); baru = true; dicocokkanVia = null; }
						else { stokLama = p.getStok() == null ? 0 : p.getStok(); kodeLama = p.getKode(); }
					}
					// Kode SELALU ditulis (bukan cuma saat baru) -- gap-closure: baris yg cocok lewat
					// fallback barcode/nama (lihat JavaDoc method) punya kode BEDA dari produk yg
					// ditemukan; harus ikut diperbarui ke nilai baru dari Excel, kalau tidak baris
					// berikutnya dgn kode lama itu tetap tak ketemu & bikin duplikat lagi.
					p.setKode(kode);
					p.setNama(nama);
					p.setBarcode(barcode.isEmpty() ? null : barcode);
					// Diset EKSPLISIT (bukan diandalkan dari @PrePersist/@PreUpdate entity) -- lihat JavaDoc
					// gap-closure di produkSimpan soal kenapa hook JPA tak bisa diandalkan di setup ini.
					p.setKunciUnik(kunciUnikBaris);
					p.setHargaJual(hargaJual);
					p.setHargaBeli(hargaBeli);
					if (kategoriId != null) p.setJenisProduk((JenisProduk) session.load(JenisProduk.class, kategoriId));
					if (pemasokId != null) p.setPemasok((PemasokProduk) session.load(PemasokProduk.class, pemasokId));
					if (satuanId != null) p.setSatuan((SatuanProduk) session.load(SatuanProduk.class, satuanId));

					if (baru) {
						try {
							session.save(p);
							session.flush();
							dibuat++;
						} catch (Exception eInsertBentrok) {
							if (!merupakanPelanggaranKunciUnikProduk(eInsertBentrok)) throw eInsertBentrok;
							// Gap-closure "kenapa masih bisa error, harusnya sudah dicegah?" -- percobaan
							// insert BENTROK dgn baris yg TERNYATA sudah ada di database walau 4 lapis
							// pencocokan di atas (kode/barcode/nama/kunci_unik dari PETA DI MEMORI) tidak
							// menemukannya -- bisa krn peta sempat basi (mis. kunci_unik baris lama belum
							// pernah di-backfill saat peta dibangun di awal method), ATAU race jarang dgn
							// permintaan impor lain yg berjalan bersamaan. DARIPADA menyerah, cari LANGSUNG
							// ke database (bukan peta) pakai kunci_unik yg SAMA PERSIS ditolak constraint-nya
							// -- itu SATU-SATUNYA sumber kebenaran yg tak mungkin basi -- lalu redirect jadi
							// UPDATE ke baris itu. Baris ini TETAP tersimpan, TIDAK ditandai gagal.
							session.connection().rollback(spBarisImpor);
							// kategori/pemasok/satuan baru (bila ada) ikut ter-rollback krn dibuat SETELAH
							// savepoint -- batalkan bookkeeping-nya (sama pola dgn blok catch luar) & jangan
							// pakai id-nya lagi utk baris ini (biar apa adanya, aman drpd merujuk baris yg
							// sudah tak ada).
							if (kategoriBaruBarisIni && !kategoriNama.isEmpty()) { petaKategori.remove(kategoriNama.toUpperCase()); kategoriBaru--; kategoriId = null; }
							if (pemasokBaruBarisIni && !pemasokNama.isEmpty()) { petaPemasok.remove(pemasokNama.toUpperCase()); pemasokBaru--; pemasokId = null; }
							if (satuanBaruBarisIni && !satuanNama.isEmpty()) { petaSatuan.remove(satuanNama.toUpperCase()); satuanBaru--; satuanId = null; }
							session.clear();
							toko = (Toko) session.load(Toko.class, tokoId);
							Long idBentrok = cariProdukIdByKunciUnik(session, tokoId.longValue(), kunciUnikBaris);
							if (idBentrok == null) throw eInsertBentrok; // Genuinely tak ketemu -- lempar spt semula.
							p = (Produk) session.get(Produk.class, idBentrok);
							stokLama = p.getStok() == null ? 0 : p.getStok();
							kodeLama = p.getKode();
							p.setKode(kode);
							p.setNama(nama);
							p.setBarcode(barcode.isEmpty() ? null : barcode);
							p.setKunciUnik(kunciUnikBaris);
							p.setHargaJual(hargaJual);
							p.setHargaBeli(hargaBeli);
							if (kategoriId != null) p.setJenisProduk((JenisProduk) session.load(JenisProduk.class, kategoriId));
							if (pemasokId != null) p.setPemasok((PemasokProduk) session.load(PemasokProduk.class, pemasokId));
							if (satuanId != null) p.setSatuan((SatuanProduk) session.load(SatuanProduk.class, satuanId));
							session.update(p);
							session.flush();
							baru = false;
							dicocokkanVia = "kunci_unik_setelah_konflik";
							diperbarui++;
						}
					} else {
						session.update(p);
						diperbarui++;
					}
					bh.put("id", p.getId());

					// Stok SENGAJA TIDAK menimpa produk.stok langsung -- lihat catatan lengkap di
					// JavaDoc kelas method ini. Opname HANYA dicatat bila stok baru != stok lama --
					// lihat catatan "Opname HANYA dicatat bila ada selisih" di JavaDoc method ini.
					double selisih = stokBaru - stokLama;
					String aksiStok;
					if (selisih != 0) {
						ais.action.master.inventory.StokOpnameScanUtil.simpanOpname(session, tokoId, p.getId(), stokBaru,
								"Impor Excel Katalog Barang", oleh);
						stokDiopname++;
						aksiStok = "diopname";
					} else {
						aksiStok = "tidak_ada_perubahan";
					}

					// Flush SEKARANG (per baris) -- bukan cuma ditumpuk sampai commit() di akhir batch --
					// supaya kegagalan DB baris ini (mis. barcode/kode dobel) MELEMPAR EXCEPTION DI SINI
					// ke blok catch baris ini (yg rollback ke savepoint), bukan menunggu commit() di akhir
					// SELURUH batch (yg kalau gagal meracuni transaksi & menggagalkan SEMUA baris lain yg
					// sudah benar -- lihat JavaDoc method induk).
					session.flush();

					// Peta diperbarui HANYA SETELAH flush berhasil -- kalau ditaruh sebelum flush spt
					// sebelumnya & flush gagal (baris ini di-rollback via savepoint), peta akan menunjuk
					// id yg sebenarnya sudah tak ada lagi di DB & meracuni baris lain di batch yg sama.
					// Diperbarui utk SEMUA baris (baru maupun update) supaya baris LAIN dlm batch yg sama
					// jg saling mencegah duplikat -- termasuk kode LAMA (bila berbeda & masih dipakai
					// baris lain yg belum diproses) tetap dibiarkan menunjuk id yg sama supaya tak
					// ketiban insert baru gara2 kode lama itu masih tercatat di peta.
					petaProdukToko.put(kodeUpper, p.getId());
					if (kodeLama != null && !kodeLama.trim().isEmpty()) petaProdukToko.put(kodeLama.trim().toUpperCase(), p.getId());
					if (barcodeUpper != null) petaProdukTokoBarcode.put(barcodeUpper, p.getId());
					petaProdukTokoNama.put(namaUpper, p.getId());
					if (!kunciUnikBaris.isEmpty()) petaProdukTokoKunciUnik.put(kunciUnikBaris, p.getId());

					bh.put("status", "berhasil");
					bh.put("produkBaru", baru);
					bh.put("kategoriBaru", kategoriBaruBarisIni);
					bh.put("pemasokBaru", pemasokBaruBarisIni);
					bh.put("satuanBaru", satuanBaruBarisIni);
					bh.put("stokLama", stokLama);
					bh.put("stokBaru", stokBaru);
					bh.put("selisih", selisih);
					bh.put("aksiStok", aksiStok);
					bh.put("dicocokkanVia", dicocokkanVia == null ? "kode" : dicocokkanVia);
					StringBuilder pesanBaris = new StringBuilder();
					pesanBaris.append(baru ? "Produk baru dibuat." : "Produk diperbarui.");
					if (aksiStok.equals("diopname")) {
						pesanBaris.append(" Stok disesuaikan via Stok Opname: ").append(fmtStok(stokLama))
								.append(" -> ").append(fmtStok(stokBaru))
								.append(" (selisih ").append(selisih > 0 ? "+" : "").append(fmtStok(selisih)).append(").");
					} else {
						pesanBaris.append(" Stok tidak berubah (").append(fmtStok(stokBaru)).append("), tidak ada penyesuaian opname.");
					}
					// Gap-closure "jangan sampai duplikat lagi": beritahu admin eksplisit kalau baris ini
					// TIDAK ketemu via kode (kode di Excel beda dari produk lama) tapi cocok lewat barcode/
					// nama, supaya bukan cuma dicegah diam2 -- admin sadar kode produk itu ikut berubah.
					if (!baru && "barcode".equals(dicocokkanVia)) {
						pesanBaris.append(" Dicocokkan via Barcode (kode di file berbeda dari data lama -- kode diperbarui dari \"")
								.append(kodeLama).append("\" ke \"").append(kode).append("\", BUKAN produk baru).");
					} else if (!baru && "nama".equals(dicocokkanVia)) {
						pesanBaris.append(" Dicocokkan via Nama Produk (kode di file berbeda dari data lama -- kode diperbarui dari \"")
								.append(kodeLama).append("\" ke \"").append(kode).append("\", BUKAN produk baru).");
					} else if (!baru && "kunci_unik".equals(dicocokkanVia)) {
						pesanBaris.append(" Dicocokkan via Kunci Unik (kode/barcode/nama di file berbeda tanda baca/spasi/huruf besar-kecil dari data lama -- kode diperbarui dari \"")
								.append(kodeLama).append("\" ke \"").append(kode).append("\", BUKAN produk baru).");
					}
					if (kategoriBaruBarisIni) pesanBaris.append(" Kategori \"").append(kategoriNama).append("\" baru dibuat.");
					if (pemasokBaruBarisIni) pesanBaris.append(" Pemasok \"").append(pemasokNama).append("\" baru dibuat.");
					if (satuanBaruBarisIni) pesanBaris.append(" Satuan \"").append(satuanNama).append("\" baru dibuat.");
					bh.put("pesan", pesanBaris.toString());
					barisHasilArr.put(bh);

					if ((dibuat + diperbarui) % BATCH_FLUSH_IMPOR_PRODUK == 0) {
						session.flush();
						session.clear();
						toko = (Toko) session.load(Toko.class, tokoId);
					}
				} catch (Exception eBaris) {
					dilewati++;
					// Rollback ke savepoint BARIS INI (bukan seluruh transaksi) -- lihat catatan lengkap
					// di titik pembuatan savepoint di atas. Tanpa ini, satu baris gagal meracuni SISA
					// transaksi Postgres & menggagalkan SEMUA baris berikutnya di batch yg sama saat
					// commit() di akhir method.
					if (spBarisImpor != null) {
						try {
							session.connection().rollback(spBarisImpor);
						} catch (Exception eRollbackSp) {
							ais.common.ErrorAuditUtil.record(eRollbackSp, "produkImporExcelKomit rollback-to-savepoint gagal baris " + (i + 1) + " toko " + tokoId);
						}
						// Batalkan jg bookkeeping in-memory yg SEMPAT dibuat baris ini sebelum gagal --
						// kategori/pemasok/satuan baru itu ikut ter-rollback lewat savepoint di atas, tapi
						// petanya (dipakai baris LAIN di batch yg sama utk hindari duplikat) masih
						// menunjuk id yg (setelah rollback) sudah tak ada lagi di DB.
						if (kategoriBaruBarisIni && !kategoriNama.isEmpty()) { petaKategori.remove(kategoriNama.toUpperCase()); kategoriBaru--; }
						if (pemasokBaruBarisIni && !pemasokNama.isEmpty()) { petaPemasok.remove(pemasokNama.toUpperCase()); pemasokBaru--; }
						if (satuanBaruBarisIni && !satuanNama.isEmpty()) { petaSatuan.remove(satuanNama.toUpperCase()); satuanBaru--; }
						session.clear();
						toko = (Toko) session.load(Toko.class, tokoId);
					}
					String pesanTeknis = eBaris.getClass().getSimpleName() + ": " + (eBaris.getMessage() == null ? eBaris.toString() : eBaris.getMessage());
					if (errorArr.length() < 50) {
						errorArr.put("Baris " + (i + 1) + ": " + pesanTeknis);
					}
					bh.put("status", "gagal");
					bh.put("pesan", "Gagal disimpan -- baris ini TIDAK tercatat, baris lain tetap diproses.");
					bh.put("teknis", pesanTeknis);
					bh.put("solusi", saranPerbaikan(eBaris));
					bh.put("aksiStok", "-");
					barisHasilArr.put(bh);
					ais.common.ErrorAuditUtil.record(eBaris, "produkImporExcelKomit baris " + (i + 1) + " toko " + tokoId);
				}
			}

			session.getTransaction().commit();

			// ==== Verifikasi pasca-commit -- pastikan baris yg dilaporkan "berhasil" BENAR-BENAR
			// tersimpan sesuai yg diharapkan, bukan cuma "tidak ada exception saat proses". SELALU
			// baca ulang via SQL NATIVE (bukan lewat entity Hibernate) -- membaca lewat session.get()
			// di sini akan menyesatkan krn Hibernate identity map cuma mengembalikan objek in-memory
			// yg SAMA yg barusan dimutasi, BUKAN baris sungguhan di database (verifikasi jadi selalu
			// "cocok" walau sebenarnya tidak ter-commit). Baris yg ternyata TIDAK sesuai diturunkan
			// statusnya jadi "gagal" dgn rincian nilai yg diharapkan vs yg sungguhan tersimpan.
			int verifikasiGagal = 0;
			for (int vi = 0; vi < barisHasilArr.length(); vi++) {
				JSONObject bh = barisHasilArr.getJSONObject(vi);
				if (!"berhasil".equals(bh.optString("status")) || bh.isNull("id")) continue;
				long idVerif = bh.getLong("id");
				try {
					java.sql.PreparedStatement psV = session.connection().prepareStatement(
							"SELECT nama, hargajual, hargabeli, stok FROM koperasi.produk WHERE id = ?");
					psV.setLong(1, idVerif);
					java.sql.ResultSet rsV = psV.executeQuery();
					if (!rsV.next()) {
						bh.put("status", "gagal");
						bh.put("teknis", "Verifikasi gagal: baris produk (id=" + idVerif + ") tidak ditemukan lagi di database setelah disimpan -- kemungkinan transaksi tidak benar-benar ter-commit.");
						bh.put("solusi", "Coba impor ulang baris ini. Bila tetap terjadi, laporkan ke admin/tim pengembang DAN WAJIB lampirkan tangkapan layar (screenshot) laporan ini.");
						dilewati++;
						verifikasiGagal++;
					} else {
						String namaTersimpan = rsV.getString(1);
						double stokTersimpan = rsV.getDouble(4);
						String namaHarap = bh.optString("nama", "");
						double stokHarap = bh.optDouble("stokBaru", 0);
						StringBuilder beda = new StringBuilder();
						if (namaTersimpan == null || !namaTersimpan.equals(namaHarap)) {
							beda.append("nama (harap \"").append(namaHarap).append("\", tersimpan \"").append(namaTersimpan).append("\")");
						}
						if (Math.abs(stokTersimpan - stokHarap) > 0.009) {
							if (beda.length() > 0) beda.append("; ");
							beda.append("stok (harap ").append(fmtStok(stokHarap)).append(", tersimpan ").append(fmtStok(stokTersimpan)).append(")");
						}
						if (beda.length() > 0) {
							bh.put("status", "gagal");
							bh.put("teknis", "Verifikasi gagal: data di database TIDAK sesuai dgn yg seharusnya disimpan -- " + beda.toString() + ".");
							bh.put("solusi", "Klik \"Hitung Ulang Stok\" di layar Katalog Barang, lalu impor ulang baris ini. Bila tetap terjadi, laporkan ke admin/tim pengembang DAN WAJIB lampirkan tangkapan layar (screenshot) laporan ini.");
							dilewati++;
							verifikasiGagal++;
						}
					}
					rsV.close();
					psV.close();
				} catch (Exception eVerif) {
					// Verifikasi itu SENDIRI gagal dijalankan (mis. koneksi terputus SETELAH commit) --
					// JANGAN turunkan status baris ini (datanya kemungkinan besar BENAR tersimpan, cuma
					// pembacaan ulangnya yg gagal) -- cukup catat sbg catatan tambahan di baris laporan.
					bh.put("catatanVerifikasi", "Verifikasi otomatis tidak bisa dijalankan: " + eVerif.getMessage());
					ais.common.ErrorAuditUtil.record(eVerif, "produkImporExcelKomit verifikasi baris toko " + tokoId);
				}
			}

			hasil.put("status", "00");
			hasil.put("total", dibuat + diperbarui + dilewati);
			hasil.put("dibuat", dibuat);
			hasil.put("diperbarui", diperbarui);
			hasil.put("dilewati", dilewati);
			hasil.put("kategoriBaru", kategoriBaru);
			hasil.put("pemasokBaru", pemasokBaru);
			hasil.put("satuanBaru", satuanBaru);
			hasil.put("stokDiopname", stokDiopname);
			hasil.put("verifikasiGagal", verifikasiGagal);
			hasil.put("error", errorArr);
			hasil.put("baris", barisHasilArr);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Nonaktifkan produk yang tidak ada di file ini" (gap-closure rekonsiliasi Accurate vs
	 * e-campus) -- checkbox OPSIONAL (default nonaktif) di layar Tinjau Impor Katalog. Dipanggil klien
	 * SETELAH {@link #produkImporExcelKomit} sungguh-sungguh selesai (dipecah beberapa permintaan
	 * karena batch besar -- lihat {@code kirimKomitExcelBertahap} Electron) dgn daftar id SELURUH
	 * produk yg BENAR tersentuh (status "berhasil") sepanjang file, lalu menonaktifkan (bukan
	 * menghapus) SEMUA produk toko ini yang MASIH aktif tapi TIDAK ada di daftar tsb -- artinya barang
	 * yg sudah tak muncul lagi di ekspor Accurate/Excel terbaru dianggap sudah dihapus/nonaktif di
	 * sumbernya & ikut dinonaktifkan di e-campus.
	 *
	 * <p><b>Gerbang keselamatan</b>: kalau {@code id_disentuh} KOSONG (mis. seluruh baris impor gagal,
	 * atau file yg diunggah kosong), method ini SENGAJA MENOLAK & tidak menonaktifkan APA PUN --
	 * tanpa ini, kesalahan/kegagalan impor bisa diam-diam menonaktifkan SELURUH katalog toko.</p>
	 *
	 * @param request payload: {@code toko_id} (opsional utk supervisor, wajib utk admin global -- sama
	 *                dgn {@link #produkImporExcelKomit}), {@code id_disentuh} (array id produk).
	 */
	public static void produkNonaktifkanTakDiimpor(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		// FIX "deadlock detected" (SQLState 40P01) saat UPDATE massal koperasi.produk: method ini
		// bisa dipanggil BERBARENGAN dengan produkImporExcelKomit/checkout Kasir yang sama-sama
		// meng-UPDATE baris produk milik toko yang sama dalam urutan berbeda -- Postgres SENGAJA
		// membatalkan salah satu transaksi (perilaku normal di bawah konkurensi tinggi, BUKAN
		// korupsi data). Pakai pola retry yg sama persis dgn produkImporExcelKomit di atas
		// (merupakanDeadlockPostgres + hingga 3 percobaan berjeda) alih-alih membiarkan deadlock
		// menembus polos ke klien sbg "kesalahan sistem" tanpa percobaan ulang otomatis.
		int percobaanMaks = 3;
		for (int percobaan = 1; percobaan <= percobaanMaks; percobaan++) {
			try {
				produkNonaktifkanTakDiimporSatuPercobaan(tbmuser, request, hasil);
				return;
			} catch (Exception e) {
				if (!merupakanDeadlockPostgres(e) || percobaan >= percobaanMaks) {
					throw e;
				}
				ais.common.ErrorAuditUtil.record(e, "produkNonaktifkanTakDiimpor deadlock -- percobaan " + percobaan
						+ "/" + percobaanMaks + " gagal, mencoba ulang");
				try {
					Thread.sleep(300L * percobaan);
				} catch (InterruptedException eSela) {
					Thread.currentThread().interrupt();
					throw e;
				}
			}
		}
	}

	private static void produkNonaktifkanTakDiimporSatuPercobaan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = gerbangDanTokoImporProduk(tbmuser, request, hasil,
				"Hanya admin/manager atau supervisor toko yang dapat menonaktifkan katalog barang.");
		if (tokoId == null) return;

		JSONArray idArr = request.optJSONArray("id_disentuh");
		if (idArr == null || idArr.length() == 0) {
			hasil.put("status", "91");
			hasil.put("description", "Daftar produk yang berhasil diimpor kosong -- dibatalkan demi keamanan (tidak ada yang dinonaktifkan).");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			StringBuilder placeholder = new StringBuilder();
			for (int i = 0; i < idArr.length(); i++) {
				if (i > 0) placeholder.append(',');
				placeholder.append('?');
			}
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"UPDATE koperasi.produk SET aktif = false WHERE toko = ? AND aktif = true AND id NOT IN (" + placeholder + ")");
			ps.setLong(1, tokoId);
			for (int i = 0; i < idArr.length(); i++) {
				ps.setLong(i + 2, idArr.getLong(i));
			}
			int dinonaktifkan = ps.executeUpdate();
			ps.close();
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("dinonaktifkan", dinonaktifkan);
			hasil.put("dipertahankan", idArr.length());
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** Format angka stok utk pesan laporan impor -- tanpa desimal ".0" yg mengganggu bila nilainya bulat (kasus paling umum), tapi TETAP tampilkan desimal aslinya bila memang pecahan (mis. stok kiloan). */
	private static String fmtStok(double v) {
		if (v == Math.rint(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
		return String.valueOf(v);
	}

	/**
	 * Saran perbaikan awam berdasarkan JENIS exception yg terjadi saat menyimpan satu baris impor --
	 * supaya laporan hasil impor tidak cuma menampilkan stack trace teknis mentah, tapi juga langkah
	 * konkret yg bisa dicoba user SEBELUM eskalasi ke admin/tim pengembang. Klasifikasi berbasis nama
	 * kelas exception + kata kunci pesan (best-effort, bukan daftar lengkap) -- kasus yg tak dikenali
	 * jatuh ke saran generik "coba ulang, lalu eskalasi dgn screenshot" di baris terakhir.
	 */
	private static String saranPerbaikan(Exception e) {
		String pesan = (e.getMessage() == null ? "" : e.getMessage()).toLowerCase();
		String namaKelas = e.getClass().getSimpleName().toLowerCase();
		if (namaKelas.contains("constraintviolation") || pesan.contains("duplicate") || pesan.contains("unique")) {
			return "Kode/barcode produk ini kemungkinan sama dgn baris lain atau produk lain yg sudah ada. Periksa tidak ada duplikat Kode/Barcode di file Excel, lalu coba impor ulang baris ini.";
		}
		if (namaKelas.contains("numberformat") || pesan.contains("numeric") || pesan.contains("invalid input syntax")) {
			return "Periksa format angka pada kolom Stok Baru/Harga Jual/Harga Beli di file Excel (pastikan hanya angka, tanpa teks/simbol seperti \"Rp\" atau titik/koma ribuan).";
		}
		if (namaKelas.contains("null") || pesan.contains("not-null") || pesan.contains("null value")) {
			return "Ada kolom wajib yang kosong pada baris ini di file Excel. Lengkapi lalu coba impor ulang.";
		}
		if (namaKelas.contains("stale") || pesan.contains("stale")) {
			return "Data produk ini baru saja diubah oleh proses lain (mis. kasir lain sedang mengedit produk yg sama). Muat ulang katalog lalu coba impor ulang baris ini.";
		}
		return "Coba impor ulang baris ini. Bila kegagalan berlanjut, laporkan ke admin/tim pengembang DAN WAJIB lampirkan tangkapan layar (screenshot) laporan ini beserta detail teknis di atas.";
	}

	/**
	 * "Hitung Ulang Stok" -- rekalkulasi stok SEMUA produk di satu toko dari rekam jejak
	 * pengadaan/opname/penjualan/pemakaian bahan baku (formula 4-suku baku, lihat
	 * {@code StokKantinUtil.recomputeStokProduk}), dipicu manual dari layar Katalog Barang (khusus
	 * supervisor/admin) -- tombol pemulihan mandiri kalau stok terlihat tidak akurat.
	 *
	 * <p><b>Langkah 1 -- perbaiki data lama.</b> Sebelum recompute, kolom {@code selisih} pada SETIAP
	 * baris {@code StokOpname} milik toko ini yang NULL atau tidak sesuai formula ({@code stokfisik -
	 * stoksistem}) diperbaiki lebih dulu via UPDATE SQL langsung -- lihat catatan lengkap di
	 * {@code StokOpnameScanUtil.simpanOpname} soal kenapa kolom ini bisa kadung salah tersimpan
	 * (computed getter yang tidak selalu dipanggil sebelum entity disimpan). Ini membuat tombol ini
	 * SEKALIGUS jadi jalan pemulihan mandiri dari bug itu tanpa admin perlu mengetik SQL manual.</p>
	 *
	 * <p><b>Langkah 2 -- recompute.</b> Delegasi ke {@code StokKantinUtil.recomputeStokProduk} per
	 * produk di toko ini (loop id, bukan {@code recomputeStokToko} langsung, supaya dibatasi HANYA
	 * produk toko ini + dihitung jumlahnya utk ringkasan hasil).</p>
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin global, diabaikan/dikunci ke toko
	 *                sendiri utk supervisor -- lihat {@link #gerbangDanTokoImporProduk}).
	 * @param hasil   diisi {@code status="00"}, {@code produkDiproses}, {@code selisihDiperbaiki}.
	 */
	public static void stokHitungUlang(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = gerbangDanTokoImporProduk(tbmuser, request, hasil,
				"Hanya admin/manager atau supervisor toko yang dapat menghitung ulang stok.");
		if (tokoId == null) return;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();

			int selisihDiperbaiki = session.createSQLQuery(
					"UPDATE koperasi.stok_opname SET selisih = stokfisik - stoksistem "
					+ "WHERE produk IN (SELECT id FROM koperasi.produk WHERE toko = " + tokoId + ") "
					+ "AND (selisih IS NULL OR selisih <> (stokfisik - stoksistem))")
					.executeUpdate();

			// Native SQL query mengembalikan java.math.BigInteger utk kolom integer/bigint
			// Postgres (bukan Long) -- cast langsung ke List<Long> akan lolos compile (unchecked)
			// tapi meledak ClassCastException saat iterasi. Ambil sbg Number lalu longValue().
			@SuppressWarnings("unchecked")
			List<Number> idsProduk = session.createSQLQuery("SELECT id FROM koperasi.produk WHERE toko = " + tokoId).list();
			int produkDiproses = 0;
			for (Number idNum : idsProduk) {
				Long id = idNum.longValue();
				// SENGAJA memakai varian *Native* (UPDATE SQL langsung pada `session` POLA B ini), BUKAN
				// {@link StokKantinUtil#recomputeStokProduk(Long)} spt sebelumnya -- varian entity itu
				// internal memanggil HibernateUtil.currentSession(), yg DI LUAR thread eksekusi ZK (mis.
				// konteks servlet POLA B seperti di sini) diam-diam jatuh ke sesi native ThreadLocal
				// TERPISAH dari `session` yg dikelola/di-commit di method ini -- perubahan jadi tak
				// tersinkron dgn transaksi yg sedang berjalan. Varian *Native* jg gagal-aman sendiri
				// (menangkap+mencatat exception internal, tak pernah melempar), sekaligus mengisolasi
				// kegagalan satu produk shg tak menggagalkan SELURUH proses hitung ulang.
				ais.action.master.inventory.StokKantinUtil.recomputeStokProdukNative(session, id);
				produkDiproses++;
				if (produkDiproses % BATCH_FLUSH_IMPOR_PRODUK == 0) {
					session.flush();
					session.clear();
				}
			}

			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("produkDiproses", produkDiproses);
			hasil.put("selisihDiperbaiki", selisihDiperbaiki);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Kompatibilitas Android -- layar Produk Android (belum dibangun layar tinjau/review spt Desktop,
	 * layar edit-ribuan-baris kurang praktis di HP) masih mengunggah langsung 1-langkah, memanggil
	 * aksi lama {@code produk_impor_excel} yang SEMPAT dihapus saat server dipecah jadi preview/komit
	 * utk kebutuhan Desktop -- method ini MENGEMBALIKAN alur 1-langkah itu TANPA mengulang logika
	 * parsing Excel: cukup panggil {@link #produkImporExcelPreview} (parse file + resolusi
	 * kategori/pemasok/satuan/produk existing SAAT INI, read-only), lalu langsung suapkan hasil
	 * {@code baris}-nya (nilai default hasil resolusi server, TANPA kesempatan diedit user seperti di
	 * Desktop) ke {@link #produkImporExcelKomit} apa adanya. Dua panggilan, DUA sesi Hibernate
	 * terpisah (bukan reuse 1 sesi) -- sengaja, supaya masing-masing method tetap mandiri/tidak perlu
	 * di-refactor jadi menerima {@code Session} dari luar hanya demi jalur kompatibilitas ini.
	 */
	public static void produkImporExcel(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		JSONObject hasilPreview = new JSONObject();
		produkImporExcelPreview(tbmuser, request, hasilPreview);
		if (!"00".equals(hasilPreview.optString("status", ""))) {
			hasil.put("status", hasilPreview.optString("status", "91"));
			hasil.put("description", hasilPreview.optString("description", "Gagal memproses file Excel."));
			return;
		}
		JSONObject requestKomit = new JSONObject();
		requestKomit.put("toko_id", hasilPreview.opt("tokoId"));
		requestKomit.put("baris", hasilPreview.optJSONArray("baris"));
		produkImporExcelKomit(tbmuser, requestKomit, hasil);
	}

	/**
	 * Tombol "Download Excel" di layar review impor (khusus supervisor) -- membangun {@code .xlsx}
	 * dari baris yang SEDANG ditampilkan/diedit user di layar (BUKAN query ulang ke database), format
	 * kolom SAMA PERSIS {@link #KOLOM_EXCEL_PRODUK}/{@link #produkEksporExcel}, supaya supervisor bisa
	 * mengunduh salinan "apa yang akan disimpan" sebelum menekan tombol Simpan.
	 *
	 * @param request payload: {@code baris} (wajib, bentuk field SAMA dgn {@link #produkImporExcelPreview}).
	 * @param hasil   diisi {@code status="00"} + {@code fileBase64}, {@code namaFile}, {@code total}.
	 */
	public static void produkGridEksporExcel(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = gerbangDanTokoImporProduk(tbmuser, request, hasil,
				"Hanya admin/manager atau supervisor toko yang dapat mengunduh katalog barang.");
		if (tokoId == null) return;

		JSONArray barisArr = request.optJSONArray("baris");
		if (barisArr == null) barisArr = new JSONArray();

		XSSFWorkbook wb = new XSSFWorkbook();
		XSSFSheet sheet = wb.createSheet("Daftar Barang dan Jasa");
		sheet.setDefaultColumnWidth(20);
		XSSFRow headerRow = sheet.createRow(0);
		for (int i = 0; i < KOLOM_EXCEL_PRODUK.length; i++) {
			headerRow.createCell(i).setCellValue(KOLOM_EXCEL_PRODUK[i]);
		}
		for (int i = 0; i < barisArr.length(); i++) {
			JSONObject b = barisArr.getJSONObject(i);
			double stokBaru = b.optDouble("stokBaru", 0);
			double hargaBeli = b.optDouble("hargaBeli", 0);
			XSSFRow row = sheet.createRow(i + 1);
			row.createCell(0).setCellValue(b.optInt("no", i + 1));
			row.createCell(1).setCellValue(b.optString("kode", ""));
			row.createCell(2).setCellValue(b.optString("barcode", ""));
			row.createCell(3).setCellValue(b.optString("kategoriNama", ""));
			row.createCell(4).setCellValue(b.optString("nama", ""));
			row.createCell(5).setCellValue(b.optString("pemasokNama", ""));
			row.createCell(6).setCellValue(b.optString("satuanNama", ""));
			row.createCell(7).setCellValue(stokBaru);
			row.createCell(8).setCellValue(b.optDouble("hargaJual", 0));
			row.createCell(9).setCellValue(hargaBeli);
			row.createCell(10).setCellValue(stokBaru * hargaBeli);
		}

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		wb.write(bos);
		hasil.put("status", "00");
		hasil.put("fileBase64", java.util.Base64.getEncoder().encodeToString(bos.toByteArray()));
		hasil.put("namaFile", "Katalog-Review-" + System.currentTimeMillis() + ".xlsx");
		hasil.put("total", barisArr.length());
	}

	/** Parsing angka toleran -- Excel kadang menyimpan angka sbg teks berformat ("Rp 8.000", "8,000.00") lewat {@code Common.getCellContent}; sisa karakter non-digit/koma/titik dibuang sebelum parse, koma dianggap ribuan (dibuang) mengikuti format akunting sumber file. */
	private static double parseAngkaAman(String s) {
		if (s == null || s.trim().isEmpty()) return 0;
		String bersih = s.trim().replace(",", "").replaceAll("[^0-9.\\-]", "");
		if (bersih.isEmpty() || bersih.equals("-") || bersih.equals(".")) return 0;
		try {
			return Double.parseDouble(bersih);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * Fitur "Konfigurasi" (Kasir Desktop) -- profil lengkap toko (Nama, Alamat, Telp, PIC, Email,
	 * dll, lihat field baru di {@link Toko}). Toko dikunci ke toko sendiri utk kasir/supervisor
	 * (pola IDOR-safe SAMA dgn {@link #pedagangList}); admin global WAJIB kirim {@code toko_id}.
	 * TIDAK ADA gerbang tambahan utk method LIHAT ini -- siapa pun yang login boleh melihat profil
	 * tokonya sendiri, gerbang supervisor hanya berlaku utk {@link #tokoProfilSimpan}.
	 *
	 * @param hasil diisi {@code status="00"}, {@code data} (seluruh field profil Toko), dan
	 *              {@code bolehUbah} (boolean, sama makna dgn {@code bolehKelola} di {@link #pedagangList}).
	 */
	public static void tokoProfilAmbil(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggil = tbmuser == null ? null : tbmuser.getPedagang();
		Long tokoId;
		if (pemanggil != null) {
			tokoId = pemanggil.getToko() == null ? null : pemanggil.getToko().getId();
		} else {
			tokoId = request.isNull("toko_id") ? null : Long.valueOf((request.get("toko_id") + "").trim());
		}
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		boolean bolehUbah = pemanggil == null || Boolean.TRUE.equals(pemanggil.getSupervisor());

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			if (toko == null) {
				hasil.put("status", "91");
				hasil.put("description", "Toko tidak ditemukan.");
				return;
			}
			JSONObject data = new JSONObject();
			data.put("id", toko.getId());
			data.put("kode", toko.getKode() == null ? "" : toko.getKode());
			data.put("nama", toko.getNama() == null ? "" : toko.getNama());
			data.put("alamat", toko.getAlamat() == null ? "" : toko.getAlamat());
			data.put("kota", toko.getKota() == null ? "" : toko.getKota());
			data.put("kodePos", toko.getKodePos() == null ? "" : toko.getKodePos());
			data.put("telp", toko.getTelp() == null ? "" : toko.getTelp());
			data.put("email", toko.getEmail() == null ? "" : toko.getEmail());
			data.put("picNama", toko.getPicNama() == null ? "" : toko.getPicNama());
			data.put("picHp", toko.getPicHp() == null ? "" : toko.getPicHp());
			data.put("npwp", toko.getNpwp() == null ? "" : toko.getNpwp());
			data.put("jamOperasional", toko.getJamOperasional() == null ? "" : toko.getJamOperasional());
			data.put("keterangan", toko.getKeterangan() == null ? "" : toko.getKeterangan());
			data.put("pesanTerimaKasih", toko.getPesanTerimaKasih());
			hasil.put("status", "00");
			hasil.put("data", data);
			hasil.put("bolehUbah", bolehUbah);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Konfigurasi" -- simpan perubahan profil toko. Gerbang SAMA persis dgn
	 * {@link #pedagangUbah}: admin global ATAU supervisor toko ybs; kasir non-supervisor ditolak
	 * ({@code status="91"}), sesuai permintaan "kalau bukan supervisor, hanya boleh lihat saja".
	 * {@code kode} SENGAJA tidak bisa diubah lewat aksi ini (dipakai referensi stabil di tempat lain
	 * di sistem) -- tetap disunting lewat form admin ZK ({@code TokoAction.java}) bila memang perlu.
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin global, diabaikan/dikunci utk
	 *                supervisor), {@code nama}, {@code alamat}, {@code kota}, {@code kode_pos},
	 *                {@code telp}, {@code email}, {@code pic_nama}, {@code pic_hp}, {@code npwp},
	 *                {@code jam_operasional}, {@code keterangan}, {@code pesan_terima_kasih}
	 *                (ucapan penutup struk & layar customer, lihat {@link Toko#getPesanTerimaKasih}).
	 */
	public static void tokoProfilSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggil = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobal = pemanggil == null;
		boolean supervisor = pemanggil != null && Boolean.TRUE.equals(pemanggil.getSupervisor());
		if (!adminGlobal && !supervisor) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengubah profil toko.");
			return;
		}
		Long tokoId;
		if (supervisor) {
			tokoId = pemanggil.getToko() == null ? null : pemanggil.getToko().getId();
		} else {
			tokoId = request.isNull("toko_id") ? null : Long.valueOf((request.get("toko_id") + "").trim());
		}
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			if (toko == null) {
				hasil.put("status", "91");
				hasil.put("description", "Toko tidak ditemukan.");
				return;
			}
			if (request.has("nama") && !request.isNull("nama")) {
				String nama = request.optString("nama", "").trim();
				if (!nama.isEmpty()) toko.setNama(nama);
			}
			if (request.has("alamat")) toko.setAlamat(request.optString("alamat", ""));
			if (request.has("kota")) toko.setKota(request.optString("kota", ""));
			if (request.has("kode_pos")) toko.setKodePos(request.optString("kode_pos", ""));
			if (request.has("telp")) toko.setTelp(request.optString("telp", ""));
			if (request.has("email")) toko.setEmail(request.optString("email", ""));
			if (request.has("pic_nama")) toko.setPicNama(request.optString("pic_nama", ""));
			if (request.has("pic_hp")) toko.setPicHp(request.optString("pic_hp", ""));
			if (request.has("npwp")) toko.setNpwp(request.optString("npwp", ""));
			if (request.has("jam_operasional")) toko.setJamOperasional(request.optString("jam_operasional", ""));
			if (request.has("keterangan")) toko.setKeterangan(request.optString("keterangan", ""));
			if (request.has("pesan_terima_kasih")) toko.setPesanTerimaKasih(request.optString("pesan_terima_kasih", ""));

			session.beginTransaction();
			session.saveOrUpdate(toko);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Butir 12 spesifikasi "dashboard kasir": tombol "Customer/Anggota" di Desktop, "fungsinya
	 * samakan dengan manajemen anggota yang ada di pos online (versi jsp)" -- versi Desktop ini
	 * SENGAJA hanya mencakup subset field yang relevan utk kasir kantin (nama, kode identitas,
	 * kontak, jenis keanggotaan, aktif) dan TIDAK mereplikasi penautan "master sivitas" penuh
	 * (mahasiswa/siswa/guru/dosen/pegawai) yang ada di {@code anggota_koperasi.jsp} -- itu integrasi
	 * data kampus yang di luar cakupan alur kasir "daftarkan pelanggan baru dengan cepat". Anggota yg
	 * butuh ditautkan ke NIM/NIS/NIP tetap dikelola lewat aplikasi web seperti biasa; keduanya menulis
	 * ke baris {@code koperasi.anggota_koperasi} yang SAMA, jadi tidak ada duplikasi data.
	 */
	public static void anggotaSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilAnggota = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalAnggota = pemanggilAnggota == null;
		boolean supervisorAnggota = pemanggilAnggota != null && Boolean.TRUE.equals(pemanggilAnggota.getSupervisor());
		String aksiAnggotaSimpan = request.isNull("id") ? "create" : "update";
		if (!bolehAksiCrud(tbmuser, pemanggilAnggota, adminGlobalAnggota, supervisorAnggota, "anggota", aksiAnggotaSimpan)) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola data Customer/Anggota.");
			return;
		}
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Nama member wajib diisi.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		Long idJenis = request.isNull("jenis_anggota_koperasi_id") ? null
				: Long.valueOf((request.get("jenis_anggota_koperasi_id") + "").trim());

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			AnggotaKoperasi anggota;
			boolean baru = (id == null);
			if (baru) {
				anggota = new AnggotaKoperasi();
			} else {
				anggota = (AnggotaKoperasi) session.get(AnggotaKoperasi.class, id);
				if (anggota == null) {
					hasil.put("status", "91");
					hasil.put("description", "Member tidak ditemukan.");
					return;
				}
			}
			anggota.setNama(nama);
			anggota.setKodeIdentitas(request.optString("kode_identitas", ""));
			anggota.setHp(request.optString("hp", ""));
			anggota.setTelp(request.optString("telp", ""));
			anggota.setEmail(request.optString("email", ""));
			anggota.setKeterangan(request.optString("keterangan", ""));
			anggota.setAktif(!request.has("aktif") || request.optBoolean("aktif", true));
			if (idJenis != null) {
				JenisAnggotaKoperasi jenis = (JenisAnggotaKoperasi) session.get(JenisAnggotaKoperasi.class, idJenis);
				anggota.setJenisAnggotaKoperasi(jenis);
			}
			if (!request.isNull("tipe_anggota_koperasi_id")) {
				ais.database.model.koperasi.TipeAnggotaKoperasi tipe = (ais.database.model.koperasi.TipeAnggotaKoperasi) session
						.get(ais.database.model.koperasi.TipeAnggotaKoperasi.class,
								Long.valueOf((request.get("tipe_anggota_koperasi_id") + "").trim()));
				anggota.setTipeAnggotaKoperasi(tipe);
			}
			// "Ubah Tanggal Kadaluarsa" (gap-closure parity anggota_koperasi.jsp) -- kosong/hilang
			// berarti TIDAK kadaluarsa (dibiarkan null), TIDAK mengubah nilai lama kalau field ini
			// sama sekali tak dikirim (mis. klien lama yg belum tahu field ini).
			if (request.has("tanggal_kadaluarsa")) {
				String tglKadaluarsa = request.optString("tanggal_kadaluarsa", "").trim();
				if (tglKadaluarsa.isEmpty()) {
					anggota.setTanggalKadaluarsa(null);
				} else {
					try {
						anggota.setTanggalKadaluarsa(new SimpleDateFormat("yyyy-MM-dd").parse(tglKadaluarsa));
					} catch (Exception eParse) {
						ais.common.ErrorAuditUtil.record(eParse, "auto-audit anggotaSimpan-parse-kadaluarsa src/ais/action/servlet/api/KantinHelper.java");
					}
				}
			}
			// Kode member manual (gap-closure "Edit Kode Member Secara Manual" di JSP) -- HANYA
			// dihormati saat UPDATE data yg sudah ada (baris baru tetap auto-generate di bawah, spt
			// perilaku lama, supaya format urut PREFIX-N/MM/YYYY/N tak diganggu klien lama yg blm
			// kirim field ini).
			if (!baru && request.has("kode") && !request.optString("kode", "").trim().isEmpty()) {
				anggota.setKode(request.optString("kode", "").trim());
			}
			// Kredensial login (opsional) -- HANYA diubah kalau dikirim eksplisit, supaya panggilan
			// lama yg tak menyertakan field ini tak sengaja menghapus/menimpa userid+pass yg sudah ada.
			if (request.has("userid") && !request.optString("userid", "").trim().isEmpty()) {
				anggota.setUserid(request.optString("userid", "").trim());
			}
			if (request.has("pass") && !request.optString("pass", "").trim().isEmpty()) {
				anggota.setPass(request.optString("pass", "").trim());
			}

			session.beginTransaction();
			if (baru) {
				anggota.setKode(anggota.generateKodeMember(session, new Date()));
				session.save(anggota);
			} else {
				session.saveOrUpdate(anggota);
			}
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", anggota.getId());
			hasil.put("kode", anggota.getKode());
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Daftar/pencarian anggota koperasi (paginasi) -- sisi "list" dari layar "Customer/Anggota"
	 * Desktop, lihat JavaDoc {@link #anggotaSimpan} soal cakupan fitur ini.
	 *
	 * @param request payload berisi {@code keyword} (opsional, cocok nama/kode/kode_identitas),
	 *                {@code page} (def:1), {@code page_size} (def:20, maks 100).
	 * @param hasil   diisi {@code status="00"}, {@code data} (array {@code {id, nama, kode,
	 *                kodeIdentitas, hp, aktif, jenisNama}}), dan {@code total}.
	 */
	public static void anggotaList(JSONObject request, JSONObject hasil) throws Exception {
		String keyword = request.optString("keyword", "").trim();
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(1, request.optInt("page_size", 20)));
		int offset = (page - 1) * pageSize;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String andKw = keyword.isEmpty() ? "" : " AND (a.nama ILIKE ? OR a.kode ILIKE ? OR a.kode_identitas ILIKE ?)";
			java.sql.Connection conn = session.connection();

			java.sql.PreparedStatement psCount = conn
					.prepareStatement("SELECT COUNT(*) FROM koperasi.anggota_koperasi a WHERE 1=1" + andKw);
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
				psCount.setString(1, kw);
				psCount.setString(2, kw);
				psCount.setString(3, kw);
			}
			java.sql.ResultSet rsCount = psCount.executeQuery();
			long total = rsCount.next() ? rsCount.getLong(1) : 0;
			rsCount.close();
			psCount.close();

			java.sql.PreparedStatement ps = conn.prepareStatement(
					"SELECT a.id, a.nama, a.kode, COALESCE(a.kode_identitas,''), COALESCE(a.hp,''), COALESCE(a.aktif,true), COALESCE(j.nama,'-'), "
							+ "COALESCE(a.telp,''), COALESCE(a.email_nasabah,''), COALESCE(a.keterangan,''), a.jenis_anggota_koperasi, "
							+ "a.tipe_anggota_koperasi, COALESCE(t.nama,'-'), a.tanggal_kadaluarsa, COALESCE(a.userid,'') "
							+ "FROM koperasi.anggota_koperasi a LEFT JOIN koperasi.jenis_anggota_koperasi j ON a.jenis_anggota_koperasi = j.id "
							+ "LEFT JOIN koperasi.tipe_anggota_koperasi t ON a.tipe_anggota_koperasi = t.id "
							+ "WHERE 1=1" + andKw + " ORDER BY a.nama ASC LIMIT ? OFFSET ?");
			int idx = 1;
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
				ps.setString(idx++, kw);
				ps.setString(idx++, kw);
				ps.setString(idx++, kw);
			}
			ps.setInt(idx++, pageSize);
			ps.setInt(idx++, offset);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			java.text.SimpleDateFormat sdfTglAl = new java.text.SimpleDateFormat("yyyy-MM-dd");
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				j.put("kode", rs.getString(3));
				j.put("kodeIdentitas", rs.getString(4));
				j.put("hp", rs.getString(5));
				j.put("aktif", rs.getBoolean(6));
				j.put("jenisNama", rs.getString(7));
				j.put("telp", rs.getString(8));
				j.put("email", rs.getString(9));
				j.put("keterangan", rs.getString(10));
				long idJenisRow = rs.getLong(11);
				j.put("jenisAnggotaKoperasiId", rs.wasNull() ? JSONObject.NULL : idJenisRow);
				long idTipeRow = rs.getLong(12);
				j.put("tipeAnggotaKoperasiId", rs.wasNull() ? JSONObject.NULL : idTipeRow);
				j.put("tipeNama", rs.getString(13));
				java.sql.Date tglKadaluarsaRow = rs.getDate(14);
				j.put("tanggalKadaluarsa", tglKadaluarsaRow == null ? JSONObject.NULL : sdfTglAl.format(tglKadaluarsaRow));
				j.put("userid", rs.getString(15));
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("pageSize", pageSize);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Picker Member Offline" (Kasir Desktop) -- pengambilan SELURUH anggota koperasi aktif
	 * secara bertahap (cursor {@code sejak_id}), dipakai Desktop utk mengisi cache SQLite lokal
	 * ({@code anggota_cache}) supaya pencarian member tetap berfungsi saat offline. BEDA dari
	 * {@link #anggotaList} (yg dipakai layar admin "Customer/Anggota", raw SQL ringan tanpa foto,
	 * berbasis kata kunci+paginasi UI) -- method ini SENGAJA memuat entitas Hibernate {@code
	 * AnggotaKoperasi} yang sesungguhnya (bukan hasil SQL mentah) karena perlu objek utuh utk
	 * ditelusuri {@link ais.common.ProfileImageUtil#cariFileFotoLain(GeneralValueObject)} (rantai
	 * relasi Siswa/Mahasiswa/Tbmuser) demi menentukan foto member -- reuse mesin resolusi foto yang
	 * SUDAH ADA dan dipakai luas di JSP/ZK, BUKAN logika baru.
	 *
	 * <p>Foto TIDAK dikirim sbg biner di sini (mahal, thousands of member) -- hanya {@code fotoUrl}
	 * (utk diunduh Desktop via {@code /al}, publik/anonim persis seperti foto produk) plus {@code
	 * fotoNama}/{@code fotoUkuran} (metadata deteksi-perubahan; Desktop membandingkan ini dgn cache
	 * lokalnya SEBELUM mengunduh ulang -- lihat {@code main.js#sinkronkanAnggotaLengkap}).</p>
	 *
	 * @param request payload berisi {@code sejak_id} (cursor, def 0 -- ambil id lebih besar dari ini)
	 *                dan {@code page_size} (def 500, maks 1000).
	 * @param hasil   diisi {@code status="00"}, {@code data} (array {@code {id, nama, kode,
	 *                kodeIdentitas, hp, telp, email, keterangan, jenisAnggotaKoperasiId, jenisNama,
	 *                wajibPin, fotoUrl, fotoNama, fotoUkuran}}), {@code maksId} (id tertinggi di batch
	 *                ini -- cursor utk panggilan berikutnya), dan {@code adaLagi} (boolean).
	 */
	public static void anggotaSyncList(JSONObject request, JSONObject hasil) throws Exception {
		long sejakId = request.optLong("sejak_id", 0);
		int pageSize = Math.min(1000, Math.max(1, request.optInt("page_size", 500)));

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			@SuppressWarnings("unchecked")
			java.util.List<AnggotaKoperasi> daftar = session.createCriteria(AnggotaKoperasi.class)
					.add(Restrictions.gt("id", Long.valueOf(sejakId)))
					.add(Restrictions.eq("aktif", true))
					.addOrder(org.hibernate.criterion.Order.asc("id"))
					.setMaxResults(pageSize)
					.list();

			JSONArray arr = new JSONArray();
			long maksId = sejakId;
			for (AnggotaKoperasi a : daftar) {
				if (a.getId() != null && a.getId().longValue() > maksId) {
					maksId = a.getId().longValue();
				}
				JenisAnggotaKoperasi jenis = a.getJenisAnggotaKoperasi();

				String fotoUrl = null;
				String fotoNama = null;
				Long fotoUkuran = null;
				try {
					ais.database.model.file.FileFotoLain foto = ais.common.ProfileImageUtil.cariFileFotoLain(a);
					if (foto != null) {
						fotoNama = foto.getNama();
						java.io.File berkas = foto.ambilFile();
						fotoUkuran = berkas == null ? null : Long.valueOf(berkas.length());
						fotoUrl = ais.common.ProfileImageUtil.getUrlFotoDariObject(a, true);
					}
				} catch (Exception eFoto) {
					// Kegagalan resolusi foto SATU anggota (mis. berkas fisik hilang) tidak boleh
					// menggagalkan seluruh batch sinkron -- anggota ini tetap ikut tanpa foto.
					ais.common.ErrorAuditUtil.record(eFoto,
							"auto-audit anggotaSyncList-foto src/ais/action/servlet/api/KantinHelper.java id=" + a.getId());
				}

				JSONObject j = new JSONObject();
				j.put("id", a.getId());
				j.put("nama", a.getNama() == null ? "" : a.getNama());
				j.put("kode", a.getKode() == null ? "" : a.getKode());
				j.put("kodeIdentitas", a.getKodeIdentitas() == null ? "" : a.getKodeIdentitas());
				j.put("hp", a.getHp() == null ? "" : a.getHp());
				j.put("telp", a.getTelp() == null ? "" : a.getTelp());
				j.put("email", a.getEmail() == null ? "" : a.getEmail());
				j.put("keterangan", a.getKeterangan() == null ? "" : a.getKeterangan());
				j.put("jenisAnggotaKoperasiId", jenis == null ? JSONObject.NULL : jenis.getId());
				j.put("jenisNama", jenis == null ? "-" : jenis.getNama());
				j.put("wajibPin", jenis != null && Boolean.TRUE.equals(jenis.getWajibPin()));
				j.put("fotoUrl", fotoUrl == null ? JSONObject.NULL : fotoUrl);
				j.put("fotoNama", fotoNama == null ? JSONObject.NULL : fotoNama);
				j.put("fotoUkuran", fotoUkuran == null ? JSONObject.NULL : fotoUkuran);
				arr.put(j);
			}

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("maksId", maksId);
			hasil.put("adaLagi", daftar.size() == pageSize);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Sekalian tampilkan anggota-anggota yang baru saja bertransaksi" -- daftar member DISTINCT
	 * yang punya transaksi TERBARU (urut waktu transaksi terakhir menurun), dipakai picker member Desktop
	 * sbg jalan pintas "pilih pelanggan tadi" tanpa perlu ketik nama/kode.
	 *
	 * <p><b>Cara kerja, dua tahap SENGAJA dipisah.</b> Tahap 1 (agregat "siapa saja + kapan terakhir")
	 * tetap raw SQL -- {@code group by ... order by max(...) ... limit N} sebelum di-hydrate tidak
	 * punya padanan Criteria yang wajar/efisien (harus tarik SELURUH riwayat transaksi ke memori dulu
	 * kalau dipaksa object query). Tahap 2 (baca detail tiap anggota utk ditampilkan) SENGAJA
	 * {@code session.get(AnggotaKoperasi.class, id)} + getter entitas -- BUKAN raw SQL/join manual ke
	 * {@code jenis_anggota_koperasi} -- supaya {@code wajibPin}/{@code minSaldo} dihitung lewat jalur
	 * yang SAMA PERSIS dengan {@code PosApi.prosesCariMember} (fallback jenis "Reguler" bila FK null
	 * sudah ditangani getter, join manual bisa keliru exclude baris ber-FK null) -- hasil kartu member
	 * di sini jadi drop-in COMPATIBLE dgn hasil pencarian biasa, {@code pilihMember(m)} di Desktop
	 * tidak perlu tahu bedanya.</p>
	 *
	 * @param request payload berisi {@code id_toko} (opsional -- null berarti lintas toko) dan
	 *                {@code limit} (opsional, default 12, maks 30).
	 * @param hasil   diisi {@code status="00"} + {@code data} (array {id,nama,kodeIdentitas,wajibPin,minSaldo,waktuTerakhir}).
	 */
	public static void anggotaTransaksiTerbaru(JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = request.isNull("id_toko") ? null : Long.valueOf(request.get("id_toko").toString());
		int limit = Math.min(30, Math.max(1, request.optInt("limit", 12)));

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder sb = new StringBuilder(
					"select anggota_koperasi as id, max(tanggal_pembayaran) as waktu_terakhir "
							+ "from koperasi.pembelian_anggota_koperasi "
							+ "where anggota_koperasi is not null ");
			if (tokoId != null) {
				sb.append(" and toko = :tokoId ");
			}
			sb.append(" group by anggota_koperasi order by waktu_terakhir desc limit :lim ");
			SQLQuery q = session.createSQLQuery(sb.toString());
			if (tokoId != null) {
				q.setParameter("tokoId", tokoId);
			}
			q.setParameter("lim", Integer.valueOf(limit));

			@SuppressWarnings("unchecked")
			java.util.List<Object[]> baris = q.list();
			JSONArray arr = new JSONArray();
			for (Object[] r : baris) {
				Long anggotaId = ((Number) r[0]).longValue();
				ais.database.model.koperasi.AnggotaKoperasi a = (ais.database.model.koperasi.AnggotaKoperasi) session
						.get(ais.database.model.koperasi.AnggotaKoperasi.class, anggotaId);
				if (a == null) {
					continue; // anggota sudah dihapus sejak transaksi terakhirnya -- lewati, bukan error.
				}
				ais.database.model.koperasi.JenisAnggotaKoperasi jenis = a.getJenisAnggotaKoperasi();
				JSONObject j = new JSONObject();
				j.put("id", a.getId());
				j.put("nama", a.getNama() == null ? "" : a.getNama());
				j.put("kodeIdentitas", a.getKodeIdentitas() == null ? "" : a.getKodeIdentitas());
				j.put("wajibPin", jenis != null && Boolean.TRUE.equals(jenis.getWajibPin()));
				j.put("minSaldo", jenis == null || jenis.getMinimalSaldo() == null ? 0 : jenis.getMinimalSaldo());
				j.put("waktuTerakhir", r[1] == null ? JSONObject.NULL
						: Common.dateFormatInput.get().format((java.util.Date) r[1]));
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Daftar SEMUA cara pembayaran koperasi aktif (tanpa filter member/jenis) -- BEDA dari
	 * {@code cara_bayar_list} (aksi checkout, `PosApi.prosesCaraBayarList`, HANYA mengembalikan
	 * baris yg sudah diizinkan utk jenis keanggotaan SATU member tertentu). Dipakai form "Jenis
	 * Member" (tab Pelanggan) utk menyusun checklist {@code daftarCaraPembayaranYangBolehDiPilih}
	 * itu sendiri -- perlu SEMUA opsi, bukan yg sudah tersaring.
	 */
	public static void caraBayarListSemua(JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT id, nama FROM koperasi.cara_pembayaran_koperasi WHERE COALESCE(aktif,true) = true ORDER BY nama ASC");
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				arr.put(j);
			}
			rs.close();
			ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** Daftar jenis keanggotaan aktif -- dipakai dropdown form "Customer/Anggota" Desktop. */
	public static void jenisAnggotaList(JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT id, nama FROM koperasi.jenis_anggota_koperasi WHERE COALESCE(aktif,true) = true ORDER BY nama ASC");
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				arr.put(j);
			}
			rs.close();
			ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** Daftar tipe keanggotaan aktif -- dipakai dropdown form "Aturan Diskon" Desktop, sama pola dgn {@link #jenisAnggotaList}. */
	public static void tipeAnggotaList(JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT id, nama FROM koperasi.tipe_anggota_koperasi WHERE COALESCE(aktif,true) = true ORDER BY nama ASC");
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				arr.put(j);
			}
			rs.close();
			ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Data Member Baru" (Pelanggan) -- hapus SATU baris {@link AnggotaKoperasi}. Gap-closure
	 * parity dgn JSP {@code anggota_koperasi.jsp} (yg sudah bisa hapus lewat endpoint generik
	 * {@code /Data} action {@code deleteData}) -- Desktop/Android/Flutter belum punya jalur ini sama
	 * sekali sebelum method ini. Gerbang SAMA persis dgn {@link #anggotaSimpan}.
	 *
	 * <p>{@link AnggotaKoperasi} dirujuk banyak tabel riwayat (pembelian_anggota_koperasi, deposit,
	 * dst) -- kalau penghapusan gagal krn constraint FK di database, pesan diarahkan ke solusi yang
	 * BENAR (nonaktifkan, BUKAN hapus) drpd menampilkan pesan SQL mentah yg membingungkan pengguna
	 * non-teknis.</p>
	 */
	public static void anggotaHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilAh = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalAh = pemanggilAh == null;
		boolean supervisorAh = pemanggilAh != null && Boolean.TRUE.equals(pemanggilAh.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggilAh, adminGlobalAh, supervisorAh, "anggota", "delete")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat menghapus data Customer/Anggota.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "ID member wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			AnggotaKoperasi anggota = (AnggotaKoperasi) session.get(AnggotaKoperasi.class, id);
			if (anggota == null) {
				hasil.put("status", "91");
				hasil.put("description", "Member tidak ditemukan.");
				return;
			}
			session.beginTransaction();
			session.delete(anggota);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("description", "Member berhasil dihapus.");
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback,
						"auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:anggotaHapus-rollback");
			}
			hasil.put("status", "91");
			hasil.put("description",
					"Member ini tidak bisa dihapus karena masih punya riwayat transaksi/topup terkait. Nonaktifkan saja member ini (Ubah -> Status Nonaktif).");
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Jenis Member" (tab {@code anggota.jsp}) -- daftar LENGKAP+berpaginasi utk layar CRUD admin,
	 * BEDA dari {@link #jenisAnggotaList} (dropdown ringan id+nama, aktif-only, TIDAK diubah supaya
	 * konsumen lama -- form Customer/Anggota &amp; Aturan Diskon -- tetap dapat SELURUH baris aktif
	 * tanpa terpotong paginasi).
	 *
	 * @param request payload berisi {@code keyword} (opsional, cocok kode/nama), {@code page} (def:1),
	 *                {@code page_size} (def:20, maks 100), {@code termasuk_nonaktif} (def:false).
	 */
	public static void jenisAnggotaListAdmin(JSONObject request, JSONObject hasil) throws Exception {
		String keyword = request.optString("keyword", "").trim();
		boolean termasukNonaktif = request.optBoolean("termasuk_nonaktif", false);
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(1, request.optInt("page_size", 20)));
		int offset = (page - 1) * pageSize;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE 1=1 ");
			if (!termasukNonaktif) where.append(" AND COALESCE(aktif,true) = true ");
			if (!keyword.isEmpty()) where.append(" AND (nama ILIKE ? OR kode ILIKE ?) ");

			java.sql.Connection conn = session.connection();
			java.sql.PreparedStatement psCount = conn
					.prepareStatement("SELECT COUNT(*) FROM koperasi.jenis_anggota_koperasi" + where);
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
				psCount.setString(1, kw);
				psCount.setString(2, kw);
			}
			java.sql.ResultSet rsCount = psCount.executeQuery();
			long total = rsCount.next() ? rsCount.getLong(1) : 0;
			rsCount.close();
			psCount.close();

			java.sql.PreparedStatement ps = conn.prepareStatement(
					"SELECT id, COALESCE(kode,''), nama, COALESCE(keterangan,''), COALESCE(aktif,true), COALESCE(dipilih,true), "
							+ "COALESCE(boleh_entry_topup_oleh_admin,false), COALESCE(istilah_sisa_saldo,'Saldo Kas'), COALESCE(tampilkan_sisa_saldo,true), "
							+ "COALESCE(istilah_cashback,'Cashback'), COALESCE(tampilkan_cashback,true), COALESCE(minimal_saldo,0), "
							+ "COALESCE(wajib_pin,false), COALESCE(wajib_belanja_rutin,false), COALESCE(target_frekuensi_belanja,0), "
							+ "COALESCE(maksimal_pelanggaran,0), COALESCE(daftar_cara_pembayaran_yang_boleh_di_pilih,''), "
							+ "(SELECT COUNT(*) FROM koperasi.anggota_koperasi a WHERE a.jenis_anggota_koperasi = j.id) "
							+ "FROM koperasi.jenis_anggota_koperasi j" + where + " ORDER BY nama ASC LIMIT ? OFFSET ?");
			int idx = 1;
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
				ps.setString(idx++, kw);
				ps.setString(idx++, kw);
			}
			ps.setInt(idx++, pageSize);
			ps.setInt(idx++, offset);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", rs.getString(2));
				j.put("nama", rs.getString(3));
				j.put("keterangan", rs.getString(4));
				j.put("aktif", rs.getBoolean(5));
				j.put("dipilih", rs.getBoolean(6));
				j.put("bolehEntryTopupOlehAdmin", rs.getBoolean(7));
				j.put("istilahSisaSaldo", rs.getString(8));
				j.put("tampilkanSisaSaldo", rs.getBoolean(9));
				j.put("istilahCashback", rs.getString(10));
				j.put("tampilkanCashback", rs.getBoolean(11));
				j.put("minimalSaldo", rs.getDouble(12));
				j.put("wajibPin", rs.getBoolean(13));
				j.put("wajibBelanjaRutin", rs.getBoolean(14));
				j.put("targetFrekuensiBelanja", rs.getInt(15));
				j.put("maksimalPelanggaran", rs.getInt(16));
				j.put("daftarCaraPembayaranYangBolehDiPilih", rs.getString(17));
				j.put("jumlahAnggota", rs.getLong(18));
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("pageSize", pageSize);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** Fitur "Jenis Member" -- simpan (create/update) satu baris {@link JenisAnggotaKoperasi}. */
	public static void jenisAnggotaSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilJa = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalJa = pemanggilJa == null;
		boolean supervisorJa = pemanggilJa != null && Boolean.TRUE.equals(pemanggilJa.getSupervisor());
		String aksiJa = request.isNull("id") ? "create" : "update";
		if (!bolehAksiCrud(tbmuser, pemanggilJa, adminGlobalJa, supervisorJa, "anggota", aksiJa)) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola Jenis Member.");
			return;
		}
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Nama jenis member wajib diisi.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JenisAnggotaKoperasi jenis;
			boolean baru = (id == null);
			if (baru) {
				jenis = new JenisAnggotaKoperasi();
			} else {
				jenis = (JenisAnggotaKoperasi) session.get(JenisAnggotaKoperasi.class, id);
				if (jenis == null) {
					hasil.put("status", "91");
					hasil.put("description", "Jenis member tidak ditemukan.");
					return;
				}
			}
			jenis.setKode(request.optString("kode", ""));
			jenis.setNama(nama);
			jenis.setKeterangan(request.optString("keterangan", ""));
			jenis.setAktif(!request.has("aktif") || request.optBoolean("aktif", true));
			jenis.setDipilih(request.optBoolean("dipilih", true));
			jenis.setBolehEntryTopupOlehAdmin(request.optBoolean("boleh_entry_topup_oleh_admin", false));
			jenis.setIstilahSisaSaldo(request.optString("istilah_sisa_saldo", "Saldo Kas"));
			jenis.setTampilkanSisaSaldo(request.optBoolean("tampilkan_sisa_saldo", true));
			jenis.setIstilahCashback(request.optString("istilah_cashback", "Cashback"));
			jenis.setTampilkanCashback(request.optBoolean("tampilkan_cashback", true));
			jenis.setMinimalSaldo(Double.valueOf(request.optDouble("minimal_saldo", 0)));
			jenis.setWajibPin(request.optBoolean("wajib_pin", false));
			jenis.setWajibBelanjaRutin(request.optBoolean("wajib_belanja_rutin", false));
			jenis.setTargetFrekuensiBelanja(Integer.valueOf(request.optInt("target_frekuensi_belanja", 0)));
			jenis.setMaksimalPelanggaran(Integer.valueOf(request.optInt("maksimal_pelanggaran", 0)));
			jenis.setDaftarCaraPembayaranYangBolehDiPilih(request.optString("daftar_cara_pembayaran_yang_boleh_di_pilih", ""));

			session.beginTransaction();
			session.saveOrUpdate(jenis);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", jenis.getId());
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** Fitur "Jenis Member" -- hapus satu baris {@link JenisAnggotaKoperasi}. */
	public static void jenisAnggotaHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilJh = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalJh = pemanggilJh == null;
		boolean supervisorJh = pemanggilJh != null && Boolean.TRUE.equals(pemanggilJh.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggilJh, adminGlobalJh, supervisorJh, "anggota", "delete")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat menghapus Jenis Member.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "ID jenis member wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JenisAnggotaKoperasi jenis = (JenisAnggotaKoperasi) session.get(JenisAnggotaKoperasi.class, id);
			if (jenis == null) {
				hasil.put("status", "91");
				hasil.put("description", "Jenis member tidak ditemukan.");
				return;
			}
			session.beginTransaction();
			session.delete(jenis);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback,
						"auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:jenisAnggotaHapus-rollback");
			}
			hasil.put("status", "91");
			hasil.put("description", "Jenis member ini tidak bisa dihapus karena masih dipakai member yang ada. Nonaktifkan saja.");
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** Fitur "Tipe Member" -- daftar LENGKAP+berpaginasi, sama pola dgn {@link #jenisAnggotaListAdmin}. */
	public static void tipeAnggotaListAdmin(JSONObject request, JSONObject hasil) throws Exception {
		String keyword = request.optString("keyword", "").trim();
		boolean termasukNonaktif = request.optBoolean("termasuk_nonaktif", false);
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(1, request.optInt("page_size", 20)));
		int offset = (page - 1) * pageSize;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE 1=1 ");
			if (!termasukNonaktif) where.append(" AND COALESCE(aktif,true) = true ");
			if (!keyword.isEmpty()) where.append(" AND (nama ILIKE ? OR kode ILIKE ?) ");

			java.sql.Connection conn = session.connection();
			java.sql.PreparedStatement psCount = conn
					.prepareStatement("SELECT COUNT(*) FROM koperasi.tipe_anggota_koperasi" + where);
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
				psCount.setString(1, kw);
				psCount.setString(2, kw);
			}
			java.sql.ResultSet rsCount = psCount.executeQuery();
			long total = rsCount.next() ? rsCount.getLong(1) : 0;
			rsCount.close();
			psCount.close();

			java.sql.PreparedStatement ps = conn.prepareStatement(
					"SELECT t.id, COALESCE(t.kode,''), t.nama, COALESCE(t.keterangan,''), COALESCE(t.aktif,true), "
							+ "(SELECT COUNT(*) FROM koperasi.anggota_koperasi a WHERE a.tipe_anggota_koperasi = t.id) "
							+ "FROM koperasi.tipe_anggota_koperasi t" + where + " ORDER BY t.nama ASC LIMIT ? OFFSET ?");
			int idx = 1;
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
				ps.setString(idx++, kw);
				ps.setString(idx++, kw);
			}
			ps.setInt(idx++, pageSize);
			ps.setInt(idx++, offset);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", rs.getString(2));
				j.put("nama", rs.getString(3));
				j.put("keterangan", rs.getString(4));
				j.put("aktif", rs.getBoolean(5));
				j.put("jumlahAnggota", rs.getLong(6));
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("pageSize", pageSize);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** Fitur "Tipe Member" -- simpan (create/update) satu baris {@code TipeAnggotaKoperasi}. */
	public static void tipeAnggotaSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilTa = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalTa = pemanggilTa == null;
		boolean supervisorTa = pemanggilTa != null && Boolean.TRUE.equals(pemanggilTa.getSupervisor());
		String aksiTa = request.isNull("id") ? "create" : "update";
		if (!bolehAksiCrud(tbmuser, pemanggilTa, adminGlobalTa, supervisorTa, "anggota", aksiTa)) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola Tipe Member.");
			return;
		}
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Nama tipe member wajib diisi.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.koperasi.TipeAnggotaKoperasi tipe;
			boolean baru = (id == null);
			if (baru) {
				tipe = new ais.database.model.koperasi.TipeAnggotaKoperasi();
			} else {
				tipe = (ais.database.model.koperasi.TipeAnggotaKoperasi) session
						.get(ais.database.model.koperasi.TipeAnggotaKoperasi.class, id);
				if (tipe == null) {
					hasil.put("status", "91");
					hasil.put("description", "Tipe member tidak ditemukan.");
					return;
				}
			}
			tipe.setKode(request.optString("kode", ""));
			tipe.setNama(nama);
			tipe.setKeterangan(request.optString("keterangan", ""));
			tipe.setAktif(!request.has("aktif") || request.optBoolean("aktif", true));

			session.beginTransaction();
			session.saveOrUpdate(tipe);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", tipe.getId());
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** Fitur "Tipe Member" -- hapus satu baris {@code TipeAnggotaKoperasi}. */
	public static void tipeAnggotaHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilTh = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalTh = pemanggilTh == null;
		boolean supervisorTh = pemanggilTh != null && Boolean.TRUE.equals(pemanggilTh.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggilTh, adminGlobalTh, supervisorTh, "anggota", "delete")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat menghapus Tipe Member.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "ID tipe member wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.koperasi.TipeAnggotaKoperasi tipe = (ais.database.model.koperasi.TipeAnggotaKoperasi) session
					.get(ais.database.model.koperasi.TipeAnggotaKoperasi.class, id);
			if (tipe == null) {
				hasil.put("status", "91");
				hasil.put("description", "Tipe member tidak ditemukan.");
				return;
			}
			session.beginTransaction();
			session.delete(tipe);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback,
						"auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:tipeAnggotaHapus-rollback");
			}
			hasil.put("status", "91");
			hasil.put("description", "Tipe member ini tidak bisa dihapus karena masih dipakai member yang ada. Nonaktifkan saja.");
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Topup" (tab {@code anggota.jsp}) -- riwayat/daftar {@link ais.database.model.Deposit}
	 * berpaginasi, dgn filter member+rentang tanggal, dipakai layar admin Pelanggan Flutter (parity dgn
	 * {@code _manajemen_topup.jsp}). BEDA dari {@link #topup}/{@link #tabungan} (saldo TERKINI satu
	 * member, dipakai alur checkout) -- ini daftar histori transaksi topup itu sendiri.
	 *
	 * @param request payload berisi {@code id_member} (opsional), {@code keyword} (opsional, cocok
	 *                nama member), {@code dari}/{@code sampai} (opsional, "yyyy-MM-dd"), {@code page}
	 *                (def:1), {@code page_size} (def:20, maks 100).
	 */
	public static void depositList(JSONObject request, JSONObject hasil) throws Exception {
		Long idMember = request.isNull("id_member") ? null : Long.valueOf((request.get("id_member") + "").trim());
		String keyword = request.optString("keyword", "").trim();
		String dari = request.optString("dari", "").trim();
		String sampai = request.optString("sampai", "").trim();
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(1, request.optInt("page_size", 20)));
		int offset = (page - 1) * pageSize;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE 1=1 ");
			if (idMember != null) where.append(" AND d.anggota_koperasi = ? ");
			if (!keyword.isEmpty()) where.append(" AND a.nama ILIKE ? ");
			if (!dari.isEmpty()) where.append(" AND d.waktu >= ?::date ");
			if (!sampai.isEmpty()) where.append(" AND d.waktu < (?::date + interval '1 day') ");

			java.sql.Connection conn = session.connection();
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			if (idMember != null) params.add(idMember);
			if (!keyword.isEmpty()) params.add("%" + keyword + "%");
			if (!dari.isEmpty()) params.add(dari);
			if (!sampai.isEmpty()) params.add(sampai);

			java.sql.PreparedStatement psCount = conn.prepareStatement(
					"SELECT COUNT(*) FROM public.deposit d LEFT JOIN koperasi.anggota_koperasi a ON d.anggota_koperasi = a.id"
							+ where);
			for (int i = 0; i < params.size(); i++) psCount.setObject(i + 1, params.get(i));
			java.sql.ResultSet rsCount = psCount.executeQuery();
			long total = rsCount.next() ? rsCount.getLong(1) : 0;
			rsCount.close();
			psCount.close();

			java.sql.PreparedStatement ps = conn.prepareStatement(
					"SELECT d.id, d.anggota_koperasi, COALESCE(a.nama, d.nama, '-'), d.nominal, d.waktu, d.tanggal_expired, "
							+ "COALESCE(d.keterangan,''), d.jenis_pembayaran, COALESCE(jp.nama,''), d.jenis_tabungan, COALESCE(jt.nama,''), "
							+ "COALESCE(d.oleh,'') "
							+ "FROM public.deposit d LEFT JOIN koperasi.anggota_koperasi a ON d.anggota_koperasi = a.id "
							+ "LEFT JOIN public.jenis_pembayaran jp ON d.jenis_pembayaran = jp.id "
							+ "LEFT JOIN public.jenis_tabungan jt ON d.jenis_tabungan = jt.id" + where
							+ " ORDER BY d.waktu DESC, d.id DESC LIMIT ? OFFSET ?");
			for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
			ps.setInt(params.size() + 1, pageSize);
			ps.setInt(params.size() + 2, offset);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			java.text.SimpleDateFormat sdfWaktu = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			java.text.SimpleDateFormat sdfTanggal = new java.text.SimpleDateFormat("yyyy-MM-dd");
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				long idAnggotaRow = rs.getLong(2);
				j.put("idMember", rs.wasNull() ? JSONObject.NULL : idAnggotaRow);
				j.put("namaMember", rs.getString(3));
				j.put("nominal", rs.getDouble(4));
				java.sql.Timestamp waktu = rs.getTimestamp(5);
				j.put("waktu", waktu == null ? JSONObject.NULL : sdfWaktu.format(waktu));
				java.sql.Timestamp expired = rs.getTimestamp(6);
				j.put("tanggalExpired", expired == null ? JSONObject.NULL : sdfTanggal.format(expired));
				j.put("keterangan", rs.getString(7));
				long idJp = rs.getLong(8);
				j.put("jenisPembayaranId", rs.wasNull() ? JSONObject.NULL : idJp);
				j.put("jenisPembayaranNama", rs.getString(9));
				long idJt = rs.getLong(10);
				j.put("jenisTabunganId", rs.wasNull() ? JSONObject.NULL : idJt);
				j.put("jenisTabunganNama", rs.getString(11));
				j.put("oleh", rs.getString(12));
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("pageSize", pageSize);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Topup" -- ubah SATU baris {@link ais.database.model.Deposit} yang sudah ada (koreksi
	 * nominal/tanggal/keterangan). Gerbang SAMA dgn {@link #topupSaldo} ({@code Tbmrole.bolehEntryTopup}).
	 */
	public static void depositUbah(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.Tbmrole roleDu = tbmuser == null ? null : tbmuser.hakAkses();
		boolean bolehDu = roleDu != null && roleDu.getBolehEntryTopup() != null && roleDu.getBolehEntryTopup().booleanValue();
		if (!bolehDu) {
			hasil.put("status", "91");
			hasil.put("description", "Anda tidak memiliki hak akses untuk mengubah entri topup.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "ID topup wajib diisi.");
			return;
		}
		double nominal = request.optDouble("nominal", 0);
		if (nominal <= 0) {
			hasil.put("status", "91");
			hasil.put("description", "Nominal topup harus lebih dari 0.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.Deposit deposit = (ais.database.model.Deposit) session.get(ais.database.model.Deposit.class, id);
			if (deposit == null) {
				hasil.put("status", "91");
				hasil.put("description", "Data topup tidak ditemukan.");
				return;
			}
			deposit.setNominal(Double.valueOf(nominal));
			deposit.setKeterangan(request.optString("keterangan", ""));
			if (!request.isNull("waktu") && !request.optString("waktu", "").trim().isEmpty()) {
				try {
					deposit.setWaktu(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(request.optString("waktu")));
				} catch (Exception eParse) {
					ais.common.ErrorAuditUtil.record(eParse, "auto-audit depositUbah-parse-waktu src/ais/action/servlet/api/KantinHelper.java");
				}
			}
			if (!request.isNull("tanggal_expired") && !request.optString("tanggal_expired", "").trim().isEmpty()) {
				try {
					java.util.Date exp = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(request.optString("tanggal_expired"));
					java.util.Calendar cal = java.util.Calendar.getInstance();
					cal.setTime(exp);
					cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
					cal.set(java.util.Calendar.MINUTE, 59);
					cal.set(java.util.Calendar.SECOND, 59);
					deposit.setTanggalExpired(cal.getTime());
				} catch (Exception eParse) {
					ais.common.ErrorAuditUtil.record(eParse, "auto-audit depositUbah-parse-expired src/ais/action/servlet/api/KantinHelper.java");
				}
			} else if (request.has("tanggal_expired")) {
				deposit.setTanggalExpired(null);
			}
			if (!request.isNull("jenis_pembayaran_id")) {
				deposit.setJenisPembayaran((ais.database.model.JenisPembayaran) session.get(ais.database.model.JenisPembayaran.class,
						Long.valueOf((request.get("jenis_pembayaran_id") + "").trim())));
			}
			if (!request.isNull("jenis_tabungan_id")) {
				deposit.setJenisTabungan((ais.database.model.JenisTabungan) session.get(ais.database.model.JenisTabungan.class,
						Long.valueOf((request.get("jenis_tabungan_id") + "").trim())));
			}
			session.beginTransaction();
			session.saveOrUpdate(deposit);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Topup" -- hapus SATU baris {@link ais.database.model.Deposit}. Gerbang SAMA dgn
	 * {@link #topupSaldo}/{@link #depositUbah}.
	 */
	public static void depositHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.Tbmrole roleDh = tbmuser == null ? null : tbmuser.hakAkses();
		boolean bolehDh = roleDh != null && roleDh.getBolehEntryTopup() != null && roleDh.getBolehEntryTopup().booleanValue();
		if (!bolehDh) {
			hasil.put("status", "91");
			hasil.put("description", "Anda tidak memiliki hak akses untuk menghapus entri topup.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "ID topup wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.Deposit deposit = (ais.database.model.Deposit) session.get(ais.database.model.Deposit.class, id);
			if (deposit == null) {
				hasil.put("status", "91");
				hasil.put("description", "Data topup tidak ditemukan.");
				return;
			}
			session.beginTransaction();
			session.delete(deposit);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Notifikasi" (tab {@code anggota.jsp}) -- log/riwayat {@link ais.database.model.Notifikasi}
	 * yg SUDAH dikirim ke member (mis. peringatan wajib-belanja-rutin), BUKAN layar konfigurasi. Sama
	 * query dgn {@code notifikasi.jsp} (join {@code n.nama = ak.userid}). Halaman JSP asalnya ADMIN-ONLY
	 * (toko==null) -- gerbang direplikasi PERSIS di sini, BUKAN dilonggarkan ke supervisor toko.
	 */
	public static void notifikasiList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		boolean isAdminNl = tbmuser != null && tbmuser.getPedagang() == null;
		if (!isAdminNl) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin yang dapat melihat log Notifikasi.");
			return;
		}
		String keyword = request.optString("keyword", "").trim();
		String dari = request.optString("dari", "").trim();
		String sampai = request.optString("sampai", "").trim();
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(1, request.optInt("page_size", 20)));
		int offset = (page - 1) * pageSize;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE 1=1 ");
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			if (!keyword.isEmpty()) {
				where.append(" AND (n.nama ILIKE ? OR n.keterangan ILIKE ? OR ak.nama ILIKE ? OR ak.hp ILIKE ?) ");
				String kw = "%" + keyword + "%";
				params.add(kw);
				params.add(kw);
				params.add(kw);
				params.add(kw);
			}
			if (!dari.isEmpty()) {
				where.append(" AND n.waktu >= ?::date ");
				params.add(dari);
			}
			if (!sampai.isEmpty()) {
				where.append(" AND n.waktu < (?::date + interval '1 day') ");
				params.add(sampai);
			}

			java.sql.Connection conn = session.connection();
			java.sql.PreparedStatement psCount = conn.prepareStatement(
					"SELECT COUNT(n.id) FROM public.notifikasi n LEFT JOIN koperasi.anggota_koperasi ak ON n.nama = ak.userid"
							+ where);
			for (int i = 0; i < params.size(); i++) psCount.setObject(i + 1, params.get(i));
			java.sql.ResultSet rsCount = psCount.executeQuery();
			long total = rsCount.next() ? rsCount.getLong(1) : 0;
			rsCount.close();
			psCount.close();

			java.sql.PreparedStatement ps = conn.prepareStatement(
					"SELECT n.id, n.waktu, n.nama, COALESCE(n.emails,''), COALESCE(n.keterangan,''), COALESCE(n.hasil,''), "
							+ "COALESCE(n.buka,false), COALESCE(ak.nama,''), COALESCE(ak.hp,'') "
							+ "FROM public.notifikasi n LEFT JOIN koperasi.anggota_koperasi ak ON n.nama = ak.userid" + where
							+ " ORDER BY n.waktu DESC, n.id DESC LIMIT ? OFFSET ?");
			for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
			ps.setInt(params.size() + 1, pageSize);
			ps.setInt(params.size() + 2, offset);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				java.sql.Timestamp waktu = rs.getTimestamp(2);
				j.put("waktu", waktu == null ? JSONObject.NULL : sdf.format(waktu));
				j.put("userid", rs.getString(3));
				j.put("emails", rs.getString(4));
				j.put("keterangan", rs.getString(5));
				j.put("tipe", rs.getString(6));
				j.put("buka", rs.getBoolean(7));
				j.put("namaMember", rs.getString(8));
				j.put("telpMember", rs.getString(9));
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("pageSize", pageSize);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** Fitur "Notifikasi" -- hapus satu baris log. Gerbang SAMA (admin-only) dgn {@link #notifikasiList}. */
	public static void notifikasiHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		boolean isAdminNh = tbmuser != null && tbmuser.getPedagang() == null;
		if (!isAdminNh) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin yang dapat menghapus log Notifikasi.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "ID notifikasi wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.Notifikasi n = (ais.database.model.Notifikasi) session.get(ais.database.model.Notifikasi.class, id);
			if (n == null) {
				hasil.put("status", "91");
				hasil.put("description", "Data notifikasi tidak ditemukan.");
				return;
			}
			session.beginTransaction();
			session.delete(n);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Sinkronisasi Siswa/Mahasiswa" -- populasi dropdown (Koperasi/Fakultas/Jurusan/Yayasan/
	 * Sekolah), reuse PERSIS query {@code sinkron_siswa_mahasiswa_service.jsp} supaya daftar yg
	 * ditampilkan Flutter identik dgn versi JSP.
	 *
	 * @param request payload berisi {@code tipe} (wajib: "koperasi"|"fakultas"|"jurusan"|"yayasan"|
	 *                "sekolah") dan {@code induk_id} (opsional, dipakai "jurusan" utk filter fakultas,
	 *                "sekolah" utk filter yayasan).
	 */
	public static void sinkronReferensi(JSONObject request, JSONObject hasil) throws Exception {
		String tipe = request.optString("tipe", "").trim();
		Long indukId = request.isNull("induk_id") ? null : Long.valueOf((request.get("induk_id") + "").trim());
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JSONArray arr = new JSONArray();
			if ("koperasi".equals(tipe)) {
				for (Object o : session.createCriteria(ais.database.model.koperasi.Koperasi.class)
						.addOrder(Order.asc("nama")).list()) {
					ais.database.model.koperasi.Koperasi k = (ais.database.model.koperasi.Koperasi) o;
					JSONObject j = new JSONObject();
					j.put("id", k.getId());
					j.put("nama", k.getNama() == null ? ("Koperasi #" + k.getId()) : k.getNama());
					arr.put(j);
				}
			} else if ("fakultas".equals(tipe)) {
				for (Object o : session.createCriteria(ais.database.model.Fakultas.class)
						.addOrder(Order.asc("nama")).list()) {
					ais.database.model.Fakultas f = (ais.database.model.Fakultas) o;
					JSONObject j = new JSONObject();
					j.put("id", f.getId());
					j.put("nama", f.getNama() == null ? "-" : f.getNama());
					arr.put(j);
				}
			} else if ("jurusan".equals(tipe)) {
				org.hibernate.Criteria c = session.createCriteria(ais.database.model.Jurusan.class)
						.addOrder(Order.asc("nama"));
				if (indukId != null) {
					ais.database.model.Fakultas fk = (ais.database.model.Fakultas) session.get(ais.database.model.Fakultas.class, indukId);
					if (fk != null) c.add(Restrictions.eq("fakultas", fk));
				}
				for (Object o : c.list()) {
					ais.database.model.Jurusan j2 = (ais.database.model.Jurusan) o;
					JSONObject j = new JSONObject();
					j.put("id", j2.getId());
					j.put("nama", j2.getNama() == null ? "-" : j2.getNama());
					arr.put(j);
				}
			} else if ("yayasan".equals(tipe)) {
				for (Object o : session.createCriteria(ais.database.model.sekolah.Yayasan.class)
						.addOrder(Order.asc("nama")).list()) {
					ais.database.model.sekolah.Yayasan y = (ais.database.model.sekolah.Yayasan) o;
					JSONObject j = new JSONObject();
					j.put("id", y.getId());
					j.put("nama", y.getNama() == null ? "-" : y.getNama());
					arr.put(j);
				}
			} else if ("sekolah".equals(tipe)) {
				org.hibernate.Criteria c = session.createCriteria(ais.database.model.sekolah.Sekolah.class)
						.addOrder(Order.asc("nama"));
				if (indukId != null) {
					ais.database.model.sekolah.Yayasan yy = (ais.database.model.sekolah.Yayasan) session
							.get(ais.database.model.sekolah.Yayasan.class, indukId);
					if (yy != null) c.add(Restrictions.eq("yayasan", yy));
				}
				for (Object o : c.list()) {
					ais.database.model.sekolah.Sekolah s = (ais.database.model.sekolah.Sekolah) o;
					JSONObject j = new JSONObject();
					j.put("id", s.getId());
					j.put("nama", s.getNama() == null ? "-" : s.getNama());
					arr.put(j);
				}
			} else {
				hasil.put("status", "91");
				hasil.put("description", "Tipe referensi tidak dikenali.");
				return;
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Sinkronisasi Siswa/Mahasiswa" -- jalankan sinkron Mahasiswa -&gt; AnggotaKoperasi, reuse
	 * PERSIS {@code Common.checkApakahMahasiswaOtomatisMenjadiAnggotaKoperasi} (satu sumber logika dgn
	 * versi JSP/ZK). Gerbang: admin global ATAU supervisor toko SAJA (TIDAK ada fallback role granular
	 * -- sengaja disamakan dgn {@code LokasiKantinUtil.bolehKelola} versi JSP, bukan {@link
	 * #bolehAksiCrud}, supaya konsisten dgn permintaan awal "sesuai versi JSP").
	 *
	 * @param request payload berisi {@code koperasi_id} (wajib), {@code tahun} (wajib, tahun angkatan),
	 *                {@code fakultas_id}/{@code jurusan_id} (opsional, filter kandidat).
	 */
	public static void sinkronMahasiswa(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilSm = tbmuser == null ? null : tbmuser.getPedagang();
		boolean bolehSm = pemanggilSm == null || Boolean.TRUE.equals(pemanggilSm.getSupervisor());
		if (!bolehSm) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/supervisor toko yang boleh menjalankan sinkronisasi.");
			return;
		}
		if (request.isNull("koperasi_id") || request.isNull("tahun")) {
			hasil.put("status", "91");
			hasil.put("description", "Koperasi dan Tahun Angkatan wajib diisi.");
			return;
		}
		Long koperasiId = Long.valueOf((request.get("koperasi_id") + "").trim());
		Integer tahun = Integer.valueOf((request.get("tahun") + "").trim());
		Long fakultasId = request.isNull("fakultas_id") ? null : Long.valueOf((request.get("fakultas_id") + "").trim());
		Long jurusanId = request.isNull("jurusan_id") ? null : Long.valueOf((request.get("jurusan_id") + "").trim());

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.koperasi.Koperasi koperasi = (ais.database.model.koperasi.Koperasi) session
					.get(ais.database.model.koperasi.Koperasi.class, koperasiId);
			if (koperasi == null) {
				hasil.put("status", "91");
				hasil.put("description", "Koperasi tidak ditemukan.");
				return;
			}
			ais.database.model.Jurusan jurusanEntity = jurusanId != null
					? (ais.database.model.Jurusan) session.get(ais.database.model.Jurusan.class, jurusanId) : null;
			ais.database.model.Fakultas fakultasEntity = fakultasId != null
					? (ais.database.model.Fakultas) session.get(ais.database.model.Fakultas.class, fakultasId) : null;
			org.hibernate.Criteria cq = session.createCriteria(ais.database.model.Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.createAlias("jurusan", "jurusan")
					.add(Restrictions.eq("tahunangkatan", tahun))
					.setProjection(org.hibernate.criterion.Projections.groupProperty("nim"));
			if (jurusanEntity != null) cq.add(Restrictions.eq("jurusan", jurusanEntity));
			if (fakultasEntity != null) cq.add(Restrictions.eq("jurusan.fakultas", fakultasEntity));
			@SuppressWarnings("unchecked")
			java.util.List<String> ids = cq.list();

			int berhasil = 0, gagal = 0;
			for (String nim : ids) {
				try {
					if (Common.checkApakahMahasiswaOtomatisMenjadiAnggotaKoperasi(nim, koperasi) != null) berhasil++;
				} catch (Exception exSatu) {
					gagal++;
					ais.common.ErrorAuditUtil.record(exSatu,
							"auto-audit sinkronMahasiswa src/ais/action/servlet/api/KantinHelper.java nim=" + nim);
				}
			}
			hasil.put("status", "00");
			hasil.put("total", ids.size());
			hasil.put("berhasil", berhasil);
			hasil.put("gagal", gagal);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Sinkronisasi Siswa/Mahasiswa" -- jalankan sinkron Siswa -&gt; AnggotaKoperasi, padanan
	 * {@link #sinkronMahasiswa} utk jenjang sekolah. Sama gerbang &amp; sumber logika (lihat JavaDoc
	 * {@link #sinkronMahasiswa}).
	 *
	 * @param request payload berisi {@code koperasi_id} (wajib), {@code tahun} (wajib, tahun masuk),
	 *                {@code yayasan_id}/{@code sekolah_id} (opsional, filter kandidat).
	 */
	public static void sinkronSiswa(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilSs = tbmuser == null ? null : tbmuser.getPedagang();
		boolean bolehSs = pemanggilSs == null || Boolean.TRUE.equals(pemanggilSs.getSupervisor());
		if (!bolehSs) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/supervisor toko yang boleh menjalankan sinkronisasi.");
			return;
		}
		if (request.isNull("koperasi_id") || request.isNull("tahun")) {
			hasil.put("status", "91");
			hasil.put("description", "Koperasi dan Tahun Masuk wajib diisi.");
			return;
		}
		Long koperasiId = Long.valueOf((request.get("koperasi_id") + "").trim());
		Integer tahun = Integer.valueOf((request.get("tahun") + "").trim());
		Long yayasanId = request.isNull("yayasan_id") ? null : Long.valueOf((request.get("yayasan_id") + "").trim());
		Long sekolahId = request.isNull("sekolah_id") ? null : Long.valueOf((request.get("sekolah_id") + "").trim());

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.koperasi.Koperasi koperasi = (ais.database.model.koperasi.Koperasi) session
					.get(ais.database.model.koperasi.Koperasi.class, koperasiId);
			if (koperasi == null) {
				hasil.put("status", "91");
				hasil.put("description", "Koperasi tidak ditemukan.");
				return;
			}
			ais.database.model.sekolah.Sekolah sekolahEntity = sekolahId != null
					? (ais.database.model.sekolah.Sekolah) session.get(ais.database.model.sekolah.Sekolah.class, sekolahId) : null;
			ais.database.model.sekolah.Yayasan yayasanEntity = yayasanId != null
					? (ais.database.model.sekolah.Yayasan) session.get(ais.database.model.sekolah.Yayasan.class, yayasanId) : null;
			org.hibernate.Criteria cq = session.createCriteria(ais.database.model.sekolah.Siswa.class)
					.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
					.add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("tahunMasuk", tahun));
			if (sekolahEntity != null) cq.add(Restrictions.eq("sekolah", sekolahEntity));
			if (yayasanEntity != null) cq.add(Restrictions.eq("yayasan", yayasanEntity));
			@SuppressWarnings("unchecked")
			java.util.List<ais.database.model.sekolah.Siswa> daftarSiswa = cq.list();

			int berhasil = 0, gagal = 0;
			for (ais.database.model.sekolah.Siswa siswa : daftarSiswa) {
				try {
					if (Common.checkApakahSiswaOtomatisMenjadiAnggotaKoperasi(siswa, koperasi) != null) berhasil++;
				} catch (Exception exSatu) {
					gagal++;
					ais.common.ErrorAuditUtil.record(exSatu,
							"auto-audit sinkronSiswa src/ais/action/servlet/api/KantinHelper.java siswaId=" + siswa.getId());
				}
			}
			hasil.put("status", "00");
			hasil.put("total", daftarSiswa.size());
			hasil.put("berhasil", berhasil);
			hasil.put("gagal", gagal);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Layar baru "Aturan Diskon" di Desktop (permintaan "fitur mengatur diskon seperti fitur POS
	 * online") -- kelola baris {@link ais.database.model.koperasi.AturanDiskon} (mesin promo yang
	 * SUDAH ADA dan SUDAH otomatis diterapkan saat checkout ZK/JSP, lihat {@code PosKantinAction.evaluasiDiskon}).
	 * Method ini HANYA menambah/mengubah/mendaftar ATURANNYA -- TIDAK mengubah/mereplikasi logika
	 * penerapan diskon saat checkout itu sendiri (di luar cakupan permintaan ini; penerapan otomatis
	 * di checkout Desktop butuh porting {@code evaluasiDiskon} terpisah, belum dikerjakan di sini).
	 *
	 * @param request payload berisi {@code keyword} (opsional, cocok nama aturan), {@code page}
	 *                (def:1), {@code page_size} (def:20, maks 100).
	 * @param hasil   diisi {@code status="00"}, {@code data} (array {@code {id, namaAturan,
	 *                produkNama, tokoNama, persentase, nominal, potonganLangsung, aktif, tanggalMulai,
	 *                tanggalSelesai}}), dan {@code total}.
	 */
	public static void diskonList(JSONObject request, JSONObject hasil) throws Exception {
		String keyword = request.optString("keyword", "").trim();
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(1, request.optInt("page_size", 20)));
		int offset = (page - 1) * pageSize;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String andKw = keyword.isEmpty() ? "" : " AND a.nama_aturan ILIKE ?";
			java.sql.Connection conn = session.connection();

			java.sql.PreparedStatement psCount = conn
					.prepareStatement("SELECT COUNT(*) FROM koperasi.aturan_diskon a WHERE 1=1" + andKw);
			if (!keyword.isEmpty()) {
				psCount.setString(1, "%" + keyword + "%");
			}
			java.sql.ResultSet rsCount = psCount.executeQuery();
			long total = rsCount.next() ? rsCount.getLong(1) : 0;
			rsCount.close();
			psCount.close();

			java.sql.PreparedStatement ps = conn.prepareStatement(
					"SELECT a.id, a.nama_aturan, COALESCE(p.nama,''), COALESCE(t.nama,''), "
							+ "COALESCE(a.persentase,0), COALESCE(a.nominal,0), COALESCE(a.potongan_langsung,true), COALESCE(a.aktif,true), "
							+ "a.tanggal_mulai, a.tanggal_selesai "
							+ "FROM koperasi.aturan_diskon a "
							+ "LEFT JOIN koperasi.produk p ON a.produk = p.id "
							+ "LEFT JOIN koperasi.toko t ON a.toko = t.id "
							+ "WHERE 1=1" + andKw + " ORDER BY a.id DESC LIMIT ? OFFSET ?");
			int idx = 1;
			if (!keyword.isEmpty()) {
				ps.setString(idx++, "%" + keyword + "%");
			}
			ps.setInt(idx++, pageSize);
			ps.setInt(idx++, offset);
			java.sql.ResultSet rs = ps.executeQuery();
			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm");
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("namaAturan", rs.getString(2));
				j.put("produkNama", rs.getString(3));
				j.put("tokoNama", rs.getString(4));
				j.put("persentase", rs.getDouble(5));
				j.put("nominal", rs.getDouble(6));
				j.put("potonganLangsung", rs.getBoolean(7));
				j.put("aktif", rs.getBoolean(8));
				java.sql.Timestamp tm = rs.getTimestamp(9);
				java.sql.Timestamp ts = rs.getTimestamp(10);
				j.put("tanggalMulai", tm == null ? "" : fmt.format(tm));
				j.put("tanggalSelesai", ts == null ? "" : fmt.format(ts));
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("pageSize", pageSize);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Simpan (buat/ubah) SATU aturan diskon -- lihat JavaDoc {@link #diskonList}.
	 *
	 * <p><b>Gerbang toko</b>: akun kasir toko ({@code tbmuser.getPedagang() != null}) HANYA boleh
	 * membuat/mengubah aturan milik tokonya SENDIRI -- {@code toko} dikunci server-side ke toko akun
	 * itu, TIDAK PERNAH dari input klien (pola IDOR-safe sama dgn {@code resolveTokoId}). Akun
	 * admin/manager boleh memilih toko manapun ATAU mengosongkannya (berlaku GLOBAL semua toko).</p>
	 *
	 * @param request payload: {@code id} (opsional, utk ubah), {@code nama_aturan} (wajib),
	 *                {@code keterangan}, {@code berlaku_semua_produk} (bila true, {@code produk}
	 *                diabaikan -- aturan berlaku semua produk), {@code kode_produk} (wajib bila
	 *                {@code berlaku_semua_produk=false}), {@code toko_id} (admin saja -- kosong berarti
	 *                global), {@code berlaku_semua_member}, {@code jenis_anggota_id},
	 *                {@code tipe_anggota_id}, {@code persentase}, {@code maksimal_potongan},
	 *                {@code nominal}, {@code potongan_langsung}, {@code berlaku_per_hari_dan_per_toko},
	 *                {@code tanggal_mulai}/{@code tanggal_selesai} (format {@code yyyy-MM-dd'T'HH:mm},
	 *                opsional), {@code aktif}.
	 */
	public static void diskonSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilDiskon = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalDiskon = pemanggilDiskon == null;
		boolean supervisorDiskon = pemanggilDiskon != null && Boolean.TRUE.equals(pemanggilDiskon.getSupervisor());
		String aksiDiskonSimpan = request.isNull("id") ? "create" : "update";
		if (!bolehAksiCrud(tbmuser, pemanggilDiskon, adminGlobalDiskon, supervisorDiskon, "diskon", aksiDiskonSimpan)) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola aturan diskon.");
			return;
		}
		String namaAturan = request.optString("nama_aturan", "").trim();
		if (namaAturan.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Nama aturan diskon wajib diisi.");
			return;
		}
		boolean berlakuSemuaProduk = request.optBoolean("berlaku_semua_produk", true);
		String kodeProduk = request.optString("kode_produk", "").trim();
		if (!berlakuSemuaProduk && kodeProduk.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Pilih produk target promo, atau centang \"Berlaku Semua Produk\".");
			return;
		}

		ais.database.model.inventory.Pedagang pedagangPembuat = tbmuser == null ? null : tbmuser.getPedagang();
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		Long idJenis = request.isNull("jenis_anggota_id") ? null : Long.valueOf((request.get("jenis_anggota_id") + "").trim());
		Long idTipe = request.isNull("tipe_anggota_id") ? null : Long.valueOf((request.get("tipe_anggota_id") + "").trim());

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.koperasi.AturanDiskon a;
			if (id == null) {
				a = new ais.database.model.koperasi.AturanDiskon();
			} else {
				a = (ais.database.model.koperasi.AturanDiskon) session.get(ais.database.model.koperasi.AturanDiskon.class, id);
				if (a == null) {
					hasil.put("status", "91");
					hasil.put("description", "Aturan diskon tidak ditemukan.");
					return;
				}
			}

			// Resolusi produk (null = berlaku semua produk)
			if (berlakuSemuaProduk) {
				a.setProduk(null);
			} else {
				Object[] rowProduk = (Object[]) session
						.createSQLQuery("SELECT id FROM koperasi.produk WHERE kode = :k")
						.setParameter("k", kodeProduk).uniqueResult();
				if (rowProduk == null) {
					hasil.put("status", "91");
					hasil.put("description", "Produk dengan kode \"" + kodeProduk + "\" tidak ditemukan.");
					return;
				}
				a.setProduk((Produk) session.get(Produk.class, ((Number) rowProduk[0]).longValue()));
			}

			// Resolusi toko: kasir toko WAJIB terkunci ke tokonya sendiri, admin bebas (termasuk kosong/global)
			if (pedagangPembuat != null) {
				a.setToko(pedagangPembuat.getToko());
			} else if (!request.isNull("toko_id") && !((request.get("toko_id") + "").trim().isEmpty())) {
				Long tokoId = Long.valueOf((request.get("toko_id") + "").trim());
				a.setToko((Toko) session.get(Toko.class, tokoId));
			} else {
				a.setToko(null);
			}

			a.setNamaAturan(namaAturan);
			a.setKeterangan(request.optString("keterangan", ""));
			a.setBerlakuSemuaMember(request.optBoolean("berlaku_semua_member", true));
			a.setJenisAnggota(idJenis == null ? null : (JenisAnggotaKoperasi) session.get(JenisAnggotaKoperasi.class, idJenis));
			a.setTipeAnggota(idTipe == null ? null
					: (ais.database.model.koperasi.TipeAnggotaKoperasi) session.get(ais.database.model.koperasi.TipeAnggotaKoperasi.class, idTipe));
			a.setPersentase(request.optDouble("persentase", 0));
			a.setMaksimalPotongan(request.optDouble("maksimal_potongan", 0));
			a.setNominal(request.optDouble("nominal", 0));
			a.setPotonganLangsung(request.optBoolean("potongan_langsung", true));
			a.setBerlakuPerHariDanPerToko(request.optBoolean("berlaku_per_hari_dan_per_toko", false));
			a.setAktif(request.optBoolean("aktif", true));

			java.text.SimpleDateFormat fmtInput = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
			String tglMulaiStr = request.optString("tanggal_mulai", "").trim();
			a.setTanggalMulai(tglMulaiStr.isEmpty() ? null : fmtInput.parse(tglMulaiStr));
			String tglSelesaiStr = request.optString("tanggal_selesai", "").trim();
			a.setTanggalSelesai(tglSelesaiStr.isEmpty() ? null : fmtInput.parse(tglSelesaiStr));

			session.beginTransaction();
			session.saveOrUpdate(a);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", a.getId());
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Mengecek status pembayaran ONLINE (VA/QRIS/e-wallet) berdasarkan {@code kodeUnik} yang
	 * di-generate saat transaksi dimulai — dipoll berkala dari front-end kasir selagi menunggu
	 * pembeli menyelesaikan pembayaran di kanal eksternal (mis. scan QRIS di aplikasi bank).
	 *
	 * <p>{@code kodeUnik} WAJIB minimal 50 karakter (validasi eksplisit di sini) — panjang minimum ini
	 * bukan angka sembarang, melainkan syarat keunikan praktis supaya kode transaksi tidak bentrok
	 * antar-toko/kasir yang memproses pembayaran bersamaan (lihat pembangkitannya di
	 * {@code PosKantinAction.generateKodeUnik()}). Kode yang ditemukan berarti gateway pembayaran
	 * sudah mengonfirmasi pelunasan (baris {@link KodePembayaranOnline} dibuat oleh callback gateway
	 * terpisah, di luar method ini) — method ini HANYA membaca status, tidak pernah membuatnya.</p>
	 *
	 * @param request payload berisi {@code kodeUnik} (wajib, minimal 50 karakter).
	 * @param hasil   diisi {@code status="00"} + {@code member}/{@code data} bila kode ditemukan
	 *                (pembayaran sukses); {@code status="01"} dengan {@code description} penjelas bila
	 *                kode tidak valid/tidak ditemukan; {@code status="90"} bila terjadi error tak
	 *                terduga saat memproses permintaan.
	 */
	public static void checkBayar(JSONObject request, JSONObject hasil) {

		try {

			if (!request.isNull("kodeUnik")) {

				String kodeUnik = !request.isNull("kodeUnik") ? (request.getString("kodeUnik")).trim() : "";

				if (!kodeUnik.isEmpty()) {
					if (kodeUnik.length() >= 50) {

						Session session = HibernateUtil.getSessionFactory().openSession();
						try {
							KodePembayaranOnline kodePembayaranOnline = (KodePembayaranOnline) session
									.createCriteria(KodePembayaranOnline.class).add(Restrictions.eq("kode", kodeUnik))
									.uniqueResult();

							if (kodePembayaranOnline != null) {
								hasil.put("member", kodePembayaranOnline.getAnggotaKoperasi().getId());
								hasil.put("data", kodePembayaranOnline.getId());
								hasil.put("status", "00");
								hasil.put("description", "Pembayaran berhasil");
							} else {
								hasil.put("status", "01");
								hasil.put("description", "KOde kodeUnik " + kodeUnik + " tidak ditemukan");
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/KantinHelper.java:452");
						} finally {
							try {
								try { session.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:455");}
								session.disconnect();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:457");
								// TODO: handle exception
							}
							try {
								session.close();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:462");
								// TODO: handle exception
							}
						}

					} else {
						hasil.put("status", "01");
						hasil.put("description", "Parameter kodeUnik minimal 50 digit");
					}
				} else {
					hasil.put("status", "01");
					hasil.put("description", "Parameter kodeUnik tidak ditemukan");
				}

			} else {
				hasil.put("status", "01");
				hasil.put("description", "Parameter kode tidak ditemukan");
			}
		} catch (Exception e) {
			try {
				hasil.put("status", "90");
				hasil.put("description", Common.tampilErrorJikaAdmin(e));
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/KantinHelper.java:485");
			}
		}
	}

	/**
	 * Resolusi {@code tokoId} IDOR-safe dipakai bersama seluruh aksi "Stok Opname" (aplikasi Android
	 * baru) -- pola SAMA PERSIS dengan {@link #pedagangList} (kasir/pedagang selalu scope tokonya
	 * sendiri, dari sesi login server, TIDAK PERNAH dipercaya dari klien; admin global wajib kirim
	 * {@code toko_id} eksplisit karena tidak terikat satu toko).
	 */
	private static Long soResolveTokoId(Tbmuser tbmuser, JSONObject request) throws Exception {
		ais.database.model.inventory.Pedagang pemanggil = tbmuser == null ? null : tbmuser.getPedagang();
		if (pemanggil != null) {
			// Multi-toko (lihat JavaDoc Tbmuser.tokoAktifMultiToko): kalau pengguna ini PERNAH
			// memilih toko (lewat sesi_kas_buka), field ini SELALU dipercaya lebih dulu -- pengguna
			// toko-tunggal biasa tidak pernah mengisi field ini jadi cabang ini tidak berlaku bagi
			// mereka (perilaku lama di baris berikutnya tetap sama persis).
			Long tokoAktifMulti = tbmuser.getTokoAktifMultiToko();
			if (tokoAktifMulti != null) {
				return tokoAktifMulti;
			}
			return pemanggil.getToko() == null ? null : pemanggil.getToko().getId();
		}
		return request.isNull("toko_id") ? null : Long.valueOf((request.get("toko_id") + "").trim());
	}

	/**
	 * Daftar SEMUA toko yang boleh dioperasikan {@code tbmuser} ini -- gabungan (a) toko dari relasi
	 * lama {@code Tbmuser.pedagang} (FK tunggal, "rumah" toko pengguna ini) DAN (b) toko dari setiap
	 * baris {@code Pedagang.tbmuser} aktif milik pengguna ini (relasi KEBALIKANNYA, diisi lewat layar
	 * admin "Ambil Pengguna" -- {@code toko.zul}/{@code PedagangAction}, sebelumnya TIDAK PERNAH
	 * dibaca oleh PosApi/KantinHelper sama sekali). Dedup by id, urutan tidak dijamin.
	 *
	 * <p>Utk pengguna toko-tunggal (kasus paling umum), gabungan ini SELALU berukuran <=1 -- dipakai
	 * jugai oleh {@link #sesiKasBuka} utk memutuskan apakah kombinasi pilih-toko perlu ditampilkan
	 * sama sekali di klien.</p>
	 */
	public static java.util.List<Toko> daftarTokoBolehDiakses(Session session, Tbmuser tbmuser) {
		java.util.LinkedHashMap<Long, Toko> peta = new java.util.LinkedHashMap<Long, Toko>();
		ais.database.model.Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		if (role != null && role.getTokoAksesJson() != null) {
			try {
				JSONArray arr = new JSONArray(role.getTokoAksesJson());
				for (int i = 0; i < arr.length(); i++) {
					Object item = arr.get(i);
					Long id = null;
					if (item instanceof JSONObject) {
						JSONObject obj = (JSONObject) item;
						if (obj.has("id")) id = Long.valueOf(obj.get("id").toString());
						else if (obj.has("tokoId")) id = Long.valueOf(obj.get("tokoId").toString());
					} else {
						id = Long.valueOf(item.toString());
					}
					if (id == null) continue;
					Toko toko = (Toko) session.get(Toko.class, id);
					if (toko != null && toko.getId() != null) {
						peta.put(toko.getId(), toko);
					}
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
			if (!peta.isEmpty()) {
				return new java.util.ArrayList<Toko>(peta.values());
			}
		}
		ais.database.model.inventory.Pedagang milikSendiri = tbmuser == null ? null : tbmuser.getPedagang();
		if (milikSendiri != null && milikSendiri.getToko() != null) {
			peta.put(milikSendiri.getToko().getId(), milikSendiri.getToko());
		}
		if (tbmuser != null) {
			java.util.List<?> daftarPedagang = session.createCriteria(ais.database.model.inventory.Pedagang.class)
					.add(Restrictions.eq("tbmuser", tbmuser))
					.add(Restrictions.eq("aktif", true)).list();
			for (Object o : daftarPedagang) {
				ais.database.model.inventory.Pedagang p = (ais.database.model.inventory.Pedagang) o;
				if (p.getToko() != null) {
					peta.put(p.getToko().getId(), p.getToko());
				}
			}
		}
		return new java.util.ArrayList<Toko>(peta.values());
	}

	/**
	 * Aksi {@code daftar_toko_saya} -- dipanggil klien (Electron/Android) sesaat setelah login utk
	 * memutuskan apakah perlu menampilkan kombo pilih-toko (data.length > 1) atau cukup label toko
	 * tunggal (data.length <= 1, sama spt perilaku lama). Lihat JavaDoc
	 * {@link #daftarTokoBolehDiakses}.
	 */
	public static void daftarTokoSaya(Tbmuser tbmuser, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.util.List<Toko> daftar = daftarTokoBolehDiakses(session, tbmuser);
			JSONArray arr = new JSONArray();
			for (Toko t : daftar) {
				JSONObject j = new JSONObject();
				j.put("id", t.getId());
				j.put("nama", t.getNama());
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("tokoAktifId", tbmuser != null && tbmuser.getTokoAktifMultiToko() != null
					? tbmuser.getTokoAktifMultiToko() : JSONObject.NULL);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Aksi {@code pilih_toko_aktif} -- dipakai POS Desktop/Android saat akun memiliki hak multi-toko
	 * dari {@code Tbmrole.tokoAksesJson}. Pilihan disimpan di {@code Tbmuser.tokoAktifMultiToko} supaya
	 * semua aksi berikutnya (katalog, ringkasan, pesanan, laporan, dst.) memakai toko yang sama tanpa
	 * perlu mengandalkan {@code tbmuser.getPedagang()}.
	 */
	public static void pilihTokoAktif(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = ambilLongToko(request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko wajib dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		org.hibernate.Transaction tx = null;
		try {
			java.util.List<Toko> daftar = daftarTokoBolehDiakses(session, tbmuser);
			Toko tokoDipilih = null;
			for (Toko t : daftar) {
				if (t.getId() != null && t.getId().equals(tokoId)) {
					tokoDipilih = t;
					break;
				}
			}
			if (tokoDipilih == null) {
				hasil.put("status", "91");
				hasil.put("description", "Toko yang dipilih tidak termasuk hak akses pengguna ini.");
				return;
			}
			tx = session.beginTransaction();
			Tbmuser userDb = tbmuser == null || tbmuser.getUserId() == null ? null
					: (Tbmuser) session.get(Tbmuser.class, tbmuser.getUserId());
			if (userDb == null) {
				hasil.put("status", "91");
				hasil.put("description", "Pengguna tidak ditemukan.");
				return;
			}
			userDb.setTokoAktifMultiToko(tokoDipilih.getId());
			session.update(userDb);
			tx.commit();
			tbmuser.setTokoAktifMultiToko(tokoDipilih.getId());
			hasil.put("status", "00");
			hasil.put("tokoId", tokoDipilih.getId());
			hasil.put("tokoNama", tokoDipilih.getNama());
			JSONObject data = new JSONObject();
			data.put("id", tokoDipilih.getId());
			data.put("nama", tokoDipilih.getNama());
			hasil.put("data", data);
		} catch (Exception e) {
			if (tx != null) {
				try { tx.rollback(); } catch (Exception e2) { /* abaikan rollback gagal */ }
			}
			throw e;
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Aksi {@code ebisnis_role_list} -- daftar Grup Pengguna (Tbmrole) aktif, utk layar admin baru
	 * "Hak Akses" di POS Desktop/Android (gap-closure: {@code EbisnisMenuKatalog}/{@code
	 * Tbmrole.ebisnisMenu} sudah lama jadi sumber kebenaran per-menu, tapi editornya SEBELUM INI hanya
	 * ada di layar ZK {@code TbmroleAction}, tak terjangkau dari klien mobile/desktop Flutter).</h3>
	 *
	 * <p>Sengaja digerbang ADMIN GLOBAL SAJA ({@code tbmuser.getPedagang() == null}, BUKAN sekadar
	 * supervisor toko) -- {@code Tbmrole} TIDAK terikat satu toko (lihat JavaDoc {@link
	 * #daftarTokoBolehDiakses}: scoping-nya lewat Yayasan/Sekolah/dst, bukan Toko), jadi mengizinkan
	 * supervisor SATU toko mengubah definisi grup yang mungkin dipakai toko lain berisiko privilege
	 * escalation lintas-toko. Sama pola gerbang dgn method admin-only lain di file ini (mis. {@link
	 * #produkDuplikatHapus}).</p>
	 */
	public static void ebisnisRoleList(Tbmuser tbmuser, JSONObject hasil) throws Exception {
		if (tbmuser != null && tbmuser.getPedagang() != null) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin sistem yang dapat mengelola Grup Pengguna & Hak Akses.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			@SuppressWarnings("unchecked")
			java.util.List<ais.database.model.Tbmrole> daftar = session.createCriteria(ais.database.model.Tbmrole.class)
					.add(org.hibernate.criterion.Restrictions.or(
							org.hibernate.criterion.Restrictions.isNull("aktif"),
							org.hibernate.criterion.Restrictions.eq("aktif", true)))
					.addOrder(org.hibernate.criterion.Order.asc("roleName"))
					.list();
			JSONArray arr = new JSONArray();
			for (ais.database.model.Tbmrole r : daftar) {
				JSONObject j = new JSONObject();
				j.put("roleId", r.getRoleId());
				j.put("roleName", r.getRoleName() == null ? "" : r.getRoleName());
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Aksi {@code ebisnis_role_menu_ambil} -- baca status checkbox per-menu SATU Grup Pengguna, utk
	 * mengisi form "Hak Akses" (13 menu inti Kasir/Desktop/Android -- {@code
	 * EbisnisMenuKatalog.MODUL_POS} -- BUKAN 12 menu JSP e-Kantin yang tak punya padanan mobile).
	 * Gerbang admin SAMA dgn {@link #ebisnisRoleList}.
	 */
	public static void ebisnisRoleMenuAmbil(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (tbmuser != null && tbmuser.getPedagang() != null) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin sistem yang dapat mengelola Grup Pengguna & Hak Akses.");
			return;
		}
		String roleId = request.isNull("role_id") ? null : request.optString("role_id", null);
		if (roleId == null || roleId.trim().isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Grup Pengguna wajib dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.Tbmrole role = (ais.database.model.Tbmrole) session.get(ais.database.model.Tbmrole.class, roleId);
			if (role == null) {
				hasil.put("status", "91");
				hasil.put("description", "Grup Pengguna tidak ditemukan.");
				return;
			}
			JSONObject terurai = ais.common.EbisnisMenuKatalog.urai(role.getEbisnisMenu());
			JSONObject menuTersimpan = terurai.getJSONObject("menu");
			JSONArray daftarMenu = new JSONArray();
			for (ais.common.EbisnisMenuKatalog.Entri e : ais.common.EbisnisMenuKatalog.DAFTAR) {
				if (!ais.common.EbisnisMenuKatalog.MODUL_POS.equals(e.modul)) {
					continue;
				}
				JSONObject j = new JSONObject();
				j.put("kunci", e.kunci);
				j.put("label", e.label);
				j.put("boleh", menuTersimpan.optBoolean(e.kunci, true));
				daftarMenu.put(j);
			}
			hasil.put("status", "00");
			hasil.put("roleId", role.getRoleId());
			hasil.put("roleName", role.getRoleName() == null ? "" : role.getRoleName());
			hasil.put("supervisor", terurai.optBoolean("supervisor", false));
			hasil.put("menu", daftarMenu);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Aksi {@code ebisnis_role_menu_simpan} -- simpan checkbox per-menu SATU Grup Pengguna. HANYA
	 * mengubah sub-objek {@code menu} (13 kunci {@code EbisnisMenuKatalog.MODUL_POS}) -- {@code
	 * supervisor}/{@code berandaKantin}/{@code landingKantin}/{@code crud} yang SUDAH tersimpan (mis.
	 * lewat layar ZK {@code TbmroleAction}) dibaca ulang & disalin apa adanya, TIDAK ditimpa kosong,
	 * supaya form ringkas ini tidak diam-diam menghapus pengaturan CRUD granular yang sudah diatur
	 * admin sebelumnya lewat layar lain. Payload: {@code role_id}, {@code menu} (map kunci->boolean,
	 * hanya kunci yg dikirim yg diubah).
	 */
	public static void ebisnisRoleMenuSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (tbmuser != null && tbmuser.getPedagang() != null) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin sistem yang dapat mengelola Grup Pengguna & Hak Akses.");
			return;
		}
		String roleId = request.isNull("role_id") ? null : request.optString("role_id", null);
		JSONObject menuBaru = request.optJSONObject("menu");
		if (roleId == null || roleId.trim().isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Grup Pengguna wajib dipilih.");
			return;
		}
		if (menuBaru == null) {
			hasil.put("status", "91");
			hasil.put("description", "Data menu wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		org.hibernate.Transaction tx = null;
		try {
			ais.database.model.Tbmrole role = (ais.database.model.Tbmrole) session.get(ais.database.model.Tbmrole.class, roleId);
			if (role == null) {
				hasil.put("status", "91");
				hasil.put("description", "Grup Pengguna tidak ditemukan.");
				return;
			}
			JSONObject terurai = ais.common.EbisnisMenuKatalog.urai(role.getEbisnisMenu());
			JSONObject menuSaatIni = terurai.getJSONObject("menu");
			for (ais.common.EbisnisMenuKatalog.Entri e : ais.common.EbisnisMenuKatalog.DAFTAR) {
				if (!ais.common.EbisnisMenuKatalog.MODUL_POS.equals(e.modul)) {
					continue;
				}
				if (menuBaru.has(e.kunci)) {
					menuSaatIni.put(e.kunci, menuBaru.optBoolean(e.kunci, true));
				}
			}
			tx = session.beginTransaction();
			role.setEbisnisMenu(terurai.toString());
			session.update(role);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("description", "Hak akses berhasil disimpan.");
		} catch (Exception e) {
			if (tx != null) {
				try { tx.rollback(); } catch (Exception e2) { /* abaikan rollback gagal */ }
			}
			throw e;
		} finally {
			tutupSessionPolaB(session);
		}
	}

	private static Long ambilLongToko(JSONObject request) {
		if (request == null) return null;
		String[] kunci = new String[] { "tokoId", "id_toko", "idToko", "toko_id" };
		for (int i = 0; i < kunci.length; i++) {
			try {
				if (!request.isNull(kunci[i])) {
					String v = (request.get(kunci[i]) + "").trim();
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
	 * <h3>Aplikasi Android "Stok Opname" -- daftar sesi opname toko ini.</h3>
	 *
	 * <p>Sesi ({@link SesiStokOpname}) murni "kepala kegiatan" (jadwal/status/petugas) untuk
	 * kebutuhan Berita Acara -- baris hasil hitung tetap di {@code stok_opname} scope toko+tanggal
	 * (lihat JavaDoc {@link SesiStokOpname}), JADI aplikasi Android BOLEH juga langsung memindai tanpa
	 * pilih sesi (kirim {@code so_simpan} tanpa sesi) persis seperti alur JSP/ZK "SO by Scan" yang
	 * sudah ada -- sesi hanyalah pengelompokan opsional untuk opname besar terjadwal gaya
	 * supermarket/minimarket (mis. "Opname Akhir Bulan Juli").</p>
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin global, diabaikan utk pedagang/kasir),
	 *                {@code status} (opsional, filter {@code RENCANA}/{@code BERJALAN}/{@code SELESAI}).
	 * @param hasil   diisi {@code status="00"}, {@code data} (array {@code {id, kode, status,
	 *                tanggalRencana, tanggalMulai, kategori, petugas, keterangan}}).
	 */
	public static void soSesiList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		String status = request.optString("status", "").trim();

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria crit = session.createCriteria(SesiStokOpname.class)
					.add(Restrictions.eq("toko.id", tokoId))
					.addOrder(org.hibernate.criterion.Order.desc("tanggalRencana"))
					.setMaxResults(100);
			if (!status.isEmpty()) {
				crit.add(Restrictions.eq("status", status));
			}
			@SuppressWarnings("unchecked")
			java.util.List<SesiStokOpname> daftar = crit.list();

			SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			JSONArray arr = new JSONArray();
			for (SesiStokOpname s : daftar) {
				JSONObject j = new JSONObject();
				j.put("id", s.getId());
				j.put("kode", s.getKode() == null ? "" : s.getKode());
				j.put("status", s.getStatus());
				j.put("tanggalRencana", s.getTanggalRencana() == null ? JSONObject.NULL : fmt.format(s.getTanggalRencana()));
				j.put("tanggalMulai", s.getTanggalMulai() == null ? JSONObject.NULL : fmt.format(s.getTanggalMulai()));
				j.put("kategori", s.getKategori() == null ? "" : s.getKategori());
				j.put("petugas", s.getPetugas() == null ? "" : s.getPetugas());
				j.put("keterangan", s.getKeterangan() == null ? "" : s.getKeterangan());
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Aplikasi Android "Stok Opname" -- mulai sesi baru (atau lanjutkan sesi BERJALAN hari ini).</h3>
	 *
	 * <p>Idempoten secara sengaja: bila toko ini SUDAH punya sesi berstatus {@code BERJALAN} yang
	 * dibuat hari ini, sesi itu dikembalikan apa adanya (bukan bikin duplikat) -- mengantisipasi
	 * petugas menutup-buka ulang aplikasi di tengah opname tanpa membuat sesi ganda per kejadian.</p>
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin), {@code kategori} (opsional, mis.
	 *                "Semua Rak"/"Minuman"), {@code petugas} (opsional, default userid pemanggil).
	 * @param hasil   diisi {@code status="00"}, {@code id} (id sesi, baru atau yang dilanjutkan),
	 *                {@code dilanjutkan} (boolean -- true bila memakai sesi BERJALAN yang sudah ada).
	 */
	public static void soSesiMulai(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			@SuppressWarnings("unchecked")
			java.util.List<SesiStokOpname> berjalan = session.createCriteria(SesiStokOpname.class)
					.add(Restrictions.eq("toko.id", tokoId))
					.add(Restrictions.eq("status", SesiStokOpname.STATUS_BERJALAN))
					.add(Restrictions.sqlRestriction("cast(tanggalmulai as date) = current_date"))
					.setMaxResults(1)
					.list();
			if (!berjalan.isEmpty()) {
				hasil.put("status", "00");
				hasil.put("id", berjalan.get(0).getId());
				hasil.put("dilanjutkan", true);
				return;
			}

			Toko toko = (Toko) session.get(Toko.class, tokoId);
			if (toko == null) {
				hasil.put("status", "91");
				hasil.put("description", "Toko tidak ditemukan.");
				return;
			}
			SesiStokOpname sesi = new SesiStokOpname();
			sesi.setToko(toko);
			sesi.setKode("SO-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()));
			sesi.setTanggalRencana(new Date());
			sesi.setTanggalMulai(new Date());
			sesi.setKategori(request.optString("kategori", ""));
			sesi.setPetugas(request.optString("petugas", tbmuser == null ? "" : tbmuser.getUserId()));
			sesi.setStatus(SesiStokOpname.STATUS_BERJALAN);
			sesi.setOleh(tbmuser == null ? "android-stok-opname" : tbmuser.getUserId());

			session.beginTransaction();
			session.save(sesi);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", sesi.getId());
			hasil.put("dilanjutkan", false);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Aplikasi Android "Stok Opname" -- tandai sesi SELESAI.</h3>
	 *
	 * @param request payload: {@code id} (wajib, id {@link SesiStokOpname}), {@code keterangan}
	 *                (opsional, catatan penutup/berita acara ringkas).
	 */
	public static void soSesiSelesai(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "Sesi tidak ditemukan.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			SesiStokOpname sesi = (SesiStokOpname) session.get(SesiStokOpname.class, id);
			if (sesi == null) {
				hasil.put("status", "91");
				hasil.put("description", "Sesi tidak ditemukan.");
				return;
			}
			sesi.setStatus(SesiStokOpname.STATUS_SELESAI);
			sesi.setTanggalSelesai(new Date());
			if (request.has("keterangan")) {
				sesi.setKeterangan(request.optString("keterangan", ""));
			}
			session.beginTransaction();
			session.saveOrUpdate(sesi);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Aplikasi Android "Stok Opname" -- pindai barcode, kembalikan info produk + stok sistem.</h3>
	 *
	 * <p>Murni proksi JSON tipis di atas {@link StokOpnameScanUtil#cariProdukByBarcode}/
	 * {@link StokOpnameScanUtil#hitungStokSistem} yang SUDAH dipakai fitur "SO by Scan (HP/PDT)"
	 * existing (JSP &amp; ZK) -- TIDAK ada logika pencocokan baru, supaya hasil pindai di aplikasi
	 * Android selalu konsisten dgn dua tampilan lain yang sudah ada &amp; teruji.</p>
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin), {@code barcode} (wajib, dicocokkan
	 *                ke {@code Produk.kode}, tidak peka huruf besar/kecil).
	 * @param hasil   diisi {@code status="00"}, {@code produkId}, {@code nama}, {@code kode},
	 *                {@code stokSistem}, {@code stokMinimum} -- atau {@code status="91"} bila barcode
	 *                tidak dikenal di toko ini (mis. barcode dari toko lain -- {@code Produk.kode}
	 *                sengaja TIDAK unik global, lihat JavaDoc {@code Toko}/{@code Produk}).
	 */
	public static void soProdukScan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		String barcode = request.optString("barcode", "").trim();
		if (barcode.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Barcode kosong.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Produk produk = ais.action.master.inventory.StokOpnameScanUtil.cariProdukByBarcode(session, tokoId, barcode);
			if (produk == null) {
				hasil.put("status", "91");
				hasil.put("description", "Barcode \"" + barcode + "\" tidak dikenal di toko ini.");
				return;
			}
			double stokSistem = ais.action.master.inventory.StokOpnameScanUtil.hitungStokSistem(session, produk.getId());
			hasil.put("status", "00");
			hasil.put("produkId", produk.getId());
			hasil.put("nama", produk.getNama() == null ? "" : produk.getNama());
			hasil.put("kode", produk.getKode() == null ? "" : produk.getKode());
			hasil.put("stokSistem", stokSistem);
			hasil.put("stokMinimum", produk.getStokMinimum() == null ? 0d : produk.getStokMinimum());
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Aplikasi Android "Stok Opname" -- simpan satu hasil hitung fisik.</h3>
	 *
	 * <p>Proksi tipis ke {@link StokOpnameScanUtil#simpanOpname} (sama persis yang dipakai JSP/ZK) --
	 * stok produk otomatis di-recompute begitu baris ini tersimpan (lihat JavaDoc method itu), JADI
	 * efeknya LANGSUNG terlihat di Kasir (web/ZK/Electron/Android) tanpa langkah approval terpisah,
	 * konsisten dgn perilaku fitur "SO by Scan" yang sudah ada (TIDAK ada gerbang persetujuan --
	 * lihat catatan riset: proyek ini punya pola approval generik {@code RevisiHelper} tapi SENGAJA
	 * tidak dipasang di sini supaya perilaku Android identik dgn JSP/ZK existing, bukan perilaku baru
	 * yang mengejutkan pengguna lama).</p>
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin), {@code produk_id} (wajib, dari hasil
	 *                {@link #soProdukScan}), {@code stok_fisik} (wajib), {@code keterangan} (opsional,
	 *                mis. "Barang Basi"/"Hilang").
	 * @param hasil   diisi {@code status="00"}, {@code id} (id baris {@link StokOpname} tersimpan),
	 *                {@code selisih}.
	 */
	public static void soSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilSo = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalSo = pemanggilSo == null;
		boolean supervisorSo = pemanggilSo != null && Boolean.TRUE.equals(pemanggilSo.getSupervisor());
		if (!adminGlobalSo && !supervisorSo) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mencatat hasil Stok Opname.");
			return;
		}
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		Long produkId = request.isNull("produk_id") ? null : Long.valueOf((request.get("produk_id") + "").trim());
		if (produkId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Produk tidak ditemukan.");
			return;
		}
		if (!request.has("stok_fisik") || request.isNull("stok_fisik")) {
			hasil.put("status", "91");
			hasil.put("description", "Stok fisik wajib diisi.");
			return;
		}
		double stokFisik = request.getDouble("stok_fisik");
		String keterangan = request.optString("keterangan", "");
		String oleh = tbmuser == null ? "android-stok-opname" : tbmuser.getUserId();

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			StokOpname so = ais.action.master.inventory.StokOpnameScanUtil.simpanOpname(session, tokoId, produkId,
					stokFisik, keterangan, oleh);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", so.getId());
			hasil.put("selisih", so.getSelisih());
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:soSimpan-rollback");
			}
			hasil.put("status", "91");
			hasil.put("description", Common.tampilErrorJikaAdmin(e));
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Aplikasi Android "Stok Opname" -- ringkasan hasil opname hari ini.</h3>
	 *
	 * <p>Proksi tipis ke {@link StokOpnameScanUtil#ringkasanHariIni} (sudah dipakai panel dasbor
	 * JSP/ZK) -- ditampilkan di layar utama app sebagai konfirmasi progres ("42 produk sudah
	 * diopname, selisih bersih -3 unit").</p>
	 */
	public static void soRingkasan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.action.master.inventory.StokOpnameScanUtil.RingkasanOpname r =
					ais.action.master.inventory.StokOpnameScanUtil.ringkasanHariIni(session, tokoId);
			hasil.put("status", "00");
			hasil.put("jumlahCatatan", r.jumlahCatatan);
			hasil.put("jumlahProduk", r.jumlahProduk);
			hasil.put("totalLebih", r.totalLebih);
			hasil.put("totalKurang", r.totalKurang);
			hasil.put("selisihBersih", r.selisihBersih);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Aplikasi Android "Stok Opname" -- daftar catatan HARI INI (rincian, bukan cuma ringkasan).</h3>
	 *
	 * <p>Proksi tipis ke {@link StokOpnameScanUtil#daftarHariIni} -- gap-closure: SEBELUMNYA layar
	 * Desktop/Android hanya menampilkan "riwayat" dari memori sesi layar itu sendiri (baris yang baru
	 * saja discan, kosong lagi begitu layar dimuat ulang), padahal kartu ringkasan (aksi
	 * {@code so_ringkasan}) sudah benar membaca dari server -- pengguna bingung kenapa angkanya ada
	 * tapi daftarnya kosong. Aksi ini melengkapi dgn RINCIAN baris per baris, langsung dari database.</p>
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin, diabaikan utk pedagang/kasir),
	 *                {@code limit} (opsional, default 50, maks 200).
	 * @param hasil   diisi {@code status="00"}, {@code data} (array
	 *                {@code {id, waktu, kode, nama, stokSistem, stokFisik, selisih, keterangan, oleh}}).
	 */
	public static void soRiwayat(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		int limit = request.optInt("limit", 50);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
			java.util.List<ais.action.master.inventory.StokOpnameScanUtil.CatatanOpname> daftar =
					ais.action.master.inventory.StokOpnameScanUtil.daftarHariIni(session, tokoId, limit);
			JSONArray data = new JSONArray();
			for (ais.action.master.inventory.StokOpnameScanUtil.CatatanOpname k : daftar) {
				JSONObject o = new JSONObject();
				o.put("id", k.id);
				o.put("waktu", k.waktuOpname == null ? "" : fmt.format(k.waktuOpname));
				o.put("kode", k.kodeProduk);
				o.put("nama", k.namaProduk);
				o.put("stokSistem", k.stokSistem);
				o.put("stokFisik", k.stokFisik);
				o.put("selisih", k.selisih);
				o.put("keterangan", k.keterangan);
				o.put("oleh", k.oleh);
				data.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", data);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Dashboard "Mutasi Barang" (gap-closure Desktop/Android) -- padanan panel "Kartu Mutasi
	 * Stok" JSP {@code stok/mutasi_stok.jsp}.</h3>
	 *
	 * <p>JSP asli membangun SQL mentah di klien lalu mengirimnya ke aksi generik
	 * {@code /Data action:"sql"} -- SENGAJA TIDAK ditiru di sini (jalur token PosApi ini tidak boleh
	 * menerima SQL bebas dari klien, lihat JavaDoc kelas). Query yang SAMA dibangun ulang di server
	 * sebagai {@code PreparedStatement} berparameter, hasilnya sama persis (KPI + 2 chart) tapi aman.
	 * Scope SELALU satu toko (toko login kasir/pedagang, atau {@code toko_id} eksplisit utk admin) --
	 * TIDAK ada mode "Semua Toko" spt versi admin JSP, konsisten dgn dasbor Ringkasan POS lain yang
	 * sudah lebih dulu ada (lihat JavaDoc {@code PosApi.prosesRingkasan}).</p>
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin, diabaikan utk pedagang/kasir),
	 *                {@code periode} (opsional, salah satu {@code today/week/month/semester/year/3years},
	 *                default {@code month} -- sama persis daftar pilihan dropdown JSP).
	 * @param hasil   diisi {@code status="00"}, {@code barangMasuk}, {@code barangKeluar},
	 *                {@code totalStok}, {@code stokKritis} (ambang &lt;10, sama persis JSP), {@code trend}
	 *                (array {@code {tanggal,masuk,keluar}}, dikelompokkan per hari atau per bulan
	 *                tergantung {@code periode}), {@code top5Keluar} (array {@code {nama,qty}}).
	 */
	public static void stokDashboard(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		String periode = request.optString("periode", "month");
		boolean grupBulanan = "year".equals(periode) || "semester".equals(periode) || "3years".equals(periode);
		String kondisiMasuk = stokDashboardKondisiWaktu("pg.waktupengadaan", periode);
		String kondisiKeluar = stokDashboardKondisiWaktu("a.waktu", periode);

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();

			double barangMasuk = 0;
			java.sql.PreparedStatement psMasuk = conn.prepareStatement(
					"SELECT COALESCE(SUM(pg.qty),0) FROM koperasi.pengadaan_produk pg WHERE pg.toko = ? AND " + kondisiMasuk);
			psMasuk.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsMasuk = psMasuk.executeQuery();
			if (rsMasuk.next()) barangMasuk = rsMasuk.getDouble(1);
			rsMasuk.close(); psMasuk.close();

			double barangKeluar = 0;
			java.sql.PreparedStatement psKeluar = conn.prepareStatement(
					"SELECT COALESCE(SUM(a.qty),0) FROM koperasi.pembelian a WHERE a.toko = ? AND " + kondisiKeluar);
			psKeluar.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsKeluar = psKeluar.executeQuery();
			if (rsKeluar.next()) barangKeluar = rsKeluar.getDouble(1);
			rsKeluar.close(); psKeluar.close();

			double totalStok = 0;
			long stokKritis = 0;
			java.sql.PreparedStatement psStok = conn.prepareStatement(
					"SELECT COALESCE(SUM(p.stok),0), COUNT(CASE WHEN p.stok < 10 THEN 1 END) FROM koperasi.produk p WHERE p.toko = ? AND p.aktif = true");
			psStok.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsStok = psStok.executeQuery();
			if (rsStok.next()) {
				totalStok = rsStok.getDouble(1);
				stokKritis = rsStok.getLong(2);
			}
			rsStok.close(); psStok.close();

			String fmtLabel = grupBulanan ? "YYYY-MM" : "YYYY-MM-DD";
			java.util.Map<String, double[]> trendMap = new java.util.LinkedHashMap<String, double[]>();

			java.sql.PreparedStatement psTrendMasuk = conn.prepareStatement(
					"SELECT TO_CHAR(pg.waktupengadaan, ?) lbl, COALESCE(SUM(pg.qty),0) FROM koperasi.pengadaan_produk pg "
							+ "WHERE pg.toko = ? AND " + kondisiMasuk + " GROUP BY lbl");
			psTrendMasuk.setString(1, fmtLabel);
			psTrendMasuk.setLong(2, tokoId.longValue());
			java.sql.ResultSet rsTrendMasuk = psTrendMasuk.executeQuery();
			while (rsTrendMasuk.next()) {
				String lbl = rsTrendMasuk.getString(1);
				double[] pair = trendMap.get(lbl);
				if (pair == null) { pair = new double[2]; trendMap.put(lbl, pair); }
				pair[0] = rsTrendMasuk.getDouble(2);
			}
			rsTrendMasuk.close(); psTrendMasuk.close();

			java.sql.PreparedStatement psTrendKeluar = conn.prepareStatement(
					"SELECT TO_CHAR(a.waktu, ?) lbl, COALESCE(SUM(a.qty),0) FROM koperasi.pembelian a "
							+ "WHERE a.toko = ? AND " + kondisiKeluar + " GROUP BY lbl");
			psTrendKeluar.setString(1, fmtLabel);
			psTrendKeluar.setLong(2, tokoId.longValue());
			java.sql.ResultSet rsTrendKeluar = psTrendKeluar.executeQuery();
			while (rsTrendKeluar.next()) {
				String lbl = rsTrendKeluar.getString(1);
				double[] pair = trendMap.get(lbl);
				if (pair == null) { pair = new double[2]; trendMap.put(lbl, pair); }
				pair[1] = rsTrendKeluar.getDouble(2);
			}
			rsTrendKeluar.close(); psTrendKeluar.close();

			java.util.List<String> labelUrut = new java.util.ArrayList<String>(trendMap.keySet());
			java.util.Collections.sort(labelUrut);
			JSONArray trend = new JSONArray();
			for (String lbl : labelUrut) {
				double[] pair = trendMap.get(lbl);
				JSONObject t = new JSONObject();
				t.put("tanggal", lbl);
				t.put("masuk", pair[0]);
				t.put("keluar", pair[1]);
				trend.put(t);
			}

			JSONArray top5 = new JSONArray();
			java.sql.PreparedStatement psTop5 = conn.prepareStatement(
					"SELECT COALESCE(p.nama,'-'), COALESCE(SUM(a.qty),0) q FROM koperasi.pembelian a "
							+ "LEFT JOIN koperasi.produk p ON a.produk = p.id WHERE a.toko = ? AND " + kondisiKeluar
							+ " GROUP BY p.nama ORDER BY q DESC LIMIT 5");
			psTop5.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsTop5 = psTop5.executeQuery();
			while (rsTop5.next()) {
				JSONObject o = new JSONObject();
				o.put("nama", rsTop5.getString(1));
				o.put("qty", rsTop5.getDouble(2));
				top5.put(o);
			}
			rsTop5.close(); psTop5.close();

			hasil.put("status", "00");
			hasil.put("barangMasuk", barangMasuk);
			hasil.put("barangKeluar", barangKeluar);
			hasil.put("totalStok", totalStok);
			hasil.put("stokKritis", stokKritis);
			hasil.put("trend", trend);
			hasil.put("top5Keluar", top5);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Dasbor Statistik Produk" (JSP/ZK/Desktop/Android) -- kartu ringkasan katalog barang satu
	 * toko: total produk, aktif/nonaktif, stok habis/rendah, total nilai stok (stok &times; harga
	 * beli), plus 3 rincian (breakdown) top-8 utk chart batang horizontal: per kategori, per
	 * pemasok/vendor, dan per rentang harga jual. Scope SELALU satu toko, pola sama {@link
	 * #stokDashboard}.
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin, diabaikan utk pedagang/kasir).
	 * @param hasil   diisi {@code status="00"}, KPI ({@code totalProduk}, {@code totalAktif},
	 *                {@code totalNonaktif}, {@code stokHabis}, {@code stokRendah} (0&lt;stok&le;5),
	 *                {@code totalNilaiStok}), dan 3 array breakdown ({@code byKategori},
	 *                {@code byPemasok}, {@code byHarga}) masing-masing {@code {label,jumlah}}.
	 */
	public static void produkStatistik(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();

			long totalProduk = 0, totalAktif = 0, totalNonaktif = 0, stokHabis = 0, stokRendah = 0;
			double totalNilaiStok = 0;
			java.sql.PreparedStatement psKpi = conn.prepareStatement(
					"SELECT COUNT(*), COUNT(CASE WHEN COALESCE(p.aktif,true)=true THEN 1 END), "
							+ "COUNT(CASE WHEN COALESCE(p.aktif,true)=false THEN 1 END), "
							+ "COUNT(CASE WHEN COALESCE(p.stok,0)<=0 THEN 1 END), "
							+ "COUNT(CASE WHEN COALESCE(p.stok,0)>0 AND COALESCE(p.stok,0)<=5 THEN 1 END), "
							+ "COALESCE(SUM(COALESCE(p.stok,0)*COALESCE(p.hargabeli,0)),0) "
							+ "FROM koperasi.produk p WHERE p.toko = ?");
			psKpi.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsKpi = psKpi.executeQuery();
			if (rsKpi.next()) {
				totalProduk = rsKpi.getLong(1);
				totalAktif = rsKpi.getLong(2);
				totalNonaktif = rsKpi.getLong(3);
				stokHabis = rsKpi.getLong(4);
				stokRendah = rsKpi.getLong(5);
				totalNilaiStok = rsKpi.getDouble(6);
			}
			rsKpi.close(); psKpi.close();

			JSONArray byKategori = new JSONArray();
			java.sql.PreparedStatement psKategori = conn.prepareStatement(
					"SELECT COALESCE(j.nama,'Tanpa Kategori') lbl, COUNT(*) cnt FROM koperasi.produk p "
							+ "LEFT JOIN koperasi.jenis_produk j ON p.jenis_produk = j.id WHERE p.toko = ? "
							+ "GROUP BY lbl ORDER BY cnt DESC LIMIT 8");
			psKategori.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsKategori = psKategori.executeQuery();
			while (rsKategori.next()) {
				JSONObject o = new JSONObject();
				o.put("label", rsKategori.getString(1));
				o.put("jumlah", rsKategori.getLong(2));
				byKategori.put(o);
			}
			rsKategori.close(); psKategori.close();

			JSONArray byPemasok = new JSONArray();
			java.sql.PreparedStatement psPemasok = conn.prepareStatement(
					"SELECT COALESCE(s.nama,'Tanpa Pemasok') lbl, COUNT(*) cnt FROM koperasi.produk p "
							+ "LEFT JOIN koperasi.pemasok_produk s ON p.pemasok = s.id WHERE p.toko = ? "
							+ "GROUP BY lbl ORDER BY cnt DESC LIMIT 8");
			psPemasok.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsPemasok = psPemasok.executeQuery();
			while (rsPemasok.next()) {
				JSONObject o = new JSONObject();
				o.put("label", rsPemasok.getString(1));
				o.put("jumlah", rsPemasok.getLong(2));
				byPemasok.put(o);
			}
			rsPemasok.close(); psPemasok.close();

			JSONArray byHarga = new JSONArray();
			java.sql.PreparedStatement psHarga = conn.prepareStatement(
					"SELECT CASE WHEN COALESCE(p.hargajual,0) < 5000 THEN '< Rp 5rb' "
							+ "WHEN COALESCE(p.hargajual,0) < 10000 THEN 'Rp 5rb - 10rb' "
							+ "WHEN COALESCE(p.hargajual,0) < 20000 THEN 'Rp 10rb - 20rb' "
							+ "WHEN COALESCE(p.hargajual,0) < 50000 THEN 'Rp 20rb - 50rb' "
							+ "ELSE '>= Rp 50rb' END lbl, COUNT(*) cnt "
							+ "FROM koperasi.produk p WHERE p.toko = ? GROUP BY lbl ORDER BY MIN(COALESCE(p.hargajual,0))");
			psHarga.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsHarga = psHarga.executeQuery();
			while (rsHarga.next()) {
				JSONObject o = new JSONObject();
				o.put("label", rsHarga.getString(1));
				o.put("jumlah", rsHarga.getLong(2));
				byHarga.put(o);
			}
			rsHarga.close(); psHarga.close();

			// Gap-closure "Per Jumlah Stok" (Toko Al-Bahjah) -- pola SAMA PERSIS dgn byHarga di atas,
			// batas bucket SELARAS dgn KPI stokHabis/stokRendah yg sudah ada (0 = Habis, 1-5 = Rendah)
			// supaya angkanya tidak pernah kontradiktif satu sama lain.
			JSONArray byStok = new JSONArray();
			java.sql.PreparedStatement psStok = conn.prepareStatement(
					"SELECT CASE WHEN COALESCE(p.stok,0) <= 0 THEN '0 (Habis)' "
							+ "WHEN COALESCE(p.stok,0) <= 5 THEN '1 - 5 (Rendah)' "
							+ "WHEN COALESCE(p.stok,0) <= 20 THEN '6 - 20' "
							+ "WHEN COALESCE(p.stok,0) <= 50 THEN '21 - 50' "
							+ "ELSE '> 50' END lbl, COUNT(*) cnt "
							+ "FROM koperasi.produk p WHERE p.toko = ? GROUP BY lbl ORDER BY MIN(COALESCE(p.stok,0))");
			psStok.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsStok = psStok.executeQuery();
			while (rsStok.next()) {
				JSONObject o = new JSONObject();
				o.put("label", rsStok.getString(1));
				o.put("jumlah", rsStok.getLong(2));
				byStok.put(o);
			}
			rsStok.close(); psStok.close();

			hasil.put("status", "00");
			hasil.put("totalProduk", totalProduk);
			hasil.put("totalAktif", totalAktif);
			hasil.put("totalNonaktif", totalNonaktif);
			hasil.put("stokHabis", stokHabis);
			hasil.put("stokRendah", stokRendah);
			hasil.put("totalNilaiStok", totalNilaiStok);
			hasil.put("byKategori", byKategori);
			hasil.put("byPemasok", byPemasok);
			hasil.put("byHarga", byHarga);
			hasil.put("byStok", byStok);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "klik kartu/bar statistik utk lihat daftar barangnya" (layar Katalog Barang, gap-closure
	 * Toko Al-Bahjah) -- membalas daftar produk MENTAH yang cocok dgn SATU KPI atau SATU baris
	 * breakdown dari {@link #produkStatistik}. SENGAJA query {@code koperasi.produk} LANGSUNG (bukan
	 * lewat aksi {@code katalog}/{@code PriceTagUtil.listProduk}) -- method itu SELALU menyaring hanya
	 * produk aktif ({@code isNull(aktif) OR aktif=true}), jadi TIDAK BISA dipakai utk kartu "Non-Aktif"
	 * (perlu menampilkan produk yg justru tidak aktif). Batas bucket harga/stok di sini WAJIB SELARAS
	 * PERSIS dgn {@link #produkStatistik} -- kalau salah satu diubah, ubah jg yg satunya, supaya angka
	 * KPI/bar yg diklik SELALU cocok dgn jumlah baris yg tampil di popup.
	 *
	 * @param request payload: {@code tipe} (wajib -- {@code total|aktif|nonaktif|stokHabis|stokRendah|
	 *                nilaiStok|kategori|pemasok|harga|stok}), {@code nilai} (label baris breakdown,
	 *                wajib HANYA utk tipe kategori/pemasok/harga/stok -- persis label dari
	 *                {@code produkStatistik}).
	 */
	public static void produkStatistikDetail(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		String tipe = request.optString("tipe", "");
		String nilai = request.optString("nilai", "");

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					"SELECT p.id, p.kode, p.barcode, p.nama, COALESCE(k.nama,'Tanpa Kategori'), COALESCE(pm.nama,'Tanpa Pemasok'), "
							+ "COALESCE(p.hargajual,0), COALESCE(p.stok,0), COALESCE(p.aktif,true) "
							+ "FROM koperasi.produk p LEFT JOIN koperasi.jenis_produk k ON p.jenis_produk = k.id "
							+ "LEFT JOIN koperasi.pemasok_produk pm ON p.pemasok = pm.id WHERE p.toko = ? ");
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			params.add(tokoId);

			if ("aktif".equals(tipe)) {
				sql.append(" AND COALESCE(p.aktif,true) = true ");
			} else if ("nonaktif".equals(tipe)) {
				sql.append(" AND COALESCE(p.aktif,true) = false ");
			} else if ("stokHabis".equals(tipe)) {
				sql.append(" AND COALESCE(p.stok,0) <= 0 ");
			} else if ("stokRendah".equals(tipe)) {
				sql.append(" AND COALESCE(p.stok,0) > 0 AND COALESCE(p.stok,0) <= 5 ");
			} else if ("kategori".equals(tipe)) {
				if ("Tanpa Kategori".equals(nilai)) {
					sql.append(" AND k.nama IS NULL ");
				} else {
					sql.append(" AND k.nama = ? ");
					params.add(nilai);
				}
			} else if ("pemasok".equals(tipe)) {
				if ("Tanpa Pemasok".equals(nilai)) {
					sql.append(" AND pm.nama IS NULL ");
				} else {
					sql.append(" AND pm.nama = ? ");
					params.add(nilai);
				}
			} else if ("harga".equals(tipe)) {
				if ("< Rp 5rb".equals(nilai)) sql.append(" AND COALESCE(p.hargajual,0) < 5000 ");
				else if ("Rp 5rb - 10rb".equals(nilai)) sql.append(" AND COALESCE(p.hargajual,0) >= 5000 AND COALESCE(p.hargajual,0) < 10000 ");
				else if ("Rp 10rb - 20rb".equals(nilai)) sql.append(" AND COALESCE(p.hargajual,0) >= 10000 AND COALESCE(p.hargajual,0) < 20000 ");
				else if ("Rp 20rb - 50rb".equals(nilai)) sql.append(" AND COALESCE(p.hargajual,0) >= 20000 AND COALESCE(p.hargajual,0) < 50000 ");
				else if (">= Rp 50rb".equals(nilai)) sql.append(" AND COALESCE(p.hargajual,0) >= 50000 ");
			} else if ("stok".equals(tipe)) {
				if ("0 (Habis)".equals(nilai)) sql.append(" AND COALESCE(p.stok,0) <= 0 ");
				else if ("1 - 5 (Rendah)".equals(nilai)) sql.append(" AND COALESCE(p.stok,0) > 0 AND COALESCE(p.stok,0) <= 5 ");
				else if ("6 - 20".equals(nilai)) sql.append(" AND COALESCE(p.stok,0) > 5 AND COALESCE(p.stok,0) <= 20 ");
				else if ("21 - 50".equals(nilai)) sql.append(" AND COALESCE(p.stok,0) > 20 AND COALESCE(p.stok,0) <= 50 ");
				else if ("> 50".equals(nilai)) sql.append(" AND COALESCE(p.stok,0) > 50 ");
			}
			// "total"/"nilaiStok"/tipe tak dikenal: tanpa filter tambahan (seluruh katalog toko).

			sql.append("nilaiStok".equals(tipe)
					? " ORDER BY (COALESCE(p.stok,0) * COALESCE(p.hargabeli,0)) DESC "
					: " ORDER BY p.nama ASC ");

			java.sql.PreparedStatement ps = conn.prepareStatement(sql.toString());
			for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject o = new JSONObject();
				o.put("id", rs.getLong(1));
				o.put("kode", rs.getString(2));
				o.put("barcode", rs.getString(3));
				o.put("nama", rs.getString(4));
				o.put("kategoriNama", rs.getString(5));
				o.put("pemasokNama", rs.getString(6));
				o.put("hargaJual", rs.getDouble(7));
				o.put("stok", rs.getDouble(8));
				o.put("aktif", rs.getBoolean(9));
				arr.put(o);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("total", arr.length());
			hasil.put("produk", arr);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Kunci pengelompokan duplikat + kondisi WHERE per mode -- dipakai bersama {@link #cariGrupDuplikat}
	 * (pratinjau) dan {@link #produkDuplikatHapus} (eksekusi, recompute ulang -- TIDAK percaya data
	 * grup dari klien). 5 mode: {@code kode}, {@code barcode}, {@code nama} (dibanding case-insensitive
	 * +trim), {@code kode_barcode} (kombinasi keduanya harus sama), {@code kode_barcode_nama}
	 * (kombinasi KETIGANYA harus sama -- gap-closure "satu lagi kondisi penghapusan", paling ketat
	 * dari semua mode, dipakai saat kode+barcode SAMA tapi admin ingin memastikan nama-nya jg identik
	 * sblm dianggap duplikat -- mis. kalau kode+barcode kebetulan bentrok tapi sebenarnya produk beda).
	 */
	private static String[] kunciDuplikat(String jenis, String alias) {
		String kunci, kondisi;
		if ("barcode".equals(jenis)) {
			kunci = alias + ".barcode";
			kondisi = " AND " + alias + ".barcode IS NOT NULL AND TRIM(" + alias + ".barcode) <> ''";
		} else if ("nama".equals(jenis)) {
			kunci = "LOWER(TRIM(" + alias + ".nama))";
			kondisi = " AND " + alias + ".nama IS NOT NULL AND TRIM(" + alias + ".nama) <> ''";
		} else if ("kode_barcode".equals(jenis)) {
			kunci = alias + ".kode || '||' || " + alias + ".barcode";
			kondisi = " AND " + alias + ".kode IS NOT NULL AND TRIM(" + alias + ".kode) <> '' AND "
					+ alias + ".barcode IS NOT NULL AND TRIM(" + alias + ".barcode) <> ''";
		} else if ("kode_barcode_nama".equals(jenis)) {
			kunci = alias + ".kode || '||' || " + alias + ".barcode || '||' || LOWER(TRIM(" + alias + ".nama))";
			kondisi = " AND " + alias + ".kode IS NOT NULL AND TRIM(" + alias + ".kode) <> '' AND "
					+ alias + ".barcode IS NOT NULL AND TRIM(" + alias + ".barcode) <> '' AND "
					+ alias + ".nama IS NOT NULL AND TRIM(" + alias + ".nama) <> ''";
		} else if ("kunci_unik".equals(jenis)) {
			// Versi SQL PERSIS sama dgn ais.common.ProdukKunciUnikUtil.hitung (Java) -- lihat JavaDoc
			// kelas itu. regexp_replace 'g' membuang SELURUH karakter selain huruf/angka/underscore,
			// menutup celah dua produk yg "sama" tapi beda tanda baca/spasi/huruf besar-kecil lolos
			// sbg baris terpisah -- inilah kunci yg (setelah kolom kunci_unik diisi backfill+dedup
			// ini) akhirnya dijadikan UNIQUE INDEX sungguhan di database.
			kunci = "LOWER(regexp_replace(COALESCE(" + alias + ".kode,'') || '_' || COALESCE(" + alias + ".barcode,'')"
					+ " || '_' || COALESCE(" + alias + ".nama,'') || '_' || COALESCE(" + alias + ".toko::text,''),"
					+ " '[^a-zA-Z0-9_]', '', 'g'))";
			kondisi = " AND " + alias + ".kode IS NOT NULL AND TRIM(" + alias + ".kode) <> '' AND "
					+ alias + ".nama IS NOT NULL AND TRIM(" + alias + ".nama) <> ''";
		} else {
			kunci = alias + ".kode";
			kondisi = " AND " + alias + ".kode IS NOT NULL AND TRIM(" + alias + ".kode) <> ''";
		}
		return new String[] { kunci, kondisi };
	}

	/**
	 * Cari grup produk duplikat pada SATU toko (tidak pernah lintas-toko, terlepas dari toggle
	 * "semua toko" browse-only di {@code prosesKatalog}) -- dipakai pratinjau ({@link
	 * #produkDuplikatCari}) MAUPUN sbg langkah PERTAMA eksekusi ({@link #produkDuplikatHapus}, yg
	 * SENGAJA recompute ulang lewat method ini, bukan menerima daftar id dari klien -- mencegah
	 * eksekusi berdasar data pratinjau yg sudah basi/stale kalau ada perubahan di antara pratinjau
	 * dan konfirmasi).
	 * @return array grup, tiap elemen {@code {kunci, items:[{id,kode,barcode,nama,hargaJual,stok,jumlahTransaksi}]}}.
	 */
	private static JSONArray cariGrupDuplikat(java.sql.Connection conn, long tokoId, String jenis) throws Exception {
		String[] kp = kunciDuplikat(jenis, "p");
		String[] kp2 = kunciDuplikat(jenis, "p2");
		String sql = "SELECT " + kp[0] + " AS kunci, p.id, p.kode, p.barcode, p.nama, "
				+ "COALESCE(p.hargajual,0) hargajual, COALESCE(p.stok,0) stok, "
				+ "(SELECT COUNT(*) FROM koperasi.pembelian trx WHERE trx.produk = p.id) jumlah_transaksi "
				+ "FROM koperasi.produk p "
				+ "WHERE p.toko = ?" + kp[1]
				+ " AND " + kp[0] + " IN ("
				+ "  SELECT " + kp2[0] + " FROM koperasi.produk p2 WHERE p2.toko = ?" + kp2[1]
				+ "  GROUP BY " + kp2[0] + " HAVING COUNT(*) > 1"
				+ ") ORDER BY " + kp[0] + ", p.id";
		java.sql.PreparedStatement ps = conn.prepareStatement(sql);
		ps.setLong(1, tokoId);
		ps.setLong(2, tokoId);
		java.sql.ResultSet rs = ps.executeQuery();
		java.util.LinkedHashMap<String, JSONArray> perKunci = new java.util.LinkedHashMap<String, JSONArray>();
		while (rs.next()) {
			String kunci = rs.getString("kunci");
			JSONObject item = new JSONObject();
			item.put("id", rs.getLong("id"));
			// FIX compile "cannot find symbol: str": helper str() tidak ada di kelas ini --
			// rs.getString(...) bisa null, JSONObject.put menolak null; ganti string kosong.
			String kolomKode = rs.getString("kode");
			String kolomBarcode = rs.getString("barcode");
			String kolomNama = rs.getString("nama");
			item.put("kode", kolomKode == null ? "" : kolomKode);
			item.put("barcode", kolomBarcode == null ? "" : kolomBarcode);
			item.put("nama", kolomNama == null ? "" : kolomNama);
			item.put("hargaJual", rs.getDouble("hargajual"));
			item.put("stok", rs.getDouble("stok"));
			item.put("jumlahTransaksi", rs.getLong("jumlah_transaksi"));
			JSONArray arr = perKunci.get(kunci);
			if (arr == null) { arr = new JSONArray(); perKunci.put(kunci, arr); }
			arr.put(item);
		}
		rs.close(); ps.close();
		JSONArray grup = new JSONArray();
		for (java.util.Map.Entry<String, JSONArray> e : perKunci.entrySet()) {
			JSONObject g = new JSONObject();
			g.put("kunci", e.getKey());
			g.put("items", e.getValue());
			grup.put(g);
		}
		return grup;
	}

	/**
	 * Fitur "Bersihkan Produk Duplikat" (pratinjau) -- gap-closure keluhan katalog toko punya baris
	 * produk kembar (kode/barcode/nama sama), lihat JavaDoc {@link #produkDuplikatHapus} soal aturan
	 * penghapusan lengkap. Method ini MURNI baca, tidak mengubah apa pun.
	 * @param request payload {@code jenis} ("kode"/"barcode"/"nama"/"kode_barcode", default "kode").
	 */
	public static void produkDuplikatCari(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		String jenis = request.optString("jenis", "kode");
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			JSONArray grup = cariGrupDuplikat(conn, tokoId.longValue(), jenis);
			long totalProdukTerlibat = 0;
			for (int i = 0; i < grup.length(); i++) totalProdukTerlibat += grup.getJSONObject(i).getJSONArray("items").length();
			hasil.put("status", "00");
			hasil.put("jenis", jenis);
			hasil.put("grup", grup);
			hasil.put("totalGrup", grup.length());
			hasil.put("totalProdukTerlibat", totalProdukTerlibat);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Gabungkan SEMUA referensi produk yg jadi KEY {@code peta} (loser) ke NILAI-nya (survivor) lalu
	 * hapus seluruh baris loser -- 14 tabel FK formal (Hibernate {@code ON DELETE NO ACTION} di
	 * semuanya, akan menolak DELETE selama masih direferensikan, jadi WAJIB direpoint dulu) + {@code
	 * public.foto_gambar_produk} (referensi informal TANPA constraint FK -- DB tidak akan
	 * memperingatkan, foto bisa jadi yatim diam-diam kalau dilewatkan) + rollup JSON self-reference
	 * {@code koperasi.produk.bahanbaku} (resep produk LAIN yang memakai salah satu loser sbg bahan
	 * baku -- BUKAN kolom FK biasa, perlu parse+tulis ulang JSON per baris). Dipanggil di DALAM
	 * transaksi milik pemanggil ({@link #produkDuplikatHapus}) -- method ini sendiri TIDAK commit/
	 * rollback.
	 *
	 * <p><b>Gap-closure PERFORMA (SATU kali panggil utk SELURUH operasi, bukan per-baris)</b>:
	 * versi SEBELUMNYA dipanggil ULANG per baris loser (bisa puluhan ribu kali pada katalog besar --
	 * observasi lapangan: 5524 grup/16572 baris duplikat) dgn 14 {@code executeUpdate} + 1 SELECT
	 * full-scan {@code LIKE} tanpa filter toko + 1 {@code executeUpdate} DELETE, MASING-MASING round-
	 * trip DB TERPISAH -- total bisa ratusan ribu round-trip, menahan transaksi/koneksi selama
	 * bermenit-menit sampai timeout klien (>2 menit) DAN menyedot kapasitas server sampai request
	 * KASIR LAIN yg sama sekali tak terkait (mis. aksi {@code konfigurasi} biasa) ikut timeout krn
	 * server/DB kehabisan kapasitas (root cause bersama 2 error timeout terpisah yg dilaporkan
	 * ErrorLog). Sekarang SEMUA pasangan loser->survivor (dari SELURUH grup) dikumpulkan dulu oleh
	 * pemanggil, lalu method ini dipanggil SATU KALI memakai JDBC batch ({@code addBatch}/{@code
	 * executeBatch}) -- jumlah ROUND-TRIP TETAP KONSTAN (~17 batch execute) TERLEPAS dari berapa ribu
	 * baris diproses. Query bahanbaku jg diperketat scope {@code toko = ?} (bukan lagi scan SELURUH
	 * database tanpa filter) krn resep logisnya hanya mereferensikan produk toko yg sama.</p>
	 *
	 * @param peta {@code idLama -> idSurvivor} utk SELURUH grup duplikat yg sedang diproses.
	 */
	/**
	 * Daftar tabel (schema.tabel, nama kolom FK ke {@code koperasi.produk}) yang WAJIB dicek/direpoint
	 * sebelum sebuah baris produk boleh dihapus -- SATU sumber kebenaran dipakai bersama oleh
	 * {@link #gabungkanDanHapusProdukBulk} (repoint ke survivor) dan
	 * {@link #produkHapusNonaktifTakTerpakai} (cek "benar-benar tak pernah dipakai" sebelum hapus
	 * permanen) supaya kedua tempat TIDAK bisa diam-diam berbeda daftar tabelnya di masa depan.
	 */
	private static final String[][] TABEL_REFERENSI_PRODUK = {
			{ "koperasi.pembelian", "produk" },
			{ "koperasi.draft_pembelian", "produk" },
			{ "koperasi.pengadaan_produk", "produk" },
			{ "koperasi.stok_opname", "produk" },
			{ "koperasi.aturan_diskon", "produk" },
			{ "koperasi.produk_komentar", "produk" },
			{ "koperasi.pengajuan_perubahan_harga_produk", "produk" },
			{ "koperasi.retur_barang", "produk" },
			{ "koperasi.produksi_kantin", "produk" },
			{ "koperasi.pemakaian_bahan_baku", "produk" },
			{ "koperasi.ambang_stok_gudang", "produk" },
			{ "koperasi.pengajuan_pembelian_gudang", "produk" },
			{ "asset.mutasi_lokasi", "produk" },
			{ "asset.pengiriman_gudang_detail", "produk" }
	};

	private static void gabungkanDanHapusProdukBulk(java.sql.Connection conn, long tokoId, java.util.Map<Long, Long> peta) throws Exception {
		if (peta.isEmpty()) return;

		for (String[] tk : TABEL_REFERENSI_PRODUK) {
			java.sql.PreparedStatement ps = conn.prepareStatement(
					"UPDATE " + tk[0] + " SET " + tk[1] + " = ? WHERE " + tk[1] + " = ?");
			for (java.util.Map.Entry<Long, Long> e : peta.entrySet()) {
				ps.setLong(1, e.getValue().longValue());
				ps.setLong(2, e.getKey().longValue());
				ps.addBatch();
			}
			ps.executeBatch();
			ps.close();
		}

		// FIX runtime: "relation public.foto_gambar_produk does not exist" -- tabel ini hidup di
		// DATABASE STREAMING TERPISAH (lihat mapping-nya HANYA di hibernate.streaming.cfg.xml, TIDAK
		// ADA di hibernate.cfg.xml utama), bukan sekadar schema lain di DB yang sama seperti tabel2 di
		// atas -- koneksi `conn` (dari session Hibernate UTAMA) tidak pernah bisa menjangkaunya sama
		// sekali. Harus lewat StreamingHibernateUtil, konsisten dgn seluruh kode lain yang menyentuh
		// tabel file/blob (lihat FileFotoLain.java). TIDAK bisa atomik bersama transaksi utama (koneksi
		// fisik berbeda) -- kegagalan di sini diTOLERANSI (foto memang sudah informal/tanpa FK
		// constraint, lihat JavaDoc kelas ini), jangan sampai menggagalkan seluruh pembersihan duplikat
		// hanya krn baris foto gagal dipindah.
		try {
			org.hibernate.Session streamSession = ais.database.hibernate.StreamingHibernateUtil.getInstance().currentSession();
			java.sql.PreparedStatement psFoto = streamSession.connection().prepareStatement(
					"UPDATE public.foto_gambar_produk SET produk = ? WHERE produk = ?");
			for (java.util.Map.Entry<Long, Long> e : peta.entrySet()) {
				psFoto.setLong(1, e.getValue().longValue());
				psFoto.setLong(2, e.getKey().longValue());
				psFoto.addBatch();
			}
			psFoto.executeBatch();
			psFoto.close();
		} catch (Exception eFoto) {
			ais.common.ErrorAuditUtil.record(eFoto, "produkDuplikatHapus-fotoGambarProduk bulk toko=" + tokoId + " total=" + peta.size());
		}

		// Rollup bahanbaku (resep) produk LAIN yg memakai salah satu loser sbg bahan -- SATU query
		// dibatasi ke toko ini saja (bukan lagi LIKE full-scan tanpa filter per baris loser).
		java.sql.PreparedStatement psCari = conn.prepareStatement(
				"SELECT id, bahanbaku FROM koperasi.produk WHERE toko = ? AND bahanbaku IS NOT NULL AND bahanbaku <> '[]'");
		psCari.setLong(1, tokoId);
		java.sql.ResultSet rsCari = psCari.executeQuery();
		java.util.List<Long> idPerluUpdate = new java.util.ArrayList<Long>();
		java.util.List<String> bahanBaruList = new java.util.ArrayList<String>();
		while (rsCari.next()) {
			long idProdukLain = rsCari.getLong(1);
			String mentah = rsCari.getString(2);
			try {
				JSONArray arr = new JSONArray(mentah);
				boolean berubah = false;
				for (int i = 0; i < arr.length(); i++) {
					JSONObject bahan = arr.optJSONObject(i);
					if (bahan == null) continue;
					long idBahan = bahan.optLong("produk", -1);
					Long survivorBahan = idBahan < 0 ? null : peta.get(Long.valueOf(idBahan));
					if (survivorBahan != null) {
						bahan.put("produk", survivorBahan.longValue());
						berubah = true;
					}
				}
				if (berubah) {
					idPerluUpdate.add(Long.valueOf(idProdukLain));
					bahanBaruList.add(arr.toString());
				}
			} catch (Exception eJson) {
				// JSON resep korup di baris ini -- lewati, jangan gagalkan seluruh pembersihan krn satu baris rusak.
				ais.common.ErrorAuditUtil.record(eJson, "auto-audit produkDuplikatHapus-bahanbaku id=" + idProdukLain);
			}
		}
		rsCari.close(); psCari.close();
		if (!idPerluUpdate.isEmpty()) {
			java.sql.PreparedStatement psUpdateBb = conn.prepareStatement("UPDATE koperasi.produk SET bahanbaku = ? WHERE id = ?");
			for (int i = 0; i < idPerluUpdate.size(); i++) {
				psUpdateBb.setString(1, bahanBaruList.get(i));
				psUpdateBb.setLong(2, idPerluUpdate.get(i).longValue());
				psUpdateBb.addBatch();
			}
			psUpdateBb.executeBatch();
			psUpdateBb.close();
		}

		java.sql.PreparedStatement psHapus = conn.prepareStatement("DELETE FROM koperasi.produk WHERE id = ?");
		for (Long idLama : peta.keySet()) {
			psHapus.setLong(1, idLama.longValue());
			psHapus.addBatch();
		}
		psHapus.executeBatch();
		psHapus.close();
	}

	/**
	 * Fitur "Bersihkan Produk Duplikat" (eksekusi) -- recompute grup duplikat SEGAR (lihat JavaDoc
	 * {@link #cariGrupDuplikat}), lalu utk TIAP grup tentukan produk yang SELAMAT (survivor) dgn
	 * aturan: produk dgn id PALING KECIL DI ANTARA yang SUDAH punya transaksi (baris {@code
	 * koperasi.pembelian}); bila TAK SATU PUN di grup itu punya transaksi, survivor = id paling kecil
	 * di seluruh grup. Aturan ini SATU aturan konsisten yg otomatis menaungi 3 skenario yg
	 * diminta pengguna: (a) sebagian punya transaksi sebagian tidak -- yg tanpa transaksi kalah lawan
	 * yg punya transaksi brp pun id-nya; (b) semua tanpa transaksi -- id terkecil menang, sisanya
	 * (id lebih besar) dihapus; (c) semua sudah punya transaksi -- id terkecil menang, transaksi
	 * grup lain digabung ({@link #gabungkanDanHapusProduk}) ke situ. SATU transaksi database utk
	 * SELURUH proses (semua grup) -- gagal di tengah jalan me-rollback semuanya, tidak ada
	 * pembersihan setengah jalan.
	 * @param request payload {@code jenis} (sama {@link #produkDuplikatCari}).
	 */
	public static void produkDuplikatHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		String jenis = request.optString("jenis", "kode");
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			JSONArray grup = cariGrupDuplikat(conn, tokoId.longValue(), jenis);

			int grupDiproses = 0, produkDihapus = 0, grupDigabungTransaksi = 0;
			JSONArray detail = new JSONArray();
			// Gap-closure PERFORMA: kumpulkan SELURUH pasangan loser->survivor dari SEMUA grup dulu
			// (murni di memori, TANPA sentuh DB), lalu eksekusi SATU KALI lewat {@link
			// #gabungkanDanHapusProdukBulk} (JDBC batch) SETELAH loop -- lihat JavaDoc method itu utk
			// alasan lengkap (versi lama memanggilnya PER BARIS, timeout+menyedot kapasitas server
			// pada katalog besar).
			java.util.Map<Long, Long> petaGabung = new java.util.LinkedHashMap<Long, Long>();
			session.beginTransaction();
			try {
				for (int i = 0; i < grup.length(); i++) {
					JSONArray items = grup.getJSONObject(i).getJSONArray("items");
					java.util.List<Long> ids = new java.util.ArrayList<Long>();
					java.util.Map<Long, Long> jumlahTrx = new java.util.HashMap<Long, Long>();
					for (int k = 0; k < items.length(); k++) {
						JSONObject it = items.getJSONObject(k);
						Long id = Long.valueOf(it.getLong("id"));
						ids.add(id);
						jumlahTrx.put(id, Long.valueOf(it.getLong("jumlahTransaksi")));
					}
					java.util.Collections.sort(ids);

					Long survivor = null;
					for (Long id : ids) {
						Long jml = jumlahTrx.get(id);
						if (jml != null && jml.longValue() > 0) { survivor = id; break; }
					}
					if (survivor == null) survivor = ids.get(0);

					JSONArray idDihapusArr = new JSONArray();
					boolean adaMerge = false;
					for (Long id : ids) {
						if (id.equals(survivor)) continue;
						Long jml = jumlahTrx.get(id);
						if (jml != null && jml.longValue() > 0) adaMerge = true;
						petaGabung.put(id, survivor);
						idDihapusArr.put(id.longValue());
						produkDihapus++;
					}
					if (adaMerge) grupDigabungTransaksi++;
					grupDiproses++;

					JSONObject d = new JSONObject();
					d.put("survivorId", survivor.longValue());
					d.put("idDihapus", idDihapusArr);
					detail.put(d);
				}

				gabungkanDanHapusProdukBulk(conn, tokoId.longValue(), petaGabung);

				// Gap-closure "kunci_unik" (lihat JavaDoc Produk.hitungKunciUnik) -- gabungkanDanHapusProdukBulk
				// menulis lewat JDBC mentah (BUKAN session.save Hibernate), jadi baris survivor TIDAK
				// otomatis memicu @PreUpdate. Backfill SEMUA baris toko ini di sini (bukan cuma yg baru
				// digabung) supaya SETIAP kali "Bersihkan Produk Duplikat" dijalankan (jenis APAPUN),
				// kolom ini ikut disegarkan penuh -- prasyarat sebelum kolom ini bisa diberi UNIQUE INDEX
				// sungguhan (lihat migrasi SQL terpisah, TIDAK dijalankan otomatis di sini).
				java.sql.PreparedStatement psBackfillKunci = conn.prepareStatement(
						"UPDATE koperasi.produk SET kunci_unik = LOWER(regexp_replace("
								+ "COALESCE(kode,'') || '_' || COALESCE(barcode,'') || '_' || COALESCE(nama,'') || '_' || COALESCE(toko::text,''),"
								+ " '[^a-zA-Z0-9_]', '', 'g')) "
								+ "WHERE toko = ? AND kode IS NOT NULL AND TRIM(kode) <> '' AND nama IS NOT NULL AND TRIM(nama) <> ''");
				psBackfillKunci.setLong(1, tokoId.longValue());
				psBackfillKunci.executeUpdate();
				psBackfillKunci.close();

				session.getTransaction().commit();
			} catch (Exception e) {
				try {
					session.getTransaction().rollback();
				} catch (Exception eRollback) {
					ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(empty-catch) produkDuplikatHapus-rollback");
				}
				throw e;
			}

			hasil.put("status", "00");
			hasil.put("jenis", jenis);
			hasil.put("grupDiproses", grupDiproses);
			hasil.put("produkDihapus", produkDihapus);
			hasil.put("grupDigabungTransaksi", grupDigabungTransaksi);
			hasil.put("detail", detail);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Hapus Non-Aktif Tak Terpakai" (layar Katalog Barang, tombol khusus supervisor/admin) --
	 * gap-closure "produk lama yang sudah dinonaktifkan menumpuk selamanya di katalog" (Toko
	 * Al-Bahjah, muncul setelah fitur {@link #produkNonaktifkanTakDiimpor} mulai dipakai). HANYA
	 * menghapus produk yang (a) berstatus Non-Aktif DAN (b) TIDAK PERNAH direferensikan di SATU PUN
	 * tabel transaksi/rekam jejak ({@link #TABEL_REFERENSI_PRODUK}) ATAU sbg bahan baku resep produk
	 * lain -- produk Non-Aktif yang PERNAH dipakai (mis. masih ada di riwayat penjualan lama) TETAP
	 * DIPERTAHANKAN (dihapus permanen akan merusak riwayat/laporan lama yang mereferensikannya),
	 * hanya dilaporkan sbg "dipertahankan" -- BUKAN error, bukan diam-diam dilewati tanpa dihitung.
	 *
	 * <p>Foto (kalau ada) ikut dihapus dari database streaming terpisah (best-effort, gagal-toleran
	 * -- sama alasan dgn {@link #gabungkanDanHapusProdukBulk}) supaya tidak jadi berkas yatim.</p>
	 */
	public static void produkHapusNonaktifTakTerpakai(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = gerbangDanTokoImporProduk(tbmuser, request, hasil,
				"Hanya admin/manager atau supervisor toko yang dapat menghapus produk non-aktif.");
		if (tokoId == null) return;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			java.sql.Connection conn = session.connection();

			java.util.List<Long> kandidat = new java.util.ArrayList<Long>();
			java.sql.PreparedStatement psKandidat = conn.prepareStatement(
					"SELECT id FROM koperasi.produk WHERE toko = ? AND aktif = false");
			psKandidat.setLong(1, tokoId);
			java.sql.ResultSet rsKandidat = psKandidat.executeQuery();
			while (rsKandidat.next()) kandidat.add(Long.valueOf(rsKandidat.getLong(1)));
			rsKandidat.close();
			psKandidat.close();

			if (kandidat.isEmpty()) {
				session.getTransaction().commit();
				hasil.put("status", "00");
				hasil.put("dihapus", 0);
				hasil.put("dipertahankan", 0);
				return;
			}

			StringBuilder placeholder = new StringBuilder();
			for (int i = 0; i < kandidat.size(); i++) {
				if (i > 0) placeholder.append(',');
				placeholder.append('?');
			}

			java.util.Set<Long> terpakai = new java.util.HashSet<Long>();
			for (String[] tk : TABEL_REFERENSI_PRODUK) {
				java.sql.PreparedStatement ps = conn.prepareStatement(
						"SELECT DISTINCT " + tk[1] + " FROM " + tk[0] + " WHERE " + tk[1] + " IN (" + placeholder + ")");
				for (int i = 0; i < kandidat.size(); i++) ps.setLong(i + 1, kandidat.get(i).longValue());
				java.sql.ResultSet rs = ps.executeQuery();
				while (rs.next()) terpakai.add(Long.valueOf(rs.getLong(1)));
				rs.close();
				ps.close();
			}

			// Bahan baku resep produk LAIN (JSON self-reference, BUKAN kolom FK biasa) -- sama pola dgn
			// gabungkanDanHapusProdukBulk, dibatasi ke toko ini saja.
			java.sql.PreparedStatement psBb = conn.prepareStatement(
					"SELECT bahanbaku FROM koperasi.produk WHERE toko = ? AND bahanbaku IS NOT NULL AND bahanbaku <> '[]'");
			psBb.setLong(1, tokoId);
			java.sql.ResultSet rsBb = psBb.executeQuery();
			while (rsBb.next()) {
				String mentah = rsBb.getString(1);
				try {
					JSONArray arr = new JSONArray(mentah);
					for (int i = 0; i < arr.length(); i++) {
						JSONObject bahan = arr.optJSONObject(i);
						if (bahan == null) continue;
						long idBahan = bahan.optLong("produk", -1);
						if (idBahan >= 0) terpakai.add(Long.valueOf(idBahan));
					}
				} catch (Exception eJson) {
					ais.common.ErrorAuditUtil.record(eJson, "produkHapusNonaktifTakTerpakai bahanbaku toko=" + tokoId);
				}
			}
			rsBb.close();
			psBb.close();

			java.util.List<Long> amanDihapus = new java.util.ArrayList<Long>();
			for (Long id : kandidat) {
				if (!terpakai.contains(id)) amanDihapus.add(id);
			}

			int dihapus = 0;
			if (!amanDihapus.isEmpty()) {
				java.sql.PreparedStatement psHapus = conn.prepareStatement("DELETE FROM koperasi.produk WHERE id = ?");
				for (Long id : amanDihapus) {
					psHapus.setLong(1, id.longValue());
					psHapus.addBatch();
				}
				psHapus.executeBatch();
				psHapus.close();
				dihapus = amanDihapus.size();
			}
			session.getTransaction().commit();

			// Foto (database streaming terpisah, TANPA FK constraint) -- best-effort, TIDAK ikut
			// membatalkan penghapusan utama yg sudah commit kalau gagal -- lihat JavaDoc
			// gabungkanDanHapusProdukBulk soal alasan lengkap kenapa harus lewat koneksi terpisah ini.
			if (!amanDihapus.isEmpty()) {
				try {
					org.hibernate.Session streamSession = ais.database.hibernate.StreamingHibernateUtil.getInstance().currentSession();
					java.sql.PreparedStatement psFoto = streamSession.connection().prepareStatement(
							"DELETE FROM public.foto_gambar_produk WHERE produk = ?");
					for (Long id : amanDihapus) {
						psFoto.setLong(1, id.longValue());
						psFoto.addBatch();
					}
					psFoto.executeBatch();
					psFoto.close();
				} catch (Exception eFoto) {
					ais.common.ErrorAuditUtil.record(eFoto, "produkHapusNonaktifTakTerpakai-fotoGambarProduk toko=" + tokoId);
				}
			}

			hasil.put("status", "00");
			hasil.put("dihapus", dihapus);
			hasil.put("dipertahankan", kandidat.size() - dihapus);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Hapus Produk Tak Ada Transaksi" (layar Katalog Barang, tombol khusus supervisor/admin) --
	 * gap-closure "toko ingin unggah ulang katalog baru tapi baris-baris lama yang salah/kotor (mis.
	 * dari upload Excel yang keliru) mengganjal" (permintaan user: "hapus produk-produk di toko ini
	 * yang belum ada transaksinya... tujuannya agar membersihkan data-data kotor agar bisa diupload
	 * kembali data baru"). BERBEDA dari {@link #produkHapusNonaktifTakTerpakai} dalam DUA hal: (a)
	 * TIDAK dibatasi status Non-Aktif -- mencakup SEMUA produk toko ini (aktif maupun tidak), krn baris
	 * yang baru saja salah-upload biasanya MASIH aktif; (b) {@code koperasi.stok_opname} SENGAJA
	 * DIKECUALIKAN dari daftar tabel "referensi yang memblokir" ({@link #TABEL_REFERENSI_PRODUK}) --
	 * kalau satu-satunya jejak sebuah produk hanyalah riwayat stok opname (bukan transaksi jual-beli
	 * sungguhan), baris stok_opname itu justru IKUT DIHAPUS (cascade) bersama produknya, PERSIS seperti
	 * diminta user ("termasuk hapus stokopname-nya jika ada"). Produk yang PERNAH benar-benar
	 * ditransaksikan ({@code koperasi.pembelian}/{@code draft_pembelian}, bagian dari {@link
	 * #TABEL_REFERENSI_PRODUK}) atau direferensikan di tabel lain mana pun (selain stok_opname) TETAP
	 * DIPERTAHANKAN -- dihapus permanen akan merusak riwayat/laporan lama yang mereferensikannya.
	 *
	 * <p>Foto (kalau ada) ikut dihapus dari database streaming terpisah (best-effort, gagal-toleran --
	 * sama alasan dgn {@link #gabungkanDanHapusProdukBulk}).</p>
	 */
	public static void produkHapusTakAdaTransaksi(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = gerbangDanTokoImporProduk(tbmuser, request, hasil,
				"Hanya admin/manager atau supervisor toko yang dapat menghapus produk tak bertransaksi.");
		if (tokoId == null) return;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			java.sql.Connection conn = session.connection();

			java.util.List<Long> kandidat = new java.util.ArrayList<Long>();
			java.sql.PreparedStatement psKandidat = conn.prepareStatement(
					"SELECT id FROM koperasi.produk WHERE toko = ?");
			psKandidat.setLong(1, tokoId);
			java.sql.ResultSet rsKandidat = psKandidat.executeQuery();
			while (rsKandidat.next()) kandidat.add(Long.valueOf(rsKandidat.getLong(1)));
			rsKandidat.close();
			psKandidat.close();

			if (kandidat.isEmpty()) {
				session.getTransaction().commit();
				hasil.put("status", "00");
				hasil.put("dihapus", 0);
				hasil.put("dipertahankan", 0);
				hasil.put("stokOpnameDihapus", 0);
				return;
			}

			StringBuilder placeholder = new StringBuilder();
			for (int i = 0; i < kandidat.size(); i++) {
				if (i > 0) placeholder.append(',');
				placeholder.append('?');
			}

			java.util.Set<Long> terpakai = new java.util.HashSet<Long>();
			for (String[] tk : TABEL_REFERENSI_PRODUK) {
				if ("koperasi.stok_opname".equals(tk[0])) continue; // sengaja dikecualikan -- lihat JavaDoc method ini
				java.sql.PreparedStatement ps = conn.prepareStatement(
						"SELECT DISTINCT " + tk[1] + " FROM " + tk[0] + " WHERE " + tk[1] + " IN (" + placeholder + ")");
				for (int i = 0; i < kandidat.size(); i++) ps.setLong(i + 1, kandidat.get(i).longValue());
				java.sql.ResultSet rs = ps.executeQuery();
				while (rs.next()) terpakai.add(Long.valueOf(rs.getLong(1)));
				rs.close();
				ps.close();
			}

			// Bahan baku resep produk LAIN (JSON self-reference, BUKAN kolom FK biasa) -- sama pola dgn
			// produkHapusNonaktifTakTerpakai, dibatasi ke toko ini saja.
			java.sql.PreparedStatement psBb = conn.prepareStatement(
					"SELECT bahanbaku FROM koperasi.produk WHERE toko = ? AND bahanbaku IS NOT NULL AND bahanbaku <> '[]'");
			psBb.setLong(1, tokoId);
			java.sql.ResultSet rsBb = psBb.executeQuery();
			while (rsBb.next()) {
				String mentah = rsBb.getString(1);
				try {
					JSONArray arr = new JSONArray(mentah);
					for (int i = 0; i < arr.length(); i++) {
						JSONObject bahan = arr.optJSONObject(i);
						if (bahan == null) continue;
						long idBahan = bahan.optLong("produk", -1);
						if (idBahan >= 0) terpakai.add(Long.valueOf(idBahan));
					}
				} catch (Exception eJson) {
					ais.common.ErrorAuditUtil.record(eJson, "produkHapusTakAdaTransaksi bahanbaku toko=" + tokoId);
				}
			}
			rsBb.close();
			psBb.close();

			java.util.List<Long> amanDihapus = new java.util.ArrayList<Long>();
			for (Long id : kandidat) {
				if (!terpakai.contains(id)) amanDihapus.add(id);
			}

			int stokOpnameDihapus = 0;
			int dihapus = 0;
			if (!amanDihapus.isEmpty()) {
				java.sql.PreparedStatement psSo = conn.prepareStatement("DELETE FROM koperasi.stok_opname WHERE produk = ?");
				for (Long id : amanDihapus) {
					psSo.setLong(1, id.longValue());
					psSo.addBatch();
				}
				int[] hasilSo = psSo.executeBatch();
				psSo.close();
				for (int n : hasilSo) if (n > 0) stokOpnameDihapus += n;

				java.sql.PreparedStatement psHapus = conn.prepareStatement("DELETE FROM koperasi.produk WHERE id = ?");
				for (Long id : amanDihapus) {
					psHapus.setLong(1, id.longValue());
					psHapus.addBatch();
				}
				psHapus.executeBatch();
				psHapus.close();
				dihapus = amanDihapus.size();
			}
			session.getTransaction().commit();

			// Foto (database streaming terpisah, TANPA FK constraint) -- best-effort, TIDAK ikut
			// membatalkan penghapusan utama yg sudah commit kalau gagal.
			if (!amanDihapus.isEmpty()) {
				try {
					org.hibernate.Session streamSession = ais.database.hibernate.StreamingHibernateUtil.getInstance().currentSession();
					java.sql.PreparedStatement psFoto = streamSession.connection().prepareStatement(
							"DELETE FROM public.foto_gambar_produk WHERE produk = ?");
					for (Long id : amanDihapus) {
						psFoto.setLong(1, id.longValue());
						psFoto.addBatch();
					}
					psFoto.executeBatch();
					psFoto.close();
				} catch (Exception eFoto) {
					ais.common.ErrorAuditUtil.record(eFoto, "produkHapusTakAdaTransaksi-fotoGambarProduk toko=" + tokoId);
				}
			}

			hasil.put("status", "00");
			hasil.put("dihapus", dihapus);
			hasil.put("dipertahankan", kandidat.size() - dihapus);
			hasil.put("stokOpnameDihapus", stokOpnameDihapus);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Dasbor Statistik Anggota/Customer" (JSP/ZK/Desktop/Android) -- kartu ringkasan anggota
	 * koperasi: total anggota, aktif/nonaktif, wajib-PIN, plus rincian (breakdown) top-8 per jenis
	 * anggota utk chart batang horizontal. TIDAK di-scope per toko -- anggota koperasi bersifat
	 * lintas-toko (satu orang bisa belanja di toko mana pun dalam satu koperasi), pola sama persis
	 * {@link #anggotaList} yang JUGA tidak menyaring per toko.
	 *
	 * @param hasil diisi {@code status="00"}, KPI ({@code totalAnggota}, {@code totalAktif},
	 *              {@code totalNonaktif}, {@code totalWajibPin}), dan {@code byJenis} (array
	 *              {@code {label,jumlah}}).
	 */
	public static void anggotaStatistik(JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();

			long totalAnggota = 0, totalAktif = 0, totalNonaktif = 0, totalWajibPin = 0;
			java.sql.PreparedStatement psKpi = conn.prepareStatement(
					"SELECT COUNT(*), COUNT(CASE WHEN COALESCE(a.aktif,true)=true THEN 1 END), "
							+ "COUNT(CASE WHEN COALESCE(a.aktif,true)=false THEN 1 END), "
							+ "COUNT(CASE WHEN COALESCE(j.wajib_pin,false)=true THEN 1 END) "
							+ "FROM koperasi.anggota_koperasi a "
							+ "LEFT JOIN koperasi.jenis_anggota_koperasi j ON a.jenis_anggota_koperasi = j.id");
			java.sql.ResultSet rsKpi = psKpi.executeQuery();
			if (rsKpi.next()) {
				totalAnggota = rsKpi.getLong(1);
				totalAktif = rsKpi.getLong(2);
				totalNonaktif = rsKpi.getLong(3);
				totalWajibPin = rsKpi.getLong(4);
			}
			rsKpi.close(); psKpi.close();

			JSONArray byJenis = new JSONArray();
			java.sql.PreparedStatement psJenis = conn.prepareStatement(
					"SELECT COALESCE(j.nama,'Tanpa Jenis') lbl, COUNT(*) cnt FROM koperasi.anggota_koperasi a "
							+ "LEFT JOIN koperasi.jenis_anggota_koperasi j ON a.jenis_anggota_koperasi = j.id "
							+ "GROUP BY lbl ORDER BY cnt DESC LIMIT 8");
			java.sql.ResultSet rsJenis = psJenis.executeQuery();
			while (rsJenis.next()) {
				JSONObject o = new JSONObject();
				o.put("label", rsJenis.getString(1));
				o.put("jumlah", rsJenis.getLong(2));
				byJenis.put(o);
			}
			rsJenis.close(); psJenis.close();

			hasil.put("status", "00");
			hasil.put("totalAnggota", totalAnggota);
			hasil.put("totalAktif", totalAktif);
			hasil.put("totalNonaktif", totalNonaktif);
			hasil.put("totalWajibPin", totalWajibPin);
			hasil.put("byJenis", byJenis);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Dasbor statistik Laporan Transaksi (gap-closure paritas Produk/Anggota + "bedakan antar mesin
	 * POS dan siapa entry/login") -- KPI hari-ini &amp; 30-hari, plus breakdown omzet per KASIR dan
	 * per MESIN (30 hari terakhir). Sumber data DISATUKAN lewat CTE {@code trx} yang meng-groupkan
	 * baris item {@code koperasi.pembelian} ke level SATU-TRANSAKSI (via {@code pembelian_anggota_koperasi})
	 * -- pola SAMA PERSIS dgn {@code PosApi.daftarOrderDenganSesi} (kasir_login_nama/nama_mesin
	 * diutamakan, fallback ke {@code a.oleh} utk transaksi lama sebelum kolom itu ada).
	 */
	public static void transaksiStatistik(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();

			String withTrx = "WITH trx AS ("
					+ "  SELECT COALESCE(a.pembelian_anggota_koperasi, a.id) AS id_transaksi, MAX(a.waktu) AS waktu, "
					+ "         COALESCE(MAX(pak.total_biaya), SUM(a.total)) AS total_biaya, "
					+ "         COALESCE(MAX(pak.kasir_login_nama), MAX(a.oleh)) AS kasir, MAX(pak.nama_mesin) AS mesin "
					+ "  FROM koperasi.pembelian a LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON pak.id = a.pembelian_anggota_koperasi "
					+ "  WHERE a.toko = ? GROUP BY 1"
					+ ") ";

			long trxHariIni = 0, trx30Hari = 0;
			double omzetHariIni = 0, omzet30Hari = 0;
			java.sql.PreparedStatement psKpi = conn.prepareStatement(withTrx
					+ "SELECT COUNT(CASE WHEN DATE(waktu)=CURRENT_DATE THEN 1 END), "
					+ "COALESCE(SUM(CASE WHEN DATE(waktu)=CURRENT_DATE THEN total_biaya END),0), "
					+ "COUNT(CASE WHEN waktu >= NOW() - INTERVAL '30 days' THEN 1 END), "
					+ "COALESCE(SUM(CASE WHEN waktu >= NOW() - INTERVAL '30 days' THEN total_biaya END),0) FROM trx");
			psKpi.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsKpi = psKpi.executeQuery();
			if (rsKpi.next()) {
				trxHariIni = rsKpi.getLong(1);
				omzetHariIni = rsKpi.getDouble(2);
				trx30Hari = rsKpi.getLong(3);
				omzet30Hari = rsKpi.getDouble(4);
			}
			rsKpi.close(); psKpi.close();

			JSONArray byKasir = new JSONArray();
			java.sql.PreparedStatement psKasir = conn.prepareStatement(withTrx
					+ "SELECT COALESCE(NULLIF(TRIM(kasir),''),'Tanpa Nama') lbl, COALESCE(SUM(total_biaya),0) nilai FROM trx "
					+ "WHERE waktu >= NOW() - INTERVAL '30 days' GROUP BY 1 ORDER BY 2 DESC LIMIT 8");
			psKasir.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsKasir = psKasir.executeQuery();
			while (rsKasir.next()) {
				JSONObject o = new JSONObject();
				o.put("label", rsKasir.getString(1));
				o.put("nilai", rsKasir.getDouble(2));
				byKasir.put(o);
			}
			rsKasir.close(); psKasir.close();

			JSONArray byMesin = new JSONArray();
			java.sql.PreparedStatement psMesin = conn.prepareStatement(withTrx
					+ "SELECT COALESCE(NULLIF(TRIM(mesin),''),'Tanpa Nama Mesin') lbl, COALESCE(SUM(total_biaya),0) nilai FROM trx "
					+ "WHERE waktu >= NOW() - INTERVAL '30 days' GROUP BY 1 ORDER BY 2 DESC LIMIT 8");
			psMesin.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsMesin = psMesin.executeQuery();
			while (rsMesin.next()) {
				JSONObject o = new JSONObject();
				o.put("label", rsMesin.getString(1));
				o.put("nilai", rsMesin.getDouble(2));
				byMesin.put(o);
			}
			rsMesin.close(); psMesin.close();

			hasil.put("status", "00");
			hasil.put("trxHariIni", trxHariIni);
			hasil.put("omzetHariIni", omzetHariIni);
			hasil.put("trx30Hari", trx30Hari);
			hasil.put("omzet30Hari", omzet30Hari);
			hasil.put("byKasir", byKasir);
			hasil.put("byMesin", byMesin);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** Ikat satu parameter PreparedStatement sesuai tipe runtime-nya -- salinan {@code PosApi.ikatParam} (private di kelas itu, tak bisa dipakai lintas-kelas), dipakai query WHERE dinamis (jumlah kondisi bervariasi) di {@link #peringkatMitra}/{@link #ambilOmzetPeriodeSebelumnya}. */
	private static void ikatParam(java.sql.PreparedStatement ps, int idx, Object v) throws Exception {
		if (v instanceof Long) ps.setLong(idx, ((Long) v).longValue());
		else if (v instanceof Integer) ps.setInt(idx, ((Integer) v).intValue());
		else ps.setString(idx, String.valueOf(v));
	}

	/**
	 * Dasbor "Peringkat Mitra/Toko" (gap-closure kloning tab ZK {@code DashboardKantinAction.
	 * buildLeaderboardMitra}, lihat JavaDoc method itu) -- daftar toko diurut OMZET DESC (30 hari
	 * terakhir bila filter tanggal kosong) + Margin% (SENGAJA disederhanakan dari versi ZK: pakai
	 * {@code hargabeli} produk langsung, TANPA rollup resep/bahan-baku JSON petaHargaPokok() ZK --
	 * konsisten dgn produkStatistik yg juga langsung pakai hargabeli utk nilai stok) + Pertumbuhan%
	 * (bandingkan periode SEBELUMNYA yg sama panjangnya, status "Tumbuh Pesat"/"Bertumbuh"/"Stabil"/
	 * "Menurun" pakai ambang batas SAMA PERSIS dgn ZK: &gt;=20%/&gt;0%/&gt;=-5%/lainnya).
	 * <p>Cross-toko (semua toko sekaligus) utk admin global (tanpa Pedagang terkait) -- SAMA pola dgn
	 * {@code PosApi.prosesDashboardUmum}; pedagang/kasir toko selalu terkunci ke tokonya sendiri
	 * (leaderboard 1-baris, sama spt ZK menampilkan pesan pengalihan utk kasus ini -- di sini tetap
	 * dikembalikan sbg data, klien yg boleh memutuskan tampilan pesan itu).</p>
	 * @param request payload opsional {@code tglMulai}/{@code tglSampai} (default 30 hari terakhir), {@code toko_id} (dipakai HANYA jika pemanggil admin global).
	 */
	public static void peringkatMitra(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			ais.database.model.inventory.Pedagang pemanggil = tbmuser == null ? null : tbmuser.getPedagang();
			Long tokoId = pemanggil != null ? (pemanggil.getToko() == null ? null : pemanggil.getToko().getId())
					: (request.isNull("toko_id") ? null : Long.valueOf((request.get("toko_id") + "").trim()));
			boolean semuaToko = (tokoId == null);

			String tglMulai = request.optString("tglMulai", "");
			String tglSampai = request.optString("tglSampai", "");
			String kondisiWaktu;
			java.util.List<Object> paramsWaktu = new java.util.ArrayList<Object>();
			if (tglMulai.length() > 0 || tglSampai.length() > 0) {
				StringBuilder wb = new StringBuilder("1=1");
				if (tglMulai.length() > 0) { wb.append(" AND DATE(p.waktu) >= ?"); paramsWaktu.add(tglMulai); }
				if (tglSampai.length() > 0) { wb.append(" AND DATE(p.waktu) <= ?"); paramsWaktu.add(tglSampai); }
				kondisiWaktu = wb.toString();
			} else {
				kondisiWaktu = "p.waktu >= NOW() - INTERVAL '30 days'";
			}
			String kondisiToko = semuaToko ? "" : " AND b.id = ?";

			StringBuilder sqlUtama = new StringBuilder(
					"SELECT b.id, b.nama, COALESCE(SUM(p.total),0) omzet, COUNT(DISTINCT p.id) trx, "
							+ "COALESCE(SUM(p.qty),0) qty, COALESCE(SUM(p.qty*COALESCE(pr.hargabeli,0)),0) modal "
							+ "FROM koperasi.pembelian p JOIN koperasi.toko b ON b.id = p.toko "
							+ "LEFT JOIN koperasi.produk pr ON pr.id = p.produk "
							+ "WHERE p.aktif = true AND " + kondisiWaktu + kondisiToko
							+ " GROUP BY b.id, b.nama ORDER BY omzet DESC");
			java.sql.PreparedStatement psUtama = conn.prepareStatement(sqlUtama.toString());
			int idx = 1;
			for (Object p : paramsWaktu) ikatParam(psUtama, idx++, p);
			if (!semuaToko) psUtama.setLong(idx++, tokoId.longValue());

			java.util.Map<Long, double[]> omzetPrevPerToko = ambilOmzetPeriodeSebelumnya(conn, tglMulai, tglSampai, semuaToko, tokoId);

			JSONArray daftar = new JSONArray();
			long totalToko = 0;
			double omzetTertinggi = -1; String namaOmzetTertinggi = null;
			double pertumbuhanTertinggi = Double.NEGATIVE_INFINITY; String namaPertumbuhanTertinggi = null;
			double pertumbuhanTerendah = Double.POSITIVE_INFINITY; String namaPertumbuhanTerendah = null;

			java.sql.ResultSet rsUtama = psUtama.executeQuery();
			while (rsUtama.next()) {
				long idToko = rsUtama.getLong(1);
				String nama = rsUtama.getString(2);
				double omzet = rsUtama.getDouble(3);
				long trx = rsUtama.getLong(4);
				double qty = rsUtama.getDouble(5);
				double modal = rsUtama.getDouble(6);
				double margin = omzet > 0 ? ((omzet - modal) / omzet) * 100.0 : 0.0;

				double[] prev = omzetPrevPerToko.get(Long.valueOf(idToko));
				Double pertumbuhan = null;
				String status;
				if (prev != null && prev[0] > 0) {
					pertumbuhan = Double.valueOf(((omzet - prev[0]) / prev[0]) * 100.0);
					double pv = pertumbuhan.doubleValue();
					status = pv >= 20 ? "Tumbuh Pesat" : pv > 0 ? "Bertumbuh" : pv >= -5 ? "Stabil" : "Menurun";
					if (pv > pertumbuhanTertinggi) { pertumbuhanTertinggi = pv; namaPertumbuhanTertinggi = nama; }
					if (pv < pertumbuhanTerendah) { pertumbuhanTerendah = pv; namaPertumbuhanTerendah = nama; }
				} else {
					status = omzet > 0 ? "Baru" : "Stabil";
				}

				JSONObject o = new JSONObject();
				o.put("tokoId", idToko);
				o.put("nama", nama);
				o.put("omzet", omzet);
				o.put("transaksi", trx);
				o.put("qty", qty);
				o.put("margin", margin);
				o.put("pertumbuhan", pertumbuhan == null ? JSONObject.NULL : pertumbuhan);
				o.put("status", status);
				daftar.put(o);

				totalToko++;
				if (omzet > omzetTertinggi) { omzetTertinggi = omzet; namaOmzetTertinggi = nama; }
			}
			rsUtama.close(); psUtama.close();

			hasil.put("status", "00");
			hasil.put("semuaToko", semuaToko);
			hasil.put("totalToko", totalToko);
			hasil.put("omzetTertinggi", totalToko > 0 ? omzetTertinggi : 0);
			hasil.put("namaOmzetTertinggi", namaOmzetTertinggi == null ? "" : namaOmzetTertinggi);
			hasil.put("pertumbuhanTertinggi", namaPertumbuhanTertinggi == null ? JSONObject.NULL : Double.valueOf(pertumbuhanTertinggi));
			hasil.put("namaPertumbuhanTertinggi", namaPertumbuhanTertinggi == null ? "" : namaPertumbuhanTertinggi);
			boolean adaPerluPerhatian = namaPertumbuhanTerendah != null && pertumbuhanTerendah < 0;
			hasil.put("pertumbuhanTerendah", adaPerluPerhatian ? Double.valueOf(pertumbuhanTerendah) : JSONObject.NULL);
			hasil.put("namaPertumbuhanTerendah", adaPerluPerhatian ? namaPertumbuhanTerendah : "");
			hasil.put("daftar", daftar);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Omzet per toko pada periode SEBELUMNYA (panjang SAMA dgn periode utama) -- dipakai
	 * {@link #peringkatMitra} menghitung persentase pertumbuhan. Bila filter tanggal kosong (default
	 * 30 hari terakhir), periode sebelumnya adalah 30 hari SEBELUM itu (hari ke-31 s.d. ke-60 lalu).
	 * @return peta {@code tokoId -> [omzetPeriodeSebelumnya]} (array 1-elemen supaya bisa dibaca java 7 tanpa boxing eksplisit di caller).
	 */
	private static java.util.Map<Long, double[]> ambilOmzetPeriodeSebelumnya(java.sql.Connection conn,
			String tglMulai, String tglSampai, boolean semuaToko, Long tokoId) throws Exception {
		java.util.Map<Long, double[]> peta = new java.util.HashMap<Long, double[]>();
		String kondisiWaktu;
		java.util.List<Object> params = new java.util.ArrayList<Object>();
		if (tglMulai.length() > 0 || tglSampai.length() > 0) {
			String mulai = tglMulai.length() > 0 ? tglMulai : tglSampai;
			String sampai = tglSampai.length() > 0 ? tglSampai : tglMulai;
			// periode sebelumnya = panjang SAMA persis, berakhir tepat sebelum "mulai" -- dihitung
			// murni via aritmetika tanggal PostgreSQL (BUKAN java.time) supaya konsisten dgn gaya
			// raw-JDBC file ini & menghindari ambiguitas timezone antara JVM vs kolom timestamp DB.
			kondisiWaktu = "p.waktu >= (CAST(? AS date) - (CAST(? AS date) - CAST(? AS date) + 1)) AND p.waktu < CAST(? AS date)";
			params.add(mulai); params.add(sampai); params.add(mulai); params.add(mulai);
		} else {
			kondisiWaktu = "p.waktu >= NOW() - INTERVAL '60 days' AND p.waktu < NOW() - INTERVAL '30 days'";
		}
		String kondisiToko = semuaToko ? "" : " AND b.id = ?";
		String sql = "SELECT b.id, COALESCE(SUM(p.total),0) FROM koperasi.pembelian p JOIN koperasi.toko b ON b.id = p.toko "
				+ "WHERE p.aktif = true AND " + kondisiWaktu + kondisiToko + " GROUP BY b.id";
		java.sql.PreparedStatement ps = conn.prepareStatement(sql);
		int idx = 1;
		for (Object p : params) ikatParam(ps, idx++, p);
		if (!semuaToko) ps.setLong(idx++, tokoId.longValue());
		java.sql.ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			peta.put(Long.valueOf(rs.getLong(1)), new double[] { rs.getDouble(2) });
		}
		rs.close(); ps.close();
		return peta;
	}

	/**
	 * Dasbor "Resep, HPP & Margin" (gap-closure kloning ZK {@code DashboardKantinAction.buildResepHpp}
	 * + {@code petaHargaPokok()}) -- HPP dihitung via rollup resep 1-level (JSON {@code bahanbaku}
	 * berisi array {@code {produk,qty}}, harga tiap bahan diambil dari {@code hargabeli}-nya SENDIRI,
	 * BUKAN rekursif walau bahan itu sendiri punya resep -- SAMA PERSIS perilaku ZK) dgn fallback ke
	 * {@code hargabeli} produk itu sendiri bila tak punya resep/JSON gagal parse. Hanya produk yg
	 * PUNYA resep (bahanbaku non-kosong) yg muncul di {@code daftarMenu} (config-only, bukan
	 * period-bound) -- pemakaian bahan baku (30 hari) tetap period-bound spt tab lain.
	 */
	public static void resepHppMargin(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			String kondisiToko = tokoId == null ? "" : " AND p.toko = ?";

			java.util.Map<Long, Double> hargaBeliSemua = new java.util.HashMap<Long, Double>();
			java.util.Map<Long, String> bahanBakuSemua = new java.util.HashMap<Long, String>();
			java.sql.PreparedStatement psSemuaProduk = conn.prepareStatement(
					"SELECT id, COALESCE(bahanbaku,''), COALESCE(hargabeli,0) FROM koperasi.produk p WHERE 1=1" + kondisiToko);
			if (tokoId != null) psSemuaProduk.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsSemuaProduk = psSemuaProduk.executeQuery();
			while (rsSemuaProduk.next()) {
				Long id = Long.valueOf(rsSemuaProduk.getLong(1));
				hargaBeliSemua.put(id, Double.valueOf(rsSemuaProduk.getDouble(3)));
				bahanBakuSemua.put(id, rsSemuaProduk.getString(2));
			}
			rsSemuaProduk.close(); psSemuaProduk.close();

			java.util.Map<Long, Double> peta = new java.util.HashMap<Long, Double>();
			for (java.util.Map.Entry<Long, String> e : bahanBakuSemua.entrySet()) {
				String bb = e.getValue() == null ? "" : e.getValue().trim();
				if (bb.isEmpty() || "[]".equals(bb)) {
					peta.put(e.getKey(), hargaBeliSemua.get(e.getKey()));
					continue;
				}
				try {
					JSONArray arr = new JSONArray(bb);
					double hpp = 0;
					for (int i = 0; i < arr.length(); i++) {
						JSONObject item = arr.optJSONObject(i);
						if (item == null) continue;
						Long idBahan = Long.valueOf(item.optLong("produk", 0));
						double qty = item.optDouble("qty", 0);
						Double hargaBahan = hargaBeliSemua.get(idBahan);
						hpp += qty * (hargaBahan == null ? 0 : hargaBahan.doubleValue());
					}
					peta.put(e.getKey(), Double.valueOf(hpp));
				} catch (Exception exParse) {
					peta.put(e.getKey(), hargaBeliSemua.get(e.getKey()));
				}
			}

			String sqlResep = "SELECT p.id, p.nama, p.bahanbaku, COALESCE(p.hargajual,0), COALESCE(jp.nama,'Lainnya') "
					+ "FROM koperasi.produk p LEFT JOIN koperasi.jenis_produk jp ON jp.id = p.jenis_produk "
					+ "WHERE p.bahanbaku IS NOT NULL AND TRIM(p.bahanbaku) NOT IN ('','[]')" + kondisiToko + " ORDER BY p.nama";
			java.sql.PreparedStatement psResep = conn.prepareStatement(sqlResep);
			if (tokoId != null) psResep.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsResep = psResep.executeQuery();

			JSONArray daftarMenu = new JSONArray();
			java.util.List<JSONObject> listMenu = new java.util.ArrayList<JSONObject>();
			long totalMenu = 0;
			double sumMargin = 0; int nMarginValid = 0;
			double marginTerendah = Double.POSITIVE_INFINITY; String namaMarginTerendah = null;
			while (rsResep.next()) {
				Long id = Long.valueOf(rsResep.getLong(1));
				String nama = rsResep.getString(2);
				String bahanbaku = rsResep.getString(3);
				double jual = rsResep.getDouble(4);
				String kategori = rsResep.getString(5);
				Double hppObj = peta.get(id);
				double hpp = hppObj == null ? 0 : hppObj.doubleValue();
				int jmlBahan = 0;
				try { JSONArray arrHitung = new JSONArray(bahanbaku); jmlBahan = arrHitung.length(); } catch (Exception exHitung) { /* abaikan */ }
				double margin = jual > 0 ? ((jual - hpp) / jual) * 100.0 : 0.0;
				double untung = jual - hpp;

				JSONObject o = new JSONObject();
				o.put("nama", nama); o.put("kategori", kategori); o.put("jmlBahan", jmlBahan);
				o.put("hpp", hpp); o.put("hargaJual", jual); o.put("untung", untung); o.put("margin", margin);
				daftarMenu.put(o);
				listMenu.add(o);

				totalMenu++;
				if (jual > 0) {
					sumMargin += margin; nMarginValid++;
					if (margin < marginTerendah) { marginTerendah = margin; namaMarginTerendah = nama; }
				}
			}
			rsResep.close(); psResep.close();

			java.util.Collections.sort(listMenu, new java.util.Comparator<JSONObject>() {
				public int compare(JSONObject a, JSONObject b) {
					try { return Double.compare(b.getDouble("margin"), a.getDouble("margin")); }
					catch (Exception exSort) { return 0; }
				}
			});
			JSONArray topMargin = new JSONArray();
			for (int i = 0; i < Math.min(12, listMenu.size()); i++) topMargin.put(listMenu.get(i));

			String kondisiTokoPb = tokoId == null ? "" : " AND pb.toko = ?";
			JSONArray byBahan = new JSONArray();
			double nilaiBahanTerpakai = 0;
			java.sql.PreparedStatement psBahan = conn.prepareStatement(
					"SELECT COALESCE(pr.nama,'-') nm, COALESCE(SUM(pb.qty),0) qty, COALESCE(SUM(pb.qty*COALESCE(pr.hargabeli,0)),0) nilai "
							+ "FROM koperasi.pemakaian_bahan_baku pb LEFT JOIN koperasi.produk pr ON pr.id = pb.produk "
							+ "WHERE pb.waktu >= NOW() - INTERVAL '30 days'" + kondisiTokoPb + " GROUP BY pr.nama ORDER BY qty DESC LIMIT 12");
			if (tokoId != null) psBahan.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsBahan = psBahan.executeQuery();
			while (rsBahan.next()) {
				JSONObject o = new JSONObject();
				o.put("label", rsBahan.getString(1)); o.put("jumlah", rsBahan.getDouble(2));
				byBahan.put(o);
				nilaiBahanTerpakai += rsBahan.getDouble(3);
			}
			rsBahan.close(); psBahan.close();

			hasil.put("status", "00");
			hasil.put("totalMenu", totalMenu);
			hasil.put("rataMargin", nMarginValid > 0 ? sumMargin / nMarginValid : 0);
			hasil.put("marginTerendah", namaMarginTerendah == null ? JSONObject.NULL : Double.valueOf(marginTerendah));
			hasil.put("namaMarginTerendah", namaMarginTerendah == null ? "" : namaMarginTerendah);
			hasil.put("nilaiBahanTerpakai", nilaiBahanTerpakai);
			hasil.put("byBahanBaku", byBahan);
			hasil.put("topMargin", topMargin);
			hasil.put("daftarMenu", daftarMenu);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Dasbor "Ramalan Penjualan" (gap-closure kloning ZK {@code ForecastPenjualanKantinAction}) --
	 * regresi linear (least-squares) atas SERI JUMLAH TRANSAKSI harian 14 hari terakhir (BUKAN
	 * omzet -- omzet cuma ditampilkan sbg tren mentah tanpa regresi, SAMA PERSIS perilaku ZK).
	 * Granularitas SENGAJA disederhanakan ke harian SAJA (ZK punya combobox harian/mingguan/bulanan,
	 * di sini cukup default paling umum dipakai) supaya payload/API tetap sederhana.
	 */
	public static void ramalanPenjualan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			String kondisiToko = tokoId == null ? "" : " AND toko = ?";
			java.sql.PreparedStatement ps = conn.prepareStatement(
					"SELECT TO_CHAR(DATE(waktu),'DD Mon') lbl, COUNT(DISTINCT COALESCE(pembelian_anggota_koperasi, id)) trx, COALESCE(SUM(total),0) omzet "
							+ "FROM koperasi.pembelian WHERE waktu >= CURRENT_DATE - INTERVAL '14 days'" + kondisiToko
							+ " GROUP BY DATE(waktu) ORDER BY DATE(waktu) ASC");
			if (tokoId != null) ps.setLong(1, tokoId.longValue());
			java.sql.ResultSet rs = ps.executeQuery();

			java.util.List<String> labelList = new java.util.ArrayList<String>();
			java.util.List<Double> counts = new java.util.ArrayList<Double>();
			java.util.List<Double> omzetList = new java.util.ArrayList<Double>();
			while (rs.next()) {
				labelList.add(rs.getString(1));
				counts.add(Double.valueOf(rs.getDouble(2)));
				omzetList.add(Double.valueOf(rs.getDouble(3)));
			}
			rs.close(); ps.close();

			int n = counts.size();
			double slope = 0, intercept = 0;
			if (n >= 2) {
				double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
				for (int i = 0; i < n; i++) {
					double x = i, y = counts.get(i).doubleValue();
					sumX += x; sumY += y; sumXY += x * y; sumX2 += x * x;
				}
				double denom = n * sumX2 - sumX * sumX;
				if (denom != 0) {
					slope = (n * sumXY - sumX * sumY) / denom;
					intercept = (sumY - slope * sumX) / n;
				} else {
					intercept = sumY / n;
				}
			} else if (n == 1) {
				intercept = counts.get(0).doubleValue();
			}

			double totalTrx = 0;
			for (Double c : counts) totalTrx += c.doubleValue();
			double rata = n > 0 ? totalTrx / n : 0;
			double prediksiBerikut = Math.max(0, slope * n + intercept);
			boolean naik = slope >= 0;
			double persen = rata > 0 ? (slope / rata) * 100.0 : 0;

			JSONArray trenTransaksi = new JSONArray();
			JSONArray trenOmzet = new JSONArray();
			for (int i = 0; i < n; i++) {
				JSONObject t = new JSONObject(); t.put("label", labelList.get(i)); t.put("nilai", counts.get(i).doubleValue()); trenTransaksi.put(t);
				JSONObject o = new JSONObject(); o.put("label", labelList.get(i)); o.put("nilai", omzetList.get(i).doubleValue()); trenOmzet.put(o);
			}
			JSONArray proyeksi = new JSONArray();
			for (int i = 1; i <= 3; i++) {
				double v = Math.max(0, slope * (n - 1 + i) + intercept);
				JSONObject p = new JSONObject(); p.put("label", "Hari +" + i); p.put("nilai", v); proyeksi.put(p);
			}

			hasil.put("status", "00");
			hasil.put("totalTransaksi", totalTrx);
			hasil.put("rataRata", rata);
			hasil.put("prediksiBerikutnya", prediksiBerikut);
			hasil.put("naik", naik);
			hasil.put("persenTren", Math.abs(persen));
			hasil.put("trenTransaksi", trenTransaksi);
			hasil.put("trenOmzet", trenOmzet);
			hasil.put("proyeksi", proyeksi);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Dasbor "Monitor Promo &amp; Cashback" (gap-closure kloning ZK {@code MonitorDiskonKantinAction},
	 * disederhanakan ke periode TETAP 30 hari -- ZK punya combobox periode+filter jenis anggota, di
	 * sini cukup default paling umum dipakai). "Saldo Cashback Mengendap" SENGAJA all-time (bukan
	 * period-bound) -- SAMA PERSIS perilaku ZK, krn itu konsep saldo kumulatif bukan aktivitas
	 * periode. Sengaja TIDAK menyertakan tabel saldo "MRS" (fitur niche khusus kelompok anggota
	 * tertentu, di luar cakupan "Monitor Promo & Cashback" yg diminta).
	 */
	public static void monitorPromoCashback(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			String kondisiTokoA = tokoId == null ? "" : " AND a.toko = ?";

			double diskonDiberikan = 0, cashbackDiberikan = 0;
			java.sql.PreparedStatement psKpi1 = conn.prepareStatement(
					"SELECT COALESCE(SUM(a.total_diskon),0), COALESCE(SUM(a.totalcashback),0) FROM koperasi.pembelian_anggota_koperasi a "
							+ "WHERE a.tanggal_pembayaran >= NOW() - INTERVAL '30 days'" + kondisiTokoA);
			if (tokoId != null) psKpi1.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsKpi1 = psKpi1.executeQuery();
			if (rsKpi1.next()) { diskonDiberikan = rsKpi1.getDouble(1); cashbackDiberikan = rsKpi1.getDouble(2); }
			rsKpi1.close(); psKpi1.close();

			double cashbackDicairkan = 0;
			java.sql.PreparedStatement psKpi2 = conn.prepareStatement(
					"SELECT COALESCE(SUM(d.nominal_cair),0) FROM koperasi.pencairan_diskon d "
							+ "WHERE d.status IN ('BERHASIL','PENDING') AND d.waktu_pencairan >= NOW() - INTERVAL '30 days'"
							+ (tokoId == null ? "" : " AND d.toko = ?"));
			if (tokoId != null) psKpi2.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsKpi2 = psKpi2.executeQuery();
			if (rsKpi2.next()) cashbackDicairkan = rsKpi2.getDouble(1);
			rsKpi2.close(); psKpi2.close();

			double cashbackAllTime = 0, cairAllTime = 0;
			java.sql.PreparedStatement psSaldo1 = conn.prepareStatement(
					"SELECT COALESCE(SUM(a.totalcashback),0) FROM koperasi.pembelian_anggota_koperasi a WHERE 1=1" + kondisiTokoA);
			if (tokoId != null) psSaldo1.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsSaldo1 = psSaldo1.executeQuery();
			if (rsSaldo1.next()) cashbackAllTime = rsSaldo1.getDouble(1);
			rsSaldo1.close(); psSaldo1.close();

			java.sql.PreparedStatement psSaldo2 = conn.prepareStatement(
					"SELECT COALESCE(SUM(d.nominal_cair),0) FROM koperasi.pencairan_diskon d WHERE d.status IN ('BERHASIL','PENDING')"
							+ (tokoId == null ? "" : " AND d.toko = ?"));
			if (tokoId != null) psSaldo2.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsSaldo2 = psSaldo2.executeQuery();
			if (rsSaldo2.next()) cairAllTime = rsSaldo2.getDouble(1);
			rsSaldo2.close(); psSaldo2.close();
			double saldoMengendap = cashbackAllTime - cairAllTime;

			String kondisiTokoDet = tokoId == null ? "" : " AND det.toko = ?";
			JSONArray topProduk = new JSONArray();
			java.sql.PreparedStatement psTopProduk = conn.prepareStatement(
					"SELECT COALESCE(pr.nama,'-') nm, COALESCE(SUM(det.diskon)+SUM(det.cashback),0) nilai "
							+ "FROM koperasi.pembelian det LEFT JOIN koperasi.produk pr ON pr.id = det.produk "
							+ "WHERE (det.diskon > 0 OR det.cashback > 0) AND det.waktu >= NOW() - INTERVAL '30 days'" + kondisiTokoDet
							+ " GROUP BY pr.nama ORDER BY nilai DESC LIMIT 5");
			if (tokoId != null) psTopProduk.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsTopProduk = psTopProduk.executeQuery();
			while (rsTopProduk.next()) {
				JSONObject o = new JSONObject(); o.put("label", rsTopProduk.getString(1)); o.put("nilai", rsTopProduk.getDouble(2));
				topProduk.put(o);
			}
			rsTopProduk.close(); psTopProduk.close();

			JSONArray topMember = new JSONArray();
			java.sql.PreparedStatement psTopMember = conn.prepareStatement(
					"SELECT COALESCE(c.nama,'-') nm, COALESCE(SUM(a.totalcashback),0) nilai "
							+ "FROM koperasi.pembelian_anggota_koperasi a LEFT JOIN koperasi.anggota_koperasi c ON c.id = a.anggota_koperasi "
							+ "WHERE a.tanggal_pembayaran >= NOW() - INTERVAL '30 days'" + kondisiTokoA
							+ " GROUP BY c.nama ORDER BY nilai DESC LIMIT 8");
			if (tokoId != null) psTopMember.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsTopMember = psTopMember.executeQuery();
			while (rsTopMember.next()) {
				JSONObject o = new JSONObject(); o.put("label", rsTopMember.getString(1)); o.put("nilai", rsTopMember.getDouble(2));
				topMember.put(o);
			}
			rsTopMember.close(); psTopMember.close();

			JSONArray aturanDiskon = new JSONArray();
			java.sql.PreparedStatement psAturan = conn.prepareStatement(
					"SELECT ad.nama_aturan, ad.potongan_langsung, COUNT(det.id) dipakai, COALESCE(SUM(det.diskon+det.cashback),0) totalBiaya "
							+ "FROM koperasi.pembelian det INNER JOIN koperasi.aturan_diskon ad ON det.aturan_diskon = ad.id "
							+ "WHERE det.waktu >= NOW() - INTERVAL '30 days'" + kondisiTokoDet
							+ " GROUP BY ad.id, ad.nama_aturan, ad.potongan_langsung ORDER BY totalBiaya DESC LIMIT 50");
			if (tokoId != null) psAturan.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsAturan = psAturan.executeQuery();
			while (rsAturan.next()) {
				JSONObject o = new JSONObject();
				o.put("namaAturan", rsAturan.getString(1));
				o.put("potonganLangsung", rsAturan.getBoolean(2));
				o.put("dipakai", rsAturan.getLong(3));
				o.put("totalBiaya", rsAturan.getDouble(4));
				aturanDiskon.put(o);
			}
			rsAturan.close(); psAturan.close();

			hasil.put("status", "00");
			hasil.put("diskonDiberikan", diskonDiberikan);
			hasil.put("cashbackDiberikan", cashbackDiberikan);
			hasil.put("cashbackDicairkan", cashbackDicairkan);
			hasil.put("saldoMengendap", saldoMengendap);
			hasil.put("topProduk", topProduk);
			hasil.put("topMember", topMember);
			hasil.put("aturanDiskon", aturanDiskon);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** Cek keberadaan tabel via {@code information_schema} (raw JDBC) -- {@code koperasi.pembatalan_transaksi} dibuat via {@code hbm2ddl=update} saat app start (BUKAN migrasi terjamin dijalankan), jadi mungkin belum ada di environment yg belum pernah start ulang sejak entity itu ditambahkan. Versi JDBC murni dari pola {@code DashboardKepatuhanKantinAction.tabelAda(Session,...)}. */
	private static boolean tabelAda(java.sql.Connection conn, String schema, String tabel) {
		try {
			java.sql.PreparedStatement ps = conn.prepareStatement(
					"SELECT 1 FROM information_schema.tables WHERE table_schema = ? AND table_name = ? LIMIT 1");
			ps.setString(1, schema.toLowerCase());
			ps.setString(2, tabel.toLowerCase());
			java.sql.ResultSet rs = ps.executeQuery();
			boolean ada = rs.next();
			rs.close(); ps.close();
			return ada;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Dasbor "Kepatuhan Operasional" (gap-closure kloning ZK {@code DashboardKepatuhanKantinAction}) --
	 * SEMUA 6 rule direplikasi: Kepatuhan Stok Opname, Sesi Kas Terbuka, Selisih Kas, Diskon Manual
	 * Tanpa Aturan, Selisih Hasil Stok Opname (join {@code koperasi.stok_opname}, kolom {@code selisih}
	 * SUDAH tersimpan langsung -- lihat JavaDoc {@link StokOpname#getSelisih}, dipercaya apa adanya
	 * konsisten dgn dasbor produksi lain), dan Pembatalan Transaksi (tabel {@code koperasi.
	 * pembatalan_transaksi}, DIGERBANG {@link #tabelAda} krn dibuat via {@code hbm2ddl=update} --
	 * environment yg belum pernah restart sejak fitur pembatalan ada akan dapat array kosong +
	 * {@code adaTabelPembatalan=false}, BUKAN error).
	 */
	public static void kepatuhanOperasional(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();

			String kondisiTokoB = tokoId == null ? "" : " AND b.id = ?";
			JSONArray opnameOverdue = new JSONArray();
			long jmlTelatOpname = 0;
			java.sql.PreparedStatement psOpname = conn.prepareStatement(
					"SELECT b.nama, MAX(s.tanggalselesai) terakhir, "
							+ "COALESCE(EXTRACT(DAY FROM NOW() - MAX(s.tanggalselesai)), 99999) hari "
							+ "FROM koperasi.toko b LEFT JOIN koperasi.sesi_stok_opname s ON s.toko = b.id AND s.status = 'SELESAI' "
							+ "WHERE 1=1" + kondisiTokoB + " GROUP BY b.id, b.nama "
							+ "HAVING COALESCE(EXTRACT(DAY FROM NOW() - MAX(s.tanggalselesai)), 99999) >= 30 ORDER BY hari DESC LIMIT 50");
			if (tokoId != null) psOpname.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsOpname = psOpname.executeQuery();
			while (rsOpname.next()) {
				JSONObject o = new JSONObject();
				o.put("toko", rsOpname.getString(1));
				java.sql.Timestamp t = rsOpname.getTimestamp(2);
				o.put("terakhirOpname", t == null ? JSONObject.NULL : t.toString());
				o.put("hariSejak", rsOpname.getDouble(3));
				opnameOverdue.put(o);
				jmlTelatOpname++;
			}
			rsOpname.close(); psOpname.close();

			String kondisiTokoS = tokoId == null ? "" : " AND s.toko = ?";
			JSONArray sesiTerbuka = new JSONArray();
			long jmlSesiLupa = 0;
			java.sql.PreparedStatement psSesi = conn.prepareStatement(
					"SELECT COALESCE(t.nama,'-') toko, s.oleh, s.waktubuka, "
							+ "EXTRACT(EPOCH FROM (NOW() - s.waktubuka))/3600 jam "
							+ "FROM koperasi.sesi_kas_kasir s LEFT JOIN koperasi.toko t ON t.id = s.toko "
							+ "WHERE (s.status = 'BUKA' OR s.status IS NULL)" + kondisiTokoS
							+ " AND EXTRACT(EPOCH FROM (NOW() - s.waktubuka))/3600 >= 24 ORDER BY jam DESC LIMIT 50");
			if (tokoId != null) psSesi.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsSesi = psSesi.executeQuery();
			while (rsSesi.next()) {
				JSONObject o = new JSONObject();
				o.put("toko", rsSesi.getString(1)); o.put("kasir", rsSesi.getString(2));
				java.sql.Timestamp wb = rsSesi.getTimestamp(3);
				o.put("waktuBuka", wb == null ? JSONObject.NULL : wb.toString());
				o.put("jamTerbuka", rsSesi.getDouble(4));
				sesiTerbuka.put(o);
				jmlSesiLupa++;
			}
			rsSesi.close(); psSesi.close();

			String kondisiTokoS2 = tokoId == null ? "" : " AND s.toko = ?";
			JSONArray selisihKas = new JSONArray();
			double totalSelisihKas = 0;
			java.sql.PreparedStatement psSelisih = conn.prepareStatement(
					"SELECT COALESCE(t.nama,'-') toko, s.oleh, s.waktututup, s.selisih "
							+ "FROM koperasi.sesi_kas_kasir s LEFT JOIN koperasi.toko t ON t.id = s.toko "
							+ "WHERE s.status = 'TUTUP' AND s.selisih <> 0 AND s.waktututup >= NOW() - INTERVAL '30 days'" + kondisiTokoS2
							+ " ORDER BY ABS(s.selisih) DESC LIMIT 50");
			if (tokoId != null) psSelisih.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsSelisih = psSelisih.executeQuery();
			while (rsSelisih.next()) {
				JSONObject o = new JSONObject();
				o.put("toko", rsSelisih.getString(1)); o.put("kasir", rsSelisih.getString(2));
				java.sql.Timestamp wt = rsSelisih.getTimestamp(3);
				o.put("waktuTutup", wt == null ? JSONObject.NULL : wt.toString());
				double selisih = rsSelisih.getDouble(4);
				o.put("selisih", selisih);
				selisihKas.put(o);
				totalSelisihKas += Math.abs(selisih);
			}
			rsSelisih.close(); psSelisih.close();

			String kondisiTokoP = tokoId == null ? "" : " AND p.toko = ?";
			JSONArray diskonManual = new JSONArray();
			double totalDiskonManual = 0;
			java.sql.PreparedStatement psDiskon = conn.prepareStatement(
					"SELECT COALESCE(t.nama,'-') toko, p.oleh, COALESCE(SUM(p.diskon),0) total, COUNT(*) jumlah "
							+ "FROM koperasi.pembelian p LEFT JOIN koperasi.toko t ON t.id = p.toko "
							+ "WHERE p.aturan_diskon IS NULL AND p.diskon > 0 AND p.aktif = true AND p.waktu >= NOW() - INTERVAL '30 days'" + kondisiTokoP
							+ " GROUP BY t.nama, p.oleh ORDER BY total DESC LIMIT 50");
			if (tokoId != null) psDiskon.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsDiskon = psDiskon.executeQuery();
			while (rsDiskon.next()) {
				JSONObject o = new JSONObject();
				o.put("toko", rsDiskon.getString(1)); o.put("kasir", rsDiskon.getString(2));
				double total = rsDiskon.getDouble(3);
				o.put("totalDiskon", total); o.put("jumlahTransaksi", rsDiskon.getLong(4));
				diskonManual.put(o);
				totalDiskonManual += total;
			}
			rsDiskon.close(); psDiskon.close();

			String kondisiTokoSo = tokoId == null ? "" : " AND so.toko = ?";
			JSONArray selisihOpname = new JSONArray();
			double totalSelisihOpnameRp = 0;
			java.sql.PreparedStatement psSelisihOpname = conn.prepareStatement(
					"SELECT COALESCE(t.nama,'-') toko, COUNT(*) jumlahBaris, "
							+ "COUNT(*) FILTER (WHERE COALESCE(so.selisih,0) <> 0) jumlahSelisih, "
							+ "COALESCE(SUM(COALESCE(so.selisih,0) * COALESCE(pr.hargabeli,0)),0) nilaiRp "
							+ "FROM koperasi.stok_opname so "
							+ "LEFT JOIN koperasi.toko t ON t.id = so.toko "
							+ "LEFT JOIN koperasi.produk pr ON pr.id = so.produk "
							+ "WHERE so.waktuopname >= NOW() - INTERVAL '30 days'" + kondisiTokoSo
							+ " GROUP BY t.nama HAVING COUNT(*) FILTER (WHERE COALESCE(so.selisih,0) <> 0) > 0 "
							+ "ORDER BY ABS(COALESCE(SUM(COALESCE(so.selisih,0) * COALESCE(pr.hargabeli,0)),0)) DESC LIMIT 50");
			if (tokoId != null) psSelisihOpname.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsSelisihOpname = psSelisihOpname.executeQuery();
			while (rsSelisihOpname.next()) {
				JSONObject o = new JSONObject();
				o.put("toko", rsSelisihOpname.getString(1));
				o.put("jumlahBaris", rsSelisihOpname.getLong(2));
				o.put("jumlahSelisih", rsSelisihOpname.getLong(3));
				double nilai = rsSelisihOpname.getDouble(4);
				o.put("nilaiRupiah", nilai);
				selisihOpname.put(o);
				totalSelisihOpnameRp += Math.abs(nilai);
			}
			rsSelisihOpname.close(); psSelisihOpname.close();

			boolean adaTabelPembatalan = tabelAda(conn, "koperasi", "pembatalan_transaksi");
			JSONArray pembatalanTransaksi = new JSONArray();
			long jmlPembatalan = 0;
			double totalNilaiDibatalkan = 0;
			if (adaTabelPembatalan) {
				String kondisiTokoPb = tokoId == null ? "" : " AND pb.toko = ?";
				java.sql.PreparedStatement psPembatalan = conn.prepareStatement(
						"SELECT COALESCE(t.nama,'-') toko, pb.dibatalkan_oleh, pb.nama_kasir, pb.total_biaya, pb.alasan, pb.tanggal_dibatalkan "
								+ "FROM koperasi.pembatalan_transaksi pb LEFT JOIN koperasi.toko t ON t.id = pb.toko "
								+ "WHERE pb.tanggal_dibatalkan >= NOW() - INTERVAL '30 days'" + kondisiTokoPb
								+ " ORDER BY pb.tanggal_dibatalkan DESC LIMIT 50");
				if (tokoId != null) psPembatalan.setLong(1, tokoId.longValue());
				java.sql.ResultSet rsPembatalan = psPembatalan.executeQuery();
				while (rsPembatalan.next()) {
					JSONObject o = new JSONObject();
					o.put("toko", rsPembatalan.getString(1));
					o.put("dibatalkanOleh", rsPembatalan.getString(2));
					o.put("namaKasir", rsPembatalan.getString(3));
					double total = rsPembatalan.getDouble(4);
					o.put("totalBiaya", total);
					o.put("alasan", rsPembatalan.getString(5));
					java.sql.Timestamp td = rsPembatalan.getTimestamp(6);
					o.put("tanggalDibatalkan", td == null ? JSONObject.NULL : td.toString());
					pembatalanTransaksi.put(o);
					jmlPembatalan++;
					totalNilaiDibatalkan += total;
				}
				rsPembatalan.close(); psPembatalan.close();
			}

			hasil.put("status", "00");
			hasil.put("jmlTelatOpname", jmlTelatOpname);
			hasil.put("jmlSesiLupaTutup", jmlSesiLupa);
			hasil.put("totalSelisihKas", totalSelisihKas);
			hasil.put("totalDiskonManual", totalDiskonManual);
			hasil.put("totalSelisihOpnameRp", totalSelisihOpnameRp);
			hasil.put("jmlPembatalan", jmlPembatalan);
			hasil.put("totalNilaiDibatalkan", totalNilaiDibatalkan);
			hasil.put("adaTabelPembatalan", adaTabelPembatalan);
			hasil.put("opnameOverdue", opnameOverdue);
			hasil.put("sesiTerbuka", sesiTerbuka);
			hasil.put("selisihKas", selisihKas);
			hasil.put("diskonManual", diskonManual);
			hasil.put("selisihOpname", selisihOpname);
			hasil.put("pembatalanTransaksi", pembatalanTransaksi);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Sinkronkan Log Error ke Server" (Desktop/Android/mesin Stok Opname) -- gap-closure
	 * keluhan admin pusat kesulitan memantau error yang terjadi di mesin POS lapangan tanpa akses
	 * fisik ke perangkatnya. Menyimpan tiap baris error klien sbg {@link ais.database.model.ErrorLog}
	 * BARU (tabel {@code public.error_log} yang SAMA dipakai layar admin "Error Log" server) --
	 * SENGAJA field bebas-teks {@code keterangan} disusun menyerupai format exception Java biasa
	 * (baris "Info: ..." di awal) supaya tetap terbaca wajar di layar admin yang sudah ada, tanpa
	 * perlu kolom/skema baru di sisi server. TIDAK ada dedup di sini -- klien (local-db.js
	 * {@code errorLogBelumSinkron}/{@code tandaiErrorLogTersinkron}) yang menjamin satu baris lokal
	 * cuma terkirim SEKALI (ditandai tersinkron setelah {@code status="00"}).
	 *
	 * @param request payload: {@code platform} ("Desktop"/"Android"/"StokOpname"), {@code nama_mesin}
	 *                (opsional, identitas mesin POS pengirim -- lihat JavaDoc {@code lampirkanNamaMesin}
	 *                Desktop main.js), {@code baris} (array {@code {waktu,sumber,tingkat,pesan,detail,layar}}).
	 * @param hasil   diisi {@code status="00"} + {@code tersimpan} (jumlah baris berhasil disimpan).
	 */
	public static void errorLogKirim(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		JSONArray baris = request.has("baris") && !request.isNull("baris") ? request.getJSONArray("baris") : new JSONArray();
		String platform = request.optString("platform", "POS");
		String namaMesin = request.optString("nama_mesin", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			int tersimpan = 0;
			for (int i = 0; i < baris.length(); i++) {
				JSONObject b = baris.optJSONObject(i);
				if (b == null) continue;
				StringBuilder sb = new StringBuilder();
				sb.append("Info: [POS ").append(platform);
				if (!namaMesin.isEmpty()) sb.append(" - ").append(namaMesin);
				sb.append("] ").append(b.optString("sumber", "-")).append("\n");
				sb.append("Tingkat: ").append(b.optString("tingkat", "error")).append("\n");
				sb.append("Layar: ").append(b.optString("layar", "-")).append("\n");
				sb.append("Waktu perangkat: ").append(b.optString("waktu", "-")).append("\n");
				if (tbmuser != null) sb.append("Kasir: ").append(tbmuser.getUserNama() == null ? "" : tbmuser.getUserNama()).append("\n");
				sb.append("\n").append(b.optString("pesan", "")).append("\n");
				String detail = b.optString("detail", "");
				if (!detail.isEmpty()) sb.append("\n").append(detail);

				ais.database.model.ErrorLog log = new ais.database.model.ErrorLog();
				log.setKeterangan(sb.toString());
				session.beginTransaction();
				session.save(log);
				session.getTransaction().commit();
				tersimpan++;
			}
			hasil.put("status", "00");
			hasil.put("tersimpan", tersimpan);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** @return fragmen kondisi waktu SQL siap-pakai utk kolom {@code kolom} (SELALU literal internal, TIDAK PERNAH dari input klien -- aman dari SQL injection) sesuai kode periode dropdown {@link #stokDashboard}, meniru persis pilihan {@code filterPeriode} JSP {@code mutasi_stok.jsp}. */
	private static String stokDashboardKondisiWaktu(String kolom, String periode) {
		if ("today".equals(periode)) return "DATE(" + kolom + ") = CURRENT_DATE";
		if ("week".equals(periode)) return "DATE(" + kolom + ") >= CURRENT_DATE - INTERVAL '7 days'";
		if ("semester".equals(periode)) return "DATE(" + kolom + ") >= CURRENT_DATE - INTERVAL '6 months'";
		if ("year".equals(periode)) return "DATE(" + kolom + ") >= date_trunc('year', CURRENT_DATE)";
		if ("3years".equals(periode)) return "DATE(" + kolom + ") >= CURRENT_DATE - INTERVAL '3 years'";
		return "DATE(" + kolom + ") >= date_trunc('month', CURRENT_DATE)"; // month, default
	}

	/**
	 * <h3>Menu "Produk" -- daftar produk utk pratinjau/cetak Price Tag (POP), gap-closure Desktop/
	 * Android -- padanan JSP {@code barang/pricetag.jsp}.</h3>
	 *
	 * <p>Proksi tipis ke {@link ais.action.master.inventory.PriceTagUtil#listProduk} (SAMA PERSIS
	 * dipakai JSP &amp; ZK -- tak ada logika baru). Tata-letak/label barcode DIBANGUN DI KLIEN (JsBarcode
	 * dari {@code Produk.kode}, sama seperti JSP -- server tidak menyimpan/generate gambar barcode apa
	 * pun), method ini murni menyediakan datanya.</p>
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin, diabaikan utk pedagang/kasir),
	 *                {@code keyword} (opsional, cari nama/kode produk).
	 * @param hasil   diisi {@code status="00"}, {@code data} (array {@code {id, kode, nama, hargaJual}}).
	 */
	public static void priceTagListProduk(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		String keyword = request.optString("keyword", "").trim();

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.util.List<ais.database.model.inventory.Produk> daftar =
					ais.action.master.inventory.PriceTagUtil.listProduk(session, tokoId, keyword);
			JSONArray data = new JSONArray();
			for (ais.database.model.inventory.Produk p : daftar) {
				JSONObject o = new JSONObject();
				o.put("id", p.getId());
				o.put("kode", p.getKode() == null ? "" : p.getKode());
				o.put("nama", p.getNama() == null ? "" : p.getNama());
				o.put("hargaJual", p.getHargaJual() == null ? 0 : p.getHargaJual());
				data.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", data);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Layar Pelanggan (Android) -- status keranjang kasir "disiarkan" sementara di memori server,
	 * TIDAK PERNAH ditulis ke database.</h3>
	 *
	 * <p>Dipakai utk skenario 2-perangkat: perangkat kasir memanggil {@link #layarPelangganKirim}
	 * setiap kali keranjang berubah (debounced di sisi klien), perangkat kedua (dipasang menghadap
	 * pelanggan) memoling {@link #layarPelangganAmbil} tiap 1-2 detik utk menampilkan isi keranjang
	 * secara langsung. Disengaja memakai peta di memori ({@code ConcurrentHashMap}), BUKAN tabel DB --
	 * data ini murni UI real-time tanpa nilai riwayat/audit dan berubah SANGAT sering selama kasir
	 * menambah/mengubah item; menulis tiap perubahan ke database akan membebani tanpa manfaat.</p>
	 *
	 * <p>Kanal disiarkan per-{@code tokoId} (satu toko = satu kanal) -- cukup utk skenario umum satu
	 * meja kasir aktif per toko, sama seperti asumsi layar pelanggan dual-monitor Desktop yang juga
	 * inheren satu-terminal. Kadaluwarsa otomatis via {@link #LAYAR_PELANGGAN_TTL_MS} supaya kalau
	 * kasir menutup app tanpa sempat mengosongkan keranjang (mis. transaksi selesai lalu app ditutup),
	 * layar pelanggan tidak macet menampilkan keranjang basi selamanya -- dianggap "belum ada
	 * aktivitas" dan kembali ke layar sambutan.</p>
	 */
	private static final long LAYAR_PELANGGAN_TTL_MS = 90000L;
	private static final java.util.concurrent.ConcurrentHashMap<Long, JSONObject> layarPelangganState =
			new java.util.concurrent.ConcurrentHashMap<Long, JSONObject>();
	private static final java.util.concurrent.ConcurrentHashMap<Long, Long> layarPelangganWaktu =
			new java.util.concurrent.ConcurrentHashMap<Long, Long>();

	/**
	 * @param request payload: {@code toko_id} (wajib utk admin, diabaikan utk pedagang/kasir),
	 *                {@code items} (array {@code {nama, jumlah, harga, subtotal}}), {@code subtotal},
	 *                {@code diskon}, {@code total}, {@code member_nama} (opsional).
	 */
	public static void layarPelangganKirim(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		JSONObject state = new JSONObject();
		state.put("items", request.isNull("items") ? new JSONArray() : request.getJSONArray("items"));
		state.put("subtotal", request.optDouble("subtotal", 0));
		state.put("diskon", request.optDouble("diskon", 0));
		state.put("total", request.optDouble("total", 0));
		state.put("memberNama", request.optString("member_nama", ""));
		layarPelangganState.put(tokoId, state);
		layarPelangganWaktu.put(tokoId, Long.valueOf(System.currentTimeMillis()));
		hasil.put("status", "00");
	}

	/**
	 * @param request payload: {@code toko_id} (wajib utk admin, diabaikan utk pedagang/kasir).
	 * @param hasil   diisi {@code status="00"}, {@code aktif} (boolean -- false = belum ada siaran
	 *                atau sudah kadaluwarsa, tampilkan layar sambutan), dan jika {@code aktif=true}:
	 *                {@code items}/{@code subtotal}/{@code diskon}/{@code total}/{@code memberNama}.
	 */
	public static void layarPelangganAmbil(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		Long waktu = layarPelangganWaktu.get(tokoId);
		JSONObject state = layarPelangganState.get(tokoId);
		boolean aktif = state != null && waktu != null
				&& (System.currentTimeMillis() - waktu.longValue()) < LAYAR_PELANGGAN_TTL_MS;
		hasil.put("status", "00");
		hasil.put("aktif", aktif);
		if (aktif) {
			hasil.put("items", state.getJSONArray("items"));
			hasil.put("subtotal", state.getDouble("subtotal"));
			hasil.put("diskon", state.getDouble("diskon"));
			hasil.put("total", state.getDouble("total"));
			hasil.put("memberNama", state.optString("memberNama", ""));
		}
	}

	/**
	 * <h3>Menu "Kulakan" (Harga Beli / Pengadaan Produk) -- daftar riwayat barang masuk toko ini.</h3>
	 *
	 * <p>Read-only ringan (raw SQL, bukan entity Hibernate penuh) di atas tabel {@code
	 * koperasi.pengadaan_produk} yang SAMA PERSIS dipakai layar ZK "Pengadaan / Kulakan (Barang Masuk)"
	 * ({@link ais.action.master.inventory.PengadaanKantinAction}) -- Desktop/Android hanya menampilkan
	 * data yang sudah ada, tidak ada skema baru. Pola paginasi+keyword identik {@link #anggotaList}.</p>
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin global, diabaikan utk pedagang/kasir --
	 *                pola IDOR-safe sama dgn {@code soResolveTokoId}), {@code keyword} (opsional, cari
	 *                nama produk/nomor faktur/nama supplier), {@code page}/{@code page_size} (def 1/20,
	 *                maks 100).
	 * @param hasil   diisi {@code status="00"}, {@code data} (array {@code {id, waktuPengadaan,
	 *                nomorFaktur, produkId, namaProduk, namaSupplier, qty, hargaBeliSatuan, totalHarga,
	 *                keterangan}}), {@code total}, {@code page}, {@code pageSize}.
	 */
	public static void kulakanList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		String keyword = request.optString("keyword", "").trim();
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(1, request.optInt("page_size", 20)));
		int offset = (page - 1) * pageSize;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String andKw = keyword.isEmpty() ? ""
					// FIX: kolom fisik di koperasi.pengadaan_produk TANPA underscore
					// (entity PengadaanProduk.nomorFaktur/namaSupplier tak punya @Column
					// eksplisit -> Hibernate default map ke "nomorfaktur"/"namasupplier",
					// sama seperti pg.hargabelisatuan/pg.totalharga di query di bawah).
					: " AND (p.nama ILIKE ? OR COALESCE(pg.nomorfaktur,'') ILIKE ? OR COALESCE(pg.namasupplier,'') ILIKE ?)";
			java.sql.Connection conn = session.connection();

			java.sql.PreparedStatement psCount = conn.prepareStatement(
					"SELECT COUNT(*) FROM koperasi.pengadaan_produk pg JOIN koperasi.produk p ON pg.produk = p.id "
							+ "WHERE pg.toko = ?" + andKw);
			int idxC = 1;
			psCount.setLong(idxC++, tokoId);
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
				psCount.setString(idxC++, kw);
				psCount.setString(idxC++, kw);
				psCount.setString(idxC++, kw);
			}
			java.sql.ResultSet rsCount = psCount.executeQuery();
			long total = rsCount.next() ? rsCount.getLong(1) : 0;
			rsCount.close();
			psCount.close();

			java.sql.PreparedStatement ps = conn.prepareStatement(
					"SELECT pg.id, pg.waktupengadaan, COALESCE(pg.nomorfaktur,''), pg.produk, p.nama, "
							+ "COALESCE(pg.namasupplier,''), pg.qty, pg.hargabelisatuan, pg.totalharga, COALESCE(pg.keterangan,'') "
							+ "FROM koperasi.pengadaan_produk pg JOIN koperasi.produk p ON pg.produk = p.id "
							+ "WHERE pg.toko = ?" + andKw + " ORDER BY pg.waktupengadaan DESC, pg.id DESC LIMIT ? OFFSET ?");
			int idx = 1;
			ps.setLong(idx++, tokoId);
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
				ps.setString(idx++, kw);
				ps.setString(idx++, kw);
				ps.setString(idx++, kw);
			}
			ps.setInt(idx++, pageSize);
			ps.setInt(idx++, offset);
			java.sql.ResultSet rs = ps.executeQuery();
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				java.sql.Timestamp wp = rs.getTimestamp(2);
				j.put("waktuPengadaan", wp == null ? "" : fmt.format(wp));
				j.put("nomorFaktur", rs.getString(3));
				j.put("produkId", rs.getLong(4));
				j.put("namaProduk", rs.getString(5));
				j.put("namaSupplier", rs.getString(6));
				j.put("qty", rs.getDouble(7));
				j.put("hargaBeliSatuan", rs.getDouble(8));
				j.put("totalHarga", rs.getDouble(9));
				j.put("keterangan", rs.getString(10));
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("pageSize", pageSize);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Menu "Kulakan" (Harga Beli / Pengadaan Produk) -- catat SATU barang masuk dari pemasok.</h3>
	 *
	 * <p>Stok &amp; harga beli produk otomatis di-recompute lewat {@link
	 * ais.action.master.inventory.StokKantinUtil#recomputeStokProduk} -- rumus IDENTIK dgn layar ZK
	 * "Pengadaan / Kulakan (Barang Masuk)" ({@link ais.action.master.inventory.PengadaanKantinAction}),
	 * tidak ada logika hitung baru. Gated supervisor, pola sama dgn {@link #produkSimpan}/{@link
	 * #diskonSimpan}/{@link #soSimpan}.</p>
	 *
	 * @param request payload: {@code produk_id} (wajib), {@code nomor_faktur}/{@code nama_supplier}/
	 *                {@code keterangan} (opsional), {@code qty} (wajib, &gt;0), {@code harga_beli_satuan}
	 *                (wajib, &gt;0).
	 * @param hasil   diisi {@code status="00"}, {@code id} (id baris {@link PengadaanProduk} tersimpan).
	 */
	public static void kulakanSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilKulakan = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalKulakan = pemanggilKulakan == null;
		boolean supervisorKulakan = pemanggilKulakan != null && Boolean.TRUE.equals(pemanggilKulakan.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggilKulakan, adminGlobalKulakan, supervisorKulakan, "kulakan", "create")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mencatat Kulakan (Harga Beli).");
			return;
		}
		Long produkId = request.isNull("produk_id") ? null : Long.valueOf((request.get("produk_id") + "").trim());
		if (produkId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Produk belum dipilih.");
			return;
		}
		double qty = request.optDouble("qty", 0);
		double hargaBeliSatuan = request.optDouble("harga_beli_satuan", 0);
		if (qty <= 0) {
			hasil.put("status", "91");
			hasil.put("description", "Jumlah masuk harus lebih dari 0.");
			return;
		}
		if (hargaBeliSatuan <= 0) {
			hasil.put("status", "91");
			hasil.put("description", "Harga beli satuan harus lebih dari 0.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Produk produk = (Produk) session.get(Produk.class, produkId);
			if (produk == null) {
				hasil.put("status", "91");
				hasil.put("description", "Produk tidak ditemukan.");
				return;
			}
			if (supervisorKulakan) {
				Long tokoSupervisor = pemanggilKulakan.getToko() == null ? null : pemanggilKulakan.getToko().getId();
				Long tokoProduk = produk.getToko() == null ? null : produk.getToko().getId();
				if (tokoSupervisor == null || !tokoSupervisor.equals(tokoProduk)) {
					hasil.put("status", "91");
					hasil.put("description", "Produk ini bukan milik toko Anda.");
					return;
				}
			}

			PengadaanProduk pg = new PengadaanProduk();
			pg.setProduk(produk);
			pg.setToko(produk.getToko());
			pg.setNomorFaktur(request.optString("nomor_faktur", ""));
			pg.setNamaSupplier(request.optString("nama_supplier", ""));
			pg.setQty(qty);
			pg.setHargaBeliSatuan(hargaBeliSatuan);
			pg.setTotalHarga(qty * hargaBeliSatuan);
			pg.setWaktuPengadaan(new Date());
			pg.setKeterangan(request.optString("keterangan", ""));
			pg.setOleh(tbmuser == null ? "kulakan" : tbmuser.getUserId());

			session.beginTransaction();
			session.save(pg);
			ais.action.master.inventory.StokKantinUtil.recomputeStokProduk(produkId);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", pg.getId());
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:kulakanSimpan-rollback");
			}
			hasil.put("status", "91");
			hasil.put("description", Common.tampilErrorJikaAdmin(e));
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Menu "Retur Penjualan" -- riwayat barang yang dikembalikan pelanggan (paginated, per toko).</h3>
	 * Pola query SAMA PERSIS {@link #kulakanList} (join produk, filter keyword nama produk/kode
	 * transaksi asal/nama pembeli, urut waktu terbaru dulu).
	 */
	public static void returPenjualanList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		String keyword = request.optString("keyword", "").trim();
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(1, request.optInt("page_size", 20)));
		int offset = (page - 1) * pageSize;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			// Kolom rp.kodetransaksiasal/namapembeli/hargasatuan/totalnilai/kondisibarang/metodepengembalian
			// SENGAJA tanpa underscore -- entity ReturPenjualan.java tidak punya @Column(name=...) eksplisit
			// utk field ini, jadi Hibernate membuat kolom fisik dgn nama default (lowercase tanpa
			// pemisah), BUKAN snake_case spt kolom lain yg memang di-@Column eksplisit (mis.
			// kembalikan_ke_stok). String SQL mentah ini WAJIB ikut penamaan fisik yg SUNGGUHAN ada
			// di database, bukan konvensi snake_case yg dipakai di tempat lain -- lihat riwayat error
			// "column rp.kode_transaksi_asal does not exist".
			String andKw = keyword.isEmpty() ? ""
					: " AND (p.nama ILIKE ? OR COALESCE(rp.kodetransaksiasal,'') ILIKE ? OR COALESCE(rp.namapembeli,'') ILIKE ?)";
			java.sql.Connection conn = session.connection();

			java.sql.PreparedStatement psCount = conn.prepareStatement(
					"SELECT COUNT(*) FROM koperasi.retur_penjualan rp JOIN koperasi.produk p ON rp.produk = p.id "
							+ "WHERE rp.toko = ?" + andKw);
			int idxC = 1;
			psCount.setLong(idxC++, tokoId);
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
				psCount.setString(idxC++, kw);
				psCount.setString(idxC++, kw);
				psCount.setString(idxC++, kw);
			}
			java.sql.ResultSet rsCount = psCount.executeQuery();
			long total = rsCount.next() ? rsCount.getLong(1) : 0;
			rsCount.close();
			psCount.close();

			java.sql.PreparedStatement ps = conn.prepareStatement(
					"SELECT rp.id, rp.waktu, COALESCE(rp.kodetransaksiasal,''), rp.produk, p.nama, "
							+ "COALESCE(rp.namapembeli,''), rp.qty, rp.hargasatuan, rp.totalnilai, "
							+ "COALESCE(rp.alasan,''), COALESCE(rp.kondisibarang,''), rp.kembalikan_ke_stok, "
							+ "COALESCE(rp.metodepengembalian,''), COALESCE(rp.keterangan,''), COALESCE(rp.oleh,'') "
							+ "FROM koperasi.retur_penjualan rp JOIN koperasi.produk p ON rp.produk = p.id "
							+ "WHERE rp.toko = ?" + andKw + " ORDER BY rp.waktu DESC, rp.id DESC LIMIT ? OFFSET ?");
			int idx = 1;
			ps.setLong(idx++, tokoId);
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
				ps.setString(idx++, kw);
				ps.setString(idx++, kw);
				ps.setString(idx++, kw);
			}
			ps.setInt(idx++, pageSize);
			ps.setInt(idx++, offset);
			java.sql.ResultSet rs = ps.executeQuery();
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				java.sql.Timestamp w = rs.getTimestamp(2);
				j.put("waktu", w == null ? "" : fmt.format(w));
				j.put("kodeTransaksiAsal", rs.getString(3));
				j.put("produkId", rs.getLong(4));
				j.put("namaProduk", rs.getString(5));
				j.put("namaPembeli", rs.getString(6));
				j.put("qty", rs.getDouble(7));
				j.put("hargaSatuan", rs.getDouble(8));
				j.put("totalNilai", rs.getDouble(9));
				j.put("alasan", rs.getString(10));
				j.put("kondisiBarang", rs.getString(11));
				j.put("kembalikanKeStok", rs.getBoolean(12));
				j.put("metodePengembalian", rs.getString(13));
				j.put("keterangan", rs.getString(14));
				j.put("oleh", rs.getString(15));
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("pageSize", pageSize);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Menu "Retur Penjualan" -- catat satu atau lebih produk yang dikembalikan pelanggan dari SATU
	 * transaksi penjualan asal.</h3>
	 *
	 * <p>Gated Supervisor -- SAMA pola gerbangnya dgn {@link #kulakanSimpan} (per keputusan eksplisit:
	 * tambah/ubah/hapus retur adalah tindakan yang bisa mengoreksi stok &amp; nilai penjualan, jadi
	 * disamakan dgn Kulakan, BUKAN dibiarkan bebas spt draft awal fitur ini). Kasir non-supervisor
	 * tetap boleh MELIHAT riwayat retur ({@link #returPenjualanList}), hanya tidak boleh menulis.</p>
	 *
	 * <p>Stok bertambah HANYA utk baris yang {@code kembalikan_ke_stok=true} (lihat JavaDoc
	 * {@link ReturPenjualan#getKembalikanKeStok()}) -- {@link
	 * ais.action.master.inventory.StokKantinUtil#recomputeStokProduk} tetap dipanggil utk SEMUA baris
	 * (aman/idempoten walau kembalikan_ke_stok=false, krn formula SQL-nya sendiri yang menyaring).</p>
	 *
	 * @param request payload: {@code pembelian_anggota_koperasi_id} (opsional -- id transaksi asal dari
	 *                {@code detail_transaksi}/{@code laporan_order_list}), {@code kode_transaksi_asal}
	 *                (opsional, nomor nota utk tampilan), {@code nama_pembeli}/{@code id_anggota_koperasi}
	 *                (opsional), {@code metode_pengembalian} (opsional), {@code items} (wajib, array
	 *                {@code {produk_id, qty, harga_satuan, alasan, kondisi_barang, kembalikan_ke_stok,
	 *                keterangan}} -- minimal satu baris).
	 * @param hasil   diisi {@code status="00"}, {@code ids} (array id baris {@link ReturPenjualan}
	 *                tersimpan), {@code totalNilaiRetur}.
	 */
	public static void returPenjualanSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilRp = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalRp = pemanggilRp == null;
		boolean supervisorRp = pemanggilRp != null && Boolean.TRUE.equals(pemanggilRp.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggilRp, adminGlobalRp, supervisorRp, "returpenjualan", "create")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mencatat Retur Penjualan.");
			return;
		}

		JSONArray items = request.optJSONArray("items");
		if (items == null || items.length() == 0) {
			hasil.put("status", "91");
			hasil.put("description", "Belum ada barang yang dipilih untuk diretur.");
			return;
		}
		Long pembelianAnggotaKoperasiId = request.isNull("pembelian_anggota_koperasi_id") ? null
				: Long.valueOf((request.get("pembelian_anggota_koperasi_id") + "").trim());
		String kodeTransaksiAsal = request.optString("kode_transaksi_asal", "");
		String namaPembeli = request.optString("nama_pembeli", "");
		String metodePengembalian = request.optString("metode_pengembalian", "");
		Long idAnggota = request.isNull("id_anggota_koperasi") ? null
				: Long.valueOf((request.get("id_anggota_koperasi") + "").trim());

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			AnggotaKoperasi anggota = idAnggota == null ? null : (AnggotaKoperasi) session.get(AnggotaKoperasi.class, idAnggota);

			session.beginTransaction();
			JSONArray ids = new JSONArray();
			double totalNilaiRetur = 0;
			for (int i = 0; i < items.length(); i++) {
				JSONObject it = items.getJSONObject(i);
				Long produkId = it.isNull("produk_id") ? null : Long.valueOf((it.get("produk_id") + "").trim());
				if (produkId == null) {
					throw new IllegalArgumentException("Produk baris ke-" + (i + 1) + " belum dipilih.");
				}
				double qty = it.optDouble("qty", 0);
				if (qty <= 0) {
					throw new IllegalArgumentException("Jumlah retur baris ke-" + (i + 1) + " harus lebih dari 0.");
				}
				double hargaSatuan = it.optDouble("harga_satuan", 0);

				Produk produk = (Produk) session.get(Produk.class, produkId);
				if (produk == null) {
					throw new IllegalArgumentException("Produk baris ke-" + (i + 1) + " tidak ditemukan.");
				}
				Long tokoProduk = produk.getToko() == null ? null : produk.getToko().getId();
				if (!adminGlobalRp) {
					Long tokoPedagang = pemanggilRp.getToko() == null ? null : pemanggilRp.getToko().getId();
					if (tokoPedagang == null || !tokoPedagang.equals(tokoProduk)) {
						throw new IllegalArgumentException("Produk baris ke-" + (i + 1) + " bukan milik toko Anda.");
					}
				}

				ReturPenjualan rp = new ReturPenjualan();
				rp.setProduk(produk);
				rp.setToko(produk.getToko());
				rp.setPembelianAnggotaKoperasiId(pembelianAnggotaKoperasiId);
				rp.setKodeTransaksiAsal(kodeTransaksiAsal);
				rp.setAnggotaKoperasi(anggota);
				rp.setNamaPembeli(namaPembeli);
				rp.setQty(qty);
				rp.setHargaSatuan(hargaSatuan);
				rp.setTotalNilai(qty * hargaSatuan);
				rp.setAlasan(it.optString("alasan", ""));
				rp.setKondisiBarang(it.optString("kondisi_barang", ""));
				rp.setKembalikanKeStok(it.optBoolean("kembalikan_ke_stok", true));
				rp.setMetodePengembalian(metodePengembalian);
				rp.setKeterangan(it.optString("keterangan", ""));
				rp.setWaktu(new Date());
				rp.setOleh(tbmuser == null ? "retur_penjualan" : tbmuser.getUserId());

				session.save(rp);
				ais.action.master.inventory.StokKantinUtil.recomputeStokProduk(produkId);

				ids.put(rp.getId());
				totalNilaiRetur += rp.getTotalNilai();
			}
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("ids", ids);
			hasil.put("totalNilaiRetur", totalNilaiRetur);
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:returPenjualanSimpan-rollback");
			}
			hasil.put("status", "91");
			hasil.put("description", Common.tampilErrorJikaAdmin(e));
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Menu "Retur Penjualan" -- ubah SATU baris retur yang sudah tersimpan.</h3>
	 *
	 * <p>Gated Supervisor, pola sama {@link #returPenjualanSimpan}. Produk baris TIDAK bisa diganti
	 * lewat aksi ini (retur selalu menempel ke produk aslinya) -- yang bisa diubah: qty, harga satuan,
	 * alasan, kondisi barang, kembalikan-ke-stok, metode pengembalian, keterangan. Stok direcompute
	 * ULANG utk produk baris ini setelah ubah (aman walau qty/kembalikan_ke_stok tidak berubah).</p>
	 *
	 * @param request payload: {@code id} (wajib, id baris {@link ReturPenjualan}), {@code qty}/
	 *                {@code harga_satuan}/{@code alasan}/{@code kondisi_barang}/
	 *                {@code kembalikan_ke_stok}/{@code metode_pengembalian}/{@code keterangan} (semua
	 *                opsional -- field yang tidak dikirim tetap memakai nilai lama).
	 * @param hasil   diisi {@code status="00"} bila berhasil.
	 */
	public static void returPenjualanUbah(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilRp = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalRp = pemanggilRp == null;
		boolean supervisorRp = pemanggilRp != null && Boolean.TRUE.equals(pemanggilRp.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggilRp, adminGlobalRp, supervisorRp, "returpenjualan", "update")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengubah Retur Penjualan.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "ID retur wajib diisi.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ReturPenjualan rp = (ReturPenjualan) session.get(ReturPenjualan.class, id);
			if (rp == null) {
				hasil.put("status", "91");
				hasil.put("description", "Data retur tidak ditemukan.");
				return;
			}
			if (!adminGlobalRp) {
				Long tokoPedagang = pemanggilRp.getToko() == null ? null : pemanggilRp.getToko().getId();
				Long tokoRetur = rp.getToko() == null ? null : rp.getToko().getId();
				if (tokoPedagang == null || !tokoPedagang.equals(tokoRetur)) {
					hasil.put("status", "91");
					hasil.put("description", "Retur ini bukan milik toko Anda.");
					return;
				}
			}

			session.beginTransaction();
			if (!request.isNull("qty")) rp.setQty(request.optDouble("qty", rp.getQty()));
			if (!request.isNull("harga_satuan")) rp.setHargaSatuan(request.optDouble("harga_satuan", rp.getHargaSatuan()));
			rp.setTotalNilai(rp.getQty() * rp.getHargaSatuan());
			if (!request.isNull("alasan")) rp.setAlasan(request.optString("alasan", rp.getAlasan()));
			if (!request.isNull("kondisi_barang")) rp.setKondisiBarang(request.optString("kondisi_barang", rp.getKondisiBarang()));
			if (!request.isNull("kembalikan_ke_stok")) rp.setKembalikanKeStok(request.optBoolean("kembalikan_ke_stok", Boolean.TRUE.equals(rp.getKembalikanKeStok())));
			if (!request.isNull("metode_pengembalian")) rp.setMetodePengembalian(request.optString("metode_pengembalian", rp.getMetodePengembalian()));
			if (!request.isNull("keterangan")) rp.setKeterangan(request.optString("keterangan", rp.getKeterangan()));

			session.update(rp);
			Long produkId = rp.getProduk() == null ? null : rp.getProduk().getId();
			session.getTransaction().commit();
			ais.action.master.inventory.StokKantinUtil.recomputeStokProduk(produkId);

			hasil.put("status", "00");
			hasil.put("description", "Retur berhasil diperbarui.");
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:returPenjualanUbah-rollback");
			}
			hasil.put("status", "91");
			hasil.put("description", Common.tampilErrorJikaAdmin(e));
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Menu "Retur Penjualan" -- hapus SATU baris retur (mis. salah entri).</h3>
	 *
	 * <p>Gated Supervisor, pola sama {@link #returPenjualanSimpan}/{@link #returPenjualanUbah}. Stok
	 * produk yang bersangkutan direcompute ULANG setelah baris dihapus -- kalau baris yang dihapus tadi
	 * {@code kembalikan_ke_stok=true}, stok otomatis TURUN kembali sejumlah qty-nya (formula SUM di
	 * {@code StokKantinUtil} tidak lagi menghitung baris yang sudah tidak ada).</p>
	 *
	 * @param request payload: {@code id} (wajib, id baris {@link ReturPenjualan}).
	 * @param hasil   diisi {@code status="00"} bila berhasil.
	 */
	public static void returPenjualanHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilRp = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalRp = pemanggilRp == null;
		boolean supervisorRp = pemanggilRp != null && Boolean.TRUE.equals(pemanggilRp.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggilRp, adminGlobalRp, supervisorRp, "returpenjualan", "delete")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat menghapus Retur Penjualan.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "ID retur wajib diisi.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ReturPenjualan rp = (ReturPenjualan) session.get(ReturPenjualan.class, id);
			if (rp == null) {
				hasil.put("status", "91");
				hasil.put("description", "Data retur tidak ditemukan.");
				return;
			}
			if (!adminGlobalRp) {
				Long tokoPedagang = pemanggilRp.getToko() == null ? null : pemanggilRp.getToko().getId();
				Long tokoRetur = rp.getToko() == null ? null : rp.getToko().getId();
				if (tokoPedagang == null || !tokoPedagang.equals(tokoRetur)) {
					hasil.put("status", "91");
					hasil.put("description", "Retur ini bukan milik toko Anda.");
					return;
				}
			}

			Long produkId = rp.getProduk() == null ? null : rp.getProduk().getId();
			session.beginTransaction();
			session.delete(rp);
			session.getTransaction().commit();
			ais.action.master.inventory.StokKantinUtil.recomputeStokProduk(produkId);

			hasil.put("status", "00");
			hasil.put("description", "Retur berhasil dihapus.");
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:returPenjualanHapus-rollback");
			}
			hasil.put("status", "91");
			hasil.put("description", Common.tampilErrorJikaAdmin(e));
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Menu "Ringkasan" (Riwayat Transaksi) -- batalkan SATU transaksi penjualan yang sudah dibayar.</h3>
	 *
	 * <p>Gap-closure Desktop/Android: JSP e-Kantin SUDAH punya fitur ini sejak lama (tombol "Batal" di
	 * {@code _riwayat_transaksi_terbaru.jsp}, endpoint {@code riwayat_transaksi_service.jsp}) -- method
	 * ini MEMAKAI ULANG PERSIS logika &amp; util yang sama ({@link PembatalanTransaksiUtil#batalkan},
	 * gerbang Supervisor, alasan wajib, blokir bila sudah diposting jurnal, kunci toko), supaya perilaku
	 * (termasuk arsip {@code koperasi.pembatalan_transaksi}) identik di ketiga permukaan (JSP/Desktop/
	 * Android) -- BUKAN reimplementasi/tabel baru.</p>
	 *
	 * <p><b>Desain arsip HARD-DELETE + snapshot teks</b> (bukan flag nonaktif) -- konsisten dgn
	 * {@link PembatalanTransaksiUtil}: baris {@code pembelian_anggota_koperasi}/{@code pembelian} asli
	 * dihapus permanen, rincian item disimpan sbg teks di {@code PembatalanTransaksiKantin.rincian}.
	 * BELUM ada mekanisme "restore" (kembalikan transaksi yang sudah dibatalkan) -- kalau nanti
	 * dibutuhkan, arsip ini sudah punya semua data mentah (rincian, total, toko, anggota) utk direkonstruksi
	 * manual, tapi otomatisasinya belum ada.</p>
	 *
	 * @param request payload: {@code id} (wajib, id {@code PembelianAnggotaKoperasi} ATAU baris
	 *                {@code pembelian} berdiri sendiri legacy), {@code alasan} (wajib).
	 * @param hasil   diisi {@code status="00"} bila berhasil.
	 */
	public static void batalkanTransaksi(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (tbmuser == null) {
			hasil.put("status", "91");
			hasil.put("description", "Sesi tidak valid.");
			return;
		}
		ais.database.model.inventory.Pedagang pedagang = tbmuser.getPedagang();
		ais.database.model.inventory.Toko tokoLogin = pedagang == null ? null : pedagang.getToko();
		boolean bolehSupervisor = pedagang == null || Boolean.TRUE.equals(pedagang.getSupervisor());
		if (!bolehSupervisor) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya supervisor/admin yang boleh membatalkan transaksi.");
			return;
		}
		String alasan = request.optString("alasan", "").trim();
		if (alasan.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Alasan pembatalan wajib diisi.");
			return;
		}
		Long idTransaksi = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		if (idTransaksi == null) {
			hasil.put("status", "91");
			hasil.put("description", "ID transaksi wajib diisi.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		org.hibernate.Transaction tx = null;
		try {
			PembelianAnggotaKoperasi trx = (PembelianAnggotaKoperasi) session.get(PembelianAnggotaKoperasi.class, idTransaksi);

			if (trx == null) {
				// Baris pembelian LEGACY tanpa header (lihat catatan sama di PosApi.prosesDetailTransaksi)
				// -- tidak punya rincian/jurnal utk diarsipkan lewat PembatalanTransaksiUtil, cukup dihapus
				// langsung, pola SAMA PERSIS riwayat_transaksi_service.jsp.
				ais.database.model.inventory.Pembelian legacy = (ais.database.model.inventory.Pembelian) session
						.createCriteria(ais.database.model.inventory.Pembelian.class)
						.add(Restrictions.idEq(idTransaksi))
						.add(Restrictions.isNull("pembelianAnggotaKoperasi"))
						.uniqueResult();
				if (legacy == null) {
					hasil.put("status", "91");
					hasil.put("description", "Transaksi tidak ditemukan.");
					return;
				}
				if (tokoLogin != null && (legacy.getToko() == null || legacy.getToko().getId() == null
						|| !tokoLogin.getId().equals(legacy.getToko().getId()))) {
					hasil.put("status", "91");
					hasil.put("description", "Transaksi ini bukan milik toko yang sedang login.");
					return;
				}
				tx = session.beginTransaction();
				session.delete(legacy);
				tx.commit();
				hasil.put("status", "00");
				hasil.put("description", "Transaksi lama tanpa header berhasil dibatalkan.");
				return;
			}

			if (tokoLogin != null && (trx.getToko() == null || trx.getToko().getId() == null
					|| !tokoLogin.getId().equals(trx.getToko().getId()))) {
				hasil.put("status", "91");
				hasil.put("description", "Transaksi ini bukan milik toko yang sedang login.");
				return;
			}
			if (trx.getPostingHistory() != null) {
				hasil.put("status", "91");
				hasil.put("description", "Transaksi sudah diposting ke jurnal, tidak bisa dibatalkan dari menu ini.");
				return;
			}

			tx = session.beginTransaction();
			ais.action.master.koperasi.helper.PembatalanTransaksiUtil.batalkan(session, trx, alasan);
			tx.commit();

			hasil.put("status", "00");
			hasil.put("description", "Transaksi berhasil dibatalkan dan tercatat di arsip pembatalan.");
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:batalkanTransaksi-rollback");
			}
			hasil.put("status", "91");
			hasil.put("description", Common.tampilErrorJikaAdmin(e));
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Kasir (Desktop/Android) -- evaluasi Aturan Diskon otomatis utk isi keranjang saat ini.</h3>
	 *
	 * <p>MURNI MENGHITUNG, tidak menyimpan apa pun -- Desktop/Android dipanggil ulang setiap kali
	 * keranjang berubah (tambah/qty/hapus/pilih member/pilih toko), lalu memasukkan angka {@code
	 * diskon}/{@code cashback}/{@code aturanDiskon} hasilnya ke payload {@code transaksi[]} yang SAMA
	 * dikirim ke aksi {@code bayar} -- {@link #bayar} TIDAK menghitung ulang diskon, ia hanya
	 * menjumlahkan angka yang sudah dikirim klien lewat {@link #hitungTotalDiskonCashback} (SAMA
	 * perilakunya dgn JSP/ZK selama ini, lihat catatan di method itu).</p>
	 *
	 * <p>Logika PORTING 1:1 dari mesin evaluasi client-side yang SUDAH ADA -- JSP {@code _pos.jsp}
	 * (fungsi {@code evaluateDiscount}/{@code recalculateCart}/{@code loadAturanDiskon}/{@code
	 * updateUsageDiskonMember}) dan ZK ({@code PosKantinAction.evaluasiDiskon}) -- BUKAN logika baru:
	 * cocokkan aturan PERTAMA per baris (produk WAJIB cocok -- kolom {@code aturan_diskon.produk}
	 * {@code NOT NULL}, toko cocok/berlaku-semua-toko, jendela tanggal berlaku, target member
	 * semua/jenis/tipe), hitung persentase ATAU nominal (persentase diprioritaskan, nominal dikali
	 * qty lalu dibatasi maks total baris), lalu terapkan batas maksimal potongan -- KUMULATIF
	 * per-hari-per-toko bila aturan itu {@code berlakuPerHariDanPerToko} (dijumlah dari transaksi hari
	 * ini yg SUDAH tersimpan di server + akumulasi antar baris DALAM SATU pemanggilan ini), atau
	 * per-baris biasa bila tidak.</p>
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin global, diabaikan utk pedagang/kasir),
	 *                {@code id_member} (opsional -- tanpa ini, aturan yg menyasar member spesifik
	 *                otomatis tidak berlaku, sama spt kasir belum pilih member di JSP/ZK), {@code
	 *                items} (array {@code {id, harga, jumlah}} -- id = id {@link Produk}, harga =
	 *                harga satuan yg dipakai di keranjang saat ini, jumlah = qty).
	 * @param hasil   diisi {@code status="00"}, {@code items} (array SEJAJAR urutan input, masing2
	 *                {@code {id, diskon, cashback, aturanDiskon, berlakuPerHariDanPerToko}}).
	 */
	public static void diskonEvaluasi(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		Long memberId = request.isNull("id_member") ? null : Long.valueOf((request.get("id_member") + "").trim());
		JSONArray items = request.optJSONArray("items");
		if (items == null) {
			items = new JSONArray();
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			JSONArray outArr = evaluasiDiskonItems(conn, tokoId, memberId, items);
			hasil.put("status", "00");
			hasil.put("items", outArr);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>"Hitung Ulang" (Pesanan) -- gap-closure Desktop/Android, padanan tombol "Hitung Ulang" di JSP
	 * {@code _draft_pesanan_anggota.jsp} (fungsi {@code hitungUlangDiskon}).</h3>
	 *
	 * <p>BEDA dari {@link #diskonEvaluasi} (murni menghitung utk keranjang yg BELUM disimpan): method
	 * ini menghitung ULANG diskon/cashback satu pesanan yang SUDAH ADA (draft ATAU sudah lunas) memakai
	 * aturan diskon TERKINI (mis. dipakai saat aturan diskon berubah setelah pesanan dibuat, atau
	 * pesanan sempat tersimpan dgn nilai salah krn bug), lalu MENYIMPAN hasilnya -- ke baris
	 * {@code koperasi.draft_pembelian} + header {@code draft_pembelian_anggota_koperasi} SELALU, dan
	 * bila pesanan SUDAH LUNAS ({@code draft.getLunas() != null}), JUGA mencerminkan angka yang sama ke
	 * baris {@code koperasi.pembelian} (dicocokkan ke baris draft per PRODUK, pola SAMA PERSIS dgn JSP)
	 * + header {@code pembelian_anggota_koperasi} -- mengoreksi transaksi yang SUDAH FINAL.</p>
	 *
	 * <p>Gerbang: admin global ATAU supervisor toko yang SAMA dgn toko pesanan ini (tindakan koreksi
	 * finansial, bukan aksi jual-beli rutin) -- SAMA pola gerbangnya dgn {@link #diskonSimpan}/
	 * {@link #soSimpan}.</p>
	 *
	 * @param request payload: {@code draft_id} (wajib, id {@link DraftPembelianAnggotaKoperasi}).
	 * @param hasil   diisi {@code status="00"}, {@code totalDiskon}, {@code totalCashback},
	 *                {@code totalBiaya}, {@code lunasDiperbarui} (boolean -- true bila baris
	 *                {@code pembelian} juga ikut dikoreksi).
	 */
	public static void pesananHitungUlang(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilPh = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalPh = pemanggilPh == null;
		boolean supervisorPh = pemanggilPh != null && Boolean.TRUE.equals(pemanggilPh.getSupervisor());
		if (!adminGlobalPh && !supervisorPh) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat menghitung ulang pesanan.");
			return;
		}
		Long draftId = request.isNull("draft_id") ? null : Long.valueOf((request.get("draft_id") + "").trim());
		if (draftId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Pesanan tidak diketahui.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			DraftPembelianAnggotaKoperasi d = (DraftPembelianAnggotaKoperasi) session
					.get(DraftPembelianAnggotaKoperasi.class, draftId);
			if (d == null) {
				hasil.put("status", "91");
				hasil.put("description", "Pesanan tidak ditemukan.");
				return;
			}
			Long tokoId = d.getToko() == null ? null : d.getToko().getId();
			if (!adminGlobalPh) {
				Long tokoSendiri = pemanggilPh.getToko() == null ? null : pemanggilPh.getToko().getId();
				if (tokoSendiri == null || tokoId == null || !tokoSendiri.equals(tokoId)) {
					hasil.put("status", "91");
					hasil.put("description", "Pesanan ini bukan milik toko Anda.");
					return;
				}
			}
			Long memberId = d.getAnggotaKoperasi() == null ? null : d.getAnggotaKoperasi().getId();

			@SuppressWarnings("unchecked")
			java.util.List<ais.database.model.inventory.DraftPembelian> itemRows = session
					.createCriteria(ais.database.model.inventory.DraftPembelian.class)
					.add(Restrictions.eq("draftPembelianAnggotaKoperasi", d)).list();

			JSONArray itemsPayload = new JSONArray();
			for (ais.database.model.inventory.DraftPembelian dp : itemRows) {
				JSONObject it = new JSONObject();
				it.put("id", dp.getProduk() == null ? JSONObject.NULL : dp.getProduk().getId());
				it.put("harga", dp.getHargaSatuan() == null ? 0 : dp.getHargaSatuan());
				it.put("jumlah", dp.getQty() == null ? 0 : dp.getQty());
				itemsPayload.put(it);
			}

			session.beginTransaction();
			java.sql.Connection conn = session.connection();
			JSONArray hasilEvaluasi = evaluasiDiskonItems(conn, tokoId, memberId, itemsPayload);

			// produkId -> {diskon, cashback, aturanDiskonId(-1 bila null)} -- dipakai ULANG utk
			// mencerminkan ke baris "pembelian" (sudah lunas) di bawah, dicocokkan per PRODUK (SAMA
			// PERSIS pola JSP) tanpa perlu menghitung ulang kedua kalinya utk sisi itu.
			java.util.Map<Long, double[]> hasilPerProduk = new java.util.HashMap<Long, double[]>();

			double totalDiskon = 0, totalCashback = 0, totalBiaya = 0;
			for (int i = 0; i < itemRows.size(); i++) {
				ais.database.model.inventory.DraftPembelian dp = itemRows.get(i);
				JSONObject satu = hasilEvaluasi.getJSONObject(i);
				double diskon = satu.optDouble("diskon", 0);
				double cashback = satu.optDouble("cashback", 0);
				Long aturanId = satu.isNull("aturanDiskon") ? null : Long.valueOf(satu.optLong("aturanDiskon"));

				dp.setDiskon(Double.valueOf(diskon));
				dp.setCashback(Double.valueOf(cashback));
				dp.setAturanDiskon(aturanId == null ? null
						: (ais.database.model.koperasi.AturanDiskon) session
								.get(ais.database.model.koperasi.AturanDiskon.class, aturanId));
				double subtotal = (dp.getHargaSatuan() == null ? 0 : dp.getHargaSatuan().doubleValue())
						* (dp.getQty() == null ? 0 : dp.getQty().doubleValue());
				dp.setTotal(Double.valueOf(subtotal - diskon));
				session.update(dp);

				totalDiskon += diskon;
				totalCashback += cashback;
				totalBiaya += (subtotal - diskon);
				if (dp.getProduk() != null) {
					hasilPerProduk.put(dp.getProduk().getId(),
							new double[] { diskon, cashback, aturanId == null ? -1 : aturanId.doubleValue() });
				}
			}
			d.setTotalDiskon(Double.valueOf(totalDiskon));
			d.setTotalCashback(Double.valueOf(totalCashback));
			d.setTotalBiaya(Double.valueOf(totalBiaya));
			session.update(d);

			boolean lunasDiperbarui = false;
			if (d.getLunas() != null) {
				PembelianAnggotaKoperasi pak = d.getLunas();
				@SuppressWarnings("unchecked")
				java.util.List<ais.database.model.inventory.Pembelian> paidRows = session
						.createCriteria(ais.database.model.inventory.Pembelian.class)
						.add(Restrictions.eq("pembelianAnggotaKoperasi", pak)).list();
				double totalDiskonLunas = 0, totalCashbackLunas = 0;
				for (ais.database.model.inventory.Pembelian p : paidRows) {
					if (p.getProduk() == null) {
						continue;
					}
					double[] cocok = hasilPerProduk.get(p.getProduk().getId());
					double diskonLunas = cocok == null ? (p.getDiskon() == null ? 0 : p.getDiskon().doubleValue()) : cocok[0];
					double cashbackLunas = cocok == null ? (p.getCashback() == null ? 0 : p.getCashback().doubleValue()) : cocok[1];
					if (cocok != null) {
						p.setDiskon(Double.valueOf(diskonLunas));
						p.setCashback(Double.valueOf(cashbackLunas));
						Long aturanIdLunas = cocok[2] < 0 ? null : Long.valueOf((long) cocok[2]);
						p.setAturanDiskon(aturanIdLunas == null ? null
								: (ais.database.model.koperasi.AturanDiskon) session
										.get(ais.database.model.koperasi.AturanDiskon.class, aturanIdLunas));
						session.update(p);
						lunasDiperbarui = true;
					}
					totalDiskonLunas += diskonLunas;
					totalCashbackLunas += cashbackLunas;
				}
				if (lunasDiperbarui) {
					pak.setTotalDiskon(Double.valueOf(totalDiskonLunas));
					pak.setTotalCashback(Double.valueOf(totalCashbackLunas));
					session.update(pak);
				}
			}

			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("totalDiskon", totalDiskon);
			hasil.put("totalCashback", totalCashback);
			hasil.put("totalBiaya", totalBiaya);
			hasil.put("lunasDiperbarui", lunasDiperbarui);
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback,
						"auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:pesananHitungUlang-rollback");
			}
			hasil.put("status", "91");
			hasil.put("description", Common.tampilErrorJikaAdmin(e));
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Inti perhitungan promo (cocokkan tiap item ke {@code koperasi.aturan_diskon} aktif yang berlaku
	 * utk produk/toko/tanggal/jenis-tipe member, terapkan persentase-atau-nominal dibatasi maksimal
	 * potongan, hormati kuota harian-per-toko bila berlaku) -- DIEKSTRAK dari {@link #diskonEvaluasi}
	 * (yang MURNI menghitung, tidak menyimpan apa pun -- dipakai membangun keranjang sebelum checkout)
	 * supaya {@link #pesananHitungUlang} (yang MENGHITUNG ULANG **DAN MENYIMPAN** utk satu pesanan yang
	 * sudah ada, termasuk yang sudah lunas -- padanan tombol "Hitung Ulang" di JSP
	 * {@code _draft_pesanan_anggota.jsp}) memakai LOGIKA PENCOCOKAN YANG SAMA PERSIS, bukan salinan
	 * kedua yang berisiko perlahan berbeda hasilnya dari yang dipakai saat checkout normal.
	 *
	 * @param conn    koneksi JDBC aktif (dari {@code session.connection()}).
	 * @param tokoId  id toko (wajib, tidak divalidasi ulang di sini -- pemanggil bertanggung jawab).
	 * @param memberId id anggota koperasi (boleh {@code null} -- item tanpa member hanya cocok dgn
	 *                 aturan {@code berlaku_semua_member=true}).
	 * @param items   array {@code {id:produkId, harga, jumlah}} (field lain diabaikan).
	 * @return array sejajar (indeks sama dgn {@code items}) berisi
	 *         {@code {id, diskon, cashback, aturanDiskon, berlakuPerHariDanPerToko}}.
	 */
	private static JSONArray evaluasiDiskonItems(java.sql.Connection conn, Long tokoId, Long memberId, JSONArray items)
			throws Exception {
		{
			java.util.LinkedHashSet<Long> produkIdSet = new java.util.LinkedHashSet<Long>();
			for (int i = 0; i < items.length(); i++) {
				JSONObject it = items.getJSONObject(i);
				if (!it.isNull("id")) {
					produkIdSet.add(Long.valueOf((it.get("id") + "").trim()));
				}
			}
			if (produkIdSet.isEmpty()) {
				return new JSONArray();
			}
			StringBuilder inKlausa = new StringBuilder();
			for (Long pid : produkIdSet) {
				if (inKlausa.length() > 0) {
					inKlausa.append(',');
				}
				inKlausa.append(pid);
			}

			java.util.List<java.util.Map<String, Object>> rules = new java.util.ArrayList<java.util.Map<String, Object>>();
			java.sql.PreparedStatement psRule = conn.prepareStatement(
					"SELECT id, produk, toko, COALESCE(berlaku_semua_member,false), jenis_anggota, tipe_anggota, "
							+ "persentase, maksimal_potongan, nominal, COALESCE(potongan_langsung,true), "
							+ "COALESCE(berlaku_per_hari_dan_per_toko,false) FROM koperasi.aturan_diskon "
							+ "WHERE aktif = true AND produk IN (" + inKlausa + ") AND (toko IS NULL OR toko = ?) "
							+ "AND (tanggal_mulai IS NULL OR tanggal_mulai <= now()) "
							+ "AND (tanggal_selesai IS NULL OR tanggal_selesai >= now()) ORDER BY id ASC");
			psRule.setLong(1, tokoId);
			java.sql.ResultSet rsRule = psRule.executeQuery();
			while (rsRule.next()) {
				java.util.Map<String, Object> r = new java.util.HashMap<String, Object>();
				r.put("id", rsRule.getLong(1));
				r.put("produk", rsRule.getLong(2));
				long tokoRule = rsRule.getLong(3);
				r.put("toko", rsRule.wasNull() ? null : Long.valueOf(tokoRule));
				r.put("berlakuSemuaMember", rsRule.getBoolean(4));
				long jenisRule = rsRule.getLong(5);
				r.put("jenisAnggota", rsRule.wasNull() ? null : Long.valueOf(jenisRule));
				long tipeRule = rsRule.getLong(6);
				r.put("tipeAnggota", rsRule.wasNull() ? null : Long.valueOf(tipeRule));
				r.put("persentase", rsRule.getDouble(7));
				r.put("maksimalPotongan", rsRule.getDouble(8));
				r.put("nominal", rsRule.getDouble(9));
				r.put("potonganLangsung", rsRule.getBoolean(10));
				r.put("berlakuPerHariDanPerToko", rsRule.getBoolean(11));
				r.put("terpakaiHariIni", Double.valueOf(0d));
				r.put("terpakaiDiKeranjang", Double.valueOf(0d));
				rules.add(r);
			}
			rsRule.close();
			psRule.close();

			Long memberJenis = null;
			Long memberTipe = null;
			if (memberId != null) {
				java.sql.PreparedStatement psM = conn.prepareStatement(
						"SELECT jenis_anggota_koperasi, tipe_anggota_koperasi FROM koperasi.anggota_koperasi WHERE id = ?");
				psM.setLong(1, memberId);
				java.sql.ResultSet rsM = psM.executeQuery();
				if (rsM.next()) {
					long j = rsM.getLong(1);
					memberJenis = rsM.wasNull() ? null : Long.valueOf(j);
					long t = rsM.getLong(2);
					memberTipe = rsM.wasNull() ? null : Long.valueOf(t);
				}
				rsM.close();
				psM.close();

				for (java.util.Map<String, Object> r : rules) {
					if (!Boolean.TRUE.equals(r.get("berlakuPerHariDanPerToko"))) {
						continue;
					}
					long ruleId = ((Long) r.get("id")).longValue();
					java.sql.PreparedStatement psU = conn.prepareStatement(
							"SELECT COALESCE(SUM(terpakai),0) FROM ("
									+ "SELECT COALESCE(SUM(COALESCE(p.diskon,0)+COALESCE(p.cashback,0)),0) as terpakai "
									+ "FROM koperasi.pembelian p LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON p.pembelian_anggota_koperasi = pak.id "
									+ "WHERE p.aturan_diskon = ? AND p.toko = ? AND pak.anggota_koperasi = ? AND DATE(pak.tanggal_pembayaran) = CURRENT_DATE "
									+ "UNION ALL "
									+ "SELECT COALESCE(SUM(COALESCE(dp.diskon,0)+COALESCE(dp.cashback,0)),0) as terpakai "
									+ "FROM koperasi.draft_pembelian dp LEFT JOIN koperasi.draft_pembelian_anggota_koperasi dpak ON dp.draft_pembelian_anggota_koperasi = dpak.id "
									+ "WHERE dp.aturan_diskon = ? AND dp.toko = ? AND dpak.anggota_koperasi = ? AND DATE(dpak.tanggal_pembayaran) = CURRENT_DATE AND dpak.lunas IS NULL"
									+ ") gabungan");
					psU.setLong(1, ruleId);
					psU.setLong(2, tokoId.longValue());
					psU.setLong(3, memberId.longValue());
					psU.setLong(4, ruleId);
					psU.setLong(5, tokoId.longValue());
					psU.setLong(6, memberId.longValue());
					java.sql.ResultSet rsU = psU.executeQuery();
					if (rsU.next()) {
						r.put("terpakaiHariIni", Double.valueOf(rsU.getDouble(1)));
					}
					rsU.close();
					psU.close();
				}
			}

			JSONArray outArr = new JSONArray();
			for (int i = 0; i < items.length(); i++) {
				JSONObject it = items.getJSONObject(i);
				Long produkId = it.isNull("id") ? null : Long.valueOf((it.get("id") + "").trim());
				double harga = it.optDouble("harga", 0);
				double jumlah = it.optDouble("jumlah", 0);
				double itemTotal = harga * jumlah;

				java.util.Map<String, Object> applied = null;
				for (java.util.Map<String, Object> r : rules) {
					if (produkId == null || !produkId.equals(r.get("produk"))) {
						continue;
					}
					Long tokoRule = (Long) r.get("toko");
					if (tokoRule != null && !tokoRule.equals(tokoId)) {
						continue;
					}
					boolean semuaMember = Boolean.TRUE.equals(r.get("berlakuSemuaMember"));
					if (!semuaMember) {
						if (memberId == null) {
							continue;
						}
						Long jenisRule = (Long) r.get("jenisAnggota");
						if (jenisRule != null && !jenisRule.equals(memberJenis)) {
							continue;
						}
						Long tipeRule = (Long) r.get("tipeAnggota");
						if (tipeRule != null && !tipeRule.equals(memberTipe)) {
							continue;
						}
					}
					applied = r;
					break;
				}

				double diskon = 0;
				double cashback = 0;
				Long aturanDiskonId = null;
				boolean berlakuPerHari = false;
				if (applied != null) {
					double persen = ((Double) applied.get("persentase")).doubleValue();
					double nominal = ((Double) applied.get("nominal")).doubleValue();
					double maksimalPotongan = ((Double) applied.get("maksimalPotongan")).doubleValue();
					double discountValue = 0;
					if (persen > 0) {
						discountValue = itemTotal * (persen / 100);
					} else if (nominal > 0) {
						discountValue = nominal * jumlah;
						if (discountValue > itemTotal) {
							discountValue = itemTotal;
						}
					}
					berlakuPerHari = Boolean.TRUE.equals(applied.get("berlakuPerHariDanPerToko"));
					if (berlakuPerHari && maksimalPotongan > 0) {
						double terpakaiHariIni = ((Double) applied.get("terpakaiHariIni")).doubleValue();
						double terpakaiDiKeranjang = ((Double) applied.get("terpakaiDiKeranjang")).doubleValue();
						double sisa = maksimalPotongan - terpakaiHariIni - terpakaiDiKeranjang;
						if (sisa <= 0) {
							discountValue = 0;
						} else if (discountValue > sisa) {
							discountValue = sisa;
						}
						applied.put("terpakaiDiKeranjang", Double.valueOf(terpakaiDiKeranjang + discountValue));
					} else if (maksimalPotongan > 0 && discountValue > maksimalPotongan) {
						discountValue = maksimalPotongan;
					}
					boolean potonganLangsung = Boolean.TRUE.equals(applied.get("potonganLangsung"));
					if (potonganLangsung) {
						diskon = discountValue;
					} else {
						cashback = discountValue;
					}
					aturanDiskonId = (Long) applied.get("id");
				}

				JSONObject out = new JSONObject();
				out.put("id", produkId);
				out.put("diskon", diskon);
				out.put("cashback", cashback);
				out.put("aturanDiskon", aturanDiskonId == null ? JSONObject.NULL : aturanDiskonId);
				out.put("berlakuPerHariDanPerToko", berlakuPerHari);
				outArr.put(out);
			}

			return outArr;
		}
	}
}
