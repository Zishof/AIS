package ais.action.master.koperasi.kas;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import ais.action.master.koperasi.helper.SesiKasUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.SesiKasKasir;
import ais.database.model.inventory.Toko;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * <h2>KasKasirZkAction — Buka/Tutup Kas Kasir versi <b>ZKoss</b> (native ZK 5.5).</h2>
 *
 * <p>
 * Padanan ZKoss dari modul JSP "Kas Kasir": membuka kas dengan modal awal, lalu menutup kas dengan
 * mengisi uang fisik sehingga selisih terhadap kas seharusnya (modal + penjualan tunai) terhitung
 * otomatis. Seluruh logika buka/tutup dan perhitungan penjualan memakai mesin pusat
 * {@link SesiKasUtil} — identik dengan versi JSP, tanpa duplikasi. Riwayat sesi ditampilkan pada
 * {@link MyGrid}. Sesi memakai {@link HibernateUtil#currentSession()} (tidak ditutup manual).
 * Toko dipaksa ke toko pedagang bila login sebagai pedagang. Kompatibel Java 1.7.
 * </p>
 *
 * @author AIS e-Kantin (modul kas kasir)
 * @see SesiKasUtil
 */
public class KasKasirZkAction extends MyWindow {

	private static final long serialVersionUID = 4419021551123457001L;

	private static final SimpleDateFormat DF = new SimpleDateFormat("dd-MM-yyyy HH:mm");

	private String oleh;
	private String olehId;
	private Long tokoId;
	private boolean lockToko;
	private Toko toko;

	private Div panel;
	private MyGrid grid;

	private String idPerangkat() {
		try {
			Object nativeSession = org.zkoss.zk.ui.Sessions.getCurrent().getNativeSession();
			if (nativeSession instanceof javax.servlet.http.HttpSession) {
				return "zk-" + ((javax.servlet.http.HttpSession) nativeSession).getId();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit KasKasirZkAction.idPerangkat");
		}
		return "zk-" + Executions.getCurrent().getDesktop().getId();
	}

	public KasKasirZkAction() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {
		setWidth("100%");
		setHeight("100%");
		setBorder("none");
		HttpServletRequest req = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
		Tbmuser u = Common.getCurrentUser(req);
		oleh = (u != null && u.getUserNama() != null) ? u.getUserNama() : (u == null ? "-" : String.valueOf(u.getUserId()));
		olehId = u == null ? "-" : String.valueOf(u.getUserId());
		if (u != null && u.getPedagang() != null && u.getPedagang().getToko() != null) {
			toko = u.getPedagang().getToko();
			tokoId = toko.getId();
			lockToko = true;
		}

		Vlayout root = new Vlayout();
		root.setStyle("padding:12px");
		root.setParent(this);
		Label ttl = new Label("Buka / Tutup Kas Kasir");
		ttl.setStyle("font-weight:800;font-size:16px;display:block");
		ttl.setParent(root);
		Label desc = new Label(ais.common.Common.getBahasaConfig("Buka kas dengan modal awal, lalu tutup kas untuk mencocokkan uang fisik dengan penjualan tunai."));
		desc.setStyle("display:block;color:#64748b;font-size:12px;margin-bottom:8px");
		desc.setParent(root);

		panel = new Div();
		panel.setStyle("border:1px solid #e9eef5;border-radius:14px;padding:16px;margin-bottom:12px;background:#fff");
		panel.setParent(root);

		Label rt = new Label(ais.common.Common.getBahasaConfig("Riwayat Sesi Kas"));
		rt.setStyle("font-weight:700;display:block;margin-bottom:6px");
		rt.setParent(root);
		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(root);
		Columns cols = new Columns();
		cols.setParent(grid);
		kol(cols, "Kasir");
		kol(cols, "Buka");
		kol(cols, "Tutup");
		kol(cols, "Modal");
		kol(cols, "Tunai");
		kol(cols, "Uang Fisik");
		kol(cols, "Selisih");
		kol(cols, "Status");
		new Rows().setParent(grid);

		render();
		muatRiwayat();
	}

	private void kol(Columns cols, String label) {
		new Column(label).setParent(cols);
	}

	/** Menggambar isi panel: form buka (bila tak ada sesi) atau form tutup (bila kas terbuka). */
	private void render() throws Exception {
		panel.getChildren().clear();
		Session session = HibernateUtil.currentSession();
		final String perangkat = idPerangkat();
		final SesiKasKasir sesi = SesiKasUtil.sesiTerbukaPerangkat(session, oleh, olehId, tokoId, perangkat);
		if (sesi != null && SesiKasUtil.ikatPerangkatJikaLama(sesi, perangkat, "ZK Web / " + perangkat)) {
			Common.refreshSaveOrUpdate(session, sesi);
		}

		if (sesi == null) {
			Label l = new Label(ais.common.Common.getBahasaConfig("Kas Tertutup"));
			l.setStyle("font-weight:700;color:#64748b;display:block;margin-bottom:8px");
			l.setParent(panel);
			Hlayout h = new Hlayout();
			h.setStyle("gap:8px;align-items:flex-end;flex-wrap:wrap");
			h.setParent(panel);
			Vlayout c1 = new Vlayout();
			c1.setParent(h);
			new Label(ais.common.Common.getBahasaConfig("Modal Awal (Rp)")).setParent(c1);
			final Decimalbox modal = new Decimalbox(BigDecimal.ZERO);
			modal.setWidth("160px");
			modal.setParent(c1);
			Vlayout c2 = new Vlayout();
			c2.setParent(h);
			new Label(ais.common.Common.getBahasaConfig("Keterangan")).setParent(c2);
			final Textbox ket = new Textbox();
			ket.setWidth("220px");
			ket.setParent(c2);
			Button buka = new Button("Buka Kas");
			buka.setStyle("background:#16a34a;color:#fff");
			buka.addEventListener(Events.ON_CLICK, new EventListener() {
				public void onEvent(Event e) throws Exception {
					double m = modal.getValue() == null ? 0 : modal.getValue().doubleValue();
					Session aktif = HibernateUtil.currentSession();
					SesiKasKasir akunLain = SesiKasUtil.sesiTerbuka(aktif, oleh, olehId, null);
					SesiKasKasir perangkatLain = SesiKasUtil.sesiTerbukaPadaPerangkat(aktif, null, perangkat);
					if (akunLain != null || perangkatLain != null) {
						MyMessageboxConfig.show(
								akunLain != null
										? "Akun ini masih mempunyai sesi kas terbuka. Tutup sesi tersebut sebelum membuka sesi baru."
										: "Perangkat ini masih dipakai oleh sesi kas lain. Tutup sesi tersebut sebelum berganti kasir.",
								"Sesi Kas Masih Aktif", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}
					SesiKasUtil.buka(aktif, lockToko ? toko : null, oleh, olehId, m, ket.getValue(),
							null, null, perangkat, "ZK Web / " + perangkat);
					render();
					muatRiwayat();
				}
			});
			buka.setParent(h);
		} else {
			double[] jual = SesiKasUtil.hitungPenjualan(session, sesi, new Date());
			final double seharusnya = (sesi.getModalAwal() == null ? 0.0 : sesi.getModalAwal().doubleValue()) + jual[0];
			Label l = new Label("Kas Terbuka sejak " + DF.format(sesi.getWaktuBuka()));
			l.setStyle("font-weight:700;color:#16a34a;display:block;margin-bottom:8px");
			l.setParent(panel);
			Hlayout info = new Hlayout();
			info.setStyle("gap:24px;flex-wrap:wrap;margin-bottom:10px");
			info.setParent(panel);
			kpi(info, "Modal Awal", sesi.getModalAwal().doubleValue(), "#0f172a");
			kpi(info, "Penjualan Tunai", jual[0], "#16a34a");
			kpi(info, "Non Tunai", jual[1], "#0d6efd");
			kpi(info, "Kas Seharusnya", seharusnya, "#0f172a");

			Hlayout h = new Hlayout();
			h.setStyle("gap:8px;align-items:flex-end;flex-wrap:wrap");
			h.setParent(panel);
			Vlayout c1 = new Vlayout();
			c1.setParent(h);
			new Label(ais.common.Common.getBahasaConfig("Uang Fisik (Rp)")).setParent(c1);
			final Decimalbox uf = new Decimalbox(new BigDecimal(Math.round(seharusnya)));
			uf.setWidth("160px");
			uf.setParent(c1);
			Vlayout c2 = new Vlayout();
			c2.setParent(h);
			new Label(ais.common.Common.getBahasaConfig("Selisih")).setParent(c2);
			final Label sel = new Label("0");
			sel.setStyle("font-size:18px;font-weight:800");
			sel.setParent(c2);
			EventListener hit = new EventListener() {
				public void onEvent(Event e) throws Exception {
					double v = (uf.getValue() == null ? 0 : uf.getValue().doubleValue()) - seharusnya;
					sel.setValue(fmt(v));
					sel.setStyle("font-size:18px;font-weight:800;color:" + (v < 0 ? "#dc2626" : "#16a34a"));
				}
			};
			uf.addEventListener(Events.ON_CHANGE, hit);
			hit.onEvent(null);
			Vlayout c3 = new Vlayout();
			c3.setParent(h);
			new Label(ais.common.Common.getBahasaConfig("Keterangan")).setParent(c3);
			final Textbox ket = new Textbox();
			ket.setWidth("200px");
			ket.setParent(c3);
			Button tutup = new Button("Tutup Kas");
			tutup.setStyle("background:#dc2626;color:#fff");
			tutup.addEventListener(Events.ON_CLICK, new EventListener() {
				public void onEvent(Event e) throws Exception {
					final double uang = uf.getValue() == null ? 0 : uf.getValue().doubleValue();
					final String keter = ket.getValue();
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menutup kas kasir sekarang? Setelah kas ditutup, sesi kas yang sedang berjalan akan diakhiri dan selisih terhadap kas seharusnya akan dicatat secara permanen.", "Konfirmasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								public void onEvent(Event ev) throws Exception {
									if (new Integer(ev.getData().toString()).intValue() == MyMessageboxConfig.OK) {
										SesiKasUtil.tutup(HibernateUtil.currentSession(), sesi, uang, keter);
										render();
										muatRiwayat();
									}
								}
							});
				}
			});
			tutup.setParent(h);
		}
	}

	private void kpi(Hlayout parent, String label, double val, String color) {
		Vlayout v = new Vlayout();
		v.setParent(parent);
		Label l = new Label(label);
		l.setStyle("font-size:11px;color:#64748b;text-transform:uppercase;font-weight:700;display:block");
		l.setParent(v);
		Label n = new Label(fmt(val));
		n.setStyle("font-size:20px;font-weight:800;color:" + color);
		n.setParent(v);
	}

	@SuppressWarnings("unchecked")
	private void muatRiwayat() {
		Rows rows = grid.getRows();
		rows.getChildren().clear();
		Session session = HibernateUtil.currentSession();
		StringBuilder w = new StringBuilder(" where 1=1 ");
		if (lockToko && tokoId != null) {
			w.append(" and k.toko=").append(tokoId);
		}
		SQLQuery q = session.createSQLQuery(
				"select k.kasir_nama, k.waktubuka, k.waktututup, coalesce(k.modalawal,0), coalesce(k.totaltunai,0), coalesce(k.uangfisik,0), coalesce(k.selisih,0), coalesce(k.status,'BUKA') "
						+ " from koperasi.sesi_kas_kasir k " + w + " order by k.waktubuka desc ");
		q.setMaxResults(100);
		List<Object[]> data = q.list();
		for (int i = 0; i < data.size(); i++) {
			Object[] a = data.get(i);
			Row r = new Row();
			r.setParent(rows);
			new Label(a[0] == null ? "-" : a[0].toString()).setParent(r);
			new Label(a[1] == null ? "" : DF.format((Date) a[1])).setParent(r);
			new Label(a[2] == null ? "-" : DF.format((Date) a[2])).setParent(r);
			kanan(r, num(a[3]));
			kanan(r, num(a[4]));
			kanan(r, num(a[5]));
			double sel = a[6] == null ? 0 : ((Number) a[6]).doubleValue();
			Label sl = new Label(fmt(sel));
			sl.setStyle("text-align:right;display:block;font-weight:700;color:" + (sel < 0 ? "#dc2626" : "#16a34a"));
			sl.setParent(r);
			new Label(a[7] == null ? "BUKA" : a[7].toString()).setParent(r);
		}
	}

	private void kanan(Row r, double v) {
		Label l = new Label(fmt(v));
		l.setStyle("text-align:right;display:block");
		l.setParent(r);
	}

	private static double num(Object o) {
		return o == null ? 0d : ((Number) o).doubleValue();
	}

	private static String fmt(double v) {
		return java.text.NumberFormat.getInstance(new java.util.Locale("id")).format(Math.round(v));
	}
}
