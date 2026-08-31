package ais.action.master.sekolah.helper;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.maintenance.MainProgressHelper;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.helper.RekapitulasiAudioHelper;
import ais.action.master.helper.RekapitulasiMateriHelper;
import ais.action.master.helper.RekapitulasiPerkuliahanHelper;
import ais.action.master.helper.RekapitulasiTugasHelper;
import ais.action.master.helper.RekapitulasiTugasKelompokHelper;
import ais.action.master.helper.RekapitulasiUjianHelper;
import ais.action.master.helper.RekapitulasiVideoHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

/**
 * Jendela dasbor e-Learning untuk modul sekolah: menyajikan ringkasan aktivitas pembelajaran
 * daring dalam beberapa tab yang dimuat lazy (dibangun saat pertama kali dibuka, bukan sekaligus
 * di awal, lewat {@link ais.ui.util.MyButtonTabbox}) — Ringkasan (statistik umum, tab pertama yang
 * langsung dimuat karena event seleksi tab bawaan ZK 5 tidak konsisten), Pertemuan
 * ({@link DashboardTimelinePertemuan}, linimasa pertemuan kelas), Ujian
 * ({@link RekapitulasiUjianHelper}), Tugas ({@link RekapitulasiTugasHelper}), Tugas Kelompok
 * ({@link RekapitulasiTugasKelompokHelper}), dan Materi (materi ajar yang diunggah guru). Data
 * disaring sesuai user yang sedang login ({@link #tbmuser}) dan sekolah konteks
 * ({@link SekolahUtil#getSekolah()}).
 */
public class ElearningSekolah extends MyWindow {

	private static final long serialVersionUID = 1L;
	private Tbmuser tbmuser;

	/** Membuka dasbor e-Learning untuk user yang sedang login; menampilkan pesan galat ramah bila gagal dimuat. */
	public ElearningSekolah() {
		super();
		tbmuser = Common.getCurrentUser();
		try {
			init();
		} catch (Exception e) {
			showError(e);
			showMessage(this, "e-Learning belum dapat ditampilkan",
					"Silakan buka kembali menu e-Learning atau hubungi admin bila pesan ini masih muncul.", true);
		}
	}

	private void init() throws Exception {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setSizable(false);
		setVisible(true);
		setStyle("background:#f8fafc;border:0;min-height:720px;height:100%;overflow:hidden;box-sizing:border-box;");

		final Sekolah sekolah = SekolahUtil.getSekolah();

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setStyle("border:0;background:#f8fafc;min-height:720px;overflow:hidden;");
		borderlayout.setParent(this);

		Center center = new Center();
		center.setBorder("none");
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setStyle("border:0;background:#f8fafc;overflow:auto;padding:0;");
		center.setParent(borderlayout);

		Div wrapper = new Div();
		wrapper.setWidth("100%");
		wrapper.setHeight("100%");
		wrapper.setStyle("background:#f8fafc;min-height:700px;height:100%;overflow:auto;"
				+ "box-sizing:border-box;padding:0;margin:0;");
		wrapper.setParent(center);

		appendHeader(wrapper, sekolah);

		Div tabHost = new Div();
		tabHost.setWidth("100%");
		tabHost.setHeight("calc(100vh - 205px)");
		tabHost.setStyle("min-height:620px;background:#ffffff;border:1px solid #dbeafe;border-radius:14px;"
				+ "box-shadow:0 8px 22px rgba(15,23,42,0.08);overflow:hidden;box-sizing:border-box;");
		tabHost.setParent(wrapper);

		final MyButtonTabbox buttonTabbox = MyButtonTabbox.buat(tabHost, "100%", new int[] { 1 });
		final Div panelRingkasan = buttonTabbox.tambahTab(1, "Ringkasan", "/img/svg/table-report.svg");
		aturPanelUtama(panelRingkasan);

		final Div panelPertemuan = buttonTabbox.tambahTabLazy(2, "Pertemuan",
				"/img/svg/list-task.svg", new MyButtonTabbox.PemuatTab() {
			public void muat(Div parent) throws Exception {
				DashboardTimelinePertemuan dashboardTimelinePertemuan = new DashboardTimelinePertemuan(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
					}
				});
				dashboardTimelinePertemuan.setWidth("100%");
				dashboardTimelinePertemuan.setHeight("100%");
				parent.appendChild(dashboardTimelinePertemuan);
			}
		});
		aturPanelUtama(panelPertemuan);

		final Div panelUjian = buttonTabbox.tambahTabLazy(3, "Ujian", "/img/svg/user-edit.svg",
				new MyButtonTabbox.PemuatTab() {
			public void muat(Div parent) throws Exception {
				RekapitulasiUjianHelper.display(parent, tbmuser);
			}
		});
		aturPanelUtama(panelUjian);

		final Div panelTugas = buttonTabbox.tambahTabLazy(4, "Tugas", "/img/svg/task-line.svg",
				new MyButtonTabbox.PemuatTab() {
			public void muat(Div parent) throws Exception {
				RekapitulasiTugasHelper.display(parent, tbmuser);
			}
		});
		aturPanelUtama(panelTugas);

		final Div panelTugasKelompok = buttonTabbox.tambahTabLazy(5, "Tugas Kelompok",
				"/img/Document-scheduled-tasks-icon.png", new MyButtonTabbox.PemuatTab() {
			public void muat(Div parent) throws Exception {
				RekapitulasiTugasKelompokHelper.display(parent, tbmuser);
			}
		});
		aturPanelUtama(panelTugasKelompok);

		final Div panelMateri = buttonTabbox.tambahTabLazy(6, "Materi", "/img/svg/books-thin.svg",
				new MyButtonTabbox.PemuatTab() {
			public void muat(Div parent) throws Exception {
				loadMateri(parent);
			}
		});
		aturPanelUtama(panelMateri);

		// Tab pertama wajib terisi pada render awal; tidak bergantung pada event
		// onSelect/onClick Tabbox native yang tidak konsisten pada ZK 5.
		loadRingkasan(panelRingkasan, sekolah);
		buttonTabbox.pilih(1);
	}

	private void aturPanelUtama(Div panel) {
		if (panel != null) {
			panel.setHeight("100%");
			panel.setStyle("background:#ffffff;overflow:auto;padding:10px;box-sizing:border-box;"
					+ "min-height:560px;");
		}
	}

	private interface LazyLoader {
		void load(Component parent) throws Exception;
	}

	private MyTabConfig createTab(Tabs tabs, String label, String image) {
		MyTabConfig tab = new MyTabConfig(label, image);
		tab.setParent(tabs);
		tab.setStyle("font-size:12px;font-weight:bold;color:#334155;padding:7px 10px;white-space:nowrap;");
		return tab;
	}

	private Tabpanel createPanel(Tabpanels tabpanels) {
		Tabpanel panel = new ais.ui.util.MyTabpanel();
		panel.setParent(tabpanels);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setStyle("background:#ffffff;overflow:auto;padding:10px;box-sizing:border-box;min-height:820px;");
		return panel;
	}

	private void appendHeader(Component parent, Sekolah sekolah) {
		Div header = new Div();
		header.setWidth("100%");
		header.setStyle("background:linear-gradient(135deg,#eff6ff,#ffffff);border:1px solid #dbeafe;"
				+ "border-radius:14px;padding:12px 14px;margin-bottom:10px;box-sizing:border-box;"
				+ "box-shadow:0 6px 16px rgba(15,23,42,0.08);");
		header.setParent(parent);

		String nama = sekolah == null || sekolah.getNama() == null || sekolah.getNama().trim().isEmpty()
				? "e-Learning" : sekolah.getNama();
		Label title = new Label("e-Learning " + nama);
		title.setStyle("display:block;font-size:15px;font-weight:bold;color:#0f172a;margin-bottom:4px;");
		title.setParent(header);

		Label desc = new Label(ais.common.Common.getBahasaConfig("Lihat ringkasan belajar, pertemuan, ujian, tugas, dan materi dalam satu tempat."));
		desc.setStyle("display:block;font-size:12px;color:#475569;line-height:1.45;white-space:normal;");
		desc.setParent(header);
	}

	private void loadRingkasan(Component parent, Sekolah sekolah) {
		if (parent == null) {
			return;
		}
		Div progress = null;
		try {
			parent.getChildren().clear();
			progress = showProgress(parent, "Memuat ringkasan belajar", "Mengambil jadwal, pertemuan, dan aktivitas terbaru.");
			if (sekolah != null && sekolah.getId() != null) {
				RekapitulasiJadwalPelajaranHelper.display(parent, tbmuser, false);
			} else {
				RekapitulasiPerkuliahanHelper.display(parent, tbmuser, false);
			}
			finishAndDetachProgress(progress);
		} catch (Exception e) {
			showError(e);
			MainProgressHelper.errorProgressPanel(progress, "Ringkasan belum dapat ditampilkan.");
			parent.getChildren().clear();
			showMessage(parent, "Ringkasan belum dapat ditampilkan",
					"Data ringkasan belajar belum tersedia atau gagal dimuat. Silakan buka kembali menu ini.", true);
		}
	}

	private void bindLazyLoad(final MyTabConfig tab, final Component parent, final LazyLoader loader) {
		EventListener listener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (parent == null || loader == null || !parent.getChildren().isEmpty()) {
					return;
				}
				Div progress = null;
				try {
					progress = showProgress(parent, "Memuat " + tab.getLabel(), "Menyiapkan data " + tab.getLabel().toLowerCase() + ".");
					loader.load(parent);
					finishAndDetachProgress(progress);
				} catch (Exception e) {
					showError(e);
					MainProgressHelper.errorProgressPanel(progress, tab.getLabel() + " belum dapat ditampilkan.");
					parent.getChildren().clear();
					showMessage(parent, tab.getLabel() + " belum dapat ditampilkan",
							"Silakan buka tab ini kembali. Data lain tetap aman dan tidak berubah.", true);
				}
			}
		};
		tab.addEventListener("onClick", listener);
		tab.addEventListener("onSelect", listener);
	}

	private void loadMateri(Component parent) throws Exception {
		Tabbox tabboxSub = new Tabbox();
		tabboxSub.setParent(parent);
		tabboxSub.setWidth("100%");
		tabboxSub.setHeight("100%");
		tabboxSub.setStyle("border:1px solid #dbeafe;border-radius:12px;overflow:auto;min-height:780px;");

		Tabs tabsSub = new Tabs();
		tabsSub.setParent(tabboxSub);
		final MyTabConfig tabMateri = createTab(tabsSub, "Materi", null);
		final MyTabConfig tabVideo = createTab(tabsSub, "Video", null);
		final MyTabConfig tabAudio = createTab(tabsSub, "Audio", null);

		Tabpanels tabpanelsSub = new Tabpanels();
		tabpanelsSub.setParent(tabboxSub);
		tabpanelsSub.setWidth("100%");
		tabpanelsSub.setHeight("100%");

		final Tabpanel panelMateri = createPanel(tabpanelsSub);
		final Tabpanel panelVideo = createPanel(tabpanelsSub);
		final Tabpanel panelAudio = createPanel(tabpanelsSub);

		try {
			RekapitulasiMateriHelper.display(panelMateri, tbmuser);
		} catch (Exception e) {
			showError(e);
			panelMateri.getChildren().clear();
			showMessage(panelMateri, "Materi belum dapat ditampilkan", "Silakan buka kembali tab Materi.", true);
		}

		bindLazyLoad(tabVideo, panelVideo, new LazyLoader() {
			public void load(Component panel) throws Exception {
				RekapitulasiVideoHelper.display(panel, tbmuser);
			}
		});
		bindLazyLoad(tabAudio, panelAudio, new LazyLoader() {
			public void load(Component panel) throws Exception {
				RekapitulasiAudioHelper.display(panel, tbmuser);
			}
		});
		try {
			tabMateri.setSelected(true);
			tabboxSub.setSelectedIndex(0);
		} catch (Exception e) {
			showError(e);
		}
	}

	private Div showProgress(Component parent, String title, String description) {
		Div box = MainProgressHelper.createProgressPanel(title, description, 12);
		box.setParent(parent);
		return box;
	}

	private void finishAndDetachProgress(Div progress) {
		if (progress == null) {
			return;
		}
		MainProgressHelper.finishProgressPanel(progress, "Selesai", true);
		detachQuietly(progress);
	}

	private void detachQuietly(Component component) {
		if (component == null) {
			return;
		}
		try {
			component.detach();
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/ElearningSekolah.java:315");
		}
	}

	private void showMessage(Component parent, String title, String description, boolean warning) {
		if (parent == null) {
			return;
		}
		Div box = new Div();
		box.setWidth("100%");
		box.setStyle("background:#ffffff;border:1px solid " + (warning ? "#fecaca" : "#bfdbfe")
				+ ";border-radius:14px;padding:14px 16px;box-sizing:border-box;"
				+ "box-shadow:0 8px 18px rgba(15,23,42,0.08);");
		box.setParent(parent);
		Label label = new Label(title == null ? "Informasi" : title);
		label.setStyle("display:block;font-size:14px;font-weight:bold;color:#0f172a;margin-bottom:5px;");
		label.setParent(box);
		Label desc = new Label(description == null ? "" : description);
		desc.setStyle("display:block;font-size:12px;color:#475569;line-height:1.45;white-space:normal;");
		desc.setParent(box);
	}

	private void showError(Exception e) {
		try {
			Common.tampilErrorJikaAdmin(e);
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/ElearningSekolah.java:340");
		}
	}
}
