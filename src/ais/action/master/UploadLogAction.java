package ais.action.master;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.UploadLogInfo;
import ais.ui.util.DataCriteria;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

import org.zkoss.zul.Html;
import ais.action.master.helper.GenericActionDashboardHelper;
/**
 * Controller/action ZK untuk upload log. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Paging paging}, {@code MyGrid grid},
 * {@code Html dashboardHtml}, {@code Html progressHtml}, {@code Textbox searchnama}, {@code MyDatebox start},
 * {@code MyDatebox end}, {@code boolean delete}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code initCriteria()}); pembacaan/pencarian ({@code refreshDashboardSafe()}, {@code
 * onSearchDefault()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class UploadLogAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	
	private Html dashboardHtml;
	private Html progressHtml;
private Textbox searchnama;
	private MyDatebox start;
	private MyDatebox end;

	private boolean delete = false;
	private String className = null;
	private MyToolbarbuttonConfig add;
	private Tbmuser tbmuser = null;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		refreshDashboardSafe();

		if (execution.getParameter("className") != null) {
			className = execution.getParameter("className");
		}

		tbmuser = Common.getCurrentUser();

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "nama", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

	}
	private void refreshDashboardSafe() {
		try {
			if (dashboardHtml != null) {
				dashboardHtml.setContent(buildDashboardUpload());
			}
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/UploadLogAction.java:109");
			}
		}
	}

	/** Analisis ringkas memakai maksimal 500 catatan terbaru yang sesuai dengan filter aktif. */
	@SuppressWarnings("unchecked")
	private String buildDashboardUpload() {
		List<UploadLogInfo> logs = initCriteria(true).setMaxResults(500).list();
		long total = countUploadSesuaiFilter(null);
		long hariIni = countUploadSesuaiFilter(awalHari(0));
		long tujuhHari = countUploadSesuaiFilter(awalHari(-6));
		UploadDashboardData data = analisisUpload(logs, total, hariIni, tujuhHari);

		StringBuilder html = new StringBuilder(24000);
		html.append("<div style='font-family:Arial,Helvetica,sans-serif;color:#0f172a;padding:12px;background:#f8fafc;'>");
		html.append(DashboardUiKit.introBanner("Dasbor Catatan Upload",
				"Pantauan rinci file yang diunggah, sumber modul, pengguna, pola waktu, dan kelengkapan metadata untuk membantu penelusuran proses upload."));

		List<DashboardUiKit.Stat> stats = new ArrayList<DashboardUiKit.Stat>();
		stats.add(new DashboardUiKit.Stat("Sesuai Filter", DashboardUiKit.money(data.total),
				"Seluruh catatan yang cocok", DashboardUiKit.PRIMARY));
		stats.add(new DashboardUiKit.Stat("Hari Ini", DashboardUiKit.money(data.hariIni),
				"Upload sejak awal hari", DashboardUiKit.GOOD));
		stats.add(new DashboardUiKit.Stat("7 Hari", DashboardUiKit.money(data.tujuhHari),
				"Aktivitas satu minggu", DashboardUiKit.ACCENT));
		stats.add(new DashboardUiKit.Stat("Perlu Diperiksa",
				DashboardUiKit.money(data.metadataKosong + data.namaBerulang),
				"Metadata kosong + nama berulang pada sampel", data.metadataKosong > 0 ? DashboardUiKit.WARN : DashboardUiKit.GOOD));
		html.append(DashboardUiKit.cards(stats));

		html.append(DashboardUiKit.openGrid(300));
		html.append(DashboardUiKit.barList("Jenis File", "Ekstensi file paling sering pada 500 catatan terbaru.",
				data.perEkstensi, DashboardUiKit.PRIMARY, "file", false, "Belum ada file."));
		html.append(DashboardUiKit.barList("Sumber Modul", "Class/modul pencatat upload yang paling aktif.",
				data.perModul, DashboardUiKit.ACCENT, "upload", false, "Sumber modul belum tercatat."));
		html.append(DashboardUiKit.closeGrid());

		html.append(DashboardUiKit.openGrid(300));
		html.append(DashboardUiKit.barList("Pengunggah Teraktif", "Identitas pencatat upload pada sampel terbaru.",
				data.perPengguna, DashboardUiKit.GOOD, "file", false, "Identitas pengunggah belum tersedia."));
		LinkedHashMap<String, Double> kelengkapan = new LinkedHashMap<String, Double>();
		kelengkapan.put("Metadata lengkap", Double.valueOf(data.sampel - data.metadataKosong));
		kelengkapan.put("Perlu dilengkapi", Double.valueOf(data.metadataKosong));
		html.append(DashboardUiKit.donut("Kelengkapan Catatan",
				"Perbandingan catatan yang memiliki nama, lokasi, dan identitas pengunggah.", kelengkapan,
				false, "Belum ada catatan upload."));
		html.append(DashboardUiKit.closeGrid());

		LinkedHashMap<String, String> rincian = new LinkedHashMap<String, String>();
		rincian.put("Sampel Dianalisis", data.sampel + " catatan terbaru");
		rincian.put("Rata-rata Harian 7 Hari", formatSatuDesimal(data.tujuhHari / 7.0) + " upload/hari");
		rincian.put("Ekstensi Dominan", data.ekstensiDominan);
		rincian.put("Modul Dominan", data.modulDominan);
		rincian.put("Nama Berulang", data.namaBerulang + " catatan tambahan");
		rincian.put("Metadata Tidak Lengkap", data.metadataKosong + " catatan");
		html.append(DashboardUiKit.insight("Rincian Analisis", "Angka berikut menjadi dasar diagnosis otomatis.", rincian));
		html.append(DashboardUiKit.smartAnalysis(data.status, data.ringkasan, data.temuan,
				data.kemungkinanPenyebab, data.tindakan));
		html.append("<div style='font-size:10px;color:#64748b;'>Analisis mendalam memakai maksimal 500 catatan terbaru agar halaman tetap responsif; total KPI tetap menghitung seluruh data sesuai filter.</div></div>");
		return html.toString();
	}

	private UploadDashboardData analisisUpload(List<UploadLogInfo> logs, long total, long hariIni, long tujuhHari) {
		UploadDashboardData data = new UploadDashboardData();
		data.total = total;
		data.hariIni = hariIni;
		data.tujuhHari = tujuhHari;
		Map<String, Integer> namaCount = new HashMap<String, Integer>();
		if (logs != null) {
			for (UploadLogInfo log : logs) {
				if (log == null) continue;
				data.sampel++;
				String nama = aman(log.getNama());
				String ekstensi = ekstensi(nama);
				String modul = ringkas(log.getClassName(), 52, "Tidak tercatat");
				String pengguna = ringkas(log.getOlehId(), 42, "Tidak tercatat");
				tambah(data.perEkstensi, ekstensi);
				tambah(data.perModul, modul);
				tambah(data.perPengguna, pengguna);
				if (nama.length() > 0) {
					Integer count = namaCount.get(nama.toLowerCase());
					namaCount.put(nama.toLowerCase(), Integer.valueOf(count == null ? 1 : count.intValue() + 1));
				}
				if (nama.length() == 0 || aman(log.getKeterangan()).length() == 0
						|| aman(log.getOlehId()).length() == 0) data.metadataKosong++;
			}
		}
		for (Integer count : namaCount.values()) {
			if (count != null && count.intValue() > 1) data.namaBerulang += count.intValue() - 1;
		}
		data.ekstensiDominan = kunciTerbesar(data.perEkstensi, "Belum ada");
		data.modulDominan = kunciTerbesar(data.perModul, "Belum ada");

		double rata = tujuhHari / 7.0;
		if (data.metadataKosong > 0 || data.namaBerulang > Math.max(5, data.sampel / 5)) {
			data.status = "PERLU PERHATIAN";
			data.ringkasan = "Ditemukan catatan upload yang perlu diverifikasi sebelum dipakai sebagai bukti proses atau audit.";
		} else if (hariIni > 20 && hariIni > rata * 2.5) {
			data.status = "PERLU PERHATIAN";
			data.ringkasan = "Aktivitas upload hari ini meningkat tajam dibanding rata-rata tujuh hari.";
		} else if (data.sampel == 0) {
			data.status = "BELUM ADA DATA";
			data.ringkasan = "Belum ada catatan yang dapat dianalisis untuk filter saat ini.";
		} else {
			data.status = "NORMAL";
			data.ringkasan = "Aktivitas upload dan kelengkapan catatan berada dalam pola yang wajar pada sampel terbaru.";
		}

		data.temuan.add("Total " + total + " catatan sesuai filter; " + hariIni + " terjadi hari ini dan " + tujuhHari + " dalam tujuh hari.");
		data.temuan.add("Jenis file dominan: " + data.ekstensiDominan + "; sumber modul dominan: " + data.modulDominan + ".");
		data.temuan.add(data.metadataKosong + " dari " + data.sampel + " sampel memiliki metadata yang belum lengkap.");
		if (data.namaBerulang > 0) data.temuan.add("Ada " + data.namaBerulang + " pemakaian nama file berulang pada sampel terbaru.");

		if (data.metadataKosong > 0) data.kemungkinanPenyebab.add("Jalur upload lama tidak selalu mengisi nama file, lokasi penyimpanan, atau identitas pengguna secara lengkap.");
		if (data.namaBerulang > 0) data.kemungkinanPenyebab.add("Pengguna mengunggah ulang file yang sama, terjadi retry, atau proses impor mencatat satu nama pada beberapa percobaan.");
		if (hariIni > 20 && hariIni > rata * 2.5) data.kemungkinanPenyebab.add("Sedang ada impor massal, periode pengisian aktif, atau pengulangan upload akibat validasi gagal.");

		data.tindakan.add("Buka tab Data dan periksa catatan terbaru dari modul " + data.modulDominan + ".");
		if (data.metadataKosong > 0) data.tindakan.add("Telusuri jalur upload yang metadata-nya kosong dan pastikan nama, lokasi file, class sumber, serta user ID selalu dicatat.");
		if (data.namaBerulang > 0) data.tindakan.add("Bandingkan waktu dan pengunggah untuk nama berulang; pastikan hanya hasil upload yang berhasil yang diproses lebih lanjut.");
		data.tindakan.add("Cocokkan catatan upload dengan error pada waktu yang sama bila file dilaporkan gagal diproses.");
		return data;
	}

	private long countUploadSesuaiFilter(Date sejak) {
		try {
			Criteria criteria = initCriteria(false);
			if (sejak != null) criteria.add(Restrictions.ge("tanggal_dirubah", sejak));
			Object value = criteria.setProjection(Projections.rowCount()).uniqueResult();
			return value instanceof Number ? ((Number) value).longValue() : 0L;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "UploadLogAction: gagal menghitung ringkasan upload");
			return 0L;
		}
	}

	private Date awalHari(int mundurHari) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, mundurHari);
		c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
		return c.getTime();
	}

	private void tambah(LinkedHashMap<String, Number> map, String key) {
		Number n = map.get(key);
		map.put(key, Integer.valueOf(n == null ? 1 : n.intValue() + 1));
	}

	private String kunciTerbesar(LinkedHashMap<String, Number> map, String fallback) {
		String best = fallback; int max = -1;
		for (Map.Entry<String, Number> entry : map.entrySet()) {
			int value = entry.getValue() == null ? 0 : entry.getValue().intValue();
			if (value > max) { max = value; best = entry.getKey(); }
		}
		return best;
	}

	private String ekstensi(String nama) {
		int dot = nama.lastIndexOf('.');
		return dot >= 0 && dot < nama.length() - 1 ? nama.substring(dot + 1).toUpperCase() : "Tanpa ekstensi";
	}

	private String aman(String value) { return value == null ? "" : value.trim(); }

	private String ringkas(String value, int max, String fallback) {
		String text = aman(value);
		if (text.length() == 0) return fallback;
		return text.length() > max ? text.substring(0, max - 3) + "..." : text;
	}

	private String formatSatuDesimal(double value) {
		return new java.text.DecimalFormat("0.0").format(value);
	}

	private static class UploadDashboardData {
		long total, hariIni, tujuhHari;
		int sampel, metadataKosong, namaBerulang;
		String status, ringkasan, ekstensiDominan, modulDominan;
		LinkedHashMap<String, Number> perEkstensi = new LinkedHashMap<String, Number>();
		LinkedHashMap<String, Number> perModul = new LinkedHashMap<String, Number>();
		LinkedHashMap<String, Number> perPengguna = new LinkedHashMap<String, Number>();
		List<String> temuan = new ArrayList<String>();
		List<String> kemungkinanPenyebab = new ArrayList<String>();
		List<String> tindakan = new ArrayList<String>();
	}



	/**
	 * Renderer lokal untuk layar/komponen {@link UploadLogAction}. Kelas ini menerjemahkan satu item data menjadi
	 * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link UploadLogAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see UploadLogAction
	 */
	class UploadLogRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final UploadLogInfo uploadLog = (UploadLogInfo) arg1;
			new Label(Common.dateFormat5.get().format(uploadLog.getTanggal_dirubah())).setParent(arg0);

			if (uploadLog.getDiuploadOleh() != null) {
				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				CommonMedia.tampilkanGambarKecil(uploadLog.getDiuploadOleh()).setParent(vbox);
				new Label(uploadLog.getDiuploadOleh().getUserNama()).setParent(vbox);
			} else {
				new Label(uploadLog.getOlehId()).setParent(arg0);
			}

			RevisiHelper.createNewRevisi(UploadLogInfo.class, uploadLog, uploadLog.getNama()).setParent(arg0);

			new Label(ekstensi(aman(uploadLog.getNama()))).setParent(arg0);
			Label sumber = new Label(ringkas(uploadLog.getClassName(), 60, "Tidak tercatat"));
			sumber.setTooltiptext(aman(uploadLog.getClassName()));
			sumber.setParent(arg0);
			Label lokasi = new Label(ringkas(uploadLog.getKeterangan(), 90, "Tidak tercatat"));
			lokasi.setTooltiptext(aman(uploadLog.getKeterangan()));
			lokasi.setParent(arg0);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Download", "/img/upload.gif");
			button.setTooltiptext("Download Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					String url = Common.getRequestHostWithProtocol() + "/AmbilFileServer?file="
							+ URLEncoder.encode(uploadLog.getKeterangan(), "UTF-8");

					Executions.getCurrent().sendRedirect(url, "_blank");
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(uploadLog);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			aksiButtons.add(button);
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(UploadLogInfo.class);

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(tbmuser == null ? Restrictions.sqlRestriction("false")
				: Restrictions.or(Restrictions.isNull("diuploadOleh"), Restrictions.eq("diuploadOleh", tbmuser)))
				.add(className != null && !className.trim().isEmpty() ? Restrictions.eq("className", className)
						: Restrictions.sqlRestriction("true"))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(start == null || start.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("(this_.tanggal_dirubah) >= ('"
								+ Common.databaseDateFormat.get().format(start.getValue()) + " 00:00:00')"))

				.add(end == null || end.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("(this_.tanggal_dirubah) <= ('"
								+ Common.databaseDateFormat.get().format(end.getValue()) + " 23:59:59')"));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		GenericActionDashboardHelper.showProgress(progressHtml, 15, "Memuat data", "Membaca data sesuai filter yang aktif.");
		Common.initPaging(initCriteria(false), paging);

		List<UploadLogInfo> uploadLog = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(uploadLog);
		grid.setRowRenderer(new UploadLogRenderer());
		grid.setModelCheckMobile(strset);
		refreshDashboardSafe();
		GenericActionDashboardHelper.hideProgress(progressHtml);
	}

}
