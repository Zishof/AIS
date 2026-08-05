package ais.action.master.ticket;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.ticket.TicketKategori;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>Konfigurasi modul Ticketing (Fase 5)</h3>
 *
 * <p>Panel administrasi untuk modul {@link TicketingAction}:</p>
 * <ul>
 *   <li><b>Aktifkan modul</b> ({@code ticketing_aktif}).</li>
 *   <li><b>Wajib lewat SOP</b> ({@code ticketing_wajib_sop}) — gerbang pengajuan via disposisi.</li>
 *   <li><b>Role developer</b> ({@code ticketing_role_developer}, CSV roleId) — melihat/mengelola semua tiket.</li>
 *   <li><b>Kelola Kategori</b> — CRUD {@link TicketKategori}.</li>
 * </ul>
 * Semua nilai disimpan di tabel {@code konfigurasi} lewat {@link Common#getKonfigurasi(String, String)} +
 * {@link Common#refreshSaveOrUpdate(Session, ais.database.model.GeneralValueObject)}. Java 1.7.
 */
public class TicketKonfigurasiAction extends MyWindow {

	private static final long serialVersionUID = 3120250724011L;

	public static final String KEY_AKTIF = "ticketing_aktif";
	public static final String KEY_WAJIB_SOP = "ticketing_wajib_sop";
	public static final String KEY_ROLE_DEV = "ticketing_role_developer";
	/** Konfigurasi baru (semantik AKTIF/TIDAK_AKTIF) — sengaja nama baru agar default bersih = OFF. */
	public static final String KEY_FAB = "ticketing_fab_tampil";

	/** Konstruktor no-arg — dipanggil framework saat menu (url = nama kelas) diklik. */
	public TicketKonfigurasiAction() {
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

			display(center);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** True bila modul ticketing diaktifkan admin. */
	public static boolean modulAktif() {
		try {
			return "true".equalsIgnoreCase(Common.getKonfigurasi(KEY_AKTIF, "true").getNilai());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketKonfigurasiAction.modulAktif");
			return true;
		}
	}

	/**
	 * True bila tombol "Ajukan Tiket" mengambang ditampilkan di shell utama. <b>Default TERSEMBUNYI</b>
	 * (harus diaktifkan admin lewat KonfigurasiNewAction atau panel Konfigurasi Ticketing). Memakai
	 * semantik {@code Konfigurasi.AKTIF/TIDAK_AKTIF} agar konsisten dengan toggle di KonfigurasiNewAction.
	 */
	public static boolean fabAktif() {
		try {
			return Common.bolehKonfigurasi(KEY_FAB, Konfigurasi.TIDAK_AKTIF);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketKonfigurasiAction.fabAktif");
			return false;
		}
	}

	/** True bila pengajuan tiket wajib lewat SOP/disposisi. */
	public static boolean wajibSop() {
		try {
			return "true".equalsIgnoreCase(Common.getKonfigurasi(KEY_WAJIB_SOP, "true").getNilai());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketKonfigurasiAction.wajibSop");
			return true;
		}
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
				"<div style='font-size:16px;font-weight:800;color:#0f172a;margin-bottom:8px;'>Konfigurasi Ticketing</div>"));

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(root);
		Columns cols = new Columns();
		cols.setParent(grid);
		new Column("", null, "220px").setParent(cols);
		new Column().setParent(cols);
		Rows rows = new Rows();
		rows.setParent(grid);

		final Checkbox aktif = new Checkbox("Aktifkan modul Ticketing");
		aktif.setChecked(modulAktif());
		baris(rows, "Status Modul", aktif);

		final Checkbox wajibSop = new Checkbox("Setiap tiket wajib diajukan lewat SOP / disposisi");
		wajibSop.setChecked(wajibSop());
		baris(rows, "Alur Pengajuan", wajibSop);

		final Checkbox fab = new Checkbox("Tampilkan tombol \"Ajukan Tiket\" mengambang di kanan-bawah");
		fab.setChecked(fabAktif());
		baris(rows, "Tombol Mengambang", fab);

		final Textbox roleDev = new Textbox();
		roleDev.setWidth("95%");
		roleDev.setValue(Common.getKonfigurasi(KEY_ROLE_DEV, "").getNilai());
		roleDev.setTooltiptext("contoh: 3,7,12 (roleId, pisah koma)");
		baris(rows, "Role Developer (CSV roleId)", roleDev);

		Hbox aksi = new Hbox();
		aksi.setStyle("padding:8px 2px;");
		aksi.setParent(root);

		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan Konfigurasi", "/img/save.gif");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				Session s = HibernateUtil.currentSession();
				simpanKonfig(s, KEY_AKTIF, aktif.isChecked() ? "true" : "false");
				simpanKonfig(s, KEY_WAJIB_SOP, wajibSop.isChecked() ? "true" : "false");
				simpanKonfig(s, KEY_FAB, fab.isChecked() ? Konfigurasi.AKTIF : Konfigurasi.TIDAK_AKTIF);
				simpanKonfig(s, KEY_ROLE_DEV, roleDev.getValue() == null ? "" : roleDev.getValue().replace(" ", ""));
				MyMessageboxConfig.show("Konfigurasi tersimpan.", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
			}
		});
		simpan.setParent(aksi);

		MyToolbarbuttonConfig kelolaKategori = new MyToolbarbuttonConfig("Kelola Kategori", "/img/svg/list.svg");
		kelolaKategori.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				bukaKelolaKategori();
			}
		});
		kelolaKategori.setParent(aksi);
	}

	private static void simpanKonfig(Session s, String key, String nilai) {
		Konfigurasi k = Common.getKonfigurasi(key, nilai);
		if (k == null) {
			k = new Konfigurasi(key, nilai);
		} else {
			k.setNilai(nilai);
		}
		Common.refreshSaveOrUpdate(s, k);
	}

	// ==================================================================================
	// CRUD Kategori
	// ==================================================================================

	private static void bukaKelolaKategori() throws InterruptedException {
		final MyWindow window = new MyWindow("Kelola Kategori Tiket", "normal", true);
		window.setWidth("560px");
		window.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Hbox toolbar = new Hbox();
		toolbar.setStyle("padding:6px;");
		toolbar.setParent(window);

		final MyDiv listHost = new MyDiv();
		listHost.setWidth("100%");

		MyToolbarbuttonConfig tambah = new MyToolbarbuttonConfig("Tambah Kategori", "/img/svg/form-one.svg");
		tambah.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				bukaFormKategori(new TicketKategori(), listHost);
			}
		});
		tambah.setParent(toolbar);

		listHost.setParent(window);
		muatKategori(listHost);

		window.setVisible(true);
		window.onModal();
	}

	private static void muatKategori(final Component listHost) {
		try {
			Common.clear(listHost);
			Session session = HibernateUtil.currentSession();
			@SuppressWarnings("unchecked")
			List<TicketKategori> list = session.createCriteria(TicketKategori.class).addOrder(Order.asc("nomorUrut"))
					.addOrder(Order.asc("id")).list();
			if (list == null || list.isEmpty()) {
				listHost.appendChild(new Label("Belum ada kategori."));
				return;
			}
			for (final TicketKategori kat : list) {
				MyDiv baris = new MyDiv();
				baris.setStyle("display:flex;align-items:center;justify-content:space-between;gap:8px;"
						+ "border:1px solid #e2e8f0;border-radius:8px;padding:8px 10px;margin-bottom:6px;");
				String warna = kat.getWarna() == null || kat.getWarna().trim().isEmpty() ? "#0ea5e9" : kat.getWarna();
				baris.appendChild(new ais.ui.util.MyHtml(
						"<span style='display:inline-flex;align-items:center;gap:8px;'>"
								+ "<span style='width:14px;height:14px;border-radius:50%;background:"
								+ ais.ui.util.DashboardUiKit.esc(warna) + ";display:inline-block;'></span><b>"
								+ ais.ui.util.DashboardUiKit.esc(kat.getNama() == null ? "" : kat.getNama())
								+ "</b></span>"));
				Hbox aksi = new Hbox();
				MyToolbarbuttonConfig edit = new MyToolbarbuttonConfig("Ubah", "/img/edit.gif");
				edit.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						bukaFormKategori(kat, listHost);
					}
				});
				edit.setParent(aksi);
				MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("Hapus", "/img/delete.gif");
				hapus.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						Session s = HibernateUtil.currentSession();
						TicketKategori d = (TicketKategori) s.get(TicketKategori.class, kat.getId());
						if (d != null) {
							s.delete(d);
							s.flush();
						}
						muatKategori(listHost);
					}
				});
				hapus.setParent(aksi);
				baris.appendChild(aksi);
				listHost.appendChild(baris);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static void bukaFormKategori(final TicketKategori kategori, final Component listHost) throws InterruptedException {
		final MyWindow w = new MyWindow(kategori.getId() == null ? "Tambah Kategori" : "Ubah Kategori", "normal", true);
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

		final Textbox nama = new Textbox(kategori.getNama());
		nama.setWidth("95%");
		baris(rows, "Nama *", nama);
		final Textbox keterangan = new Textbox(kategori.getKeterangan());
		keterangan.setMultiline(true);
		keterangan.setRows(2);
		keterangan.setWidth("95%");
		baris(rows, "Keterangan", keterangan);
		final Textbox warna = new Textbox(kategori.getWarna());
		warna.setWidth("95%");
		warna.setTooltiptext("#0ea5e9");
		baris(rows, "Warna (hex)", warna);
		final Intbox urut = new Intbox(kategori.getNomorUrut());
		urut.setWidth("70px");
		baris(rows, "Nomor Urut", urut);

		Hbox footer = new Hbox();
		footer.setStyle("padding:8px 4px;");
		footer.setParent(w);
		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				w.detach();
			}
		});
		batal.setParent(footer);
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (nama.getValue() == null || nama.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Nama kategori wajib diisi.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				Session s = HibernateUtil.currentSession();
				kategori.setNama(nama.getValue().trim());
				kategori.setKeterangan(keterangan.getValue());
				kategori.setWarna(warna.getValue());
				kategori.setNomorUrut(urut.getValue());
				if (kategori.getAktif() == null) {
					kategori.setAktif(true);
				}
				Common.refreshSaveOrUpdate(s, kategori);
				w.detach();
				muatKategori(listHost);
			}
		});
		simpan.setParent(footer);

		w.setVisible(true);
		w.onModal();
	}

	private static void baris(Rows rows, String label, Component field) {
		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(label));
		row.appendChild(field);
	}
}
