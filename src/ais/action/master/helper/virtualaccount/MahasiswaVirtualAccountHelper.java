package ais.action.master.helper.virtualaccount;

import java.io.File;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Row;

import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.IndonesianNumberToWords;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.KegiatanTemporary;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.VirtualAccountBank;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyMessageboxConfig;

/**
 * Helper reusable untuk pembayaran mahasiswa berbasis Virtual Account.
 *
 * Tujuannya agar DaftarUlangMahasiswaLamaAction, DaftarUlangMahasiswaBaruAction,
 * dan KegiatanTemporaryAction memakai cara validasi, hitung total, dan tampilan VA
 * yang sama. Class ini sengaja Java 1.7 friendly: tanpa lambda, stream, atau API baru.
 */
public final class MahasiswaVirtualAccountHelper {

	public static final String DEFAULT_VA_WINDOW = "/common/online/no_va.zul";
	/**
	 * Jika AKTIF, sistem menolak pembuatan VA baru untuk rincian tagihan yang
	 * identik dan sudah tercatat dibayar. Nilai bawaan sengaja TIDAK AKTIF agar
	 * kampus tetap dapat menerima pembayaran bertahap/angsuran melalui VA baru.
	 * Pemeriksaan ini juga dibatasi hanya untuk instalasi yang mengaktifkan kanal
	 * Bank Kaltimtara; bank lain tidak ikut terkena aturan tersebut.
	 */
	public static final String KONFIGURASI_CEGAH_VA_TAGIHAN_SUDAH_DIBAYAR =
			"cegah_va_baru_jika_tagihan_sudah_dibayar";

	private MahasiswaVirtualAccountHelper() {
	}

	/**
	 * Tipe implementasi bersarang {@link TagihanSudahDibayarException} milik {@link
	 * MahasiswaVirtualAccountHelper}. Kelas ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya
	 * tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * MahasiswaVirtualAccountHelper}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 *
	 * @see MahasiswaVirtualAccountHelper
	 */
	public static class TagihanSudahDibayarException extends Exception {
		private static final long serialVersionUID = 1L;

		public TagihanSudahDibayarException(String message) {
			super(message);
		}
	}

	/**
	 * Mencegah penerbitan VA baru bila tagihan identik sudah pernah dibayar.
	 * Pemeriksaan tidak dibatasi bank/channel: setelah payment sah, pengguna tidak
	 * boleh membuat VA pengganti untuk item tagihan yang sama melalui kanal lain.
	 */
	@SuppressWarnings("unchecked")
	public static void pastikanTagihanBelumDibayar(Session session, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa calonMahasiswa, Integer semester, JenisKegiatan jenisKegiatan,
			JadwalPembayaran jadwalPembayaran, String keterangan, String cicilan, String detailBiaya,
			Double total) throws TagihanSudahDibayarException {
		/*
		 * Default FALSE/TIDAK AKTIF: pembayaran yang sudah pernah masuk tidak
		 * otomatis menutup kesempatan membuat VA angsuran berikutnya. Institusi
		 * yang menggunakan Bank Kaltimtara dan membutuhkan pencegahan VA ganda
		 * dapat mengaktifkan konfigurasi ini.
		 */
		if (!Common.bolehKonfigurasi(KONFIGURASI_CEGAH_VA_TAGIHAN_SUDAH_DIBAYAR,
				Konfigurasi.TIDAK_AKTIF)
				|| (!Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_bankaltimtara",
						Konfigurasi.TIDAK_AKTIF)
						&& !Common.bolehKonfigurasi("aktifkan_va_bankaltimtara_baru",
								Konfigurasi.TIDAK_AKTIF))) {
			return;
		}
		if (session == null || !session.isOpen()) {
			throw new IllegalStateException("Session database tidak tersedia untuk memeriksa status pembayaran.");
		}
		if ((mahasiswa == null || mahasiswa.getId() == null)
				&& (calonMahasiswa == null || calonMahasiswa.getId() == null)) {
			return;
		}

		Criteria criteria = session.createCriteria(VirtualAccountBank.class)
				.add(Restrictions.isNotNull("kegiatan"))
				.add(Restrictions.or(Restrictions.isNull("terjadiKendala"),
						Restrictions.eq("terjadiKendala", false)))
				.add(mahasiswa != null && mahasiswa.getId() != null
						? Restrictions.eq("mahasiswa", mahasiswa)
						: Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa))
				.add(semester == null ? Restrictions.isNull("semester") : Restrictions.eq("semester", semester))
				.add(jenisKegiatan == null ? Restrictions.isNull("jenisKegiatan")
						: Restrictions.eq("jenisKegiatan", jenisKegiatan))
				.add(jadwalPembayaran == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jadwalPembayaran", jadwalPembayaran))
				.addOrder(Order.desc("id")).setMaxResults(100);

		List<VirtualAccountBank> pembayaranSebelumnya = criteria.list();
		for (VirtualAccountBank pembayaran : pembayaranSebelumnya) {
			if (!tagihanSama(pembayaran, keterangan, cicilan, detailBiaya, total)) {
				continue;
			}
			String waktu = pembayaran.getWaktuBayar() == null ? ""
					: " pada " + Common.dateFormat3.get().format(pembayaran.getWaktuBayar());
			throw new TagihanSudahDibayarException(
					"Tagihan ini sudah dibayar menggunakan VA " + pembayaran.getKode() + waktu
							+ ". VA baru tidak dibuat. Jika status tersebut tidak sesuai mutasi bank, koreksi dahulu melalui menu Virtual Account.");
		}
	}

	private static boolean tagihanSama(VirtualAccountBank pembayaran, String keterangan, String cicilan,
			String detailBiaya, Double total) {
		String ketBaru = normalisasiKeterangan(keterangan);
		String ketLama = normalisasiKeterangan(pembayaran.getKeterangan());
		if (!ketBaru.isEmpty() && ketBaru.equals(ketLama)) {
			return true;
		}

		String cicilanBaru = normalisasiToken(cicilan);
		String cicilanLama = normalisasiToken(pembayaran.getCicilan());
		if (!cicilanBaru.isEmpty() && cicilanBaru.equals(cicilanLama)) {
			return true;
		}

		String detailBaru = normalisasiToken(detailBiaya);
		String detailLama = normalisasiToken(pembayaran.getDetailbiaya());
		double nilaiBaru = total == null ? 0.0 : total.doubleValue();
		double nilaiLama = pembayaran.getTotal() == null ? 0.0 : pembayaran.getTotal().doubleValue();
		return !detailBaru.isEmpty() && detailBaru.equals(detailLama)
				&& Math.abs(nilaiBaru - nilaiLama) < 0.01;
	}

	private static String normalisasiKeterangan(String nilai) {
		if (nilai == null) {
			return "";
		}
		return nilai.toLowerCase().replace("qris:true", "").replace("finpay:true", "")
				.replaceAll("\\s+", "").trim();
	}

	private static String normalisasiToken(String nilai) {
		if (nilai == null || nilai.trim().isEmpty()) {
			return "";
		}
		TreeSet<String> token = new TreeSet<String>();
		for (String bagian : nilai.split(",")) {
			if (bagian != null && !bagian.trim().isEmpty()) {
				token.add(bagian.trim().toLowerCase());
			}
		}
		return token.toString();
	}


	public static Session openSession() {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.setFlushMode(FlushMode.COMMIT);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/MahasiswaVirtualAccountHelper.java:48");
		}
		return session;
	}

	/**
	 * Tambahkan satu token ("Item-..."/"Bulanan-..."/id) ke string cicilan VA
	 * TANPA menduplikasi token identik.
	 *
	 * Latar belakang: string cicilan VA dipakai PembayaranAction/servlet bank
	 * untuk MENGHITUNG ULANG total saat inquiry (cek tagihan) lalu menimpa
	 * VirtualAccountBank.total lewat updateTotal(). Bila pemanggil downloadData
	 * sudah mengisi param "cicilan" DAN juga mengirim gridCicilan berisi item
	 * yang sama (kasus checkout tampilan baru _lanjut_bayar_services.jsp),
	 * token tercatat dua kali sehingga tagihan menjadi 2x lipat begitu
	 * nasabah sekadar cek tagihan dari aplikasi bank. Guard ini membuat
	 * penambahan token idempoten di seluruh class Download*.
	 */
	public static String tambahTokenCicilan(String cicilan, String token) {
		if (token == null || token.trim().isEmpty()) {
			return cicilan == null ? "" : cicilan;
		}
		String c = cicilan == null ? "" : cicilan;
		if (("," + c + ",").contains("," + token + ",")) {
			return c;
		}
		return c.isEmpty() ? token : c + "," + token;
	}

	/** Varian StringBuilder dari {@link #tambahTokenCicilan(String, String)}. */
	public static void tambahTokenCicilan(StringBuilder cicilan, String token) {
		if (cicilan == null || token == null || token.trim().isEmpty()) {
			return;
		}
		if (("," + cicilan + ",").contains("," + token + ",")) {
			return;
		}
		if (cicilan.length() > 0) {
			cicilan.append(",");
		}
		cicilan.append(token);
	}

	public static Session ensureOpen(Session session) {
		try {
			if (session != null && session.isOpen()) {
				return session;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/MahasiswaVirtualAccountHelper.java:96");
		}
		return openSession();
	}

	public static void beginTransactionIfNeeded(Session session) {
		if (session == null) {
			throw new IllegalStateException("Session database belum dibuat.");
		}
		try {
			if (!session.isOpen()) {
				throw new IllegalStateException("Session database sudah tertutup.");
			}
			if (!session.getTransaction().isActive()) {
				session.beginTransaction();
			}
		} catch (RuntimeException e) {
			throw e;
		}
	}

	public static void commitTransactionIfActive(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen() && session.getTransaction().isActive()) {
				session.getTransaction().commit();
			}
		} catch (RuntimeException e) {
			throw e;
		}
	}

	public static void rollbackTransactionIfActive(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen() && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/MahasiswaVirtualAccountHelper.java:138");
		}
	}

	public static void closeSessionQuietly(Session session) {
		closeOpenedSession(session);
	}

	public static void closeHibernateContextQuietly() {
		try {
			HibernateUtil.closeSession();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/MahasiswaVirtualAccountHelper.java:149");
		}
	}

	public static void closeOpenedSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/MahasiswaVirtualAccountHelper.java:159");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/MahasiswaVirtualAccountHelper.java:163");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/MahasiswaVirtualAccountHelper.java:169");
		}
	}

	public static void closeNativeSession(Session session) {
		closeOpenedSession(session);
		try {
			HibernateUtil.closeSession();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/MahasiswaVirtualAccountHelper.java:177");
		}
	}

	public static void rollbackQuietly(Transaction tx) {
		if (tx == null) {
			return;
		}
		try {
			if (tx.isActive()) {
				tx.rollback();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/MahasiswaVirtualAccountHelper.java:189");
		}
	}

	@SuppressWarnings("unchecked")
	public static Set<KegiatanTemporary> ambilKegiatanTemporaryTerpilih(Grid grid) {
		Set<KegiatanTemporary> selected = new HashSet<KegiatanTemporary>();
		if (grid == null || grid.getRows() == null) {
			return selected;
		}
		List<Row> rows = grid.getRows().getChildren();
		for (Row row : rows) {
			if (row == null || !row.hasAttribute("checkboxConfig")) {
				continue;
			}
			Object checkboxObject = row.getAttribute("checkboxConfig");
			if (!(checkboxObject instanceof MyCheckboxConfig)) {
				continue;
			}
			MyCheckboxConfig checkboxConfig = (MyCheckboxConfig) checkboxObject;
			if (checkboxConfig != null && checkboxConfig.isChecked()) {
				Object kt = row.getAttribute("kegiatanTemporary");
				if (kt instanceof KegiatanTemporary) {
					selected.add((KegiatanTemporary) kt);
				}
			}
		}
		return selected;
	}

	public static Mahasiswa ambilMahasiswa(Set<KegiatanTemporary> selected) {
		if (selected == null) {
			return null;
		}
		for (KegiatanTemporary kegiatanTemporary : selected) {
			if (kegiatanTemporary != null && kegiatanTemporary.getMahasiswa() != null) {
				return kegiatanTemporary.getMahasiswa();
			}
		}
		return null;
	}

	public static BiodataCalonMahasiswa ambilCalonMahasiswa(Set<KegiatanTemporary> selected) {
		if (selected == null) {
			return null;
		}
		for (KegiatanTemporary kegiatanTemporary : selected) {
			if (kegiatanTemporary != null && kegiatanTemporary.getCalonMahasiswa() != null) {
				return kegiatanTemporary.getCalonMahasiswa();
			}
		}
		return null;
	}

	public static double hitungTotal(Set<KegiatanTemporary> selected) {
		double total = 0.0;
		if (selected == null) {
			return total;
		}
		for (KegiatanTemporary kegiatanTemporary : selected) {
			if (kegiatanTemporary != null && kegiatanTemporary.getAmount() != null) {
				total += kegiatanTemporary.getAmount().doubleValue();
			}
		}
		return total;
	}

	public static String validasiKeranjang(Set<KegiatanTemporary> selected) {
		if (selected == null || selected.isEmpty()) {
			return "Pilihlah satu atau lebih dari satu item keranjang pembayaran yang ingin Anda bayarkan";
		}
		Mahasiswa mahasiswa = null;
		BiodataCalonMahasiswa calonMahasiswa = null;
		for (KegiatanTemporary kegiatanTemporary : selected) {
			if (kegiatanTemporary == null) {
				continue;
			}
			if (kegiatanTemporary.getAmount() == null || Math.abs(kegiatanTemporary.getAmount().doubleValue()) < 0.01) {
				return "Terdapat item keranjang dengan nominal kosong atau nol. Periksa kembali rincian pembayaran.";
			}
			if (kegiatanTemporary.getMahasiswa() != null) {
				if (mahasiswa == null) {
					mahasiswa = kegiatanTemporary.getMahasiswa();
				} else if (mahasiswa.getId() != null && kegiatanTemporary.getMahasiswa().getId() != null
						&& !mahasiswa.getId().equals(kegiatanTemporary.getMahasiswa().getId())) {
					return "Item keranjang harus berasal dari mahasiswa yang sama.";
				}
			}
			if (kegiatanTemporary.getCalonMahasiswa() != null) {
				if (calonMahasiswa == null) {
					calonMahasiswa = kegiatanTemporary.getCalonMahasiswa();
				} else if (calonMahasiswa.getId() != null && kegiatanTemporary.getCalonMahasiswa().getId() != null
						&& !calonMahasiswa.getId().equals(kegiatanTemporary.getCalonMahasiswa().getId())) {
					return "Item keranjang harus berasal dari calon mahasiswa yang sama.";
				}
			}
		}
		if (hitungTotal(selected) < 0.01) {
			return "Tagihan tidak sesuai";
		}
		return null;
	}

	public static Double getKonfigurasiDouble(String key, String defaultValue) {
		try {
			String nilai = Common.getKonfigurasi(key, defaultValue).getNilai();
			return Double.valueOf(nilai == null || nilai.trim().length() == 0 ? defaultValue : nilai.trim());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return Double.valueOf(0.0);
		}
	}

	public static String buatPesanKonfirmasi(String labelGateway, Set<KegiatanTemporary> selected, Double biayaAdministrasi) {
		Mahasiswa mahasiswa = ambilMahasiswa(selected);
		BiodataCalonMahasiswa calonMahasiswa = ambilCalonMahasiswa(selected);
		double total = hitungTotal(selected);
		double admin = biayaAdministrasi == null ? 0.0 : biayaAdministrasi.doubleValue();
		String nama = mahasiswa != null ? mahasiswa.getNama() : (calonMahasiswa == null ? "-" : calonMahasiswa.getNama());
		StringBuilder sb = new StringBuilder();
		sb.append("Apakah yakin ingin melakukan ").append(labelGateway == null ? "pembayaran" : labelGateway);
		sb.append(" untuk:\nMahasiswa : ").append(nama);
		sb.append("\nJumlah yang akan dibayar : ").append(Common.numberFormat.get().format(total));
		if (admin > 0.1) {
			sb.append("\nBiaya administrasi : ").append(Common.numberFormat.get().format(admin));
			sb.append("\nTotal yang akan dibayar : ").append(Common.numberFormat.get().format(total + admin));
		}
		sb.append("\nTerbilang : ").append(IndonesianNumberToWords.convert((long) (total + admin)));
		return sb.toString();
	}

	public static String buatUrlNoVa(VirtualAccountBank va, Mahasiswa mahasiswa, BiodataCalonMahasiswa calonMahasiswa,
			Double biayaAdministrasi, String fileZulPrefix, String prefixBankLainKode) throws Exception {
		if (va == null || va.getId() == null) {
			return null;
		}
		double admin = biayaAdministrasi == null ? 0.0 : biayaAdministrasi.doubleValue();
		String code = va.getKode();
		File fileQr = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + va.getId() + ".png");
		BarcodeCommon.generateCRCode(code, fileQr);

		String kodeBankLain = prefixBankLainKode;
		if (kodeBankLain != null && kodeBankLain.trim().length() > 0) {
			if (va.getKanalPembayaran() != null && va.getKanalPembayaran().getBsiUsername() != null
					&& va.getKanalPembayaran().getBsiUsername().trim().length() > 0) {
				code = va.getKanalPembayaran().getBsiUsername().trim() + code;
			}
			kodeBankLain = kodeBankLain + code;
		}

		double total = va.getTotal() == null ? 0.0 : va.getTotal().doubleValue();
		String nama = mahasiswa != null ? mahasiswa.getNama() : (calonMahasiswa == null ? "" : calonMahasiswa.getNama());
		String prefix = fileZulPrefix == null || fileZulPrefix.trim().length() == 0 ? DEFAULT_VA_WINDOW : fileZulPrefix;
		String url = prefix + "?va=" + URLEncoder.encode(va.getKode(), "UTF-8")
				+ "&nominal=" + URLEncoder.encode("Rp. " + Common.numberFormat.get().format(total), "UTF-8")
				+ "&biayaAdministrasi="
				+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(admin), "UTF-8")
				+ "&nama=" + URLEncoder.encode(nama, "UTF-8")
				+ "&kadalurasa="
				+ URLEncoder.encode(va.getKadaluarsaWaktu() == null ? "" : Common.dateFormat.get().format(va.getKadaluarsaWaktu()), "UTF-8")
				+ "&biayaTotal="
				+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(total + admin), "UTF-8")
				+ "&qr=" + URLEncoder.encode(Common.getRequestHostWithProtocol() + "/report/" + fileQr.getName(), "UTF-8")
				+ "&terbilang="
				+ URLEncoder.encode(IndonesianNumberToWords.convert((long) (total + admin)), "UTF-8")
				+ "&tampilBiayaAdministrasi=" + (admin > 0.1);
		if (kodeBankLain != null && kodeBankLain.trim().length() > 0) {
			url += "&kodeBankLain=" + URLEncoder.encode(kodeBankLain, "UTF-8");
		}
		return url;
	}

	public static void tampilkanHasilVirtualAccount(VirtualAccountBank va, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa calonMahasiswa, Double biayaAdministrasi, String fileZulPrefix,
			String prefixBankLainKode) throws Exception {
		if (va != null && va.getLink() != null && va.getLink().trim().length() > 0) {
			Clients.evalJavaScript("popupCenter({url: '" + va.getLink() + "', title: 'Book', w: 1200, h: 600});");
			return;
		}
		String url = buatUrlNoVa(va, mahasiswa, calonMahasiswa, biayaAdministrasi, fileZulPrefix, prefixBankLainKode);
		if (url != null) {
			Common.displayWindow(url, true, "75%");
			return;
		}
		PesanFormalHelper.tampilkanGagal(
				"Menampilkan Nomor Virtual Account/tautan pembayaran",
				"Sistem tidak berhasil membentuk Nomor Virtual Account maupun tautan (link) pembayaran untuk transaksi ini. Kemungkinan data tagihan belum tersimpan dengan benar, atau layanan Virtual Account/kanal pembayaran sedang tidak merespon.",
				new String[] {
						"Ulangi proses pembayaran dari awal.",
						"Periksa kembali data tagihan/keranjang yang dipilih, pastikan tidak kosong.",
						"Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." });
	}

	public static void redirectLinkVirtualAccount(VirtualAccountBank va) {
		try {
			if (va != null && va.getLink() != null && va.getLink().trim().length() > 0) {
				ExecutionsCtrl.getCurrent().sendRedirect(va.getLink(), "_blank");
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Mengarahkan (redirect) ke tautan pembayaran Virtual Account",
					e,
					new String[] {
							"Ulangi proses dengan mengklik kembali tombol/tautan pembayaran.",
							"Pastikan browser tidak memblokir pop-up/redirect untuk halaman ini.",
							"Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." });
		}
	}



	private static String js(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").replace("\r", " ");
	}

	public static void tampilkanProgress(String judul, String keterangan, int persen) {
		int p = persen < 0 ? 0 : (persen > 100 ? 100 : persen);
		String id = "aisVaPaymentProgress";
		StringBuilder script = new StringBuilder();
		script.append("(function(){");
		script.append("var id='").append(id).append("';");
		script.append("var e=document.getElementById(id);");
		script.append("if(!e){e=document.createElement('div');e.id=id;document.body.appendChild(e);}");
		script.append("e.style.cssText='position:fixed;z-index:99999;right:22px;bottom:22px;width:360px;max-width:calc(100% - 44px);background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);color:#fff;border-radius:18px;box-shadow:0 18px 45px rgba(15,23,42,.35);font-family:Arial,sans-serif;padding:16px;box-sizing:border-box;';");
		script.append("e.innerHTML=\"");
		script.append("<div style='font-size:12px;opacity:.8;text-transform:uppercase;letter-spacing:.10em;'>Memuat Data</div>");
		script.append("<div style='font-size:16px;font-weight:800;margin-top:4px;'>").append(js(judul)).append("</div>");
		script.append("<div style='font-size:12px;line-height:1.45;margin-top:5px;opacity:.92;'>").append(js(keterangan)).append("</div>");
		script.append("<div style='height:10px;border-radius:999px;background:rgba(255,255,255,.20);overflow:hidden;margin-top:12px;'>");
		script.append("<div style='height:10px;border-radius:999px;background:linear-gradient(90deg,#67e8f9,#bbf7d0);width:").append(p).append("%;transition:width .25s ease;'></div>");
		script.append("</div><div style='text-align:right;font-size:12px;font-weight:800;margin-top:6px;'>").append(p).append("%</div>\";");
		script.append("})();");
		Clients.evalJavaScript(script.toString());
	}

	public static void sembunyikanProgress() {
		Clients.evalJavaScript("(function(){var e=document.getElementById('aisVaPaymentProgress');if(e){e.style.opacity='0';setTimeout(function(){if(e&&e.parentNode){e.parentNode.removeChild(e);}},450);}})();");
	}

	@SuppressWarnings("unchecked")
	public static void applyGatewayFlag(Map param, String gatewayId) {
		if (param == null || gatewayId == null) {
			return;
		}
		String gateway = gatewayId.trim().toLowerCase();
		if (gateway.length() == 0) {
			return;
		}
		param.put(gateway, true);
		if ("smartlink".equals(gateway)) {
			param.put("smartlink", true);
		}
		param.put("esmartlinkBayarVia", gateway);
	}
}
