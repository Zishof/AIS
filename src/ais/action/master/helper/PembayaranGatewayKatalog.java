package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.MyDoublebox;

/**
 * <h2>Katalog terpusat saluran pembayaran (payment gateway) beserta aturan aktivasinya.</h2>
 *
 * <p>
 * Class ini merupakan <b>single source of truth</b> untuk seluruh saluran pembayaran yang
 * dikenal aplikasi — baik pembayaran daring melalui gerbang pihak ketiga (IPaymu, Faspay,
 * Finpay, Doku, CIMB, Jatelindo), Virtual Account bank langsung (BNI, BSI, BRI), keluarga
 * "Bank Online" berbasis {@code DownloadTagihanMahasiswaBankOnline} (Online, Online&nbsp;2,
 * Smartlink, Maja, QRIS, Finpay-Bank, Flip, Otto, BRIVA), bank daerah dengan class VA khusus
 * (Bank NTT, BTN, BJB, Bankaltimtara), maupun pembayaran tunai/manual di kasir.
 * </p>
 *
 * <h3>Mengapa class ini dibuat?</h3>
 * <p>
 * Sebelum class ini ada, definisi saluran pembayaran (kunci konfigurasi on/off, label tombol,
 * token filter per jenis kegiatan, kunci konfigurasi host bank dan biaya administrasi)
 * <i>diduplikasi</i> di minimal tiga tempat: {@code DaftarUlangMahasiswaBaruAction.menuBayar()},
 * {@code DaftarUlangMahasiswaLamaAction.menuBayar()}, dan array {@code gatewayData} di halaman
 * JSP {@code /WEB-INF/baru/modul/bayarmhs/_lanjut_bayar.jsp}. Duplikasi seperti itu rawan
 * <i>drift</i>: ketika administrator menonaktifkan sebuah gateway lewat konfigurasi, tombolnya
 * bisa saja masih tampil di salah satu antarmuka karena kunci konfigurasi yang dicek berbeda
 * ejaan atau nilai default-nya tidak seragam. Dengan memusatkan definisi di sini, antarmuka
 * ZK ({@link WizardPembayaranMhsHelper}, DaftarUlang*Action) dan antarmuka JSP
 * ({@code _lanjut_bayar.jsp}) membaca aturan yang <b>persis sama</b>, sehingga konfigurasi
 * on/off dijamin konsisten di semua tampilan — desktop, mobile, ZK, maupun JSP.
 * </p>
 *
 * <h3>Aturan aktivasi (mengikuti persis perilaku DaftarUlangMahasiswa*Action)</h3>
 * <ol>
 *   <li><b>Konfigurasi global</b> — gateway aktif bila nilai konfigurasi pada
 *       {@link Gateway#configKey} sama dengan {@link Konfigurasi#AKTIF}; bila konfigurasi
 *       belum pernah dibuat, dianggap {@link Konfigurasi#TIDAK_AKTIF} (default mati).</li>
 *   <li><b>Konfigurasi per Perguruan Tinggi</b> — untuk gateway dengan
 *       {@link Gateway#cekPtSpesifik} bernilai {@code true}, dicek kunci tambahan
 *       {@code configKey + "_pt_" + idPerguruanTinggi} dengan default {@code AKTIF}
 *       (artinya: bila konfigurasi global aktif dan konfigurasi PT belum dibuat,
 *       tombol tetap tampil — persis perilaku
 *       {@code DaftarUlangMahasiswaLamaAction.setupPaymentGateway()}).</li>
 *   <li><b>Filter per Jenis Kegiatan</b> — field {@code JenisKegiatan.namaBankPembayaran}
 *       dapat membatasi saluran yang boleh dipakai untuk jenis kegiatan tertentu. Bila field
 *       tersebut kosong, semua saluran lolos; bila terisi, hanya saluran yang token-nya
 *       ({@link Gateway#tokenJenisKegiatan}) tercantum dalam format {@code ";token;"} yang
 *       lolos.</li>
 *   <li><b>Kapabilitas antarmuka</b> — tidak semua antarmuka mampu mengeksekusi semua
 *       gateway (mis. checkout JSP belum mem-porting alur legacy Doku/IPaymu). Set
 *       {@link #KAPABILITAS_WIZARD_ZK} dan {@link #KAPABILITAS_CHECKOUT_JSP} mendeklarasikan
 *       kemampuan masing-masing antarmuka; {@link #yangTampil(JenisKegiatan, Collection)}
 *       memfilter katalog dengan ketiga aturan di atas <i>plus</i> kapabilitas ini.</li>
 * </ol>
 *
 * <h3>Cara pakai</h3>
 * <pre>
 * // Dari wizard ZK:
 * for (PembayaranGatewayKatalog.Gateway g :
 *         PembayaranGatewayKatalog.yangTampil(jenisKegiatan, PembayaranGatewayKatalog.KAPABILITAS_WIZARD_ZK)) {
 *     buatTombol(g.label, g.id);
 * }
 *
 * // Dari JSP (scriptlet):
 * List&lt;PembayaranGatewayKatalog.Gateway&gt; gws =
 *         PembayaranGatewayKatalog.yangTampil(jk, PembayaranGatewayKatalog.KAPABILITAS_CHECKOUT_JSP);
 * </pre>
 *
 * <h3>Menambah gateway baru</h3>
 * <p>
 * Cukup tambahkan satu entri {@code new Gateway(...)} pada blok inisialisasi {@link #SEMUA},
 * lalu tambahkan id-nya ke set kapabilitas antarmuka yang benar-benar mampu mengeksekusinya.
 * Tidak ada file lain yang perlu diubah untuk urusan tampil/tidaknya tombol.
 * </p>
 *
 * <p>
 * <b>Catatan kompatibilitas:</b> ditulis untuk Java&nbsp;7 (tanpa lambda/stream) dan ZK&nbsp;5 CE.
 * Class ini <i>stateless</i> — seluruh method static dan aman dipanggil dari thread event ZK
 * maupun request JSP.
 * </p>
 *
 * @see WizardPembayaranMhsHelper wizard pembayaran ZK yang mengonsumsi katalog ini
 * @see ais.action.master.DaftarUlangMahasiswaLamaAction referensi perilaku aktivasi
 */
public class PembayaranGatewayKatalog {

	/** Kategori: gateway dipanggil langsung via {@code XxxCommon.onSaveXxx} (Doku, IPaymu, dst). */
	public static final String KATEGORI_LANGSUNG = "LANGSUNG";
	/** Kategori: keluarga "Bank Online" via {@code DownloadTagihanMahasiswaBankOnline}. */
	public static final String KATEGORI_BANK_ONLINE = "BANK_ONLINE";
	/** Kategori: bank daerah dengan class {@code Download*} tersendiri (NTT, BTN, BJB, Bankaltimtara). */
	public static final String KATEGORI_BANK_KHUSUS = "BANK_KHUSUS";

	/**
	 * Deskriptor satu saluran pembayaran. Immutable — seluruh field final dan publik agar
	 * ringkas dibaca dari JSP scriptlet maupun kode Java biasa.
	 */
	public static class Gateway {
		/** Identitas unik internal (dipakai sebagai kunci dispatch di UI). */
		public final String id;
		/** Label tombol yang dilihat pengguna — sama persis dengan DaftarUlang*Action. */
		public final String label;
		/** Kunci konfigurasi on/off global, mis. {@code aktifkan_pembayaran_via_ipaymu}. */
		public final String configKey;
		/** Bila true, dicek juga kunci {@code configKey_pt_<idPT>} (default AKTIF). */
		public final boolean cekPtSpesifik;
		/** Token pada {@code JenisKegiatan.namaBankPembayaran}, dicocokkan sebagai ";token;". */
		public final String tokenJenisKegiatan;
		/** Salah satu {@link #KATEGORI_LANGSUNG}/{@link #KATEGORI_BANK_ONLINE}/{@link #KATEGORI_BANK_KHUSUS}. */
		public final String kategori;
		/** Kunci konfigurasi host bank (hanya kategori BANK_ONLINE), boleh null. */
		public final String bankHostConfig;
		/** Kunci konfigurasi biaya administrasi (hanya kategori BANK_ONLINE), boleh null. */
		public final String adminFeeConfig;
		/** Kunci konfigurasi prefix kode bank lain untuk tampilan VA, boleh null. */
		public final String prefixKodeLainConfig;
		/** Class ikon Font Awesome untuk antarmuka JSP/HTML, mis. {@code fas fa-university}. */
		public final String ikon;

		Gateway(String id, String label, String configKey, boolean cekPtSpesifik, String tokenJenisKegiatan,
				String kategori, String bankHostConfig, String adminFeeConfig, String prefixKodeLainConfig,
				String ikon) {
			this.id = id;
			this.label = label;
			this.configKey = configKey;
			this.cekPtSpesifik = cekPtSpesifik;
			this.tokenJenisKegiatan = tokenJenisKegiatan;
			this.kategori = kategori;
			this.bankHostConfig = bankHostConfig;
			this.adminFeeConfig = adminFeeConfig;
			this.prefixKodeLainConfig = prefixKodeLainConfig;
			this.ikon = ikon;
		}
	}

	/**
	 * Katalog lengkap, berurutan sama dengan urutan tombol pada
	 * {@code DaftarUlangMahasiswaLamaAction.menuBayar()} agar pengalaman pengguna konsisten
	 * antar tampilan.
	 */
	private static final List<Gateway> SEMUA = new ArrayList<Gateway>();
	static {
		// --- Gerbang langsung (XxxCommon.onSaveXxx) -------------------------------------
		SEMUA.add(new Gateway("doku", "BAYAR VIA DOKU", "aktifkan_pembayaran_via_doku", false, "doku",
				KATEGORI_LANGSUNG, null, null, null, "far fa-credit-card"));
		SEMUA.add(new Gateway("ipaymu", "BAYAR VIA IPaymu", "aktifkan_pembayaran_via_ipaymu", false, "ipaymu",
				KATEGORI_LANGSUNG, null, null, null, "fas fa-wallet"));
		SEMUA.add(new Gateway("faspay", "BAYAR VIA FASPAY", "aktifkan_pembayaran_via_faspay", false, "faspay",
				KATEGORI_LANGSUNG, null, null, null, "far fa-credit-card"));
		SEMUA.add(new Gateway("jatelindo", "BAYAR VIA JATELINDO", "aktifkan_pembayaran_via_jatelindo", true,
				"jatelindo", KATEGORI_LANGSUNG, null, null, null, "fas fa-university"));
		SEMUA.add(new Gateway("cimb", "BAYAR VIA CIMB", "aktifkan_pembayaran_via_cimb", false, "cimb",
				KATEGORI_LANGSUNG, null, null, null, "fas fa-university"));
		SEMUA.add(new Gateway("bni", "BAYAR VIA BNI", "aktifkan_pembayaran_via_bni", true, "bni",
				KATEGORI_LANGSUNG, null, null, null, "fas fa-university"));
		// Token bsi_lama mengikuti filter jenis kegiatan pada DaftarUlang (BSI e-collection lama).
		SEMUA.add(new Gateway("bsi", "BAYAR VIA BSI", "aktifkan_pembayaran_via_bsi", true, "bsi_lama",
				KATEGORI_LANGSUNG, null, null, null, "fas fa-university"));
		SEMUA.add(new Gateway("bri", "BAYAR VIA BRI", "aktifkan_pembayaran_via_bri", true, "bri",
				KATEGORI_LANGSUNG, null, null, null, "fas fa-money-check-alt"));
		SEMUA.add(new Gateway("finpay", "BAYAR VIA FINPAY", "aktifkan_pembayaran_via_finpay", false, "finpay",
				KATEGORI_LANGSUNG, null, null, null, "fas fa-money-bill-wave"));

		// --- Bank daerah / class Download khusus ----------------------------------------
		SEMUA.add(new Gateway("ntt", "BAYAR VIA BANK NTT", "aktifkan_pembayaran_via_bank_ntt", false, "ntt",
				KATEGORI_BANK_KHUSUS, null, null, null, "fas fa-university"));
		SEMUA.add(new Gateway("btn", "BAYAR VIA BANK BTN", "aktifkan_pembayaran_via_bank_btn", true, "btn",
				KATEGORI_BANK_KHUSUS, null, null, null, "fas fa-university"));
		SEMUA.add(new Gateway("bjb", "BAYAR VIA BANK BJB", "aktifkan_pembayaran_via_bank_bjb", false, "bjb",
				KATEGORI_BANK_KHUSUS, null, null, null, "fas fa-university"));
		SEMUA.add(new Gateway("bankaltimtara", "BAYAR VIA Bankaltimtara",
				"aktifkan_pembayaran_via_bank_bankaltimtara", false, "bankaltimtara",
				KATEGORI_BANK_KHUSUS, null, "bankaltimtara_biaya_administrasi", null, "fas fa-university"));

		// --- Keluarga "Bank Online" (DownloadTagihanMahasiswaBankOnline) ----------------
		SEMUA.add(new Gateway("online", "BAYAR ONLINE", "aktifkan_pembayaran_via_bank_online", true, "online",
				KATEGORI_BANK_ONLINE, "online_bank_host_ip", "online_biaya_administrasi",
				"prefix_kode_bank_lain_online", "fas fa-globe"));
		SEMUA.add(new Gateway("online_2", "BAYAR ONLINE 2", "aktifkan_pembayaran_via_bank_online_2", true,
				"online_2", KATEGORI_BANK_ONLINE, "online_2_bank_host_ip", "online_biaya_administrasi_2",
				"prefix_kode_bank_lain_online_2", "fas fa-globe"));
		SEMUA.add(new Gateway("smartlink", "BAYAR VIA ONLINE", "aktifkan_pembayaran_via_bank_online_smartlink",
				true, "smartlink", KATEGORI_BANK_ONLINE, "online_bank_host_ip",
				"online_smartlink_biaya_administrasi", "prefix_kode_bank_lain_online", "fas fa-link"));
		SEMUA.add(new Gateway("maja", "BAYAR VIA BSI", "aktifkan_pembayaran_via_bank_maja", true, "maja",
				KATEGORI_BANK_ONLINE, "maja_bank_host_ip", "maja_biaya_administrasi",
				"prefix_kode_bank_lain_maja", "fas fa-university"));
		SEMUA.add(new Gateway("qris", "BAYAR QRIS", "aktifkan_pembayaran_via_bank_qris", false, "qris",
				KATEGORI_BANK_ONLINE, "qris_bank_host_ip", "qris_biaya_administrasi", null, "fas fa-qrcode"));
		SEMUA.add(new Gateway("bank_finpay", "BAYAR FINPAY", "aktifkan_pembayaran_via_bank_finpay", false,
				"finpay", KATEGORI_BANK_ONLINE, "finpay_bank_host_ip", "finpay_biaya_administrasi", null,
				"fas fa-money-bill-wave"));
		SEMUA.add(new Gateway("flip", "BAYAR VIA FLIP", "aktifkan_pembayaran_via_bank_flip", false, "flip",
				KATEGORI_BANK_ONLINE, "flip_bank_host_ip", "flip_biaya_administrasi", null,
				"fas fa-exchange-alt"));
		SEMUA.add(new Gateway("otto", "BAYAR OTTO", "aktifkan_pembayaran_via_bank_otto", false, "otto",
				KATEGORI_BANK_ONLINE, "otto_bank_host_ip", "otto_biaya_administrasi", null,
				"fas fa-mobile-alt"));
		SEMUA.add(new Gateway("briva", "BAYAR VIA BRIVA", "aktifkan_pembayaran_via_bank_briva", false, "briva",
				KATEGORI_BANK_ONLINE, "briva_bank_host_ip", "briva_biaya_administrasi", null,
				"fas fa-money-check-alt"));
	}

	/**
	 * Gateway yang mampu dieksekusi oleh wizard ZK {@link WizardPembayaranMhsHelper}:
	 * seluruh kategori LANGSUNG (via {@code populate*DariDetailBiaya} + {@code onSave*}),
	 * seluruh keluarga BANK_ONLINE (via {@code DownloadTagihanMahasiswaBankOnline}
	 * dengan grid cicilan tiruan dari {@link #buatGridCicilanMock}), serta seluruh
	 * bank khusus/daerah paket {@code ais.action.master.helper.virtualaccount}:
	 * BTN/NTT/BJB ({@code DownloadTagihanMahasiswaBankBtn|Ntt|Bjb.downloadData}) dan
	 * Bankaltimtara ({@code DownloadTagihanMahasiswaBankBankaltimtara.downloadData}
	 * dengan pilihan metode Virtual Account / QRIS + biaya administrasi konfigurasi
	 * {@code bankaltimtara_biaya_administrasi}).
	 */
	public static final Set<String> KAPABILITAS_WIZARD_ZK = new LinkedHashSet<String>(Arrays.asList(
			"doku", "ipaymu", "faspay", "jatelindo", "cimb", "bni", "bsi", "bri", "finpay",
			"online", "online_2", "smartlink", "maja", "qris", "bank_finpay", "flip", "otto", "briva",
			"btn", "ntt", "bjb", "bankaltimtara"));

	/**
	 * Gateway yang mampu dieksekusi checkout JSP ({@code _lanjut_bayar_services.jsp}).
	 * Alur legacy Doku/IPaymu/Faspay/Jatelindo/CIMB/BNI/BRI belum diporting ke checkout
	 * JSP; menampilkan tombolnya justru berbahaya (riwayat bug: VA terbit di bank yang
	 * salah), sehingga sengaja tidak masuk set ini.
	 */
	public static final Set<String> KAPABILITAS_CHECKOUT_JSP = new LinkedHashSet<String>(Arrays.asList(
			"online", "online_2", "smartlink", "maja", "qris", "bank_finpay", "flip", "otto", "briva",
			"bankaltimtara", "bsi"));

	/** @return salinan daftar seluruh gateway yang dikenal, berurutan tampilan. */
	public static List<Gateway> semua() {
		return new ArrayList<Gateway>(SEMUA);
	}

	/** @return deskriptor gateway ber-id tersebut, atau {@code null} bila tidak dikenal. */
	public static Gateway cari(String id) {
		if (id == null) return null;
		for (Gateway g : SEMUA) {
			if (g.id.equals(id)) return g;
		}
		return null;
	}

	/**
	 * Cek konfigurasi on/off sebuah gateway — replikasi persis logika
	 * {@code DaftarUlangMahasiswaLamaAction.setupPaymentGateway()}: konfigurasi global
	 * default {@link Konfigurasi#TIDAK_AKTIF}; bila lolos dan gateway menuntut cek per-PT,
	 * kunci {@code configKey_pt_<idPT>} dicek dengan default {@link Konfigurasi#AKTIF}.
	 */
	public static boolean aktif(Gateway g) {
		if (g == null) return false;
		boolean isActive;
		try {
			isActive = Konfigurasi.AKTIF
					.equals(Common.getKonfigurasi(g.configKey, Konfigurasi.TIDAK_AKTIF).getNilai());
		} catch (Exception e) {
			return false;
		}
		if (isActive && g.cekPtSpesifik) {
			try {
				PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();
				if (pt != null && pt.getId() != null) {
					isActive = Konfigurasi.AKTIF.equals(Common
							.getKonfigurasi(g.configKey + "_pt_" + pt.getId(), Konfigurasi.AKTIF).getNilai());
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranGatewayKatalog.java:280");
				// gagal resolve PT → pakai hasil cek global saja (perilaku toleran DaftarUlang)
			}
		}
		return isActive;
	}

	/**
	 * Filter per Jenis Kegiatan: bila {@code namaBankPembayaran} kosong semua saluran lolos,
	 * bila terisi hanya token {@code ";token;"} yang tercantum yang lolos — replikasi pola
	 * {@code jenisKegiatan.getNamaBankPembayaran().toLowerCase().contains(";" + id + ";")}.
	 */
	public static boolean cocokJenisKegiatan(Gateway g, JenisKegiatan jk) {
		if (g == null) return false;
		if (jk == null) return true;
		String daftar;
		try {
			daftar = jk.getNamaBankPembayaran();
		} catch (Exception e) {
			return true;
		}
		if (daftar == null || daftar.trim().isEmpty()) return true;
		return daftar.toLowerCase().contains(";" + g.tokenJenisKegiatan + ";");
	}

	/** Gabungan {@link #aktif} + {@link #cocokJenisKegiatan}. */
	public static boolean tampil(Gateway g, JenisKegiatan jk) {
		return aktif(g) && cocokJenisKegiatan(g, jk);
	}

	/**
	 * Daftar gateway yang harus ditampilkan sebuah antarmuka: lolos konfigurasi, lolos filter
	 * jenis kegiatan, dan termasuk kapabilitas antarmuka pemanggil.
	 *
	 * @param jk          jenis kegiatan pembayaran aktif (boleh null → tanpa filter)
	 * @param kapabilitas id gateway yang mampu dieksekusi antarmuka pemanggil, mis.
	 *                    {@link #KAPABILITAS_WIZARD_ZK} atau {@link #KAPABILITAS_CHECKOUT_JSP}
	 */
	public static List<Gateway> yangTampil(JenisKegiatan jk, Collection<String> kapabilitas) {
		Set<String> mampu = kapabilitas == null ? null : new HashSet<String>(kapabilitas);
		List<Gateway> hasil = new ArrayList<Gateway>();
		for (Gateway g : SEMUA) {
			if (mampu != null && !mampu.contains(g.id)) continue;
			if (tampil(g, jk)) hasil.add(g);
		}
		return hasil;
	}

	/**
	 * Cek apakah pembayaran tunai/manual boleh tampil, mengikuti
	 * {@code DaftarUlangMahasiswaLamaAction} baris 5316: kunci
	 * {@code aktifkan_pembayaran_manual} dengan default {@link Konfigurasi#AKTIF},
	 * ditambah filter token {@code ;tunai;} pada jenis kegiatan.
	 */
	public static boolean tunaiAktif(JenisKegiatan jk) {
		boolean aktif;
		try {
			aktif = Konfigurasi.AKTIF
					.equals(Common.getKonfigurasi("aktifkan_pembayaran_manual", Konfigurasi.AKTIF).getNilai());
		} catch (Exception e) {
			aktif = true;
		}
		if (!aktif) return false;
		if (jk != null) {
			try {
				String daftar = jk.getNamaBankPembayaran();
				if (daftar != null && !daftar.trim().isEmpty()
						&& !daftar.toLowerCase().contains(";tunai;")) {
					return false;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranGatewayKatalog.java:350"); /* toleran */ }
		}
		return true;
	}

	/**
	 * Cek apakah admin ber-{@code userId} tersebut DILARANG membayar langsung, berdasarkan
	 * konfigurasi {@code admin_lain_yang_tidak_bisa_membayar_langsung} (daftar userId
	 * dipisah titik-koma) — replikasi gate pada DaftarUlang dan {@code _lanjut_bayar.jsp}.
	 */
	public static boolean adminDilarangBayarLangsung(String userId) {
		if (userId == null) return false;
		try {
			String admLain = Common.getKonfigurasi("admin_lain_yang_tidak_bisa_membayar_langsung", "").getNilai();
			if (admLain == null || admLain.isEmpty()) return false;
			for (String a : admLain.split(";")) {
				if (a.trim().equalsIgnoreCase(userId.trim())) return true;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PembayaranGatewayKatalog.java:368"); /* toleran */ }
		return false;
	}

	/**
	 * Membangun {@code Grid} cicilan tiruan (in-memory, tidak menempel ke component tree)
	 * dengan struktur atribut baris yang dituntut oleh class-class
	 * {@code Download*BankOnline.downloadData(...)}: {@code jumlahCicilan}
	 * ({@link MyDoublebox}), {@code cicilanPembayaran} ({@link CicilanPembayaran} ber-id
	 * null), {@code detailBiaya}, dan {@code itemBiaya} ({@link Combobox} terseleksi).
	 *
	 * <p>Pola ini identik dengan "deep mocking grid" pada
	 * {@code _lanjut_bayar_services.jsp} sehingga wizard ZK dan checkout JSP menghasilkan
	 * data VA yang sama untuk item dan nominal yang sama.</p>
	 *
	 * @param biayas   item biaya yang dibayar (urutan sejajar dengan {@code nominals})
	 * @param nominals nominal yang dibayarkan per item (angsuran/partial diperbolehkan)
	 * @return grid siap dilempar sebagai argumen {@code gridCicilan}
	 */
	public static Grid buatGridCicilanMock(List<DetailBiaya> biayas, List<Double> nominals) {
		return buatGridCicilanMock(biayas, nominals, null, null);
	}

	/**
	 * Varian lengkap {@link #buatGridCicilanMock(List, List)}: mendukung item BULANAN/
	 * angsuran ({@link ais.database.model.PengaturanPembayaranBulanan} pada indeks yang
	 * sama — menghasilkan token {@code "Bulanan-<id>-<nilai>"} alih-alih {@code "Item-..."})
	 * serta atribut {@code "tanggal"} ({@link ais.ui.util.MyDatebox}) yang dituntut
	 * {@code populate*RequestDetail(Grid, ...)} milik gateway langsung.
	 */
	public static Grid buatGridCicilanMock(List<DetailBiaya> biayas, List<Double> nominals,
			List<ais.database.model.PengaturanPembayaranBulanan> bulanans, List<java.util.Date> tanggals) {
		Grid grid = new Grid();
		Rows rows = new Rows();
		grid.appendChild(rows);
		if (biayas == null) return grid;
		for (int i = 0; i < biayas.size(); i++) {
			DetailBiaya db = biayas.get(i);
			if (db == null) continue;
			double nominal = (nominals != null && i < nominals.size() && nominals.get(i) != null)
					? nominals.get(i).doubleValue() : 0;
			if (nominal <= 0) continue;

			Row row = new Row();
			MyDoublebox dbox = new MyDoublebox();
			dbox.setValue(nominal);
			row.setAttribute("jumlahCicilan", dbox);

			CicilanPembayaran mockCp = new CicilanPembayaran();
			mockCp.setId(null);
			mockCp.setDetailBiaya(db);
			if (bulanans != null && i < bulanans.size() && bulanans.get(i) != null) {
				mockCp.setPengaturanPembayaranBulanan(bulanans.get(i));
			}
			row.setAttribute("cicilanPembayaran", mockCp);
			row.setAttribute("detailBiaya", db);

			Combobox cb = new Combobox();
			Comboitem ci = new Comboitem();
			ci.setValue(db);
			cb.appendChild(ci);
			cb.setSelectedItem(ci);
			row.setAttribute("itemBiaya", cb);

			ais.ui.util.MyDatebox tgl = new ais.ui.util.MyDatebox();
			tgl.setValue(tanggals != null && i < tanggals.size() && tanggals.get(i) != null
					? tanggals.get(i) : ais.ui.util.WaktuUtil.getDate());
			row.setAttribute("tanggal", tgl);

			rows.appendChild(row);
		}
		return grid;
	}
}
