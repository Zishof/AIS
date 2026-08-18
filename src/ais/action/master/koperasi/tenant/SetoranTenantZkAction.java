package ais.action.master.koperasi.tenant;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

import ais.action.master.koperasi.helper.LokasiKantinUtil;
import ais.action.master.koperasi.helper.TenantSetoranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.SetoranTenant;
import ais.database.model.inventory.Toko;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * <h2>SetoranTenantZkAction — Setoran &amp; Bagi Hasil Tenant versi <b>ZKoss</b> (native ZK 5.5).</h2>
 *
 * <p>
 * Padanan ZKoss dari modul JSP "Setoran Tenant": grid daftar setoran tiap tenant/stan beserta
 * tambah/ubah/hapus lewat dialog. Perhitungan bagi hasil, total kewajiban, dan status LUNAS/KURANG
 * memakai mesin pusat {@link TenantSetoranUtil} — identik dengan versi JSP, tanpa duplikasi. Hak ubah
 * mengikuti {@link LokasiKantinUtil#bolehKelola(HttpServletRequest)} (admin, bukan pedagang). Sesi
 * memakai {@link HibernateUtil#currentSession()} (tidak ditutup manual). Kompatibel Java 1.7.
 * </p>
 *
 * @author AIS e-Kantin (modul tenant)
 * @see TenantSetoranUtil
 */
public class SetoranTenantZkAction extends MyWindow {

	private static final long serialVersionUID = 4419021551123458001L;

	private static final SimpleDateFormat DF = new SimpleDateFormat("dd-MM-yyyy");

	private MyGrid grid;
	private boolean boleh;

	public SetoranTenantZkAction() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() {
		setWidth("100%");
		setHeight("100%");
		setBorder("none");
		HttpServletRequest req = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
		boleh = LokasiKantinUtil.bolehKelola(req);

		Vlayout root = new Vlayout();
		root.setStyle("padding:12px");
		root.setParent(this);
		Hlayout head = new Hlayout();
		head.setStyle("gap:10px;align-items:center;margin-bottom:8px");
		head.setParent(root);
		Vlayout t = new Vlayout();
		t.setParent(head);
		Label ttl = new Label(ais.common.Common.getBahasaConfig("Setoran & Bagi Hasil Tenant"));
		ttl.setStyle("font-weight:800;font-size:16px;display:block");
		ttl.setParent(t);
		Label desc = new Label("Kewajiban (bagi hasil + sewa + biaya) dan setoran tiap tenant/stan per periode.");
		desc.setStyle("color:#64748b;font-size:12px");
		desc.setParent(t);
		if (boleh) {
			Button tambah = new Button("Tambah");
			tambah.setStyle("margin-left:auto");
			tambah.addEventListener(Events.ON_CLICK, new EventListener() {
				public void onEvent(Event e) throws Exception {
					openForm(null);
				}
			});
			tambah.setParent(head);
		}

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(root);
		Columns cols = new Columns();
		cols.setParent(grid);
		kol(cols, "Tenant");
		kol(cols, "Periode");
		kol(cols, "Tanggal");
		kol(cols, "Omzet");
		kol(cols, "Kewajiban");
		kol(cols, "Setoran");
		kol(cols, "Sisa");
		kol(cols, "Status");
		if (boleh) {
			kol(cols, "Aksi");
		}
		new Rows().setParent(grid);
		refresh();
	}

	private void kol(Columns cols, String label) {
		new Column(label).setParent(cols);
	}

	private void refresh() {
		Rows rows = grid.getRows();
		rows.getChildren().clear();
		Session session = HibernateUtil.currentSession();
		List<SetoranTenant> ls = TenantSetoranUtil.daftar(session, null, 300);
		for (int i = 0; i < ls.size(); i++) {
			final SetoranTenant s = ls.get(i);
			double kw = TenantSetoranUtil.kewajiban(s);
			double sisa = kw - TenantSetoranUtil.nz(s.getSetoran());
			Row r = new Row();
			r.setParent(rows);
			new Label(s.getToko() != null && s.getToko().getNama() != null ? s.getToko().getNama() : "-").setParent(r);
			new Label(s.getPeriode() == null ? "-" : s.getPeriode()).setParent(r);
			new Label(s.getTanggal() == null ? "-" : DF.format(s.getTanggal())).setParent(r);
			kanan(r, TenantSetoranUtil.nz(s.getOmzet()));
			kanan(r, kw);
			kanan(r, TenantSetoranUtil.nz(s.getSetoran()));
			Label sl = new Label(fmt(sisa));
			sl.setStyle("text-align:right;display:block;font-weight:700;color:" + (sisa > 0 ? "#dc2626" : "#16a34a"));
			sl.setParent(r);
			Label st = new Label(s.getStatus());
			st.setStyle("padding:1px 8px;border-radius:999px;font-size:11px;font-weight:700;color:#fff;background:"
					+ (SetoranTenant.STATUS_LUNAS.equals(s.getStatus()) ? "#16a34a" : "#dc2626"));
			st.setParent(r);
			if (boleh) {
				Hlayout aksi = new Hlayout();
				aksi.setParent(r);
				Button ubah = new Button("Ubah");
				ubah.addEventListener(Events.ON_CLICK, new EventListener() {
					public void onEvent(Event e) throws Exception {
						openForm(s);
					}
				});
				ubah.setParent(aksi);
				Button hapus = new Button("Hapus");
				hapus.addEventListener(Events.ON_CLICK, new EventListener() {
					public void onEvent(Event e) throws Exception {
						onDelete(s);
					}
				});
				hapus.setParent(aksi);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void openForm(final SetoranTenant s) throws Exception {
		if (!boleh) {
			return;
		}
		Session session = HibernateUtil.currentSession();
		final Window win = new Window(s == null ? "Tambah Setoran Tenant" : "Ubah Setoran Tenant", "normal", true);
		win.setWidth("460px");
		win.setParent(this);
		Vlayout box = new Vlayout();
		box.setStyle("padding:14px");
		box.setParent(win);

		new Label("Tenant / Stan *").setParent(box);
		final Combobox cbToko = new Combobox();
		cbToko.setReadonly(true);
		cbToko.setWidth("100%");
		cbToko.setParent(box);
		List<Toko> tokoList = session.createCriteria(Toko.class).addOrder(Order.asc("nama")).setMaxResults(2000).list();
		Long tokoTerpilih = (s != null && s.getToko() != null) ? s.getToko().getId() : null;
		for (int i = 0; i < tokoList.size(); i++) {
			Toko tk = tokoList.get(i);
			Comboitem ci = new Comboitem(tk.getNama() == null ? ("Toko #" + tk.getId()) : tk.getNama());
			ci.setValue(tk.getId());
			ci.setParent(cbToko);
			if (tokoTerpilih != null && tokoTerpilih.equals(tk.getId())) {
				cbToko.setSelectedItem(ci);
			}
		}

		final Textbox periode = tb(box, "Periode (mis. 2026-07)", s == null ? "" : s.getPeriode());
		new Label(ais.common.Common.getBahasaConfig("Tanggal")).setParent(box);
		final Datebox tanggal = new Datebox(s == null || s.getTanggal() == null ? new Date() : s.getTanggal());
		tanggal.setWidth("100%");
		tanggal.setParent(box);
		final Decimalbox omzet = db(box, "Omzet", s == null ? 0 : TenantSetoranUtil.nz(s.getOmzet()));
		final Decimalbox persen = db(box, "% Bagi Hasil", s == null ? 0 : TenantSetoranUtil.nz(s.getPersenBagiHasil()));
		final Decimalbox bagi = db(box, "Bagi Hasil (Rp, opsional)", s == null ? 0 : TenantSetoranUtil.nz(s.getNilaiBagiHasil()));
		final Decimalbox sewa = db(box, "Sewa", s == null ? 0 : TenantSetoranUtil.nz(s.getSewa()));
		final Decimalbox biaya = db(box, "Biaya Layanan", s == null ? 0 : TenantSetoranUtil.nz(s.getBiayaLayanan()));
		final Decimalbox setoran = db(box, "Setoran", s == null ? 0 : TenantSetoranUtil.nz(s.getSetoran()));
		final Textbox ket = tb(box, "Keterangan", s == null ? "" : s.getKeterangan());

		Hlayout foot = new Hlayout();
		foot.setStyle("margin-top:12px;gap:8px");
		foot.setParent(box);
		Button batal = new Button("Batal");
		batal.addEventListener(Events.ON_CLICK, new EventListener() {
			public void onEvent(Event e) throws Exception {
				win.detach();
			}
		});
		batal.setParent(foot);
		Button simpan = new Button("Simpan");
		simpan.setStyle("background:#0d6efd;color:#fff");
		simpan.addEventListener(Events.ON_CLICK, new EventListener() {
			public void onEvent(Event e) throws Exception {
				if (cbToko.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon maaf, tenant/stan belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar Tenant/Stan; (2) pilih salah satu tenant; (3) ulangi penyimpanan setoran.");
					return;
				}
				Long tokoId = (Long) cbToko.getSelectedItem().getValue();
				Toko toko = (Toko) HibernateUtil.currentSession().get(Toko.class, tokoId);
				HttpServletRequest req = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
				Tbmuser u = Common.getCurrentUser(req);
				String oleh = u != null && u.getUserNama() != null ? u.getUserNama() : (u == null ? null : String.valueOf(u.getUserId()));
				String olehId = u == null ? null : String.valueOf(u.getUserId());
				try {
					TenantSetoranUtil.simpan(HibernateUtil.currentSession(), s == null ? null : s.getId(), toko,
							tanggal.getValue(), periode.getValue(), d(omzet), d(persen), d(bagi), d(sewa), d(biaya),
							d(setoran), ket.getValue(), oleh, olehId);
					win.detach();
					refresh();
				} catch (Exception ex) {
					Common.tampilErrorJikaAdmin(ex);
					MyMessageboxConfig.show(MyMessageboxConfig.format(
							"Mohon maaf, penyimpanan setoran tenant gagal diproses. Rincian: {V1}. Langkah yang dapat dilakukan: (1) periksa kembali data yang diisi; (2) coba simpan ulang; (3) apabila masih gagal, mohon hubungi administrator sistem.",
							ex.getMessage()));
				}
			}
		});
		simpan.setParent(foot);
		win.doModal();
	}

	private void onDelete(final SetoranTenant s) throws Exception {
		MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data setoran tenant ini? Data yang telah dihapus tidak dapat dikembalikan.", "Konfirmasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
				MyMessageboxConfig.QUESTION, new EventListener() {
					public void onEvent(Event e) throws Exception {
						if (new Integer(e.getData().toString()).intValue() == MyMessageboxConfig.OK) {
							try {
								TenantSetoranUtil.hapus(HibernateUtil.currentSession(), s.getId());
								refresh();
							} catch (Exception ex) {
								Common.tampilErrorJikaAdmin(ex);
								MyMessageboxConfig.show(MyMessageboxConfig.format(
										"Mohon maaf, penghapusan data setoran tenant gagal diproses. Rincian: {V1}. Langkah yang dapat dilakukan: (1) pastikan data tidak sedang digunakan pada transaksi lain; (2) coba ulangi penghapusan; (3) apabila masih gagal, mohon hubungi administrator sistem.",
										ex.getMessage()));
							}
						}
					}
				});
	}

	private void kanan(Row r, double v) {
		Label l = new Label(fmt(v));
		l.setStyle("text-align:right;display:block");
		l.setParent(r);
	}

	private Textbox tb(Vlayout box, String label, String value) {
		new Label(label).setParent(box);
		Textbox t = new Textbox(value == null ? "" : value);
		t.setWidth("100%");
		t.setParent(box);
		return t;
	}

	private Decimalbox db(Vlayout box, String label, double value) {
		new Label(label).setParent(box);
		Decimalbox d = new Decimalbox(new BigDecimal(value));
		d.setWidth("100%");
		d.setParent(box);
		return d;
	}

	private static double d(Decimalbox b) {
		return b.getValue() == null ? 0 : b.getValue().doubleValue();
	}

	private static String fmt(double v) {
		return java.text.NumberFormat.getInstance(new java.util.Locale("id")).format(Math.round(v));
	}
}
