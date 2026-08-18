package ais.action.master.ticket;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.crm.CrmLostReason;
import ais.database.model.crm.CrmPipelineType;
import ais.database.model.crm.CrmSalesTeam;
import ais.database.model.crm.CrmSalesTeamMember;
import ais.database.model.crm.CrmStage;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>CrmKonfigurasiHelper — konfigurasi modul CRM (tab "CRM" di {@link TicketKonfigurasiAction})</h3>
 *
 * <p>CRUD untuk {@link CrmPipelineType} (+ {@link CrmStage} bersarang), {@link CrmLostReason}, dan
 * {@link CrmSalesTeam} (+ {@link CrmSalesTeamMember} bersarang). Mengikuti pola CRUD kategori pada
 * {@link TicketKonfigurasiAction#bukaKelolaKategori()}. Java 1.7.</p>
 */
public final class CrmKonfigurasiHelper {

	private CrmKonfigurasiHelper() {
	}

	public static void display(Component parent) {
		if (parent == null) {
			return;
		}
		Common.clear(parent);
		MyDiv root = new MyDiv();
		root.setStyle("padding:10px;box-sizing:border-box;");
		root.setParent(parent);

		root.appendChild(new ais.ui.util.MyHtml(
				"<div style='font-size:16px;font-weight:800;color:#0f172a;margin-bottom:4px;'>Konfigurasi CRM</div>"
						+ "<div style='font-size:12px;color:#64748b;margin-bottom:10px;'>Jenis pipeline & tahap bisa "
						+ "dikonfigurasi bebas — dipakai untuk admisi calon mahasiswa/siswa, kemitraan/vendor, donasi "
						+ "alumni, atau kebutuhan lain.</div>"));

		Hbox aksi = new Hbox();
		aksi.setStyle("gap:8px;");
		aksi.setParent(root);

		MyToolbarbuttonConfig btnPipeline = new MyToolbarbuttonConfig("Kelola Jenis Pipeline & Tahap", "/img/svg/list.svg");
		btnPipeline.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				bukaKelolaPipeline();
			}
		});
		btnPipeline.setParent(aksi);

		MyToolbarbuttonConfig btnLost = new MyToolbarbuttonConfig("Kelola Alasan Kalah", "/img/svg/list.svg");
		btnLost.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				bukaKelolaLostReason();
			}
		});
		btnLost.setParent(aksi);

		MyToolbarbuttonConfig btnTim = new MyToolbarbuttonConfig("Kelola Tim Penjualan", "/img/svg/list.svg");
		btnTim.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				bukaKelolaTim();
			}
		});
		btnTim.setParent(aksi);
	}

	// ==================================================================================
	// CrmPipelineType + CrmStage
	// ==================================================================================

	private static void bukaKelolaPipeline() throws InterruptedException {
		final MyWindow window = new MyWindow("Kelola Jenis Pipeline & Tahap", "normal", true);
		window.setWidth("640px");
		window.setHeight("80%");
		window.setContentStyle("overflow:auto;");
		window.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Hbox toolbar = new Hbox();
		toolbar.setStyle("padding:6px;");
		toolbar.setParent(window);
		final MyDiv listHost = new MyDiv();
		listHost.setWidth("100%");

		MyToolbarbuttonConfig tambah = new MyToolbarbuttonConfig("Tambah Jenis Pipeline", "/img/svg/form-one.svg");
		tambah.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				bukaFormPipeline(new CrmPipelineType(), listHost);
			}
		});
		tambah.setParent(toolbar);

		listHost.setParent(window);
		muatPipeline(listHost);

		window.setVisible(true);
		window.onModal();
	}

	private static void muatPipeline(final Component listHost) {
		try {
			Common.clear(listHost);
			Session session = HibernateUtil.currentSession();
			@SuppressWarnings("unchecked")
			List<CrmPipelineType> list = session.createCriteria(CrmPipelineType.class).addOrder(Order.asc("nomorUrut"))
					.addOrder(Order.asc("id")).list();
			if (list == null || list.isEmpty()) {
				listHost.appendChild(new Label("Belum ada jenis pipeline. Tambahkan minimal satu untuk mulai memakai CRM."));
				return;
			}
			for (final CrmPipelineType pt : list) {
				MyDiv baris = new MyDiv();
				baris.setStyle("border:1px solid #e2e8f0;border-radius:8px;padding:8px 10px;margin-bottom:6px;");
				baris.setParent(listHost);

				MyDiv header = new MyDiv();
				header.setStyle("display:flex;align-items:center;justify-content:space-between;gap:8px;");
				header.setParent(baris);
				header.appendChild(new ais.ui.util.MyHtml("<b>" + esc(safe(pt.getNama())) + "</b>"));

				Hbox aksi = new Hbox();
				aksi.setStyle("gap:4px;");
				MyToolbarbuttonConfig tahap = new MyToolbarbuttonConfig("Kelola Tahap", "/img/svg/list.svg");
				tahap.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						bukaKelolaStage(pt);
					}
				});
				tahap.setParent(aksi);
				MyToolbarbuttonConfig edit = new MyToolbarbuttonConfig("Ubah", "/img/edit.gif");
				edit.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						bukaFormPipeline(pt, listHost);
					}
				});
				edit.setParent(aksi);
				MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("Hapus", "/img/delete.gif");
				hapus.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						Session s = HibernateUtil.currentSession();
						CrmPipelineType d = (CrmPipelineType) s.get(CrmPipelineType.class, pt.getId());
						if (d != null) {
							s.delete(d);
							s.flush();
						}
						muatPipeline(listHost);
					}
				});
				hapus.setParent(aksi);
				header.appendChild(aksi);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static void bukaFormPipeline(final CrmPipelineType pt, final Component listHost) throws InterruptedException {
		final MyWindow w = new MyWindow(pt.getId() == null ? "Tambah Jenis Pipeline" : "Ubah Jenis Pipeline", "normal", true);
		w.setWidth("420px");
		w.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(w);
		Columns cols = new Columns();
		cols.setParent(grid);
		new Column("", null, "120px").setParent(cols);
		new Column().setParent(cols);
		Rows rows = new Rows();
		rows.setParent(grid);

		final Textbox nama = new Textbox(pt.getNama());
		nama.setWidth("95%");
		baris(rows, "Nama *", nama);
		final Textbox keterangan = new Textbox(pt.getKeterangan());
		keterangan.setMultiline(true);
		keterangan.setRows(2);
		keterangan.setWidth("95%");
		baris(rows, "Keterangan", keterangan);
		final Intbox urut = new Intbox(pt.getNomorUrut());
		urut.setWidth("70px");
		baris(rows, "Nomor Urut", urut);

		Hbox footer = new Hbox();
		footer.setStyle("padding:8px 4px;");
		footer.setParent(w);
		tombolBatal(footer, w);
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (nama.getValue() == null || nama.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Nama jenis pipeline wajib diisi.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				Session s = HibernateUtil.currentSession();
				pt.setNama(nama.getValue().trim());
				pt.setKeterangan(keterangan.getValue());
				pt.setNomorUrut(urut.getValue());
				if (pt.getAktif() == null) {
					pt.setAktif(true);
				}
				Common.refreshSaveOrUpdate(s, pt);
				w.detach();
				muatPipeline(listHost);
			}
		});
		simpan.setParent(footer);

		w.setVisible(true);
		w.onModal();
	}

	private static void bukaKelolaStage(final CrmPipelineType pt) throws InterruptedException {
		final MyWindow window = new MyWindow("Tahap Pipeline — " + safe(pt.getNama()), "normal", true);
		window.setWidth("560px");
		window.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Hbox toolbar = new Hbox();
		toolbar.setStyle("padding:6px;");
		toolbar.setParent(window);
		final MyDiv listHost = new MyDiv();
		listHost.setWidth("100%");

		MyToolbarbuttonConfig tambah = new MyToolbarbuttonConfig("Tambah Tahap", "/img/svg/form-one.svg");
		tambah.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				CrmStage baru = new CrmStage();
				baru.setPipelineType(pt);
				bukaFormStage(baru, listHost);
			}
		});
		tambah.setParent(toolbar);

		listHost.setParent(window);
		muatStage(pt, listHost);

		window.setVisible(true);
		window.onModal();
	}

	private static void muatStage(final CrmPipelineType pt, final Component listHost) {
		try {
			Common.clear(listHost);
			Session session = HibernateUtil.currentSession();
			@SuppressWarnings("unchecked")
			List<CrmStage> list = session.createCriteria(CrmStage.class).add(Restrictions.eq("pipelineType", pt))
					.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("id")).list();
			if (list == null || list.isEmpty()) {
				listHost.appendChild(new Label("Belum ada tahap. Tambahkan mis. Baru / Kualifikasi / Penawaran / Menang / Kalah."));
				return;
			}
			for (final CrmStage st : list) {
				MyDiv baris = new MyDiv();
				baris.setStyle("display:flex;align-items:center;justify-content:space-between;gap:8px;"
						+ "border:1px solid #e2e8f0;border-radius:8px;padding:8px 10px;margin-bottom:6px;");
				baris.setParent(listHost);
				String warna = st.getWarna() == null || st.getWarna().trim().isEmpty() ? "#0ea5e9" : st.getWarna();
				String tag = Boolean.TRUE.equals(st.getIsWon()) ? " [Menang]" : (Boolean.TRUE.equals(st.getIsLost()) ? " [Kalah]" : "");
				baris.appendChild(new ais.ui.util.MyHtml(
						"<span style='display:inline-flex;align-items:center;gap:8px;'>"
								+ "<span style='width:14px;height:14px;border-radius:50%;background:" + esc(warna)
								+ ";display:inline-block;'></span><b>" + esc(safe(st.getNama())) + esc(tag) + "</b></span>"));
				Hbox aksi = new Hbox();
				MyToolbarbuttonConfig edit = new MyToolbarbuttonConfig("Ubah", "/img/edit.gif");
				edit.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						bukaFormStage(st, listHost);
					}
				});
				edit.setParent(aksi);
				MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("Hapus", "/img/delete.gif");
				hapus.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						Session s = HibernateUtil.currentSession();
						CrmStage d = (CrmStage) s.get(CrmStage.class, st.getId());
						if (d != null) {
							s.delete(d);
							s.flush();
						}
						muatStage(pt, listHost);
					}
				});
				hapus.setParent(aksi);
				baris.appendChild(aksi);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static void bukaFormStage(final CrmStage st, final Component listHost) throws InterruptedException {
		final MyWindow w = new MyWindow(st.getId() == null ? "Tambah Tahap" : "Ubah Tahap", "normal", true);
		w.setWidth("420px");
		w.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(w);
		Columns cols = new Columns();
		cols.setParent(grid);
		new Column("", null, "140px").setParent(cols);
		new Column().setParent(cols);
		Rows rows = new Rows();
		rows.setParent(grid);

		final Textbox nama = new Textbox(st.getNama());
		nama.setWidth("95%");
		baris(rows, "Nama *", nama);
		final Intbox urut = new Intbox(st.getNomorUrut());
		urut.setWidth("70px");
		baris(rows, "Nomor Urut", urut);
		final Intbox prob = new Intbox(st.getProbabilitasDefault());
		prob.setWidth("70px");
		baris(rows, "Probabilitas Default (%)", prob);
		final Textbox warna = new Textbox(st.getWarna());
		warna.setWidth("95%");
		warna.setTooltiptext("#0ea5e9");
		baris(rows, "Warna (hex)", warna);
		final Checkbox isWon = new Checkbox("Tahap ini = Menang (Won)");
		isWon.setChecked(Boolean.TRUE.equals(st.getIsWon()));
		baris(rows, "", isWon);
		final Checkbox isLost = new Checkbox("Tahap ini = Kalah (Lost)");
		isLost.setChecked(Boolean.TRUE.equals(st.getIsLost()));
		baris(rows, "", isLost);

		Hbox footer = new Hbox();
		footer.setStyle("padding:8px 4px;");
		footer.setParent(w);
		tombolBatal(footer, w);
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (nama.getValue() == null || nama.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Nama tahap wajib diisi.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				Session s = HibernateUtil.currentSession();
				st.setNama(nama.getValue().trim());
				st.setNomorUrut(urut.getValue());
				st.setProbabilitasDefault(prob.getValue());
				st.setWarna(warna.getValue());
				st.setIsWon(isWon.isChecked());
				st.setIsLost(isLost.isChecked());
				if (st.getAktif() == null) {
					st.setAktif(true);
				}
				Common.refreshSaveOrUpdate(s, st);
				w.detach();
				muatStage(st.getPipelineType(), listHost);
			}
		});
		simpan.setParent(footer);

		w.setVisible(true);
		w.onModal();
	}

	// ==================================================================================
	// CrmLostReason
	// ==================================================================================

	private static void bukaKelolaLostReason() throws InterruptedException {
		final MyWindow window = new MyWindow("Kelola Alasan Kalah", "normal", true);
		window.setWidth("480px");
		window.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Hbox toolbar = new Hbox();
		toolbar.setStyle("padding:6px;");
		toolbar.setParent(window);
		final MyDiv listHost = new MyDiv();
		listHost.setWidth("100%");

		MyToolbarbuttonConfig tambah = new MyToolbarbuttonConfig("Tambah Alasan", "/img/svg/form-one.svg");
		tambah.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				bukaFormLostReason(new CrmLostReason(), listHost);
			}
		});
		tambah.setParent(toolbar);

		listHost.setParent(window);
		muatLostReason(listHost);

		window.setVisible(true);
		window.onModal();
	}

	private static void muatLostReason(final Component listHost) {
		try {
			Common.clear(listHost);
			Session session = HibernateUtil.currentSession();
			@SuppressWarnings("unchecked")
			List<CrmLostReason> list = session.createCriteria(CrmLostReason.class).addOrder(Order.asc("nomorUrut"))
					.addOrder(Order.asc("id")).list();
			if (list == null || list.isEmpty()) {
				listHost.appendChild(new Label("Belum ada alasan kalah."));
				return;
			}
			for (final CrmLostReason lr : list) {
				MyDiv baris = new MyDiv();
				baris.setStyle("display:flex;align-items:center;justify-content:space-between;gap:8px;"
						+ "border:1px solid #e2e8f0;border-radius:8px;padding:8px 10px;margin-bottom:6px;");
				baris.setParent(listHost);
				baris.appendChild(new Label(safe(lr.getNama())));
				Hbox aksi = new Hbox();
				MyToolbarbuttonConfig edit = new MyToolbarbuttonConfig("Ubah", "/img/edit.gif");
				edit.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						bukaFormLostReason(lr, listHost);
					}
				});
				edit.setParent(aksi);
				MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("Hapus", "/img/delete.gif");
				hapus.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						Session s = HibernateUtil.currentSession();
						CrmLostReason d = (CrmLostReason) s.get(CrmLostReason.class, lr.getId());
						if (d != null) {
							s.delete(d);
							s.flush();
						}
						muatLostReason(listHost);
					}
				});
				hapus.setParent(aksi);
				baris.appendChild(aksi);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static void bukaFormLostReason(final CrmLostReason lr, final Component listHost) throws InterruptedException {
		final MyWindow w = new MyWindow(lr.getId() == null ? "Tambah Alasan Kalah" : "Ubah Alasan Kalah", "normal", true);
		w.setWidth("420px");
		w.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(w);
		Columns cols = new Columns();
		cols.setParent(grid);
		new Column("", null, "120px").setParent(cols);
		new Column().setParent(cols);
		Rows rows = new Rows();
		rows.setParent(grid);

		final Textbox nama = new Textbox(lr.getNama());
		nama.setWidth("95%");
		baris(rows, "Nama *", nama);
		final Textbox keterangan = new Textbox(lr.getKeterangan());
		keterangan.setMultiline(true);
		keterangan.setRows(2);
		keterangan.setWidth("95%");
		baris(rows, "Keterangan", keterangan);
		final Intbox urut = new Intbox(lr.getNomorUrut());
		urut.setWidth("70px");
		baris(rows, "Nomor Urut", urut);

		Hbox footer = new Hbox();
		footer.setStyle("padding:8px 4px;");
		footer.setParent(w);
		tombolBatal(footer, w);
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (nama.getValue() == null || nama.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Nama alasan wajib diisi.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				Session s = HibernateUtil.currentSession();
				lr.setNama(nama.getValue().trim());
				lr.setKeterangan(keterangan.getValue());
				lr.setNomorUrut(urut.getValue());
				if (lr.getAktif() == null) {
					lr.setAktif(true);
				}
				Common.refreshSaveOrUpdate(s, lr);
				w.detach();
				muatLostReason(listHost);
			}
		});
		simpan.setParent(footer);

		w.setVisible(true);
		w.onModal();
	}

	// ==================================================================================
	// CrmSalesTeam + CrmSalesTeamMember
	// ==================================================================================

	private static void bukaKelolaTim() throws InterruptedException {
		final MyWindow window = new MyWindow("Kelola Tim Penjualan", "normal", true);
		window.setWidth("560px");
		window.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Hbox toolbar = new Hbox();
		toolbar.setStyle("padding:6px;");
		toolbar.setParent(window);
		final MyDiv listHost = new MyDiv();
		listHost.setWidth("100%");

		MyToolbarbuttonConfig tambah = new MyToolbarbuttonConfig("Tambah Tim", "/img/svg/form-one.svg");
		tambah.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				bukaFormTim(new CrmSalesTeam(), listHost);
			}
		});
		tambah.setParent(toolbar);

		listHost.setParent(window);
		muatTim(listHost);

		window.setVisible(true);
		window.onModal();
	}

	private static void muatTim(final Component listHost) {
		try {
			Common.clear(listHost);
			Session session = HibernateUtil.currentSession();
			@SuppressWarnings("unchecked")
			List<CrmSalesTeam> list = session.createCriteria(CrmSalesTeam.class).addOrder(Order.asc("nama")).list();
			if (list == null || list.isEmpty()) {
				listHost.appendChild(new Label("Belum ada tim penjualan."));
				return;
			}
			for (final CrmSalesTeam tim : list) {
				MyDiv baris = new MyDiv();
				baris.setStyle("display:flex;align-items:center;justify-content:space-between;gap:8px;"
						+ "border:1px solid #e2e8f0;border-radius:8px;padding:8px 10px;margin-bottom:6px;");
				baris.setParent(listHost);
				baris.appendChild(new ais.ui.util.MyHtml("<b>" + esc(safe(tim.getNama())) + "</b>"));
				Hbox aksi = new Hbox();
				aksi.setStyle("gap:4px;");
				MyToolbarbuttonConfig anggota = new MyToolbarbuttonConfig("Kelola Anggota", "/img/svg/list.svg");
				anggota.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						bukaKelolaAnggota(tim);
					}
				});
				anggota.setParent(aksi);
				MyToolbarbuttonConfig edit = new MyToolbarbuttonConfig("Ubah", "/img/edit.gif");
				edit.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						bukaFormTim(tim, listHost);
					}
				});
				edit.setParent(aksi);
				MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("Hapus", "/img/delete.gif");
				hapus.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						Session s = HibernateUtil.currentSession();
						CrmSalesTeam d = (CrmSalesTeam) s.get(CrmSalesTeam.class, tim.getId());
						if (d != null) {
							s.delete(d);
							s.flush();
						}
						muatTim(listHost);
					}
				});
				hapus.setParent(aksi);
				baris.appendChild(aksi);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static void bukaFormTim(final CrmSalesTeam tim, final Component listHost) throws InterruptedException {
		final MyWindow w = new MyWindow(tim.getId() == null ? "Tambah Tim" : "Ubah Tim", "normal", true);
		w.setWidth("420px");
		w.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(w);
		Columns cols = new Columns();
		cols.setParent(grid);
		new Column("", null, "120px").setParent(cols);
		new Column().setParent(cols);
		Rows rows = new Rows();
		rows.setParent(grid);

		final Textbox nama = new Textbox(tim.getNama());
		nama.setWidth("95%");
		baris(rows, "Nama *", nama);
		final Textbox keterangan = new Textbox(tim.getKeterangan());
		keterangan.setMultiline(true);
		keterangan.setRows(2);
		keterangan.setWidth("95%");
		baris(rows, "Keterangan", keterangan);

		Hbox footer = new Hbox();
		footer.setStyle("padding:8px 4px;");
		footer.setParent(w);
		tombolBatal(footer, w);
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (nama.getValue() == null || nama.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Nama tim wajib diisi.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				Session s = HibernateUtil.currentSession();
				tim.setNama(nama.getValue().trim());
				tim.setKeterangan(keterangan.getValue());
				if (tim.getAktif() == null) {
					tim.setAktif(true);
				}
				Common.refreshSaveOrUpdate(s, tim);
				w.detach();
				muatTim(listHost);
			}
		});
		simpan.setParent(footer);

		w.setVisible(true);
		w.onModal();
	}

	private static void bukaKelolaAnggota(final CrmSalesTeam tim) throws InterruptedException {
		final MyWindow window = new MyWindow("Anggota Tim — " + safe(tim.getNama()), "normal", true);
		window.setWidth("560px");
		window.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		MyDiv tambahBox = new MyDiv();
		tambahBox.setStyle("display:flex;align-items:center;gap:6px;flex-wrap:wrap;padding:6px;"
				+ "border-bottom:1px solid #e2e8f0;margin-bottom:6px;");
		tambahBox.setParent(window);

		final Textbox cariUser = new Textbox();
		cariUser.setWidth("160px");
		cariUser.setTooltiptext("Ketik sebagian nama/userId lalu klik Cari");
		final Combobox hasilUser = new Combobox();
		hasilUser.setReadonly(true);
		hasilUser.setWidth("200px");
		final Combobox peran = new Combobox();
		peran.setReadonly(true);
		for (String k : CrmSalesTeamMember.PERAN_TIM_DATA.keySet()) {
			org.zkoss.zul.Comboitem ci = new org.zkoss.zul.Comboitem(CrmSalesTeamMember.PERAN_TIM_DATA.get(k));
			ci.setValue(k);
			ci.setParent(peran);
		}
		peran.setSelectedIndex(0);

		final MyDiv listHost = new MyDiv();
		listHost.setWidth("100%");
		listHost.setStyle("padding:6px;");

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				hasilUser.getChildren().clear();
				if (cariUser.getValue() == null || cariUser.getValue().trim().isEmpty()) {
					return;
				}
				Session session = HibernateUtil.currentSession();
				@SuppressWarnings("unchecked")
				List<Tbmuser> hasil = session.createCriteria(Tbmuser.class)
						.add(Restrictions.or(Restrictions.ilike("userNama", cariUser.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("userId", cariUser.getValue().trim(), MatchMode.ANYWHERE)))
						.setMaxResults(20).list();
				for (Tbmuser u : hasil) {
					org.zkoss.zul.Comboitem ci = new org.zkoss.zul.Comboitem(safe(u.getUserNama()) + " (" + safe(u.getUserId()) + ")");
					ci.setValue(u);
					ci.setParent(hasilUser);
				}
				if (hasilUser.getItemCount() > 0) {
					hasilUser.setSelectedIndex(0);
				}
			}
		});

		MyToolbarbuttonConfig tambahAnggota = new MyToolbarbuttonConfig("Tambah Anggota", "/img/svg/form-one.svg");
		tambahAnggota.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (hasilUser.getSelectedItem() == null || hasilUser.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Cari & pilih pengguna terlebih dahulu.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				Session session = HibernateUtil.currentSession();
				CrmSalesTeamMember m = new CrmSalesTeamMember(tim);
				m.setAnggota((Tbmuser) hasilUser.getSelectedItem().getValue());
				m.setPeranTim((String) peran.getSelectedItem().getValue());
				m.setAktif(true);
				Common.refreshSaveOrUpdate(session, m);
				muatAnggota(tim, listHost);
			}
		});

		tambahBox.appendChild(new Label("Cari:"));
		tambahBox.appendChild(cariUser);
		cari.setParent(tambahBox);
		tambahBox.appendChild(hasilUser);
		tambahBox.appendChild(new Label("Peran:"));
		tambahBox.appendChild(peran);
		tambahAnggota.setParent(tambahBox);

		listHost.setParent(window);
		muatAnggota(tim, listHost);

		window.setVisible(true);
		window.onModal();
	}

	private static void muatAnggota(final CrmSalesTeam tim, final Component listHost) {
		try {
			Common.clear(listHost);
			Session session = HibernateUtil.currentSession();
			@SuppressWarnings("unchecked")
			List<CrmSalesTeamMember> list = session.createCriteria(CrmSalesTeamMember.class)
					.add(Restrictions.eq("salesTeam", tim)).addOrder(Order.asc("id")).list();
			if (list == null || list.isEmpty()) {
				listHost.appendChild(new Label("Belum ada anggota."));
				return;
			}
			for (final CrmSalesTeamMember m : list) {
				MyDiv baris = new MyDiv();
				baris.setStyle("display:flex;align-items:center;justify-content:space-between;gap:8px;"
						+ "border:1px solid #e2e8f0;border-radius:8px;padding:6px 10px;margin-bottom:6px;");
				baris.setParent(listHost);
				String label = m.getAnggota() == null ? "-" : safe(m.getAnggota().getUserNama());
				baris.appendChild(new ais.ui.util.MyHtml(esc(label) + " <span style='color:#64748b;font-size:11px;'>("
						+ esc(m.getPeranTimLabel()) + ")</span>"));
				MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("Hapus", "/img/delete.gif");
				hapus.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						Session s = HibernateUtil.currentSession();
						CrmSalesTeamMember d = (CrmSalesTeamMember) s.get(CrmSalesTeamMember.class, m.getId());
						if (d != null) {
							s.delete(d);
							s.flush();
						}
						muatAnggota(tim, listHost);
					}
				});
				baris.appendChild(hapus);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	// ==================================================================================
	// Util kecil
	// ==================================================================================

	private static void baris(Rows rows, String label, Component field) {
		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(label));
		row.appendChild(field);
	}

	private static void tombolBatal(Hbox footer, final MyWindow w) {
		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				w.detach();
			}
		});
		batal.setParent(footer);
	}

	private static String safe(String s) {
		return s == null ? "" : s;
	}

	private static String esc(String s) {
		return ais.ui.util.DashboardUiKit.esc(s == null ? "" : s);
	}
}
