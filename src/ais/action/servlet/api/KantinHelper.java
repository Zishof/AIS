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
import org.zkoss.poi.ss.usermodel.FormulaEvaluator;
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
import ais.database.model.inventory.DraftPembelian;
import ais.database.model.inventory.PemasokProduk;
import ais.database.model.inventory.Pembelian;
import ais.database.model.inventory.PengadaanFaktur;
import ais.database.model.inventory.PengadaanProduk;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.ProdukBatch;
import ais.database.model.inventory.MutasiProdukBatch;
import ais.database.model.inventory.ReturPembelian;
import ais.database.model.inventory.ReturPenjualan;
import ais.database.model.inventory.SatuanProduk;
import ais.database.model.inventory.SesiKasKasir;
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
import ais.database.model.koperasi.TipeAnggotaKoperasi;
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
		final double diskonFaktur;
		final double nilaiDiskonFaktur;
		final boolean diskonFakturPersen;

		TotalHitung(double total, double totalDiskon, double totalCashback,
				double diskonFaktur, double nilaiDiskonFaktur, boolean diskonFakturPersen) {
			this.total = total;
			this.totalDiskon = totalDiskon;
			this.totalCashback = totalCashback;
			this.diskonFaktur = diskonFaktur;
			this.nilaiDiskonFaktur = nilaiDiskonFaktur;
			this.diskonFakturPersen = diskonFakturPersen;
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
	/**
	 * Gap-closure "Produk Ekstra" -- meratakan {@code transaksi} (yang tiap itemnya BOLEH punya key
	 * opsional {@code ekstra}: array {@code {id,kode,nama,harga,jumlah}}, {@code jumlah} = pengali
	 * PER-UNIT induk) jadi {@code JSONArray} BARU berisi baris flat sejajar dgn baris induknya,
	 * SUPAYA logic yang sudah generic/per-produk-id (cek kadaluarsa, lock stok,
	 * {@link #hitungTotalDiskonCashback}, {@code BahanBakuUtil.konsumsiBahanBaku}, recompute stok)
	 * bisa dipakai APA ADANYA tanpa tahu konsep "ekstra" sama sekali -- lihat memory roadmap Produk
	 * Ekstra. TIDAK PERNAH memutasi {@code transaksi} asli (dikembalikan array baru) -- pemanggil
	 * checkout MASIH perlu {@code transaksi} nested asli utk {@code simpanRinci}, satu-satunya
	 * tempat yang perlu paham nesting (bikin baris Pembelian/DraftPembelian ekstra + set indukId).
	 *
	 * <p>Item TANPA key {@code ekstra} (atau kosong) diteruskan apa adanya, TIDAK diubah -- ini yang
	 * menjamin transaksi checkout lama (99% kasus hari ini) 100% identik perilakunya, nol regresi.</p>
	 *
	 * @return array baru; entry ekstra dapat {@code diskon=0}/{@code cashback=0} (ekstra SENGAJA
	 *         belum independently discount-eligible di batch ini).
	 */
	private static JSONArray ratakanTransaksiDenganEkstra(JSONArray transaksi) {
		JSONArray hasil = new JSONArray();
		for (int i = 0; i < transaksi.length(); i++) {
			try {
				JSONObject item = transaksi.getJSONObject(i);
				hasil.put(item);
				if (item.isNull("ekstra")) {
					continue;
				}
				JSONArray ekstraArr = item.optJSONArray("ekstra");
				if (ekstraArr == null || ekstraArr.length() == 0) {
					continue;
				}
				double jumlahInduk = item.isNull("jumlah") ? 1.0
						: Double.parseDouble((item.get("jumlah") + "").trim());
				for (int k = 0; k < ekstraArr.length(); k++) {
					try {
						JSONObject e = ekstraArr.getJSONObject(k);
						double jumlahEkstra = e.isNull("jumlah") ? 1.0
								: Double.parseDouble((e.get("jumlah") + "").trim());
						JSONObject rata = new JSONObject();
						rata.put("id", e.isNull("id") ? JSONObject.NULL : e.get("id"));
						rata.put("kode", e.optString("kode", "_"));
						rata.put("nama", e.optString("nama", "_"));
						rata.put("harga", e.isNull("harga") ? 0.0 : Double.parseDouble((e.get("harga") + "").trim()));
						rata.put("jumlah", jumlahInduk * jumlahEkstra);
						rata.put("diskon", 0.0);
						rata.put("cashback", 0.0);
						hasil.put(rata);
					} catch (Exception eEkstra) {
						ais.common.ErrorAuditUtil.record(eEkstra,
								"auto-audit src/ais/action/servlet/api/KantinHelper.java:ratakanTransaksiDenganEkstra:ekstra");
					}
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit src/ais/action/servlet/api/KantinHelper.java:ratakanTransaksiDenganEkstra");
			}
		}
		return hasil;
	}

	private static TotalHitung hitungTotalDiskonCashback(JSONObject jsonObject, JSONArray transaksi,
			String auditTagSuffix) throws Exception {
		double pajak = jsonObject.isNull("pajak") ? 0.0
				: Math.max(0.0, Double.parseDouble((jsonObject.get("pajak") + "").trim()));
		double total = pajak;
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
		// Potongan faktur dimasukkan langsung oleh kasir dan diterapkan SETELAH seluruh
		// diskon item. Server menghitung ulang nilainya agar klien tidak dapat mengirim
		// total akhir buatan. Nilai persen dibatasi 0..100 dan nominal tidak boleh
		// melampaui nilai barang setelah diskon; pajak tidak pernah ikut dipotong.
		String tipeDiskonFaktur = jsonObject.optString("diskon_faktur_tipe", "NOMINAL").trim();
		boolean diskonFakturPersen = "PERSEN".equalsIgnoreCase(tipeDiskonFaktur)
				|| "PERCENT".equalsIgnoreCase(tipeDiskonFaktur);
		double nilaiDiskonFaktur = jsonObject.isNull("diskon_faktur_nilai") ? 0.0
				: Math.max(0.0, Double.parseDouble((jsonObject.get("diskon_faktur_nilai") + "").trim()));
		if (diskonFakturPersen) nilaiDiskonFaktur = Math.min(100.0, nilaiDiskonFaktur);
		double dasarDiskonFaktur = Math.max(0.0, total - pajak);
		double diskonFaktur = diskonFakturPersen
				? dasarDiskonFaktur * nilaiDiskonFaktur / 100.0
				: Math.min(dasarDiskonFaktur, nilaiDiskonFaktur);
		total = Math.max(0.0, total - diskonFaktur);
		totalDiskon += diskonFaktur;
		return new TotalHitung(total, totalDiskon, totalCashback, diskonFaktur,
				nilaiDiskonFaktur, diskonFakturPersen);
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

		void terapkanKe(PembelianAnggotaKoperasi p, CaraPembayaranKoperasi caraUtama, Double totalBiaya) {
			p.setCaraPembayaranKoperasi2(cara2);
			p.setNominalBayar2(nominal2);
			p.setCaraPembayaranKoperasi3(cara3);
			p.setNominalBayar3(nominal3);
			p.setCaraPembayaranKoperasi4(cara4);
			p.setNominalBayar4(nominal4);
			p.setCaraPembayaranKoperasi5(cara5);
			p.setNominalBayar5(nominal5);

			double n2 = nominalPositif(nominal2);
			double n3 = nominalPositif(nominal3);
			double n4 = nominalPositif(nominal4);
			double n5 = nominalPositif(nominal5);
			double n1 = Math.max(0.0, nominalPositif(totalBiaya) - n2 - n3 - n4 - n5);
			double tunai = nominalTunai(caraUtama, n1) + nominalTunai(cara2, n2)
					+ nominalTunai(cara3, n3) + nominalTunai(cara4, n4) + nominalTunai(cara5, n5);
			double nonTunai = nominalNonTunai(caraUtama, n1) + nominalNonTunai(cara2, n2)
					+ nominalNonTunai(cara3, n3) + nominalNonTunai(cara4, n4) + nominalNonTunai(cara5, n5);
			p.setBayarTunai(Double.valueOf(tunai));
			p.setBayarNonTunai(Double.valueOf(nonTunai));
		}

		private static double nominalPositif(Double nilai) {
			return nilai == null ? 0.0 : Math.max(0.0, nilai.doubleValue());
		}

		private static boolean dibayar(CaraPembayaranKoperasi cara) {
			return cara != null && !Boolean.TRUE.equals(cara.getMasukSebagaiHutang());
		}

		private static double nominalTunai(CaraPembayaranKoperasi cara, double nominal) {
			return dibayar(cara) && Boolean.TRUE.equals(cara.getAdaKembalian()) ? nominal : 0.0;
		}

		private static double nominalNonTunai(CaraPembayaranKoperasi cara, double nominal) {
			return dibayar(cara) && !Boolean.TRUE.equals(cara.getAdaKembalian()) ? nominal : 0.0;
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

	/**
	 * Menjadikan keranjang yang sedang dilihat kasir sebagai sumber kebenaran ketika sebuah draft
	 * tertahan difinalisasi. Draft lama dapat memiliki rincian parsial akibat implementasi lama yang
	 * melakukan commit per item. Menolak perbedaan itu terus-menerus membuat draft tersebut mustahil
	 * dibayar, sedangkan memakai rincian lama akan menjual item yang salah. Karena itu seluruh rincian
	 * draft yang BELUM lunas diganti dalam satu transaksi database, lalu diverifikasi jumlahnya.
	 */
	private static void sinkronkanRincianDraftUntukFinalisasi(Session session,
			DraftPembelianAnggotaKoperasi draft, JSONArray transaksi, String kodeUnik, Date waktu,
			Toko toko, AnggotaKoperasi anggota, KodePembayaranOnline kodePembayaranOnline,
			CaraPembayaranKoperasi caraBayar, TotalHitung totalHitung) {
		if (draft == null || draft.getId() == null || draft.getLunas() != null) return;
		if (transaksi == null || transaksi.length() == 0) {
			throw new IllegalStateException("Keranjang pembayaran kosong sehingga draft tidak dapat difinalisasi.");
		}
		Long tokoDraft = draft.getToko() == null ? null : draft.getToko().getId();
		if (tokoDraft == null || toko == null || !tokoDraft.equals(toko.getId())) {
			throw new IllegalStateException("Draft pesanan bukan milik toko yang sedang aktif.");
		}

		boolean mulaiTransaksi = session.getTransaction() == null || !session.getTransaction().isActive();
		if (!mulaiTransaksi) {
			throw new IllegalStateException("Sinkronisasi rincian draft dipanggil saat transaksi database lain masih aktif.");
		}
		try {
			session.beginTransaction();
			session.createSQLQuery("update koperasi.draft_pembelian set lunas=null "
					+ "where draft_pembelian_anggota_koperasi=:draftId")
					.setParameter("draftId", draft.getId()).executeUpdate();
			session.createSQLQuery("delete from koperasi.draft_pembelian "
					+ "where draft_pembelian_anggota_koperasi=:draftId")
					.setParameter("draftId", draft.getId()).executeUpdate();

			int jumlahDiharapkan = 0;
			for (int i = 0; i < transaksi.length(); i++) {
				JSONObject item = transaksi.getJSONObject(i);
				DraftPembelian induk = buatRincianDraftAtomik(session, draft, item, kodeUnik, waktu,
						toko, anggota, kodePembayaranOnline, null, 1.0);
				jumlahDiharapkan++;
				JSONArray ekstra = item.optJSONArray("ekstra");
				for (int k = 0; ekstra != null && k < ekstra.length(); k++) {
					buatRincianDraftAtomik(session, draft, ekstra.getJSONObject(k), kodeUnik, waktu,
							toko, anggota, kodePembayaranOnline, induk.getId(),
							item.optDouble("jumlah", 1.0));
					jumlahDiharapkan++;
				}
			}

			draft.setAnggotaKoperasi(anggota);
			draft.setCaraPembayaranKoperasi(caraBayar);
			draft.setKodePembayaranOnline(kodePembayaranOnline);
			draft.setTanggalPembayaran(waktu);
			draft.setTotalDiskon(Double.valueOf(totalHitung.totalDiskon));
			draft.setTotalCashback(Double.valueOf(totalHitung.totalCashback));
			draft.setTotalBiaya(Double.valueOf(totalHitung.total));
			draft.setBiaya(Double.valueOf(totalHitung.total));
			session.update(draft);
			session.flush();

			Number jumlahAktual = (Number) session.createSQLQuery("select count(*) from koperasi.draft_pembelian "
					+ "where draft_pembelian_anggota_koperasi=:draftId")
					.setParameter("draftId", draft.getId()).uniqueResult();
			int aktual = jumlahAktual == null ? 0 : jumlahAktual.intValue();
			if (aktual != jumlahDiharapkan) {
				throw new IllegalStateException("Sinkronisasi rincian draft belum lengkap: " + aktual
						+ " dari " + jumlahDiharapkan + " baris.");
			}
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session.getTransaction() != null && session.getTransaction().isActive()) {
				try { session.getTransaction().rollback(); }
				catch (Exception rollbackError) {
					ais.common.ErrorAuditUtil.record(rollbackError,
							"auto-audit KantinHelper:sinkronkanRincianDraftUntukFinalisasiRollback");
				}
			}
			throw e instanceof IllegalStateException ? (IllegalStateException) e
					: new IllegalStateException("Rincian draft belum dapat disinkronkan secara lengkap.", e);
		}
	}

	private static DraftPembelian buatRincianDraftAtomik(Session session,
			DraftPembelianAnggotaKoperasi draft, JSONObject item, String kodeUnik, Date waktu,
			Toko toko, AnggotaKoperasi anggota, KodePembayaranOnline kodePembayaranOnline,
			Long indukId, double pengaliJumlah) {
		String idText = item.optString("id", "").trim();
		if (!Common.isNumber(idText)) throw new IllegalStateException("Produk pada keranjang tidak memiliki ID yang valid.");
		Produk produk = (Produk) session.get(Produk.class, Long.valueOf(idText));
		if (produk == null) throw new IllegalStateException("Produk #" + idText + " tidak ditemukan.");
		Long tokoProduk = produk.getToko() == null ? null : produk.getToko().getId();
		if (tokoProduk == null || !tokoProduk.equals(toko.getId())) {
			throw new IllegalStateException("Produk " + produk.getNama() + " bukan milik toko yang sedang aktif.");
		}
		double qty = item.optDouble("jumlah", 1.0) * pengaliJumlah;
		double harga = item.optDouble("harga", 0.0);
		double diskon = indukId == null ? item.optDouble("diskon", 0.0) : 0.0;
		double cashback = indukId == null ? item.optDouble("cashback", 0.0) : 0.0;
		if (qty <= 0 || harga < 0 || diskon < 0 || cashback < 0) {
			throw new IllegalStateException("Nilai jumlah/harga/promo produk " + produk.getNama() + " tidak valid.");
		}

		DraftPembelian detail = new DraftPembelian();
		detail.setDraftPembelianAnggotaKoperasi(draft);
		detail.setAnggotaKoperasi(anggota);
		detail.setKode(kodeUnik + "-" + item.optString("kode", produk.getKode()));
		detail.setNama(item.optString("nama", produk.getNama()));
		detail.setKodePembayaranOnline(kodePembayaranOnline);
		detail.setQty(Double.valueOf(qty));
		detail.setHargaSatuan(Double.valueOf(harga));
		detail.setDiskon(Double.valueOf(diskon));
		detail.setCashback(Double.valueOf(cashback));
		if (indukId == null && !item.isNull("aturanDiskon")
				&& Common.isNumber(item.optString("aturanDiskon", ""))) {
			detail.setAturanDiskon((ais.database.model.koperasi.AturanDiskon) session.get(
					ais.database.model.koperasi.AturanDiskon.class,
					Long.valueOf(item.optString("aturanDiskon"))));
		}
		detail.setProduk(produk);
		detail.setToko(toko);
		detail.setWaktu(waktu);
		detail.setTotal(Double.valueOf((harga * qty) - diskon));
		detail.setIndukId(indukId);
		session.save(detail);
		session.flush();
		return detail;
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
					Long iddraftPembelianAnggotaKoperasi = (jsonObject.isNull("draftPembelianAnggotaKoperasi")
							|| !Common.isNumber((jsonObject.get("draftPembelianAnggotaKoperasi") + "").trim())) ? null
									: Long.parseLong((jsonObject.get("draftPembelianAnggotaKoperasi") + "").trim());
					DraftPembelianAnggotaKoperasi draftPembelianAnggotaKoperasi = (iddraftPembelianAnggotaKoperasi == null
							? null
							: (DraftPembelianAnggotaKoperasi) session
									.createCriteria(DraftPembelianAnggotaKoperasi.class)
									.add(Restrictions.idEq(iddraftPembelianAnggotaKoperasi)).uniqueResult());
					// Fitur "Sesi Kasir": gerbang SERVER-SIDE (belt-and-suspenders) di titik SATU-SATUNYA
					// tempat checkout FINAL ditulis -- JSP dan Desktop (PosApi) sebelumnya TIDAK punya
					// gerbang sama sekali (hanya ZK PosKantinAction.onBayar() yg mengecek client-side
					// sebelum sampai ke sini). Dipasang di sini, ketiga platform otomatis konsisten tanpa
					// duplikasi logika. Sengaja TIDAK dipasang di draft_bayar() -- "Simpan/Tahan Keranjang"
					// bukan komitmen finansial, jadi tidak perlu sesi kas terbuka.
					//
					// Sakelar terpusat DEFAULT AKTIF. Unit tanpa laci/shift kas boleh menonaktifkannya
					// secara eksplisit; ketika aktif, validasi akun+toko+perangkat+sesi tetap wajib.
					boolean wajibSesiKas = Common.bolehKonfigurasi(
							ais.database.model.Konfigurasi.KANTIN_POS_WAJIB_SESI_KAS,
							ais.database.model.Konfigurasi.AKTIF);
					SesiKasKasir sesiKasAktif = null;
					String idPerangkatTransaksi = idPerangkat(jsonObject);
					if (wajibSesiKas) {
						String[] idKasir = identitasKasir(tbmuser);
						if (idPerangkatTransaksi == null) {
							hasil.put("status", "91");
							hasil.put("description", "Perangkat kasir belum dikenali. Perbarui aplikasi atau muat ulang halaman, lalu buka sesi kas kembali.");
							return;
						}
						sesiKasAktif = ais.action.master.koperasi.helper.SesiKasUtil.sesiTerbukaPerangkat(session,
								idKasir[0], idKasir[1], toko.getId(), idPerangkatTransaksi);
						if (sesiKasAktif == null) {
							SesiKasKasir sesiLain = ais.action.master.koperasi.helper.SesiKasUtil.sesiTerbuka(session,
									idKasir[0], idKasir[1], null);
							hasil.put("status", "91");
							hasil.put("description", sesiLain == null
									? "Belum ada Sesi Kas Kasir yang terbuka pada perangkat ini. Buka kas terlebih dahulu sebelum memproses pembayaran."
									: "Akun ini masih memiliki sesi kas pada perangkat lain ("
											+ (sesiLain.getNamaPerangkat() == null ? sesiLain.getIdPerangkat() : sesiLain.getNamaPerangkat())
											+ "). Tutup sesi tersebut atau minta supervisor melakukan penutupan resmi.");
							return;
						}
						ais.action.master.koperasi.helper.SesiKasUtil.ikatPerangkatJikaLama(
								sesiKasAktif, idPerangkatTransaksi, namaPerangkat(jsonObject));
						String kodeSesiDiminta = jsonObject.optString("kode_sesi_kas", "").trim();
						if (kodeSesiDiminta.length() > 0 && !kodeSesiDiminta.equals(sesiKasAktif.getKode())) {
							// Pembayaran langsung selalu diikat ke sesi server yang sedang aktif. Kode sesi
							// dari perangkat hanya menjadi batas audit yang keras ketika request merupakan
							// pengiriman ulang transaksi pending/offline.
							boolean pengirimanPending = jsonObject.optBoolean("pengiriman_pending", true);
							boolean draftKasirSama = false;
							if (draftPembelianAnggotaKoperasi != null) {
								Tbmuser kasirDraft = draftPembelianAnggotaKoperasi.getTbmuser();
								draftKasirSama = kasirDraft != null && idKasir[1].equals(kasirDraft.getUserId());
								if (!draftKasirSama && draftPembelianAnggotaKoperasi.getKasirLoginNama() != null) {
									draftKasirSama = idKasir[0].equalsIgnoreCase(
											draftPembelianAnggotaKoperasi.getKasirLoginNama().trim());
								}
								if (!draftKasirSama && draftPembelianAnggotaKoperasi.getOlehId() != null) {
									draftKasirSama = idKasir[1].equals(draftPembelianAnggotaKoperasi.getOlehId().trim());
								}
							}
							boolean transaksiLangsung = !pengirimanPending && draftPembelianAnggotaKoperasi == null;
							if (!draftKasirSama && !transaksiLangsung) {
								hasil.put("status", "91");
								hasil.put("description", "Transaksi berasal dari sesi kas yang berbeda dan bukan transaksi tertahan milik kasir yang sedang login.");
								return;
							}
						}
					}
					JSONArray transaksi = jsonObject.getJSONArray("transaksi");
					// Hitung promo sekali lagi di SERVER sebelum penyimpanan. Dengan demikian POS
					// Desktop, Android, JSP, ZK dan pemanggil API lain memperoleh aturan grup,
					// potongan, serta cashback yang sama walaupun versi UI berbeda atau cache
					// katalognya belum segar. Nilai dari klien tidak dijadikan sumber kebenaran.
					terapkanEvaluasiDiskonServer(session.connection(), toko.getId(),
							anggotaKoperasi == null ? null : anggotaKoperasi.getId(), transaksi);
					// Gap-closure "Produk Ekstra" -- versi RATA dipakai oleh SEMUA logic generic per-baris
					// di bawah (cek kadaluarsa, lock stok, hitung total, konsumsi bahan baku, recompute
					// stok) supaya ekstra ikut tervalidasi/terhitung/terdekremen persis spt baris biasa
					// TANPA satu pun dari method itu perlu tahu konsep "ekstra" -- lihat JavaDoc
					// ratakanTransaksiDenganEkstra. `transaksi` NESTED ASLI tetap dipakai di simpanRinci
					// (satu-satunya tempat yang perlu bikin baris Pembelian ekstra + set indukId).
					JSONArray transaksiRata = ratakanTransaksiDenganEkstra(transaksi);

					// Fase 0 (2026-07-26, gap analisis PDF klien "Kadaluarsa"): produk yang SUDAH lewat
					// tanggal kadaluarsa (Produk.tanggalExpired) WAJIB diblokir keras -- BEDA dgn kekurangan
					// stok biasa di bawah (yang sengaja fail-open per instruksi 2026-07-20), krn menjual
					// barang kadaluarsa adalah masalah keamanan pangan/kepatuhan, bukan sekadar masalah
					// data stok historis blm bersih. Field ini sudah ada (dipakai laporan "akan/sudah
					// kadaluarsa" di LaporanKantinUtil) tapi SEBELUM INI tidak pernah dicek sama sekali di
					// jalur checkout -- kasir bisa saja menjual produk kadaluarsa tanpa peringatan apa pun.
					boolean bolehStokHabisToko = Boolean.TRUE.equals(toko.getBolehTransaksiStokHabis());
					List<String> produkKadaluarsa = cekProdukKadaluarsa(session, transaksiRata);
					if (!produkKadaluarsa.isEmpty()) {
						hasil.put("status", "91");
						hasil.put("description", "Produk berikut sudah melewati tanggal kadaluarsa dan tidak boleh dijual: "
								+ gabungkanDenganKoma(produkKadaluarsa) + ". Segera pisahkan dari stok jual.");
						return;
					}
					List<String> batchKurang = cekKetersediaanBatchFefo(
							session, transaksiRata, bolehStokHabisToko);
					if (!batchKurang.isEmpty()) {
						hasil.put("status", "91");
						hasil.put("description", "Stok batch aktif yang belum kedaluwarsa tidak mencukupi: "
								+ gabungkanDenganKoma(batchKurang)
								+ ". Periksa stok fisik atau aktifkan izin transaksi stok habis pada toko ini.");
						return;
					}

					// Fase 1: validasi stok server-side dengan row lock (SELECT...FOR UPDATE) -- sebelumnya
					// TIDAK ADA pengecekan stok di server sama sekali (hanya client-side lolosCekStok() di
					// PosKantinAction, yang bisa dilewati sepenuhnya lewat panggilan langsung ke
					// /Data?action=bayar).
					//
					// Kebijakan stok bersifat per toko dan default OFF. Bila toko mengaktifkan
					// bolehTransaksiStokHabis, validasi stok agregat/batch dilewati untuk toko itu saja.
					// Saat OFF, hanya produk dengan override izinkanJualMinusStok=true yang boleh lanjut.
					// Produk kedaluwarsa tetap selalu diblokir pada fase sebelumnya.
					HasilValidasiStok stokKurang = validasiStokCukupDenganLock(
							transaksiRata, toko.getId(), bolehStokHabisToko);
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
					// Kekurangan yang memang diizinkan oleh kebijakan toko atau override produk bukan
					// exception. Jangan membuat RuntimeException buatan karena akan tampil sebagai ERROR
					// produksi walaupun transaksi berhasil. Audit transaksi dan mutasi stok normal sudah
					// cukup untuk menelusuri penjualan dengan saldo stok nol/minus.

					TotalHitung th = hitungTotalDiskonCashback(jsonObject, transaksiRata, "bayarHitungTotal");
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

					// Gerbang batas hutang (2026-08-10): kalau salah satu SLOT pembayaran memakai cara
					// bayar yg ditandai CaraPembayaranKoperasi.masukSebagaiHutang, transaksi ini menambah
					// piutang toko ke anggota -- WAJIB dicek terhadap TipeAnggotaKoperasi.maksimalBolehUtang
					// SEBELUM transaksi ditulis (bukan sesudah), supaya anggota tidak bisa menumpuk hutang
					// melewati batas tipenya. Dilewati kalau tak ada anggota (transaksi umum/kasir tanpa
					// member selalu lunas tunai/non-tunai, bukan hutang perorangan).
					if (anggotaKoperasi != null) {
						double slot1Nominal = Math.max(0.0,
								total.doubleValue() - split.nominal2 - split.nominal3 - split.nominal4 - split.nominal5);
						double hutangBaru = 0.0;
						if (caraPembayaranKoperasiOnline != null
								&& Boolean.TRUE.equals(caraPembayaranKoperasiOnline.getMasukSebagaiHutang())) {
							hutangBaru += slot1Nominal;
						}
						if (split.cara2 != null && Boolean.TRUE.equals(split.cara2.getMasukSebagaiHutang())) hutangBaru += split.nominal2;
						if (split.cara3 != null && Boolean.TRUE.equals(split.cara3.getMasukSebagaiHutang())) hutangBaru += split.nominal3;
						if (split.cara4 != null && Boolean.TRUE.equals(split.cara4.getMasukSebagaiHutang())) hutangBaru += split.nominal4;
						if (split.cara5 != null && Boolean.TRUE.equals(split.cara5.getMasukSebagaiHutang())) hutangBaru += split.nominal5;

						if (hutangBaru > 0.0) {
							TipeAnggotaKoperasi tipeAnggotaHutang = anggotaKoperasi.getTipeAnggotaKoperasi();
							double maksimalBolehUtang = tipeAnggotaHutang == null ? 0.0
									: tipeAnggotaHutang.getMaksimalBolehUtang();
							double hutangBerjalan = hitungTotalHutangBerjalan(session, anggotaKoperasi.getId());
							if (hutangBerjalan + hutangBaru > maksimalBolehUtang + 0.5) {
								hasil.put("status", "91");
								hasil.put("description", "Transaksi ditolak: batas maksimal hutang anggota ini ("
										+ Common.numberFormat.get().format(maksimalBolehUtang) + ") akan terlampaui. "
										+ "Hutang berjalan saat ini " + Common.numberFormat.get().format(hutangBerjalan)
										+ ", transaksi ini menambah " + Common.numberFormat.get().format(hutangBaru) + ".");
								return;
							}
						}
					}

					KodePembayaranOnline kodePembayaranOnline = (KodePembayaranOnline) (kodePembayaranOnlineId == null
							? null
							: session.createCriteria(KodePembayaranOnline.class)
									.add(Restrictions.idEq(kodePembayaranOnlineId)).uniqueResult());

					// Keranjang kasir boleh berubah setelah draft tertahan dimuat (tambah/hapus produk,
					// koreksi qty, maupun promo). Build lama membandingkan jumlah rincian lalu selalu
					// menolak jika berbeda, sehingga draft parsial 1/5 tidak pernah dapat diselesaikan.
					// Sinkronisasi ini mengganti rincian draft BELUM LUNAS secara atomik memakai payload
					// terkini, sebelum rincian tersebut disalin menjadi transaksi final.
					sinkronkanRincianDraftUntukFinalisasi(session, draftPembelianAnggotaKoperasi,
							transaksi, kodeUnik, currentWaktu, toko, anggotaKoperasi,
							kodePembayaranOnline, caraPembayaranKoperasiOnline, th);

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
						pembelianAnggotaKoperasi.setDiskon(Double.valueOf(th.nilaiDiskonFaktur));
						pembelianAnggotaKoperasi.setDiskonDalamPersen(Boolean.valueOf(th.diskonFakturPersen));
						pembelianAnggotaKoperasi.setTotalCashback(totalCashback);
						pembelianAnggotaKoperasi.setKodePembayaranOnline(kodePembayaranOnline);
						pembelianAnggotaKoperasi.setKode(kodeUnik);
						pembelianAnggotaKoperasi.setAnggotaKoperasi(anggotaKoperasi);
						pembelianAnggotaKoperasi.setTanggalPembayaran(currentWaktu);
						pembelianAnggotaKoperasi.setCaraPembayaranKoperasi(caraPembayaranKoperasiOnline);
						pembelianAnggotaKoperasi.setTotalBiaya(total);
						pembelianAnggotaKoperasi.setBiaya(total);
						split.terapkanKe(pembelianAnggotaKoperasi, caraPembayaranKoperasiOnline, total);
						pembelianAnggotaKoperasi.setLokasi(lokasi);
						pembelianAnggotaKoperasi.setTbmuser(tbmuser);
						pembelianAnggotaKoperasi.setKasirLoginNama(kasirLoginNamaVal);
						pembelianAnggotaKoperasi.setNamaMesin(namaMesinVal);
						pembelianAnggotaKoperasi.setToko(toko);
						pembelianAnggotaKoperasi.setSesiKasKasir(sesiKasAktif);
						pembelianAnggotaKoperasi.setIdPerangkat(idPerangkatTransaksi);
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
						pembelianAnggotaKoperasi.setDiskon(Double.valueOf(th.nilaiDiskonFaktur));
						pembelianAnggotaKoperasi.setDiskonDalamPersen(Boolean.valueOf(th.diskonFakturPersen));
						pembelianAnggotaKoperasi.setTotalCashback(totalCashback);
						pembelianAnggotaKoperasi.setKodePembayaranOnline(kodePembayaranOnline);
						pembelianAnggotaKoperasi.setKode(kodeUnik);
						pembelianAnggotaKoperasi.setAnggotaKoperasi(anggotaKoperasi);
						pembelianAnggotaKoperasi.setTanggalPembayaran(currentWaktu);
						pembelianAnggotaKoperasi.setCaraPembayaranKoperasi(caraPembayaranKoperasiOnline);
						pembelianAnggotaKoperasi.setTotalBiaya(total);
						pembelianAnggotaKoperasi.setBiaya(total);
						split.terapkanKe(pembelianAnggotaKoperasi, caraPembayaranKoperasiOnline, total);
						pembelianAnggotaKoperasi.setLokasi(lokasi);
						pembelianAnggotaKoperasi.setTbmuser(tbmuser);
						pembelianAnggotaKoperasi.setKasirLoginNama(kasirLoginNamaVal);
						pembelianAnggotaKoperasi.setNamaMesin(namaMesinVal);
						pembelianAnggotaKoperasi.setToko(toko);
						pembelianAnggotaKoperasi.setSesiKasKasir(sesiKasAktif);
						pembelianAnggotaKoperasi.setIdPerangkat(idPerangkatTransaksi);
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
					ais.action.master.koperasi.helper.PembelianReferenceCleanupUtil
							.lepasDraftPembelianLunasUntukHeader(session, pembelianAnggotaKoperasi.getId());
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

					// Produk yang sudah memakai lot dikurangi otomatis dengan FEFO (tanggal paling dekat
					// lebih dahulu). Produk legacy tanpa lot tetap memakai perhitungan stok lama.
					try {
						konsumsiBatchFefo(session, transaksiRata, pembelianAnggotaKoperasi.getId(),
								tbmuser == null ? "pos" : tbmuser.getUserId(), bolehStokHabisToko);
					} catch (Exception exBatch) {
						exBatch.printStackTrace();
						ais.common.ErrorAuditUtil.record(exBatch,
								"auto-audit src/ais/action/servlet/api/KantinHelper.java:konsumsiBatchFefo");
					}

					// === Otomatis: kurangi stok bahan baku (resep/BOM) untuk produk ber-resep yang terjual.
					// Fail-safe: kegagalan di sini tidak boleh menggagalkan transaksi penjualan. ===
					try {
						java.util.Set<Long> bahanTerpakai = ais.action.master.inventory.BahanBakuUtil
								.konsumsiBahanBaku(session, transaksiRata, toko, currentWaktu, pembelianAnggotaKoperasi);
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
						for (int i = 0; i < transaksiRata.length(); i++) {
							try {
								JSONObject t = transaksiRata.getJSONObject(i);
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
					hasil.put("total", total.doubleValue());
					hasil.put("totalDiskon", totalDiskon.doubleValue());
					hasil.put("diskonFaktur", th.diskonFaktur);
					hasil.put("terlayani", dimintaLangsungTerlayani(jsonObject));
					hasil.put("status", "00");

				} catch (Exception e) {
					hasil.put("status", "91");
					// Pesan utama untuk kasir tidak boleh berisi nama class, SQL, atau stack trace.
					// PosApi akan mengubah description ini menjadi bahasa awam yang seragam;
					// exception lengkap tetap disediakan secara terpisah untuk tombol Detail Error.
					hasil.put("description", "Pembayaran belum dapat disimpan. Seluruh proses dibatalkan agar tidak ada data transaksi yang tersimpan sebagian.");
					hasil.put("teknis", detailTeknisError(e));
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/KantinHelper.java:209");
				} finally {
					// Gap-closure "Kas Terbuka tapi checkout ditolak" (2026-08-12): sebelumnya blok ini
					// clear/disconnect/close TANPA rollback dulu -- persis pola LAMA yang sudah terbukti
					// jadi akar bug "idle in transaction" di javadoc tutupSessionPolaB (koneksi kembali ke
					// pool c3p0 masih menggenggam transaksi implisit terbuka, permintaan BERIKUTNYA yang
					// meminjam koneksi yg SAMA bisa terus melihat snapshot data LAMA -- termasuk gerbang
					// Sesi Kas di atas yg baca SesiKasKasir dari SESSION INI, paling awal dipanggil di
					// method ini, paling rentan kena efeknya). bayar() ditulis sebelum tutupSessionPolaB
					// ada dan tak pernah dimigrasikan -- sekarang pakai helper yg sama spt ~100 method lain.
					tutupSessionPolaB(session);
				}
			}
		}
	}

	private static String detailTeknisError(Throwable error) {
		if (error == null) return "Tidak ada detail exception.";
		java.io.StringWriter sw = new java.io.StringWriter();
		java.io.PrintWriter pw = new java.io.PrintWriter(sw);
		error.printStackTrace(pw);
		pw.flush();
		return sw.toString();
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
	 * AKTIF -- default TIDAK_AKTIF (OFF). Berbeda dari {@code KANTIN_POS_WAJIB_SESI_KAS}
	 * yang default-nya AKTIF. Sebab: rumus
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
		java.util.Date sekarang = awalHariIni();
		for (int i = 0; i < transaksi.length(); i++) {
			try {
				JSONObject t = transaksi.getJSONObject(i);
				if (t.isNull("id")) {
					continue;
				}
				Long pid = Long.valueOf(Long.parseLong((t.get("id") + "").trim()));
				Produk p = (Produk) session.get(Produk.class, pid);
				Long jumlahBatch = (Long) session.createQuery("select count(*) from ProdukBatch where produk.id=:pid")
						.setParameter("pid", pid).uniqueResult();
				if (jumlahBatch != null && jumlahBatch.longValue() > 0) continue;
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

	/** Validasi keras stok batch aktif dan belum kedaluwarsa untuk produk yang sudah dikelola per lot. */
	private static List<String> cekKetersediaanBatchFefo(Session session, JSONArray transaksi,
			boolean bolehStokHabisToko) {
		List<String> hasil = new java.util.ArrayList<String>();
		if (bolehStokHabisToko) return hasil;
		Map<Long, Double> diminta = kelompokkanQtyProduk(transaksi);
		for (Map.Entry<Long, Double> en : diminta.entrySet()) {
			Long totalBatch = (Long) session.createQuery("select count(*) from ProdukBatch where produk.id=:pid")
					.setParameter("pid", en.getKey()).uniqueResult();
			if (totalBatch == null || totalBatch.longValue() == 0) continue;
			Double tersedia = (Double) session.createQuery("select sum(stok) from ProdukBatch where produk.id=:pid "
					+ "and status=:status and tanggalExpired>=current_date() and stok>0")
					.setParameter("pid", en.getKey()).setParameter("status", ProdukBatch.STATUS_AKTIF).uniqueResult();
			double ada = tersedia == null ? 0.0 : tersedia.doubleValue();
			if (ada + 0.000001 < en.getValue().doubleValue()) {
				Produk p = (Produk) session.get(Produk.class, en.getKey());
				// Override produk berlaku sampai lapisan batch. Sebelumnya validasi agregat sudah
				// melewatkan produk ini, tetapi validasi batch tetap menolaknya sehingga pilihan
				// "Selalu Boleh Dijual Walau Stok Minus" tampak tidak bekerja.
				if (p != null && Boolean.TRUE.equals(p.getIzinkanJualMinusStok())) continue;
				hasil.add((p == null ? ("#" + en.getKey()) : p.getNama()) + " (batch layak jual " + ada
						+ ", diminta " + en.getValue() + ")");
			}
		}
		return hasil;
	}

	private static Map<Long, Double> kelompokkanQtyProduk(JSONArray transaksi) {
		Map<Long, Double> hasil = new java.util.TreeMap<Long, Double>();
		if (transaksi == null) return hasil;
		for (int i=0;i<transaksi.length();i++) try {
			JSONObject t=transaksi.getJSONObject(i); if(t.isNull("id"))continue;
			Long id=Long.valueOf((t.get("id")+"").trim()); double qty=t.optDouble("jumlah",0); if(qty<=0)continue;
			Double lama=hasil.get(id);hasil.put(id,Double.valueOf((lama==null?0:lama.doubleValue())+qty));
		} catch(Exception e){ais.common.ErrorAuditUtil.record(e,"auto-audit KantinHelper:kelompokkanQtyProduk");}
		return hasil;
	}

	@SuppressWarnings("unchecked")
	private static void konsumsiBatchFefo(Session session, JSONArray transaksi, Long penjualanId, String oleh,
			boolean bolehStokHabisToko) {
		Map<Long, Double> diminta=kelompokkanQtyProduk(transaksi); if(diminta.isEmpty())return;
		boolean mulai=!session.getTransaction().isActive(); if(mulai)session.beginTransaction();
		try {
			for(Map.Entry<Long,Double> en:diminta.entrySet()){
				List<ProdukBatch> semua=session.createCriteria(ProdukBatch.class).add(Restrictions.eq("produk.id",en.getKey()))
						.setMaxResults(1).list(); if(semua.isEmpty())continue;
				List<ProdukBatch> batches=session.createCriteria(ProdukBatch.class).add(Restrictions.eq("produk.id",en.getKey()))
						.add(Restrictions.eq("status",ProdukBatch.STATUS_AKTIF)).add(Restrictions.ge("tanggalExpired",awalHariIni()))
						.add(Restrictions.gt("stok",Double.valueOf(0))).addOrder(Order.asc("tanggalExpired"))
						.setLockMode(org.hibernate.LockMode.UPGRADE).list();
				double sisa=en.getValue().doubleValue();
				for(ProdukBatch b:batches){if(sisa<=0)break;double ambil=Math.min(sisa,b.getStok().doubleValue());
					b.setStok(Double.valueOf(b.getStok().doubleValue()-ambil));session.saveOrUpdate(b);session.flush();
					catatMutasiBatch(session,b,"PENJUALAN",0,ambil,"PENJUALAN-"+penjualanId,"Alokasi otomatis FEFO",oleh);sisa-=ambil;}
				if(sisa>0.000001){
					Produk p=(Produk)session.get(Produk.class,en.getKey());
					if(!bolehStokHabisToko && (p==null||!Boolean.TRUE.equals(p.getIzinkanJualMinusStok())))
						throw new IllegalStateException("Stok batch berubah saat checkout untuk produk #"+en.getKey());
					// Kekurangan sengaja dibiarkan pada stok agregat (hasil recompute penjualan),
					// sedangkan batch fisik yang ada tidak dipaksa menjadi negatif.
				}
			}
			if(mulai)session.getTransaction().commit();
		}catch(RuntimeException e){if(mulai&&session.getTransaction().isActive())session.getTransaction().rollback();throw e;}
	}

	@SuppressWarnings("unchecked")
	private static void transferBatchFefo(Session session, Produk asal, Produk tujuan, double qty,
			String referensi, String oleh) {
		List<ProdukBatch> ada = session.createCriteria(ProdukBatch.class)
				.add(Restrictions.eq("produk.id", asal.getId())).setMaxResults(1).list();
		if (ada.isEmpty()) return; // produk legacy tetap mengikuti mutasi stok agregat existing
		List<ProdukBatch> sumber = session.createCriteria(ProdukBatch.class)
				.add(Restrictions.eq("produk.id", asal.getId()))
				.add(Restrictions.eq("status", ProdukBatch.STATUS_AKTIF))
				.add(Restrictions.ge("tanggalExpired", awalHariIni()))
				.add(Restrictions.gt("stok", Double.valueOf(0)))
				.addOrder(Order.asc("tanggalExpired"))
				.setLockMode(org.hibernate.LockMode.UPGRADE).list();
		double sisa = qty;
		for (ProdukBatch dari : sumber) {
			if (sisa <= 0) break;
			double pindah = Math.min(sisa, dari.getStok().doubleValue());
			List<ProdukBatch> cocok = session.createCriteria(ProdukBatch.class)
					.add(Restrictions.eq("produk.id", tujuan.getId()))
					.add(Restrictions.eq("nomorBatch", dari.getNomorBatch()))
					.add(Restrictions.eq("tanggalExpired", dari.getTanggalExpired())).setMaxResults(1).list();
			ProdukBatch ke;
			if (cocok.isEmpty()) {
				ke = new ProdukBatch(); ke.setProduk(tujuan); ke.setToko(tujuan.getToko());
				ke.setNomorBatch(dari.getNomorBatch()); ke.setTanggalProduksi(dari.getTanggalProduksi());
				ke.setTanggalExpired(dari.getTanggalExpired()); ke.setStok(Double.valueOf(0));
				ke.setHargaModal(dari.getHargaModal()); ke.setStatus(ProdukBatch.STATUS_AKTIF); ke.setOleh(oleh);
				session.save(ke); session.flush();
			} else ke = cocok.get(0);
			dari.setStok(Double.valueOf(dari.getStok().doubleValue() - pindah));
			ke.setStok(Double.valueOf(ke.getStok().doubleValue() + pindah));
			session.saveOrUpdate(dari); session.saveOrUpdate(ke); session.flush();
			catatMutasiBatch(session, dari, "MUTASI_KELUAR", 0, pindah, referensi, "Transfer antar outlet", oleh);
			catatMutasiBatch(session, ke, "MUTASI_MASUK", pindah, 0, referensi, "Transfer antar outlet", oleh);
			sisa -= pindah;
		}
		if (sisa > 0.000001) throw new IllegalArgumentException(
				"Stok batch yang aktif dan belum kedaluwarsa tidak cukup untuk ditransfer (kurang " + sisa + ").");
	}

	private static HasilValidasiStok validasiStokCukupDenganLock(JSONArray transaksi, Long tokoId,
			boolean bolehStokHabisToko) {
		if (transaksi == null || transaksi.length() == 0) {
			return null;
		}
		if (bolehStokHabisToko) return null;
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

		java.util.List<String> kurang = new java.util.ArrayList<String>();
		java.util.List<String> wajibBlokir = new java.util.ArrayList<String>();
		Session lockSession = HibernateUtil.getSessionFactory().openSession();
		try {
			lockSession.getTransaction().begin();
			for (java.util.Map.Entry<Long, Double> en : diminta.entrySet()) {
				Long pid = en.getKey();
				double qtyDiminta = en.getValue().doubleValue();
				Object[] row = (Object[]) lockSession.createSQLQuery("SELECT nama, ("
						+ ais.action.master.inventory.StokKantinUtil.formulaStokSql(pid) + "),"
						+ " izinkan_jual_minus_stok"
						+ " FROM koperasi.produk p WHERE p.id = " + pid
						+ " AND p.toko = " + tokoId + " FOR UPDATE").uniqueResult();
				if (row == null) {
					continue;
				}
				String nama = row[0] == null ? ("#" + pid) : row[0].toString();
				double stokLive = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
				Boolean overridePerItem = (row[2] instanceof Boolean) ? (Boolean) row[2] : null;
				if (stokLive < qtyDiminta && !Boolean.TRUE.equals(overridePerItem)) {
					String deskripsi = nama + " (sisa " + stokLive + ", diminta " + qtyDiminta + ")";
					kurang.add(deskripsi);
					wajibBlokir.add(deskripsi);
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
			tutupSessionPolaB(lockSession);
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

	private static String gabungkanDenganKoma(java.util.List<String> nilai) {
		StringBuilder hasil = new StringBuilder();
		if (nilai == null) return "";
		for (int i = 0; i < nilai.size(); i++) {
			if (i > 0) hasil.append(", ");
			hasil.append(nilai.get(i));
		}
		return hasil.toString();
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
					// Gap-closure "Produk Ekstra" -- total HARUS ikut hitung harga ekstra; tidak ada cek
					// stok/kadaluarsa/BOM di draft_bayar() (memang sengaja, lihat JavaDoc kelas ini) jadi
					// hanya hitungTotalDiskonCashback yang perlu versi rata di sini. `transaksi` NESTED
					// asli tetap dipakai simpanRinci di bawah (bikin baris DraftPembelian ekstra + indukId).
					JSONArray transaksiRata = ratakanTransaksiDenganEkstra(transaksi);
					TotalHitung th = hitungTotalDiskonCashback(jsonObject, transaksiRata, "draftBayarHitungTotal");
					Double total = Double.valueOf(th.total);
					Double totalDiskon = Double.valueOf(th.totalDiskon);
					Double totalCashback = Double.valueOf(th.totalCashback);

					DraftPembelianAnggotaKoperasi pembelianAnggotaKoperasi = (DraftPembelianAnggotaKoperasi) (id == null
							? null
							: session.createCriteria(DraftPembelianAnggotaKoperasi.class).add(Restrictions.idEq(id))
									.uniqueResult());
					boolean draftBaru = pembelianAnggotaKoperasi == null;
					if (pembelianAnggotaKoperasi == null) {
						pembelianAnggotaKoperasi = new DraftPembelianAnggotaKoperasi();
					}
					pembelianAnggotaKoperasi.setKeterangan(jsonObject.isNull("keterangan") ? null
							: (jsonObject.get("keterangan") + "").trim());
					pembelianAnggotaKoperasi.setMejaKantin(mejaKantin);
					pembelianAnggotaKoperasi.setTotalDiskon(totalDiskon);
					pembelianAnggotaKoperasi.setDiskon(Double.valueOf(th.nilaiDiskonFaktur));
					pembelianAnggotaKoperasi.setDiskonDalamPersen(Boolean.valueOf(th.diskonFakturPersen));
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
					int jumlahRincianDiharapkan = transaksiRata.length();
					if (arrayTransaksi.length() != jumlahRincianDiharapkan) {
						// Implementasi model lama menyimpan per item. Bila satu item gagal, jangan pernah
						// mengembalikan status sukses untuk draft parsial. Bersihkan rincian parsial agar
						// tidak dapat dimuat/dibayar sebagai keranjang yang seolah-olah lengkap.
						try {
							session.beginTransaction();
							session.createSQLQuery("update koperasi.draft_pembelian set lunas=null "
									+ "where draft_pembelian_anggota_koperasi=:draftId")
									.setParameter("draftId", pembelianAnggotaKoperasi.getId()).executeUpdate();
							session.createSQLQuery("delete from koperasi.draft_pembelian "
									+ "where draft_pembelian_anggota_koperasi=:draftId")
									.setParameter("draftId", pembelianAnggotaKoperasi.getId()).executeUpdate();
							if (draftBaru) session.delete(pembelianAnggotaKoperasi);
							session.getTransaction().commit();
						} catch (Exception bersihError) {
							if (session.getTransaction() != null && session.getTransaction().isActive()) {
								try { session.getTransaction().rollback(); } catch (Exception rollbackError) {
									ais.common.ErrorAuditUtil.record(rollbackError,
											"auto-audit KantinHelper:draftBayarBersihParsialRollback");
								}
							}
							ais.common.ErrorAuditUtil.record(bersihError,
									"auto-audit KantinHelper:draftBayarBersihParsial");
						}
						hasil.put("status", "91");
						hasil.put("description", "Rincian keranjang belum tersimpan lengkap ("
								+ arrayTransaksi.length() + " dari " + jumlahRincianDiharapkan
								+ " baris). Tidak ada draft parsial yang dipertahankan. Silakan coba tekan \"Tahan\" lagi.");
						ais.common.ErrorAuditUtil.record(
								new RuntimeException("draft_bayar: " + arrayTransaksi.length() + " dari "
										+ jumlahRincianDiharapkan
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

	/** Identitas instalasi POS wajib untuk operasi sesi kas dan pembayaran final. */
	private static String idPerangkat(JSONObject request) {
		return ais.action.master.koperasi.helper.SesiKasUtil.normalisasiIdPerangkat(
				request == null ? null : request.optString("id_perangkat", null));
	}

	private static String namaPerangkat(JSONObject request) {
		String nama = request == null ? null : request.optString("nama_perangkat", null);
		if (nama == null || nama.trim().length() == 0) nama = request == null ? null : request.optString("nama_mesin", null);
		if (nama == null) return null;
		nama = nama.trim();
		return nama.length() > 150 ? nama.substring(0, 150) : nama;
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
		String perangkat = idPerangkat(request);
		if (perangkat == null) {
			hasil.put("status", "91");
			hasil.put("terbuka", false);
			hasil.put("description", "Perangkat belum dikenali. Muat ulang halaman atau perbarui aplikasi sebelum membuka kas.");
			return;
		}
		// Logging diagnostik (SAMA gaya dgn [SESI-KAS-BUKA]) -- ditambahkan setelah laporan lapangan
		// "sesi_kas_buka membalas sukses+commit terbukti di log, tapi sesi_kas_status BERULANG KALI
		// (bukan sekadar sesaat, dicoba ulang beberapa kali dgn jeda beberapa detik s/d menit) tetap
		// melaporkan tertutup". Sebelumnya method ini SAMA SEKALI tidak mencetak apa pun, jadi tidak
		// ada cara membandingkan identitas (oleh/olehId/toko) yang dipakai QUERY BACA ini terhadap yang
		// dipakai QUERY TULIS di sesiKasBuka -- kejadian berikutnya sekarang bisa dibandingkan LANGSUNG
		// dari catalina.out utk memastikan apakah keduanya benar-benar cocok atau ada perbedaan
		// (mis. spasi/kapitalisasi/nilai berbeda) yang selama ini luput dari pengamatan.
		System.out.println("[SESI-KAS-STATUS] cek -- kasir(kasirNama=" + id[0] + ", kasirUserId=" + id[1] + "), toko=" + tokoId);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.inventory.SesiKasKasir sesi = ais.action.master.koperasi.helper.SesiKasUtil
					.sesiTerbukaPerangkat(session, id[0], id[1], tokoId, perangkat);
			// Endpoint status harus murni baca. Mengikat sesi warisan di sini membuat polling
			// status melakukan UPDATE dan dapat berbenturan dengan unique index perangkat bila
			// perangkat baru saja diklaim sesi lain. Pengikatan tetap dilakukan secara eksplisit
			// pada alur Buka/Tutup Kas yang memiliki transaksi dan penanganan konflik sendiri.
			System.out.println("[SESI-KAS-STATUS] hasil query -- " + (sesi == null ? "TIDAK DITEMUKAN (null)"
					: ("DITEMUKAN id=" + sesi.getId() + ", kasirNama=" + sesi.getKasirNama() + ", kasirUserId=" + sesi.getKasirUserId()
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
				SesiKasKasir sesiPerangkat = ais.action.master.koperasi.helper.SesiKasUtil
						.sesiTerbukaPadaPerangkat(session, tokoId, perangkat);
				SesiKasKasir sesiLain = ais.action.master.koperasi.helper.SesiKasUtil
						.sesiTerbuka(session, id[0], id[1], null);
				if (sesiPerangkat != null) {
					hasil.put("sesiDiPerangkatLain", true);
					hasil.put("namaPerangkatLain", sesiPerangkat.getNamaPerangkat());
					hasil.put("description", "Perangkat ini sedang memiliki sesi kas aktif milik "
							+ (sesiPerangkat.getKasirNama() == null ? "kasir lain" : sesiPerangkat.getKasirNama())
							+ ". Tutup sesi tersebut sebelum berganti kasir.");
				} else if (sesiLain != null) {
					hasil.put("sesiDiPerangkatLain", true);
					hasil.put("namaPerangkatLain", sesiLain.getNamaPerangkat());
					hasil.put("description", "Sesi kas akun ini sedang terbuka pada perangkat lain: "
							+ (sesiLain.getNamaPerangkat() == null ? sesiLain.getIdPerangkat() : sesiLain.getNamaPerangkat()) + ".");
				}
			} else {
				JSONObject ringkasan = ais.action.master.koperasi.helper.SesiKasUtil
						.laporanTutupKas(session, sesi, new Date(), 0);
				double modalAwal = sesi.getModalAwal() == null ? 0.0 : sesi.getModalAwal().doubleValue();
				hasil.put("terbuka", true);
				hasil.put("waktuBuka", Common.dateFormatInput.get().format(sesi.getWaktuBuka()));
				hasil.put("modalAwal", modalAwal);
				hasil.put("totalTunai", ringkasan.optDouble("penjualanTunai"));
				hasil.put("totalNonTunai", ringkasan.optDouble("penjualanNonTunai"));
				hasil.put("kasSaatIni", ringkasan.optDouble("kasSeharusnya"));
				hasil.put("ringkasanBerjalan", ringkasan);
				hasil.put("idSesiKas", sesi.getId());
				hasil.put("kodeSesiKas", sesi.getKode());
				hasil.put("kasirNama", sesi.getKasirNama());
				hasil.put("kasirUserId", sesi.getKasirUserId());
				hasil.put("tokoId", sesi.getToko() == null ? JSONObject.NULL : sesi.getToko().getId());
				hasil.put("idPerangkat", sesi.getIdPerangkat());
				hasil.put("namaPerangkat", sesi.getNamaPerangkat());
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
		String perangkat = idPerangkat(request);
		String namaPerangkatNilai = namaPerangkat(request);
		if (perangkat == null) {
			hasil.put("status", "91");
			hasil.put("description", "Perangkat belum dikenali. Muat ulang halaman atau perbarui aplikasi sebelum membuka kas.");
			return;
		}
		System.out.println("[SESI-KAS-BUKA] mulai -- kasir(kasirNama=" + id[0] + ", kasirUserId=" + id[1] + "), toko=" + tokoId
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
				if ((sudahAda.getIdPerangkat() != null && !perangkat.equals(sudahAda.getIdPerangkat())) ||
						(!id[1].equals(sudahAda.getKasirUserId()) && !id[0].equals(sudahAda.getKasirNama()))) {
					hasil.put("status", "91");
					hasil.put("description", "Kode sesi ini dimiliki akun atau perangkat lain dan tidak dapat digunakan kembali.");
					return;
				}
				if (ais.action.master.koperasi.helper.SesiKasUtil.ikatPerangkatJikaLama(
						sudahAda, perangkat, namaPerangkatNilai)) {
					session.beginTransaction();
					Common.refreshSaveOrUpdate(session, sudahAda);
					session.getTransaction().commit();
				}
				System.out.println("[SESI-KAS-BUKA] idempotensi -- kode=" + kode + " SUDAH ada, id=" + sudahAda.getId()
						+ " -- balas sukses apa adanya (bukan retry gagal, bukan duplikat).");
				hasil.put("status", "00");
				hasil.put("id_server", sudahAda.getId());
				return;
			}

			// Pemulihan aman: status klien dapat tertinggal (mis. respons status sebelumnya
			// gagal/versi lama memilih sesi warisan), lalu kasir menekan Buka Kas lagi dengan
			// kode baru. Jika akun+toko+perangkat ini sebenarnya SUDAH mempunyai sesi aktif,
			// jangan menolak dan jangan membuat sesi kedua; kembalikan sesi server yang sah.
			SesiKasKasir sesiAktifPerangkat = ais.action.master.koperasi.helper.SesiKasUtil
					.sesiTerbukaPerangkat(session, id[0], id[1], tokoId, perangkat);
			if (sesiAktifPerangkat != null) {
				if (ais.action.master.koperasi.helper.SesiKasUtil.ikatPerangkatJikaLama(
						sesiAktifPerangkat, perangkat, namaPerangkatNilai)) {
					session.beginTransaction();
					Common.refreshSaveOrUpdate(session, sesiAktifPerangkat);
					session.getTransaction().commit();
				}
				hasil.put("status", "00");
				hasil.put("dipulihkan", true);
				hasil.put("id_server", sesiAktifPerangkat.getId());
				hasil.put("kode", sesiAktifPerangkat.getKode());
				hasil.put("modalAwal", sesiAktifPerangkat.getModalAwal());
				hasil.put("description", "Sesi kas yang sudah aktif pada perangkat ini berhasil dipulihkan.");
				return;
			}

			Long idTerbuka = ais.action.master.koperasi.helper.SesiKasUtil.idSesiTerbuka(session, id[0], id[1], null);
			if (idTerbuka != null) {
				SesiKasKasir sesiAkun = (SesiKasKasir) session.get(SesiKasKasir.class, idTerbuka);
				System.out.println("[SESI-KAS-BUKA] ditolak -- sudah ada sesi terbuka id=" + idTerbuka
						+ " utk kasir/toko yg sama.");
				hasil.put("status", "91");
				hasil.put("description", "Sesi kas akun ini masih terbuka pada perangkat "
						+ (sesiAkun == null || sesiAkun.getNamaPerangkat() == null
								? (sesiAkun == null ? "lain" : sesiAkun.getIdPerangkat())
								: sesiAkun.getNamaPerangkat())
						+ ". Tutup kas pada perangkat tersebut sebelum membuka sesi baru.");
				return;
			}
			SesiKasKasir sesiPerangkat = ais.action.master.koperasi.helper.SesiKasUtil
					.sesiTerbukaPadaPerangkat(session, null, perangkat);
			if (sesiPerangkat != null) {
				hasil.put("status", "91");
				hasil.put("description", "Perangkat ini masih memiliki sesi kas milik "
						+ sesiPerangkat.getKasirNama() + ". Tutup sesi tersebut sebelum pengguna lain membuka kas.");
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
						toko, id[0], id[1], modalAwal, keterangan, kode, waktuBukaKlien,
						perangkat, namaPerangkatNilai);
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
			String pesanKonteks = "[SESI-KAS-BUKA] GAGAL -- kasir(kasirNama=" + id[0] + ", kasirUserId=" + id[1] + "), toko=" + tokoId
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
			System.out.println("[SESI-KAS-BUKA][DIAGNOSTIK] " + label + " -- kasir(kasirNama=" + id[0] + ", kasirUserId="
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
		String perangkat = idPerangkat(request);
		if (perangkat == null) {
			hasil.put("status", "91");
			hasil.put("description", "Perangkat belum dikenali. Tutup kas harus dilakukan dari perangkat yang membuka sesi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			// Cari lewat kode dulu (lebih presisi, sesi PASTI yg dimaksud klien) -- jatuh ke pencarian
			// "sesi terbuka milik kasir ini" lama bila kode tak diisi/tak ditemukan (mis. buka belum
			// sempat tersinkron -- lihat javadoc method).
			ais.database.model.inventory.SesiKasKasir sesi = kode == null ? null
					: ais.action.master.koperasi.helper.SesiKasUtil.cariByKode(session, kode);
			if (sesi != null && ((sesi.getIdPerangkat() != null && !perangkat.equals(sesi.getIdPerangkat()))
					|| (!id[1].equals(sesi.getKasirUserId()) && !id[0].equals(sesi.getKasirNama())))) {
				hasil.put("status", "91");
				hasil.put("description", "Sesi kas hanya dapat ditutup oleh akun dan perangkat yang membukanya. Hubungi supervisor jika perangkat asal tidak tersedia.");
				return;
			}
			if (sesi != null && ais.database.model.inventory.SesiKasKasir.STATUS_TUTUP.equals(sesi.getStatus())) {
				// Idempotensi: sesi ini SUDAH ditutup sebelumnya (retry sinkron krn respons hilang) --
				// balas hasil yg SUDAH tercatat apa adanya, JANGAN hitung ulang (waktu "sekarang" sudah
				// bukan waktu tutup sungguhan lagi, hasil hitung ulang akan keliru).
				hasil.put("status", "00");
				hasil.put("selisih", sesi.getSelisih());
				hasil.put("laporanTutupKas",
						ais.action.master.koperasi.helper.SesiKasUtil.laporanTersimpanAtauHitung(session, sesi));
				hasil.put("stokMenipis", daftarProdukStokMenipis(session, tokoId));
				return;
			}
			if (sesi == null) {
				sesi = ais.action.master.koperasi.helper.SesiKasUtil.sesiTerbukaPerangkat(session, id[0], id[1], tokoId, perangkat);
			}
			if (sesi == null) {
				hasil.put("status", "91");
				hasil.put("description", "Tidak ada sesi kas yang terbuka untuk ditutup.");
				return;
			}
			ais.action.master.koperasi.helper.SesiKasUtil.ikatPerangkatJikaLama(
					sesi, perangkat, namaPerangkat(request));
			double uangFisik = request.optDouble("uang_fisik", 0);
			if (Double.isNaN(uangFisik) || Double.isInfinite(uangFisik) || uangFisik < 0) {
				hasil.put("status", "91");
				hasil.put("description", "Nominal uang fisik penutupan harus berupa angka nol atau lebih.");
				return;
			}
			String keterangan = request.optString("keterangan", "");
			boolean adaKoreksiModal = !request.isNull("modal_awal_koreksi");
			boolean adaKoreksiTunai = !request.isNull("penjualan_tunai_koreksi");
			double modalAwalLama = sesi.getModalAwal() == null ? 0.0 : sesi.getModalAwal().doubleValue();
			double modalAwalKoreksi = modalAwalLama;
			Double penjualanTunaiKoreksi = null;
			String alasanKoreksi = request.optString("alasan_koreksi", "").trim();
			if (adaKoreksiModal) {
				modalAwalKoreksi = request.optDouble("modal_awal_koreksi", -1);
				if (Double.isNaN(modalAwalKoreksi) || Double.isInfinite(modalAwalKoreksi)
						|| modalAwalKoreksi < 0) {
					hasil.put("status", "91");
					hasil.put("description", "Nominal modal awal hasil koreksi harus berupa angka nol atau lebih.");
					return;
				}
			}
			if (adaKoreksiTunai) {
				double nilai = request.optDouble("penjualan_tunai_koreksi", -1);
				if (Double.isNaN(nilai) || Double.isInfinite(nilai) || nilai < 0) { hasil.put("status", "91"); hasil.put("description", "Penjualan tunai hasil koreksi harus berupa angka nol atau lebih."); return; }
				penjualanTunaiKoreksi = Double.valueOf(nilai);
			}
			if ((adaKoreksiModal || adaKoreksiTunai) && alasanKoreksi.length() < 5) { hasil.put("status", "91"); hasil.put("description", "Alasan koreksi nominal wajib diisi minimal 5 karakter untuk kebutuhan audit."); return; }
			java.util.Date waktuTutupKlien = null;
			if (!request.isNull("waktu_tutup")) {
				try {
					waktuTutupKlien = Common.dateFormatInput.get().parse(request.optString("waktu_tutup", null));
				} catch (Exception eParse) {
					waktuTutupKlien = null;
				}
			}
			session.beginTransaction();
			if (adaKoreksiModal) {
				sesi.setModalAwal(Double.valueOf(modalAwalKoreksi));
				String pelaku = tbmuser == null ? "admin" : tbmuser.getUserId();
				String catatanAudit = "[KOREKSI SUPERVISOR " + pelaku + "] Modal awal Rp "
						+ Math.round(modalAwalLama) + " menjadi Rp " + Math.round(modalAwalKoreksi)
						+ ". Alasan: " + alasanKoreksi;
				keterangan = keterangan == null || keterangan.trim().length() == 0
						? catatanAudit : keterangan.trim() + "\n" + catatanAudit;
			}
			if (adaKoreksiTunai) {
				JSONObject sebelum = ais.action.master.koperasi.helper.SesiKasUtil.laporanTutupKas(session, sesi, waktuTutupKlien == null ? new Date() : waktuTutupKlien, uangFisik);
				String pelaku = tbmuser == null ? "admin" : tbmuser.getUserId();
				String catatanAudit = "[KOREKSI SUPERVISOR " + pelaku + "] Penjualan tunai Rp " + Math.round(sebelum.optDouble("penjualanTunai")) + " menjadi Rp " + Math.round(penjualanTunaiKoreksi.doubleValue()) + ". Alasan: " + alasanKoreksi;
				keterangan = keterangan == null || keterangan.trim().length() == 0 ? catatanAudit : keterangan.trim() + "\n" + catatanAudit;
			}
			double selisih = ais.action.master.koperasi.helper.SesiKasUtil.tutup(session, sesi, uangFisik, keterangan, waktuTutupKlien, penjualanTunaiKoreksi);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("selisih", selisih);
			hasil.put("modalAwal", sesi.getModalAwal());
			hasil.put("laporanTutupKas",
					ais.action.master.koperasi.helper.SesiKasUtil.laporanTersimpanAtauHitung(session, sesi));
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
			ais.database.model.JenisPembayaran jenisPembayaran = null;
			ais.database.model.JenisTabungan jenisTabungan = null;
			if (!request.isNull("jenis_pembayaran_id")
					&& !request.optString("jenis_pembayaran_id", "").trim().isEmpty()) {
				String id = request.optString("jenis_pembayaran_id", "").trim();
				if (!Common.isNumber(id)) {
					hasil.put("status", "91");
					hasil.put("description", "Cara pembayaran yang dipilih tidak valid.");
					return;
				}
				jenisPembayaran = (ais.database.model.JenisPembayaran) session.get(
						ais.database.model.JenisPembayaran.class, Long.valueOf(id));
			} else {
				jenisPembayaran = (ais.database.model.JenisPembayaran) session
						.createCriteria(ais.database.model.JenisPembayaran.class)
						.add(Restrictions.eq("defaultPembayaran", Boolean.TRUE))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
						.add(Restrictions.isNull("jenisTabungan")).setMaxResults(1).uniqueResult();
				if (jenisPembayaran == null) {
					jenisPembayaran = (ais.database.model.JenisPembayaran) session
							.createCriteria(ais.database.model.JenisPembayaran.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
							.add(Restrictions.isNull("jenisTabungan")).addOrder(Order.asc("id"))
							.setMaxResults(1).uniqueResult();
				}
			}
			if (!request.isNull("jenis_tabungan_id")
					&& !request.optString("jenis_tabungan_id", "").trim().isEmpty()) {
				String id = request.optString("jenis_tabungan_id", "").trim();
				if (!Common.isNumber(id)) {
					hasil.put("status", "91");
					hasil.put("description", "Jenis tabungan yang dipilih tidak valid.");
					return;
				}
				jenisTabungan = (ais.database.model.JenisTabungan) session.get(
						ais.database.model.JenisTabungan.class, Long.valueOf(id));
			} else {
				jenisTabungan = (ais.database.model.JenisTabungan) session
						.createCriteria(ais.database.model.JenisTabungan.class)
						.add(Restrictions.eq("defaultTabungan", Boolean.TRUE))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
						.setMaxResults(1).uniqueResult();
				if (jenisTabungan == null) {
					jenisTabungan = (ais.database.model.JenisTabungan) session
							.createCriteria(ais.database.model.JenisTabungan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
							.addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();
				}
			}
			if (jenisPembayaran == null || jenisTabungan == null) {
				hasil.put("status", "91");
				hasil.put("description",
						"Topup belum dapat disimpan karena Cara Pembayaran atau Jenis Tabungan default belum dikonfigurasi. Buka menu Keuangan, tetapkan satu data aktif sebagai default, lalu coba kembali.");
				return;
			}
			ais.database.model.Deposit deposit = new ais.database.model.Deposit();
			deposit.setAnggotaKoperasi(anggota);
			deposit.setNominal(Double.valueOf(nominal));
			// Form admin lama selalu mengisi dua relasi wajib ini. Klien POS boleh tidak
			// mengirimkannya, tetapi server tetap memasang entity default yang dimuat
			// dari session ini (bukan singleton statis yang mungkin sudah detached).
			deposit.setJenisPembayaran(jenisPembayaran);
			deposit.setJenisTabungan(jenisTabungan);
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
			p.setKebijakanRetur(KebijakanReturApiHelper.resolveAtauBawaan(session, request));
			// Gap-closure "Jenis Item" (Produk vs Bahan Baku) -- lihat JavaDoc Produk.getJenisItem().
			if (request.has("jenis_item")) {
				String jenisItem = request.optString("jenis_item", "JUAL").trim().toUpperCase();
				p.setJenisItem(jenisItem.isEmpty() ? "JUAL" : jenisItem);
			}
			// Gap-closure "Produk Ekstra" -- JSON array id mentah (mis. "[601,602]"), disimpan apa
			// adanya, TIDAK di-snapshot spt bahan_baku -- lihat JavaDoc Produk.getEkstraPilihan().
			if (request.has("ekstra_pilihan")) {
				p.setEkstraPilihan(request.isNull("ekstra_pilihan") ? null
						: request.getJSONArray("ekstra_pilihan").toString());
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

	/** Maksimal jumlah foto per produk (gap-closure permintaan user eksplisit: "maksimal 10 foto") --
	 * ditegakkan DI SINI (server) sbg sumber kebenaran; klien juga menegakkan batas yg sama utk UX
	 * responsif, tapi validasi sungguhan HARUS di sini spy tak bisa dilewati lewat panggilan API
	 * langsung. */
	private static final int MAKS_FOTO_PRODUK = 10;

	/**
	 * Verifikasi produk ini milik toko pemanggil (IDOR-safe, pola SAMA dgn cabang update
	 * {@link #produkSimpan}) -- dipakai bersama oleh {@link #produkFotoUpload} dan
	 * {@link #produkFotoHapus}. Admin/global (pemanggil==null) selalu lolos tanpa dibatasi toko.
	 *
	 * @return {@link Produk} bila lolos verifikasi; {@code null} bila tidak (alasan penolakan
	 *         SUDAH di-put ke {@code hasil} oleh method ini, pemanggil cukup langsung {@code return}).
	 */
	private static Produk verifikasiPemilikProdukUntukFoto(Session session, Tbmuser tbmuser, Long produkId,
			JSONObject hasil) throws Exception {
		Produk p = (Produk) session.get(Produk.class, produkId);
		if (p == null) {
			hasil.put("status", "91");
			hasil.put("description", "Produk tidak ditemukan.");
			return null;
		}
		ais.database.model.inventory.Pedagang pemanggil = tbmuser == null ? null : tbmuser.getPedagang();
		boolean supervisor = pemanggil != null && Boolean.TRUE.equals(pemanggil.getSupervisor());
		if (supervisor) {
			Long tokoSupervisor = pemanggil.getToko() == null ? null : pemanggil.getToko().getId();
			Long tokoProduk = p.getToko() == null ? null : p.getToko().getId();
			if (tokoSupervisor == null || !tokoSupervisor.equals(tokoProduk)) {
				hasil.put("status", "91");
				hasil.put("description", "Produk ini bukan milik toko Anda.");
				return null;
			}
		}
		return p;
	}

	/**
	 * Daftar foto satu produk (BACA-saja, urut lama-&gt;baru = urutan unggah) -- {@code data} berisi
	 * {@code {id}} apa adanya per baris; {@link ais.action.servlet.PosApi} yang menyuntik
	 * {@code urlGambar} tiap item SETELAH method ini kembali (pola SAMA dgn
	 * {@code layar_pelanggan_slide_list}/{@code buildUrlGambarLayarPelangganSlide}) -- method ini
	 * sendiri TIDAK tahu apa pun soal URL/host.
	 *
	 * @param request payload: {@code produk_id} (wajib).
	 */
	public static void produkFotoList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (request.isNull("produk_id")) {
			hasil.put("status", "91");
			hasil.put("description", "ID produk wajib diisi.");
			return;
		}
		Long produkId = Long.valueOf((request.get("produk_id") + "").trim());
		Session streamSession = ais.database.hibernate.StreamingHibernateUtil.getInstance().openSession();
		try {
			@SuppressWarnings("unchecked")
			List<ais.database.model.file.FotoGambarProduk> daftar = streamSession
					.createCriteria(ais.database.model.file.FotoGambarProduk.class)
					.add(Restrictions.eq("produk", produkId)).addOrder(Order.asc("id")).list();
			JSONArray arr = new JSONArray();
			for (ais.database.model.file.FotoGambarProduk f : daftar) {
				JSONObject o = new JSONObject();
				o.put("id", f.getId());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			ais.database.hibernate.HibernateUtil.closeSessionQuietly(streamSession);
		}
	}

	/**
	 * Unggah satu foto produk (klien loop panggilan ini utk banyak foto sekaligus -- pola SAMA dgn
	 * {@code layarPelangganSlideUpload}: satu aksi = satu berkas, base64 di body JSON, TIDAK ada
	 * endpoint multipart terpisah di codebase ini). Menolak (status "91") bila produk ini SUDAH
	 * mencapai {@value #MAKS_FOTO_PRODUK} foto.
	 *
	 * @param request payload: {@code produk_id} (wajib), {@code file_base64} (wajib), {@code nama_file}
	 *                (opsional, default nama generik+waktu -- klien SEBAIKNYA selalu mengirim nama
	 *                asli berkas utk ekstensi yg benar).
	 */
	public static void produkFotoUpload(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggil = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobal = pemanggil == null;
		boolean supervisor = pemanggil != null && Boolean.TRUE.equals(pemanggil.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggil, adminGlobal, supervisor, "produk", "update")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola foto produk.");
			return;
		}
		if (request.isNull("produk_id")) {
			hasil.put("status", "91");
			hasil.put("description", "ID produk wajib diisi.");
			return;
		}
		Long produkId = Long.valueOf((request.get("produk_id") + "").trim());
		String base64 = request.optString("file_base64", "").trim();
		if (base64.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Berkas gambar wajib diisi.");
			return;
		}
		byte[] bytes;
		try {
			bytes = java.util.Base64.getDecoder().decode(base64);
		} catch (IllegalArgumentException eb64) {
			hasil.put("status", "91");
			hasil.put("description", "Data gambar tidak valid (base64 gagal diurai).");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			if (verifikasiPemilikProdukUntukFoto(session, tbmuser, produkId, hasil) == null) return;
		} finally {
			tutupSessionPolaB(session);
		}

		Session streamSession = ais.database.hibernate.StreamingHibernateUtil.getInstance().openSession();
		try {
			long jumlahSekarang = (Long) streamSession
					.createCriteria(ais.database.model.file.FotoGambarProduk.class)
					.add(Restrictions.eq("produk", produkId))
					.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
			if (jumlahSekarang >= MAKS_FOTO_PRODUK) {
				hasil.put("status", "91");
				hasil.put("description",
						"Maksimal " + MAKS_FOTO_PRODUK + " foto per produk. Hapus foto lama sebelum menambah yang baru.");
				return;
			}
			String namaFile = request.optString("nama_file", "").trim();
			if (namaFile.isEmpty()) {
				namaFile = "foto-" + produkId + "-" + System.currentTimeMillis() + ".jpg";
			}
			ais.database.model.file.FotoGambarProduk f = new ais.database.model.file.FotoGambarProduk();
			f.setProduk(produkId);
			f.setNama(namaFile);
			f.setOleh(tbmuser == null ? null : tbmuser.getUserNama());
			f.setOlehId(tbmuser == null ? null : String.valueOf(tbmuser.getUserId()));
			f.setFoto(org.hibernate.Hibernate.createBlob(bytes));
			streamSession.beginTransaction();
			streamSession.save(f);
			streamSession.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", f.getId());
		} finally {
			ais.database.hibernate.HibernateUtil.closeSessionQuietly(streamSession);
		}

		// Tandai Produk.adaFileGambar=true (DB UTAMA, terpisah dari blob di DB streaming di atas) --
		// gerbang dipakai PosApi.prosesKatalog utk mengisi `gambarUrl` thumbnail (lihat javadoc
		// Produk.getAdaFileGambar). Kegagalan di sini TIDAK membatalkan upload yg sudah commit di
		// atas -- foto tetap tersimpan, hanya thumbnail lama kemungkinan tak ikut nyala sampai
		// produk ini disimpan ulang lewat jalur lain; dicatat ke audit, bukan dilempar ke klien.
		try {
			Session sesiFlag = HibernateUtil.getSessionFactory().openSession();
			try {
				Produk pFlag = (Produk) sesiFlag.get(Produk.class, produkId);
				if (pFlag != null && !Boolean.TRUE.equals(pFlag.getAdaFileGambar())) {
					pFlag.setAdaFileGambar(true);
					sesiFlag.beginTransaction();
					sesiFlag.saveOrUpdate(pFlag);
					sesiFlag.getTransaction().commit();
				}
			} finally {
				tutupSessionPolaB(sesiFlag);
			}
		} catch (Exception eFlag) {
			ais.common.ErrorAuditUtil.record(eFlag, "produk-foto-upload-set-flag src/ais/action/servlet/api/KantinHelper.java:produkFotoUpload");
		}
	}

	/** Hapus satu foto produk -- gerbang+kepemilikan toko SAMA pola dgn {@link #produkFotoUpload}.
	 * @param request payload: {@code id} (wajib -- id baris {@code FotoGambarProduk}, BUKAN id produk). */
	public static void produkFotoHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggil = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobal = pemanggil == null;
		boolean supervisor = pemanggil != null && Boolean.TRUE.equals(pemanggil.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggil, adminGlobal, supervisor, "produk", "update")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola foto produk.");
			return;
		}
		if (request.isNull("id")) {
			hasil.put("status", "91");
			hasil.put("description", "ID foto wajib diisi.");
			return;
		}
		Long id = Long.valueOf((request.get("id") + "").trim());

		Session streamSession = ais.database.hibernate.StreamingHibernateUtil.getInstance().openSession();
		try {
			ais.database.model.file.FotoGambarProduk f = (ais.database.model.file.FotoGambarProduk) streamSession
					.get(ais.database.model.file.FotoGambarProduk.class, id);
			if (f == null) {
				hasil.put("status", "00");
				return;
			}
			Long produkId = f.getProduk();
			if (produkId != null) {
				Session session = HibernateUtil.getSessionFactory().openSession();
				try {
					if (verifikasiPemilikProdukUntukFoto(session, tbmuser, produkId, hasil) == null) return;
				} finally {
					tutupSessionPolaB(session);
				}
			}
			streamSession.beginTransaction();
			streamSession.delete(f);
			streamSession.getTransaction().commit();

			// Kalau ini foto TERAKHIR produk tsb, matikan Produk.adaFileGambar (pola sama dgn
			// alasan set true di produkFotoUpload) -- dicek SETELAH commit delete di atas supaya
			// hitungannya akurat (baris yg baru dihapus sudah benar-benar hilang).
			if (produkId != null) {
				long sisa = (Long) streamSession
						.createCriteria(ais.database.model.file.FotoGambarProduk.class)
						.add(Restrictions.eq("produk", produkId))
						.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
				if (sisa == 0) {
					try {
						Session sesiFlag = HibernateUtil.getSessionFactory().openSession();
						try {
							Produk pFlag = (Produk) sesiFlag.get(Produk.class, produkId);
							if (pFlag != null && Boolean.TRUE.equals(pFlag.getAdaFileGambar())) {
								pFlag.setAdaFileGambar(false);
								sesiFlag.beginTransaction();
								sesiFlag.saveOrUpdate(pFlag);
								sesiFlag.getTransaction().commit();
							}
						} finally {
							tutupSessionPolaB(sesiFlag);
						}
					} catch (Exception eFlag) {
						ais.common.ErrorAuditUtil.record(eFlag, "produk-foto-hapus-clear-flag src/ais/action/servlet/api/KantinHelper.java:produkFotoHapus");
					}
				}
			}
			hasil.put("status", "00");
		} finally {
			ais.database.hibernate.HibernateUtil.closeSessionQuietly(streamSession);
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
			FormulaEvaluator formulaEvaluator = wb.getCreationHelper().createFormulaEvaluator();
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
				double stokBaru = idx.stok >= 0 ? bacaAngkaExcel(sheet, idx.stok, r, formulaEvaluator) : 0;
				double hargaJual = idx.hargaJual >= 0 ? bacaAngkaExcel(sheet, idx.hargaJual, r, formulaEvaluator) : 0;
				double hargaBeli = idx.hargaBeli >= 0 ? bacaAngkaExcel(sheet, idx.hargaBeli, r, formulaEvaluator) : 0;

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
		String bersih = s.trim().replaceAll("[^0-9.,\\-]", "");
		int titikTerakhir = bersih.lastIndexOf('.');
		int komaTerakhir = bersih.lastIndexOf(',');
		if (titikTerakhir >= 0 && komaTerakhir >= 0) {
			// Separator paling kanan adalah desimal, yang lain pemisah ribuan.
			if (komaTerakhir > titikTerakhir) {
				bersih = bersih.replace(".", "").replace(',', '.');
			} else {
				bersih = bersih.replace(",", "");
			}
		} else if (komaTerakhir >= 0) {
			int digitBelakang = bersih.length() - komaTerakhir - 1;
			bersih = digitBelakang <= 2 ? bersih.replace(',', '.') : bersih.replace(",", "");
		} else if (titikTerakhir >= 0 && bersih.indexOf('.') != titikTerakhir) {
			bersih = bersih.replace(".", "");
		}
		if (bersih.isEmpty() || bersih.equals("-") || bersih.equals(".")) return 0;
		try {
			return Double.parseDouble(bersih);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/** Baca angka Excel termasuk formula yang belum punya cached-value terbaru. */
	private static double bacaAngkaExcel(XSSFSheet sheet, int kolom, int baris,
			FormulaEvaluator evaluator) {
		XSSFCell cell = Common.getCell(sheet, kolom, baris);
		if (cell == null) return 0;
		try {
			if (cell.getCellType() == XSSFCell.CELL_TYPE_FORMULA) {
				evaluator.evaluateFormulaCell(cell);
			}
		} catch (Exception e) {
			// Formula eksternal kadang tidak dapat dievaluasi server; cached value
			// tetap dibaca oleh Common.getCellContent sebagai jalur cadangan.
		}
		return parseAngkaAman(Common.getCellContent(cell));
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
	private static final String[] ALASAN_TAHAN_DEFAULT = new String[] {
			"Pelanggan masih memilih barang", "Pelanggan mengambil uang", "Pelanggan mengambil kartu pembayaran",
			"Pelanggan membuka aplikasi pembayaran", "Menunggu konfirmasi harga", "Menunggu pengecekan stok",
			"Menunggu persetujuan supervisor", "Menunggu data member", "Menunggu perubahan metode pembayaran",
			"Menunggu pembayaran tunai", "Menunggu pembayaran QRIS", "Menunggu pembayaran transfer",
			"Menunggu saldo member mencukupi", "Menunggu pesanan dilengkapi", "Barang perlu ditimbang ulang",
			"Barcode atau produk perlu diperiksa", "Antrean dialihkan sementara", "Pelanggan akan kembali",
			"Pesanan perlu dikonfirmasi ulang", "Kendala jaringan atau perangkat sementara" };

	/** Daftar alasan tahan per toko; konfigurasi kosong selalu kembali ke 20 alasan operasional bawaan. */
	public static JSONArray alasanTahanUntukToko(Toko toko) {
		JSONArray hasil = new JSONArray();
		String mentah = toko == null ? null : toko.getAlasanTahanJson();
		if (mentah != null && !mentah.trim().isEmpty()) {
			try {
				JSONArray tersimpan = new JSONArray(mentah);
				for (int i = 0; i < tersimpan.length(); i++) {
					String nilai = tersimpan.optString(i, "").trim();
					if (!nilai.isEmpty()) hasil.put(nilai);
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "baca konfigurasi alasan transaksi tahan");
			}
		}
		if (hasil.length() == 0) for (String nilai : ALASAN_TAHAN_DEFAULT) hasil.put(nilai);
		return hasil;
	}

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
			data.put("alasanTahan", alasanTahanUntukToko(toko));
			data.put("bolehTransaksiStokHabis", Boolean.TRUE.equals(toko.getBolehTransaksiStokHabis()));
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
			if (request.has("boleh_transaksi_stok_habis")) {
				toko.setBolehTransaksiStokHabis(Boolean.valueOf(
						request.optBoolean("boleh_transaksi_stok_habis", false)));
			}
			if (request.has("alasan_tahan") && !request.isNull("alasan_tahan")) {
				JSONArray sumber = request.getJSONArray("alasan_tahan");
				JSONArray bersih = new JSONArray();
				java.util.Set<String> unik = new java.util.LinkedHashSet<String>();
				for (int i = 0; i < sumber.length() && bersih.length() < 100; i++) {
					String nilai = sumber.optString(i, "").trim();
					if (nilai.length() > 200) nilai = nilai.substring(0, 200).trim();
					if (!nilai.isEmpty() && unik.add(nilai.toLowerCase(java.util.Locale.ENGLISH))) bersih.put(nilai);
				}
				if (bersih.length() == 0) {
					hasil.put("status", "91");
					hasil.put("description", "Daftar alasan transaksi ditahan tidak boleh kosong.");
					return;
				}
				toko.setAlasanTahanJson(bersih.toString());
			}

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

	/**
	 * Fitur "Cara Pembayaran" (CRUD admin) -- paritas dgn JSP {@code cara_bayar/index.jsp}, dipakai
	 * Desktop/Android/Flutter yang tidak punya jalur raw-SQL client-side spt JSP. Beda dgn
	 * {@link #caraBayarListSemua} (hanya {@code id,nama} utk dropdown picker) -- di sini SEMUA kolom
	 * dikembalikan utk layar manajemen.
	 */
	public static void caraBayarListAdmin(JSONObject request, JSONObject hasil) throws Exception {
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
					.prepareStatement("SELECT COUNT(*) FROM koperasi.cara_pembayaran_koperasi" + where);
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
					"SELECT id, COALESCE(kode,''), nama, COALESCE(keterangan,''), manual, online, "
							+ "COALESCE(memotong_deposit,false), COALESCE(masuk_sebagai_hutang,false), COALESCE(aktif,true), "
							+ "COALESCE(ada_kembalian, nama ILIKE '%tunai%') "
							+ "FROM koperasi.cara_pembayaran_koperasi" + where + " ORDER BY nama ASC LIMIT ? OFFSET ?");
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
				boolean manual = rs.getBoolean(5);
				j.put("manual", rs.wasNull() ? JSONObject.NULL : manual);
				j.put("online", rs.getBoolean(6));
				j.put("memotongDeposit", rs.getBoolean(7));
				j.put("masukSebagaiHutang", rs.getBoolean(8));
				j.put("aktif", rs.getBoolean(9));
				j.put("adaKembalian", rs.getBoolean(10));
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

	/** Fitur "Cara Pembayaran" -- simpan (create/update) satu baris {@code CaraPembayaranKoperasi}. */
	public static void caraBayarSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilCb = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalCb = pemanggilCb == null;
		boolean supervisorCb = pemanggilCb != null && Boolean.TRUE.equals(pemanggilCb.getSupervisor());
		String aksiCb = request.isNull("id") ? "create" : "update";
		if (!bolehAksiCrud(tbmuser, pemanggilCb, adminGlobalCb, supervisorCb, "pembayaran", aksiCb)) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola Cara Pembayaran.");
			return;
		}
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Nama cara pembayaran wajib diisi.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.koperasi.CaraPembayaranKoperasi cara;
			boolean baru = (id == null);
			if (baru) {
				cara = new ais.database.model.koperasi.CaraPembayaranKoperasi();
			} else {
				cara = (ais.database.model.koperasi.CaraPembayaranKoperasi) session
						.get(ais.database.model.koperasi.CaraPembayaranKoperasi.class, id);
				if (cara == null) {
					hasil.put("status", "91");
					hasil.put("description", "Cara pembayaran tidak ditemukan.");
					return;
				}
			}
			cara.setKode(request.optString("kode", ""));
			cara.setNama(nama);
			cara.setKeterangan(request.optString("keterangan", ""));
			if (request.has("manual")) {
				cara.setManual(request.isNull("manual") ? null : Boolean.valueOf(request.optBoolean("manual")));
			}
			cara.setOnline(request.optBoolean("online", false));
			cara.setMemotongDeposit(Boolean.valueOf(request.optBoolean("memotongDeposit", false)));
			cara.setMasukSebagaiHutang(Boolean.valueOf(request.optBoolean("masukSebagaiHutang", false)));
			if (request.has("adaKembalian")) {
				cara.setAdaKembalian(request.isNull("adaKembalian") ? null : Boolean.valueOf(request.optBoolean("adaKembalian")));
			}
			cara.setAktif(!request.has("aktif") || request.optBoolean("aktif", true));

			session.beginTransaction();
			session.saveOrUpdate(cara);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", cara.getId());
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** Fitur "Cara Pembayaran" -- hapus satu baris {@code CaraPembayaranKoperasi}. */
	public static void caraBayarHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilCh = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalCh = pemanggilCh == null;
		boolean supervisorCh = pemanggilCh != null && Boolean.TRUE.equals(pemanggilCh.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggilCh, adminGlobalCh, supervisorCh, "pembayaran", "delete")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat menghapus Cara Pembayaran.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "ID cara pembayaran wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.koperasi.CaraPembayaranKoperasi cara = (ais.database.model.koperasi.CaraPembayaranKoperasi) session
					.get(ais.database.model.koperasi.CaraPembayaranKoperasi.class, id);
			if (cara == null) {
				hasil.put("status", "91");
				hasil.put("description", "Cara pembayaran tidak ditemukan.");
				return;
			}
			session.beginTransaction();
			session.delete(cara);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} catch (Exception eHapusCara) {
			hasil.put("status", "91");
			hasil.put("description", "Gagal menghapus: metode pembayaran ini sudah dipakai di transaksi. Nonaktifkan saja, jangan dihapus.");
			ais.common.ErrorAuditUtil.record(eHapusCara, "auto-audit caraBayarHapus src/ais/action/servlet/api/KantinHelper.java");
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
							+ "(SELECT COUNT(*) FROM koperasi.anggota_koperasi a WHERE a.tipe_anggota_koperasi = t.id), "
							+ "COALESCE(t.maksimal_boleh_utang,0) "
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
				j.put("maksimalBolehUtang", rs.getDouble(7));
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
			if (request.has("maksimalBolehUtang") && !request.isNull("maksimalBolehUtang")) {
				tipe.setMaksimalBolehUtang(request.optDouble("maksimalBolehUtang", 0.0));
			}

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
	 * Fitur "Mutasi Tabungan" (tab {@code anggota.jsp} / layar Mutasi Tabungan Desktop-Android) --
	 * versi Java dari query UNION ALL raw-SQL client-side di {@code _mutasi_tabungan.jsp} (JSP admin
	 * boleh raw-SQL client-side, Desktop/Android/Flutter TIDAK -- makanya perlu method khusus ini).
	 * Saldo berjalan (window function) dihitung atas SELURUH baris dlm rentang tanggal dulu, BARU
	 * dipotong halaman -- makanya {@code total} bisa lebih besar dari {@code page_size} tapi
	 * paginasi dilakukan di CLIENT (pola sama dgn Pesanan/Produk: server flat-list, client paging),
	 * supaya "Saldo Per Penabung"/"Saldo Total" tetap identik dgn urutan baris yg ditampilkan.
	 *
	 * @param request payload berisi {@code dari}/{@code sampai} (wajib, "yyyy-MM-dd"),
	 *                {@code id_anggota} (opsional).
	 */
	public static void mutasiTabunganList(JSONObject request, JSONObject hasil) throws Exception {
		String dari = request.optString("dari", "").trim();
		String sampai = request.optString("sampai", "").trim();
		if (dari.isEmpty() || sampai.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Tanggal Mulai dan Tanggal Akhir wajib diisi.");
			return;
		}
		Long idAnggota = request.isNull("id_anggota") ? null : Long.valueOf((request.get("id_anggota") + "").trim());

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String filterAnggotaDeposit = idAnggota != null ? " AND d.anggota_koperasi = ? " : "";
			String filterAnggotaPembelian = idAnggota != null ? " AND p.anggota_koperasi = ? " : "";
			String filterAnggotaPencairan = idAnggota != null ? " AND pc.anggota_koperasi = ? " : "";

			String sql = "WITH semua_mutasi AS ( "
					+ "  SELECT d.waktu AS waktu, ('D' || d.id) AS baris_id, (a.kode || ' - ' || a.nama) AS nama_anggota, d.anggota_koperasi AS id_anggota, "
					+ "         CASE WHEN d.nominal >= 0 THEN 'Topup Tabungan' ELSE 'Pengeluaran Manual' END AS jenis_mutasi, "
					+ "         COALESCE(d.keterangan, '') AS keterangan, GREATEST(d.nominal, 0) AS masuk, GREATEST(-d.nominal, 0) AS keluar "
					+ "  FROM public.deposit d JOIN koperasi.anggota_koperasi a ON d.anggota_koperasi = a.id "
					+ "  WHERE d.anggota_koperasi IS NOT NULL " + filterAnggotaDeposit
					+ "  UNION ALL "
					+ "  SELECT p.waktu, ('P' || p.id), (a.kode || ' - ' || a.nama), p.anggota_koperasi, "
					+ "         'Pembelian/Belanja' AS jenis_mutasi, COALESCE(p.kode, '') AS keterangan, 0 AS masuk, COALESCE(p.total, 0) AS keluar "
					+ "  FROM koperasi.pembelian p "
					+ "  JOIN koperasi.anggota_koperasi a ON p.anggota_koperasi = a.id "
					+ "  JOIN koperasi.cara_pembayaran_koperasi cpk ON p.cara_pembayaran_koperasi = cpk.id "
					+ "  WHERE p.anggota_koperasi IS NOT NULL AND (cpk.manual = false OR cpk.memotong_deposit = true) "
					+ filterAnggotaPembelian
					+ "  UNION ALL "
					+ "  SELECT pc.waktu_pencairan, ('C' || pc.id), (a.kode || ' - ' || a.nama), pc.anggota_koperasi, "
					+ "         'Cashback Diskon' AS jenis_mutasi, 'Pencairan diskon' AS keterangan, COALESCE(pc.nominal_cair, 0) AS masuk, 0 AS keluar "
					+ "  FROM koperasi.pencairan_diskon pc "
					+ "  JOIN koperasi.anggota_koperasi a ON pc.anggota_koperasi = a.id "
					+ "  JOIN koperasi.cara_pembayaran_koperasi cpk2 ON pc.cara_pembayaran = cpk2.id "
					+ "  WHERE pc.status = 'BERHASIL' AND cpk2.manual = false "
					+ filterAnggotaPencairan
					+ "), saldo_awal AS ( "
					+ "  SELECT id_anggota, SUM(masuk-keluar) AS saldo_awal FROM semua_mutasi WHERE waktu < ?::date GROUP BY id_anggota "
					+ "), mutasi AS ( "
					+ "  SELECT * FROM semua_mutasi WHERE waktu >= ?::date AND waktu < (?::date + interval '1 day') "
					+ ") "
					+ "SELECT m.waktu, m.baris_id, m.nama_anggota, m.id_anggota, m.jenis_mutasi, m.keterangan, m.masuk, m.keluar, "
					+ "  COALESCE(sa.saldo_awal,0) + SUM(m.masuk - m.keluar) OVER (PARTITION BY m.id_anggota ORDER BY m.waktu, m.baris_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS saldo_per_penabung, "
					+ "  (SELECT COALESCE(SUM(saldo_awal),0) FROM saldo_awal) + SUM(m.masuk - m.keluar) OVER (ORDER BY m.waktu, m.baris_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS saldo_total, "
					+ "  COALESCE(sa.saldo_awal,0) AS saldo_awal "
					+ "FROM mutasi m LEFT JOIN saldo_awal sa ON sa.id_anggota=m.id_anggota ORDER BY m.waktu ASC, m.baris_id ASC LIMIT 3000";

			java.sql.PreparedStatement ps = session.connection().prepareStatement(sql);
			int idx = 1;
			if (idAnggota != null) ps.setLong(idx++, idAnggota);
			if (idAnggota != null) ps.setLong(idx++, idAnggota);
			if (idAnggota != null) ps.setLong(idx++, idAnggota);
			ps.setString(idx++, dari);
			ps.setString(idx++, dari);
			ps.setString(idx++, sampai);

			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			java.text.SimpleDateFormat sdfWaktu = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			while (rs.next()) {
				JSONObject j = new JSONObject();
				java.sql.Timestamp waktu = rs.getTimestamp(1);
				j.put("waktu", waktu == null ? JSONObject.NULL : sdfWaktu.format(waktu));
				j.put("barisId", rs.getString(2));
				j.put("namaAnggota", rs.getString(3));
				long idA = rs.getLong(4);
				j.put("idAnggota", rs.wasNull() ? JSONObject.NULL : idA);
				j.put("jenisMutasi", rs.getString(5));
				j.put("keterangan", rs.getString(6));
				j.put("masuk", rs.getDouble(7));
				j.put("keluar", rs.getDouble(8));
				j.put("saldoPerPenabung", rs.getDouble(9));
				j.put("saldoTotal", rs.getDouble(10));
				j.put("saldoAwal", rs.getDouble(11));
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", arr.length());
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Mutasi Hutang" (tab {@code anggota.jsp} / layar Mutasi Hutang Desktop-Android) -- versi
	 * Java dari query UNION ALL raw-SQL client-side di {@code _mutasi_hutang.jsp}. DEBIT ("Hutang
	 * Bertambah") dihitung per SLOT split-pembayaran {@code koperasi.pembelian_anggota_koperasi} yg
	 * cara-bayarnya ditandai {@link ais.database.model.koperasi.CaraPembayaranKoperasi#getMasukSebagaiHutang()},
	 * KREDIT ("Pembayaran") dari {@link ais.database.model.koperasi.PembayaranHutang}. Lihat JavaDoc
	 * {@link #mutasiTabunganList} soal alasan flat-list (bukan server-paginated).
	 */
	public static void mutasiHutangList(JSONObject request, JSONObject hasil) throws Exception {
		String dari = request.optString("dari", "").trim();
		String sampai = request.optString("sampai", "").trim();
		if (dari.isEmpty() || sampai.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Tanggal Mulai dan Tanggal Akhir wajib diisi.");
			return;
		}
		Long idAnggota = request.isNull("id_anggota") ? null : Long.valueOf((request.get("id_anggota") + "").trim());

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String n1 = "GREATEST(0, COALESCE(h.total_biaya,0) - COALESCE(h.nominal_bayar_2,0) - COALESCE(h.nominal_bayar_3,0) - COALESCE(h.nominal_bayar_4,0) - COALESCE(h.nominal_bayar_5,0))";
			String filterH = idAnggota != null ? " AND h.anggota_koperasi = ? " : "";
			String filterPh = idAnggota != null ? " AND ph.anggota_koperasi = ? " : "";

			StringBuilder sql = new StringBuilder();
			sql.append("WITH semua_mutasi AS ( ");
			String[] slotJoin = { "h.cara_pembayaran_koperasi", "h.cara_pembayaran_koperasi_2",
					"h.cara_pembayaran_koperasi_3", "h.cara_pembayaran_koperasi_4", "h.cara_pembayaran_koperasi_5" };
			String[] slotNominal = { n1, "COALESCE(h.nominal_bayar_2,0)", "COALESCE(h.nominal_bayar_3,0)",
					"COALESCE(h.nominal_bayar_4,0)", "COALESCE(h.nominal_bayar_5,0)" };
			for (int slot = 1; slot <= 5; slot++) {
				if (slot > 1) sql.append(" UNION ALL ");
				sql.append("  SELECT h.tanggal_pembayaran AS waktu, ('H").append(slot).append("' || h.id) AS baris_id, "
						+ "(a.kode || ' - ' || a.nama) AS nama_anggota, h.anggota_koperasi AS id_anggota, "
						+ "'Belanja (Hutang)' AS jenis_mutasi, COALESCE(h.kode, '') AS keterangan, ")
						.append(slotNominal[slot - 1]).append(" AS bertambah, 0 AS berkurang "
						+ "FROM koperasi.pembelian_anggota_koperasi h "
						+ "JOIN koperasi.anggota_koperasi a ON h.anggota_koperasi = a.id "
						+ "JOIN koperasi.cara_pembayaran_koperasi cpk").append(slot).append(" ON ")
						.append(slotJoin[slot - 1]).append(" = cpk").append(slot).append(".id "
						+ "WHERE cpk").append(slot).append(".masuk_sebagai_hutang = true AND h.anggota_koperasi IS NOT NULL AND ")
						.append(slotNominal[slot - 1]).append(" > 0 ").append(filterH);
			}
			sql.append(" UNION ALL ")
					.append("  SELECT ph.waktu, ('C' || ph.id), (a.kode || ' - ' || a.nama), ph.anggota_koperasi, "
							+ "'Pembayaran Hutang', COALESCE(ph.keterangan, ''), 0, COALESCE(ph.nominal, 0) "
							+ "FROM koperasi.pembayaran_hutang ph "
							+ "JOIN koperasi.anggota_koperasi a ON ph.anggota_koperasi = a.id "
							+ "WHERE ph.anggota_koperasi IS NOT NULL ").append(filterPh)
					.append("), saldo_awal AS ( "
							+ "SELECT id_anggota, SUM(bertambah-berkurang) AS saldo_awal FROM semua_mutasi WHERE waktu < ?::date GROUP BY id_anggota"
							+ "), mutasi AS (SELECT * FROM semua_mutasi WHERE waktu >= ?::date AND waktu < (?::date + interval '1 day')) "
							+ "SELECT waktu, baris_id, nama_anggota, m.id_anggota, jenis_mutasi, keterangan, bertambah, berkurang, "
							+ "  COALESCE(sa.saldo_awal,0) + SUM(bertambah - berkurang) OVER (PARTITION BY m.id_anggota ORDER BY waktu, baris_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS saldo_per_anggota, "
							+ "  (SELECT COALESCE(SUM(saldo_awal),0) FROM saldo_awal) + SUM(bertambah - berkurang) OVER (ORDER BY waktu, baris_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS saldo_total, "
							+ "  COALESCE(sa.saldo_awal,0) AS saldo_awal "
							+ "FROM mutasi m LEFT JOIN saldo_awal sa ON sa.id_anggota=m.id_anggota ORDER BY waktu ASC, baris_id ASC LIMIT 3000");

			java.sql.PreparedStatement ps = session.connection().prepareStatement(sql.toString());
			int idx = 1;
			for (int slot = 1; slot <= 5; slot++) {
				if (idAnggota != null) ps.setLong(idx++, idAnggota);
			}
			if (idAnggota != null) ps.setLong(idx++, idAnggota);
			ps.setString(idx++, dari);
			ps.setString(idx++, dari);
			ps.setString(idx++, sampai);

			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			java.text.SimpleDateFormat sdfWaktu = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			while (rs.next()) {
				JSONObject j = new JSONObject();
				java.sql.Timestamp waktu = rs.getTimestamp(1);
				j.put("waktu", waktu == null ? JSONObject.NULL : sdfWaktu.format(waktu));
				j.put("barisId", rs.getString(2));
				j.put("namaAnggota", rs.getString(3));
				long idA = rs.getLong(4);
				j.put("idAnggota", rs.wasNull() ? JSONObject.NULL : idA);
				j.put("jenisMutasi", rs.getString(5));
				j.put("keterangan", rs.getString(6));
				j.put("bertambah", rs.getDouble(7));
				j.put("berkurang", rs.getDouble(8));
				j.put("saldoPerAnggota", rs.getDouble(9));
				j.put("saldoTotal", rs.getDouble(10));
				j.put("saldoAwal", rs.getDouble(11));
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", arr.length());
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Total hutang berjalan SATU anggota per SAAT INI -- dipakai gerbang batas
	 * {@link ais.database.model.koperasi.TipeAnggotaKoperasi#getMaksimalBolehUtang()} saat checkout
	 * (lihat {@link #cekBatasHutang}) maupun ditampilkan di kartu ringkasan anggota. Pola query SAMA
	 * dgn {@link #mutasiHutangList} tapi tanpa rentang tanggal (SEMUA histori) dan hasil di-SUM
	 * langsung di SQL, bukan per-baris.
	 */
	public static double hitungTotalHutangBerjalan(Session session, long idAnggota) throws Exception {
		String n1 = "GREATEST(0, COALESCE(h.total_biaya,0) - COALESCE(h.nominal_bayar_2,0) - COALESCE(h.nominal_bayar_3,0) - COALESCE(h.nominal_bayar_4,0) - COALESCE(h.nominal_bayar_5,0))";
		String sql = "SELECT COALESCE(( "
				+ "  SELECT SUM(CASE WHEN cpk1.masuk_sebagai_hutang = true THEN " + n1 + " ELSE 0 END "
				+ "    + CASE WHEN cpk2.masuk_sebagai_hutang = true THEN COALESCE(h.nominal_bayar_2,0) ELSE 0 END "
				+ "    + CASE WHEN cpk3.masuk_sebagai_hutang = true THEN COALESCE(h.nominal_bayar_3,0) ELSE 0 END "
				+ "    + CASE WHEN cpk4.masuk_sebagai_hutang = true THEN COALESCE(h.nominal_bayar_4,0) ELSE 0 END "
				+ "    + CASE WHEN cpk5.masuk_sebagai_hutang = true THEN COALESCE(h.nominal_bayar_5,0) ELSE 0 END) "
				+ "  FROM koperasi.pembelian_anggota_koperasi h "
				+ "  LEFT JOIN koperasi.cara_pembayaran_koperasi cpk1 ON h.cara_pembayaran_koperasi = cpk1.id "
				+ "  LEFT JOIN koperasi.cara_pembayaran_koperasi cpk2 ON h.cara_pembayaran_koperasi_2 = cpk2.id "
				+ "  LEFT JOIN koperasi.cara_pembayaran_koperasi cpk3 ON h.cara_pembayaran_koperasi_3 = cpk3.id "
				+ "  LEFT JOIN koperasi.cara_pembayaran_koperasi cpk4 ON h.cara_pembayaran_koperasi_4 = cpk4.id "
				+ "  LEFT JOIN koperasi.cara_pembayaran_koperasi cpk5 ON h.cara_pembayaran_koperasi_5 = cpk5.id "
				+ "  WHERE h.anggota_koperasi = ? "
				+ "),0) - COALESCE(( "
				+ "  SELECT SUM(ph.nominal) FROM koperasi.pembayaran_hutang ph WHERE ph.anggota_koperasi = ? "
				+ "),0) AS saldo_hutang";
		java.sql.PreparedStatement ps = session.connection().prepareStatement(sql);
		ps.setLong(1, idAnggota);
		ps.setLong(2, idAnggota);
		java.sql.ResultSet rs = ps.executeQuery();
		double saldo = rs.next() ? rs.getDouble(1) : 0.0;
		rs.close();
		ps.close();
		return Math.max(0.0, saldo);
	}

	/**
	 * Fitur "Bayar Hutang" (form entri di layar Mutasi Hutang) -- simpan (create/update) satu baris
	 * {@link ais.database.model.koperasi.PembayaranHutang}. Gerbang SAMA dgn {@link #topupSaldo}
	 * ({@code Tbmrole.bolehEntryTopup}) -- entri finansial manual, bukan bagian grid CRUD menu.
	 */
	public static void hutangBayarSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.Tbmrole roleHb = tbmuser == null ? null : tbmuser.hakAkses();
		boolean bolehHb = roleHb != null && roleHb.getBolehEntryTopup() != null && roleHb.getBolehEntryTopup().booleanValue();
		if (!bolehHb) {
			hasil.put("status", "91");
			hasil.put("description", "Anda tidak memiliki hak akses untuk mencatat pembayaran hutang.");
			return;
		}
		Long idAnggota = request.isNull("id_member") ? null : Long.valueOf((request.get("id_member") + "").trim());
		if (idAnggota == null) {
			hasil.put("status", "91");
			hasil.put("description", "Anggota wajib dipilih.");
			return;
		}
		double nominal = request.optDouble("nominal", 0);
		if (nominal <= 0) {
			hasil.put("status", "91");
			hasil.put("description", "Nominal pembayaran harus lebih dari 0.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.koperasi.PembayaranHutang bayar;
			if (id == null) {
				bayar = new ais.database.model.koperasi.PembayaranHutang();
				bayar.setAnggotaKoperasi((ais.database.model.koperasi.AnggotaKoperasi) session
						.get(ais.database.model.koperasi.AnggotaKoperasi.class, idAnggota));
			} else {
				bayar = (ais.database.model.koperasi.PembayaranHutang) session
						.get(ais.database.model.koperasi.PembayaranHutang.class, id);
				if (bayar == null) {
					hasil.put("status", "91");
					hasil.put("description", "Data pembayaran hutang tidak ditemukan.");
					return;
				}
			}
			bayar.setNominal(Double.valueOf(nominal));
			bayar.setKeterangan(request.optString("keterangan", ""));
			if (!request.isNull("waktu") && !request.optString("waktu", "").trim().isEmpty()) {
				try {
					bayar.setWaktu(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(request.optString("waktu")));
				} catch (Exception eParse) {
					ais.common.ErrorAuditUtil.record(eParse, "auto-audit hutangBayarSimpan-parse-waktu src/ais/action/servlet/api/KantinHelper.java");
				}
			} else if (id == null) {
				bayar.setWaktu(ais.ui.util.WaktuUtil.getDate());
			}
			session.beginTransaction();
			session.saveOrUpdate(bayar);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", bayar.getId());
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fitur "Bayar Hutang" -- hapus satu baris {@link ais.database.model.koperasi.PembayaranHutang}.
	 * Gerbang SAMA dgn {@link #hutangBayarSimpan}.
	 */
	public static void hutangBayarHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.Tbmrole roleHh = tbmuser == null ? null : tbmuser.hakAkses();
		boolean bolehHh = roleHh != null && roleHh.getBolehEntryTopup() != null && roleHh.getBolehEntryTopup().booleanValue();
		if (!bolehHh) {
			hasil.put("status", "91");
			hasil.put("description", "Anda tidak memiliki hak akses untuk menghapus entri pembayaran hutang.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "ID pembayaran hutang wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.koperasi.PembayaranHutang bayar = (ais.database.model.koperasi.PembayaranHutang) session
					.get(ais.database.model.koperasi.PembayaranHutang.class, id);
			if (bayar == null) {
				hasil.put("status", "91");
				hasil.put("description", "Data pembayaran hutang tidak ditemukan.");
				return;
			}
			session.beginTransaction();
			session.delete(bayar);
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
	 * Fitur "Sinkronisasi Master Data Sivitas" -- jalankan sinkron Dosen -&gt; AnggotaKoperasi, padanan
	 * {@link #sinkronMahasiswa} utk dosen. Sama gerbang &amp; sumber logika (lihat JavaDoc
	 * {@link #sinkronMahasiswa}); reuse {@code Common.checkApakahDosenOtomatisMenjadiAnggotaKoperasi}
	 * (satu sumber logika dgn tombol "Singkronkan Dosen" ZK {@code AnggotaKoperasiAction}).
	 *
	 * @param request payload berisi {@code koperasi_id} (wajib), {@code fakultas_id}/{@code jurusan_id}
	 *                (opsional, filter kandidat via Dosen.jurusan).
	 */
	public static void sinkronDosen(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilSd = tbmuser == null ? null : tbmuser.getPedagang();
		boolean bolehSd = pemanggilSd == null || Boolean.TRUE.equals(pemanggilSd.getSupervisor());
		if (!bolehSd) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/supervisor toko yang boleh menjalankan sinkronisasi.");
			return;
		}
		if (request.isNull("koperasi_id")) {
			hasil.put("status", "91");
			hasil.put("description", "Koperasi wajib diisi.");
			return;
		}
		Long koperasiId = Long.valueOf((request.get("koperasi_id") + "").trim());
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
			org.hibernate.Criteria cq = session.createCriteria(ais.database.model.Dosen.class)
					.createAlias("jurusan", "jurusan", org.hibernate.Criteria.LEFT_JOIN)
					.add(Restrictions.ne("nidn", "")).add(Restrictions.isNotNull("nidn"))
					.setProjection(org.hibernate.criterion.Projections.groupProperty("nidn"));
			if (jurusanEntity != null) cq.add(Restrictions.eq("jurusan", jurusanEntity));
			if (fakultasEntity != null) cq.add(Restrictions.eq("jurusan.fakultas", fakultasEntity));
			@SuppressWarnings("unchecked")
			java.util.List<String> ids = cq.list();

			int berhasil = 0, gagal = 0;
			for (String nidn : ids) {
				try {
					if (Common.checkApakahDosenOtomatisMenjadiAnggotaKoperasi(nidn, koperasi) != null) berhasil++;
				} catch (Exception exSatu) {
					gagal++;
					ais.common.ErrorAuditUtil.record(exSatu,
							"auto-audit sinkronDosen src/ais/action/servlet/api/KantinHelper.java nidn=" + nidn);
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
	 * Fitur "Sinkronisasi Master Data Sivitas" -- jalankan sinkron Guru -&gt; AnggotaKoperasi, padanan
	 * {@link #sinkronSiswa} utk guru. Sama gerbang &amp; sumber logika (lihat JavaDoc
	 * {@link #sinkronMahasiswa}); reuse {@code Common.checkApakahGuruOtomatisMenjadiAnggotaKoperasi}.
	 *
	 * @param request payload berisi {@code koperasi_id} (wajib), {@code yayasan_id}/{@code sekolah_id}
	 *                (opsional, filter kandidat).
	 */
	public static void sinkronGuru(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilSg = tbmuser == null ? null : tbmuser.getPedagang();
		boolean bolehSg = pemanggilSg == null || Boolean.TRUE.equals(pemanggilSg.getSupervisor());
		if (!bolehSg) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/supervisor toko yang boleh menjalankan sinkronisasi.");
			return;
		}
		if (request.isNull("koperasi_id")) {
			hasil.put("status", "91");
			hasil.put("description", "Koperasi wajib diisi.");
			return;
		}
		Long koperasiId = Long.valueOf((request.get("koperasi_id") + "").trim());
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
			org.hibernate.Criteria cq = session.createCriteria(ais.database.model.sekolah.Guru.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.ne("nip", "")).add(Restrictions.isNotNull("nip"))
					.setProjection(org.hibernate.criterion.Projections.groupProperty("nip"));
			if (sekolahEntity != null) cq.add(Restrictions.eq("sekolah", sekolahEntity));
			if (yayasanEntity != null) cq.add(Restrictions.eq("yayasan", yayasanEntity));
			@SuppressWarnings("unchecked")
			java.util.List<String> ids = cq.list();

			int berhasil = 0, gagal = 0;
			for (String nip : ids) {
				try {
					if (Common.checkApakahGuruOtomatisMenjadiAnggotaKoperasi(nip, koperasi) != null) berhasil++;
				} catch (Exception exSatu) {
					gagal++;
					ais.common.ErrorAuditUtil.record(exSatu,
							"auto-audit sinkronGuru src/ais/action/servlet/api/KantinHelper.java nip=" + nip);
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
	 * Fitur "Sinkronisasi Master Data Sivitas" -- jalankan sinkron Pegawai -&gt; AnggotaKoperasi,
	 * padanan {@link #sinkronMahasiswa} utk pegawai umum. Sama gerbang &amp; sumber logika (lihat
	 * JavaDoc {@link #sinkronMahasiswa}); reuse {@code Common.checkApakahPegawaiOtomatisMenjadiAnggotaKoperasi}
	 * (satu sumber logika dgn tombol "Singkronkan Pegawai" ZK {@code AnggotaKoperasiAction}).
	 *
	 * <p><b>Dedup thd Dosen/Guru (PENTING).</b> Kandidat pegawai SENGAJA mengecualikan baris yang
	 * sudah tertaut ke {@code dosen} ATAU {@code guru} -- pegawai yang sebenarnya seorang dosen/guru
	 * HANYA disinkronkan lewat jalur {@link #sinkronDosen}/{@link #sinkronGuru} (kunci NIDN/NIP),
	 * tidak boleh terhitung dobel lewat jalur pegawai umum (kunci {@code mycode}). Versi ZK
	 * {@code AnggotaKoperasiAction} lama HANYA mengecualikan {@code dosen} (bukan {@code guru}) --
	 * celah itu ditutup di sini sekaligus.</p>
	 *
	 * <p><b>Pegawai tanpa kode.</b> Pegawai dengan {@code mycode} null/kosong TETAP disinkronkan
	 * (bukan dilewati) lewat overload {@code Common.checkApakahPegawaiOtomatisMenjadiAnggotaKoperasi(Long, Koperasi)}
	 * yang mencocokkan via FK {@code pegawai.id} (bukan {@code mycode}) dan men-generate kode
	 * member otomatis pada saat pertama kali dibuat.</p>
	 *
	 * @param request payload berisi {@code koperasi_id} (wajib).
	 */
	public static void sinkronPegawai(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilSp = tbmuser == null ? null : tbmuser.getPedagang();
		boolean bolehSp = pemanggilSp == null || Boolean.TRUE.equals(pemanggilSp.getSupervisor());
		if (!bolehSp) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/supervisor toko yang boleh menjalankan sinkronisasi.");
			return;
		}
		if (request.isNull("koperasi_id")) {
			hasil.put("status", "91");
			hasil.put("description", "Koperasi wajib diisi.");
			return;
		}
		Long koperasiId = Long.valueOf((request.get("koperasi_id") + "").trim());

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.koperasi.Koperasi koperasi = (ais.database.model.koperasi.Koperasi) session
					.get(ais.database.model.koperasi.Koperasi.class, koperasiId);
			if (koperasi == null) {
				hasil.put("status", "91");
				hasil.put("description", "Koperasi tidak ditemukan.");
				return;
			}
			org.hibernate.Criteria cq = session.createCriteria(ais.database.model.Pegawai.class)
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.add(Restrictions.isNull("dosen")).add(Restrictions.isNull("guru"))
					.add(Restrictions.ne("mycode", "")).add(Restrictions.isNotNull("mycode"))
					.setProjection(org.hibernate.criterion.Projections.groupProperty("mycode"));
			@SuppressWarnings("unchecked")
			java.util.List<String> ids = cq.list();

			// Pegawai TANPA kode (mycode null/kosong) -- permintaan eksplisit user 2026-08-12:
			// jangan dilewati begitu saja, tetap ambil, kode di-generate otomatis (lihat
			// Common.checkApakahPegawaiOtomatisMenjadiAnggotaKoperasi(Long, Koperasi)). Diambil
			// per-id (BUKAN group-by mycode spt di atas -- mycode kosong akan collapse SEMUA
			// pegawai tanpa kode jadi satu grup, cuma 1 yg kesinkron).
			org.hibernate.Criteria cqTanpaKode = session.createCriteria(ais.database.model.Pegawai.class)
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.add(Restrictions.isNull("dosen")).add(Restrictions.isNull("guru"))
					.add(Restrictions.or(Restrictions.isNull("mycode"), Restrictions.eq("mycode", "")))
					.setProjection(org.hibernate.criterion.Projections.property("id"));
			@SuppressWarnings("unchecked")
			java.util.List<Long> idsTanpaKode = cqTanpaKode.list();

			int berhasil = 0, gagal = 0;
			for (String mycode : ids) {
				try {
					if (Common.checkApakahPegawaiOtomatisMenjadiAnggotaKoperasi(mycode, koperasi) != null) berhasil++;
				} catch (Exception exSatu) {
					gagal++;
					ais.common.ErrorAuditUtil.record(exSatu,
							"auto-audit sinkronPegawai src/ais/action/servlet/api/KantinHelper.java mycode=" + mycode);
				}
			}
			for (Long idPegawai : idsTanpaKode) {
				try {
					if (Common.checkApakahPegawaiOtomatisMenjadiAnggotaKoperasi(idPegawai, koperasi) != null) berhasil++;
				} catch (Exception exSatu) {
					gagal++;
					ais.common.ErrorAuditUtil.record(exSatu, "auto-audit sinkronPegawai(tanpa-kode) "
							+ "src/ais/action/servlet/api/KantinHelper.java idPegawai=" + idPegawai);
				}
			}
			hasil.put("status", "00");
			hasil.put("total", ids.size() + idsTanpaKode.size());
			hasil.put("berhasil", berhasil);
			hasil.put("gagal", gagal);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Gerbang akses fitur Screensaver Layar Pelanggan (menu Konfigurasi tab baru) -- SAMA persis
	 * pola "admin ATAU supervisor toko" yang sudah dipakai {@code tokoProfilSimpan} (tab Profil
	 * Toko di layar yang sama), bukan {@code bolehAksiCrud} granular (fitur ini murni pengaturan
	 * tampilan mesin/toko, bukan data bisnis).
	 */
	private static boolean bolehKelolaScreensaver(Tbmuser tbmuser) {
		ais.database.model.inventory.Pedagang pemanggil = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobal = pemanggil == null;
		boolean supervisor = pemanggil != null && Boolean.TRUE.equals(pemanggil.getSupervisor());
		return adminGlobal || supervisor;
	}

	/**
	 * Resolusi {@code toko_id} utk fitur Screensaver -- SAMA pola dgn {@code diskonSimpan}/
	 * {@code pencairanDiskonList}: pedagang toko SELALU dikunci ke tokonya sendiri (IDOR-safe,
	 * parameter klien diabaikan), admin/manager global bebas memilih ATAU mengosongkan (null =
	 * lingkup semua toko -- HANYA valid utk query baca "list milik semua toko admin", bukan utk
	 * upload/simpan yang WAJIB toko konkret).
	 */
	private static Long resolveTokoIdScreensaver(Tbmuser tbmuser, JSONObject request) throws Exception {
		ais.database.model.inventory.Pedagang pemanggil = tbmuser == null ? null : tbmuser.getPedagang();
		if (pemanggil != null) {
			return pemanggil.getToko() == null ? null : pemanggil.getToko().getId();
		}
		return (!request.isNull("toko_id") && !((request.get("toko_id") + "").trim().isEmpty()))
				? Long.valueOf((request.get("toko_id") + "").trim())
				: null;
	}

	/**
	 * Daftar gambar slideshow screensaver Layar Pelanggan (utk layar kelola di Konfigurasi).
	 * Query lewat {@code StreamingHibernateUtil} (tabel {@code layar_pelanggan_slide} ADA di DB
	 * streaming, bukan utama -- lihat JavaDoc {@link ais.database.model.file.LayarPelangganSlide}).
	 *
	 * @param request payload: {@code toko_id} (admin saja, opsional -- pedagang toko selalu dikunci).
	 */
	public static void layarPelangganSlideList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehKelolaScreensaver(tbmuser)) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola screensaver.");
			return;
		}
		Long tokoId = resolveTokoIdScreensaver(tbmuser, request);
		Session session = ais.database.hibernate.StreamingHibernateUtil.getInstance().openSession();
		try {
			org.hibernate.Criteria cq = session.createCriteria(ais.database.model.file.LayarPelangganSlide.class)
					.addOrder(org.hibernate.criterion.Order.asc("urutan")).addOrder(org.hibernate.criterion.Order.asc("id"));
			if (tokoId != null) cq.add(Restrictions.eq("tokoId", tokoId));
			@SuppressWarnings("unchecked")
			java.util.List<ais.database.model.file.LayarPelangganSlide> daftar = cq.list();
			JSONArray arr = new JSONArray();
			for (ais.database.model.file.LayarPelangganSlide s : daftar) {
				JSONObject j = new JSONObject();
				j.put("id", s.getId());
				j.put("namaFile", s.getNamaFile() == null ? "" : s.getNamaFile());
				j.put("tokoId", s.getTokoId() == null ? JSONObject.NULL : s.getTokoId());
				j.put("idMesin", s.getIdMesin() == null ? JSONObject.NULL : s.getIdMesin());
				j.put("urutan", s.getUrutan());
				j.put("aktif", s.getAktif());
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			ais.database.hibernate.HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Unggah satu gambar slideshow screensaver -- payload gambar base64 (pola SAMA dgn
	 * {@code impor_excel_produk} Flutter, satu-satunya presedan "kirim byte ke server lewat aksi
	 * JSON" di codebase ini; tak ada endpoint multipart terpisah).
	 *
	 * @param request payload: {@code gambar_base64} (wajib), {@code nama_file} (wajib),
	 *                {@code toko_id} (admin saja -- pedagang toko selalu dikunci; WAJIB resolve
	 *                ke toko konkret, tidak boleh null), {@code id_mesin} (opsional -- kosong =
	 *                tampil di SEMUA mesin toko ini; diisi = HANYA mesin dgn id itu, lihat
	 *                JavaDoc entity), {@code urutan} (opsional, default di belakang daftar).
	 */
	public static void layarPelangganSlideUpload(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehKelolaScreensaver(tbmuser)) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola screensaver.");
			return;
		}
		String base64 = request.optString("gambar_base64", "").trim();
		String namaFile = request.optString("nama_file", "").trim();
		if (base64.isEmpty() || namaFile.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Gambar dan nama berkas wajib diisi.");
			return;
		}
		Long tokoId = resolveTokoIdScreensaver(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko wajib dipilih sebelum mengunggah gambar screensaver.");
			return;
		}
		String idMesin = request.optString("id_mesin", "").trim();
		Integer urutan = request.isNull("urutan") ? null : request.optInt("urutan", 0);

		byte[] bytes;
		try {
			bytes = java.util.Base64.getDecoder().decode(base64);
		} catch (IllegalArgumentException eb64) {
			hasil.put("status", "91");
			hasil.put("description", "Data gambar tidak valid (base64 gagal diurai).");
			return;
		}

		Session session = ais.database.hibernate.StreamingHibernateUtil.getInstance().openSession();
		try {
			ais.database.model.file.LayarPelangganSlide s = new ais.database.model.file.LayarPelangganSlide();
			s.setNamaFile(namaFile);
			s.setTokoId(tokoId);
			s.setIdMesin(idMesin.isEmpty() ? null : idMesin);
			s.setUrutan(urutan);
			s.setAktif(true);
			s.setTanggalUpload(ais.ui.util.WaktuUtil.getDate());
			s.setGambar(org.hibernate.Hibernate.createBlob(bytes));
			session.beginTransaction();
			session.save(s);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", s.getId());
		} finally {
			ais.database.hibernate.HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Ubah metadata satu slide (urutan/aktif/lingkup mesin) TANPA mengganti gambarnya --
	 * dipisah dari {@link #layarPelangganSlideUpload} supaya reorder/toggle-aktif ringan (tak
	 * perlu kirim ulang base64 gambar setiap kali).
	 *
	 * @param request payload: {@code id} (wajib), {@code urutan}, {@code aktif}, {@code id_mesin}
	 *                (kosong string = berlaku semua mesin toko).
	 */
	public static void layarPelangganSlideUbah(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehKelolaScreensaver(tbmuser)) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola screensaver.");
			return;
		}
		if (request.isNull("id")) {
			hasil.put("status", "91");
			hasil.put("description", "ID slide wajib diisi.");
			return;
		}
		Long id = Long.valueOf((request.get("id") + "").trim());
		Long tokoId = resolveTokoIdScreensaver(tbmuser, request);

		Session session = ais.database.hibernate.StreamingHibernateUtil.getInstance().openSession();
		try {
			ais.database.model.file.LayarPelangganSlide s = (ais.database.model.file.LayarPelangganSlide) session
					.get(ais.database.model.file.LayarPelangganSlide.class, id);
			if (s == null) {
				hasil.put("status", "91");
				hasil.put("description", "Slide tidak ditemukan.");
				return;
			}
			// Pedagang toko HANYA boleh mengubah slide milik tokonya sendiri (IDOR-safe).
			if (tokoId != null && !tokoId.equals(s.getTokoId())) {
				hasil.put("status", "91");
				hasil.put("description", "Slide ini bukan milik toko Anda.");
				return;
			}
			if (!request.isNull("urutan")) s.setUrutan(request.optInt("urutan", 0));
			if (!request.isNull("aktif")) s.setAktif(request.optBoolean("aktif", true));
			if (!request.isNull("id_mesin")) {
				String idMesin = request.optString("id_mesin", "").trim();
				s.setIdMesin(idMesin.isEmpty() ? null : idMesin);
			}
			session.beginTransaction();
			session.saveOrUpdate(s);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			ais.database.hibernate.HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Hapus satu slide screensaver -- gerbang toko SAMA pola dgn {@link #layarPelangganSlideUbah}. */
	public static void layarPelangganSlideHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehKelolaScreensaver(tbmuser)) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola screensaver.");
			return;
		}
		if (request.isNull("id")) {
			hasil.put("status", "91");
			hasil.put("description", "ID slide wajib diisi.");
			return;
		}
		Long id = Long.valueOf((request.get("id") + "").trim());
		Long tokoId = resolveTokoIdScreensaver(tbmuser, request);

		Session session = ais.database.hibernate.StreamingHibernateUtil.getInstance().openSession();
		try {
			ais.database.model.file.LayarPelangganSlide s = (ais.database.model.file.LayarPelangganSlide) session
					.get(ais.database.model.file.LayarPelangganSlide.class, id);
			if (s == null) {
				hasil.put("status", "00");
				return;
			}
			if (tokoId != null && !tokoId.equals(s.getTokoId())) {
				hasil.put("status", "91");
				hasil.put("description", "Slide ini bukan milik toko Anda.");
				return;
			}
			session.beginTransaction();
			session.delete(s);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			ais.database.hibernate.HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Ambil pengaturan screensaver satu toko (get-or-default -- baris belum pernah dibuat = SEMUA
	 * nilai default dikembalikan, {@code aktif=false} supaya fitur baru ini opt-in, bukan otomatis
	 * menyala di instalasi lama begitu di-deploy).
	 *
	 * @param request payload: {@code toko_id} (admin saja, opsional; pedagang toko selalu dikunci).
	 */
	public static void layarPelangganScreensaverConfigAmbil(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = resolveTokoIdScreensaver(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko wajib dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.koperasi.LayarPelangganScreensaverConfig cfg = (ais.database.model.koperasi.LayarPelangganScreensaverConfig) session
					.createCriteria(ais.database.model.koperasi.LayarPelangganScreensaverConfig.class)
					.add(Restrictions.eq("tokoId", tokoId)).setMaxResults(1).uniqueResult();
			JSONObject j = new JSONObject();
			j.put("aktif", cfg == null ? false : cfg.getAktif());
			j.put("modeTampilan", cfg == null ? ais.database.model.koperasi.LayarPelangganScreensaverConfig.MODE_FULLSCREEN : cfg.getModeTampilan());
			j.put("animasi", cfg == null ? ais.database.model.koperasi.LayarPelangganScreensaverConfig.ANIMASI_FADE : cfg.getAnimasi());
			j.put("durasiDetik", cfg == null ? 6 : cfg.getDurasiDetik());
			j.put("idleDetik", cfg == null ? 30 : cfg.getIdleDetik());
			hasil.put("status", "00");
			hasil.put("data", j);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Simpan pengaturan screensaver satu toko (upsert -- buat baris baru bila belum ada).
	 *
	 * @param request payload: {@code toko_id} (admin saja), {@code aktif}, {@code mode_tampilan}
	 *                (FULLSCREEN/SETENGAH), {@code animasi} (FADE/SLIDE/ZOOM/KEN_BURNS),
	 *                {@code durasi_detik}, {@code idle_detik}.
	 */
	public static void layarPelangganScreensaverConfigSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehKelolaScreensaver(tbmuser)) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola screensaver.");
			return;
		}
		Long tokoId = resolveTokoIdScreensaver(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko wajib dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.koperasi.LayarPelangganScreensaverConfig cfg = (ais.database.model.koperasi.LayarPelangganScreensaverConfig) session
					.createCriteria(ais.database.model.koperasi.LayarPelangganScreensaverConfig.class)
					.add(Restrictions.eq("tokoId", tokoId)).setMaxResults(1).uniqueResult();
			if (cfg == null) {
				cfg = new ais.database.model.koperasi.LayarPelangganScreensaverConfig();
				cfg.setTokoId(tokoId);
			}
			cfg.setAktif(request.optBoolean("aktif", false));
			cfg.setModeTampilan(request.optString("mode_tampilan", ais.database.model.koperasi.LayarPelangganScreensaverConfig.MODE_FULLSCREEN));
			cfg.setAnimasi(request.optString("animasi", ais.database.model.koperasi.LayarPelangganScreensaverConfig.ANIMASI_FADE));
			cfg.setDurasiDetik(request.optInt("durasi_detik", 6));
			cfg.setIdleDetik(request.optInt("idle_detik", 30));
			session.beginTransaction();
			session.saveOrUpdate(cfg);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Fetch gabungan (config + daftar slide aktif utk lingkup pemanggil) dipanggil Layar Pelanggan
	 * SEKALI setiap kali masuk state screensaver (BUKAN dipoll tiap 500ms spt {@code
	 * layarPelangganAmbil} -- konten slideshow jarang berubah, cukup disegarkan tiap kali screensaver
	 * baru menyala supaya gambar baru ter-upload ikut muncul tanpa perlu restart aplikasi).
	 *
	 * <p>Filter lingkup: {@code toko_id = X AND aktif = true AND (id_mesin IS NULL OR id_mesin =
	 * &lt;id_mesin milik pemanggil&gt;)} -- gambar "semua mesin" (id_mesin null) SELALU ikut,
	 * gambar "khusus mesin ini" hanya muncul di mesin yang cocok.</p>
	 *
	 * @param request payload: {@code toko_id} (wajib), {@code id_mesin} (opsional, dari
	 *                {@code IdentitasMesin.instance.idMesin} sisi Flutter).
	 */
	public static void layarPelangganSlideUntukTampil(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (request.isNull("toko_id")) {
			hasil.put("status", "91");
			hasil.put("description", "Toko wajib diisi.");
			return;
		}
		Long tokoId = Long.valueOf((request.get("toko_id") + "").trim());
		String idMesin = request.optString("id_mesin", "").trim();

		JSONObject cfgJson = new JSONObject();
		Session sesiUtama = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.koperasi.LayarPelangganScreensaverConfig cfg = (ais.database.model.koperasi.LayarPelangganScreensaverConfig) sesiUtama
					.createCriteria(ais.database.model.koperasi.LayarPelangganScreensaverConfig.class)
					.add(Restrictions.eq("tokoId", tokoId)).setMaxResults(1).uniqueResult();
			cfgJson.put("aktif", cfg == null ? false : cfg.getAktif());
			cfgJson.put("modeTampilan", cfg == null ? ais.database.model.koperasi.LayarPelangganScreensaverConfig.MODE_FULLSCREEN : cfg.getModeTampilan());
			cfgJson.put("animasi", cfg == null ? ais.database.model.koperasi.LayarPelangganScreensaverConfig.ANIMASI_FADE : cfg.getAnimasi());
			cfgJson.put("durasiDetik", cfg == null ? 6 : cfg.getDurasiDetik());
			cfgJson.put("idleDetik", cfg == null ? 30 : cfg.getIdleDetik());
		} finally {
			tutupSessionPolaB(sesiUtama);
		}

		JSONArray arr = new JSONArray();
		if (cfgJson.optBoolean("aktif", false)) {
			Session sesiStreaming = ais.database.hibernate.StreamingHibernateUtil.getInstance().openSession();
			try {
				org.hibernate.Criteria cq = sesiStreaming.createCriteria(ais.database.model.file.LayarPelangganSlide.class)
						.add(Restrictions.eq("tokoId", tokoId))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(idMesin.isEmpty() ? Restrictions.isNull("idMesin")
								: Restrictions.or(Restrictions.isNull("idMesin"), Restrictions.eq("idMesin", idMesin)))
						.addOrder(org.hibernate.criterion.Order.asc("urutan")).addOrder(org.hibernate.criterion.Order.asc("id"));
				@SuppressWarnings("unchecked")
				java.util.List<ais.database.model.file.LayarPelangganSlide> daftar = cq.list();
				for (ais.database.model.file.LayarPelangganSlide s : daftar) {
					JSONObject j = new JSONObject();
					j.put("id", s.getId());
					arr.put(j);
				}
			} finally {
				ais.database.hibernate.HibernateUtil.closeSessionQuietly(sesiStreaming);
			}
		}

		hasil.put("status", "00");
		hasil.put("config", cfgJson);
		hasil.put("slides", arr);
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
							+ "a.tanggal_mulai, a.tanggal_selesai, a.hari_aktif, COALESCE(a.aktivasi_manual,false), "
							+ "COALESCE(a.prioritas,100),COALESCE(a.dapat_digabung,false),COALESCE(a.dasar_perhitungan,'SETELAH_DISKON'),COALESCE(a.grup_eksklusif,'') "
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
				j.put("hariAktif", rs.getString(11) == null ? "" : rs.getString(11));
				j.put("aktivasiManual", rs.getBoolean(12));
				j.put("prioritas", rs.getInt(13));
				j.put("dapatDigabung", rs.getBoolean(14));
				j.put("dasarPerhitungan", rs.getString(15));
				j.put("grupEksklusif", rs.getString(16));
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
				Number idProduk = (Number) session
						.createSQLQuery("SELECT id FROM koperasi.produk WHERE kode = :k")
						.setParameter("k", kodeProduk).uniqueResult();
				if (idProduk == null) {
					hasil.put("status", "91");
					hasil.put("description", "Produk dengan kode \"" + kodeProduk + "\" tidak ditemukan.");
					return;
				}
				a.setProduk((Produk) session.get(Produk.class, idProduk.longValue()));
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
			a.setPrioritas(Math.max(0, request.optInt("prioritas", 100)));
			a.setDapatDigabung(request.optBoolean("dapat_digabung", false));
			String dasarPerhitungan = request.optString("dasar_perhitungan", "SETELAH_DISKON").toUpperCase();
			a.setDasarPerhitungan("HARGA_AWAL".equals(dasarPerhitungan) ? dasarPerhitungan : "SETELAH_DISKON");
			a.setGrupEksklusif(request.optString("grup_eksklusif", "").trim());
			a.setPotonganLangsung(request.optBoolean("potongan_langsung", true));
			a.setBerlakuPerHariDanPerToko(request.optBoolean("berlaku_per_hari_dan_per_toko", false));
			a.setAktif(request.optBoolean("aktif", true));

			java.text.SimpleDateFormat fmtInput = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
			String tglMulaiStr = request.optString("tanggal_mulai", "").trim();
			a.setTanggalMulai(tglMulaiStr.isEmpty() ? null : fmtInput.parse(tglMulaiStr));
			String tglSelesaiStr = request.optString("tanggal_selesai", "").trim();
			a.setTanggalSelesai(tglSelesaiStr.isEmpty() ? null : fmtInput.parse(tglSelesaiStr));
			// Gap-closure "Promo Pilih Hari" -- CSV dari klien (mis. "1,2,3,4,5"), kosong/tak dikirim =
			// berlaku semua hari. Lihat JavaDoc AturanDiskon.getHariAktif().
			String hariAktifStr = request.optString("hari_aktif", "").trim();
			a.setHariAktif(hariAktifStr.isEmpty() ? null : hariAktifStr);
			// Gap-closure "Aktivasi Manual" -- lihat JavaDoc AturanDiskon.getAktivasiManual(): true =
			// aturan ini DIKECUALIKAN dari auto-apply, kasir pilih manual lewat tombol Promo saat checkout.
			a.setAktivasiManual(request.optBoolean("aktivasi_manual", false));

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
	 * <h3>Pencairan Diskon -- riwayat (paginated, per toko), gap-closure paritas tab "Pencairan
	 * Diskon" JSP ({@code webapp/WEB-INF/baru/modul/kantin/diskon/pencairan_diskon.jsp}) yang
	 * sebelumnya HANYA reachable dari cookie-session JSP (raw {@code /Data} {@code action:"sql"}) --
	 * Electron/Flutter (Bearer-token) tidak punya jalur itu.</h3>
	 *
	 * @param request payload: {@code keyword} (cocok kode_pencairan/nama/kode_identitas anggota),
	 *                {@code status} (opsional, PENDING/BERHASIL/DITOLAK), {@code toko_id} (opsional
	 *                utk admin -- kosong = semua toko; pedagang toko SELALU dikunci ke tokonya
	 *                sendiri, parameter ini diabaikan utk pedagang), {@code page}/{@code page_size}.
	 */
	public static void pencairanDiskonList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilPd = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalPd = pemanggilPd == null;
		String keyword = request.optString("keyword", "").trim();
		String status = request.optString("status", "").trim();
		Long tokoId = null;
		if (!adminGlobalPd) {
			tokoId = pemanggilPd.getToko() == null ? null : pemanggilPd.getToko().getId();
		} else if (!request.isNull("toko_id") && !((request.get("toko_id") + "").trim().isEmpty())) {
			tokoId = Long.valueOf((request.get("toko_id") + "").trim());
		}
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(1, request.optInt("page_size", 20)));
		int offset = (page - 1) * pageSize;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE 1=1");
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			if (!keyword.isEmpty()) {
				where.append(" AND (a.kode_pencairan ILIKE ? OR ak.nama ILIKE ? OR ak.kode_identitas ILIKE ?)");
				String kw = "%" + keyword + "%";
				params.add(kw); params.add(kw); params.add(kw);
			}
			if (!status.isEmpty()) {
				where.append(" AND a.status = ?");
				params.add(status);
			}
			if (tokoId != null) {
				where.append(" AND a.toko = ?");
				params.add(tokoId);
			}

			java.sql.Connection conn = session.connection();
			java.sql.PreparedStatement psCount = conn.prepareStatement(
					"SELECT COUNT(*) FROM koperasi.pencairan_diskon a LEFT JOIN koperasi.anggota_koperasi ak ON a.anggota_koperasi = ak.id"
							+ where);
			for (int i = 0; i < params.size(); i++) psCount.setObject(i + 1, params.get(i));
			java.sql.ResultSet rsCount = psCount.executeQuery();
			long total = rsCount.next() ? rsCount.getLong(1) : 0;
			rsCount.close(); psCount.close();

			java.sql.PreparedStatement ps = conn.prepareStatement(
					"SELECT a.id, a.kode_pencairan, TO_CHAR(a.waktu_pencairan,'YYYY-MM-DD HH24:MI') , "
							+ "TO_CHAR(a.waktu_pencairan,'YYYY-MM-DD\"T\"HH24:MI'), a.nominal_cair, a.status, COALESCE(a.keterangan,''), "
							+ "TO_CHAR(a.tanggal_expired_jika_berupa_topup,'YYYY-MM-DD'), t.id, COALESCE(t.nama,''), "
							+ "ak.id, COALESCE(ak.nama,''), COALESCE(ak.kode_identitas,''), cb.id, COALESCE(cb.nama,'') "
							+ "FROM koperasi.pencairan_diskon a "
							+ "LEFT JOIN koperasi.toko t ON a.toko = t.id "
							+ "LEFT JOIN koperasi.anggota_koperasi ak ON a.anggota_koperasi = ak.id "
							+ "LEFT JOIN koperasi.cara_pembayaran_koperasi cb ON a.cara_pembayaran = cb.id"
							+ where + " ORDER BY a.waktu_pencairan DESC, a.id DESC LIMIT ? OFFSET ?");
			for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
			ps.setInt(params.size() + 1, pageSize);
			ps.setInt(params.size() + 2, offset);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kodePencairan", rs.getString(2));
				j.put("waktuPencairan", rs.getString(3));
				j.put("waktuPencairanRaw", rs.getString(4));
				j.put("nominalCair", rs.getDouble(5));
				j.put("status", rs.getString(6));
				j.put("keterangan", rs.getString(7));
				String tglExp = rs.getString(8);
				j.put("tanggalExpired", tglExp == null ? JSONObject.NULL : tglExp);
				long idToko = rs.getLong(9);
				j.put("tokoId", rs.wasNull() ? JSONObject.NULL : idToko);
				j.put("tokoNama", rs.getString(10));
				j.put("anggotaId", rs.getLong(11));
				j.put("anggotaNama", rs.getString(12));
				j.put("anggotaKode", rs.getString(13));
				j.put("caraPembayaranId", rs.getLong(14));
				j.put("caraPembayaranNama", rs.getString(15));
				arr.put(j);
			}
			rs.close(); ps.close();

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
	 * <h3>Sisa saldo cashback member yang belum dicairkan.</h3> Formula SAMA PERSIS
	 * {@code cekSaldoMember} di {@code pencairan_diskon.jsp}: total cashback dari
	 * {@code pembelian_anggota_koperasi} dikurangi total pencairan berstatus BERHASIL/PENDING.
	 * Dipakai baik oleh form Flutter (live saldo saat memilih anggota) MAUPUN sbg validasi
	 * server-side di {@link #pencairanDiskonSimpan} -- JSP hanya mengecek ini di klien (bisa
	 * dilewati), server-side wajib jadi sumber kebenaran akhir utk aksi finansial.
	 *
	 * @param exceptId baris pencairan yang DIKECUALIKAN dari perhitungan "sudah dicairkan" (dipakai
	 *                 saat MENGUBAH baris yang sudah ada, supaya nominal baris itu sendiri tidak
	 *                 dihitung dobel); {@code null} saat membuat baris baru.
	 */
	private static double pencairanDiskonSisaSaldo(Session session, Long anggotaId, Long exceptId) throws Exception {
		java.sql.Connection conn = session.connection();
		String exceptCond = exceptId == null ? "" : " AND id != ?";
		java.sql.PreparedStatement ps = conn.prepareStatement(
				"SELECT (COALESCE((SELECT SUM(totalcashback) FROM koperasi.pembelian_anggota_koperasi WHERE anggota_koperasi = ?),0) - "
						+ "COALESCE((SELECT SUM(nominal_cair) FROM koperasi.pencairan_diskon WHERE anggota_koperasi = ? AND status IN ('BERHASIL','PENDING')"
						+ exceptCond + "),0))");
		ps.setLong(1, anggotaId);
		ps.setLong(2, anggotaId);
		if (exceptId != null) ps.setLong(3, exceptId);
		java.sql.ResultSet rs = ps.executeQuery();
		double sisa = rs.next() ? rs.getDouble(1) : 0.0;
		rs.close(); ps.close();
		return sisa;
	}

	/** <h3>Pencairan Diskon -- helper cek sisa saldo member (dipakai form Flutter, live saat memilih anggota).</h3> */
	public static void pencairanDiskonSaldoMember(JSONObject request, JSONObject hasil) throws Exception {
		Long anggotaId = request.isNull("anggota_koperasi_id") ? null
				: Long.valueOf((request.get("anggota_koperasi_id") + "").trim());
		if (anggotaId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Anggota koperasi wajib dipilih.");
			return;
		}
		Long exceptId = request.isNull("except_id") ? null : Long.valueOf((request.get("except_id") + "").trim());
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			double sisa = pencairanDiskonSisaSaldo(session, anggotaId, exceptId);
			hasil.put("status", "00");
			hasil.put("sisaSaldo", sisa);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Pencairan Diskon -- simpan (buat/ubah) SATU baris.</h3> Gerbang SAMA dgn
	 * {@link #diskonSimpan} (admin/supervisor-only -- aksi finansial, bukan utk kasir biasa).
	 *
	 * @param request payload: {@code id} (opsional, utk ubah), {@code kode_pencairan} (opsional --
	 *                kosong = digenerate server {@code "WD-"+timestamp}), {@code anggota_koperasi_id}
	 *                (wajib), {@code toko_id} (admin saja, opsional), {@code cara_pembayaran_id}
	 *                (wajib), {@code nominal_cair} (wajib, &gt;0), {@code waktu_pencairan} (wajib,
	 *                {@code yyyy-MM-dd'T'HH:mm}), {@code tanggal_expired} (opsional, {@code
	 *                yyyy-MM-dd}), {@code status} (PENDING/BERHASIL/DITOLAK, default PENDING),
	 *                {@code keterangan} (opsional).
	 */
	public static void pencairanDiskonSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilPs = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalPs = pemanggilPs == null;
		boolean supervisorPs = pemanggilPs != null && Boolean.TRUE.equals(pemanggilPs.getSupervisor());
		String aksiPs = request.isNull("id") ? "create" : "update";
		if (!bolehAksiCrud(tbmuser, pemanggilPs, adminGlobalPs, supervisorPs, "pencairandiskon", aksiPs)) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola Pencairan Diskon.");
			return;
		}
		Long anggotaId = request.isNull("anggota_koperasi_id") ? null
				: Long.valueOf((request.get("anggota_koperasi_id") + "").trim());
		if (anggotaId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Anggota koperasi wajib dipilih.");
			return;
		}
		Long caraBayarId = request.isNull("cara_pembayaran_id") ? null
				: Long.valueOf((request.get("cara_pembayaran_id") + "").trim());
		if (caraBayarId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Cara pencairan wajib dipilih.");
			return;
		}
		double nominalCair = request.optDouble("nominal_cair", 0);
		if (nominalCair <= 0) {
			hasil.put("status", "91");
			hasil.put("description", "Nominal pencairan harus lebih dari 0.");
			return;
		}
		String waktuStr = request.optString("waktu_pencairan", "").trim();
		if (waktuStr.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Waktu pencairan wajib diisi.");
			return;
		}
		String status = request.optString("status", "PENDING").trim();
		if (status.isEmpty()) status = "PENDING";
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			// Validasi saldo SERVER-SIDE (sumber kebenaran akhir aksi finansial, bukan sekadar
			// pengecekan klien spt di JSP) -- dilewati kalau status DITOLAK (uang tidak sungguh
			// keluar), sama semantik dgn pencairan_diskon.jsp.
			if (!"DITOLAK".equals(status)) {
				double sisaSaldo = pencairanDiskonSisaSaldo(session, anggotaId, id);
				if (nominalCair > sisaSaldo) {
					hasil.put("status", "91");
					hasil.put("description", "Saldo cashback member tidak mencukupi. Sisa saldo saat ini: Rp"
							+ String.format("%,.0f", sisaSaldo).replace(',', '.'));
					return;
				}
			}

			ais.database.model.koperasi.PencairanDiskon p;
			if (id == null) {
				p = new ais.database.model.koperasi.PencairanDiskon();
			} else {
				p = (ais.database.model.koperasi.PencairanDiskon) session.get(ais.database.model.koperasi.PencairanDiskon.class, id);
				if (p == null) {
					hasil.put("status", "91");
					hasil.put("description", "Data pencairan tidak ditemukan.");
					return;
				}
			}

			ais.database.model.koperasi.AnggotaKoperasi anggota = (ais.database.model.koperasi.AnggotaKoperasi) session
					.get(ais.database.model.koperasi.AnggotaKoperasi.class, anggotaId);
			if (anggota == null) {
				hasil.put("status", "91");
				hasil.put("description", "Anggota koperasi tidak ditemukan.");
				return;
			}
			ais.database.model.koperasi.CaraPembayaranKoperasi caraBayar = (ais.database.model.koperasi.CaraPembayaranKoperasi) session
					.get(ais.database.model.koperasi.CaraPembayaranKoperasi.class, caraBayarId);
			if (caraBayar == null) {
				hasil.put("status", "91");
				hasil.put("description", "Cara pencairan tidak ditemukan.");
				return;
			}

			p.setAnggotaKoperasi(anggota);
			p.setCaraPembayaran(caraBayar);
			// Resolusi toko: pedagang toko WAJIB terkunci ke tokonya sendiri, admin bebas (termasuk kosong).
			if (pemanggilPs != null) {
				p.setToko(pemanggilPs.getToko());
			} else if (!request.isNull("toko_id") && !((request.get("toko_id") + "").trim().isEmpty())) {
				p.setToko((Toko) session.get(Toko.class, Long.valueOf((request.get("toko_id") + "").trim())));
			} else {
				p.setToko(null);
			}

			String kodePencairan = request.optString("kode_pencairan", "").trim();
			p.setKodePencairan(kodePencairan.isEmpty() ? ("WD-" + System.currentTimeMillis()) : kodePencairan);

			java.text.SimpleDateFormat fmtInput = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
			p.setWaktuPencairan(fmtInput.parse(waktuStr));
			String tglExpStr = request.optString("tanggal_expired", "").trim();
			p.setTanggalExpiredJikaBerupaTopup(
					tglExpStr.isEmpty() ? null : new java.text.SimpleDateFormat("yyyy-MM-dd").parse(tglExpStr));

			p.setNominalCair(nominalCair);
			p.setStatus(status);
			p.setKeterangan(request.optString("keterangan", ""));
			p.setOleh(tbmuser == null ? "pencairan_diskon" : tbmuser.getUserNama());
			p.setOlehId(tbmuser == null ? "-" : String.valueOf(tbmuser.getUserId()));

			session.beginTransaction();
			try {
				session.saveOrUpdate(p);
				session.getTransaction().commit();
			} catch (org.hibernate.exception.ConstraintViolationException eDup) {
				try { if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				} } catch (Exception eRb) { ais.common.ErrorAuditUtil.record(eRb, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:pencairanDiskonSimpan-rollback-dup"); }
				hasil.put("status", "91");
				hasil.put("description", "Nomor referensi \"" + p.getKodePencairan() + "\" sudah dipakai baris lain. Ubah nomor referensi lalu coba lagi.");
				return;
			}
			hasil.put("status", "00");
			hasil.put("id", p.getId());
			hasil.put("kodePencairan", p.getKodePencairan());
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** <h3>Pencairan Diskon -- hapus SATU baris.</h3> Gerbang SAMA dgn {@link #pencairanDiskonSimpan}. */
	public static void pencairanDiskonHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilHp = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalHp = pemanggilHp == null;
		boolean supervisorHp = pemanggilHp != null && Boolean.TRUE.equals(pemanggilHp.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggilHp, adminGlobalHp, supervisorHp, "pencairandiskon", "delete")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat menghapus Pencairan Diskon.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "ID pencairan wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.koperasi.PencairanDiskon p = (ais.database.model.koperasi.PencairanDiskon) session
					.get(ais.database.model.koperasi.PencairanDiskon.class, id);
			if (p == null) {
				hasil.put("status", "91");
				hasil.put("description", "Data pencairan tidak ditemukan.");
				return;
			}
			if (pemanggilHp != null) {
				Long tokoPedagang = pemanggilHp.getToko() == null ? null : pemanggilHp.getToko().getId();
				Long tokoBaris = p.getToko() == null ? null : p.getToko().getId();
				if (tokoPedagang == null || !tokoPedagang.equals(tokoBaris)) {
					hasil.put("status", "91");
					hasil.put("description", "Data pencairan ini bukan milik toko Anda.");
					return;
				}
			}
			session.beginTransaction();
			session.delete(p);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("description", "Data pencairan berhasil dihapus.");
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
	 *
	 * <p><b>Gap-closure "Kunci Toko per Akun" (2026-08-11).</b> {@code Pedagang.toko} (diisi lewat
	 * field "Toko / Penjual" di {@code TbmuserAction}) SEKARANG dicek LEBIH DULU -- kalau terisi,
	 * akun ini terkunci ke toko itu, TITIK, walau {@code tokoAktifMultiToko} masih menyimpan nilai
	 * BEKAS dari sesi multi-toko SEBELUM akun ini dikunci admin (nilai bekas itu diabaikan, bukan
	 * dipercaya lagi). Sama pola prioritas dgn {@link #daftarTokoBolehDiakses}.</p>
	 */
	private static Long soResolveTokoId(Tbmuser tbmuser, JSONObject request) throws Exception {
		ais.database.model.inventory.Pedagang pemanggil = tbmuser == null ? null : tbmuser.getPedagang();
		if (pemanggil != null) {
			if (pemanggil.getToko() != null) {
				return pemanggil.getToko().getId();
			}
			// Toko/Penjual TIDAK ditentukan -- akun ini bukan dikunci, boleh multi-toko. Field
			// tokoAktifMultiToko (diisi lewat sesi_kas_buka) baru relevan di sini.
			return tbmuser.getTokoAktifMultiToko();
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
	 *
	 * <p><b>Gap-closure "Kunci Toko per Akun" (2026-08-11).</b> Field "Toko / Penjual" di layar
	 * admin {@code TbmuserAction} menulis ke {@code Tbmuser.getPedagang().getToko()} -- kalau field
	 * itu DIISI, akun ini WAJIB terkunci ke toko tersebut SAJA di POS Android/Desktop/JSP + Stok
	 * Opname Android, TIDAK BOLEH ada pilihan ke toko lain, walau grup penggunanya (Tbmrole) punya
	 * konfigurasi multi-toko ({@code tokoAksesJson}) ATAU akun ini juga punya baris {@code Pedagang}
	 * tambahan dari layar "Ambil Pengguna". SEBELUMNYA kode ini memberi PRIORITAS ke
	 * {@code tokoAksesJson} (return lebih dulu di atas), sehingga grup multi-toko diam-diam
	 * MENGALAHKAN kunci per-akun ini -- kasir yg semestinya terkunci tetap melihat combo pilih-toko.
	 * Sekarang {@code Pedagang.toko} dicek PALING AWAL dan, bila terisi, langsung
	 * DIKEMBALIKAN SENDIRIAN (bukan digabung) -- kunci akun selalu menang. {@code tokoAksesJson}/
	 * multi-{@code Pedagang} HANYA dipakai sbg fallback ketika {@code Pedagang.toko} kosong (field
	 * "Toko/Penjual" memang sengaja tidak ditentukan -- kasus admin/manajer/kasir multi-toko).</p>
	 */
	public static java.util.List<Toko> daftarTokoBolehDiakses(Session session, Tbmuser tbmuser) {
		java.util.LinkedHashMap<Long, Toko> peta = new java.util.LinkedHashMap<Long, Toko>();
		ais.database.model.inventory.Pedagang milikSendiri = tbmuser == null ? null : tbmuser.getPedagang();
		if (milikSendiri != null && milikSendiri.getToko() != null) {
			// Kunci per-akun (Toko/Penjual di TbmuserAction) -- SELALU menang, abaikan tokoAksesJson
			// role dan baris Pedagang tambahan lain sama sekali. Satu toko, tanpa kompromi.
			peta.put(milikSendiri.getToko().getId(), milikSendiri.getToko());
			return new java.util.ArrayList<Toko>(peta.values());
		}
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
				// Modul POS + seluruh modul VARIAN (Inventory & Sales / Apotik / eMedik) --
				// sebelumnya HANYA MODUL_POS, sehingga kunci varian tidak pernah bisa
				// dilihat/diatur dari layar Hak Akses Flutter (gap ketahuan saat uji E2E
				// FASE A-4: simpan kunci apotik_* diam-diam terfilter). MODUL_KANTIN_JSP
				// tetap di luar (layarnya JSP, diatur dari editor ZK Grup Pengguna).
				// Default tampil per kunci ikut jalur terpusat KUNCI_DEFAULT_NONAKTIF
				// (kunci varian false, kunci lama true) -- bukan hardcode true.
				if (!ais.common.EbisnisMenuKatalog.MODUL_POS.equals(e.modul)
						&& !ais.common.EbisnisMenuKatalog.MODUL_INVENTORY_SALES.equals(e.modul)
						&& !ais.common.EbisnisMenuKatalog.MODUL_APOTIK.equals(e.modul)
						&& !ais.common.EbisnisMenuKatalog.MODUL_EMEDIK.equals(e.modul)) {
					continue;
				}
				JSONObject j = new JSONObject();
				j.put("kunci", e.kunci);
				j.put("label", e.label);
				j.put("modul", e.modul);
				j.put("boleh", menuTersimpan.optBoolean(e.kunci,
						!ais.common.EbisnisMenuKatalog.KUNCI_DEFAULT_NONAKTIF.contains(e.kunci)));
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
				// Selaras ebisnisRoleMenuAmbil: POS + modul varian (lihat komentar di sana);
				// kunci di payload yang bukan bagian modul ini tetap diabaikan diam2
				// (perilaku lama utk kunci tak dikenal dipertahankan).
				if (!ais.common.EbisnisMenuKatalog.MODUL_POS.equals(e.modul)
						&& !ais.common.EbisnisMenuKatalog.MODUL_INVENTORY_SALES.equals(e.modul)
						&& !ais.common.EbisnisMenuKatalog.MODUL_APOTIK.equals(e.modul)
						&& !ais.common.EbisnisMenuKatalog.MODUL_EMEDIK.equals(e.modul)) {
					continue;
				}
				if (menuBaru.has(e.kunci)) {
					menuSaatIni.put(e.kunci, menuBaru.optBoolean(e.kunci, true));
				}
			}
			// OPSIONAL: sub-objek `crud` (aksi granular per KUNCI_CRUD). ADITIF & backward-compat --
			// payload lama tanpa `crud` tidak mengubah CRUD sama sekali. Hanya kunci yg disebut yg
			// diperbarui; kunci di luar KUNCI_CRUD diabaikan. Dipakai mengaktifkan create/update/
			// delete/approve/reject varian (mis. apotik_pengadaan) yg sebelumnya tak bisa di-grant.
			JSONObject crudBaru = request.optJSONObject("crud");
			if (crudBaru != null) {
				JSONObject crudSaatIni = terurai.optJSONObject("crud");
				if (crudSaatIni == null) {
					crudSaatIni = new JSONObject();
					terurai.put("crud", crudSaatIni);
				}
				for (String kunciCrud : ais.common.EbisnisMenuKatalog.KUNCI_CRUD) {
					JSONObject aksiBaru = crudBaru.optJSONObject(kunciCrud);
					if (aksiBaru == null) {
						continue;
					}
					JSONObject aksiSaatIni = crudSaatIni.optJSONObject(kunciCrud);
					if (aksiSaatIni == null) {
						aksiSaatIni = new JSONObject();
						crudSaatIni.put(kunciCrud, aksiSaatIni);
					}
					for (String aksi : ais.common.EbisnisMenuKatalog.AKSI_CRUD) {
						if (aksiBaru.has(aksi)) {
							aksiSaatIni.put(aksi, aksiBaru.optBoolean(aksi, false));
						}
					}
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
		// Gap-closure (audit hak-akses): sebelumnya HANYA admin/supervisor (gerbang keras, checkbox
		// "Stok Opname" di matriks Hak Akses Pedagang tak berpengaruh apa pun). Sekarang konsisten dgn
		// menu granular lain (anggota/produk/kulakan/dst) -- kasir non-supervisor BOLEH kalau rolenya
		// diberi hak "Stok Opname: Create" secara eksplisit.
		if (!bolehAksiCrud(tbmuser, pemanggilSo, adminGlobalSo, supervisorSo, "stokopname", "create")) {
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
	 * <h3>Stok Opname Desktop/Android -- tombol "Unduh Excel" (gap-closure, padanan tombol yang
	 * sudah ada di JSP {@code kantin/stok/index.jsp}).</h3>
	 *
	 * <p>BEDA dari {@link #soRiwayat} (yang SENGAJA dibatasi "hari ini" saja utk kartu ringkasan
	 * layar) -- method ini TIDAK dibatasi tanggal secara default (seluruh riwayat toko), supaya
	 * unduhan benar-benar berguna sbg arsip/laporan, bukan cuma cerminan tabel di layar.</p>
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin, diabaikan utk pedagang/kasir),
	 *                {@code bulan} (opsional, format {@code YYYY-MM}, filter bulan opname).
	 * @param hasil   diisi {@code status="00"} + {@code fileBase64}, {@code namaFile}, {@code total}.
	 */
	public static void soEksporExcel(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		String bulan = request.optString("bulan", "").trim();

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder sql = new StringBuilder(
					"SELECT a.id, TO_CHAR(a.waktuopname, 'YYYY-MM-DD HH24:MI') as waktu, " +
					"a.stoksistem, a.stokfisik, a.selisih, a.keterangan, " +
					"a.produk AS id_produk, p.nama AS nama_produk, p.kode AS kode_produk " +
					"FROM koperasi.stok_opname a LEFT JOIN koperasi.produk p ON a.produk = p.id " +
					"WHERE a.toko = :tokoId ");
			if (!bulan.isEmpty()) {
				sql.append("AND TO_CHAR(a.waktuopname, 'YYYY-MM') = :bulan ");
			}
			sql.append("ORDER BY a.waktuopname DESC, a.id DESC");

			org.hibernate.SQLQuery q = session.createSQLQuery(sql.toString());
			q.setParameter("tokoId", tokoId);
			if (!bulan.isEmpty()) q.setParameter("bulan", bulan);

			@SuppressWarnings("unchecked")
			List<Object[]> rows = q.list();

			XSSFWorkbook wb = new XSSFWorkbook();
			XSSFSheet sheet = wb.createSheet("Stok Opname");
			sheet.setDefaultColumnWidth(18);
			String[] kolom = { "ID_SISTEM", "ID_PRODUK", "NAMA_PRODUK_INFO", "WAKTU_OPNAME", "STOK_SISTEM", "STOK_FISIK", "SELISIH", "KETERANGAN" };
			XSSFRow header = sheet.createRow(0);
			for (int i = 0; i < kolom.length; i++) header.createCell(i).setCellValue(kolom[i]);

			int r = 1;
			for (Object[] row : rows) {
				XSSFRow xr = sheet.createRow(r++);
				xr.createCell(0).setCellValue(row[0] == null ? "" : row[0].toString());
				xr.createCell(1).setCellValue(row[6] == null ? "" : row[6].toString());
				String kode = row[8] == null ? "" : " [" + row[8] + "]";
				xr.createCell(2).setCellValue((row[7] == null ? "" : row[7].toString()) + kode);
				xr.createCell(3).setCellValue(row[1] == null ? "" : row[1].toString());
				xr.createCell(4).setCellValue(row[2] == null ? 0 : ((Number) row[2]).doubleValue());
				xr.createCell(5).setCellValue(row[3] == null ? 0 : ((Number) row[3]).doubleValue());
				xr.createCell(6).setCellValue(row[4] == null ? 0 : ((Number) row[4]).doubleValue());
				xr.createCell(7).setCellValue(row[5] == null ? "" : row[5].toString());
			}

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			wb.write(bos);
			hasil.put("status", "00");
			hasil.put("fileBase64", java.util.Base64.getEncoder().encodeToString(bos.toByteArray()));
			hasil.put("namaFile", "StokOpname-" + System.currentTimeMillis() + ".xlsx");
			hasil.put("total", rows.size());
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Stok Opname Desktop/Android -- tombol "Unggah Excel" (gap-closure, padanan tombol yang
	 * sudah ada di JSP {@code kantin/stok/index.jsp}).</h3>
	 *
	 * <p>SENGAJA insert-only (setiap baris file selalu jadi catatan {@link StokOpname} BARU lewat
	 * {@link StokOpnameScanUtil#simpanOpname}, TIDAK PERNAH meng-update baris lama sekalipun kolom
	 * {@code ID_SISTEM} terisi) -- konsisten dgn semantik "SO by Scan" yang sudah ada (setiap
	 * hitungan fisik = catatan log baru, bukan koreksi diam-diam atas catatan lama) dan lebih aman
	 * utk alur "hitung offline di Excel, lalu upload sbg batch" drpd upsert diam-diam. Kolom
	 * {@code ID_PRODUK} WAJIB (dari template hasil "Unduh Excel") -- baris tanpa itu dilewati.</p>
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin), {@code file_base64} (wajib).
	 * @param hasil   diisi {@code status="00"}, {@code disimpan} (jumlah baris berhasil), {@code dilewati} (jumlah baris gagal/tak lengkap).
	 */
	public static void soImporExcel(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilSo = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalSo = pemanggilSo == null;
		boolean supervisorSo = pemanggilSo != null && Boolean.TRUE.equals(pemanggilSo.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggilSo, adminGlobalSo, supervisorSo, "stokopname", "create")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengunggah Stok Opname.");
			return;
		}
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
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

		String oleh = tbmuser == null ? "excel-import-stok-opname" : tbmuser.getUserId();
		int disimpan = 0;
		int dilewati = 0;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes));
			XSSFSheet sheet = wb.getSheetAt(0);
			int lastRow = sheet.getLastRowNum();
			for (int r = 1; r <= lastRow; r++) {
				XSSFRow row = sheet.getRow(r);
				if (row == null) continue;
				String idProdukTeks = Common.getCellContent(row.getCell(1)).trim();
				String stokFisikTeks = Common.getCellContent(row.getCell(5)).trim();
				String keterangan = Common.getCellContent(row.getCell(7)).trim();
				if (idProdukTeks.isEmpty() || stokFisikTeks.isEmpty()) {
					dilewati++;
					continue;
				}
				try {
					Long produkId = Long.valueOf(idProdukTeks);
					double stokFisik = parseAngkaAman(stokFisikTeks);
					session.beginTransaction();
					ais.action.master.inventory.StokOpnameScanUtil.simpanOpname(session, tokoId, produkId, stokFisik, keterangan, oleh);
					session.getTransaction().commit();
					disimpan++;
				} catch (Exception eBaris) {
					try {
						if (session.getTransaction() != null && session.getTransaction().isActive()) {
							session.getTransaction().rollback();
						}
					} catch (Exception eRollback) {
						ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:soImporExcel-rollback");
					}
					dilewati++;
				}
			}
			hasil.put("status", "00");
			hasil.put("disimpan", disimpan);
			hasil.put("dilewati", dilewati);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Mutasi Stok Antar Outlet (Fase 3 roadmap F&amp;B, klien item #5) -- transfer stok dari toko
	 * asal ke toko tujuan.</h3>
	 *
	 * <p>Karena tiap outlet punya baris {@link Produk} TERPISAH utk "barang yang sama", produk tujuan
	 * dicoba di-match OTOMATIS di toko tujuan lewat kode/barcode produk asal (persis kode ATAU persis
	 * barcode, keduanya case-insensitive/trim) -- kalau ketemu TEPAT SATU kandidat, dipakai langsung;
	 * kalau ketemu nol atau lebih dari satu (ambigu), method ini menolak dgn {@code status="92"} +
	 * {@code butuhPilihManual=true} supaya klien menampilkan picker cari-produk di toko tujuan lalu
	 * mengirim ulang dgn {@code produk_tujuan_id} eksplisit (melewati auto-match sepenuhnya).</p>
	 *
	 * <p>Baris {@link ais.database.model.inventory.MutasiStokToko} yg tersimpan LANGSUNG memicu
	 * {@link StokKantinUtil#recomputeStokProdukNative} utk KEDUA produk (asal &amp; tujuan) -- efeknya
	 * terlihat seketika di Kasir kedua toko, TANPA langkah approval terpisah, konsisten dgn "SO by
	 * Scan"/Kulakan yg sudah ada.</p>
	 *
	 * <p>Gerbang SENGAJA supervisor/admin-only (TIDAK memakai matriks hak-akses granular {@code
	 * bolehAksiCrud} spt menu lain) -- fitur baru & lintas-2-toko sekaligus, mulai dari default paling
	 * ketat lebih aman drpd langsung mengekspos ke kasir biasa.</p>
	 *
	 * @param request payload: {@code toko_id} (toko ASAL -- wajib utk admin, diabaikan/dikunci utk
	 *                pedagang, pola sama {@link #soResolveTokoId}), {@code produk_asal_id} (wajib),
	 *                {@code toko_tujuan_id} (wajib), {@code produk_tujuan_id} (opsional -- lewati
	 *                auto-match bila diisi), {@code qty} (wajib, &gt;0), {@code keterangan} (opsional).
	 * @param hasil   diisi {@code status="00"}, {@code id}, {@code produkTujuanNama} bila berhasil;
	 *                {@code status="92"}+{@code butuhPilihManual=true}+{@code kandidat} (array
	 *                {@code {id,nama,kode}}, 0 atau &gt;1 baris) bila auto-match gagal.
	 */
	public static void mutasiStokSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggil = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobal = pemanggil == null;
		boolean supervisor = pemanggil != null && Boolean.TRUE.equals(pemanggil.getSupervisor());
		if (!adminGlobal && !supervisor) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mencatat Mutasi Stok Antar Outlet.");
			return;
		}
		Long tokoAsalId = soResolveTokoId(tbmuser, request);
		if (tokoAsalId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko asal tidak diketahui.");
			return;
		}
		Long produkAsalId = request.isNull("produk_asal_id") ? null : Long.valueOf((request.get("produk_asal_id") + "").trim());
		Long tokoTujuanId = request.isNull("toko_tujuan_id") ? null : Long.valueOf((request.get("toko_tujuan_id") + "").trim());
		if (produkAsalId == null || tokoTujuanId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Produk asal dan toko tujuan wajib diisi.");
			return;
		}
		if (tokoTujuanId.equals(tokoAsalId)) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tujuan tidak boleh sama dengan toko asal.");
			return;
		}
		if (!request.has("qty") || request.isNull("qty") || request.getDouble("qty") <= 0) {
			hasil.put("status", "91");
			hasil.put("description", "Jumlah (qty) wajib diisi lebih dari 0.");
			return;
		}
		double qty = request.getDouble("qty");
		String keterangan = request.optString("keterangan", "");
		String oleh = tbmuser == null ? "admin" : tbmuser.getUserId();

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Produk produkAsal = (Produk) session.get(Produk.class, produkAsalId);
			if (produkAsal == null || produkAsal.getToko() == null || !produkAsal.getToko().getId().equals(tokoAsalId)) {
				hasil.put("status", "91");
				hasil.put("description", "Produk asal tidak ditemukan di toko asal.");
				return;
			}
			if (produkAsal.getStok() == null || produkAsal.getStok() < qty) {
				hasil.put("status", "91");
				hasil.put("description", "Stok produk asal tidak cukup (tersedia " + (produkAsal.getStok() == null ? 0 : produkAsal.getStok()) + ").");
				return;
			}
			Toko tokoTujuan = (Toko) session.get(Toko.class, tokoTujuanId);
			if (tokoTujuan == null) {
				hasil.put("status", "91");
				hasil.put("description", "Toko tujuan tidak ditemukan.");
				return;
			}

			Produk produkTujuan;
			Long produkTujuanIdEksplisit = request.isNull("produk_tujuan_id") ? null : Long.valueOf((request.get("produk_tujuan_id") + "").trim());
			if (produkTujuanIdEksplisit != null) {
				produkTujuan = (Produk) session.get(Produk.class, produkTujuanIdEksplisit);
				if (produkTujuan == null || produkTujuan.getToko() == null || !produkTujuan.getToko().getId().equals(tokoTujuanId)) {
					hasil.put("status", "91");
					hasil.put("description", "Produk tujuan tidak ditemukan di toko tujuan.");
					return;
				}
			} else {
				String kodeAsal = produkAsal.getKode() == null ? "" : produkAsal.getKode().trim();
				String barcodeAsal = produkAsal.getBarcode() == null ? "" : produkAsal.getBarcode().trim();
				org.hibernate.criterion.Disjunction pencocokan = Restrictions.disjunction();
				boolean adaKriteria = false;
				if (!kodeAsal.isEmpty()) { pencocokan.add(Restrictions.eq("kode", kodeAsal).ignoreCase()); adaKriteria = true; }
				if (!barcodeAsal.isEmpty()) { pencocokan.add(Restrictions.eq("barcode", barcodeAsal).ignoreCase()); adaKriteria = true; }
				@SuppressWarnings("unchecked")
				java.util.List<Produk> kandidat = adaKriteria
						? session.createCriteria(Produk.class)
								.add(Restrictions.eq("toko", tokoTujuan))
								.add(Restrictions.eq("aktif", true))
								.add(pencocokan)
								.list()
						: new java.util.ArrayList<Produk>();
				if (kandidat.size() != 1) {
					hasil.put("status", "92");
					hasil.put("butuhPilihManual", true);
					JSONArray arr = new JSONArray();
					for (Produk k : kandidat) {
						JSONObject o = new JSONObject();
						o.put("id", k.getId());
						o.put("nama", k.getNama());
						o.put("kode", k.getKode());
						arr.put(o);
					}
					hasil.put("kandidat", arr);
					hasil.put("description", kandidat.isEmpty()
							? "Produk yang sama tidak ditemukan otomatis di toko tujuan -- pilih manual."
							: "Ditemukan lebih dari satu kandidat produk di toko tujuan -- pilih manual.");
					return;
				}
				produkTujuan = kandidat.get(0);
			}

			session.beginTransaction();
			ais.database.model.inventory.MutasiStokToko m = new ais.database.model.inventory.MutasiStokToko();
			m.setProdukAsal(produkAsal);
			m.setProdukTujuan(produkTujuan);
			m.setTokoAsal(produkAsal.getToko());
			m.setTokoTujuan(tokoTujuan);
			m.setQty(qty);
			m.setWaktu(ais.ui.util.WaktuUtil.getDate());
			m.setKeterangan(keterangan);
			m.setOleh(oleh);
			session.save(m);
			session.flush();
			transferBatchFefo(session, produkAsal, produkTujuan, qty, "MUTASI-" + m.getId(), oleh);
			ais.action.master.inventory.StokKantinUtil.recomputeStokProdukNative(session, produkAsal.getId());
			ais.action.master.inventory.StokKantinUtil.recomputeStokProdukNative(session, produkTujuan.getId());
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", m.getId());
			hasil.put("produkTujuanNama", produkTujuan.getNama());
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:mutasiStokSimpan-rollback");
			}
			hasil.put("status", "91");
			hasil.put("description", Common.tampilErrorJikaAdmin(e));
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Mutasi Stok Antar Outlet -- riwayat.</h3>
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin, dikunci ke toko sendiri utk pedagang
	 *                -- riwayat mencakup baris di mana toko ybs jadi ASAL ATAU TUJUAN), {@code limit}
	 *                (opsional, default 50, maks 200).
	 * @param hasil   diisi {@code status="00"}, {@code data} (array {@code {id, waktu, arah
	 *                ("masuk"/"keluar"), produkAsalNama, produkTujuanNama, tokoAsalNama, tokoTujuanNama,
	 *                qty, keterangan, oleh}}).
	 */
	public static void mutasiStokList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		int limit = Math.min(request.optInt("limit", 50), 200);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			@SuppressWarnings("unchecked")
			java.util.List<Object[]> rows = session.createSQLQuery(
					"SELECT m.id, TO_CHAR(m.waktu, 'YYYY-MM-DD HH24:MI') as waktu, "
							+ "pa.nama AS produk_asal_nama, pt.nama AS produk_tujuan_nama, "
							+ "ta.nama AS toko_asal_nama, tt.nama AS toko_tujuan_nama, "
							+ "m.toko_asal, m.toko_tujuan, m.qty, m.keterangan, m.oleh "
							+ "FROM koperasi.mutasi_stok_toko m "
							+ "LEFT JOIN koperasi.produk pa ON m.produk_asal = pa.id "
							+ "LEFT JOIN koperasi.produk pt ON m.produk_tujuan = pt.id "
							+ "LEFT JOIN koperasi.toko ta ON m.toko_asal = ta.id "
							+ "LEFT JOIN koperasi.toko tt ON m.toko_tujuan = tt.id "
							+ "WHERE m.toko_asal = :tokoId OR m.toko_tujuan = :tokoId "
							+ "ORDER BY m.waktu DESC, m.id DESC LIMIT :limit")
					.setParameter("tokoId", tokoId)
					.setParameter("limit", limit)
					.list();
			JSONArray data = new JSONArray();
			for (Object[] r : rows) {
				JSONObject o = new JSONObject();
				o.put("id", r[0]);
				o.put("waktu", r[1] == null ? "" : r[1].toString());
				o.put("produkAsalNama", r[2] == null ? "" : r[2].toString());
				o.put("produkTujuanNama", r[3] == null ? "" : r[3].toString());
				o.put("tokoAsalNama", r[4] == null ? "" : r[4].toString());
				o.put("tokoTujuanNama", r[5] == null ? "" : r[5].toString());
				Long baristTokoAsalId = r[6] == null ? null : ((Number) r[6]).longValue();
				o.put("arah", tokoId.equals(baristTokoAsalId) ? "keluar" : "masuk");
				o.put("qty", r[8] == null ? 0 : ((Number) r[8]).doubleValue());
				o.put("keterangan", r[9] == null ? "" : r[9].toString());
				o.put("oleh", r[10] == null ? "" : r[10].toString());
				data.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", data);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>"Monitor Keluar/Masuk Barang" -- buku besar mutasi stok (gap-closure, layar Stok Opname
	 * Flutter/Electron).</h3>
	 *
	 * <p>Beda dari {@link #mutasiStokList} (HANYA mutasi antar-outlet) -- method ini menyatukan
	 * SEMUA 7 sumber yang dibaca {@link StokKantinUtil#formulaStokSql} (satu-satunya rumus kanonik
	 * stok, lihat javadoc kelas itu) jadi SATU daftar kronologis: Pengadaan/Kulakan (+), Stok Opname
	 * (selisih, sudah bertanda), Penjualan (-), Pemakaian Bahan Baku (-), Retur Penjualan (+, HANYA
	 * baris {@code kembalikan_ke_stok=true}), Mutasi Antar Outlet (+ di toko tujuan / - di toko asal,
	 * dari baris yang SAMA), dan Retur Pembelian (-). Kasir/admin bisa langsung lihat KENAPA stok
	 * satu produk berubah tanpa harus membuka 5+ layar berbeda satu-satu.</p>
	 *
	 * <p>Kolom tabel per sumber TIDAK seragam (lihat {@link StokKantinUtil#formulaStokSql} utk nama
	 * kolom native tiap tabel) -- beberapa TANPA {@code @Column} eksplisit jadi nama kolom DB-nya
	 * collapse tanpa underscore (mis. {@code waktupengadaan}, {@code waktuopname}, {@code namasupplier}
	 * -- lihat memory project soal Hibernate implicit-naming). Jangan disamakan formatnya dgn kolom
	 * yang MEMANG punya underscore ({@code kembalikan_ke_stok}, {@code kode_faktur_asal}, dst).</p>
	 *
	 * <p>Dibatasi {@code hari} (default 30, maks 365) -- BUKAN filter tanggal presisi spt laporan lain
	 * (unduhan Excel), krn 8 cabang UNION ALL tanpa batas waktu bisa jadi mahal di toko yg sudah lama
	 * jalan. Ambil {@code limit+1} baris utk deteksi murah "adaLagi" (paging "muat lebih banyak")
	 * tanpa perlu query {@code COUNT(*)} kedua yang sama mahalnya dgn query utama.</p>
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin, dikunci ke toko sendiri utk pedagang),
	 *                {@code produk_id} (opsional -- filter satu produk, utk lihat riwayat lengkapnya),
	 *                {@code hari} (opsional, default 30, maks 365), {@code limit} (opsional, default
	 *                100, maks 300), {@code offset} (opsional, default 0, utk "muat lebih banyak").
	 * @param hasil   diisi {@code status="00"}, {@code data} (array {@code waktu, jenis, produkId,
	 *                produkKode, produkNama, qty, keterangan}, terurut waktu TERBARU dulu), dan
	 *                {@code adaLagi} (boolean).
	 */
	public static void stokMutasiLedger(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		Long produkId = request.isNull("produk_id") ? null : Long.valueOf(request.get("produk_id").toString());
		int hari = Math.min(Math.max(request.optInt("hari", 30), 1), 365);
		int limit = Math.min(Math.max(request.optInt("limit", 100), 1), 300);
		int offset = Math.max(request.optInt("offset", 0), 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String sejak = "(now() - interval '" + hari + " days')";
			String filterProduk = produkId == null ? "" : " AND u.produk_id = " + produkId;
			String sql = "SELECT u.waktu, u.jenis, u.produk_id, pr.kode AS produk_kode, pr.nama AS produk_nama, u.qty, u.keterangan FROM ("
					+ "SELECT p.waktupengadaan AS waktu, 'Pengadaan (Kulakan)' AS jenis, p.produk AS produk_id, p.qty AS qty, "
					+ "TRIM(BOTH ' - ' FROM COALESCE(p.namasupplier,'') || CASE WHEN p.nomorfaktur IS NOT NULL AND p.nomorfaktur <> '' THEN ' - Faktur ' || p.nomorfaktur ELSE '' END) AS keterangan "
					+ "FROM koperasi.pengadaan_produk p WHERE p.toko = " + tokoId + " AND p.waktupengadaan >= " + sejak
					+ " UNION ALL "
					+ "SELECT a.waktuopname AS waktu, 'Stok Opname' AS jenis, a.produk AS produk_id, a.selisih AS qty, COALESCE(a.keterangan,'') AS keterangan "
					+ "FROM koperasi.stok_opname a WHERE a.toko = " + tokoId + " AND a.waktuopname >= " + sejak
					+ " UNION ALL "
					+ "SELECT b.waktu AS waktu, 'Penjualan' AS jenis, b.produk AS produk_id, -b.qty AS qty, COALESCE(b.keterangan,'') AS keterangan "
					+ "FROM koperasi.pembelian b WHERE b.toko = " + tokoId + " AND b.waktu >= " + sejak
					+ " UNION ALL "
					+ "SELECT c.waktu AS waktu, 'Pemakaian Bahan Baku' AS jenis, c.produk AS produk_id, -c.qty AS qty, COALESCE(c.keterangan,'') AS keterangan "
					+ "FROM koperasi.pemakaian_bahan_baku c WHERE c.toko = " + tokoId + " AND c.waktu >= " + sejak
					+ " UNION ALL "
					+ "SELECT d.waktu AS waktu, 'Retur Penjualan' AS jenis, d.produk AS produk_id, d.qty AS qty, "
					+ "('Dari ' || COALESCE(d.namapembeli,'-') || CASE WHEN d.alasan IS NOT NULL AND d.alasan <> '' THEN ': ' || d.alasan ELSE '' END) AS keterangan "
					+ "FROM koperasi.retur_penjualan d WHERE d.toko = " + tokoId + " AND d.kembalikan_ke_stok = true AND d.waktu >= " + sejak
					+ " UNION ALL "
					+ "SELECT e.waktu AS waktu, 'Mutasi Masuk (Antar Outlet)' AS jenis, e.produk_tujuan AS produk_id, e.qty AS qty, COALESCE(e.keterangan,'') AS keterangan "
					+ "FROM koperasi.mutasi_stok_toko e WHERE e.toko_tujuan = " + tokoId + " AND e.produk_tujuan IS NOT NULL AND e.waktu >= " + sejak
					+ " UNION ALL "
					+ "SELECT e2.waktu AS waktu, 'Mutasi Keluar (Antar Outlet)' AS jenis, e2.produk_asal AS produk_id, -e2.qty AS qty, COALESCE(e2.keterangan,'') AS keterangan "
					+ "FROM koperasi.mutasi_stok_toko e2 WHERE e2.toko_asal = " + tokoId + " AND e2.produk_asal IS NOT NULL AND e2.waktu >= " + sejak
					+ " UNION ALL "
					+ "SELECT f.waktu AS waktu, 'Retur Pembelian' AS jenis, f.produk AS produk_id, -f.qty AS qty, "
					+ "(CASE WHEN f.kode_faktur_asal IS NOT NULL AND f.kode_faktur_asal <> '' THEN 'Faktur ' || f.kode_faktur_asal || ' ' ELSE '' END || COALESCE(f.alasan,'')) AS keterangan "
					+ "FROM koperasi.retur_pembelian f WHERE f.toko = " + tokoId + " AND f.waktu >= " + sejak
					+ ") u LEFT JOIN koperasi.produk pr ON pr.id = u.produk_id"
					+ " WHERE 1=1" + filterProduk
					+ " ORDER BY u.waktu DESC LIMIT " + (limit + 1) + " OFFSET " + offset;
			@SuppressWarnings("unchecked")
			java.util.List<Object[]> rows = session.createSQLQuery(sql).list();
			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
			boolean adaLagi = rows.size() > limit;
			JSONArray data = new JSONArray();
			for (int i = 0; i < Math.min(rows.size(), limit); i++) {
				Object[] r = rows.get(i);
				JSONObject o = new JSONObject();
				o.put("waktu", r[0] == null ? "" : fmt.format((java.util.Date) r[0]));
				o.put("jenis", r[1] == null ? "" : r[1].toString());
				o.put("produkId", r[2] == null ? null : ((Number) r[2]).longValue());
				o.put("produkKode", r[3] == null ? "" : r[3].toString());
				o.put("produkNama", r[4] == null ? "(produk terhapus)" : r[4].toString());
				o.put("qty", r[5] == null ? 0 : ((Number) r[5]).doubleValue());
				o.put("keterangan", r[6] == null ? "" : r[6].toString());
				data.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", data);
			hasil.put("adaLagi", adaLagi);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Ringkasan mutasi gudang per produk untuk tab Produk > Mutasi Barang.
	 * Saldo akhir historis diturunkan dari stok saat ini dikurangi seluruh mutasi
	 * setelah tanggal akhir; cara ini tetap mengakui stok awal hasil migrasi/impor
	 * yang tidak mempunyai dokumen mutasi lama. Nilai persediaan memakai harga beli
	 * produk saat laporan dibuat, konsisten dengan KPI Nilai Stok.
	 */
	public static void produkMutasiRingkasan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		String dari = request.optString("dari", "").trim();
		String sampai = request.optString("sampai", "").trim();
		if (!dari.matches("\\d{4}-\\d{2}-\\d{2}") || !sampai.matches("\\d{4}-\\d{2}-\\d{2}")) {
			hasil.put("status", "91");
			hasil.put("description", "Tanggal mulai dan akhir wajib diisi dengan format yyyy-MM-dd.");
			return;
		}
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(500, Math.max(1, request.optInt("page_size", 15)));
		String keyword = request.optString("keyword", "").trim();
		Long kategoriId = request.isNull("kategori_id") ? null
				: Long.valueOf((request.get("kategori_id") + "").trim());

		String tid = tokoId.toString();
		String sql = "WITH u AS ("
				+ "SELECT p.produk produk_id,p.waktupengadaan waktu,p.qty qty FROM koperasi.pengadaan_produk p WHERE p.toko=" + tid + " AND p.waktupengadaan>=CAST(? AS date) "
				+ "UNION ALL SELECT a.produk,a.waktuopname,a.selisih FROM koperasi.stok_opname a WHERE a.toko=" + tid + " AND a.waktuopname>=CAST(? AS date) "
				+ "UNION ALL SELECT b.produk,b.waktu,-b.qty FROM koperasi.pembelian b WHERE b.toko=" + tid + " AND b.waktu>=CAST(? AS date) "
				+ "UNION ALL SELECT c.produk,c.waktu,-c.qty FROM koperasi.pemakaian_bahan_baku c WHERE c.toko=" + tid + " AND c.waktu>=CAST(? AS date) "
				+ "UNION ALL SELECT d.produk,d.waktu,d.qty FROM koperasi.retur_penjualan d WHERE d.toko=" + tid + " AND d.kembalikan_ke_stok=true AND d.waktu>=CAST(? AS date) "
				+ "UNION ALL SELECT e.produk_tujuan,e.waktu,e.qty FROM koperasi.mutasi_stok_toko e WHERE e.toko_tujuan=" + tid + " AND e.produk_tujuan IS NOT NULL AND e.waktu>=CAST(? AS date) "
				+ "UNION ALL SELECT e2.produk_asal,e2.waktu,-e2.qty FROM koperasi.mutasi_stok_toko e2 WHERE e2.toko_asal=" + tid + " AND e2.produk_asal IS NOT NULL AND e2.waktu>=CAST(? AS date) "
				+ "UNION ALL SELECT f.produk,f.waktu,-f.qty FROM koperasi.retur_pembelian f WHERE f.toko=" + tid + " AND f.waktu>=CAST(? AS date)), "
				+ "a AS (SELECT produk_id,"
				+ "COALESCE(SUM(CASE WHEN waktu<CAST(? AS date)+interval '1 day' AND qty>0 THEN qty ELSE 0 END),0) masuk,"
				+ "COALESCE(SUM(CASE WHEN waktu<CAST(? AS date)+interval '1 day' AND qty<0 THEN -qty ELSE 0 END),0) keluar,"
				+ "COALESCE(SUM(CASE WHEN waktu>=CAST(? AS date)+interval '1 day' THEN qty ELSE 0 END),0) sesudah FROM u GROUP BY produk_id),"
				+ "r AS (SELECT COALESCE(j.nama,'TANPA KATEGORI') kategori,p.kode,COALESCE(p.barcode,'') barcode,p.nama,"
				+ "COALESCE(p.hargabeli,0) harga_beli,COALESCE(a.masuk,0) masuk,COALESCE(a.keluar,0) keluar,"
				+ "COALESCE(p.stok,0)-COALESCE(a.sesudah,0) saldo_akhir,"
				+ "COALESCE(p.stok,0)-COALESCE(a.sesudah,0)-COALESCE(a.masuk,0)+COALESCE(a.keluar,0) saldo_awal "
				+ "FROM koperasi.produk p LEFT JOIN koperasi.jenis_produk j ON j.id=p.jenis_produk LEFT JOIN a ON a.produk_id=p.id "
				+ "WHERE p.toko=? "
				+ (kategoriId == null ? "" : "AND p.jenis_produk=? ")
				+ (keyword.isEmpty() ? "" : "AND (LOWER(COALESCE(p.nama,'')) LIKE ? OR LOWER(COALESCE(p.kode,'')) LIKE ? OR LOWER(COALESCE(p.barcode,'')) LIKE ?) ")
				+ ") SELECT kategori,kode,barcode,nama,saldo_awal,saldo_awal*harga_beli nilai_awal,masuk,masuk*harga_beli nilai_masuk,"
				+ "keluar,keluar*harga_beli nilai_keluar,saldo_akhir,saldo_akhir*harga_beli nilai_akhir,"
				+ "COUNT(*) OVER() total_baris,SUM(saldo_awal) OVER() t_saldo_awal,SUM(saldo_awal*harga_beli) OVER() t_nilai_awal,"
				+ "SUM(masuk) OVER() t_masuk,SUM(masuk*harga_beli) OVER() t_nilai_masuk,SUM(keluar) OVER() t_keluar,"
				+ "SUM(keluar*harga_beli) OVER() t_nilai_keluar,SUM(saldo_akhir) OVER() t_saldo_akhir,SUM(saldo_akhir*harga_beli) OVER() t_nilai_akhir "
				+ "FROM r ORDER BY kategori,nama,kode LIMIT ? OFFSET ?";
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.PreparedStatement ps = session.connection().prepareStatement(sql);
			int i = 1;
			for (int x = 0; x < 8; x++) ps.setString(i++, dari);
			ps.setString(i++, sampai); ps.setString(i++, sampai); ps.setString(i++, sampai);
			ps.setLong(i++, tokoId.longValue());
			if (kategoriId != null) ps.setLong(i++, kategoriId.longValue());
			if (!keyword.isEmpty()) {
				String q = "%" + keyword.toLowerCase() + "%";
				ps.setString(i++, q); ps.setString(i++, q); ps.setString(i++, q);
			}
			ps.setInt(i++, pageSize); ps.setInt(i++, (page - 1) * pageSize);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray data = new JSONArray();
			JSONObject summary = new JSONObject();
			long total = 0;
			while (rs.next()) {
				JSONObject o = new JSONObject();
				o.put("kategori", rs.getString(1)); o.put("kode", rs.getString(2));
				o.put("barcode", rs.getString(3)); o.put("nama", rs.getString(4));
				o.put("saldoAwal", rs.getDouble(5)); o.put("nilaiAwal", rs.getDouble(6));
				o.put("masuk", rs.getDouble(7)); o.put("nilaiMasuk", rs.getDouble(8));
				o.put("keluar", rs.getDouble(9)); o.put("nilaiKeluar", rs.getDouble(10));
				o.put("saldoAkhir", rs.getDouble(11)); o.put("nilaiAkhir", rs.getDouble(12));
				data.put(o); total = rs.getLong(13);
				summary.put("saldoAwal", rs.getDouble(14)); summary.put("nilaiAwal", rs.getDouble(15));
				summary.put("masuk", rs.getDouble(16)); summary.put("nilaiMasuk", rs.getDouble(17));
				summary.put("keluar", rs.getDouble(18)); summary.put("nilaiKeluar", rs.getDouble(19));
				summary.put("saldoAkhir", rs.getDouble(20)); summary.put("nilaiAkhir", rs.getDouble(21));
			}
			rs.close(); ps.close();
			hasil.put("status", "00"); hasil.put("data", data); hasil.put("total", total);
			hasil.put("page", page); hasil.put("pageSize", pageSize); hasil.put("summary", summary);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Sinkronkan Ulang Stok &amp; Harga Modal (gap-closure bug JSP {@code pengadaan/index.jsp}
	 * 2026-08-11).</h3>
	 *
	 * <p>Tombol JSP "Sinkronkan Stok &amp; Harga Modal" SEBELUMNYA menjalankan UPDATE SQL mentah di
	 * klien dgn rumus stok 3-suku USANG (pengadaan + Σselisih opname − pembelian) yg ditulis SEBELUM
	 * {@link StokKantinUtil#formulaStokSql} tumbuh jadi 7 suku sepanjang sesi ini (pemakaian bahan
	 * baku, retur penjualan, mutasi stok antar outlet ×2, retur pembelian) -- kalau tombol lama itu
	 * diklik, SEMUA kontribusi 5 suku baru itu akan HILANG DIAM-DIAM (stok ditimpa balik ke versi
	 * lama yg salah). Method ini menggantikannya dgn memanggil {@link
	 * StokKantinUtil#recomputeStokProdukNative} (rumus KANONIK, SATU sumber kebenaran, varian
	 * ber-{@code session} eksplisit -- BUKAN {@code recomputeStokProduk(Long)} yg bergantung pada
	 * {@code HibernateUtil.currentSession()} ambient milik konteks ZK/FilterJSP, tak cocok dipakai dari
	 * sini yg membuka session sendiri spt {@link #mutasiStokSimpan}/{@link #kulakanFakturSimpan}) per
	 * produk milik toko ybs, plus penyesuaian harga beli/jual dari pengadaan terakhir ditulis ulang
	 * inline di sini (logic IDENTIK {@code recomputeStokProduk(Long)}, disalin krn varian native itu
	 * sengaja TIDAK menyentuh harga) -- meniru maksud tombol lama, lewat jalur yg sudah benar.</p>
	 *
	 * <p>Gerbang SAMA dgn {@link #mutasiStokSimpan} (admin/supervisor-only) -- operasi berat yg
	 * menyentuh SEMUA produk toko, bukan utk kasir biasa.</p>
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin, dikunci ke toko sendiri utk pedagang).
	 * @param hasil   diisi {@code status="00"}, {@code jumlahProduk} (banyak produk yg direcompute).
	 */
	public static void sinkronStokToko(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilSs = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalSs = pemanggilSs == null;
		boolean supervisorSs = pemanggilSs != null && Boolean.TRUE.equals(pemanggilSs.getSupervisor());
		if (!adminGlobalSs && !supervisorSs) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat menyinkronkan stok.");
			return;
		}
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			@SuppressWarnings("unchecked")
			java.util.List<Object> ids = session.createSQLQuery("SELECT id FROM koperasi.produk WHERE toko = " + tokoId).list();
			session.beginTransaction();
			int jumlah = 0;
			for (Object o : ids) {
				Long produkId = ((Number) o).longValue();
				try {
					ais.action.master.inventory.StokKantinUtil.recomputeStokProdukNative(session, produkId);
					Object latestObj = session.createSQLQuery(
							"SELECT hargabelisatuan FROM koperasi.pengadaan_produk WHERE produk = " + produkId
									+ " ORDER BY waktupengadaan DESC, id DESC LIMIT 1").uniqueResult();
					double latest = latestObj == null ? 0.0 : ((Number) latestObj).doubleValue();
					if (latest > 0) {
						Produk p = (Produk) session.get(Produk.class, produkId);
						if (p != null) {
							p.setHargaBeli(latest);
							double hargaJual = p.getHargaJual() == null ? 0 : p.getHargaJual();
							if (latest >= hargaJual) {
								p.setHargaJual(latest);
							}
							session.update(p);
						}
					}
					jumlah++;
				} catch (Exception eSatu) {
					ais.common.ErrorAuditUtil.record(eSatu, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:sinkronStokToko-satu");
				}
			}
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("jumlahProduk", jumlah);
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:sinkronStokToko-rollback");
			}
			hasil.put("status", "91");
			hasil.put("description", Common.tampilErrorJikaAdmin(e));
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Mutasi Stok Antar Outlet -- daftar SEMUA toko aktif (bukan cuma toko yg boleh diakses
	 * pemanggil).</h3>
	 *
	 * <p>SENGAJA beda dari {@link #daftarTokoSaya} ({@code daftarTokoBolehDiakses} -- "toko milik
	 * saya", cuma 1 baris utk pedagang toko-terkunci) -- picker "Toko Tujuan" di fitur ini harus bisa
	 * menunjuk toko MANAPUN di sistem (itulah maksud "antar outlet"), bukan cuma toko sendiri. JSP
	 * asli memakai raw SQL lewat aksi {@code sql} (hanya tersedia utk sesi JSP cookie-based); Electron
	 * &amp; Flutter (Bearer-token PosApi) tidak punya jalur itu, jadi aksi kecil ini dibuat sebagai
	 * padanannya. Nama toko bukan data sensitif -- gerbang cukup "sudah login", sama spt {@link
	 * #penyediaList}.</p>
	 */
	public static void mutasiStokTokoList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			@SuppressWarnings("unchecked")
			java.util.List<Toko> daftar = session.createCriteria(Toko.class)
					.add(Restrictions.eq("aktif", true))
					.addOrder(org.hibernate.criterion.Order.asc("nama")).list();
			JSONArray arr = new JSONArray();
			for (Toko t : daftar) {
				JSONObject j = new JSONObject();
				j.put("id", t.getId());
				j.put("nama", t.getNama());
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Mutasi Stok Antar Outlet -- cari produk di toko MANAPUN (dipakai picker Produk Asal &amp;
	 * picker manual Produk Tujuan).</h3>
	 *
	 * <p>Beda dari {@code prosesKatalog} (PosApi, terkunci ke toko pemanggil via {@code
	 * resolveTokoId}) -- {@code toko_id} di sini WAJIB dikirim eksplisit oleh klien (bukan
	 * auto-resolve ke toko pemanggil), karena titik pakainya justru untuk melihat toko LAIN. Gerbang
	 * SAMA dgn {@link #mutasiStokSimpan} (admin/supervisor-only) -- fitur lintas-toko, bukan utk kasir
	 * biasa. Hanya {@code jenisItem} "JUAL" (pola OR-IS-NULL) yg dikembalikan -- Bahan Baku/Ekstra
	 * tidak relevan utk transfer stok antar outlet.</p>
	 *
	 * @param request payload: {@code toko_id} (wajib), {@code keyword} (opsional).
	 * @param hasil   diisi {@code status="00"}, {@code data} (array {@code {id,nama,kode,stok}},
	 *                maks 50 baris).
	 */
	public static void mutasiStokProdukList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilMp = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalMp = pemanggilMp == null;
		boolean supervisorMp = pemanggilMp != null && Boolean.TRUE.equals(pemanggilMp.getSupervisor());
		if (!adminGlobalMp && !supervisorMp) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat memakai Mutasi Stok Antar Outlet.");
			return;
		}
		Long tokoId = request.isNull("toko_id") ? null : Long.valueOf((request.get("toko_id") + "").trim());
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko wajib diisi.");
			return;
		}
		String keyword = request.optString("keyword", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			if (toko == null) {
				hasil.put("status", "91");
				hasil.put("description", "Toko tidak ditemukan.");
				return;
			}
			org.hibernate.Criteria c = session.createCriteria(Produk.class)
					.add(Restrictions.eq("toko", toko))
					.add(Restrictions.eq("aktif", true))
					.add(Restrictions.or(Restrictions.isNull("jenisItem"), Restrictions.eq("jenisItem", "JUAL")))
					.addOrder(org.hibernate.criterion.Order.asc("nama"));
			if (!keyword.isEmpty()) {
				c.add(Restrictions.or(
						Restrictions.ilike("nama", keyword, org.hibernate.criterion.MatchMode.ANYWHERE),
						Restrictions.ilike("kode", keyword, org.hibernate.criterion.MatchMode.ANYWHERE)));
			}
			c.setMaxResults(50);
			@SuppressWarnings("unchecked")
			java.util.List<Produk> daftar = c.list();
			JSONArray arr = new JSONArray();
			for (Produk p : daftar) {
				JSONObject j = new JSONObject();
				j.put("id", p.getId());
				j.put("nama", p.getNama());
				j.put("kode", p.getKode() == null ? "" : p.getKode());
				j.put("stok", p.getStok() == null ? 0 : p.getStok());
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
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
	public static void pembantuPiutangList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String dari = request.optString("dari", "").trim();
		String sampai = request.optString("sampai", "").trim();
		if (!dari.matches("\\d{4}-\\d{2}-\\d{2}") || !sampai.matches("\\d{4}-\\d{2}-\\d{2}")) {
			hasil.put("status", "91");
			hasil.put("description", "Tanggal Mulai dan Tanggal Akhir wajib diisi dengan format yang benar.");
			return;
		}
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(5000, Math.max(1, request.optInt("page_size", 15)));
		String q = request.optString("q", "").trim().toLowerCase();
		Long idAnggota = request.isNull("id_anggota") ? null
				: Long.valueOf((request.get("id_anggota") + "").trim());

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String n1 = "GREATEST(0, COALESCE(h.total_biaya,0) - COALESCE(h.nominal_bayar_2,0)"
					+ " - COALESCE(h.nominal_bayar_3,0) - COALESCE(h.nominal_bayar_4,0)"
					+ " - COALESCE(h.nominal_bayar_5,0))";
			String hutangPos = "(CASE WHEN COALESCE(c1.masuk_sebagai_hutang,false) THEN " + n1
					+ " ELSE 0 END + CASE WHEN COALESCE(c2.masuk_sebagai_hutang,false) THEN COALESCE(h.nominal_bayar_2,0) ELSE 0 END"
					+ " + CASE WHEN COALESCE(c3.masuk_sebagai_hutang,false) THEN COALESCE(h.nominal_bayar_3,0) ELSE 0 END"
					+ " + CASE WHEN COALESCE(c4.masuk_sebagai_hutang,false) THEN COALESCE(h.nominal_bayar_4,0) ELSE 0 END"
					+ " + CASE WHEN COALESCE(c5.masuk_sebagai_hutang,false) THEN COALESCE(h.nominal_bayar_5,0) ELSE 0 END)";

			StringBuilder sql = new StringBuilder();
			sql.append("WITH events AS (")
				.append(" SELECT h.anggota_koperasi AS id_anggota, a.kode, a.nama, h.tanggal_pembayaran AS tanggal,")
				.append(hutangPos).append("::numeric AS faktur, 0::numeric AS pembayaran, 0::numeric AS retur,")
				.append(" 0::numeric AS uang_muka, 0::numeric AS jurnal_umum")
				.append(" FROM koperasi.pembelian_anggota_koperasi h")
				.append(" JOIN koperasi.anggota_koperasi a ON a.id=h.anggota_koperasi")
				.append(" LEFT JOIN koperasi.cara_pembayaran_koperasi c1 ON c1.id=h.cara_pembayaran_koperasi")
				.append(" LEFT JOIN koperasi.cara_pembayaran_koperasi c2 ON c2.id=h.cara_pembayaran_koperasi_2")
				.append(" LEFT JOIN koperasi.cara_pembayaran_koperasi c3 ON c3.id=h.cara_pembayaran_koperasi_3")
				.append(" LEFT JOIN koperasi.cara_pembayaran_koperasi c4 ON c4.id=h.cara_pembayaran_koperasi_4")
				.append(" LEFT JOIN koperasi.cara_pembayaran_koperasi c5 ON c5.id=h.cara_pembayaran_koperasi_5")
				.append(" WHERE h.anggota_koperasi IS NOT NULL AND ").append(hutangPos).append(" > 0")
				.append(" UNION ALL SELECT ph.anggota_koperasi,a.kode,a.nama,ph.waktu,0,COALESCE(ph.nominal,0),0,0,0")
				.append(" FROM koperasi.pembayaran_hutang ph JOIN koperasi.anggota_koperasi a ON a.id=ph.anggota_koperasi")
				.append(" WHERE ph.anggota_koperasi IS NOT NULL")
				.append(" UNION ALL SELECT rp.anggota_koperasi,a.kode,a.nama,rp.waktu,0,0,COALESCE(rp.totalnilai,0),0,0")
				.append(" FROM koperasi.retur_penjualan rp JOIN koperasi.anggota_koperasi a ON a.id=rp.anggota_koperasi")
				.append(" WHERE rp.anggota_koperasi IS NOT NULL AND rp.pembelian_anggota_koperasi_id IS NOT NULL")
				.append(" AND EXISTS (SELECT 1 FROM koperasi.pembelian_anggota_koperasi hr")
				.append(" LEFT JOIN koperasi.cara_pembayaran_koperasi rc1 ON rc1.id=hr.cara_pembayaran_koperasi")
				.append(" LEFT JOIN koperasi.cara_pembayaran_koperasi rc2 ON rc2.id=hr.cara_pembayaran_koperasi_2")
				.append(" LEFT JOIN koperasi.cara_pembayaran_koperasi rc3 ON rc3.id=hr.cara_pembayaran_koperasi_3")
				.append(" LEFT JOIN koperasi.cara_pembayaran_koperasi rc4 ON rc4.id=hr.cara_pembayaran_koperasi_4")
				.append(" LEFT JOIN koperasi.cara_pembayaran_koperasi rc5 ON rc5.id=hr.cara_pembayaran_koperasi_5")
				.append(" WHERE hr.id=rp.pembelian_anggota_koperasi_id AND (COALESCE(rc1.masuk_sebagai_hutang,false)")
				.append(" OR COALESCE(rc2.masuk_sebagai_hutang,false) OR COALESCE(rc3.masuk_sebagai_hutang,false)")
				.append(" OR COALESCE(rc4.masuk_sebagai_hutang,false) OR COALESCE(rc5.masuk_sebagai_hutang,false)))")
				.append(" UNION ALL SELECT d.customer,a.kode,a.nama,d.tanggal,COALESCE(d.total_faktur,0),0,0,")
				.append(" COALESCE(d.dibayar_awal,0),0 FROM koperasi.piutang_customer_doc d")
				.append(" JOIN koperasi.anggota_koperasi a ON a.id=d.customer WHERE COALESCE(d.status,'AKTIF')='AKTIF'")
				.append(" UNION ALL SELECT p.customer,a.kode,a.nama,p.tanggal,0,")
				.append(" CASE WHEN COALESCE(p.metode,'TUNAI')='RETUR' THEN 0 ELSE COALESCE(p.nominal,0) END,")
				.append(" CASE WHEN COALESCE(p.metode,'TUNAI')='RETUR' THEN COALESCE(p.nominal,0) ELSE 0 END,0,0")
				.append(" FROM koperasi.penerimaan_piutang_customer p JOIN koperasi.anggota_koperasi a ON a.id=p.customer")
				.append(" WHERE COALESCE(p.status_dok,'AKTIF')<>'DIBATALKAN'")
				.append("), rekap AS (SELECT id_anggota,kode,nama,")
				.append(" SUM(CASE WHEN tanggal < ?::date THEN faktur-pembayaran-retur-uang_muka+jurnal_umum ELSE 0 END) saldo_awal,")
				.append(" SUM(CASE WHEN tanggal>=?::date AND tanggal<(?::date+interval '1 day') THEN faktur ELSE 0 END) faktur,")
				.append(" SUM(CASE WHEN tanggal>=?::date AND tanggal<(?::date+interval '1 day') THEN pembayaran ELSE 0 END) pembayaran,")
				.append(" SUM(CASE WHEN tanggal>=?::date AND tanggal<(?::date+interval '1 day') THEN retur ELSE 0 END) retur,")
				.append(" SUM(CASE WHEN tanggal>=?::date AND tanggal<(?::date+interval '1 day') THEN uang_muka ELSE 0 END) uang_muka,")
				.append(" SUM(CASE WHEN tanggal>=?::date AND tanggal<(?::date+interval '1 day') THEN jurnal_umum ELSE 0 END) jurnal_umum")
				.append(" FROM events GROUP BY id_anggota,kode,nama), filtered AS (SELECT *,")
				.append(" saldo_awal+faktur-pembayaran-retur-uang_muka+jurnal_umum AS saldo_akhir FROM rekap WHERE")
				.append(" (ABS(saldo_awal)+ABS(faktur)+ABS(pembayaran)+ABS(retur)+ABS(uang_muka)+ABS(jurnal_umum))>0.009");
			if (idAnggota != null) sql.append(" AND id_anggota=?");
			if (!q.isEmpty()) sql.append(" AND (LOWER(COALESCE(kode,'')) LIKE ? OR LOWER(COALESCE(nama,'')) LIKE ?)");
			sql.append(") SELECT id_anggota,kode,nama,saldo_awal,faktur,pembayaran,retur,uang_muka,jurnal_umum,saldo_akhir,")
				.append(" COUNT(*) OVER() total_data,SUM(saldo_awal) OVER() total_saldo_awal,SUM(faktur) OVER() total_faktur,")
				.append(" SUM(pembayaran) OVER() total_pembayaran,SUM(retur) OVER() total_retur,SUM(uang_muka) OVER() total_uang_muka,")
				.append(" SUM(jurnal_umum) OVER() total_jurnal_umum,SUM(saldo_akhir) OVER() total_saldo_akhir")
				.append(" FROM filtered ORDER BY nama,kode LIMIT ").append(pageSize)
				.append(" OFFSET ").append((page - 1) * pageSize);

			java.sql.PreparedStatement ps = session.connection().prepareStatement(sql.toString());
			int ix = 1;
			ps.setString(ix++, dari);
			for (int i = 0; i < 5; i++) { ps.setString(ix++, dari); ps.setString(ix++, sampai); }
			if (idAnggota != null) ps.setLong(ix++, idAnggota.longValue());
			if (!q.isEmpty()) { ps.setString(ix++, "%" + q + "%"); ps.setString(ix++, "%" + q + "%"); }

			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray data = new JSONArray();
			JSONObject total = new JSONObject();
			long totalData = 0;
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("idAnggota", rs.getLong(1));
				j.put("kodeAnggota", rs.getString(2));
				j.put("namaAnggota", rs.getString(3));
				j.put("saldoAwal", rs.getDouble(4));
				j.put("faktur", rs.getDouble(5));
				j.put("pembayaran", rs.getDouble(6));
				j.put("retur", rs.getDouble(7));
				j.put("uangMuka", rs.getDouble(8));
				j.put("jurnalUmum", rs.getDouble(9));
				j.put("saldoAkhir", rs.getDouble(10));
				data.put(j);
				totalData = rs.getLong(11);
				if (total.length() == 0) {
					total.put("saldoAwal", rs.getDouble(12)); total.put("faktur", rs.getDouble(13));
					total.put("pembayaran", rs.getDouble(14)); total.put("retur", rs.getDouble(15));
					total.put("uangMuka", rs.getDouble(16)); total.put("jurnalUmum", rs.getDouble(17));
					total.put("saldoAkhir", rs.getDouble(18));
				}
			}
			rs.close(); ps.close();
			if (total.length() == 0) {
				total.put("saldoAwal", 0); total.put("faktur", 0); total.put("pembayaran", 0);
				total.put("retur", 0); total.put("uangMuka", 0); total.put("jurnalUmum", 0); total.put("saldoAkhir", 0);
			}
			hasil.put("status", "00"); hasil.put("data", data); hasil.put("total", total);
			hasil.put("totalData", totalData); hasil.put("page", page); hasil.put("pageSize", pageSize);
			hasil.put("totalPages", Math.max(1, (totalData + pageSize - 1) / pageSize));
		} finally {
			tutupSessionPolaB(session);
		}
	}


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
	 * -- pola SAMA PERSIS dgn {@code PosApi.daftarOrderDenganSesi}. Identitas kasir hanya diambil
	 * dari snapshot {@code kasir_login_nama} pada master transaksi; kolom audit {@code oleh/olehId}
	 * tidak boleh dipakai sebagai kasir karena dapat berisi akun integrasi seperti external_update.
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
					+ "         COALESCE(NULLIF(TRIM(MAX(pak.kasir_login_nama)),''),'Kasir tidak tercatat') AS kasir, MAX(pak.nama_mesin) AS mesin "
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
	/**
	 * Kondisi waktu SQL utk {@code periode} -- SAMA PERSIS semantik {@code MonitorDiskonKantinAction
	 * .timeCond(String)} (versi ZK) supaya KPI/tren/top-list Monitor Diskon konsisten antara ZK dan
	 * API JSON (Electron/Flutter), sekalipun implementasinya dobel-tulis (raw JDBC di sini vs
	 * {@code scalar()}/{@code rows()} di sana -- keduanya cuma tipis di atas SQL mentah, tak ada
	 * abstraksi bersama yg bisa dipakai lintas ZK/servlet tanpa refactor lebih besar).
	 *
	 * <p>{@code "last30"} BUKAN pilihan periode versi ZK -- sentinel INTERNAL yg mereproduksi PERSIS
	 * kondisi lama method ini ({@code >= NOW() - INTERVAL '30 days'}) sebelum filter periode
	 * ditambahkan, dipakai sbg DEFAULT saat klien tak mengirim {@code periode} sama sekali. Ini WAJIB
	 * supaya pemanggil lama ({@code RingkasanTabPromo} Flutter, tab "Promo & Cashback" dasbor
	 * Ringkasan -- tak pernah mengirim {@code periode}) TIDAK diam-diam berubah angkanya jadi
	 * "bulan berjalan" begitu parameter baru ini di-deploy -- hanya pemanggil BARU yg sengaja
	 * mengirim {@code periode} eksplisit (mis. tab Monitor Diskon baru) yg dapat semantik ZK.</p>
	 */
	private static String monitorDiskonTimeCond(String periode, String kolom) {
		String p = periode == null ? "last30" : periode;
		if ("today".equals(p)) {
			return " AND " + kolom + " >= current_date ";
		}
		if ("week".equals(p)) {
			return " AND " + kolom + " >= current_date - interval '7 days' ";
		}
		if ("month".equals(p)) {
			return " AND date_trunc('month'," + kolom + ") = date_trunc('month',current_date) ";
		}
		if ("semester".equals(p)) {
			return " AND " + kolom + " >= current_date - interval '6 months' ";
		}
		if ("year".equals(p)) {
			return " AND date_trunc('year'," + kolom + ") = date_trunc('year',current_date) ";
		}
		if ("3years".equals(p)) {
			return " AND " + kolom + " >= current_date - interval '3 years' ";
		}
		// "last30" (default sentinel, lihat javadoc) -- juga fallback utk nilai tak dikenal.
		return " AND " + kolom + " >= NOW() - INTERVAL '30 days' ";
	}

	private static boolean monitorDiskonBulanan(String periode) {
		return "year".equals(periode) || "semester".equals(periode) || "3years".equals(periode);
	}

	public static void monitorPromoCashback(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		// Default "last30" (BUKAN "month") -- lihat javadoc monitorDiskonTimeCond utk alasan
		// kompatibilitas mundur dgn pemanggil lama yg tak pernah mengirim parameter ini.
		String periode = request.optString("periode", "last30").trim();
		if (periode.isEmpty()) periode = "last30";
		Long jenisAnggotaId = request.isNull("jenis_anggota_id") ? null
				: Long.valueOf((request.get("jenis_anggota_id") + "").trim());
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			String kondisiTokoA = tokoId == null ? "" : " AND a.toko = ?";
			// jenisAnggotaId sudah divalidasi Long (bukan string mentah dari pengguna) -- aman
			// disisipkan sbg literal langsung, menghindari perlu menata ulang urutan bind-param `?`
			// yg sudah ada di tiap query di bawah (pola sama dgn MonitorDiskonKantinAction ZK yg
			// juga menyisipkan jenisId sbg literal, bukan bind param).
			String joinJenisPak = jenisAnggotaId == null ? ""
					: " INNER JOIN koperasi.anggota_koperasi jc ON a.anggota_koperasi = jc.id ";
			String condJenisPak = jenisAnggotaId == null ? "" : " AND jc.jenis_anggota_koperasi = " + jenisAnggotaId;
			String joinJenisD = jenisAnggotaId == null ? ""
					: " INNER JOIN koperasi.anggota_koperasi jc ON d.anggota_koperasi = jc.id ";
			String condJenisD = jenisAnggotaId == null ? "" : " AND jc.jenis_anggota_koperasi = " + jenisAnggotaId;
			String joinJenisDet = jenisAnggotaId == null ? ""
					: " INNER JOIN koperasi.pembelian_anggota_koperasi jpak ON det.pembelian_anggota_koperasi = jpak.id "
							+ " INNER JOIN koperasi.anggota_koperasi jc ON jpak.anggota_koperasi = jc.id ";
			String condJenisDet = jenisAnggotaId == null ? "" : " AND jc.jenis_anggota_koperasi = " + jenisAnggotaId;

			double diskonDiberikan = 0, cashbackDiberikan = 0;
			java.sql.PreparedStatement psKpi1 = conn.prepareStatement(
					"SELECT COALESCE(SUM(a.total_diskon),0), COALESCE(SUM(a.totalcashback),0) FROM koperasi.pembelian_anggota_koperasi a "
							+ joinJenisPak + "WHERE 1=1" + kondisiTokoA
							+ monitorDiskonTimeCond(periode, "a.tanggal_pembayaran") + condJenisPak);
			if (tokoId != null) psKpi1.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsKpi1 = psKpi1.executeQuery();
			if (rsKpi1.next()) { diskonDiberikan = rsKpi1.getDouble(1); cashbackDiberikan = rsKpi1.getDouble(2); }
			rsKpi1.close(); psKpi1.close();

			double cashbackDicairkan = 0;
			java.sql.PreparedStatement psKpi2 = conn.prepareStatement(
					"SELECT COALESCE(SUM(d.nominal_cair),0) FROM koperasi.pencairan_diskon d " + joinJenisD
							+ "WHERE d.status IN ('BERHASIL','PENDING')" + (tokoId == null ? "" : " AND d.toko = ?")
							+ monitorDiskonTimeCond(periode, "d.waktu_pencairan") + condJenisD);
			if (tokoId != null) psKpi2.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsKpi2 = psKpi2.executeQuery();
			if (rsKpi2.next()) cashbackDicairkan = rsKpi2.getDouble(1);
			rsKpi2.close(); psKpi2.close();

			// Saldo mengendap SENGAJA ALL-TIME (tanpa timeCond) -- sama semantik ZK: akumulasi saldo
			// cashback yg belum dicairkan sejak awal, bukan terbatas periode filter yg sedang dipilih.
			double cashbackAllTime = 0, cairAllTime = 0;
			java.sql.PreparedStatement psSaldo1 = conn.prepareStatement(
					"SELECT COALESCE(SUM(a.totalcashback),0) FROM koperasi.pembelian_anggota_koperasi a " + joinJenisPak
							+ "WHERE 1=1" + kondisiTokoA + condJenisPak);
			if (tokoId != null) psSaldo1.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsSaldo1 = psSaldo1.executeQuery();
			if (rsSaldo1.next()) cashbackAllTime = rsSaldo1.getDouble(1);
			rsSaldo1.close(); psSaldo1.close();

			java.sql.PreparedStatement psSaldo2 = conn.prepareStatement(
					"SELECT COALESCE(SUM(d.nominal_cair),0) FROM koperasi.pencairan_diskon d " + joinJenisD
							+ "WHERE d.status IN ('BERHASIL','PENDING')" + (tokoId == null ? "" : " AND d.toko = ?")
							+ condJenisD);
			if (tokoId != null) psSaldo2.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsSaldo2 = psSaldo2.executeQuery();
			if (rsSaldo2.next()) cairAllTime = rsSaldo2.getDouble(1);
			rsSaldo2.close(); psSaldo2.close();
			double saldoMengendap = cashbackAllTime - cairAllTime;

			// ---- Tren diskon vs cashback (BARU -- gap-closure paritas Monitor Diskon Flutter) ----
			String fmtTren = monitorDiskonBulanan(periode) ? "YYYY-MM" : "YYYY-MM-DD";
			JSONArray tren = new JSONArray();
			java.sql.PreparedStatement psTren = conn.prepareStatement(
					"SELECT TO_CHAR(a.tanggal_pembayaran,'" + fmtTren + "') t, COALESCE(SUM(a.total_diskon),0), "
							+ "COALESCE(SUM(a.totalcashback),0) FROM koperasi.pembelian_anggota_koperasi a " + joinJenisPak
							+ "WHERE (a.total_diskon > 0 OR a.totalcashback > 0)" + kondisiTokoA
							+ monitorDiskonTimeCond(periode, "a.tanggal_pembayaran") + condJenisPak
							+ " GROUP BY TO_CHAR(a.tanggal_pembayaran,'" + fmtTren + "') ORDER BY t ASC");
			if (tokoId != null) psTren.setLong(1, tokoId.longValue());
			java.sql.ResultSet rsTren = psTren.executeQuery();
			while (rsTren.next()) {
				JSONObject o = new JSONObject();
				o.put("periode", rsTren.getString(1));
				o.put("diskon", rsTren.getDouble(2));
				o.put("cashback", rsTren.getDouble(3));
				tren.put(o);
			}
			rsTren.close(); psTren.close();

			String kondisiTokoDet = tokoId == null ? "" : " AND det.toko = ?";
			JSONArray topProduk = new JSONArray();
			java.sql.PreparedStatement psTopProduk = conn.prepareStatement(
					"SELECT COALESCE(pr.nama,'-') nm, COALESCE(SUM(det.diskon)+SUM(det.cashback),0) nilai "
							+ "FROM koperasi.pembelian det LEFT JOIN koperasi.produk pr ON pr.id = det.produk " + joinJenisDet
							+ "WHERE (det.diskon > 0 OR det.cashback > 0)" + kondisiTokoDet
							+ monitorDiskonTimeCond(periode, "det.waktu") + condJenisDet
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
							+ "WHERE (a.total_diskon > 0 OR a.totalcashback > 0)" + kondisiTokoA
							+ monitorDiskonTimeCond(periode, "a.tanggal_pembayaran")
							+ (jenisAnggotaId == null ? "" : " AND c.jenis_anggota_koperasi = " + jenisAnggotaId)
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
							+ "FROM koperasi.pembelian det INNER JOIN koperasi.aturan_diskon ad ON det.aturan_diskon = ad.id " + joinJenisDet
							+ "WHERE 1=1" + kondisiTokoDet + monitorDiskonTimeCond(periode, "det.waktu") + condJenisDet
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
			hasil.put("periode", periode);
			hasil.put("diskonDiberikan", diskonDiberikan);
			hasil.put("cashbackDiberikan", cashbackDiberikan);
			hasil.put("cashbackDicairkan", cashbackDicairkan);
			hasil.put("saldoMengendap", saldoMengendap);
			hasil.put("tren", tren);
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
					"SELECT COALESCE(t.nama,'-') toko, s.kasir_nama, s.waktubuka, "
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
					"SELECT COALESCE(t.nama,'-') toko, s.kasir_nama, s.waktututup, s.selisih "
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
		// Gap-closure "Survey Kepuasan Pelanggan" -- "tipe" opsional dari klien membedakan siaran
		// keranjang biasa ("keranjang", default/tak dikirim) dari siaran "transaksi baru saja SUKSES,
		// tampilkan rating bintang" ("sukses") -- dipakai Layar Pelanggan Flutter (arsitektur polling
		// 2-perangkat, BEDA dgn Layar Pelanggan Electron yang push langsung antar-window lewat IPC
		// lokal tanpa lewat server sama sekali) utk tahu kapan harus pindah dari layar keranjang ke
		// layar ucapan terima kasih + rating, tanpa perlu endpoint terpisah.
		state.put("tipe", request.optString("tipe", "keranjang"));
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
			hasil.put("tipe", state.optString("tipe", "keranjang"));
		}
	}

	/**
	 * Gap-closure "Survey Kepuasan Pelanggan" (rating 1-5 lewat Layar Pelanggan setelah transaksi
	 * selesai) -- lihat JavaDoc entity {@link ais.database.model.inventory.SurveyKepuasanPos}.
	 * Diajukan oleh perangkat Layar Pelanggan (BUKAN kasir) memakai sesi Tbmuser kasir yang sama
	 * (sama batas kepercayaan dgn {@link #layarPelangganKirim}/{@link #verifikasiPin}) -- tidak ada
	 * gerbang CRUD tambahan selain autentikasi token yang sudah dilakukan di lapisan servlet.
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin, diabaikan utk pedagang/kasir),
	 *                {@code rating} (wajib, 1-5), {@code catatan} (opsional).
	 */
	public static void surveyKepuasanSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		int rating = request.optInt("rating", 0);
		if (rating < 1 || rating > 5) {
			hasil.put("status", "91");
			hasil.put("description", "Rating wajib antara 1-5.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.inventory.SurveyKepuasanPos s = new ais.database.model.inventory.SurveyKepuasanPos();
			s.setToko((Toko) session.get(Toko.class, tokoId));
			s.setRating(Integer.valueOf(rating));
			String catatan = request.optString("catatan", "").trim();
			s.setCatatan(catatan.isEmpty() ? null : catatan);
			session.beginTransaction();
			session.save(s);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** Simpan satu baris buku besar batch setelah saldo batch berubah. */
	private static void catatMutasiBatch(Session session, ProdukBatch batch, String jenis, double masuk,
			double keluar, String referensi, String keterangan, String oleh) {
		MutasiProdukBatch mutasi = new MutasiProdukBatch();
		mutasi.setBatch(batch);
		mutasi.setWaktu(new Date());
		mutasi.setJenis(jenis);
		mutasi.setMasuk(Double.valueOf(masuk));
		mutasi.setKeluar(Double.valueOf(keluar));
		mutasi.setSaldo(batch.getStok());
		mutasi.setReferensi(referensi);
		mutasi.setKeterangan(keterangan);
		mutasi.setOleh(oleh);
		session.save(mutasi);
	}

	private static Date parseTanggalBatch(String nilai) throws Exception {
		String teks = nilai == null ? "" : nilai.trim();
		if (teks.length() >= 10) teks = teks.substring(0, 10);
		return new SimpleDateFormat("yyyy-MM-dd").parse(teks);
	}

	private static Date awalHariIni() {
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.set(java.util.Calendar.HOUR_OF_DAY, 0);
		c.set(java.util.Calendar.MINUTE, 0);
		c.set(java.util.Calendar.SECOND, 0);
		c.set(java.util.Calendar.MILLISECOND, 0);
		return c.getTime();
	}

	/**
	 * Tambahkan stok penerimaan ke lot yang sama (produk+toko+nomor batch+tanggal kedaluwarsa).
	 * Data batch bersifat opsional agar payload Kulakan versi lama tetap 100% kompatibel.
	 */
	private static ProdukBatch tambahPenerimaanBatch(Session session, Produk produk, JSONObject sumber,
			double qty, double hargaModal, String referensi, String oleh) throws Exception {
		String nomor = sumber.optString("nomor_batch", "").trim();
		String tanggalText = sumber.optString("tanggal_expired", "").trim();
		if (nomor.isEmpty() && tanggalText.isEmpty()) {
			return null;
		}
		if (nomor.isEmpty() || tanggalText.isEmpty()) {
			throw new IllegalArgumentException("Nomor batch dan tanggal kedaluwarsa wajib diisi bersamaan.");
		}
		Date tanggalExpired = parseTanggalBatch(tanggalText);
		Date tanggalProduksi = sumber.optString("tanggal_produksi", "").trim().isEmpty() ? null
				: parseTanggalBatch(sumber.getString("tanggal_produksi"));
		@SuppressWarnings("unchecked")
		List<ProdukBatch> cocok = session.createCriteria(ProdukBatch.class)
				.add(Restrictions.eq("produk.id", produk.getId()))
				.add(Restrictions.eq("toko.id", produk.getToko().getId()))
				.add(Restrictions.eq("nomorBatch", nomor))
				.add(Restrictions.eq("tanggalExpired", tanggalExpired)).setMaxResults(1).list();
		ProdukBatch batch;
		if (cocok.isEmpty()) {
			batch = new ProdukBatch();
			batch.setProduk(produk);
			batch.setToko(produk.getToko());
			batch.setNomorBatch(nomor);
			batch.setTanggalExpired(tanggalExpired);
			batch.setTanggalProduksi(tanggalProduksi);
			batch.setStok(Double.valueOf(qty));
			batch.setHargaModal(Double.valueOf(hargaModal));
			batch.setStatus(ProdukBatch.STATUS_AKTIF);
			batch.setOleh(oleh);
			session.save(batch);
		} else {
			batch = cocok.get(0);
			batch.setStok(Double.valueOf(batch.getStok().doubleValue() + qty));
			batch.setHargaModal(Double.valueOf(hargaModal));
			if (tanggalProduksi != null) batch.setTanggalProduksi(tanggalProduksi);
			batch.setStatus(ProdukBatch.STATUS_AKTIF);
			session.saveOrUpdate(batch);
		}
		session.flush();
		catatMutasiBatch(session, batch, "KULAKAN", qty, 0.0, referensi,
				"Penerimaan barang dari Kulakan", oleh);
		return batch;
	}

	/** Daftar dan ringkasan batch kedaluwarsa per toko, termasuk data produk legacy. */
	public static void kedaluwarsaList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		String filter = request.optString("filter", "90_HARI").trim().toUpperCase();
		String keyword = request.optString("keyword", "").trim();
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(1, request.optInt("page_size", 30)));
		int offset = (page - 1) * pageSize;
		String kondisi;
		if ("KADALUWARSA".equals(filter)) kondisi = "tanggal_expired < current_date";
		else if ("7_HARI".equals(filter)) kondisi = "tanggal_expired between current_date and current_date + 7";
		else if ("30_HARI".equals(filter)) kondisi = "tanggal_expired between current_date and current_date + 30";
		else if ("90_HARI".equals(filter)) kondisi = "tanggal_expired between current_date and current_date + 90";
		else if ("TANPA_TANGGAL".equals(filter)) kondisi = "tanggal_expired is null";
		else if ("KARANTINA".equals(filter)) kondisi = "status = 'KARANTINA'";
		else kondisi = "1=1";

		String cte = "WITH data AS ("
				+ "SELECT pb.id batch_id,p.id produk_id,p.kode,p.nama,pb.nomor_batch,pb.tanggal_produksi,pb.tanggal_expired,pb.stok,pb.status,pb.keterangan,false legacy "
				+ "FROM koperasi.produk_batch pb JOIN koperasi.produk p ON p.id=pb.produk WHERE pb.toko=? "
				+ "UNION ALL SELECT NULL,p.id,p.kode,p.nama,COALESCE(p.batch,''),NULL,p.tanggal_expired,COALESCE(p.stok,0),'AKTIF','',true "
				+ "FROM koperasi.produk p WHERE p.toko=? AND COALESCE(p.aktif,true)=true AND NOT EXISTS "
				+ "(SELECT 1 FROM koperasi.produk_batch pb WHERE pb.produk=p.id)) ";
		String andKw = keyword.isEmpty() ? "" : " AND (nama ILIKE ? OR COALESCE(kode,'') ILIKE ? OR COALESCE(nomor_batch,'') ILIKE ?)";
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			java.sql.PreparedStatement ringkas = conn.prepareStatement(cte
					+ "SELECT COUNT(*),COALESCE(SUM(CASE WHEN tanggal_expired<current_date AND stok>0 THEN 1 ELSE 0 END),0),"
					+ "COALESCE(SUM(CASE WHEN tanggal_expired between current_date and current_date+30 AND stok>0 THEN 1 ELSE 0 END),0),"
					+ "COALESCE(SUM(CASE WHEN tanggal_expired between current_date and current_date+90 AND stok>0 THEN 1 ELSE 0 END),0),"
					+ "COALESCE(SUM(CASE WHEN tanggal_expired IS NULL AND stok>0 THEN 1 ELSE 0 END),0),"
					+ "COALESCE(SUM(CASE WHEN status='KARANTINA' AND stok>0 THEN stok ELSE 0 END),0) FROM data");
			ringkas.setLong(1, tokoId); ringkas.setLong(2, tokoId);
			java.sql.ResultSet rr = ringkas.executeQuery();
			JSONObject summary = new JSONObject();
			if (rr.next()) {
				summary.put("total", rr.getLong(1));
				summary.put("kedaluwarsa", rr.getLong(2));
				summary.put("dalam30Hari", rr.getLong(3));
				summary.put("dalam90Hari", rr.getLong(4));
				summary.put("tanpaTanggal", rr.getLong(5));
				summary.put("stokKarantina", rr.getDouble(6));
			}
			rr.close(); ringkas.close();

			java.sql.PreparedStatement count = conn.prepareStatement(cte + "SELECT COUNT(*) FROM data WHERE " + kondisi + andKw);
			int ci = 1; count.setLong(ci++, tokoId); count.setLong(ci++, tokoId);
			if (!keyword.isEmpty()) { String kw="%"+keyword+"%"; count.setString(ci++,kw); count.setString(ci++,kw); count.setString(ci++,kw); }
			java.sql.ResultSet cr=count.executeQuery(); long total=cr.next()?cr.getLong(1):0; cr.close(); count.close();

			java.sql.PreparedStatement ps = conn.prepareStatement(cte
					+ "SELECT batch_id,produk_id,kode,nama,nomor_batch,tanggal_produksi,tanggal_expired,stok,status,keterangan,legacy,"
					+ "CASE WHEN tanggal_expired IS NULL THEN NULL ELSE tanggal_expired-current_date END sisa_hari "
					+ "FROM data WHERE " + kondisi + andKw + " ORDER BY tanggal_expired NULLS LAST,nama LIMIT ? OFFSET ?");
			int pi=1; ps.setLong(pi++,tokoId); ps.setLong(pi++,tokoId);
			if(!keyword.isEmpty()){String kw="%"+keyword+"%";ps.setString(pi++,kw);ps.setString(pi++,kw);ps.setString(pi++,kw);}
			ps.setInt(pi++,pageSize); ps.setInt(pi++,offset);
			java.sql.ResultSet rs=ps.executeQuery(); JSONArray data=new JSONArray();
			while(rs.next()){
				JSONObject j=new JSONObject(); Object bid=rs.getObject(1); j.put("batchId",bid==null?JSONObject.NULL:rs.getLong(1));
				j.put("produkId",rs.getLong(2)); j.put("kode",rs.getString(3)); j.put("nama",rs.getString(4));
				j.put("nomorBatch",rs.getString(5));
				java.sql.Date tp=rs.getDate(6),te=rs.getDate(7);
				j.put("tanggalProduksi",tp==null?JSONObject.NULL:tp.toString()); j.put("tanggalExpired",te==null?JSONObject.NULL:te.toString());
				j.put("stok",rs.getDouble(8)); j.put("status",rs.getString(9)); j.put("keterangan",rs.getString(10));
				j.put("legacy",rs.getBoolean(11)); Object sh=rs.getObject(12); j.put("sisaHari",sh==null?JSONObject.NULL:rs.getInt(12)); data.put(j);
			}
			rs.close(); ps.close();
			hasil.put("status","00"); hasil.put("data",data); hasil.put("summary",summary); hasil.put("total",total);
			hasil.put("page",page); hasil.put("pageSize",pageSize);
		} finally { tutupSessionPolaB(session); }
	}

	/** Daftar ringkas produk toko untuk picker "Tambah Batch" di JSP/Android. */
	public static void produkBatchProdukList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91"); hasil.put("description", "Toko tidak diketahui."); return;
		}
		String keyword = request.optString("keyword", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String sql = "SELECT id,COALESCE(kode,''),nama,COALESCE(stok,0) FROM koperasi.produk "
					+ "WHERE toko=:toko AND COALESCE(aktif,true)=true ";
			if (!keyword.isEmpty()) sql += "AND (nama ILIKE :kw OR COALESCE(kode,'') ILIKE :kw OR COALESCE(barcode,'') ILIKE :kw) ";
			sql += "ORDER BY nama LIMIT 100";
			SQLQuery q = session.createSQLQuery(sql);
			q.setParameter("toko", tokoId);
			if (!keyword.isEmpty()) q.setParameter("kw", "%" + keyword + "%");
			JSONArray data = new JSONArray();
			for (Object o : q.list()) {
				Object[] a = (Object[]) o; JSONObject j = new JSONObject();
				j.put("id", a[0]); j.put("kode", a[1]); j.put("nama", a[2]);
				j.put("stok", a[3] == null ? 0 : ((Number) a[3]).doubleValue()); data.put(j);
			}
			hasil.put("status", "00"); hasil.put("data", data);
		} finally { tutupSessionPolaB(session); }
	}

	/** Buat/ubah batch dan catat selisih stok fisik sebagai mutasi OPNAME_BATCH. */
	public static void produkBatchSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pedagang=tbmuser==null?null:tbmuser.getPedagang();
		boolean admin=pedagang==null, supervisor=pedagang!=null&&Boolean.TRUE.equals(pedagang.getSupervisor());
		if(!bolehAksiCrud(tbmuser,pedagang,admin,supervisor,"stokopname","create")){
			hasil.put("status","91"); hasil.put("description","Anda tidak berhak mengubah data batch."); return;
		}
		Long tokoId=soResolveTokoId(tbmuser,request); Long batchId=request.isNull("batch_id")?null:Long.valueOf(request.get("batch_id")+"");
		Long produkId=request.isNull("produk_id")?null:Long.valueOf(request.get("produk_id")+"");
		String nomor=request.optString("nomor_batch","").trim(), tanggal=request.optString("tanggal_expired","").trim();
		if(produkId==null||nomor.isEmpty()||tanggal.isEmpty()){hasil.put("status","91");hasil.put("description","Produk, nomor batch, dan tanggal kedaluwarsa wajib diisi.");return;}
		String status=request.optString("status",ProdukBatch.STATUS_AKTIF).trim().toUpperCase();
		if(!ProdukBatch.STATUS_AKTIF.equals(status)&&!ProdukBatch.STATUS_KARANTINA.equals(status)&&!ProdukBatch.STATUS_DIMUSNAHKAN.equals(status)){
			hasil.put("status","91");hasil.put("description","Status batch tidak valid.");return;
		}
		Session session=HibernateUtil.getSessionFactory().openSession();
		try{
			Produk produk=(Produk)session.get(Produk.class,produkId);
			if(produk==null||produk.getToko()==null||!produk.getToko().getId().equals(tokoId)){hasil.put("status","91");hasil.put("description","Produk bukan milik toko aktif.");return;}
			session.beginTransaction(); ProdukBatch b=batchId==null?new ProdukBatch():(ProdukBatch)session.get(ProdukBatch.class,batchId);
			if(b==null){throw new IllegalArgumentException("Batch tidak ditemukan.");}
			if(b.getId()!=null&&!b.getToko().getId().equals(tokoId)){throw new IllegalArgumentException("Batch bukan milik toko aktif.");}
			double lama=b.getId()==null?0:b.getStok().doubleValue(); double baru=request.has("stok_fisik")?request.getDouble("stok_fisik"):lama;
			if(baru<0)throw new IllegalArgumentException("Stok batch tidak boleh negatif.");
			b.setProduk(produk);b.setToko(produk.getToko());b.setNomorBatch(nomor);b.setTanggalExpired(parseTanggalBatch(tanggal));
			String produksi=request.optString("tanggal_produksi","").trim();b.setTanggalProduksi(produksi.isEmpty()?null:parseTanggalBatch(produksi));
			b.setStok(Double.valueOf(baru));b.setStatus(status);b.setKeterangan(request.optString("keterangan",""));b.setOleh(tbmuser==null?"batch":tbmuser.getUserId());
			session.saveOrUpdate(b);session.flush(); double selisih=baru-lama;
			if(selisih!=0)catatMutasiBatch(session,b,"OPNAME_BATCH",Math.max(0,selisih),Math.max(0,-selisih),"BATCH-"+b.getId(),request.optString("keterangan",""),tbmuser==null?"batch":tbmuser.getUserId());
			session.getTransaction().commit();hasil.put("status","00");hasil.put("batchId",b.getId());hasil.put("selisih",selisih);
		}catch(Exception e){try{if(session.getTransaction()!=null&&session.getTransaction().isActive())session.getTransaction().rollback();}catch(Exception ignored){}hasil.put("status","91");hasil.put("description",Common.tampilErrorJikaAdmin(e));}
		finally{tutupSessionPolaB(session);}
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
			session.flush();
			tambahPenerimaanBatch(session, produk, request, qty, hargaBeliSatuan,
					"KULAKAN-" + pg.getId(), tbmuser == null ? "kulakan" : tbmuser.getUserId());
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
	 * <h3>Kulakan per-Faktur (gap-closure permintaan user 2026-08-11) -- catat SATU faktur/nota
	 * supplier (nomor faktur/tanggal/supplier diisi SEKALI) diikuti banyak baris produk sekaligus,
	 * satu transaksi.</h3>
	 *
	 * <p>Pola items[] SAMA PERSIS {@link #returPenjualanSimpan} -- header dibangun sekali
	 * ({@link PengadaanFaktur}), lalu satu baris {@link PengadaanProduk} per item dgn FK
	 * {@code fakturPengadaan} menunjuk header yg sama. {@code kulakanSimpan}/{@code kulakanList} lama
	 * (satu baris per panggilan, TANPA header) TETAP ADA apa adanya -- aksi ini murni ADDITIF, jalur
	 * lama tidak diubah/dihapus, data lama (fakturPengadaan=null) tetap valid selamanya.</p>
	 *
	 * <p><b>Diskon/potongan faktur</b>: {@code total_faktur_manual} opsional -- diisi HANYA bila
	 * nilai nota fisik LEBIH KECIL dari jumlah hitungan baris (qty*harga tiap item dijumlah); server
	 * (BUKAN klien) menghitung {@code diskon = totalHitung - totalFakturManual} (clamp ke 0 minimal,
	 * tak pernah negatif -- bila manual justru LEBIH BESAR dari hitungan, diskon dianggap 0, total
	 * final tetap dari hitungan baris, kelebihan pengisian diabaikan bukan dianggap "diskon negatif").</p>
	 *
	 * @param request payload: {@code toko_id} (toko asal -- wajib utk admin, dikunci pedagang),
	 *                {@code nomor_faktur} (wajib), {@code tanggal_faktur} (opsional, ISO -- default
	 *                sekarang), {@code supplier_id} (opsional, id {@code library.Penyedia}),
	 *                {@code total_faktur_manual} (opsional), {@code keterangan} (opsional),
	 *                {@code items} (wajib, array {@code {produk_id, qty, harga_beli_satuan,
	 *                keterangan}}, minimal satu baris).
	 * @param hasil   diisi {@code status="00"}, {@code fakturId}, {@code totalHitung}, {@code diskon},
	 *                {@code totalFakturFinal}, {@code jumlahItem}.
	 */
	public static void kulakanFakturSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilKf = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalKf = pemanggilKf == null;
		boolean supervisorKf = pemanggilKf != null && Boolean.TRUE.equals(pemanggilKf.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggilKf, adminGlobalKf, supervisorKf, "kulakan", "create")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mencatat Kulakan (Harga Beli).");
			return;
		}
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		String nomorFaktur = request.optString("nomor_faktur", "").trim();
		if (nomorFaktur.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Nomor Faktur wajib diisi.");
			return;
		}
		JSONArray items = request.optJSONArray("items");
		if (items == null || items.length() == 0) {
			hasil.put("status", "91");
			hasil.put("description", "Belum ada barang yang dimasukkan untuk faktur ini.");
			return;
		}
		Long supplierId = request.isNull("supplier_id") ? null : Long.valueOf((request.get("supplier_id") + "").trim());
		Double totalManual = request.isNull("total_faktur_manual") ? null : request.getDouble("total_faktur_manual");

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			if (toko == null) {
				hasil.put("status", "91");
				hasil.put("description", "Toko tidak ditemukan.");
				return;
			}
			ais.database.model.library.Penyedia supplier = supplierId == null ? null
					: (ais.database.model.library.Penyedia) session.get(ais.database.model.library.Penyedia.class, supplierId);

			double totalHitung = 0;
			for (int i = 0; i < items.length(); i++) {
				JSONObject it = items.getJSONObject(i);
				totalHitung += it.optDouble("qty", 0) * it.optDouble("harga_beli_satuan", 0);
			}
			double totalFinal = totalManual == null ? totalHitung : totalManual;
			double diskon = totalManual == null ? 0 : Math.max(0, totalHitung - totalManual);

			session.beginTransaction();
			PengadaanFaktur header = new PengadaanFaktur();
			header.setToko(toko);
			header.setSupplier(supplier);
			header.setNomorFaktur(nomorFaktur);
			header.setTanggalFaktur(request.isNull("tanggal_faktur") ? new Date()
					: Common.dateFormatInput.get().parse(request.getString("tanggal_faktur")));
			header.setTotalFakturManual(totalManual);
			header.setTotalHitungSaatSimpan(totalHitung);
			header.setDiskon(diskon);
			header.setKeterangan(request.optString("keterangan", ""));
			header.setOleh(tbmuser == null ? "kulakan_faktur" : tbmuser.getUserId());
			header.setWaktu(new Date());
			session.save(header);

			String oleh = tbmuser == null ? "kulakan_faktur" : tbmuser.getUserId();
			for (int i = 0; i < items.length(); i++) {
				JSONObject it = items.getJSONObject(i);
				Long produkId = it.isNull("produk_id") ? null : Long.valueOf((it.get("produk_id") + "").trim());
				if (produkId == null) {
					throw new IllegalArgumentException("Produk baris ke-" + (i + 1) + " belum dipilih.");
				}
				double qty = it.optDouble("qty", 0);
				if (qty <= 0) {
					throw new IllegalArgumentException("Jumlah baris ke-" + (i + 1) + " harus lebih dari 0.");
				}
				double hargaBeliSatuan = it.optDouble("harga_beli_satuan", 0);
				if (hargaBeliSatuan <= 0) {
					throw new IllegalArgumentException("Harga beli baris ke-" + (i + 1) + " harus lebih dari 0.");
				}
				Produk produk = (Produk) session.get(Produk.class, produkId);
				if (produk == null) {
					throw new IllegalArgumentException("Produk baris ke-" + (i + 1) + " tidak ditemukan.");
				}
				if (!adminGlobalKf) {
					Long tokoProduk = produk.getToko() == null ? null : produk.getToko().getId();
					if (!tokoId.equals(tokoProduk)) {
						throw new IllegalArgumentException("Produk baris ke-" + (i + 1) + " bukan milik toko Anda.");
					}
				}

				PengadaanProduk pg = new PengadaanProduk();
				pg.setProduk(produk);
				pg.setToko(produk.getToko());
				pg.setFakturPengadaan(header);
				pg.setNomorFaktur(nomorFaktur);
				pg.setNamaSupplier(supplier == null ? "" : supplier.getNama());
				pg.setQty(qty);
				pg.setHargaBeliSatuan(hargaBeliSatuan);
				pg.setTotalHarga(qty * hargaBeliSatuan);
				pg.setWaktuPengadaan(header.getTanggalFaktur());
				pg.setKeterangan(it.optString("keterangan", ""));
				pg.setOleh(oleh);
				session.save(pg);
				session.flush();
				tambahPenerimaanBatch(session, produk, it, qty, hargaBeliSatuan,
						"FAKTUR-" + header.getId() + "-PENGADAAN-" + pg.getId(), oleh);
				ais.action.master.inventory.StokKantinUtil.recomputeStokProdukNative(session, produkId);
			}
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("fakturId", header.getId());
			hasil.put("totalHitung", totalHitung);
			hasil.put("diskon", diskon);
			hasil.put("totalFakturFinal", totalFinal);
			hasil.put("jumlahItem", items.length());
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:kulakanFakturSimpan-rollback");
			}
			hasil.put("status", "91");
			hasil.put("description", Common.tampilErrorJikaAdmin(e));
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Kulakan per-Faktur -- riwayat header (paginated, per toko).</h3> Satu baris hasil = satu
	 * faktur (bukan satu produk) -- {@code jumlahItem}/{@code totalHitung} dihitung agregat dari
	 * {@link PengadaanProduk} yg menunjuk header ybs.
	 */
	public static void kulakanFakturList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
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
					: " AND (COALESCE(f.nomor_faktur,'') ILIKE ? OR COALESCE(s.nama,'') ILIKE ?)";
			java.sql.Connection conn = session.connection();

			java.sql.PreparedStatement psCount = conn.prepareStatement(
					"SELECT COUNT(*) FROM koperasi.pengadaan_faktur f LEFT JOIN library.penyedia s ON f.supplier = s.id "
							+ "WHERE f.toko = ?" + andKw);
			int idxC = 1;
			psCount.setLong(idxC++, tokoId);
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
				psCount.setString(idxC++, kw);
				psCount.setString(idxC++, kw);
			}
			java.sql.ResultSet rsCount = psCount.executeQuery();
			long total = rsCount.next() ? rsCount.getLong(1) : 0;
			rsCount.close();
			psCount.close();

			java.sql.PreparedStatement ps = conn.prepareStatement(
					"SELECT f.id, f.tanggal_faktur, COALESCE(f.nomor_faktur,''), COALESCE(s.nama,''), "
							+ "f.total_faktur_manual, f.total_hitung_saat_simpan, f.diskon, COALESCE(f.keterangan,''), "
							+ "(SELECT COUNT(*) FROM koperasi.pengadaan_produk pg WHERE pg.faktur_pengadaan = f.id) AS jumlah_item "
							+ "FROM koperasi.pengadaan_faktur f LEFT JOIN library.penyedia s ON f.supplier = s.id "
							+ "WHERE f.toko = ?" + andKw + " ORDER BY f.tanggal_faktur DESC, f.id DESC LIMIT ? OFFSET ?");
			int idx = 1;
			ps.setLong(idx++, tokoId);
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
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
				j.put("fakturId", rs.getLong(1));
				java.sql.Timestamp tgl = rs.getTimestamp(2);
				j.put("tanggalFaktur", tgl == null ? "" : fmt.format(tgl));
				j.put("nomorFaktur", rs.getString(3));
				j.put("namaSupplier", rs.getString(4));
				double totalManual = rs.getDouble(5);
				j.put("totalFakturManual", rs.wasNull() ? JSONObject.NULL : totalManual);
				j.put("totalHitung", rs.getDouble(6));
				j.put("diskon", rs.getDouble(7));
				j.put("keterangan", rs.getString(8));
				j.put("jumlahItem", rs.getInt(9));
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

	/** <h3>Kulakan per-Faktur -- detail SATU header (dipakai layar detail/tap-riwayat).</h3> */
	public static void kulakanFakturDetail(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long fakturId = request.isNull("faktur_id") ? null : Long.valueOf((request.get("faktur_id") + "").trim());
		if (fakturId == null) {
			hasil.put("status", "91");
			hasil.put("description", "ID faktur wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PengadaanFaktur f = (PengadaanFaktur) session.get(PengadaanFaktur.class, fakturId);
			if (f == null) {
				hasil.put("status", "91");
				hasil.put("description", "Faktur tidak ditemukan.");
				return;
			}
			JSONObject header = new JSONObject();
			header.put("fakturId", f.getId());
			header.put("nomorFaktur", f.getNomorFaktur());
			header.put("tanggalFaktur", Common.dateFormatInput.get().format(f.getTanggalFaktur()));
			header.put("namaSupplier", f.getSupplier() == null ? "" : f.getSupplier().getNama());
			header.put("totalFakturManual", f.getTotalFakturManual() == null ? JSONObject.NULL : f.getTotalFakturManual());
			header.put("totalHitung", f.getTotalHitungSaatSimpan());
			header.put("diskon", f.getDiskon());
			header.put("totalFakturFinal", f.getTotalFakturFinal());
			header.put("keterangan", f.getKeterangan());

			@SuppressWarnings("unchecked")
			java.util.List<PengadaanProduk> items = session.createCriteria(PengadaanProduk.class)
					.add(org.hibernate.criterion.Restrictions.eq("fakturPengadaan", f)).list();
			JSONArray arr = new JSONArray();
			for (PengadaanProduk pg : items) {
				JSONObject j = new JSONObject();
				j.put("id", pg.getId());
				j.put("produkId", pg.getProduk().getId());
				j.put("namaProduk", pg.getProduk().getNama());
				j.put("qty", pg.getQty());
				j.put("hargaBeliSatuan", pg.getHargaBeliSatuan());
				j.put("totalHarga", pg.getTotalHarga());
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("header", header);
			hasil.put("items", arr);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Master Supplier (gap-closure "Data supplier belum ngelink ke master supplier") -- cari
	 * {@link ais.database.model.library.Penyedia} berdasarkan nama.</h3> Dipakai picker supplier di
	 * layar Kulakan per-Faktur (Desktop/Android). Boleh dipanggil siapa saja yg login (murni baca).
	 */
	public static void penyediaList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String keyword = request.optString("keyword", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(ais.database.model.library.Penyedia.class)
					.addOrder(org.hibernate.criterion.Order.asc("nama"));
			if (!keyword.isEmpty()) {
				c.add(org.hibernate.criterion.Restrictions.ilike("nama", keyword, org.hibernate.criterion.MatchMode.ANYWHERE));
			}
			c.setMaxResults(50);
			@SuppressWarnings("unchecked")
			java.util.List<ais.database.model.library.Penyedia> daftar = c.list();
			JSONArray arr = new JSONArray();
			for (ais.database.model.library.Penyedia p : daftar) {
				JSONObject j = new JSONObject();
				j.put("id", p.getId());
				j.put("nama", p.getNama());
				j.put("kontak", p.getKontak() == null ? "" : p.getKontak());
				j.put("telp", p.getTelp() == null ? "" : p.getTelp());
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Master Supplier -- tambah cepat {@link ais.database.model.library.Penyedia} baru dari
	 * layar Kulakan</h3> (kantin admin blm tentu punya akses ke modul Library tempat entity ini
	 * biasanya dikelola). Boleh dipanggil siapa saja yg boleh mencatat Kulakan (gerbang SAMA dgn
	 * {@link #kulakanFakturSimpan}) -- bukan admin-only, supaya alur "supplier baru belum terdaftar"
	 * tidak memblokir kasir/supervisor toko yg sedang mencatat faktur.
	 */
	public static void penyediaSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilPy = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalPy = pemanggilPy == null;
		boolean supervisorPy = pemanggilPy != null && Boolean.TRUE.equals(pemanggilPy.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggilPy, adminGlobalPy, supervisorPy, "kulakan", "create")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat menambah Supplier baru.");
			return;
		}
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Nama supplier wajib diisi.");
			return;
		}
		// Gap-closure layar "Supplier (Penyedia)" CRUD -- `id` hadir = ubah, kosong/tak ada = tambah
		// (perilaku LAMA dipertahankan apa adanya, ini murni tambahan opsional supaya picker cepat-
		// tambah dari Kulakan tetap jalan tanpa berubah, lihat JavaDoc method ini).
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.library.Penyedia p;
			if (id != null) {
				p = (ais.database.model.library.Penyedia) session.get(ais.database.model.library.Penyedia.class, id);
				if (p == null) {
					hasil.put("status", "91");
					hasil.put("description", "Supplier tidak ditemukan.");
					return;
				}
			} else {
				p = new ais.database.model.library.Penyedia();
			}
			p.setNama(nama);
			p.setKode(request.optString("kode", ""));
			p.setKontak(request.optString("kontak", ""));
			p.setTelp(request.optString("telp", ""));
			p.setFax(request.optString("fax", ""));
			p.setEmail(request.optString("email", ""));
			p.setAlamat(request.optString("alamat", ""));
			p.setKodePos(request.optString("kode_pos", ""));
			p.setKeterangan(request.optString("keterangan", ""));

			session.beginTransaction();
			session.saveOrUpdate(p);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", p.getId());
			hasil.put("nama", p.getNama());
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:penyediaSimpan-rollback");
			}
			hasil.put("status", "91");
			hasil.put("description", Common.tampilErrorJikaAdmin(e));
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Layar "Supplier (Penyedia)" CRUD -- daftar lengkap (semua kolom) + paginasi.</h3> Beda
	 * dari {@link #penyediaList} (hanya {@code id,nama,kontak,telp}, maks 50, utk picker cepat di
	 * Kulakan) -- di sini SEMUA kolom dikembalikan utk layar manajemen master data supplier.
	 */
	public static void penyediaListAdmin(JSONObject request, JSONObject hasil) throws Exception {
		String keyword = request.optString("keyword", "").trim();
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(1, request.optInt("page_size", 20)));

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria cCount = session.createCriteria(ais.database.model.library.Penyedia.class);
			org.hibernate.Criteria cData = session.createCriteria(ais.database.model.library.Penyedia.class)
					.addOrder(org.hibernate.criterion.Order.asc("nama"));
			if (!keyword.isEmpty()) {
				org.hibernate.criterion.Disjunction atau = org.hibernate.criterion.Restrictions.disjunction();
				atau.add(org.hibernate.criterion.Restrictions.ilike("nama", keyword, org.hibernate.criterion.MatchMode.ANYWHERE));
				atau.add(org.hibernate.criterion.Restrictions.ilike("kode", keyword, org.hibernate.criterion.MatchMode.ANYWHERE));
				cCount.add(atau);
				cData.add(org.hibernate.criterion.Restrictions.sqlRestriction(
						"(lower({alias}.nama) like lower(?) or lower({alias}.kode) like lower(?))",
						new Object[] { "%" + keyword + "%", "%" + keyword + "%" },
						new org.hibernate.type.Type[] { org.hibernate.type.StandardBasicTypes.STRING,
								org.hibernate.type.StandardBasicTypes.STRING }));
			}
			long total = (Long) cCount.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
			cData.setFirstResult((page - 1) * pageSize).setMaxResults(pageSize);
			@SuppressWarnings("unchecked")
			java.util.List<ais.database.model.library.Penyedia> daftar = cData.list();
			JSONArray arr = new JSONArray();
			for (ais.database.model.library.Penyedia p : daftar) {
				JSONObject j = new JSONObject();
				j.put("id", p.getId());
				j.put("kode", p.getKode() == null ? "" : p.getKode());
				j.put("nama", p.getNama());
				j.put("kontak", p.getKontak() == null ? "" : p.getKontak());
				j.put("telp", p.getTelp() == null ? "" : p.getTelp());
				j.put("fax", p.getFax() == null ? "" : p.getFax());
				j.put("email", p.getEmail() == null ? "" : p.getEmail());
				j.put("alamat", p.getAlamat() == null ? "" : p.getAlamat());
				j.put("kode_pos", p.getKodePos() == null ? "" : p.getKodePos());
				j.put("keterangan", p.getKeterangan() == null ? "" : p.getKeterangan());
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("page_size", pageSize);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** Hapus Supplier (Penyedia) -- gerbang SAMA dgn {@link #penyediaSimpan} (kunci CRUD "kulakan").
	 * Tak ada pre-check referensi terpisah -- kalau supplier ini masih dipakai di faktur Kulakan
	 * ({@link ais.database.model.inventory.PengadaanFaktur#getSupplier()}), constraint FK di DB
	 * menolak DELETE, ditangkap di catch (pola sama dgn {@link #caraBayarHapus}). */
	public static void penyediaHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilPyH = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalPyH = pemanggilPyH == null;
		boolean supervisorPyH = pemanggilPyH != null && Boolean.TRUE.equals(pemanggilPyH.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggilPyH, adminGlobalPyH, supervisorPyH, "kulakan", "delete")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat menghapus Supplier.");
			return;
		}
		Long id = request.isNull("id") ? null : Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "ID supplier wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.library.Penyedia p = (ais.database.model.library.Penyedia) session
					.get(ais.database.model.library.Penyedia.class, id);
			if (p == null) {
				hasil.put("status", "91");
				hasil.put("description", "Supplier tidak ditemukan.");
				return;
			}
			session.beginTransaction();
			session.delete(p);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} catch (Exception eHapusPy) {
			hasil.put("status", "91");
			hasil.put("description", "Gagal menghapus: supplier ini sudah dipakai di faktur Kulakan.");
			ais.common.ErrorAuditUtil.record(eHapusPy, "auto-audit penyediaHapus src/ais/action/servlet/api/KantinHelper.java");
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Retur Pembelian (gap-closure roadmap Fase 3, permintaan user 2026-08-11) -- catat satu atau
	 * lebih produk yang dikembalikan KE SUPPLIER, satu transaksi.</h3>
	 *
	 * <p>Pola items[] SAMA PERSIS {@link #returPenjualanSimpan}/{@link #kulakanFakturSimpan}. Gated
	 * Supervisor -- SAMA pola gerbangnya dgn {@link #returPenjualanSimpan} (tindakan yg mengoreksi
	 * stok, bukan dibiarkan bebas). Stok SELALU berkurang (tanpa flag kembalikan-ke-stok, beda dari
	 * Retur Penjualan -- lihat JavaDoc {@link ReturPembelian}).</p>
	 *
	 * @param request payload: {@code faktur_pengadaan_id} (opsional -- id header {@link
	 *                PengadaanFaktur} asal), {@code kode_faktur_asal} (opsional, utk tampilan bila
	 *                tak ada link faktur), {@code supplier_id} (opsional), {@code items} (wajib,
	 *                array {@code {produk_id, qty, harga_satuan, alasan, keterangan}}).
	 * @param hasil   diisi {@code status="00"}, {@code ids}, {@code totalNilaiRetur}.
	 */
	public static void returPembelianSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilRb = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalRb = pemanggilRb == null;
		boolean supervisorRb = pemanggilRb != null && Boolean.TRUE.equals(pemanggilRb.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggilRb, adminGlobalRb, supervisorRb, "returpembelian", "create")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mencatat Retur Pembelian.");
			return;
		}
		JSONArray items = request.optJSONArray("items");
		if (items == null || items.length() == 0) {
			hasil.put("status", "91");
			hasil.put("description", "Belum ada barang yang dipilih untuk diretur.");
			return;
		}
		Long fakturPengadaanId = request.isNull("faktur_pengadaan_id") ? null
				: Long.valueOf((request.get("faktur_pengadaan_id") + "").trim());
		String kodeFakturAsal = request.optString("kode_faktur_asal", "");
		Long supplierId = request.isNull("supplier_id") ? null : Long.valueOf((request.get("supplier_id") + "").trim());

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PengadaanFaktur faktur = fakturPengadaanId == null ? null
					: (PengadaanFaktur) session.get(PengadaanFaktur.class, fakturPengadaanId);
			ais.database.model.library.Penyedia supplier = supplierId == null ? null
					: (ais.database.model.library.Penyedia) session.get(ais.database.model.library.Penyedia.class, supplierId);
			if (faktur != null && kodeFakturAsal.isEmpty()) {
				kodeFakturAsal = faktur.getNomorFaktur();
			}
			if (faktur != null && supplier == null) {
				supplier = faktur.getSupplier();
			}

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
				if (!adminGlobalRb) {
					Long tokoPedagang = pemanggilRb.getToko() == null ? null : pemanggilRb.getToko().getId();
					Long tokoProduk = produk.getToko() == null ? null : produk.getToko().getId();
					if (tokoPedagang == null || !tokoPedagang.equals(tokoProduk)) {
						throw new IllegalArgumentException("Produk baris ke-" + (i + 1) + " bukan milik toko Anda.");
					}
				}

				ReturPembelian rb = new ReturPembelian();
				rb.setProduk(produk);
				rb.setToko(produk.getToko());
				rb.setFakturPengadaan(faktur);
				rb.setKodeFakturAsal(kodeFakturAsal);
				rb.setSupplier(supplier);
				rb.setQty(qty);
				rb.setHargaSatuan(hargaSatuan);
				rb.setTotalNilai(qty * hargaSatuan);
				rb.setAlasan(it.optString("alasan", ""));
				rb.setKeterangan(it.optString("keterangan", ""));
				rb.setWaktu(new Date());
				rb.setOleh(tbmuser == null ? "retur_pembelian" : tbmuser.getUserId());

				session.save(rb);
				ais.action.master.inventory.StokKantinUtil.recomputeStokProduk(produkId);

				ids.put(rb.getId());
				totalNilaiRetur += rb.getTotalNilai();
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
				ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:returPembelianSimpan-rollback");
			}
			hasil.put("status", "91");
			hasil.put("description", Common.tampilErrorJikaAdmin(e));
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** <h3>Retur Pembelian -- riwayat (paginated, per toko).</h3> Pola query SAMA PERSIS {@link #returPenjualanList}. */
	public static void returPembelianList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
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
					: " AND (p.nama ILIKE ? OR COALESCE(rb.kode_faktur_asal,'') ILIKE ?)";
			java.sql.Connection conn = session.connection();

			java.sql.PreparedStatement psCount = conn.prepareStatement(
					"SELECT COUNT(*) FROM koperasi.retur_pembelian rb JOIN koperasi.produk p ON rb.produk = p.id "
							+ "WHERE rb.toko = ?" + andKw);
			int idxC = 1;
			psCount.setLong(idxC++, tokoId);
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
				psCount.setString(idxC++, kw);
				psCount.setString(idxC++, kw);
			}
			java.sql.ResultSet rsCount = psCount.executeQuery();
			long total = rsCount.next() ? rsCount.getLong(1) : 0;
			rsCount.close();
			psCount.close();

			// Kolom fisik rb.hargasatuan/totalnilai TANPA underscore (pola sama ReturPenjualan --
			// entity ReturPembelian tidak punya @Column eksplisit utk field ini).
			java.sql.PreparedStatement ps = conn.prepareStatement(
					"SELECT rb.id, rb.waktu, COALESCE(rb.kode_faktur_asal,''), rb.produk, p.nama, "
							+ "rb.qty, rb.hargasatuan, rb.totalnilai, COALESCE(rb.alasan,''), COALESCE(rb.keterangan,''), COALESCE(rb.oleh,'') "
							+ "FROM koperasi.retur_pembelian rb JOIN koperasi.produk p ON rb.produk = p.id "
							+ "WHERE rb.toko = ?" + andKw + " ORDER BY rb.waktu DESC, rb.id DESC LIMIT ? OFFSET ?");
			int idx = 1;
			ps.setLong(idx++, tokoId);
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
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
				j.put("kodeFakturAsal", rs.getString(3));
				j.put("produkId", rs.getLong(4));
				j.put("namaProduk", rs.getString(5));
				j.put("qty", rs.getDouble(6));
				j.put("hargaSatuan", rs.getDouble(7));
				j.put("totalNilai", rs.getDouble(8));
				j.put("alasan", rs.getString(9));
				j.put("keterangan", rs.getString(10));
				j.put("oleh", rs.getString(11));
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

	/** <h3>Retur Pembelian -- hapus SATU baris (mis. salah entri).</h3> Pola sama {@link #returPenjualanHapus}. */
	public static void returPembelianHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		ais.database.model.inventory.Pedagang pemanggilRb = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobalRb = pemanggilRb == null;
		boolean supervisorRb = pemanggilRb != null && Boolean.TRUE.equals(pemanggilRb.getSupervisor());
		if (!bolehAksiCrud(tbmuser, pemanggilRb, adminGlobalRb, supervisorRb, "returpembelian", "delete")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat menghapus Retur Pembelian.");
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
			ReturPembelian rb = (ReturPembelian) session.get(ReturPembelian.class, id);
			if (rb == null) {
				hasil.put("status", "91");
				hasil.put("description", "Data retur tidak ditemukan.");
				return;
			}
			if (!adminGlobalRb) {
				Long tokoPedagang = pemanggilRb.getToko() == null ? null : pemanggilRb.getToko().getId();
				Long tokoRetur = rb.getToko() == null ? null : rb.getToko().getId();
				if (tokoPedagang == null || !tokoPedagang.equals(tokoRetur)) {
					hasil.put("status", "91");
					hasil.put("description", "Retur ini bukan milik toko Anda.");
					return;
				}
			}

			Long produkId = rb.getProduk() == null ? null : rb.getProduk().getId();
			session.beginTransaction();
			session.delete(rb);
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
				ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinHelper.java:returPembelianHapus-rollback");
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
		ais.database.model.Tbmrole role = tbmuser.hakAkses();
		org.json.JSONObject menuRole = ais.common.EbisnisMenuKatalog.urai(
				role == null ? null : role.getEbisnisMenu());
		boolean bolehSupervisor = pedagang == null || Boolean.TRUE.equals(pedagang.getSupervisor())
				|| menuRole.optBoolean("supervisor", false);
		boolean bolehHapus = ais.common.EbisnisMenuKatalog.bolehAksi(
				menuRole, "riwayatpenjualan", "delete")
				|| ais.common.EbisnisMenuKatalog.bolehAksi(
						menuRole, "riwayatpenjualan", "reject");
		if (!bolehSupervisor && !bolehHapus) {
			hasil.put("status", "91");
			hasil.put("description", "Akun Anda tidak memiliki hak pembatalan transaksi. Minta supervisor atau admin memberikan izin Hapus/Tolak pada menu Riwayat Penjualan.");
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

	/** Daftar sesi kas pada toko aktif untuk tab supervisor di Konfigurasi. */
	@SuppressWarnings("unchecked")
	public static void sesiKasDaftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = request.isNull("id_toko") ? null : Long.valueOf(request.get("id_toko").toString());
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			if (!bolehAksesTokoSesiKas(session, tbmuser, tokoId)) {
				hasil.put("status", "91");
				hasil.put("description", "Toko sesi kas tidak termasuk toko yang boleh diakses akun ini.");
				return;
			}
			org.hibernate.Criteria c = session.createCriteria(SesiKasKasir.class);
			if (tokoId != null) c.createAlias("toko", "t").add(Restrictions.eq("t.id", tokoId));
			c.addOrder(Order.desc("waktuBuka")).setMaxResults(200);
			java.util.List<SesiKasKasir> daftar = c.list();
			JSONArray rows = new JSONArray();
			for (SesiKasKasir sesi : daftar) {
				Date sampai = SesiKasKasir.STATUS_TUTUP.equals(sesi.getStatus()) && sesi.getWaktuTutup() != null
						? sesi.getWaktuTutup() : new Date();
				JSONObject laporan;
				if (SesiKasKasir.STATUS_TUTUP.equals(sesi.getStatus())) {
					try {
						laporan = sesi.getLaporanTutupJson() == null ? new JSONObject()
								: new JSONObject(sesi.getLaporanTutupJson());
					} catch (Exception eSnapshot) {
						laporan = new JSONObject();
					}
					if (!laporan.has("penjualanTunai")) laporan.put("penjualanTunai", sesi.getTotalTunai());
					if (!laporan.has("penjualanNonTunai")) laporan.put("penjualanNonTunai", sesi.getTotalNonTunai());
					if (!laporan.has("kasSeharusnya")) laporan.put("kasSeharusnya", sesi.getModalAwal() + sesi.getTotalTunai());
				} else {
					laporan = ais.action.master.koperasi.helper.SesiKasUtil.laporanTutupKas(session, sesi, sampai, 0);
				}
				JSONObject row = new JSONObject();
				row.put("id", sesi.getId());
				row.put("kode", sesi.getKode() == null ? JSONObject.NULL : sesi.getKode());
				row.put("kasirNama", sesi.getKasirNama() == null ? "" : sesi.getKasirNama());
				row.put("kasirUserId", sesi.getKasirUserId() == null ? "" : sesi.getKasirUserId());
				row.put("tokoId", sesi.getToko() == null ? JSONObject.NULL : sesi.getToko().getId());
				row.put("tokoNama", sesi.getToko() == null ? "" : sesi.getToko().getNama());
				row.put("perangkat", sesi.getNamaPerangkat() == null || sesi.getNamaPerangkat().trim().length() == 0
						? (sesi.getIdPerangkat() == null ? "" : sesi.getIdPerangkat()) : sesi.getNamaPerangkat());
				row.put("waktuBuka", Common.dateFormatInput.get().format(sesi.getWaktuBuka()));
				row.put("waktuTutup", sesi.getWaktuTutup() == null ? JSONObject.NULL
						: Common.dateFormatInput.get().format(sesi.getWaktuTutup()));
				row.put("statusSesi", sesi.getStatus());
				row.put("modalAwal", sesi.getModalAwal());
				row.put("penjualanTunai", laporan.optDouble("penjualanTunai", sesi.getTotalTunai()));
				row.put("penjualanNonTunai", laporan.optDouble("penjualanNonTunai", sesi.getTotalNonTunai()));
				row.put("kasSeharusnya", laporan.optDouble("kasSeharusnya", 0));
				row.put("uangFisik", SesiKasKasir.STATUS_TUTUP.equals(sesi.getStatus()) ? sesi.getUangFisik() : JSONObject.NULL);
				row.put("selisih", SesiKasKasir.STATUS_TUTUP.equals(sesi.getStatus()) ? sesi.getSelisih() : JSONObject.NULL);
				row.put("jumlahTransaksi", laporan.optInt("jumlahTransaksi", 0));
				row.put("keterangan", sesi.getKeterangan() == null ? "" : sesi.getKeterangan());
				rows.put(row);
			}
			hasil.put("status", "00");
			hasil.put("sesi", rows);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	private static boolean bolehAksesTokoSesiKas(Session session, Tbmuser tbmuser, Long tokoId) {
		if (tbmuser == null || tbmuser.getPedagang() == null) return true;
		if (tokoId == null) return false;
		java.util.List<Toko> daftar = daftarTokoBolehDiakses(session, tbmuser);
		for (Toko toko : daftar) if (toko != null && tokoId.equals(toko.getId())) return true;
		return false;
	}

	/** Koreksi status dan nominal sesi; pemanggil wajib sudah lolos gerbang supervisor di PosApi. */
	public static void sesiKasKoreksi(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long idSesi = request.isNull("id_sesi") ? null : Long.valueOf(request.get("id_sesi").toString());
		String statusBaru = request.optString("status_sesi", "").trim().toUpperCase();
		String alasan = request.optString("alasan_koreksi", "").trim();
		if (idSesi == null || (!SesiKasKasir.STATUS_BUKA.equals(statusBaru) && !SesiKasKasir.STATUS_TUTUP.equals(statusBaru))) {
			hasil.put("status", "91"); hasil.put("description", "Sesi dan status koreksi wajib dipilih."); return;
		}
		if (alasan.length() < 5) {
			hasil.put("status", "91"); hasil.put("description", "Alasan koreksi wajib diisi minimal 5 karakter."); return;
		}
		double modal = request.optDouble("modal_awal", -1);
		double tunai = request.optDouble("penjualan_tunai", -1);
		double uangFisik = request.optDouble("uang_fisik", -1);
		if (modal < 0 || Double.isNaN(modal) || Double.isInfinite(modal)) {
			hasil.put("status", "91"); hasil.put("description", "Modal awal harus berupa angka nol atau lebih."); return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		org.hibernate.Transaction tx = null;
		try {
			tx = session.beginTransaction();
			SesiKasKasir sesi = (SesiKasKasir) session.get(SesiKasKasir.class, idSesi);
			if (sesi == null) { hasil.put("status", "91"); hasil.put("description", "Sesi kas tidak ditemukan."); return; }
			Long tokoSesi = sesi.getToko() == null ? null : sesi.getToko().getId();
			if (!bolehAksesTokoSesiKas(session, tbmuser, tokoSesi)) {
				hasil.put("status", "91"); hasil.put("description", "Sesi kas berada di toko yang tidak boleh diakses akun ini."); return;
			}
			String statusLama = sesi.getStatus();
			if (SesiKasKasir.STATUS_BUKA.equals(statusBaru)) {
				SesiKasKasir sesiAkun = ais.action.master.koperasi.helper.SesiKasUtil.sesiTerbuka(
						session, sesi.getKasirNama(), sesi.getKasirUserId(), null);
				SesiKasKasir sesiPerangkat = ais.action.master.koperasi.helper.SesiKasUtil.sesiTerbukaPadaPerangkat(
						session, null, sesi.getIdPerangkat());
				if ((sesiAkun != null && !sesiAkun.getId().equals(sesi.getId()))
						|| (sesiPerangkat != null && !sesiPerangkat.getId().equals(sesi.getId()))) {
					hasil.put("status", "91");
					hasil.put("description", "Sesi tidak dapat dibuka kembali karena kasir atau perangkat masih memiliki sesi terbuka lain.");
					return;
				}
			}
			sesi.setModalAwal(Double.valueOf(modal));
			String pelaku = tbmuser == null ? "admin" : tbmuser.getUserId();
			String audit = "[KOREKSI SUPERVISOR " + pelaku + "] Status " + statusLama + " menjadi "
					+ statusBaru + ", modal Rp " + Math.round(modal) + ". Alasan: " + alasan;
			String catatan = sesi.getKeterangan();
			catatan = catatan == null || catatan.trim().length() == 0 ? audit : catatan.trim() + "\n" + audit;
			if (SesiKasKasir.STATUS_TUTUP.equals(statusBaru)) {
				if (tunai < 0 || uangFisik < 0 || Double.isNaN(tunai) || Double.isInfinite(tunai)
						|| Double.isNaN(uangFisik) || Double.isInfinite(uangFisik)) {
					throw new IllegalArgumentException("Penjualan tunai dan uang fisik harus berupa angka nol atau lebih.");
				}
				Date waktuTutup = sesi.getWaktuTutup() == null ? new Date() : sesi.getWaktuTutup();
				ais.action.master.koperasi.helper.SesiKasUtil.tutup(session, sesi, uangFisik, catatan,
						waktuTutup, Double.valueOf(tunai));
			} else {
				sesi.setStatus(SesiKasKasir.STATUS_BUKA);
				sesi.setWaktuTutup(null);
				sesi.setTotalTunai(Double.valueOf(0));
				sesi.setTotalNonTunai(Double.valueOf(0));
				sesi.setUangFisik(Double.valueOf(0));
				sesi.setSelisih(Double.valueOf(0));
				sesi.setLaporanTutupJson(null);
				sesi.setKeterangan(catatan);
			}
			// `sesi` berasal dari session.get(), sehingga perubahan setter otomatis dipersist oleh
			// dirty checking. Flush dan baca balik langsung dari DB mencegah respons sukses palsu.
			session.flush();
			Object statusTersimpan = session.createSQLQuery(
					"select status as status_sesi from koperasi.sesi_kas_kasir where id=:id")
					.addScalar("status_sesi", org.hibernate.Hibernate.STRING)
					.setParameter("id", idSesi).uniqueResult();
			if (statusTersimpan == null || !statusBaru.equals(statusTersimpan.toString())) {
				throw new IllegalStateException("Status sesi kas gagal disimpan. Perubahan dibatalkan agar data tidak setengah tersimpan.");
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("description", "Koreksi sesi kas berhasil disimpan.");
			hasil.put("statusSesi", statusBaru);
			hasil.put("modalAwal", sesi.getModalAwal());
			hasil.put("penjualanTunai", sesi.getTotalTunai());
			hasil.put("uangFisik", sesi.getUangFisik());
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) tx.rollback();
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "sesiKasKoreksi-rollback");
			}
			throw e;
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/** Hanya admin global atau supervisor toko yang boleh mengoreksi transaksi lunas. */
	public static boolean bolehEditTransaksi(Tbmuser tbmuser) {
		if (tbmuser == null) return false;
		ais.database.model.inventory.Pedagang pedagang = tbmuser.getPedagang();
		if (pedagang == null || Boolean.TRUE.equals(pedagang.getSupervisor())) return true;
		ais.database.model.Tbmrole role = tbmuser.hakAkses();
		org.json.JSONObject menuRole = ais.common.EbisnisMenuKatalog.urai(
				role == null ? null : role.getEbisnisMenu());
		return menuRole.optBoolean("supervisor", false);
	}

	/** Mencari akun kasir aktif langsung dari public.tbmuser untuk koreksi transaksi. */
	public static void editTransaksiKasirCari(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehEditTransaksi(tbmuser)) {
			hasil.put("status", "91"); hasil.put("description", "Hanya supervisor atau admin yang dapat mencari kasir untuk koreksi transaksi."); return;
		}
		String kata = request.optString("keyword", "").trim();
		if (kata.length() < 2) {
			hasil.put("status", "91"); hasil.put("description", "Ketik sedikitnya 2 karakter ID atau nama kasir."); return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		java.sql.PreparedStatement ps = null; java.sql.ResultSet rs = null;
		try {
			ps = session.connection().prepareStatement("select userid,coalesce(nullif(trim(usernama),''),userid) from public.tbmuser "
					+ "where coalesce(aktif,true)=true and (userid ilike ? or coalesce(usernama,'') ilike ?) "
					+ "order by coalesce(nullif(trim(usernama),''),userid),userid limit 20");
			String pola = "%" + kata + "%"; ps.setString(1, pola); ps.setString(2, pola); rs = ps.executeQuery();
			JSONArray data = new JSONArray();
			while (rs.next()) { JSONObject akun = new JSONObject(); akun.put("userId", rs.getString(1)); akun.put("nama", rs.getString(2)); data.put(akun); }
			hasil.put("status", "00"); hasil.put("data", data);
		} finally {
			try { if (rs != null) rs.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "editTransaksiKasirCari-rs-close"); }
			try { if (ps != null) ps.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "editTransaksiKasirCari-ps-close"); }
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Audit read-only stok tersimpan terhadap saldo yang dibentuk seluruh sumber
	 * mutasi kanonik. Query UNION dihitung sekali per produk agar layar tidak
	 * menjalankan delapan subquery untuk setiap baris produk.
	 */
	public static void produkRekonsiliasiLedger(Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		Long tokoId = soResolveTokoId(tbmuser, request);
		if (tokoId == null) {
			hasil.put("status", "91");
			hasil.put("description", "Toko tidak diketahui.");
			return;
		}
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(1, request.optInt("page_size", 15)));
		String keyword = request.optString("keyword", "").trim().toLowerCase();
		boolean hanyaSelisih = request.optBoolean("hanya_selisih", true);
		String mutasi = "SELECT produk AS produk_id, qty, waktupengadaan AS waktu FROM koperasi.pengadaan_produk"
				+ " UNION ALL SELECT produk, selisih, waktuopname FROM koperasi.stok_opname"
				+ " UNION ALL SELECT produk, -qty, waktu FROM koperasi.pembelian"
				+ " UNION ALL SELECT produk, -qty, waktu FROM koperasi.pemakaian_bahan_baku"
				+ " UNION ALL SELECT produk, qty, waktu FROM koperasi.retur_penjualan WHERE kembalikan_ke_stok=true"
				+ " UNION ALL SELECT produk_tujuan, qty, waktu FROM koperasi.mutasi_stok_toko WHERE produk_tujuan IS NOT NULL"
				+ " UNION ALL SELECT produk_asal, -qty, waktu FROM koperasi.mutasi_stok_toko WHERE produk_asal IS NOT NULL"
				+ " UNION ALL SELECT produk, -qty, waktu FROM koperasi.retur_pembelian";
		String cte = "WITH mutasi AS (" + mutasi + "), ledger AS (SELECT produk_id,COALESCE(SUM(qty),0) stok_ledger,MAX(waktu) terakhir FROM mutasi GROUP BY produk_id) ";
		String filter = " WHERE p.toko=:tokoId";
		if (keyword.length() > 0) {
			filter += " AND (LOWER(COALESCE(p.nama,'')) LIKE :keyword OR LOWER(COALESCE(p.kode,'')) LIKE :keyword OR LOWER(COALESCE(p.barcode,'')) LIKE :keyword)";
		}
		if (hanyaSelisih) {
			filter += " AND l.produk_id IS NOT NULL AND ABS(COALESCE(p.stok,0)-COALESCE(l.stok_ledger,0))>0.000001";
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			SQLQuery stats = session.createSQLQuery(cte
					+ "SELECT COUNT(*),SUM(CASE WHEN l.produk_id IS NOT NULL THEN 1 ELSE 0 END),"
					+ "SUM(CASE WHEN l.produk_id IS NULL THEN 1 ELSE 0 END),"
					+ "COALESCE(SUM(CASE WHEN l.produk_id IS NOT NULL THEN ABS(COALESCE(p.stok,0)-COALESCE(l.stok_ledger,0)) ELSE 0 END),0)"
					+ " FROM koperasi.produk p LEFT JOIN ledger l ON l.produk_id=p.id WHERE p.toko=:tokoId");
			stats.setLong("tokoId", tokoId.longValue());
			Object[] s = (Object[]) stats.uniqueResult();
			hasil.put("jumlahProduk", s == null || s[0] == null ? 0 : ((Number) s[0]).longValue());
			hasil.put("produkTercakupLedger", s == null || s[1] == null ? 0 : ((Number) s[1]).longValue());
			hasil.put("produkBelumTercakup", s == null || s[2] == null ? 0 : ((Number) s[2]).longValue());
			hasil.put("totalSelisihAbsolut", s == null || s[3] == null ? 0 : ((Number) s[3]).doubleValue());

			SQLQuery count = session.createSQLQuery(cte + "SELECT COUNT(*) FROM koperasi.produk p LEFT JOIN ledger l ON l.produk_id=p.id" + filter);
			count.setLong("tokoId", tokoId.longValue());
			if (keyword.length() > 0) count.setString("keyword", "%" + keyword + "%");
			Number total = (Number) count.uniqueResult();
			hasil.put("total", total == null ? 0 : total.longValue());

			SQLQuery list = session.createSQLQuery(cte
					+ "SELECT p.id,p.nama,p.kode,p.barcode,COALESCE(p.stok,0),COALESCE(l.stok_ledger,0),"
					+ "COALESCE(p.stok,0)-COALESCE(l.stok_ledger,0),l.terakhir"
					+ " FROM koperasi.produk p LEFT JOIN ledger l ON l.produk_id=p.id" + filter
					+ " ORDER BY ABS(COALESCE(p.stok,0)-COALESCE(l.stok_ledger,0)) DESC,p.nama ASC");
			list.setLong("tokoId", tokoId.longValue());
			if (keyword.length() > 0) list.setString("keyword", "%" + keyword + "%");
			list.setFirstResult((page - 1) * pageSize);
			list.setMaxResults(pageSize);
			@SuppressWarnings("unchecked")
			java.util.List<Object[]> rows = list.list();
			JSONArray data = new JSONArray();
			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
			for (Object[] r : rows) {
				JSONObject o = new JSONObject();
				o.put("id", r[0]); o.put("nama", r[1] == null ? "-" : r[1].toString());
				o.put("kode", r[2] == null ? "" : r[2].toString()); o.put("barcode", r[3] == null ? "" : r[3].toString());
				o.put("stokTersimpan", r[4] == null ? 0 : ((Number) r[4]).doubleValue());
				o.put("stokLedger", r[5] == null ? 0 : ((Number) r[5]).doubleValue());
				o.put("selisih", r[6] == null ? 0 : ((Number) r[6]).doubleValue());
				o.put("mutasiTerakhir", r[7] == null ? null : fmt.format((java.util.Date) r[7]));
				data.put(o);
			}
			hasil.put("data", data);
			hasil.put("status", "00");
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * Koreksi transaksi oleh supervisor. Semua perubahan dilakukan atomik, dicatat oleh Hibernate
	 * Envers, dan alasan koreksi ikut disimpan pada keterangan header agar mudah ditelusuri.
	 */
	@SuppressWarnings("unchecked")
	public static void editTransaksi(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehEditTransaksi(tbmuser)) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya supervisor atau admin yang dapat mengubah transaksi yang sudah selesai.");
			return;
		}
		String alasan = request.optString("alasan", "").trim();
		JSONArray itemRequest = request.optJSONArray("item");
		if (alasan.length() < 5 || itemRequest == null || itemRequest.length() == 0 || request.isNull("id")) {
			hasil.put("status", "91");
			hasil.put("description", "Alasan minimal 5 karakter dan sedikitnya satu barang wajib diisi.");
			return;
		}
		if (alasan.length() > 1000) {
			hasil.put("status", "91");
			hasil.put("description", "Alasan koreksi maksimal 1000 karakter.");
			return;
		}
		Long transaksiId;
		try { transaksiId = Long.valueOf((request.get("id") + "").trim()); }
		catch (Exception e) {
			hasil.put("status", "91"); hasil.put("description", "ID transaksi tidak valid."); return;
		}
		Date waktuBaru = null;
		String waktuTeks = request.optString("waktu", "").trim();
		String[] polaWaktu = new String[] { "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss", "dd-MM-yyyy HH:mm:ss", "dd-MM-yyyy HH:mm" };
		for (int i = 0; waktuBaru == null && i < polaWaktu.length; i++) {
			try { SimpleDateFormat f = new SimpleDateFormat(polaWaktu[i]); f.setLenient(false); waktuBaru = f.parse(waktuTeks); }
			catch (Exception abaikan) { }
		}
		if (waktuBaru == null || waktuBaru.after(new Date(System.currentTimeMillis() + 60000L))) {
			hasil.put("status", "91"); hasil.put("description", "Tanggal dan jam transaksi tidak valid atau berada di masa depan."); return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		org.hibernate.Transaction tx = null;
		try {
			PembelianAnggotaKoperasi trx = (PembelianAnggotaKoperasi) session.get(PembelianAnggotaKoperasi.class, transaksiId);
			if (trx == null) throw new IllegalStateException("Transaksi tidak ditemukan atau merupakan transaksi lama yang tidak dapat dikoreksi.");
			Toko tokoLogin = tbmuser.getPedagang() == null ? null : tbmuser.getPedagang().getToko();
			if (tokoLogin != null && (trx.getToko() == null || !tokoLogin.getId().equals(trx.getToko().getId())))
				throw new IllegalStateException("Transaksi ini bukan milik toko yang sedang login.");
			if (trx.getPostingHistory() != null)
				throw new IllegalStateException("Transaksi sudah diposting ke jurnal sehingga tidak dapat diedit.");
			Number retur = (Number) session.createSQLQuery("select count(*) from koperasi.retur_penjualan where pembelian_anggota_koperasi_id=:id")
					.setParameter("id", transaksiId).uniqueResult();
			if (retur != null && retur.longValue() > 0)
				throw new IllegalStateException("Transaksi sudah memiliki retur sehingga tidak dapat diedit.");
			Tbmuser kasirBaru = null;
			String kasirUserIdBaru = request.optString("kasir_user_id", "").trim();
			if (kasirUserIdBaru.length() > 0) {
				kasirBaru = (Tbmuser) session.get(Tbmuser.class, kasirUserIdBaru);
				if (kasirBaru == null || !Boolean.TRUE.equals(kasirBaru.getAktif())) throw new IllegalStateException("Akun kasir yang dipilih tidak ditemukan atau sudah tidak aktif.");
			}

			List<Pembelian> lama = session.createCriteria(Pembelian.class)
					.add(Restrictions.eq("pembelianAnggotaKoperasi", trx)).list();
			Map<Long, Pembelian> lamaById = new HashMap<Long, Pembelian>();
			java.util.Set<Long> produkTerdampak = new java.util.HashSet<Long>();
			double diskonItemLama = 0.0;
			for (Pembelian p : lama) {
				lamaById.put(p.getId(), p);
				if (p.getProduk() != null) produkTerdampak.add(p.getProduk().getId());
				diskonItemLama += p.getDiskon() == null ? 0.0 : p.getDiskon().doubleValue();
			}
			double diskonFakturTetap = Math.max(0.0, (trx.getTotalDiskon() == null ? 0.0 : trx.getTotalDiskon().doubleValue()) - diskonItemLama);
			java.util.Set<Long> dipakai = new java.util.HashSet<Long>();
			tx = session.beginTransaction();
			double jumlahRincian = 0.0, diskonItemBaru = 0.0, cashbackBaru = 0.0;
			for (int i = 0; i < itemRequest.length(); i++) {
				JSONObject j = itemRequest.getJSONObject(i);
				double qty = j.optDouble("qty", 0.0);
				if (qty <= 0) throw new IllegalStateException("Jumlah barang pada baris " + (i + 1) + " harus lebih dari nol.");
				Long pembelianId = null;
				if (!j.isNull("pembelian_id") && Common.isNumber(j.optString("pembelian_id", ""))) pembelianId = Long.valueOf(j.optString("pembelian_id"));
				Pembelian p = pembelianId == null ? null : lamaById.get(pembelianId);
				if (pembelianId != null && p == null) throw new IllegalStateException("Rincian transaksi tidak sesuai dengan transaksi yang dipilih.");
				Long produkId = Common.isNumber(j.optString("produk_id", "")) ? Long.valueOf(j.optString("produk_id")) : null;
				Produk produk = produkId == null ? null : (Produk) session.get(Produk.class, produkId);
				if (produk == null || produk.getToko() == null || trx.getToko() == null || !trx.getToko().getId().equals(produk.getToko().getId()))
					throw new IllegalStateException("Produk pada baris " + (i + 1) + " tidak ditemukan di toko transaksi.");
				if (p == null) {
					p = new Pembelian();
					p.setPembelianAnggotaKoperasi(trx); p.setAnggotaKoperasi(trx.getAnggotaKoperasi());
					p.setToko(trx.getToko()); p.setProduk(produk); p.setKode(trx.getKode() + "-" + produk.getKode());
					p.setHargaSatuan(produk.getHargaJual()); p.setDiskon(Double.valueOf(0)); p.setCashback(Double.valueOf(0));
					p.setCaraPembayaranKoperasi(trx.getCaraPembayaranKoperasi()); p.setTbmuser(kasirBaru == null ? trx.getTbmuser() : kasirBaru);
					p.setOleh(tbmuser.getUserNama()); p.setOlehId(tbmuser.getId() + "");
					session.save(p);
				} else {
					dipakai.add(p.getId());
					double qtyLama = p.getQty() == null ? 0.0 : p.getQty().doubleValue();
					double diskonLama = p.getDiskon() == null ? 0.0 : p.getDiskon().doubleValue();
					p.setDiskon(Double.valueOf(qtyLama <= 0 ? 0.0 : diskonLama * qty / qtyLama));
				}
				p.setProduk(produk); p.setQty(Double.valueOf(qty)); p.setWaktu(waktuBaru);
				if (kasirBaru != null) p.setTbmuser(kasirBaru);
				p.setTotal(Double.valueOf((p.getHargaSatuan() * qty) - p.getDiskon())); session.saveOrUpdate(p);
				produkTerdampak.add(produk.getId());
				jumlahRincian += Math.max(0.0, (p.getHargaSatuan() * qty) - p.getDiskon());
				diskonItemBaru += p.getDiskon() == null ? 0.0 : p.getDiskon().doubleValue();
				cashbackBaru += p.getCashback() == null ? 0.0 : p.getCashback().doubleValue();
			}
			for (Pembelian p : lama) if (!dipakai.contains(p.getId())) session.delete(p);

			double pajak = trx.getHargaPpn() == null ? 0.0 : trx.getHargaPpn().doubleValue();
			double totalBaru = Math.max(0.0, jumlahRincian - diskonFakturTetap + pajak);
			double tunaiLama = trx.getBayarTunai() == null ? 0.0 : trx.getBayarTunai().doubleValue();
			double nonTunaiLama = trx.getBayarNonTunai() == null ? 0.0 : trx.getBayarNonTunai().doubleValue();
			double komposisi = tunaiLama + nonTunaiLama;
			if (komposisi > 0) {
				double tunaiBaru = totalBaru * tunaiLama / komposisi;
				trx.setBayarTunai(Double.valueOf(tunaiBaru)); trx.setBayarNonTunai(Double.valueOf(totalBaru - tunaiBaru));
			}
			/*
			 * Header transaksi yang berasal dari pesanan tertahan tetap membaca tanggal dan kasir
			 * dari draft asal (lihat getter PembelianAnggotaKoperasi). Karena itu koreksi harus
			 * diterapkan ke kedua entity dalam transaksi database yang sama. Jika hanya header
			 * yang diubah, Hibernate akan membaca getter saat flush dan mengembalikan nilai lama
			 * dari draft; rincian Pembelian kemudian ikut memakai tanggal lama tersebut.
			 */
			DraftPembelianAnggotaKoperasi draftAsal = trx.getDraftPembelianAnggotaKoperasi();
			if (draftAsal != null) {
				draftAsal.setTanggalPembayaran(waktuBaru);
				if (kasirBaru != null) {
					draftAsal.setTbmuser(kasirBaru);
					draftAsal.setKasirLoginNama(kasirBaru.getUserNama() == null
							|| kasirBaru.getUserNama().trim().length() == 0
									? kasirBaru.getUserId() : kasirBaru.getUserNama());
				}
				session.update(draftAsal);
			}
			trx.setKembalian(Double.valueOf(0)); trx.setTanggalPembayaran(waktuBaru);
			Tbmuser kasirEfektif = kasirBaru == null ? trx.getTbmuser() : kasirBaru;
			String namaKasirEfektif = trx.getKasirLoginNama();
			if (kasirBaru != null) {
				namaKasirEfektif = kasirBaru.getUserNama();
				if (namaKasirEfektif == null || namaKasirEfektif.trim().length() == 0) namaKasirEfektif = kasirBaru.getUserId();
				trx.setTbmuser(kasirBaru); trx.setKasirLoginNama(namaKasirEfektif);
			}
			org.hibernate.Criteria kriteriaSesi = session.createCriteria(SesiKasKasir.class)
					.add(Restrictions.eq("toko", trx.getToko())).add(Restrictions.le("waktuBuka", waktuBaru))
					.add(Restrictions.or(Restrictions.isNull("waktuTutup"), Restrictions.ge("waktuTutup", waktuBaru)));
			if (kasirEfektif != null && kasirEfektif.getUserId() != null) kriteriaSesi.add(Restrictions.or(Restrictions.eq("kasirUserId", kasirEfektif.getUserId()), Restrictions.eq("kasirNama", namaKasirEfektif)));
			else if (namaKasirEfektif != null && namaKasirEfektif.trim().length() > 0) kriteriaSesi.add(Restrictions.eq("kasirNama", namaKasirEfektif));
			else kriteriaSesi.add(Restrictions.sqlRestriction("1=0"));
			trx.setSesiKasKasir((SesiKasKasir) kriteriaSesi.addOrder(Order.desc("waktuBuka")).setMaxResults(1).uniqueResult());
			trx.setTotalDiskon(Double.valueOf(diskonItemBaru + diskonFakturTetap));
			trx.setTotalCashback(Double.valueOf(cashbackBaru)); trx.setTotalBiaya(Double.valueOf(totalBaru)); trx.setBiaya(Double.valueOf(totalBaru));
			String catatanLama = trx.getKeterangan() == null ? "" : trx.getKeterangan().trim();
			String catatanKoreksi = "KOREKSI SUPERVISOR " + new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()) + ": " + alasan;
			String catatanGabung = (catatanLama.length() == 0 ? "" : catatanLama + " | ") + catatanKoreksi;
			if (catatanGabung.length() > 255) catatanGabung = catatanKoreksi;
			if (catatanGabung.length() > 255) catatanGabung = catatanGabung.substring(0, 255);
			trx.setKeterangan(catatanGabung);
			trx.setOleh(tbmuser.getUserNama()); trx.setOlehId(tbmuser.getId() + ""); session.update(trx); session.flush();
			for (Long produkId : produkTerdampak) ais.action.master.inventory.StokKantinUtil.recomputeStokProdukNative(session, produkId);
			tx.commit();
			hasil.put("status", "00"); hasil.put("description", "Transaksi berhasil dikoreksi. Total pembayaran dan stok sudah dihitung ulang.");
			hasil.put("totalBiaya", totalBaru);
		} catch (Exception e) {
			if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception rollback) { ais.common.ErrorAuditUtil.record(rollback, "editTransaksi-rollback"); }
			hasil.put("status", "91"); hasil.put("description", e instanceof IllegalStateException ? e.getMessage() : Common.tampilErrorJikaAdmin(e));
		} finally { tutupSessionPolaB(session); }
	}

	/**
	 * <h3>Kasir (Desktop/Android) -- evaluasi Aturan Diskon otomatis utk isi keranjang saat ini.</h3>
	 *
	 * <p>MURNI MENGHITUNG, tidak menyimpan apa pun -- Desktop/Android dipanggil ulang setiap kali
	 * keranjang berubah (tambah/qty/hapus/pilih member/pilih toko), lalu memasukkan angka {@code
	 * diskon}/{@code cashback}/{@code aturanDiskon} hasilnya ke payload {@code transaksi[]} yang SAMA
	 * dikirim ke aksi {@code bayar}. Saat checkout, {@link #bayar} mengevaluasi ulang aturan tersebut
	 * di server melalui {@link #terapkanEvaluasiDiskonServer}; angka dari klien hanya untuk pratinjau
	 * dan bukan sumber kebenaran finansial.</p>
	 *
	 * <p>Logika PORTING 1:1 dari mesin evaluasi client-side yang SUDAH ADA -- JSP {@code _pos.jsp}
	 * (fungsi {@code evaluateDiscount}/{@code recalculateCart}/{@code loadAturanDiskon}/{@code
	 * updateUsageDiskonMember}) dan ZK ({@code PosKantinAction.evaluasiDiskon}) -- BUKAN logika baru:
	 * cocokkan aturan PERTAMA per baris (produk cocok ATAU aturan berlaku semua produk -- kolom
	 * {@code aturan_diskon.produk} NULLABLE, {@code NULL} = semua produk; toko cocok/berlaku-semua-toko;
	 * jendela tanggal berlaku; hari aktif; target member semua/jenis/tipe; DIKECUALIKAN kalau
	 * {@code aktivasiManual=true} -- aturan itu hanya bisa aktif lewat kasir memilih manual, lihat
	 * {@link #diskonManualList}), hitung persentase ATAU nominal (persentase diprioritaskan, nominal
	 * dikali qty lalu dibatasi maks total baris), lalu terapkan batas maksimal potongan -- KUMULATIF
	 * per-hari-per-toko bila aturan itu {@code berlakuPerHariDanPerToko} (dijumlah dari transaksi hari
	 * ini yg SUDAH tersimpan di server + akumulasi antar baris DALAM SATU pemanggilan ini), atau
	 * per-baris biasa bila tidak.</p>
	 *
	 * @param request payload: {@code toko_id} (wajib utk admin global, diabaikan utk pedagang/kasir),
	 *                {@code id_member} (opsional -- tanpa ini, aturan yg menyasar member spesifik
	 *                otomatis tidak berlaku, sama spt kasir belum pilih member di JSP/ZK), {@code
	 *                hanya_aturan_id} (opsional, level-request -- default bila item tak menyertakan
	 *                miliknya sendiri, lihat di bawah), {@code items} (array {@code {id, harga, jumlah,
	 *                hanya_aturan_id}} -- id = id {@link Produk}, harga = harga satuan yg dipakai di
	 *                keranjang saat ini, jumlah = qty, {@code hanya_aturan_id} OPSIONAL PER BARIS --
	 *                gap-closure "Aktivasi Manual": kosongkan utk baris auto-apply biasa (aturan
	 *                {@code aktivasiManual=true} dikecualikan), isi dgn id {@code AturanDiskon} kalau
	 *                kasir sudah memilih promo manual utk baris itu lewat picker "Promo" (lihat
	 *                {@link #diskonManualList}) -- 1 keranjang boleh campur baris auto &amp; manual
	 *                dalam SATU panggilan ini).
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
		Long hanyaAturanId = request.isNull("hanya_aturan_id") ? null
				: Long.valueOf((request.get("hanya_aturan_id") + "").trim());
		JSONArray items = request.optJSONArray("items");
		if (items == null) {
			items = new JSONArray();
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			JSONArray outArr = evaluasiDiskonItems(conn, tokoId, memberId, items, hanyaAturanId);
			hasil.put("status", "00");
			hasil.put("items", outArr);
		} finally {
			tutupSessionPolaB(session);
		}
	}

	/**
	 * <h3>Kasir -- daftar Promo "Aktivasi Manual" yang eligible utk keranjang saat ini.</h3>
	 *
	 * <p>Promo dgn {@code aktivasiManual=true} SENGAJA dikecualikan dari {@link #diskonEvaluasi}
	 * (jalur auto-apply) -- baru bisa aktif kalau kasir memilihnya lewat picker "Promo" di UI. Method
	 * ini murni LISTING (nama/keterangan, tanpa menghitung nominal potongan -- nominal baru dihitung
	 * saat kasir sudah pilih 1 promo, lewat panggilan {@link #diskonEvaluasi} ulang dgn {@code
	 * hanya_aturan_id} diisi id promo terpilih, memakai mesin hitung yang SAMA PERSIS dgn auto-apply).
	 * Reuse total: query kandidat + cek kelayakan SAMA PERSIS dgn {@link #evaluasiDiskonItems}, lewat
	 * {@link #loadAturanDiskonKandidat}/{@link #aturanEligibleUntukItem}.</p>
	 *
	 * @param request payload sama dgn {@link #diskonEvaluasi}: {@code toko_id}, {@code id_member}
	 *                (opsional), {@code items} ({@code {id, harga, jumlah}}).
	 * @param hasil   diisi {@code status="00"}, {@code promo} (array {@code {id, namaAturan,
	 *                keterangan, persentase, nominal, potonganLangsung}}, hanya promo yang eligible
	 *                utk MINIMAL SATU baris di {@code items}).
	 */
	public static void diskonManualList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
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

			java.util.LinkedHashSet<Long> produkIdSet = new java.util.LinkedHashSet<Long>();
			java.util.List<Long> produkIdPerItem = new java.util.ArrayList<Long>();
			for (int i = 0; i < items.length(); i++) {
				JSONObject it = items.getJSONObject(i);
				Long pid = it.isNull("id") ? null : Long.valueOf((it.get("id") + "").trim());
				produkIdPerItem.add(pid);
				if (pid != null) {
					produkIdSet.add(pid);
				}
			}
			if (produkIdSet.isEmpty()) {
				hasil.put("status", "00");
				hasil.put("promo", new JSONArray());
				return;
			}

			java.util.List<java.util.Map<String, Object>> rules = loadAturanDiskonKandidat(conn, tokoId, produkIdSet);

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
			}

			JSONArray promoArr = new JSONArray();
			for (java.util.Map<String, Object> r : rules) {
				if (!Boolean.TRUE.equals(r.get("aktivasiManual"))) {
					continue;
				}
				boolean eligibleUntukSalahSatu = false;
				for (Long produkId : produkIdPerItem) {
					if (produkId != null
							&& aturanEligibleUntukItem(r, produkId, tokoId, memberId, memberJenis, memberTipe)) {
						eligibleUntukSalahSatu = true;
						break;
					}
				}
				if (!eligibleUntukSalahSatu) {
					continue;
				}
				JSONObject out = new JSONObject();
				out.put("id", r.get("id"));
				out.put("namaAturan", r.get("namaAturan"));
				out.put("keterangan", r.get("keterangan") == null ? "" : r.get("keterangan"));
				out.put("persentase", r.get("persentase"));
				out.put("nominal", r.get("nominal"));
				out.put("potonganLangsung", r.get("potonganLangsung"));
				promoArr.put(out);
			}
			hasil.put("status", "00");
			hasil.put("promo", promoArr);
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
			JSONArray hasilEvaluasi = evaluasiDiskonItems(conn, tokoId, memberId, itemsPayload, null);

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
	/**
	 * Muat kandidat {@code AturanDiskon} yang aktif, dalam jendela tanggal, cocok toko, dan cocok
	 * PRODUK ATAU berlaku semua produk ({@code produk IS NULL}) -- dipakai bersama oleh
	 * {@link #evaluasiDiskonItems} (auto-apply/apply-manual) dan {@link #diskonManualList} (listing
	 * promo manual yang eligible), supaya query+row-mapping tidak dobel.
	 */
	private static java.util.List<java.util.Map<String, Object>> loadAturanDiskonKandidat(java.sql.Connection conn,
			Long tokoId, java.util.Set<Long> produkIdSet) throws Exception {
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
						+ "COALESCE(berlaku_per_hari_dan_per_toko,false), hari_aktif, COALESCE(aktivasi_manual,false), "
						+ "nama_aturan, keterangan,COALESCE(prioritas,100),COALESCE(dapat_digabung,false),"
						+ "COALESCE(dasar_perhitungan,'SETELAH_DISKON'),COALESCE(grup_eksklusif,'') FROM koperasi.aturan_diskon "
						+ "WHERE aktif = true AND (produk IS NULL OR produk IN (" + inKlausa + ")) "
						+ "AND (toko IS NULL OR toko = ?) "
						+ "AND (tanggal_mulai IS NULL OR tanggal_mulai <= now()) "
						+ "AND (tanggal_selesai IS NULL OR tanggal_selesai >= now()) ORDER BY id ASC");
		psRule.setLong(1, tokoId);
		java.sql.ResultSet rsRule = psRule.executeQuery();
		while (rsRule.next()) {
			java.util.Map<String, Object> r = new java.util.HashMap<String, Object>();
			r.put("id", rsRule.getLong(1));
			long produkRaw = rsRule.getLong(2);
			r.put("produk", rsRule.wasNull() ? null : Long.valueOf(produkRaw));
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
			r.put("hariAktif", rsRule.getString(12));
			r.put("aktivasiManual", rsRule.getBoolean(13));
			r.put("namaAturan", rsRule.getString(14));
			r.put("keterangan", rsRule.getString(15));
			r.put("prioritas", Integer.valueOf(rsRule.getInt(16)));
			r.put("dapatDigabung", Boolean.valueOf(rsRule.getBoolean(17)));
			r.put("dasarPerhitungan", rsRule.getString(18));
			r.put("grupEksklusif", rsRule.getString(19));
			r.put("terpakaiHariIni", Double.valueOf(0d));
			r.put("terpakaiDiKeranjang", Double.valueOf(0d));
			rules.add(r);
		}
		rsRule.close();
		psRule.close();

		// Grup Diskon memakai mesin hitung yang SAMA dengan aturan per-produk. Kandidat
		// grup diletakkan di depan agar aturan massal yang sengaja dibuat admin langsung
		// terlihat di seluruh kanal tanpa perlu menduplikasi logika di tiap klien.
		java.util.List<java.util.Map<String, Object>> groupRules = new java.util.ArrayList<java.util.Map<String, Object>>();
		java.sql.PreparedStatement psGroup = conn.prepareStatement(
				"SELECT g.id,d.produk,g.toko,NOT COALESCE(g.khusus_member,false), "
						+ "g.jenis_anggota,g.tipe_anggota,g.persentase,g.maksimal_potongan,g.nominal,COALESCE(g.potongan_langsung,true), "
						+ "g.hari_aktif,g.nama_grup,g.keterangan,COALESCE(g.khusus_member,false),COALESCE(g.jenis_member_json,'[]'), "
						+ "COALESCE(g.tipe_member_json,'[]'),COALESCE(g.cashback,0),COALESCE(g.prioritas,100),"
						+ "COALESCE(g.dapat_digabung,false),COALESCE(g.dasar_perhitungan,'SETELAH_DISKON'),COALESCE(g.grup_eksklusif,'') "
						+ "FROM koperasi.grup_aturan_diskon g JOIN koperasi.grup_aturan_diskon_detail d ON d.grup_aturan_diskon=g.id AND COALESCE(d.aktif,true) "
						+ "WHERE COALESCE(g.aktif,true) AND d.produk IN (" + inKlausa + ") AND (g.toko IS NULL OR g.toko=?) "
						+ "AND (g.tanggal_mulai IS NULL OR g.tanggal_mulai<=now()) AND (g.tanggal_selesai IS NULL OR g.tanggal_selesai>=now()) ORDER BY g.id ASC");
		psGroup.setLong(1, tokoId);
		java.sql.ResultSet rsGroup = psGroup.executeQuery();
		while (rsGroup.next()) {
			java.util.Map<String,Object> r = new java.util.HashMap<String,Object>();
			r.put("id", rsGroup.getLong(1)); r.put("produk", Long.valueOf(rsGroup.getLong(2)));
			long tg=rsGroup.getLong(3); r.put("toko",rsGroup.wasNull()?null:Long.valueOf(tg));
			r.put("berlakuSemuaMember",rsGroup.getBoolean(4));
			long ja=rsGroup.getLong(5); r.put("jenisAnggota",rsGroup.wasNull()?null:Long.valueOf(ja));
			long ta=rsGroup.getLong(6); r.put("tipeAnggota",rsGroup.wasNull()?null:Long.valueOf(ta));
			r.put("persentase",Double.valueOf(rsGroup.getDouble(7))); r.put("maksimalPotongan",Double.valueOf(rsGroup.getDouble(8)));
			r.put("nominal",Double.valueOf(rsGroup.getDouble(9))); r.put("potonganLangsung",Boolean.valueOf(rsGroup.getBoolean(10)));
			r.put("berlakuPerHariDanPerToko",Boolean.FALSE); r.put("hariAktif",rsGroup.getString(11)); r.put("aktivasiManual",Boolean.FALSE);
			r.put("namaAturan",rsGroup.getString(12)); r.put("keterangan",rsGroup.getString(13)); r.put("khususMember",Boolean.valueOf(rsGroup.getBoolean(14)));
			r.put("jenisMemberJson",rsGroup.getString(15)); r.put("tipeMemberJson",rsGroup.getString(16)); r.put("cashbackTetap",Double.valueOf(rsGroup.getDouble(17)));
			r.put("prioritas",Integer.valueOf(rsGroup.getInt(18))); r.put("dapatDigabung",Boolean.valueOf(rsGroup.getBoolean(19)));
			r.put("dasarPerhitungan",rsGroup.getString(20)); r.put("grupEksklusif",rsGroup.getString(21));
			r.put("sumberGrup",Boolean.TRUE); r.put("terpakaiHariIni",Double.valueOf(0d)); r.put("terpakaiDiKeranjang",Double.valueOf(0d));
			groupRules.add(r);
		}
		rsGroup.close(); psGroup.close(); rules.addAll(groupRules);
		java.util.Collections.sort(rules, new java.util.Comparator<java.util.Map<String,Object>>() {
			public int compare(java.util.Map<String,Object> a, java.util.Map<String,Object> b) {
				int pa=((Integer)a.get("prioritas")).intValue(), pb=((Integer)b.get("prioritas")).intValue();
				if(pa!=pb) return pa>pb?-1:1;
				boolean sa=a.get("produk")!=null, sb=b.get("produk")!=null;
				if(sa!=sb) return sa?-1:1;
				double da=((Double)a.get("persentase")).doubleValue(), db=((Double)b.get("persentase")).doubleValue();
				if(da!=db) return da>db?-1:1;
				double na=((Double)a.get("nominal")).doubleValue(), nb=((Double)b.get("nominal")).doubleValue();
				if(na!=nb) return na>nb?-1:1;
				return ((Long)a.get("id")).compareTo((Long)b.get("id"));
			}
		});
		return rules;
	}

	private static double nilaiPotensialAturan(java.util.Map<String,Object> r,double dasar,double jumlah) {
		double persen=((Double)r.get("persentase")).doubleValue();
		double nominal=((Double)r.get("nominal")).doubleValue();
		double nilai=persen>0?dasar*(persen/100d):Math.min(dasar,nominal*jumlah);
		double maks=((Double)r.get("maksimalPotongan")).doubleValue();
		return maks>0?Math.min(nilai,maks):nilai;
	}

	/**
	 * Cek kelayakan SATU aturan (produk/toko/hari/member) untuk satu baris item -- TIDAK termasuk
	 * cek {@code aktivasiManual} (itu keputusan si PEMANGGIL: auto-apply skip yang manual, picker
	 * manual justru HANYA ambil yang manual). Dipakai bersama oleh jalur auto-apply, jalur
	 * apply-satu-aturan-terpilih, dan {@link #diskonManualList}.
	 */
	private static boolean aturanEligibleUntukItem(java.util.Map<String, Object> r, Long produkId, Long tokoId,
			Long memberId, Long memberJenis, Long memberTipe) {
		Object ruleProduk = r.get("produk");
		if (ruleProduk != null && !ruleProduk.equals(produkId)) {
			return false;
		}
		Long tokoRule = (Long) r.get("toko");
		if (tokoRule != null && !tokoRule.equals(tokoId)) {
			return false;
		}
		if (!ais.common.HariAktifUtil.aktifPadaHari((String) r.get("hariAktif"), new java.util.Date())) {
			return false;
		}
		boolean semuaMember = Boolean.TRUE.equals(r.get("berlakuSemuaMember"));
		boolean khususMember = Boolean.TRUE.equals(r.get("khususMember"));
		if (!semuaMember || khususMember) {
			if (memberId == null) {
				return false;
			}
			if (!jsonIdMemuat((String) r.get("jenisMemberJson"), memberJenis)
					|| !jsonIdMemuat((String) r.get("tipeMemberJson"), memberTipe)) {
				return false;
			}
			Long jenisRule = (Long) r.get("jenisAnggota");
			if (jenisRule != null && !jenisRule.equals(memberJenis)) {
				return false;
			}
			Long tipeRule = (Long) r.get("tipeAnggota");
			if (tipeRule != null && !tipeRule.equals(memberTipe)) {
				return false;
			}
		}
		return true;
	}

	private static boolean jsonIdMemuat(String json, Long nilai) {
		if (json == null || json.trim().isEmpty() || "[]".equals(json.trim())) return true;
		if (nilai == null) return false;
		try {
			JSONArray a=new JSONArray(json);
			for(int i=0;i<a.length();i++) if(String.valueOf(nilai).equals(String.valueOf(a.get(i)))) return true;
			return false;
		} catch(Exception e) { return false; }
	}

	/**
	 * @param hanyaAturanIdDefault nilai default bila item TIDAK menyertakan {@code hanya_aturan_id}
	 *                      sendiri (lihat javadoc per-item di bawah) -- biasanya {@code null} (auto-apply
	 *                      utk seluruh keranjang, dipakai {@link #diskonEvaluasi} apa adanya).
	 *                      <p>Per-item {@code hanya_aturan_id} SENGAJA didukung (bukan cuma satu nilai
	 *                      global) supaya SATU keranjang boleh berisi CAMPURAN baris auto-apply dan
	 *                      baris promo-manual-terpilih dalam SATU panggilan (mis. kasir sudah pilih
	 *                      promo manual utk sebagian baris, baris lain tetap auto) -- tanpa ini,
	 *                      Electron/Flutter terpaksa memanggil aksi ini berkali-kali lalu
	 *                      menggabung hasil secara manual di klien, rawan drift. Per baris: {@code null}
	 *                      (atau field tak ada) = jalur normal/auto-apply, aturan {@code
	 *                      aktivasiManual=true} DIKECUALIKAN, ambil aturan pertama yang eligible. Bila
	 *                      DIISI (kasir sudah pilih 1 promo manual lewat picker, lihat
	 *                      {@link #diskonManualList}): HANYA hitung aturan dgn id itu (terlepas dari
	 *                      flag {@code aktivasiManual}-nya), pakai mesin hitung yang SAMA PERSIS
	 *                      (persentase/nominal/batas maksimal/kumulatif per-hari).
	 */
	private static JSONArray evaluasiDiskonItems(java.sql.Connection conn, Long tokoId, Long memberId,
			JSONArray items, Long hanyaAturanIdDefault) throws Exception {
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

			java.util.List<java.util.Map<String, Object>> rules = loadAturanDiskonKandidat(conn, tokoId, produkIdSet);

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
				final double harga = it.optDouble("harga", 0);
				final double jumlah = it.optDouble("jumlah", 0);
				final double itemTotal = harga * jumlah;
				Long hanyaAturanIdItem = it.isNull("hanya_aturan_id")
						? hanyaAturanIdDefault
						: Long.valueOf((it.get("hanya_aturan_id") + "").trim());

				java.util.List<java.util.Map<String,Object>> eligible = new java.util.ArrayList<java.util.Map<String,Object>>();
				for (java.util.Map<String, Object> r : rules) {
					if (hanyaAturanIdItem != null) {
						if (Boolean.TRUE.equals(r.get("sumberGrup")) || !hanyaAturanIdItem.equals(r.get("id"))) {
							continue;
						}
					} else if (Boolean.TRUE.equals(r.get("aktivasiManual"))) {
						continue; // dikecualikan dari auto-apply -- hanya aktif lewat picker manual
					}
					if (produkId == null || !aturanEligibleUntukItem(r, produkId, tokoId, memberId, memberJenis,
							memberTipe)) {
						continue;
					}
					eligible.add(r);
				}
				java.util.Collections.sort(eligible,new java.util.Comparator<java.util.Map<String,Object>>(){
					public int compare(java.util.Map<String,Object> a,java.util.Map<String,Object> b){
						int pa=((Integer)a.get("prioritas")).intValue(),pb=((Integer)b.get("prioritas")).intValue();
						if(pa!=pb)return pa>pb?-1:1;
						double va=nilaiPotensialAturan(a,itemTotal,jumlah),vb=nilaiPotensialAturan(b,itemTotal,jumlah);
						if(va!=vb)return va>vb?-1:1;
						return ((Long)a.get("id")).compareTo((Long)b.get("id"));
					}
				});

				double diskon = 0;
				double cashback = 0;
				Long aturanDiskonId = null;
				Long grupAturanDiskonId = null;
				boolean berlakuPerHari = false;
				java.util.List<String> namaPromoDiterapkan=new java.util.ArrayList<String>();
				java.util.Set<String> grupEksklusifTerpakai=new java.util.HashSet<String>();
				java.util.Map<String,Object> pertama=eligible.isEmpty()?null:eligible.get(0);
				for(int ri=0;ri<eligible.size();ri++) {
					java.util.Map<String,Object> applied=eligible.get(ri);
					if(ri>0 && (hanyaAturanIdItem!=null || !Boolean.TRUE.equals(pertama.get("dapatDigabung"))
							|| !Boolean.TRUE.equals(applied.get("dapatDigabung")))) break;
					String eks=String.valueOf(applied.get("grupEksklusif")==null?"":applied.get("grupEksklusif")).trim();
					if(!eks.isEmpty() && grupEksklusifTerpakai.contains(eks)) continue;
					if(!eks.isEmpty()) grupEksklusifTerpakai.add(eks);
					double dasar="HARGA_AWAL".equals(applied.get("dasarPerhitungan"))?itemTotal:Math.max(0,itemTotal-diskon);
					double persen = ((Double) applied.get("persentase")).doubleValue();
					double nominal = ((Double) applied.get("nominal")).doubleValue();
					double maksimalPotongan = ((Double) applied.get("maksimalPotongan")).doubleValue();
					double discountValue = 0;
					if (persen > 0) {
						discountValue = dasar * (persen / 100);
					} else if (nominal > 0) {
						discountValue = nominal * jumlah;
						if (discountValue > dasar) {
							discountValue = dasar;
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
						diskon += Math.min(Math.max(0,itemTotal-diskon),discountValue);
					} else {
						cashback += discountValue;
					}
					// Grup dapat memberi potongan dan cashback sekaligus. Cashback eksplisit
					// dihitung per unit dan tidak mengurangi total tagihan.
					double cashbackTetap = applied.get("cashbackTetap") instanceof Double
							? ((Double) applied.get("cashbackTetap")).doubleValue() : 0d;
					if (cashbackTetap > 0) cashback += Math.min(itemTotal, cashbackTetap * jumlah);
					// FK pembelian.aturan_diskon menunjuk aturan lama, bukan header grup.
					// Nilai finansial tetap tersimpan di diskon/cashback; id grup dikirim terpisah.
					if(Boolean.TRUE.equals(applied.get("sumberGrup"))) {
						if(grupAturanDiskonId==null) grupAturanDiskonId=(Long)applied.get("id");
					} else if(aturanDiskonId==null) aturanDiskonId=(Long)applied.get("id");
					namaPromoDiterapkan.add(String.valueOf(applied.get("namaAturan")));
				}
				// Total manfaat tidak boleh melebihi nilai barang: diskon 100% tidak
				// boleh masih menghasilkan cashback tambahan.
				cashback=Math.min(Math.max(0,itemTotal-diskon),cashback);

				JSONObject out = new JSONObject();
				out.put("id", produkId);
				out.put("diskon", diskon);
				out.put("cashback", cashback);
				out.put("aturanDiskon", aturanDiskonId == null ? JSONObject.NULL : aturanDiskonId);
				out.put("grupAturanDiskon", grupAturanDiskonId == null ? JSONObject.NULL : grupAturanDiskonId);
				StringBuilder namaPromoGabung=new StringBuilder();
				JSONArray promoJson=new JSONArray();
				for(String nama:namaPromoDiterapkan){if(namaPromoGabung.length()>0)namaPromoGabung.append(" + ");namaPromoGabung.append(nama);promoJson.put(nama);}
				out.put("namaPromo", namaPromoGabung.toString());
				out.put("promoDiterapkan", promoJson);
				out.put("berlakuPerHariDanPerToko", berlakuPerHari);
				outArr.put(out);
			}

			return outArr;
		}
	}

	private static void terapkanEvaluasiDiskonServer(java.sql.Connection conn, Long tokoId, Long memberId,
			JSONArray transaksi) throws Exception {
		JSONArray input=new JSONArray();
		for(int i=0;i<transaksi.length();i++){
			JSONObject asal=transaksi.getJSONObject(i), item=new JSONObject();
			item.put("id",asal.get("id")); item.put("harga",asal.optDouble("harga",0));
			item.put("jumlah",asal.optDouble("jumlah",1));
			if(!asal.isNull("aturanDiskon")) item.put("hanya_aturan_id",asal.get("aturanDiskon"));
			input.put(item);
		}
		JSONArray hasil=evaluasiDiskonItems(conn,tokoId,memberId,input,null);
		for(int i=0;i<transaksi.length()&&i<hasil.length();i++){
			JSONObject asal=transaksi.getJSONObject(i), hitung=hasil.getJSONObject(i);
			if (asal.optBoolean("diskon_bebas", false)) {
				double harga = asal.optDouble("harga", 0), jumlah = asal.optDouble("jumlah", 1);
				double nilaiBaris = harga * jumlah;
				String tipe = asal.optString("diskon_bebas_tipe", "NOMINAL").trim().toUpperCase();
				double nilai = asal.optDouble("diskon_bebas_nilai", -1);
				if (harga < 0 || jumlah <= 0 || Double.isNaN(nilai) || Double.isInfinite(nilai) || nilai < 0
						|| ("PERSEN".equals(tipe) && nilai > 100) || (!"PERSEN".equals(tipe) && nilai > nilaiBaris)) throw new IllegalStateException("Diskon bebas pada item ke-" + (i + 1) + " tidak valid atau melebihi nilai barang.");
				double diskonBebas = "PERSEN".equals(tipe) ? nilaiBaris * nilai / 100.0 : nilai;
				asal.put("diskon", Math.min(nilaiBaris, Math.max(0, diskonBebas)));
				asal.put("cashback", 0); asal.put("aturanDiskon", JSONObject.NULL);
				asal.put("grupAturanDiskon", JSONObject.NULL); asal.put("namaPromo", "Diskon Bebas");
				continue;
			}
			asal.put("diskon",hitung.optDouble("diskon",0));
			asal.put("cashback",hitung.optDouble("cashback",0));
			asal.put("aturanDiskon",hitung.isNull("aturanDiskon")?JSONObject.NULL:hitung.get("aturanDiskon"));
			asal.put("grupAturanDiskon",hitung.isNull("grupAturanDiskon")?JSONObject.NULL:hitung.get("grupAturanDiskon"));
			asal.put("namaPromo",hitung.optString("namaPromo",""));
		}
	}
}
