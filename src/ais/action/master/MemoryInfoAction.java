package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Label;
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
import ais.ui.util.MyToolbarbuttonConfig;

import org.zkoss.zul.Html;
import ais.action.master.helper.GenericActionDashboardHelper;
public class MemoryInfoAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Paging paging;
	private MyGrid grid;

	
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
			double persen = (memoryInfo.getTotalFreeMemory() * 100.0) / memoryInfo.getAllocatedMemory();
			new Label(Common.numberFormat.get().format(persen) + "%").setParent(arg0);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(MemoryInfo.class);

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		GenericActionDashboardHelper.showProgress(progressHtml, 15, "Memuat data", "Membaca data sesuai filter yang aktif.");
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
			return HibernateUtil.currentSession().createCriteria(MemoryInfo.class)
					.addOrder(Order.desc("id")).setMaxResults(maks).list();
		} catch (Exception e) {
			return new java.util.ArrayList<MemoryInfo>();
		}
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
									long bebas = UserOnlineCounter.bytesToMegabytesLong(m.getTotalFreeMemory());
									long pakai = alok - bebas;
									long persen = alok > 0 ? Math.round(pakai * 100.0 / alok) : 0;
									r.add(new Object[] { "Batas Maksimum", max + " MB", "Kapasitas memori tertinggi" });
									r.add(new Object[] { "Sedang Dipakai", pakai + " MB", persen + "% dari yang dialokasikan" });
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
									long bebas = UserOnlineCounter.bytesToMegabytesLong(m.getTotalFreeMemory());
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
									long bebas = UserOnlineCounter.bytesToMegabytesLong(m.getTotalFreeMemory());
									long pakai = alok - bebas;
									long persen = alok > 0 ? Math.round(pakai * 100.0 / alok) : 0;
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
