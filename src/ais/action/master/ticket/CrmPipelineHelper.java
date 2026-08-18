package ais.action.master.ticket;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.crm.CrmActivity;
import ais.database.model.crm.CrmCatatan;
import ais.database.model.crm.CrmLead;
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
 * <h3>CrmPipelineHelper — Pipeline CRM (tab "Pipeline CRM" di {@link TicketingAction})</h3>
 *
 * <p>Papan Kanban per {@link CrmStage} (drag & drop native ZK, tanpa lib pihak-ketiga), daftar
 * "Lead Masuk" yang belum dikualifikasi, popup detail (konversi ke peluang, aktivitas, catatan,
 * menang/kalah), dan tampilan daftar (Grid) alternatif. Fase 1 modul CRM. Java 1.7.</p>
 */
public final class CrmPipelineHelper {

	private static final String DND = "crmlead";

	private CrmPipelineHelper() {
	}

	// ==================================================================================
	// Layar utama
	// ==================================================================================

	public static void display(Component parent, final Tbmuser tbmuser) {
		if (parent == null) {
			return;
		}
		Common.clear(parent);

		final MyDiv root = new MyDiv();
		root.setStyle("min-height:200px;padding:6px;box-sizing:border-box;");
		root.setParent(parent);

		org.zkoss.zul.Toolbar toolbar = new org.zkoss.zul.Toolbar();
		toolbar.setStyle("display:flex;flex-wrap:wrap;align-items:center;gap:6px;padding:6px;");
		toolbar.setParent(root);

		final Combobox filterPipeline = new Combobox();
		filterPipeline.setWidth("180px");
		try {
			Common.insertComboDanSemua(filterPipeline, "nama", "Semua Pipeline", CrmPipelineType.class);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) CrmPipelineHelper.display-pipeline");
		}

		final Combobox filterTim = new Combobox();
		filterTim.setWidth("160px");
		try {
			Common.insertComboDanSemua(filterTim, "nama", "Semua Tim", CrmSalesTeam.class);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) CrmPipelineHelper.display-tim");
		}

		final Textbox cari = new Textbox();
		cari.setCols(14);

		final MyDiv contentHost = new MyDiv();
		contentHost.setStyle("width:100%;");
		final boolean[] modeList = { false };

		MyToolbarbuttonConfig btnBuat = new MyToolbarbuttonConfig("Lead Baru", "/img/svg/form-one.svg");
		btnBuat.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				bukaFormLeadBaru(tbmuser, new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						render(contentHost, tbmuser, filterPipeline, filterTim, cari, modeList);
					}
				});
			}
		});
		btnBuat.setParent(toolbar);

		toolbar.appendChild(new Label("Pipeline:"));
		toolbar.appendChild(filterPipeline);
		toolbar.appendChild(new Label("Tim:"));
		toolbar.appendChild(filterTim);
		toolbar.appendChild(new Label("Cari:"));
		toolbar.appendChild(cari);

		EventListener reloadListener = new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				render(contentHost, tbmuser, filterPipeline, filterTim, cari, modeList);
			}
		};
		MyToolbarbuttonConfig btnCari = new MyToolbarbuttonConfig("Cari / Refresh", "/img/svg/search.svg");
		btnCari.addEventListener("onClick", reloadListener);
		btnCari.setParent(toolbar);
		cari.addEventListener("onOK", reloadListener);
		filterPipeline.addEventListener("onChange", reloadListener);
		filterTim.addEventListener("onChange", reloadListener);

		MyToolbarbuttonConfig btnKanban = new MyToolbarbuttonConfig("Tampilan Kanban", "/img/svg/list.svg");
		btnKanban.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				modeList[0] = false;
				render(contentHost, tbmuser, filterPipeline, filterTim, cari, modeList);
			}
		});
		btnKanban.setParent(toolbar);
		MyToolbarbuttonConfig btnList = new MyToolbarbuttonConfig("Tampilan Daftar", "/img/svg/list.svg");
		btnList.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				modeList[0] = true;
				render(contentHost, tbmuser, filterPipeline, filterTim, cari, modeList);
			}
		});
		btnList.setParent(toolbar);

		contentHost.setParent(root);
		render(contentHost, tbmuser, filterPipeline, filterTim, cari, modeList);
	}

	private static CrmPipelineType nilaiPipeline(Combobox combo) {
		if (combo == null || combo.getSelectedItem() == null) {
			return null;
		}
		Object v = combo.getSelectedItem().getValue();
		return v instanceof CrmPipelineType ? (CrmPipelineType) v : null;
	}

	private static CrmSalesTeam nilaiTim(Combobox combo) {
		if (combo == null || combo.getSelectedItem() == null) {
			return null;
		}
		Object v = combo.getSelectedItem().getValue();
		return v instanceof CrmSalesTeam ? (CrmSalesTeam) v : null;
	}

	private static void render(final Component contentHost, final Tbmuser tbmuser, final Combobox filterPipeline,
			final Combobox filterTim, final Textbox cari, final boolean[] modeList) {
		try {
			Common.clear(contentHost);
			Session session = HibernateUtil.currentSession();

			CrmPipelineType pipeline = nilaiPipeline(filterPipeline);
			CrmSalesTeam tim = nilaiTim(filterTim);
			String kataKunci = cari == null ? "" : cari.getValue();

			if (pipeline == null) {
				@SuppressWarnings("unchecked")
				List<CrmPipelineType> semuaPipeline = session.createCriteria(CrmPipelineType.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("id")).list();
				if (semuaPipeline == null || semuaPipeline.isEmpty()) {
					contentHost.appendChild(new ais.ui.util.MyHtml(
							"<div style='padding:18px;color:#64748b;'>Belum ada jenis pipeline dikonfigurasi. "
									+ "Buka menu <b>Konfigurasi Ticketing</b> &rarr; tab CRM &rarr; \"Kelola Jenis Pipeline & Tahap\" "
									+ "untuk menambahkan jenis pipeline (mis. Admisi Mahasiswa Baru, Kemitraan/Vendor, dst).</div>"));
					return;
				}
				pipeline = semuaPipeline.get(0);
			}

			final EventListener reload = new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					render(contentHost, tbmuser, filterPipeline, filterTim, cari, modeList);
				}
			};

			if (modeList[0]) {
				renderList(contentHost, tbmuser, pipeline, tim, kataKunci, reload);
			} else {
				renderKanban(contentHost, tbmuser, pipeline, tim, kataKunci, reload);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	// ==================================================================================
	// Papan Kanban
	// ==================================================================================

	@SuppressWarnings("unchecked")
	private static void renderKanban(final Component contentHost, final Tbmuser tbmuser, final CrmPipelineType pipeline,
			CrmSalesTeam tim, String cari, final EventListener reload) {
		Session session = HibernateUtil.currentSession();

		// --- Lead Masuk (belum dikualifikasi) ---
		Criteria criteriaLead = session.createCriteria(CrmLead.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("pipelineType", pipeline)).add(Restrictions.eq("tipe", CrmLead.TIPE_LEAD));
		terapkanFilter(criteriaLead, tim, cari);
		List<CrmLead> leadMasuk = criteriaLead.addOrder(Order.desc("id")).setMaxResults(200).list();

		MyDiv leadBox = new MyDiv();
		leadBox.setStyle("margin-bottom:10px;");
		leadBox.setParent(contentHost);
		leadBox.appendChild(new ais.ui.util.MyHtml("<div style='font-size:13px;font-weight:700;color:#0f172a;margin-bottom:6px;'>"
				+ "Lead Masuk <span style='color:#64748b;font-weight:400;'>(belum dikualifikasi &middot; " + leadMasuk.size()
				+ ")</span></div>"));
		Hbox leadRow = new Hbox();
		leadRow.setStyle("overflow-x:auto;width:100%;gap:8px;padding-bottom:4px;");
		leadRow.setParent(leadBox);
		if (leadMasuk.isEmpty()) {
			leadRow.appendChild(new ais.ui.util.MyHtml("<div style='color:#94a3b8;padding:6px;'>Tidak ada lead baru.</div>"));
		}
		for (final CrmLead l : leadMasuk) {
			leadRow.appendChild(buatKartuLead(l, tbmuser, reload));
		}

		// --- Tahap Pipeline (Kanban Peluang) ---
		List<CrmStage> stages = session.createCriteria(CrmStage.class).add(Restrictions.eq("pipelineType", pipeline))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("id")).list();
		if (stages.isEmpty()) {
			contentHost.appendChild(new ais.ui.util.MyHtml(
					"<div style='padding:18px;color:#64748b;'>Belum ada tahap untuk pipeline \"" + esc(safe(pipeline.getNama()))
							+ "\". Buka Konfigurasi CRM &rarr; \"Kelola Jenis Pipeline & Tahap\" &rarr; Kelola Tahap.</div>"));
			return;
		}

		Hbox board = new Hbox();
		board.setStyle("overflow-x:auto;width:100%;align-items:flex-start;gap:8px;");
		board.setParent(contentHost);

		for (final CrmStage stage : stages) {
			final Div kolom = new Div();
			kolom.setStyle("min-width:230px;max-width:260px;background:#f8fafc;border-radius:10px;padding:8px;"
					+ "flex:0 0 auto;");
			kolom.setAttribute("stageId", stage.getId());
			kolom.setDroppable(DND);
			board.appendChild(kolom);

			Criteria criteriaStage = session.createCriteria(CrmLead.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("pipelineType", pipeline)).add(Restrictions.eq("stage", stage));
			terapkanFilter(criteriaStage, tim, cari);
			List<CrmLead> leadsStage = criteriaStage.addOrder(Order.desc("id")).setMaxResults(200).list();

			String warna = stage.getWarna() == null || stage.getWarna().trim().isEmpty() ? "#0ea5e9" : stage.getWarna();
			kolom.appendChild(new ais.ui.util.MyHtml("<div style='display:flex;align-items:center;justify-content:space-between;"
					+ "margin-bottom:6px;'><b style='color:#0f172a;font-size:12px;'>"
					+ "<span style='width:10px;height:10px;border-radius:50%;background:" + esc(warna)
					+ ";display:inline-block;margin-right:4px;'></span>" + esc(safe(stage.getNama())) + "</b>"
					+ "<span style='font-size:11px;color:#64748b;'>" + leadsStage.size() + "</span></div>"));

			final Div dropZone = new Div();
			dropZone.setStyle("min-height:60px;");
			dropZone.setAttribute("stageId", stage.getId());
			dropZone.setDroppable(DND);
			dropZone.setParent(kolom);
			dropZone.addEventListener("onDrop", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					DropEvent de = (DropEvent) e;
					Object dragged = de.getDragged();
					if (!(dragged instanceof Component)) {
						return;
					}
					Object leadIdAttr = ((Component) dragged).getAttribute("leadId");
					if (!(leadIdAttr instanceof Long)) {
						return;
					}
					pindahTahap((Long) leadIdAttr, stage.getId(), pipeline, tbmuser, reload);
				}
			});

			for (final CrmLead l : leadsStage) {
				dropZone.appendChild(buatKartuLead(l, tbmuser, reload));
			}
		}
	}

	private static void terapkanFilter(Criteria criteria, CrmSalesTeam tim, String cari) {
		if (tim != null) {
			criteria.add(Restrictions.eq("salesTeam", tim));
		}
		if (cari != null && cari.trim().length() > 0) {
			criteria.add(Restrictions.or(Restrictions.ilike("judul", cari.trim(), MatchMode.ANYWHERE),
					Restrictions.or(Restrictions.ilike("kontakNama", cari.trim(), MatchMode.ANYWHERE),
							Restrictions.ilike("kontakInstansi", cari.trim(), MatchMode.ANYWHERE))));
		}
	}

	/** Kartu satu lead/peluang — draggable (kecuali di daftar "Lead Masuk"), klik → detail. */
	private static Component buatKartuLead(final CrmLead l, final Tbmuser tbmuser, final EventListener reload) {
		MyDiv kartu = new MyDiv();
		kartu.setStyle("border:1px solid #e2e8f0;border-radius:8px;padding:8px 10px;margin-bottom:6px;"
				+ "background:#ffffff;cursor:pointer;min-width:200px;");
		kartu.setAttribute("leadId", l.getId());
		if (CrmLead.TIPE_PELUANG.equals(l.getTipe())) {
			kartu.setDraggable(DND);
		}
		int overdue = hitungAktivitasOverdue(l.getId());
		String badgeOverdue = overdue > 0
				? "<span style='background:#dc2626;color:#fff;font-size:10px;font-weight:700;padding:2px 6px;border-radius:999px;margin-left:4px;'>"
						+ overdue + " terlambat</span>"
				: "";
		String html = "<div style='font-weight:700;color:#0f172a;font-size:12px;'>" + esc(safe(l.getJudul())) + badgeOverdue
				+ "</div>"
				+ (l.getKontakInstansi() == null || l.getKontakInstansi().trim().isEmpty() ? "" : "<div style='font-size:11px;color:#64748b;margin-top:2px;'>"
						+ esc(l.getKontakInstansi()) + "</div>")
				+ "<div style='font-size:11px;color:#334155;margin-top:4px;display:flex;justify-content:space-between;'>"
				+ "<span>" + esc(l.getDitugaskanUser() == null ? "-" : safe(l.getDitugaskanUser().getUserNama())) + "</span>"
				+ "<span style='font-weight:700;'>" + formatRupiah(l.getNilaiEstimasi()) + "</span></div>";
		kartu.appendChild(new ais.ui.util.MyHtml(html));
		kartu.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				bukaDetail(l.getId(), tbmuser, reload);
			}
		});
		return kartu;
	}

	private static int hitungAktivitasOverdue(Long leadId) {
		try {
			Session session = HibernateUtil.currentSession();
			Number n = (Number) session.createCriteria(CrmActivity.class).add(Restrictions.eq("lead.id", leadId))
					.add(Restrictions.eq("status", CrmActivity.STATUS_BELUM_DIMULAI))
					.add(Restrictions.lt("targetDate", new Date())).setProjection(org.hibernate.criterion.Projections.rowCount())
					.uniqueResult();
			return n == null ? 0 : n.intValue();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) CrmPipelineHelper.hitungAktivitasOverdue");
			return 0;
		}
	}

	/** Pindahkan lead ke tahap baru (drag & drop) — otomatis proses gerbang Menang/Kalah. */
	private static void pindahTahap(final Long leadId, Long stageBaruId, CrmPipelineType pipeline, final Tbmuser tbmuser,
			final EventListener onSelesai) throws Exception {
		Session session = HibernateUtil.currentSession();
		final CrmLead lead = (CrmLead) session.get(CrmLead.class, leadId);
		CrmStage stageBaru = (CrmStage) session.get(CrmStage.class, stageBaruId);
		if (lead == null || stageBaru == null) {
			return;
		}
		if (Boolean.TRUE.equals(stageBaru.getIsLost())) {
			bukaFormLost(lead, stageBaru, onSelesai);
			return;
		}
		lead.setStage(stageBaru);
		if (Boolean.TRUE.equals(stageBaru.getIsWon())) {
			lead.setStatusMenangKalah(CrmLead.STATUS_WON);
			lead.setTanggalDitutup(new Date());
		} else {
			// Digeser ke tahap terbuka biasa — buka kembali bila sebelumnya ditutup.
			lead.setStatusMenangKalah(CrmLead.STATUS_OPEN);
			lead.setTanggalDitutup(null);
			lead.setLostReason(null);
			lead.setCatatanKalah(null);
		}
		Common.refreshUpdate(session, lead);
		if (onSelesai != null) {
			onSelesai.onEvent(new Event("onSelesai"));
		}
	}

	private static void bukaFormLost(final CrmLead lead, final CrmStage stageLost, final EventListener onSelesai)
			throws InterruptedException {
		final MyWindow w = new MyWindow("Tandai Kalah — " + safe(lead.getJudul()), "normal", true);
		w.setWidth("420px");
		w.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(w);
		Columns cols = new Columns();
		cols.setParent(grid);
		new Column("", null, "120px").setParent(cols);
		new Column().setParent(cols);
		Rows rows = new Rows();
		rows.setParent(grid);

		final Combobox alasan = new Combobox();
		alasan.setReadonly(true);
		try {
			Common.insertComboDanSemua(alasan, "nama", CrmLostReason.class);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) CrmPipelineHelper.bukaFormLost-alasan");
		}
		baris(rows, "Alasan Kalah *", alasan);
		final Textbox catatan = new Textbox();
		catatan.setMultiline(true);
		catatan.setRows(3);
		catatan.setWidth("95%");
		baris(rows, "Catatan", catatan);

		Hbox footer = new Hbox();
		footer.setStyle("padding:8px 4px;");
		footer.setParent(w);
		tombolBatal(footer, w);
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (alasan.getSelectedItem() == null || !(alasan.getSelectedItem().getValue() instanceof CrmLostReason)) {
					MyMessageboxConfig.show("Alasan kalah wajib dipilih.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				Session s = HibernateUtil.currentSession();
				CrmLead l = (CrmLead) s.get(CrmLead.class, lead.getId());
				l.setStage(stageLost);
				l.setStatusMenangKalah(CrmLead.STATUS_LOST);
				l.setLostReason((CrmLostReason) alasan.getSelectedItem().getValue());
				l.setCatatanKalah(catatan.getValue());
				l.setTanggalDitutup(new Date());
				Common.refreshUpdate(s, l);
				w.detach();
				if (onSelesai != null) {
					onSelesai.onEvent(new Event("onSelesai"));
				}
			}
		});
		simpan.setParent(footer);

		w.setVisible(true);
		w.onModal();
	}

	// ==================================================================================
	// Tampilan Daftar
	// ==================================================================================

	@SuppressWarnings("unchecked")
	private static void renderList(final Component contentHost, final Tbmuser tbmuser, CrmPipelineType pipeline,
			CrmSalesTeam tim, String cari, final EventListener reload) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(CrmLead.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("pipelineType", pipeline));
		terapkanFilter(criteria, tim, cari);
		List<CrmLead> leads = criteria.addOrder(Order.desc("id")).setMaxResults(500).list();

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(contentHost);
		Columns cols = new Columns();
		cols.setParent(grid);
		new Column("Judul").setParent(cols);
		new Column("Tipe", null, "80px").setParent(cols);
		new Column("Tahap", null, "120px").setParent(cols);
		new Column("Tim", null, "130px").setParent(cols);
		new Column("PIC", null, "140px").setParent(cols);
		new Column("Estimasi", null, "120px").setParent(cols);
		new Column("Status", null, "90px").setParent(cols);
		Rows rows = new Rows();
		rows.setParent(grid);
		if (leads.isEmpty()) {
			contentHost.appendChild(new ais.ui.util.MyHtml("<div style='padding:12px;color:#94a3b8;'>Tidak ada data.</div>"));
		}
		for (final CrmLead l : leads) {
			Row r = new Row();
			r.setStyle("cursor:pointer;");
			r.setParent(rows);
			r.appendChild(new Label(safe(l.getJudul())));
			r.appendChild(new Label(CrmLead.TIPE_PELUANG.equals(l.getTipe()) ? "Peluang" : "Lead"));
			r.appendChild(new Label(l.getStage() == null ? "-" : safe(l.getStage().getNama())));
			r.appendChild(new Label(l.getSalesTeam() == null ? "-" : safe(l.getSalesTeam().getNama())));
			r.appendChild(new Label(l.getDitugaskanUser() == null ? "-" : safe(l.getDitugaskanUser().getUserNama())));
			r.appendChild(new Label(formatRupiah(l.getNilaiEstimasi())));
			r.appendChild(new Label(labelStatus(l.getStatusMenangKalah())));
			r.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					bukaDetail(l.getId(), tbmuser, reload);
				}
			});
		}
	}

	// ==================================================================================
	// Form Lead Baru
	// ==================================================================================

	private static void bukaFormLeadBaru(final Tbmuser tbmuser, final EventListener onSelesai) throws InterruptedException {
		final MyWindow window = new MyWindow("Lead Baru", "normal", true);
		window.setWidth("640px");
		window.setContentStyle("overflow:auto;");
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(window);
		Columns cols = new Columns();
		cols.setParent(grid);
		new Column("", null, "160px").setParent(cols);
		new Column().setParent(cols);
		Rows rows = new Rows();
		rows.setParent(grid);

		final Combobox pipelineType = new Combobox();
		pipelineType.setReadonly(true);
		try {
			Common.insertComboDanSemua(pipelineType, "nama", CrmPipelineType.class);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) CrmPipelineHelper.bukaFormLeadBaru-pipeline");
		}
		baris(rows, "Jenis Pipeline *", pipelineType);

		final Textbox judul = new Textbox();
		judul.setWidth("95%");
		judul.setTooltiptext("Kosongkan untuk memakai nama kontak + instansi secara otomatis.");
		baris(rows, "Judul", judul);

		final Textbox kontakNama = new Textbox();
		kontakNama.setWidth("95%");
		baris(rows, "Nama Kontak", kontakNama);
		final Textbox kontakInstansi = new Textbox();
		kontakInstansi.setWidth("95%");
		baris(rows, "Instansi", kontakInstansi);
		final Textbox kontakEmail = new Textbox();
		kontakEmail.setWidth("95%");
		baris(rows, "Email", kontakEmail);
		final Textbox kontakTelepon = new Textbox();
		kontakTelepon.setWidth("95%");
		baris(rows, "Telepon", kontakTelepon);

		final Textbox sumber = new Textbox();
		sumber.setWidth("95%");
		sumber.setTooltiptext("mis. Website, Rujukan, Pameran, Media Sosial");
		baris(rows, "Sumber", sumber);

		final Combobox salesTeam = new Combobox();
		salesTeam.setReadonly(true);
		try {
			Common.insertComboDanSemua(salesTeam, "nama", "- Belum ditugaskan -", CrmSalesTeam.class);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) CrmPipelineHelper.bukaFormLeadBaru-tim");
		}
		baris(rows, "Tim Penjualan", salesTeam);

		final Combobox ditugaskanUser = new Combobox();
		ditugaskanUser.setReadonly(true);
		isiPicKosong(ditugaskanUser);
		baris(rows, "Ditugaskan Ke", ditugaskanUser);
		salesTeam.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				isiPicDariTim(ditugaskanUser, nilaiTim(salesTeam));
			}
		});

		final Decimalbox nilaiEstimasi = new Decimalbox();
		nilaiEstimasi.setWidth("140px");
		baris(rows, "Nilai Estimasi (Rp)", nilaiEstimasi);
		final Datebox tanggalTutup = new Datebox();
		baris(rows, "Target Tanggal Tutup", tanggalTutup);

		Hbox footer = new Hbox();
		footer.setStyle("padding:8px 4px;");
		footer.setParent(window);
		tombolBatal(footer, window);
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (pipelineType.getSelectedItem() == null || !(pipelineType.getSelectedItem().getValue() instanceof CrmPipelineType)) {
					MyMessageboxConfig.show("Jenis pipeline wajib dipilih.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				String judulFinal = judul.getValue() == null || judul.getValue().trim().isEmpty()
						? (safe(kontakNama.getValue()) + (kontakInstansi.getValue() == null || kontakInstansi.getValue().trim().isEmpty()
								? "" : " - " + kontakInstansi.getValue().trim())).trim()
						: judul.getValue().trim();
				if (judulFinal.isEmpty()) {
					MyMessageboxConfig.show("Isi Judul atau minimal Nama Kontak.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				Session session = HibernateUtil.currentSession();
				CrmLead lead = new CrmLead();
				lead.setTipe(CrmLead.TIPE_LEAD);
				lead.setPipelineType((CrmPipelineType) pipelineType.getSelectedItem().getValue());
				lead.setJudul(judulFinal);
				lead.setKontakNama(kontakNama.getValue());
				lead.setKontakInstansi(kontakInstansi.getValue());
				lead.setKontakEmail(kontakEmail.getValue());
				lead.setKontakTelepon(kontakTelepon.getValue());
				lead.setSumber(sumber.getValue());
				lead.setSalesTeam(nilaiTim(salesTeam));
				if (ditugaskanUser.getSelectedItem() != null && ditugaskanUser.getSelectedItem().getValue() instanceof Tbmuser) {
					lead.setDitugaskanUser((Tbmuser) ditugaskanUser.getSelectedItem().getValue());
				}
				lead.setNilaiEstimasi(nilaiEstimasi.getValue());
				lead.setTanggalTutupDiharapkan(tanggalTutup.getValue());
				lead.setStatusMenangKalah(CrmLead.STATUS_OPEN);
				lead.setTanggalDibuat(new Date());
				lead.setAktif(true);
				Common.refreshSaveOrUpdate(session, lead);
				CrmNotifikasi.leadDitugaskan(lead);
				window.detach();
				if (onSelesai != null) {
					onSelesai.onEvent(new Event("onSelesai"));
				}
			}
		});
		simpan.setParent(footer);

		window.setVisible(true);
		window.onModal();
	}

	private static void isiPicKosong(Combobox combo) {
		combo.getChildren().clear();
		Comboitem ci = new Comboitem("- Belum ditugaskan -");
		ci.setValue(null);
		ci.setParent(combo);
		combo.setSelectedIndex(0);
	}

	@SuppressWarnings("unchecked")
	private static void isiPicDariTim(Combobox combo, CrmSalesTeam tim) {
		isiPicKosong(combo);
		if (tim == null) {
			return;
		}
		try {
			Session session = HibernateUtil.currentSession();
			List<CrmSalesTeamMember> anggota = session.createCriteria(CrmSalesTeamMember.class)
					.add(Restrictions.eq("salesTeam", tim)).addOrder(Order.asc("id")).list();
			for (CrmSalesTeamMember m : anggota) {
				if (m.getAnggota() == null) {
					continue;
				}
				Comboitem ci = new Comboitem(safe(m.getAnggota().getUserNama()) + " (" + m.getPeranTimLabel() + ")");
				ci.setValue(m.getAnggota());
				ci.setParent(combo);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) CrmPipelineHelper.isiPicDariTim");
		}
	}

	// ==================================================================================
	// Detail Lead/Peluang
	// ==================================================================================

	private static void bukaDetail(final Long leadId, final Tbmuser tbmuser, final EventListener onSelesai)
			throws InterruptedException {
		final MyWindow window = new MyWindow("Detail Prospek", "normal", true);
		window.setWidth("760px");
		window.setHeight("88%");
		window.setContentStyle("overflow:auto;");
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		final Vbox isi = new Vbox();
		isi.setWidth("100%");
		isi.setStyle("padding:10px;");
		isi.setParent(window);

		renderDetailBody(isi, leadId, tbmuser, window, onSelesai);

		Hbox footer = new Hbox();
		footer.setStyle("padding:8px 4px;");
		footer.setParent(window);
		MyToolbarbuttonConfig tutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		tutup.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				window.detach();
				if (onSelesai != null) {
					onSelesai.onEvent(new Event("onSelesai"));
				}
			}
		});
		tutup.setParent(footer);

		window.setVisible(true);
		window.onModal();
	}

	@SuppressWarnings("unchecked")
	private static void renderDetailBody(final Vbox isi, final Long leadId, final Tbmuser tbmuser, final MyWindow window,
			final EventListener onSelesai) {
		try {
			Common.clear(isi);
			Session session = HibernateUtil.currentSession();
			final CrmLead lead = (CrmLead) session.get(CrmLead.class, leadId);
			if (lead == null) {
				isi.appendChild(new Label("Data tidak ditemukan."));
				return;
			}
			final EventListener refresh = new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					renderDetailBody(isi, leadId, tbmuser, window, onSelesai);
				}
			};

			String warnaStatus = CrmLead.STATUS_WON.equals(lead.getStatusMenangKalah()) ? "#16a34a"
					: (CrmLead.STATUS_LOST.equals(lead.getStatusMenangKalah()) ? "#dc2626" : "#0ea5e9");
			isi.appendChild(new ais.ui.util.MyHtml(
					"<div style='padding:12px;border-radius:10px;background:#0f172a;color:#fff;'>"
							+ "<div style='font-size:18px;font-weight:800;'>" + esc(safe(lead.getJudul())) + "</div>"
							+ "<div style='font-size:12px;opacity:.9;margin-top:4px;'>"
							+ esc(CrmLead.TIPE_PELUANG.equals(lead.getTipe()) ? "Peluang" : "Lead") + " &middot; "
							+ esc(lead.getPipelineType() == null ? "-" : safe(lead.getPipelineType().getNama()))
							+ (lead.getStage() == null ? "" : " &middot; Tahap: " + esc(safe(lead.getStage().getNama()))) + "</div>"
							+ "<span style='display:inline-block;margin-top:6px;font-size:11px;font-weight:700;padding:3px 9px;"
							+ "border-radius:999px;color:#fff;background:" + warnaStatus + ";'>"
							+ esc(labelStatus(lead.getStatusMenangKalah())) + "</span></div>"));

			// --- Aksi utama (konversi / menang / kalah) ---
			MyDiv aksiBox = new MyDiv();
			aksiBox.setStyle("display:flex;gap:8px;flex-wrap:wrap;margin:8px 0;");
			aksiBox.setParent(isi);
			if (CrmLead.TIPE_LEAD.equals(lead.getTipe())) {
				MyToolbarbuttonConfig konversi = new MyToolbarbuttonConfig("Konversi ke Peluang", "/img/svg/form-one.svg");
				konversi.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						bukaFormKonversi(lead, refresh);
					}
				});
				konversi.setParent(aksiBox);
			} else if (CrmLead.STATUS_OPEN.equals(lead.getStatusMenangKalah())) {
				MyToolbarbuttonConfig menang = new MyToolbarbuttonConfig("Tandai Menang", "/img/svg/form-one.svg");
				menang.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						Session s = HibernateUtil.currentSession();
						CrmLead l = (CrmLead) s.get(CrmLead.class, lead.getId());
						CrmStage stageWon = (CrmStage) s.createCriteria(CrmStage.class)
								.add(Restrictions.eq("pipelineType", l.getPipelineType())).add(Restrictions.eq("isWon", true))
								.setMaxResults(1).uniqueResult();
						if (stageWon != null) {
							l.setStage(stageWon);
						}
						l.setStatusMenangKalah(CrmLead.STATUS_WON);
						l.setTanggalDitutup(new Date());
						Common.refreshUpdate(s, l);
						refresh.onEvent(new Event("onSelesai"));
					}
				});
				menang.setParent(aksiBox);
				MyToolbarbuttonConfig kalah = new MyToolbarbuttonConfig("Tandai Kalah", "/img/cancel.gif");
				kalah.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						Session s = HibernateUtil.currentSession();
						CrmStage stageLost = (CrmStage) s.createCriteria(CrmStage.class)
								.add(Restrictions.eq("pipelineType", lead.getPipelineType())).add(Restrictions.eq("isLost", true))
								.setMaxResults(1).uniqueResult();
						bukaFormLost(lead, stageLost, refresh);
					}
				});
				kalah.setParent(aksiBox);
			}

			// --- Info & edit ---
			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(isi);
			Columns cols = new Columns();
			cols.setParent(grid);
			new Column("", null, "150px").setParent(cols);
			new Column().setParent(cols);
			Rows rows = new Rows();
			rows.setParent(grid);

			final Textbox kontakNama = new Textbox(lead.getKontakNama());
			kontakNama.setWidth("95%");
			baris(rows, "Nama Kontak", kontakNama);
			final Textbox kontakInstansi = new Textbox(lead.getKontakInstansi());
			kontakInstansi.setWidth("95%");
			baris(rows, "Instansi", kontakInstansi);
			final Textbox kontakEmail = new Textbox(lead.getKontakEmail());
			kontakEmail.setWidth("95%");
			baris(rows, "Email", kontakEmail);
			final Textbox kontakTelepon = new Textbox(lead.getKontakTelepon());
			kontakTelepon.setWidth("95%");
			baris(rows, "Telepon", kontakTelepon);

			final Combobox salesTeam = new Combobox();
			salesTeam.setReadonly(true);
			try {
				Common.insertComboDanSemua(salesTeam, "nama", "- Belum ditugaskan -", CrmSalesTeam.class);
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) CrmPipelineHelper.renderDetailBody-tim");
			}
			pilihComboEntitas(salesTeam, lead.getSalesTeam());
			baris(rows, "Tim Penjualan", salesTeam);

			final Combobox ditugaskanUser = new Combobox();
			ditugaskanUser.setReadonly(true);
			isiPicDariTim(ditugaskanUser, lead.getSalesTeam());
			pilihComboEntitas(ditugaskanUser, lead.getDitugaskanUser());
			baris(rows, "Ditugaskan Ke", ditugaskanUser);
			salesTeam.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					isiPicDariTim(ditugaskanUser, nilaiTim(salesTeam));
				}
			});

			final Decimalbox nilaiEstimasi = new Decimalbox(lead.getNilaiEstimasi());
			nilaiEstimasi.setWidth("140px");
			baris(rows, "Nilai Estimasi (Rp)", nilaiEstimasi);
			final Intbox probabilitas = new Intbox(lead.getProbabilitas());
			probabilitas.setWidth("70px");
			baris(rows, "Probabilitas (%)", probabilitas);
			final Datebox tanggalTutup = new Datebox(lead.getTanggalTutupDiharapkan());
			baris(rows, "Target Tanggal Tutup", tanggalTutup);

			Hbox simpanBox = new Hbox();
			simpanBox.setStyle("padding:6px 0;");
			simpanBox.setParent(isi);
			MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan Perubahan", "/img/save.gif");
			simpan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					Session s = HibernateUtil.currentSession();
					CrmLead l = (CrmLead) s.get(CrmLead.class, lead.getId());
					Tbmuser ditugaskanLama = l.getDitugaskanUser();
					l.setKontakNama(kontakNama.getValue());
					l.setKontakInstansi(kontakInstansi.getValue());
					l.setKontakEmail(kontakEmail.getValue());
					l.setKontakTelepon(kontakTelepon.getValue());
					l.setSalesTeam(nilaiTim(salesTeam));
					Tbmuser picBaru = ditugaskanUser.getSelectedItem() != null
							&& ditugaskanUser.getSelectedItem().getValue() instanceof Tbmuser
									? (Tbmuser) ditugaskanUser.getSelectedItem().getValue() : null;
					l.setDitugaskanUser(picBaru);
					l.setNilaiEstimasi(nilaiEstimasi.getValue());
					l.setProbabilitas(probabilitas.getValue());
					l.setTanggalTutupDiharapkan(tanggalTutup.getValue());
					Common.refreshUpdate(s, l);
					if (picBaru != null && (ditugaskanLama == null || !picBaru.getUserId().equals(ditugaskanLama.getUserId()))) {
						CrmNotifikasi.leadDitugaskan(l);
					}
					MyMessageboxConfig.show("Perubahan tersimpan.", "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					refresh.onEvent(new Event("onSelesai"));
				}
			});
			simpan.setParent(simpanBox);

			// --- Aktivitas ---
			isi.appendChild(new ais.ui.util.MyHtml("<b>Aktivitas / Tindak Lanjut</b>"));
			final MyDiv aktivitasHost = new MyDiv();
			aktivitasHost.setWidth("100%");
			aktivitasHost.setParent(isi);
			muatAktivitas(aktivitasHost, lead, refresh);

			MyDiv tambahAktivitasBox = new MyDiv();
			tambahAktivitasBox.setStyle("border-top:1px solid #e2e8f0;margin-top:6px;padding-top:6px;display:flex;"
					+ "gap:6px;flex-wrap:wrap;align-items:center;");
			tambahAktivitasBox.setParent(isi);
			final Combobox jenisAktivitas = new Combobox();
			jenisAktivitas.setReadonly(true);
			for (String k : CrmActivity.JENIS_DATA.keySet()) {
				Comboitem ci = new Comboitem(CrmActivity.JENIS_DATA.get(k));
				ci.setValue(k);
				ci.setParent(jenisAktivitas);
			}
			jenisAktivitas.setSelectedIndex(0);
			final Textbox catatanAktivitas = new Textbox();
			catatanAktivitas.setWidth("200px");
			catatanAktivitas.setTooltiptext("Catatan singkat aktivitas");
			final Datebox targetAktivitas = new Datebox(new Date());
			MyToolbarbuttonConfig tambahAktivitas = new MyToolbarbuttonConfig("Tambah Aktivitas", "/img/svg/form-one.svg");
			tambahAktivitas.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					Session s = HibernateUtil.currentSession();
					CrmLead l = (CrmLead) s.get(CrmLead.class, lead.getId());
					CrmActivity a = new CrmActivity(l);
					a.setJenis((String) jenisAktivitas.getSelectedItem().getValue());
					a.setCatatan(catatanAktivitas.getValue());
					a.setTargetDate(targetAktivitas.getValue());
					a.setStatus(CrmActivity.STATUS_BELUM_DIMULAI);
					a.setPicUser(l.getDitugaskanUser());
					a.setAktif(true);
					Common.refreshSaveOrUpdate(s, a);
					CrmNotifikasi.aktivitasDitugaskan(a);
					refresh.onEvent(new Event("onSelesai"));
				}
			});
			tambahAktivitasBox.appendChild(new Label("Jenis:"));
			tambahAktivitasBox.appendChild(jenisAktivitas);
			tambahAktivitasBox.appendChild(new Label("Target:"));
			tambahAktivitasBox.appendChild(targetAktivitas);
			tambahAktivitasBox.appendChild(catatanAktivitas);
			tambahAktivitas.setParent(tambahAktivitasBox);

			// --- Catatan / Timeline ---
			isi.appendChild(new ais.ui.util.MyHtml("<b>Catatan</b>"));
			final MyDiv catatanHost = new MyDiv();
			catatanHost.setWidth("100%");
			catatanHost.setParent(isi);
			muatCatatan(catatanHost, lead);

			MyDiv tambahCatatanBox = new MyDiv();
			tambahCatatanBox.setStyle("border-top:1px solid #e2e8f0;margin-top:6px;padding-top:6px;");
			tambahCatatanBox.setParent(isi);
			final Textbox catatanBaru = new Textbox();
			catatanBaru.setMultiline(true);
			catatanBaru.setRows(2);
			catatanBaru.setWidth("98%");
			catatanBaru.setParent(tambahCatatanBox);
			MyToolbarbuttonConfig kirimCatatan = new MyToolbarbuttonConfig("Tambah Catatan", "/img/svg/form-one.svg");
			kirimCatatan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					if (catatanBaru.getValue() == null || catatanBaru.getValue().trim().isEmpty()) {
						return;
					}
					Session s = HibernateUtil.currentSession();
					CrmLead l = (CrmLead) s.get(CrmLead.class, lead.getId());
					CrmCatatan c = new CrmCatatan(l);
					c.setIsi(catatanBaru.getValue().trim());
					c.setTanggal(new Date());
					if (tbmuser != null) {
						c.setUserId(tbmuser.getUserId());
						c.setNama(tbmuser.getUserNama());
					}
					Common.refreshSaveOrUpdate(s, c);
					catatanBaru.setValue("");
					muatCatatan(catatanHost, l);
				}
			});
			kirimCatatan.setParent(tambahCatatanBox);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static void bukaFormKonversi(final CrmLead lead, final EventListener onSelesai) throws InterruptedException {
		final MyWindow w = new MyWindow("Konversi ke Peluang", "normal", true);
		w.setWidth("420px");
		w.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(w);
		Columns cols = new Columns();
		cols.setParent(grid);
		new Column("", null, "120px").setParent(cols);
		new Column().setParent(cols);
		Rows rows = new Rows();
		rows.setParent(grid);

		final Combobox stage = new Combobox();
		stage.setReadonly(true);
		try {
			Session session = HibernateUtil.currentSession();
			@SuppressWarnings("unchecked")
			List<CrmStage> stages = session.createCriteria(CrmStage.class)
					.add(Restrictions.eq("pipelineType", lead.getPipelineType()))
					.add(Restrictions.or(Restrictions.isNull("isLost"), Restrictions.eq("isLost", false)))
					.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("id")).list();
			for (CrmStage st : stages) {
				Comboitem ci = new Comboitem(safe(st.getNama()));
				ci.setValue(st);
				ci.setParent(stage);
			}
			if (stage.getItemCount() > 0) {
				stage.setSelectedIndex(0);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) CrmPipelineHelper.bukaFormKonversi-stage");
		}
		baris(rows, "Tahap Awal *", stage);

		Hbox footer = new Hbox();
		footer.setStyle("padding:8px 4px;");
		footer.setParent(w);
		tombolBatal(footer, w);
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Konversi", "/img/save.gif");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (stage.getSelectedItem() == null || !(stage.getSelectedItem().getValue() instanceof CrmStage)) {
					MyMessageboxConfig.show("Tahap awal wajib dipilih. Jika belum ada tahap, tambahkan dulu di Konfigurasi CRM.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}
				Session s = HibernateUtil.currentSession();
				CrmLead l = (CrmLead) s.get(CrmLead.class, lead.getId());
				CrmStage stageBaru = (CrmStage) stage.getSelectedItem().getValue();
				l.setTipe(CrmLead.TIPE_PELUANG);
				l.setStage(stageBaru);
				if ((l.getProbabilitas() == null || l.getProbabilitas() == 0) && stageBaru.getProbabilitasDefault() != null) {
					l.setProbabilitas(stageBaru.getProbabilitasDefault());
				}
				l.setTanggalDikonversiPeluang(new Date());
				Common.refreshUpdate(s, l);
				w.detach();
				if (onSelesai != null) {
					onSelesai.onEvent(new Event("onSelesai"));
				}
			}
		});
		simpan.setParent(footer);

		w.setVisible(true);
		w.onModal();
	}

	@SuppressWarnings("unchecked")
	private static void muatAktivitas(Component host, CrmLead lead, final EventListener onUbah) {
		try {
			Common.clear(host);
			Session session = HibernateUtil.currentSession();
			List<CrmActivity> list = session.createCriteria(CrmActivity.class).add(Restrictions.eq("lead", lead))
					.addOrder(Order.asc("targetDate")).addOrder(Order.asc("id")).list();
			if (list == null || list.isEmpty()) {
				host.appendChild(new ais.ui.util.MyHtml("<div style='color:#94a3b8;padding:4px 2px;'>Belum ada aktivitas.</div>"));
				return;
			}
			for (final CrmActivity a : list) {
				boolean selesai = CrmActivity.STATUS_SELESAI.equals(a.getStatus());
				boolean terlambat = !selesai && a.getTargetDate() != null && a.getTargetDate().before(new Date());
				String warna = selesai ? "#16a34a" : (terlambat ? "#dc2626" : "#0ea5e9");
				MyDiv baris = new MyDiv();
				baris.setStyle("display:flex;align-items:center;justify-content:space-between;gap:8px;"
						+ "border-left:3px solid " + warna + ";background:#f8fafc;border-radius:6px;padding:6px 10px;margin-bottom:4px;");
				baris.setParent(host);
				String waktu = a.getTargetDate() == null ? "-" : Common.dateFormat5.get().format(a.getTargetDate());
				baris.appendChild(new ais.ui.util.MyHtml("<div style='font-size:11px;'><b>" + esc(CrmActivity.JENIS_DATA.get(a.getJenis()))
						+ "</b> &middot; " + esc(waktu) + (a.getPicUser() == null ? "" : " &middot; " + esc(safe(a.getPicUser().getUserNama())))
						+ (a.getCatatan() == null || a.getCatatan().trim().isEmpty() ? "" : "<div style='color:#334155;margin-top:2px;'>"
								+ esc(a.getCatatan()) + "</div>")
						+ "</div>"));
				if (!selesai) {
					MyToolbarbuttonConfig tandai = new MyToolbarbuttonConfig("Tandai Selesai", "/img/svg/form-one.svg");
					tandai.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event e) throws Exception {
							Session s = HibernateUtil.currentSession();
							CrmActivity x = (CrmActivity) s.get(CrmActivity.class, a.getId());
							x.setStatus(CrmActivity.STATUS_SELESAI);
							x.setTanggalSelesai(new Date());
							Common.refreshUpdate(s, x);
							if (onUbah != null) {
								onUbah.onEvent(new Event("onSelesai"));
							}
						}
					});
					baris.appendChild(tandai);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static void muatCatatan(Component host, CrmLead lead) {
		try {
			Common.clear(host);
			Session session = HibernateUtil.currentSession();
			List<CrmCatatan> list = session.createCriteria(CrmCatatan.class).add(Restrictions.eq("lead", lead))
					.addOrder(Order.asc("id")).list();
			if (list == null || list.isEmpty()) {
				host.appendChild(new ais.ui.util.MyHtml("<div style='color:#94a3b8;padding:6px 2px;'>Belum ada catatan.</div>"));
				return;
			}
			for (CrmCatatan c : list) {
				String waktu = c.getTanggal() == null ? "" : Common.dateFormat5.get().format(c.getTanggal());
				host.appendChild(new ais.ui.util.MyHtml(
						"<div style='background:#f1f5f9;border-radius:8px;padding:8px 10px;margin-bottom:6px;'>"
								+ "<div style='font-size:11px;color:#334155;'><b>" + esc(safe(c.getNama())) + "</b> &middot; "
								+ esc(waktu) + "</div>"
								+ "<div style='white-space:pre-wrap;color:#0f172a;margin-top:3px;'>" + esc(safe(c.getIsi())) + "</div></div>"));
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	// ==================================================================================
	// Util kecil
	// ==================================================================================

	private static void pilihComboEntitas(Combobox combo, Object entitas) {
		if (entitas == null) {
			return;
		}
		for (Object o : combo.getItems()) {
			Comboitem ci = (Comboitem) o;
			if (entitas.equals(ci.getValue())) {
				combo.setSelectedItem(ci);
				return;
			}
		}
	}

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

	private static String labelStatus(String status) {
		if (CrmLead.STATUS_WON.equals(status)) {
			return "Menang";
		}
		if (CrmLead.STATUS_LOST.equals(status)) {
			return "Kalah";
		}
		return "Terbuka";
	}

	private static String formatRupiah(BigDecimal v) {
		if (v == null) {
			v = BigDecimal.ZERO;
		}
		try {
			return "Rp " + String.format("%,.0f", v);
		} catch (Exception e) {
			return "Rp " + v.toString();
		}
	}

	private static String safe(String s) {
		return s == null ? "" : s;
	}

	private static String esc(String s) {
		return ais.ui.util.DashboardUiKit.esc(s == null ? "" : s);
	}
}
