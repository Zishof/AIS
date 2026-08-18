package ais.action.master.ticket;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.ticket.Ticket;
import ais.database.model.ticket.TicketKategori;
import ais.database.model.ticket.TicketKomentar;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>TicketingAction — modul Ticketing Management (monitoring dev &amp; implementasi sistem)</h3>
 *
 * <p>UI mandiri (dibangun di kode, dipasang lewat menu / {@link #display(Component, Tbmuser)}):</p>
 * <ul>
 *   <li>Daftar tiket TER-SCOPED sesuai posisi pengguna (pengaju sendiri / satuan kerja / hak akses),
 *       admin &amp; developer melihat semua.</li>
 *   <li>Buat tiket baru (kendala/permintaan modul/progress/interaksi).</li>
 *   <li>Detail tiket: info + kontrol pengelola (status/prioritas/progress/penanganan) + thread
 *       komentar (dev↔user &amp; user↔user, ada catatan internal) + lampiran (via {@link LampiranLain}).</li>
 * </ul>
 *
 * <p>Bersifat GENERAL: dapat dipakai pegawai/dosen/guru/siswa/mahasiswa/calon/ortu/vendor/umum.
 * Tiket {@code extends DataSop} sehingga siap diajukan lewat SOP/disposisi (integrasi engine SOP =
 * fase lanjutan). Java 1.7.</p>
 */
public class TicketingAction extends MyWindow {

	private static final long serialVersionUID = 3120250724010L;

	/** Jenis lampiran (dipakai LampiranLain). */
	public static final String LAMPIRAN_TIKET = "TICKET";
	public static final String LAMPIRAN_KOMENTAR = "TICKET_KOMENTAR";

	/** Konstruktor no-arg — dipanggil framework saat menu (url = nama kelas) diklik. */
	public TicketingAction() {
		super();
		try {
			setHeight("100%");
			setWidth("100%");
			setBorder("none");
			setClosable(false);
			setPosition("center");

			org.zkoss.zul.Borderlayout borderlayout = new org.zkoss.zul.Borderlayout();
			borderlayout.setParent(this);
			org.zkoss.zul.Center center = new org.zkoss.zul.Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);
			center.setAutoscroll(true);

			display(center, Common.getCurrentUser());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	// ==================================================================================
	// Util peran / scoping
	// ==================================================================================

	/** True bila pengguna adalah admin atau developer (boleh melihat & mengelola SEMUA tiket). */
	public static boolean bolehKelolaSemua(Tbmuser tbmuser) {
		try {
			if (Common.getApakahAdmin()) {
				return true;
			}
			String daftarDev = Common.getKonfigurasi("ticketing_role_developer", "").getNilai();
			if (daftarDev != null && daftarDev.trim().length() > 0 && tbmuser != null && tbmuser.hakAkses() != null
					&& tbmuser.hakAkses().getRoleId() != null) {
				return (","+daftarDev.replace(" ", "")+",").indexOf("," + tbmuser.hakAkses().getRoleId() + ",") >= 0;
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketingAction.bolehKelolaSemua");
		}
		return false;
	}

	/** Menentukan tipe pengguna pengaju secara otomatis. */
	public static String tipePengguna(Tbmuser tbmuser) {
		try {
			if (tbmuser == null) {
				return "Umum";
			}
			if (tbmuser.getMahasiswa() != null) {
				return "Mahasiswa";
			}
			if (tbmuser.getBiodataCalonMahasiswa() != null) {
				return "Calon Mahasiswa";
			}
			if (tbmuser.getSiswa() != null) {
				return "Siswa";
			}
			if (tbmuser.getCalonSiswa() != null) {
				return "Calon Siswa";
			}
			if (tbmuser.ambilDosen() != null) {
				return "Dosen";
			}
			if (tbmuser.ambilGuru() != null) {
				return "Guru";
			}
			if (tbmuser.getPegawai() != null) {
				return "Pegawai";
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketingAction.tipePengguna");
		}
		return "Umum";
	}

	/**
	 * Kriteria daftar tiket TER-SCOPED: pengelola melihat semua; selain itu hanya tiket yang
	 * diajukan sendiri, ditugaskan ke dirinya, dalam satuan kerja yang sama, atau hak akses (roleId)
	 * yang di-target.
	 */
	public static Criteria scopedCriteria(Session session, Tbmuser tbmuser) {
		return initCriteria(session, tbmuser);
	}

	private static Criteria initCriteria(Session session, Tbmuser tbmuser) {
		Criteria criteria = session.createCriteria(Ticket.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (!bolehKelolaSemua(tbmuser) && tbmuser != null) {
			org.hibernate.criterion.Disjunction or = Restrictions.disjunction();
			String uid = tbmuser.getUserId();
			if (uid != null) {
				or.add(Restrictions.eq("pengajuUserId", uid));
				or.add(Restrictions.eq("ditugaskanKeUserId", uid));
			}
			try {
				String roleId = tbmuser.hakAkses() == null ? null : tbmuser.hakAkses().getRoleId();
				if (roleId != null) {
					or.add(Restrictions.ilike("hakAksesTarget", "," + roleId + ",", MatchMode.ANYWHERE));
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketingAction.initCriteria-role");
			}
			try {
				SatuanKerja sk = tbmuser.ambilSatuanKerja();
				if (sk != null && sk.getId() != null) {
					or.add(Restrictions.eq("satuanKerja", sk));
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketingAction.initCriteria-sk");
			}
			criteria.add(or);
		}
		criteria.addOrder(Order.desc("id"));
		return criteria;
	}

	// ==================================================================================
	// Layar utama
	// ==================================================================================

	/**
	 * Layar utama modul — dua tab: "Tiket Dukungan" (konten asli modul Ticketing) dan
	 * "Pipeline CRM" (digabung sesuai keputusan produk: CRM adalah perluasan modul ini, bukan
	 * modul/menu terpisah — lihat {@link CrmPipelineHelper}).
	 */
	public static void display(Component parent, final Tbmuser tbmuser) {
		if (parent == null) {
			return;
		}
		Common.clear(parent);

		org.zkoss.zul.Tabbox tabbox = new org.zkoss.zul.Tabbox();
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");
		tabbox.setParent(parent);
		org.zkoss.zul.Tabs tabs = new org.zkoss.zul.Tabs();
		tabs.setParent(tabbox);
		new org.zkoss.zul.Tab("Tiket Dukungan").setParent(tabs);
		new org.zkoss.zul.Tab("Pipeline CRM").setParent(tabs);
		org.zkoss.zul.Tabpanels tabpanels = new org.zkoss.zul.Tabpanels();
		tabpanels.setParent(tabbox);
		org.zkoss.zul.Tabpanel panelTiket = new org.zkoss.zul.Tabpanel();
		ais.ui.util.ZkCompat.setFlex(panelTiket, true);
		panelTiket.setParent(tabpanels);
		org.zkoss.zul.Tabpanel panelCrm = new org.zkoss.zul.Tabpanel();
		ais.ui.util.ZkCompat.setFlex(panelCrm, true);
		panelCrm.setParent(tabpanels);

		displayTiket(panelTiket, tbmuser);
		CrmPipelineHelper.display(panelCrm, tbmuser);
	}

	private static void displayTiket(Component parent, final Tbmuser tbmuser) {
		final MyDiv root = new MyDiv();
		root.setStyle("min-height:200px;padding:6px;box-sizing:border-box;");
		root.setParent(parent);

		// --- Toolbar ---
		org.zkoss.zul.Toolbar toolbar = new org.zkoss.zul.Toolbar();
		toolbar.setStyle("display:flex;flex-wrap:wrap;align-items:center;gap:6px;padding:6px;");
		toolbar.setParent(root);

		final Textbox cari = new Textbox();
		cari.setCols(14);
		final Combobox filterStatus = new Combobox();
		filterStatus.setReadonly(true);
		isiComboString(filterStatus, new String[] { "Semua", Ticket.STATUS_BARU, Ticket.STATUS_DITINJAU,
				Ticket.STATUS_DIPROSES, Ticket.STATUS_MENUNGGU_RESPON, Ticket.STATUS_SELESAI, Ticket.STATUS_DITUTUP,
				Ticket.STATUS_DITOLAK });
		final Combobox filterTipe = new Combobox();
		filterTipe.setReadonly(true);
		isiComboString(filterTipe, new String[] { "Semua", Ticket.TIPE_KENDALA, Ticket.TIPE_PERMINTAAN,
				Ticket.TIPE_PROGRESS, Ticket.TIPE_INTERAKSI, Ticket.TIPE_LAINNYA });

		final MyDiv listHost = new MyDiv();
		listHost.setStyle("width:100%;");

		MyToolbarbuttonConfig btnBuat = new MyToolbarbuttonConfig("Buat Tiket", "/img/svg/form-one.svg");
		btnBuat.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (TicketKonfigurasiAction.wajibSop()) {
					// Gerbang WAJIB-SOP: pengajuan harus lewat engine disposisi (TicketFormSop), bukan
					// simpan langsung. Arahkan pengguna ke menu "Pengajuan SOP (Workflow)".
					MyMessageboxConfig.show(
							"Pengajuan tiket wajib melalui alur SOP. Silakan buka menu \"Pengajuan SOP (Workflow)\", "
									+ "pilih SOP \"Pengajuan Ticket / Kendala Sistem\", lalu isi & ajukan formulirnya.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}
				bukaFormBuat(tbmuser, new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						reload(listHost, tbmuser, cari.getValue(), nilaiCombo(filterStatus), nilaiCombo(filterTipe));
					}
				});
			}
		});
		btnBuat.setParent(toolbar);

		toolbar.appendChild(new Label("Cari:"));
		toolbar.appendChild(cari);
		toolbar.appendChild(new Label("Status:"));
		toolbar.appendChild(filterStatus);
		toolbar.appendChild(new Label("Tipe:"));
		toolbar.appendChild(filterTipe);

		MyToolbarbuttonConfig btnCari = new MyToolbarbuttonConfig("Cari / Refresh", "/img/svg/search.svg");
		EventListener reloadListener = new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				reload(listHost, tbmuser, cari.getValue(), nilaiCombo(filterStatus), nilaiCombo(filterTipe));
			}
		};
		btnCari.addEventListener("onClick", reloadListener);
		btnCari.setParent(toolbar);
		cari.addEventListener("onOK", reloadListener);
		filterStatus.addEventListener("onChange", reloadListener);
		filterTipe.addEventListener("onChange", reloadListener);

		listHost.setParent(root);
		reload(listHost, tbmuser, "", "Semua", "Semua");
	}

	private static void reload(Component listHost, Tbmuser tbmuser, String cari, String status, String tipe) {
		try {
			Common.clear(listHost);
			Session session = HibernateUtil.currentSession();
			Criteria criteria = initCriteria(session, tbmuser);
			if (cari != null && cari.trim().length() > 0) {
				criteria.add(Restrictions.or(Restrictions.ilike("judul", cari.trim(), MatchMode.ANYWHERE),
						Restrictions.or(Restrictions.ilike("deskripsi", cari.trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nomorTiket", cari.trim(), MatchMode.ANYWHERE))));
			}
			if (status != null && !"Semua".equals(status)) {
				criteria.add(Restrictions.eq("status", status));
			}
			if (tipe != null && !"Semua".equals(tipe)) {
				criteria.add(Restrictions.eq("tipe", tipe));
			}
			@SuppressWarnings("unchecked")
			List<Ticket> tickets = criteria.setMaxResults(300).list();

			if (tickets == null || tickets.isEmpty()) {
				Label kosong = new Label("Belum ada tiket.");
				kosong.setStyle("display:block;padding:18px;color:#64748b;");
				kosong.setParent(listHost);
				return;
			}
			for (final Ticket t : tickets) {
				listHost.appendChild(buatKartuTiket(t, tbmuser, listHost));
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Kartu ringkas satu tiket (klik → detail). */
	private static Component buatKartuTiket(final Ticket t, final Tbmuser tbmuser, final Component listHost) {
		MyDiv kartu = new MyDiv();
		kartu.setStyle("border:1px solid #e2e8f0;border-radius:10px;padding:10px 12px;margin-bottom:8px;"
				+ "background:#ffffff;cursor:pointer;");
		String warnaStatus = warnaStatus(t.getStatus());
		String html = "<div style='display:flex;justify-content:space-between;gap:8px;flex-wrap:wrap;'>"
				+ "<div style='min-width:0;'>"
				+ "<div style='font-weight:700;color:#0f172a;'>" + esc(safe(t.getNomorTiket())) + " &middot; "
				+ esc(safe(t.getJudul())) + "</div>"
				+ "<div style='font-size:11px;color:#64748b;margin-top:3px;'>"
				+ esc(safe(t.getTipe())) + " &middot; " + esc(safe(t.getPengajuNama())) + " ("
				+ esc(safe(t.getPengajuTipe())) + ") &middot; prioritas " + esc(safe(t.getPrioritas())) + "</div></div>"
				+ "<div style='text-align:right;white-space:nowrap;'>"
				+ "<span style='font-size:11px;font-weight:700;padding:3px 9px;border-radius:999px;color:#fff;background:"
				+ warnaStatus + ";'>" + esc(safe(t.getStatus())) + "</span>"
				+ "<div style='font-size:11px;color:#334155;margin-top:4px;'>Progress " + t.getProgress() + "%</div>"
				+ "</div></div>";
		kartu.appendChild(new ais.ui.util.MyHtml(html));
		kartu.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				bukaDetail(t.getId(), tbmuser, new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						reload(listHost, tbmuser, "", "Semua", "Semua");
					}
				});
			}
		});
		return kartu;
	}

	// ==================================================================================
	// Form Buat Tiket
	// ==================================================================================

	private static void bukaFormBuat(final Tbmuser tbmuser, final EventListener onSelesai) throws InterruptedException {
		final MyWindow window = new MyWindow("Buat Tiket Baru", "normal", true);
		window.setWidth("640px");
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(window);
		org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
		cols.setParent(grid);
		new org.zkoss.zul.Column("", null, "150px").setParent(cols);
		new org.zkoss.zul.Column().setParent(cols);
		org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
		rows.setParent(grid);

		final Textbox judul = new Textbox();
		judul.setWidth("95%");
		barisForm(rows, "Judul *", judul);

		final Textbox deskripsi = new Textbox();
		deskripsi.setMultiline(true);
		deskripsi.setRows(4);
		deskripsi.setWidth("95%");
		barisForm(rows, "Deskripsi", deskripsi);

		final Combobox tipe = new Combobox();
		tipe.setReadonly(true);
		isiComboString(tipe, new String[] { Ticket.TIPE_KENDALA, Ticket.TIPE_PERMINTAAN, Ticket.TIPE_PROGRESS,
				Ticket.TIPE_INTERAKSI, Ticket.TIPE_LAINNYA });
		tipe.setSelectedIndex(0);
		barisForm(rows, "Tipe", tipe);

		final Combobox prioritas = new Combobox();
		prioritas.setReadonly(true);
		isiComboString(prioritas, new String[] { Ticket.PRIORITAS_RENDAH, Ticket.PRIORITAS_SEDANG,
				Ticket.PRIORITAS_TINGGI, Ticket.PRIORITAS_KRITIS });
		prioritas.setSelectedIndex(1);
		barisForm(rows, "Prioritas", prioritas);

		final Combobox kategori = new Combobox();
		kategori.setReadonly(true);
		try {
			Common.insertComboDanSemua(kategori, "nama", TicketKategori.class);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketingAction.bukaFormBuat-kategori");
		}
		barisForm(rows, "Kategori", kategori);

		final Textbox modul = new Textbox();
		modul.setWidth("95%");
		barisForm(rows, "Modul terkait", modul);

		final Combobox satuanKerja = new Combobox();
		satuanKerja.setReadonly(true);
		try {
			Common.insertComboDanSemua(satuanKerja, "nama", SatuanKerja.class);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketingAction.bukaFormBuat-sk");
		}
		barisForm(rows, "Satuan Kerja tujuan", satuanKerja);

		Hbox footer = new Hbox();
		footer.setStyle("padding:8px 4px;");
		footer.setParent(window);
		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				window.detach();
			}
		});
		batal.setParent(footer);
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (judul.getValue() == null || judul.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Judul tiket wajib diisi.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				Session session = HibernateUtil.currentSession();
				Ticket t = new Ticket();
				t.setJudul(judul.getValue().trim());
				t.setDeskripsi(deskripsi.getValue());
				t.setTipe(nilaiCombo(tipe));
				t.setPrioritas(nilaiCombo(prioritas));
				t.setModul(modul.getValue());
				t.setStatus(Ticket.STATUS_BARU);
				t.setProgress(0);
				t.setAktif(true);
				t.setTanggalDibuat(new Date());
				if (kategori.getSelectedItem() != null && kategori.getSelectedItem().getValue() != null) {
					t.setTicketKategori((TicketKategori) kategori.getSelectedItem().getValue());
				}
				if (satuanKerja.getSelectedItem() != null && satuanKerja.getSelectedItem().getValue() != null) {
					t.setSatuanKerja((SatuanKerja) satuanKerja.getSelectedItem().getValue());
				}
				if (tbmuser != null) {
					t.setPengajuUserId(tbmuser.getUserId());
					t.setPengajuNama(tbmuser.getUserNama());
					t.setPengajuTipe(tipePengguna(tbmuser));
					try {
						if (tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null) {
							t.setHakAksesTarget("," + tbmuser.hakAkses().getRoleId() + ",");
						}
					} catch (Exception ex) {
						ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) TicketingAction.simpan-role");
					}
				}
				Common.refreshSaveOrUpdate(session, t);
				// Nomor tiket rapi setelah id tersedia.
				if (t.getId() != null && (t.getNomorTiket() == null || t.getNomorTiket().trim().isEmpty())) {
					t.setNomorTiket("TKT-" + t.getId());
					Common.refreshUpdate(session, t);
				}
				TicketNotifikasi.tiketBaru(t);
				// CATATAN (WAJIB-SOP): titik integrasi pengajuan ke engine SOP/disposisi diletakkan di
				// sini (buat DisposisiSop + AlurSop awal lalu t.setDisposisiSop(...)). Diimplementasikan
				// pada fase integrasi SOP; sementara tiket tersimpan berstatus "Baru".
				window.detach();
				if (onSelesai != null) {
					onSelesai.onEvent(new Event("onSelesai"));
				}
				MyMessageboxConfig.show("Tiket berhasil dibuat dengan nomor " + t.getNomorTiket() + ".", "Informasi",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			}
		});
		simpan.setParent(footer);

		window.setVisible(true);
		window.onModal();
	}

	// ==================================================================================
	// Detail tiket + komentar + lampiran + kontrol pengelola
	// ==================================================================================

	/** Buka detail tiket dari klik notifikasi lonceng (memakai pengguna yang sedang login). */
	public static void bukaDetailNotifikasi(Long ticketId) {
		try {
			if (ticketId == null) {
				return;
			}
			bukaDetail(ticketId, Common.getCurrentUser(), null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static void bukaDetail(final Long ticketId, final Tbmuser tbmuser, final EventListener onSelesai) throws InterruptedException {
		final Session session = HibernateUtil.currentSession();
		final Ticket t = (Ticket) session.get(Ticket.class, ticketId);
		if (t == null) {
			return;
		}
		final boolean kelola = bolehKelolaSemua(tbmuser)
				|| (tbmuser != null && tbmuser.getUserId() != null && tbmuser.getUserId().equals(t.getDitugaskanKeUserId()));

		final MyWindow window = new MyWindow("Detail Tiket " + safe(t.getNomorTiket()), "normal", true);
		window.setWidth("90%");
		window.setHeight("90%");
		window.setContentStyle("overflow:auto;");
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Vbox isi = new Vbox();
		isi.setWidth("100%");
		isi.setStyle("padding:10px;");
		isi.setParent(window);

		// Header info
		isi.appendChild(new ais.ui.util.MyHtml(
				"<div style='padding:12px;border-radius:10px;background:#0f172a;color:#fff;'>"
						+ "<div style='font-size:18px;font-weight:800;'>" + esc(safe(t.getJudul())) + "</div>"
						+ "<div style='font-size:12px;opacity:.9;margin-top:4px;'>" + esc(safe(t.getNomorTiket()))
						+ " &middot; " + esc(safe(t.getTipe())) + " &middot; oleh " + esc(safe(t.getPengajuNama()))
						+ " (" + esc(safe(t.getPengajuTipe())) + ")</div></div>"));
		if (t.getDeskripsi() != null && t.getDeskripsi().trim().length() > 0) {
			isi.appendChild(new ais.ui.util.MyHtml(
					"<div style='padding:10px 2px;white-space:pre-wrap;color:#0f172a;'>" + esc(t.getDeskripsi())
							+ "</div>"));
		}

		// Kontrol pengelola
		if (kelola) {
			MyDiv panelKelola = new MyDiv();
			panelKelola.setStyle("border:1px solid #e2e8f0;border-radius:10px;padding:10px;margin:6px 0;"
					+ "display:flex;flex-wrap:wrap;gap:10px;align-items:center;");
			panelKelola.setParent(isi);

			final Combobox statusC = new Combobox();
			statusC.setReadonly(true);
			isiComboString(statusC, new String[] { Ticket.STATUS_BARU, Ticket.STATUS_DITINJAU, Ticket.STATUS_DIPROSES,
					Ticket.STATUS_MENUNGGU_RESPON, Ticket.STATUS_SELESAI, Ticket.STATUS_DITUTUP, Ticket.STATUS_DITOLAK });
			pilihCombo(statusC, t.getStatus());

			final Combobox prioC = new Combobox();
			prioC.setReadonly(true);
			isiComboString(prioC, new String[] { Ticket.PRIORITAS_RENDAH, Ticket.PRIORITAS_SEDANG,
					Ticket.PRIORITAS_TINGGI, Ticket.PRIORITAS_KRITIS });
			pilihCombo(prioC, t.getPrioritas());

			final Intbox progressB = new Intbox(t.getProgress());
			progressB.setWidth("70px");

			panelKelola.appendChild(new Label("Status:"));
			panelKelola.appendChild(statusC);
			panelKelola.appendChild(new Label("Prioritas:"));
			panelKelola.appendChild(prioC);
			panelKelola.appendChild(new Label("Progress %:"));
			panelKelola.appendChild(progressB);

			MyToolbarbuttonConfig simpanKelola = new MyToolbarbuttonConfig("Simpan Perubahan", "/img/save.gif");
			simpanKelola.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					Session s = HibernateUtil.currentSession();
					s.refresh(t);
					String statusLama = t.getStatus();
					t.setStatus(nilaiCombo(statusC));
					t.setPrioritas(nilaiCombo(prioC));
					t.setProgress(progressB.getValue() == null ? 0 : progressB.getValue());
					if (Ticket.STATUS_SELESAI.equals(t.getStatus()) && t.getTanggalSelesai() == null) {
						t.setTanggalSelesai(new Date());
					}
					Common.refreshUpdate(s, t);
					if (statusLama != null && !statusLama.equals(t.getStatus())) {
						TicketNotifikasi.statusBerubah(t, statusLama);
					}
					MyMessageboxConfig.show("Perubahan tiket tersimpan.", "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					if (onSelesai != null) {
						onSelesai.onEvent(new Event("onSelesai"));
					}
				}
			});
			simpanKelola.setParent(panelKelola);
		}

		// Lampiran tiket
		MyDiv lampiranBox = new MyDiv();
		lampiranBox.setStyle("margin:8px 0;");
		lampiranBox.setParent(isi);
		lampiranBox.appendChild(new ais.ui.util.MyHtml("<b>Lampiran Tiket</b>"));
		Hbox lampiranHbox = new Hbox();
		lampiranHbox.setParent(lampiranBox);
		try {
			LampiranLain.createDownloadUploadFileLain(lampiranHbox, t.getId(), LAMPIRAN_TIKET, "Lampiran Tiket", false,
					new EventListener() {
						@Override
						public void onEvent(Event e) throws Exception {
						}
					}, null, false, false, false, true);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketingAction.detail-lampiran");
		}

		// Thread komentar
		isi.appendChild(new ais.ui.util.MyHtml("<b>Diskusi / Pesan</b>"));
		final MyDiv komentarHost = new MyDiv();
		komentarHost.setWidth("100%");
		komentarHost.setParent(isi);
		muatKomentar(komentarHost, t, tbmuser, kelola);

		// Tambah komentar
		MyDiv tambahBox = new MyDiv();
		tambahBox.setStyle("border-top:1px solid #e2e8f0;margin-top:8px;padding-top:8px;");
		tambahBox.setParent(isi);
		final Textbox komentarBaru = new Textbox();
		komentarBaru.setMultiline(true);
		komentarBaru.setRows(3);
		komentarBaru.setWidth("98%");
		komentarBaru.setParent(tambahBox);
		final org.zkoss.zul.Checkbox internalCb = new org.zkoss.zul.Checkbox("Catatan internal (hanya pengelola)");
		internalCb.setVisible(kelola);
		internalCb.setParent(tambahBox);
		MyToolbarbuttonConfig kirim = new MyToolbarbuttonConfig("Kirim Pesan", "/img/svg/form-one.svg");
		kirim.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (komentarBaru.getValue() == null || komentarBaru.getValue().trim().isEmpty()) {
					return;
				}
				Session s = HibernateUtil.currentSession();
				TicketKomentar k = new TicketKomentar();
				k.setTicket(t);
				k.setIsi(komentarBaru.getValue().trim());
				k.setInternal(kelola && internalCb.isChecked());
				k.setTanggal(new Date());
				if (tbmuser != null) {
					k.setUserId(tbmuser.getUserId());
					k.setNama(tbmuser.getUserNama());
					k.setTipePengguna(tipePengguna(tbmuser));
				}
				Common.refreshSaveOrUpdate(s, k);
				TicketNotifikasi.komentarBaru(t, k, kelola);
				komentarBaru.setValue("");
				internalCb.setChecked(false);
				muatKomentar(komentarHost, t, tbmuser, kelola);
			}
		});
		kirim.setParent(tambahBox);

		Hbox footer = new Hbox();
		footer.setStyle("padding:8px 4px;");
		footer.setParent(window);
		MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak PDF", "/img/svg/file-pdf.svg");
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				try {
					java.io.File pdf = TicketPdf.cetak(t);
					if (pdf != null) {
						org.zkoss.zul.Filedownload.save(pdf, "application/pdf");
					}
				} catch (Exception ex) {
					Common.tampilErrorJikaAdmin(ex);
				}
			}
		});
		cetak.setParent(footer);
		MyToolbarbuttonConfig tutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		tutup.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				window.detach();
			}
		});
		tutup.setParent(footer);

		window.setVisible(true);
		window.onModal();
	}

	private static void muatKomentar(Component host, Ticket t, Tbmuser tbmuser, boolean kelola) {
		try {
			Common.clear(host);
			Session session = HibernateUtil.currentSession();
			@SuppressWarnings("unchecked")
			List<TicketKomentar> komentars = session.createCriteria(TicketKomentar.class)
					.add(Restrictions.eq("ticket", t)).addOrder(Order.asc("id")).list();
			if (komentars == null || komentars.isEmpty()) {
				host.appendChild(new ais.ui.util.MyHtml(
						"<div style='color:#94a3b8;padding:6px 2px;'>Belum ada pesan.</div>"));
				return;
			}
			for (TicketKomentar k : komentars) {
				if (Boolean.TRUE.equals(k.getInternal()) && !kelola) {
					continue; // catatan internal tak tampil ke non-pengelola
				}
				String bg = Boolean.TRUE.equals(k.getInternal()) ? "#fff7ed" : "#f1f5f9";
				String tag = Boolean.TRUE.equals(k.getInternal())
						? "<span style='font-size:10px;color:#b45309;font-weight:700;'> [internal]</span>" : "";
				String waktu = k.getTanggal() == null ? "" : Common.dateFormat5.get().format(k.getTanggal());
				host.appendChild(new ais.ui.util.MyHtml(
						"<div style='background:" + bg + ";border-radius:8px;padding:8px 10px;margin-bottom:6px;'>"
								+ "<div style='font-size:11px;color:#334155;'><b>" + esc(safe(k.getNama())) + "</b> ("
								+ esc(safe(k.getTipePengguna())) + ") &middot; " + esc(waktu) + tag + "</div>"
								+ "<div style='white-space:pre-wrap;color:#0f172a;margin-top:3px;'>" + esc(safe(k.getIsi()))
								+ "</div></div>"));
				// Lampiran per pesan
				Hbox hb = new Hbox();
				hb.setParent(host);
				try {
					LampiranLain.createDownloadUploadFileLain(hb, k.getId(), LAMPIRAN_KOMENTAR, "Lampiran", false,
							new EventListener() {
								@Override
								public void onEvent(Event e) throws Exception {
								}
							}, null, false, true, false, true);
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketingAction.komentar-lampiran");
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	// ==================================================================================
	// Util kecil
	// ==================================================================================

	private static void barisForm(org.zkoss.zul.Rows rows, String label, Component field) {
		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(label));
		row.appendChild(field);
	}

	private static void isiComboString(Combobox combo, String[] nilai) {
		combo.getChildren().clear();
		for (String n : nilai) {
			Comboitem ci = new Comboitem(n);
			ci.setValue("Semua".equals(n) ? "Semua" : n);
			ci.setParent(combo);
		}
		if (combo.getItemCount() > 0) {
			combo.setSelectedIndex(0);
		}
	}

	private static void pilihCombo(Combobox combo, String nilai) {
		if (nilai == null) {
			return;
		}
		for (Object o : combo.getItems()) {
			Comboitem ci = (Comboitem) o;
			if (nilai.equals(ci.getValue()) || nilai.equals(ci.getLabel())) {
				combo.setSelectedItem(ci);
				return;
			}
		}
	}

	private static String nilaiCombo(Combobox combo) {
		if (combo == null || combo.getSelectedItem() == null) {
			return null;
		}
		Object v = combo.getSelectedItem().getValue();
		return v == null ? combo.getSelectedItem().getLabel() : v.toString();
	}

	private static String warnaStatus(String status) {
		if (Ticket.STATUS_SELESAI.equals(status)) {
			return "#16a34a";
		}
		if (Ticket.STATUS_DIPROSES.equals(status)) {
			return "#2563eb";
		}
		if (Ticket.STATUS_MENUNGGU_RESPON.equals(status)) {
			return "#d97706";
		}
		if (Ticket.STATUS_DITOLAK.equals(status) || Ticket.STATUS_DITUTUP.equals(status)) {
			return "#64748b";
		}
		return "#0ea5e9";
	}

	private static String safe(String s) {
		return s == null ? "" : s;
	}

	private static String esc(String s) {
		return ais.ui.util.DashboardUiKit.esc(s == null ? "" : s);
	}
}
