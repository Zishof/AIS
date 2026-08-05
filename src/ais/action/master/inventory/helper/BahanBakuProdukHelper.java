package ais.action.master.inventory.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vlayout;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.ui.util.MyDoublebox;

/**
 * <h3>Editor Bahan Baku (Bill of Materials) &amp; perhitungan HPP untuk Produk (versi ZK)</h3>
 *
 * <p>Memungkinkan satu {@link Produk} disusun dari bahan baku yang berasal dari produk lain. HPP
 * (Harga Pokok Penjualan/modal) dihitung otomatis = Σ(qty × harga) seluruh bahan baku. Data disimpan
 * sebagai string JSON pada kolom {@code Produk.bahanBaku} (lihat {@link Produk#getBahanBaku()}),
 * dengan format yang SAMA dengan versi JSP:
 * {@code [{"produk":<id>,"nama":"...","qty":<angka>,"harga":<angka>}]}.</p>
 *
 * <h3>Cara pakai</h3>
 * <pre>
 *   helper = new BahanBakuProdukHelper(produk.getId(), hargaBeliDoublebox);
 *   formRow.appendChild(helper.build(scopeToko, produk.getBahanBaku()));
 *   // ... saat simpan:
 *   produk.setBahanBaku(helper.toJson());
 * </pre>
 *
 * <h3>Pemeliharaan</h3>
 * <p>UI dibangun programatik (Vlayout): baris pemilih (Combobox produk + qty + tombol Tambah), grid
 * daftar bahan baku, total HPP, dan tombol opsional "Jadikan Harga Beli" yang mengisi
 * {@code hargaBeliTarget}. Kandidat bahan baku diambil via proyeksi Criteria (bukan entitas penuh →
 * hindari LazyInitException lintas-event) dan dibatasi 2000 baris; produk yang sedang diedit
 * dikecualikan agar tak menjadi bahan baku dirinya sendiri. JSON rusak diabaikan (mulai kosong).</p>
 */
public class BahanBakuProdukHelper {

	/** Satu baris bahan baku. */
	private static class Item {
		long produk;
		String nama;
		double qty;
		double harga;

		Item(long produk, String nama, double qty, double harga) {
			this.produk = produk;
			this.nama = nama;
			this.qty = qty;
			this.harga = harga;
		}

		double subtotal() {
			return qty * harga;
		}
	}

	/** Kandidat bahan baku (produk lain) — disimpan sbg nilai Comboitem (ringan, tanpa entitas). */
	private static class Opt {
		long id;
		String nama;
		double harga;

		Opt(long id, String nama, double harga) {
			this.id = id;
			this.nama = nama;
			this.harga = harga;
		}
	}

	private final List<Item> items = new ArrayList<Item>();
	private final Long currentProdukId;
	private final MyDoublebox hargaBeliTarget;

	private Combobox cboBahan;
	private MyDoublebox txtQty;
	private Grid grid;
	private Label lblTotal;

	public BahanBakuProdukHelper(Long currentProdukId, MyDoublebox hargaBeliTarget) {
		this.currentProdukId = currentProdukId;
		this.hargaBeliTarget = hargaBeliTarget;
	}

	/** Membangun komponen editor dan mengisinya dari JSON yang sudah ada. */
	public Component build(Toko scopeToko, String existingJson) {
		parse(existingJson);

		Vlayout box = new Vlayout();
		box.setWidth("100%");
		box.setStyle("gap:4px;");

		Label hint = new Label(ais.common.Common.getBahasaConfig("Susun produk dari bahan baku (produk lain). HPP dihitung otomatis dari total bahan baku."));
		hint.setStyle("font-size:11px;color:#64748b;");
		hint.setMultiline(true);
		hint.setParent(box);

		Hlayout pick = new Hlayout();
		pick.setStyle("gap:6px;align-items:center;");
		pick.setParent(box);
		cboBahan = new Combobox();
		cboBahan.setAutodrop(true);
		cboBahan.setWidth("250px");
		cboBahan.setTooltiptext("Ketik untuk mencari bahan baku");
		muatKandidat(scopeToko);
		pick.appendChild(cboBahan);
		pick.appendChild(new Label("x"));
		txtQty = new MyDoublebox(Double.valueOf(1.0));
		txtQty.setWidth("70px");
		pick.appendChild(txtQty);
		Button tambah = new Button("Tambah");
		tambah.setImage("/img/add.gif");
		tambah.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				onTambah();
			}
		});
		pick.appendChild(tambah);

		grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setStyle("border:0;background:transparent;");
		Columns cols = new Columns();
		cols.setParent(grid);
		addCol(cols, "Bahan Baku", null);
		addCol(cols, "Qty", "70px");
		addCol(cols, "Harga", "110px");
		addCol(cols, "Subtotal", "120px");
		addCol(cols, "", "44px");
		new Rows().setParent(grid);
		grid.setParent(box);

		Hlayout foot = new Hlayout();
		foot.setStyle("gap:8px;align-items:center;justify-content:flex-end;margin-top:4px;");
		foot.setParent(box);
		Label cap = new Label(ais.common.Common.getBahasaConfig("Total HPP:"));
		cap.setStyle("font-weight:700;");
		foot.appendChild(cap);
		lblTotal = new Label(ais.common.Common.getBahasaConfig("Rp 0"));
		lblTotal.setStyle("font-weight:800;color:#2563eb;");
		foot.appendChild(lblTotal);
		if (hargaBeliTarget != null) {
			Button setHpp = new Button("Jadikan Harga Beli");
			setHpp.setImage("/img/save.gif");
			setHpp.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					double t = total();
					if (t > 0) {
						hargaBeliTarget.setValue(Double.valueOf(t));
					}
				}
			});
			foot.appendChild(setHpp);
		}

		render();
		return box;
	}

	/** Total HPP = Σ(qty × harga). */
	public double total() {
		double t = 0;
		for (Item it : items) {
			t += it.subtotal();
		}
		return t;
	}

	/** Serialisasi bahan baku ke string JSON untuk disimpan ke {@code Produk.bahanBaku}. */
	public String toJson() throws Exception {
		JSONArray arr = new JSONArray();
		for (Item it : items) {
			JSONObject o = new JSONObject();
			o.put("produk", it.produk);
			o.put("nama", it.nama);
			o.put("qty", it.qty);
			o.put("harga", it.harga);
			arr.put(o);
		}
		return arr.toString();
	}

	// ---------------- internal ----------------

	private void onTambah() throws Exception {
		Comboitem ci = cboBahan.getSelectedItem();
		if (ci == null || ci.getValue() == null) {
			ais.ui.util.MyMessageboxConfig.show("Pilih bahan baku terlebih dahulu.", "Peringatan",
					ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
			return;
		}
		Opt o = (Opt) ci.getValue();
		Double q = txtQty.getValue();
		double qty = q == null ? 0 : q.doubleValue();
		if (qty <= 0) {
			ais.ui.util.MyMessageboxConfig.show("Qty harus lebih dari 0.", "Peringatan",
					ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
			return;
		}
		for (Item it : items) {
			if (it.produk == o.id) {
				it.qty += qty;
				txtQty.setValue(Double.valueOf(1.0));
				render();
				return;
			}
		}
		items.add(new Item(o.id, o.nama, qty, o.harga));
		txtQty.setValue(Double.valueOf(1.0));
		render();
	}

	private void render() {
		Rows rows = grid.getRows();
		if (rows == null) {
			rows = new Rows();
			rows.setParent(grid);
		}
		rows.getChildren().clear();
		double total = 0;
		for (int i = 0; i < items.size(); i++) {
			final int idx = i;
			Item it = items.get(i);
			total += it.subtotal();
			Row r = new Row();
			r.setParent(rows);
			new Label(it.nama).setParent(r);
			new Label(Common.numberFormat.get().format(it.qty)).setParent(r);
			new Label("Rp " + Common.numberFormat.get().format(it.harga)).setParent(r);
			Label sub = new Label("Rp " + Common.numberFormat.get().format(it.subtotal()));
			sub.setStyle("font-weight:700;");
			sub.setParent(r);
			Button del = new Button();
			del.setImage("/img/delete.gif");
			del.setTooltiptext("Hapus bahan baku");
			del.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					if (idx >= 0 && idx < items.size()) {
						items.remove(idx);
					}
					render();
				}
			});
			del.setParent(r);
		}
		if (items.isEmpty()) {
			Row r = new Row();
			r.setParent(rows);
			Label l = new Label(ais.common.Common.getBahasaConfig("Belum ada bahan baku (produk dijual tanpa resep)."));
			l.setStyle("color:#94a3b8;font-style:italic;");
			l.setParent(r);
		}
		if (lblTotal != null) {
			lblTotal.setValue("Rp " + Common.numberFormat.get().format(total));
		}
	}

	private void muatKandidat(Toko scopeToko) {
		try {
			Session session = HibernateUtil.currentSession();
			org.hibernate.Criteria c = session.createCriteria(Produk.class)
					.setProjection(Projections.projectionList().add(Projections.property("id"))
							.add(Projections.property("kode")).add(Projections.property("nama"))
							.add(Projections.property("hargaBeli")))
					.add(scopeToko == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("toko", scopeToko))
					.add(currentProdukId == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ne("id", currentProdukId))
					.addOrder(Order.asc("nama")).setMaxResults(2000);
			@SuppressWarnings("unchecked")
			List<Object[]> list = c.list();
			for (Object[] o : list) {
				long id = ((Number) o[0]).longValue();
				String kode = o[1] == null ? "" : o[1].toString();
				String nama = o[2] == null ? "" : o[2].toString();
				double harga = o[3] == null ? 0.0 : ((Number) o[3]).doubleValue();
				Comboitem ci = new Comboitem((kode.isEmpty() ? "" : kode + " - ") + nama + " (Rp "
						+ Common.numberFormat.get().format(harga) + ")");
				ci.setValue(new Opt(id, nama, harga));
				ci.setParent(cboBahan);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void parse(String json) {
		items.clear();
		if (json == null || json.trim().isEmpty()) {
			return;
		}
		try {
			JSONArray arr = new JSONArray(json.trim());
			for (int i = 0; i < arr.length(); i++) {
				JSONObject o = arr.getJSONObject(i);
				long pid = o.optLong("produk", 0);
				if (pid <= 0) {
					continue;
				}
				items.add(new Item(pid, o.optString("nama", ""), o.optDouble("qty", 0), o.optDouble("harga", 0)));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/inventory/helper/BahanBakuProdukHelper.java:326");
			// JSON rusak/format lama -> mulai dari kosong, jangan gagalkan form
		}
	}

	private void addCol(Columns cols, String label, String width) {
		Column c = new Column();
		c.setLabel(label);
		if (width != null) {
			c.setWidth(width);
		}
		c.setParent(cols);
	}
}
