package ais.action.master;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Label;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.UserOnlineCounter;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.MemoryInfo;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyGrid;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyToolbarbuttonConfig;

import org.zkoss.zul.Html;
import ais.action.master.helper.GenericActionDashboardHelper;
public class MemoryInfoAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private static final int MAKS_SAMPEL_AI = 300;

	private Paging paging;
	private MyGrid grid;
	private MyDatebox start;
	private MyDatebox end;
	private Combobox statusPemakaian;

	
	private Html dashboardHtml;
	private Html progressHtml;
private MyToolbarbuttonConfig find;

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
		initDefaultTanggal();
		refreshDashboardSafe();

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "tanggal_dirubah", "maxMemory", "allocatedMemory", "freeMemory",
				"totalFreeMemory" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(MemoryInfo.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		// Tombol laporan modern (Cetak + Ekspor Excel + progress + grafik HTML/CSS) via mesin reuse.
		if (find != null && find.getParent() != null) {
			ais.action.master.helper.DashboardReportKit.pasangTombol(find.getParent(), self, buatSumberLaporanMemori());

			MyToolbarbuttonConfig copyAi = new MyToolbarbuttonConfig("Copy Instruksi AI", "/img/svg/copy.svg");
			copyAi.setTooltiptext("Salin prompt AI berisi sampel pemakaian memori terbaru ke clipboard");
			copyAi.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					copyInstruksiAiKeClipboard();
				}
			});
			find.getParent().appendChild(copyAi);
		}

		MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("Hapus Semua Info Memori", "/img/svg/trash.svg");
		Common.appendKeToolbar(hapus, find, comp);
		hapus.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				HibernateUtil.currentSession().createSQLQuery("delete from memory_info").executeUpdate();
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});
			}
		});
	}

	/** Default satu minggu terakhir sampai hari ini. */
	private void initDefaultTanggal() {
		Calendar hariIni = Calendar.getInstance();
		hariIni.set(Calendar.HOUR_OF_DAY, 0);
		hariIni.set(Calendar.MINUTE, 0);
		hariIni.set(Calendar.SECOND, 0);
		hariIni.set(Calendar.MILLISECOND, 0);
		if (end != null) {
			end.setReadonly(true);
			end.setValue(hariIni.getTime());
		}
		if (start != null) {
			start.setReadonly(true);
			Calendar satuMinggu = (Calendar) hariIni.clone();
			satuMinggu.add(Calendar.DATE, -7);
			start.setValue(satuMinggu.getTime());
		}
	}

	private Date tanggalMulai() {
		return start == null ? null : start.getValue();
	}

	private Date tanggalSampaiEksklusif() {
		if (end == null || end.getValue() == null) {
			return null;
		}
		Calendar batas = Calendar.getInstance();
		batas.setTime(end.getValue());
		batas.set(Calendar.HOUR_OF_DAY, 0);
		batas.set(Calendar.MINUTE, 0);
		batas.set(Calendar.SECOND, 0);
		batas.set(Calendar.MILLISECOND, 0);
		batas.add(Calendar.DATE, 1);
		return batas.getTime();
	}

	private String statusPemakaianTerpilih() {
		if (statusPemakaian == null || statusPemakaian.getSelectedItem() == null
				|| statusPemakaian.getSelectedItem().getValue() == null) {
			return "SEMUA";
		}
		return statusPemakaian.getSelectedItem().getValue().toString();
	}
	private void refreshDashboardSafe() {
		try {
			GenericActionDashboardHelper.refresh(dashboardHtml, progressHtml, MemoryInfo.class,
					"Dasbor Pemakaian Memori", "Ringkasan kondisi memori server agar pemantauan beban sistem lebih mudah dipahami.");
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/MemoryInfoAction.java:103");
			}
		}
	}



	class MemoryInfoRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final MemoryInfo memoryInfo = (MemoryInfo) arg1;
			arg0.setValign("top");
			RevisiHelper.createNewRevisi(MemoryInfo.class, memoryInfo,
					Common.dateFormat5.get().format(memoryInfo.getTanggal_dirubah())).setParent(arg0);
			new Label(Common.numberFormat.get().format(UserOnlineCounter.bytesToMegabytesLong(memoryInfo.getMaxMemory()))
					+ "Mb").setParent(arg0);
			new Label(
					Common.numberFormat.get().format(UserOnlineCounter.bytesToMegabytesLong(memoryInfo.getAllocatedMemory()))
							+ "Mb").setParent(arg0);
			new Label(Common.numberFormat.get().format(UserOnlineCounter.bytesToMegabytesLong(memoryInfo.getFreeMemory()))
					+ "Mb").setParent(arg0);
			new Label(
					Common.numberFormat.get().format(UserOnlineCounter.bytesToMegabytesLong(memoryInfo.getTotalFreeMemory()))
							+ "Mb").setParent(arg0);
			double persen = nilai(memoryInfo.getMaxMemory()) > 0
					? (nilai(memoryInfo.getTotalFreeMemory()) * 100.0) / nilai(memoryInfo.getMaxMemory()) : 0;
			new Label(Common.numberFormat.get().format(persen) + "%").setParent(arg0);
			double terpakai = 100.0 - persen;
			Label status = new Label(terpakai >= 85.0 ? "Kritis" : terpakai >= 75.0 ? "Waspada" : "Normal");
			status.setStyle(terpakai >= 85.0
					? "color:#b91c1c;font-weight:bold;"
					: terpakai >= 75.0 ? "color:#b45309;font-weight:bold;" : "color:#15803d;font-weight:bold;");
			status.setParent(arg0);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(MemoryInfo.class);
		Date mulai = tanggalMulai();
		Date sampai = tanggalSampaiEksklusif();
		if (mulai != null) {
			criteria.add(Restrictions.ge("tanggal_dirubah", mulai));
		}
		if (sampai != null) {
			criteria.add(Restrictions.lt("tanggal_dirubah", sampai));
		}
		String status = statusPemakaianTerpilih();
		if ("KRITIS".equals(status)) {
			criteria.add(Restrictions.sqlRestriction(
					"{alias}.maxMemory > 0 and ({alias}.maxMemory - {alias}.totalFreeMemory) * 100 >= {alias}.maxMemory * 85"));
		} else if ("WASPADA".equals(status)) {
			criteria.add(Restrictions.sqlRestriction(
					"{alias}.maxMemory > 0 and ({alias}.maxMemory - {alias}.totalFreeMemory) * 100 >= {alias}.maxMemory * 75 and ({alias}.maxMemory - {alias}.totalFreeMemory) * 100 < {alias}.maxMemory * 85"));
		} else if ("NORMAL".equals(status)) {
			criteria.add(Restrictions.sqlRestriction(
					"{alias}.maxMemory > 0 and ({alias}.maxMemory - {alias}.totalFreeMemory) * 100 < {alias}.maxMemory * 75"));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		GenericActionDashboardHelper.showProgress(progressHtml, 15, "Memuat data", "Membaca data sesuai filter yang aktif.");
		if (paging != null && event != null && !"onPaging".equals(event.getName())) {
			paging.setActivePage(0);
		}
		Common.initPaging(initCriteria(false), paging);

		List<MemoryInfo> memoryInfo = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(memoryInfo);
		grid.setRowRenderer(new MemoryInfoRenderer());
		grid.setModelCheckMobile(strset);
		refreshDashboardSafe();
		GenericActionDashboardHelper.hideProgress(progressHtml);
	}

	/**
	 * Mengambil {@code maks} sampel memori terbaru (urut id menurun) memakai {@code currentSession()}
	 * yang ditutup otomatis oleh kerangka kerja — maka tidak boleh ditutup manual di sini.
	 */
	@SuppressWarnings("unchecked")
	private List<MemoryInfo> ambilSampelMemori(int maks) {
		try {
			return initCriteria(true).setMaxResults(maks).list();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"MemoryInfoAction: gagal mengambil sampel memori sesuai filter aktif");
			return new java.util.ArrayList<MemoryInfo>();
		}
	}

	private void copyInstruksiAiKeClipboard() {
		try {
			GenericActionDashboardHelper.showProgress(progressHtml, 15, "Menyiapkan analisis AI",
					"Mengumpulkan sampel pemakaian memori terbaru.");
			String prompt = buatInstruksiAiMemori();
			GenericActionDashboardHelper.showProgress(progressHtml, 90, "Menyalin",
					"Instruksi dan data memori sedang disalin ke clipboard.");
			Clients.evalJavaScript(buildCopyToClipboardScript(prompt));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			GenericActionDashboardHelper.hideProgress(progressHtml);
		}
	}

	private String buatInstruksiAiMemori() {
		List<MemoryInfo> sampel = ambilSampelMemori(MAKS_SAMPEL_AI);
		StringBuilder text = new StringBuilder(65536);
		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		Set<Long> kapasitas = new LinkedHashSet<Long>();

		text.append("Tolong analisis kondisi memori JVM eCampus berdasarkan seluruh sampel berikut.\n\n");
		text.append("Tugas analisis:\n");
		text.append("1. Nilai tekanan memori, risiko OutOfMemoryError, lonjakan, dan arah tren.\n");
		text.append("2. Temukan waktu pemakaian tertinggi serta sampel yang perlu diperiksa segera.\n");
		text.append("3. Jangan menyimpulkan memory leak hanya dari seri pendek; jelaskan bukti tambahan yang perlu diperiksa, termasuk GC log, heap dump, dan thread/process terkait.\n");
		text.append("4. Jika nilai max_mb berbeda, kelompokkan dan analisis tiap kapasitas secara terpisah karena data mungkin berasal dari JVM/node berbeda. Jangan mencampur tren antarkelompok.\n");
		text.append("5. Berikan rekomendasi berurutan: tindakan mendesak, pemeriksaan lanjutan, lalu perbaikan konfigurasi/kode yang aman.\n");
		text.append("6. Gunakan rumus: used = allocated - free_allocated = max - total_free; total_free_pct = total_free / max.\n\n");
		text.append("Dibuat pada       : ").append(format.format(new Date())).append('\n');
		text.append("Filter tanggal    : ")
				.append(start == null || start.getValue() == null ? "-" : format.format(start.getValue()))
				.append(" s.d. ")
				.append(end == null || end.getValue() == null ? "-" : format.format(end.getValue())).append('\n');
		text.append("Filter pemakaian  : ").append(statusPemakaianTerpilih()).append('\n');
		text.append("Jumlah sampel     : ").append(sampel.size()).append(" (maksimal ").append(MAKS_SAMPEL_AI).append(")\n");
		text.append("Urutan data       : terlama ke terbaru\n");

		for (MemoryInfo m : sampel) {
			kapasitas.add(Long.valueOf(toMb(nilai(m.getMaxMemory()))));
		}
		text.append("Kelompok max JVM  : ").append(kapasitas).append(" MB\n");
		if (kapasitas.size() > 1) {
			text.append("PERINGATAN        : Ada lebih dari satu kapasitas maksimum; kemungkinan sampel berasal dari beberapa JVM/node.\n");
		}
		text.append("\n=== DATA PEMAKAIAN MEMORI ECAMPUS ===\n");

		if (sampel.isEmpty()) {
			text.append("Tidak ada sampel pemakaian memori yang tersedia.\n");
			return text.toString();
		}

		int nomor = 1;
		for (int i = sampel.size() - 1; i >= 0; i--) {
			MemoryInfo m = sampel.get(i);
			long max = nilai(m.getMaxMemory());
			long allocated = nilai(m.getAllocatedMemory());
			long freeAllocated = nilai(m.getFreeMemory());
			long totalFree = nilai(m.getTotalFreeMemory());
			long used = allocated - freeAllocated;
			double usedPct = max > 0 ? used * 100.0 / max : 0;
			double totalFreePct = max > 0 ? totalFree * 100.0 / max : 0;
			text.append("SAMPLE ").append(nomor++).append(" | id=").append(m.getId());
			text.append(" | waktu=").append(m.getTanggal_dirubah() == null ? "-" : format.format(m.getTanggal_dirubah()));
			text.append(" | max_mb=").append(toMb(max));
			text.append(" | allocated_mb=").append(toMb(allocated));
			text.append(" | free_allocated_mb=").append(toMb(freeAllocated));
			text.append(" | total_free_mb=").append(toMb(totalFree));
			text.append(" | used_mb=").append(toMb(used));
			text.append(" | used_pct=").append(formatPersen(usedPct));
			text.append(" | total_free_pct=").append(formatPersen(totalFreePct)).append('\n');
		}
		text.append("=== AKHIR DATA PEMAKAIAN MEMORI ===\n");
		return text.toString();
	}

	private long nilai(Long value) {
		return value == null ? 0 : value.longValue();
	}

	private long toMb(long bytes) {
		return UserOnlineCounter.bytesToMegabytesLong(bytes);
	}

	private String formatPersen(double value) {
		return Common.numberFormat.get().format(value) + "%";
	}

	private String buildCopyToClipboardScript(String text) {
		String escaped = escapeJavaScriptString(text);
		StringBuilder js = new StringBuilder(escaped.length() + 1800);
		js.append("(function(){");
		js.append("var text=\"").append(escaped).append("\";");
		js.append("function toast(msg){try{");
		js.append("var d=document.createElement('div');");
		js.append("d.style.cssText='position:fixed;right:18px;bottom:18px;z-index:999999;background:#0f172a;color:#fff;padding:10px 14px;border-radius:12px;font:12px Arial,Helvetica,sans-serif;box-shadow:0 14px 32px rgba(15,23,42,.28);max-width:360px;';");
		js.append("d.innerHTML=msg;document.body.appendChild(d);setTimeout(function(){try{document.body.removeChild(d);}catch(e){}},1800);");
		js.append("}catch(e){}}");
		js.append("function fallback(){var ta=document.createElement('textarea');ta.value=text;ta.setAttribute('readonly','readonly');ta.style.position='fixed';ta.style.left='-9999px';ta.style.top='0';document.body.appendChild(ta);ta.focus();ta.select();try{var ok=document.execCommand('copy');toast(ok?'Data memori berhasil disalin ke clipboard':'Silakan salin manual dari kotak yang muncul');if(!ok){window.prompt('Copy:',text);}}catch(e){window.prompt('Copy:',text);}try{document.body.removeChild(ta);}catch(e){}}");
		js.append("if(navigator.clipboard&&navigator.clipboard.writeText){navigator.clipboard.writeText(text).then(function(){toast('Data memori berhasil disalin ke clipboard');},function(){fallback();});}else{fallback();}");
		js.append("})();");
		return js.toString();
	}

	private String escapeJavaScriptString(String value) {
		if (value == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(value.length() + 32);
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '\\') sb.append("\\\\");
			else if (c == '"') sb.append("\\\"");
			else if (c == '\n') sb.append("\\n");
			else if (c == '\r') sb.append("\\r");
			else if (c == '\t') sb.append("\\t");
			else if (c == '\b') sb.append("\\b");
			else if (c == '\f') sb.append("\\f");
			else if (c < 32) {
				String hex = Integer.toHexString(c);
				sb.append("\\u");
				for (int j = hex.length(); j < 4; j++) sb.append('0');
				sb.append(hex);
			} else sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * Menyusun deskripsi laporan "Ringkasan Pemakaian Memori Server" untuk mesin
	 * {@link ais.action.master.helper.DashboardReportKit}: kartu kondisi terkini (KPI),
	 * tren pemakaian (grafik garis), dan tabel riwayat terbaru.
	 */
	private ais.action.master.helper.DashboardReportKit.SumberLaporan buatSumberLaporanMemori() {
		return new ais.action.master.helper.DashboardReportKit.SumberLaporan() {
			@Override
			public String judul() {
				return "Ringkasan Pemakaian Memori Server";
			}

			@Override
			public String subjudul() {
				return "";
			}

			@Override
			public String deskripsi() {
				return "Melihat seberapa berat kerja memori server dan kapan pemakaiannya paling tinggi.";
			}

			@Override
			public List<ais.action.master.helper.DashboardReportKit.Bagian> bagian() {
				List<ais.action.master.helper.DashboardReportKit.Bagian> b =
						new java.util.ArrayList<ais.action.master.helper.DashboardReportKit.Bagian>();

				b.add(ais.action.master.helper.DashboardReportKit.kpi("Kondisi Terkini",
						"Angka penting memori pada pengambilan data terakhir.",
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							@Override
							public List<Object[]> ambil() {
								List<Object[]> r = new java.util.ArrayList<Object[]>();
								List<MemoryInfo> s = ambilSampelMemori(1);
								if (!s.isEmpty()) {
									MemoryInfo m = s.get(0);
									long max = UserOnlineCounter.bytesToMegabytesLong(m.getMaxMemory());
									long alok = UserOnlineCounter.bytesToMegabytesLong(m.getAllocatedMemory());
									long bebasDalamAlokasi = UserOnlineCounter.bytesToMegabytesLong(m.getFreeMemory());
									long bebas = UserOnlineCounter.bytesToMegabytesLong(m.getTotalFreeMemory());
									long pakai = alok - bebasDalamAlokasi;
									long persen = max > 0 ? Math.round(pakai * 100.0 / max) : 0;
									r.add(new Object[] { "Batas Maksimum", max + " MB", "Kapasitas memori tertinggi" });
									r.add(new Object[] { "Sedang Dipakai", pakai + " MB", persen + "% dari batas maksimum" });
									r.add(new Object[] { "Masih Bebas", bebas + " MB", "Sisa memori yang siap dipakai" });
									r.add(new Object[] { "Dialokasikan", alok + " MB", "Memori yang telah disiapkan sistem" });
								}
								return r;
							}
						}));

				b.add(ais.action.master.helper.DashboardReportKit.garis("Tren Pemakaian Memori",
						"Naik-turun pemakaian memori dari waktu ke waktu; puncak menandai saat server paling sibuk.",
						new String[] { "Waktu", "Terpakai (MB)" },
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							@Override
							public List<Object[]> ambil() {
								List<Object[]> r = new java.util.ArrayList<Object[]>();
								List<MemoryInfo> s = ambilSampelMemori(120);
								for (int i = s.size() - 1; i >= 0; i--) {
									MemoryInfo m = s.get(i);
									long alok = UserOnlineCounter.bytesToMegabytesLong(m.getAllocatedMemory());
									long bebas = UserOnlineCounter.bytesToMegabytesLong(m.getFreeMemory());
									r.add(new Object[] { Common.dateFormat5.get().format(m.getTanggal_dirubah()),
											Long.valueOf(alok - bebas) });
								}
								return r;
							}
						}));

				b.add(ais.action.master.helper.DashboardReportKit.tabel("Riwayat Terbaru",
						"Daftar rincian beberapa pengambilan data memori terakhir.",
						new String[] { "Waktu", "Maks (MB)", "Dialokasikan (MB)", "Terpakai (MB)", "Bebas (MB)", "Pemakaian %" },
						new ais.action.master.helper.DashboardReportKit.PenyediaBaris() {
							@Override
							public List<Object[]> ambil() {
								List<Object[]> r = new java.util.ArrayList<Object[]>();
								for (MemoryInfo m : ambilSampelMemori(50)) {
									long max = UserOnlineCounter.bytesToMegabytesLong(m.getMaxMemory());
									long alok = UserOnlineCounter.bytesToMegabytesLong(m.getAllocatedMemory());
									long bebasDalamAlokasi = UserOnlineCounter.bytesToMegabytesLong(m.getFreeMemory());
									long bebas = UserOnlineCounter.bytesToMegabytesLong(m.getTotalFreeMemory());
									long pakai = alok - bebasDalamAlokasi;
									long persen = max > 0 ? Math.round(pakai * 100.0 / max) : 0;
									r.add(new Object[] { Common.dateFormat5.get().format(m.getTanggal_dirubah()),
											Long.valueOf(max), Long.valueOf(alok), Long.valueOf(pakai),
											Long.valueOf(bebas), Long.valueOf(persen) });
								}
								return r;
							}
						}));
				return b;
			}
		};
	}

}
