package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
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
import ais.common.InitDataHelper;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS — lihat {@link ais.ui.util.GetEventListener} untuk
 * arsitektur kerangka umum (constructor/display/onSearchDefault/renderer/callback). BERBEDA dari
 * hampir semua subclass {@code AmbilData*Banbox} lain: sumber datanya BUKAN entity Hibernate,
 * melainkan {@code ais.common.ConstantValues.treeMapFormSop} — peta statik in-memory
 * ({@code kode kelas form Java} &rarr; {@code nama tampilan}) hasil pemindaian reflektif seluruh
 * kelas "Form SOP" ({@code InitDataHelper.reInitClass()}, dipanggil otomatis bila peta masih
 * kosong). Dipakai pada konfigurasi alur SOP untuk memilih SATU jenis form yang akan dipasang
 * pada suatu langkah alur persetujuan.
 * <p>
 * Popup menampilkan grid pilih-tunggal (via {@link Radiogroup}/{@link Radio}) dengan filter teks
 * "Nama" yang dicocokkan secara case-insensitive-substring (bukan ILIKE SQL, karena sumber data
 * bukan query database) terhadap NAMA TAMPILAN atau KODE KELAS. Hasil terpilih disimpan sebagai
 * {@code String} (kode kelas) pada attribute {@code "data"}/{@code "myValue"}, BUKAN sebagai
 * entity — konsumen komponen ini membaca kode kelas tersebut, bukan objek domain.
 *
 * @see Bandbox
 */
public class AmbilDataFormSopBanbox extends Bandbox implements GetEventListener {

	/**
	 * Serial version UID standar untuk kompatibilitas serialisasi komponen ZK.
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;

	private EventListener eventListener;

	/**
	 * Membangun komponen dan memasang listener {@code onOpen} yang, pada pembukaan pertama,
	 * membangun popup ({@link #display()}), mengikuti kerangka umum di
	 * {@link ais.ui.util.GetEventListener}.
	 */
	public AmbilDataFormSopBanbox() {
		super();

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

	/**
	 * Merender satu baris grid dari elemen {@code String[]{namaTampilan, kodeKelas}}: radio pilih
	 * berlabel nama tampilan (indeks 0), dan label nama tampilan (indeks 1 — walau bernama
	 * "Class" pada kolomnya, yang ditampilkan tetap {@code d[1]} yang isinya sama dengan kode
	 * kelas form). Memilih baris menutup popup, menyimpan kode kelas ({@code d[1]}) ke attribute
	 * {@code "data"}/{@code "myValue"} pada Bandbox (BUKAN entity — lihat Javadoc kelas), mengisi
	 * teks tampilan dengan nama ({@code d[0]}), lalu memicu {@link #eventListener} bila terpasang.
	 *
	 * @see AmbilDataFormSopBanbox
	 */
	class SiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final String[] d = (String[]) arg1;

			Radio checkbox = new Radio(d[0]);
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(siswa.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					AmbilDataFormSopBanbox.this.setOpen(false);
					AmbilDataFormSopBanbox.this.setAttribute("data", d[1]);
					AmbilDataFormSopBanbox.this.setAttribute("myValue", d[1]);
					AmbilDataFormSopBanbox.this.setValue(d[0]);
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(d[1]).setParent(arg0);
		}

	}

	/**
	 * Membangun popup pencarian (dipanggil sekali saat pertama dibuka): form filter Nama, grid
	 * hasil bermold "paging", lalu memuat data awal lewat {@link #onSearchDefault(Event)}.
	 */
	public void display() {

		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("550px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Form SOP");
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

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		nama = new Textbox();
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama);
		nama.setWidth("90%");
		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(5);
		grid.getPagingChild().setMold("os");
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

		Columns columns = new Columns();

		columns.setParent(grid);

		grid.setPageSize(10);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Class");

		onSearchDefault(null);

	}

	/**
	 * Menyiapkan {@code ConstantValues.treeMapFormSop} lewat {@code InitDataHelper.reInitClass()}
	 * bila masih kosong, lalu menyaring entri-entrinya (nama tampilan atau kode kelas mengandung
	 * teks filter, case-insensitive) menjadi list {@code String[]{namaTampilan, kodeKelas}}.
	 * Mengisi ulang grid dengan hasilnya beserta {@link SiswaRenderer}.
	 *
	 * @param event tidak dipakai, hanya mengikuti signature standar listener pencarian
	 */
	public void onSearchDefault(Event event) {

		if (ConstantValues.treeMapFormSop.isEmpty()) {
			InitDataHelper.reInitClass();
		}

		List<String[]> d = new ArrayList<String[]>();

		for (String key : ConstantValues.treeMapFormSop.keySet()) {
			try {
				String val = ConstantValues.treeMapFormSop.get(key);
				if (nama.getValue().trim().isEmpty() || val.toLowerCase().contains(nama.getValue().trim().toLowerCase())
						|| key.toLowerCase().contains(nama.getValue().trim().toLowerCase())) {
					d.add(new String[] { val, key });
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataFormSopBanbox.java:227");
				// TODO: handle exception
			}
		}
		ListModel strset = new SimpleListModel(d);
		grid.setRowRenderer(new SiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** @param eventListener dipanggil setiap kali user memilih satu form SOP */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** @return listener pemilihan form SOP yang sedang terpasang, boleh {@code null} */
	public EventListener getEventListener() {
		return eventListener;
	}

}
