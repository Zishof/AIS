package ais.action.master.kursus.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.kursus.KomponenProdukKursus;
import ais.database.model.kursus.ProdukKursus;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.kursus.ProdukKursus}
 * — lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback). Produk kursus adalah paket/jenis kursus yang
 * ditawarkan di modul Kursus AIS (mis. paket kursus bahasa tertentu), tersusun dari sejumlah
 * {@link ais.database.model.kursus.KomponenProdukKursus} (biaya-biaya penyusunnya) yang disimpan sebagai
 * JSON pada field {@code hargaKomponens}.
 *
 * <p>Pencarian memakai dua {@code Textbox}: {@code nama} (ilike ke kolom {@code nama}) dan
 * {@code noregistrasi} (berlabel "Kode", ilike ke kolom {@code kode}), keduanya opsional dan digabung
 * AND; hanya produk aktif ({@code aktif} null atau {@code true}) yang disertakan, diurutkan ascending
 * berdasar id, dibatasi {@code Common.MAX_RESULT_20} baris. Hasil ditampilkan dalam {@link Radiogroup}
 * (pilih-tunggal via {@link Radio}) dengan kolom Kode, Nama, dan sub-grid rincian komponen per baris
 * (Komponen/Qty/Harga/Total beserta footer Total). Renderer baris memiliki efek samping non-trivial:
 * total harga dihitung ulang dari {@code hargaKomponens} saat merender, dan bila berbeda dari
 * {@code produkKursus.getHargaTotal()} tersimpan, entity langsung disimpan ulang lewat
 * {@code Common.refreshSaveOrUpdate(produkKursus)} (auto-sinkronisasi harga total, bukan bug). Tidak ada
 * parameter constructor tambahan.</p>
 *
 * @see Bandbox
 */
public class AmbilDataProdukKursusBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();

	/**
	 * Membangun Bandbox read-only dan memasang listener {@code onOpen} standar: popup dibangun lazy
	 * (sekali saja) via {@link #display()}, lalu dibuka lewat {@link Common#createDefaultTimer}. Tidak
	 * ada parameter filter dari entity induk.
	 */
	public AmbilDataProdukKursusBanbox() {
		super();

		setReadonly(true);

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {
					display();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							setOpen(true);
						}
					});
				}

			}
		});

	}

	private Textbox nama;
	private Textbox noregistrasi;
	private EventListener eventListener;

	/**
	 * @return listener aktif saat ini, atau {@code null} bila belum diset
	 */
	public EventListener getEventListener() {
		return eventListener;
	}

	/**
	 * Menetapkan listener yang dipanggil setelah baris produk kursus dipilih.
	 *
	 * @param eventListener listener baru yang akan dipasang
	 */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * Merender satu baris grid hasil pencarian produk kursus: label Kode, Nama, dan sub-grid rincian
	 * komponen (dibaca dari JSON {@code produkKursus.getHargaKomponens()}, tiap entri menaut ke
	 * {@link KomponenProdukKursus} lewat {@code ConstantValues.ambil(...)}) berikut kolom Qty/Harga/Total
	 * dan footer Total, plus satu {@link Radio} pilihan di kolom pertama. Saat merender, total harga
	 * dihitung ulang dari komponen-komponennya; bila berbeda dari {@code hargaTotal} tersimpan, entity
	 * {@link ProdukKursus} langsung disimpan ulang via {@code Common.refreshSaveOrUpdate(...)} (efek
	 * samping tulis-saat-baca yang disengaja, bukan bug). Saat radio dicentang ({@code onCheck}), popup
	 * ditutup, entity terpilih disimpan lewat {@code setAttribute("produkKursus"/"myValue", ...)} dan teks
	 * tampilan Bandbox diisi nama produk, lalu {@link #eventListener} (bila terpasang) diberi tahu —
	 * mengikuti pola callback standar yang dijelaskan di {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataProdukKursusBanbox
	 */
	class ProdukKursusRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ProdukKursus produkKursus = (ProdukKursus) arg1;
			Radio checkbox = new Radio(produkKursus.getNama());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", produkKursus);
			// checkbox.setId(produkKursus.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataProdukKursusBanbox.this.setOpen(false);
					AmbilDataProdukKursusBanbox.this.setAttribute("produkKursus", produkKursus);
					AmbilDataProdukKursusBanbox.this.setAttribute("myValue", produkKursus);
					AmbilDataProdukKursusBanbox.this.setValue(produkKursus.getNama());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(produkKursus.getKode()).setParent(arg0);
			new Label(produkKursus.getNama()).setParent(arg0);

			Grid grid = new Grid();grid.setSclass("dgrid");
			grid.setWidth("100%");
			grid.setParent(arg0);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig("Komponen");
			column.setParent(columns);

			column = new MyColumnConfig("Qty");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("10%");

			column = new MyColumnConfig("Harga");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("20%");

			column = new MyColumnConfig("Total");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("20%");

			Rows rows = new Rows();
			rows.setParent(grid);
			JSONArray array = new JSONArray(produkKursus.getHargaKomponens());
			Double hargaTotal = 0.0;
			for (int i = 0; i < array.length(); i++) {

				JSONObject jsonObject = array.getJSONObject(i);

				Long key = null;
				if (!jsonObject.isNull("key")) {
					key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
				}

				if (key != null) {

					KomponenProdukKursus komponenProdukKursusBiaya = (KomponenProdukKursus) (jsonObject
							.isNull("komponenProdukKursus") ? null
									: ConstantValues.ambil(KomponenProdukKursus.class.getName(),
											ais.common.CommonJSONUtil.ambilLong(jsonObject,"komponenProdukKursus")));
					Double hrg = jsonObject.isNull("harga") ? 0.0 : jsonObject.getDouble("harga");
					Double qty = jsonObject.isNull("qty") ? 1.0 : jsonObject.getDouble("qty");

					hargaTotal += (qty * hrg);

					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setParent(rows);

					row.appendChild(new MyLabelAgakKecil(
							komponenProdukKursusBiaya == null ? "" : komponenProdukKursusBiaya.getNama()));
					row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(qty)));
					row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(hrg)));
					row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(qty * hrg)));
				}
			}
			Foot foot = new Foot();
			foot.setParent(grid);

			Footer footer = new Footer("Total");
			foot.appendChild(footer);

			footer = new Footer("");
			foot.appendChild(footer);

			footer = new Footer("");
			foot.appendChild(footer);

			if (hargaTotal.intValue() != produkKursus.getHargaTotal().intValue()) {
				produkKursus.setHargaTotal(hargaTotal);
				Common.refreshSaveOrUpdate(produkKursus);
			}

			Footer footerTotal = new Footer(Common.numberFormat.get().format(produkKursus.getHargaTotal()));
			foot.appendChild(footerTotal);

		}

	}

	/**
	 * Membangun isi {@link Bandpopup} sekali: panel judul "Daftar Produk Kursus" berisi form pencarian
	 * ({@code Textbox nama}, {@code Textbox noregistrasi}) + tombol Cari/Bersihkan, kolom Nama dilengkapi
	 * checkbox header "select all", dan grid hasil dibungkus {@link Radiogroup} (pilih-tunggal). Diakhiri
	 * memanggil {@link #onSearchDefault(Event)} dengan {@code null} agar grid langsung terisi saat popup
	 * pertama dibuka.
	 */
	public void display() {
		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("900px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Produk Kursus");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.Grid gridUtama = new org.zkoss.zul.Grid();
		gridUtama.setWidth("100%");
		ais.ui.util.ZkCompat.setFlex(gridUtama, true);
		gridUtama.setParent(center);
		Rows rowsUtama = new Rows();
		rowsUtama.setParent(gridUtama);

		Row rowUtama = new Row();
		rowUtama.setParent(rowsUtama);
		//
		//
		//
		//

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(noregistrasi = new Textbox());
		noregistrasi.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// row.appendChild(new ais.ui.util.MyLabelConfig("No Ujian"));
		// row.appendChild(noujian = new Textbox());
		// noujian.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// South south = new South();
		// ais.ui.util.ZkCompat.setFlex(south, true);
		// south.setParent(div);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);
		toolbar.appendChild(Common.createCleanButton(this, this));

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT_100. */
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		final Checkbox checkbox = new Checkbox("Nama");
		column.appendChild(checkbox);
		checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {
					try {
						Checkbox myCheckbox = (Checkbox) row.getAttribute("checkbox");
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kursus/helper/AmbilDataProdukKursusBanbox.java:334");

					}
				}
			}
		});

		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Produk");
		column.setWidth("60%");

		onSearchDefault(null);

	}

	/**
	 * Menjalankan pencarian {@link ProdukKursus} yang aktif ({@code aktif} null atau {@code true}),
	 * difilter opsional lewat {@code Textbox nama} (ilike ke kolom {@code nama}) dan
	 * {@code Textbox noregistrasi} (ilike ke kolom {@code kode}), keduanya digabung AND bila diisi,
	 * diurutkan ascending berdasar id dan dibatasi {@code Common.MAX_RESULT_20} baris. Hasil dipasang
	 * ke {@link #grid} lewat {@link ProdukKursusRenderer} dan {@code SimpleListModel}.
	 *
	 * @param event event pemicu (boleh {@code null}, mis. saat dipanggil dari {@link #display()})
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		System.out.println("onSearchDefault");

		List<ProdukKursus> produkKursus = session.createCriteria(ProdukKursus.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))) 
						.addOrder(Order.asc("id"))
						.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
						.add(noregistrasi.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("kode", noregistrasi.getValue().trim(), MatchMode.ANYWHERE))
						// .add(Restrictions.ilike("noUjian",
						// noujian.getText().trim(),
						// MatchMode.ANYWHERE)).setMaxResults(Common.MAX_RESULT)
						.setMaxResults(Common.MAX_RESULT_20).list();
		ListModel strset = new SimpleListModel(produkKursus);
		grid.setRowRenderer(new ProdukKursusRenderer());
		grid.setModelCheckMobile(strset);

	}
}
