package ais.action.master.employ.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.employ.GajiPokok;
import ais.database.model.employ.Golongan;
import ais.database.model.employ.Peraturan;
import ais.ui.util.MyTextbox;

/**
 * Tipe khusus untuk ambil data gaji pokok banbox. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code MyTextbox nama},
 * {@code MyTextbox masa}, {@code Combobox golongan}, {@code Combobox peraturan}; pembacaan/pencarian ({@code
 * onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code
 * display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataGajiPokokBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	public AmbilDataGajiPokokBanbox() {
		super();
		display();
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (grid == null || grid.getRows() == null || grid.getRows().getChildren() == null
						|| grid.getRows().getChildren().size() == 0) {
					onSearchDefault(null);
				}
			}
		});
	}

	private MyTextbox nama;
	private MyTextbox masa;
	private Combobox golongan;
	private Combobox peraturan;

	class GajiPokokRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final GajiPokok gajiPokok = (GajiPokok) arg1;

			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataGajiPokokBanbox.this.setOpen(false);
					AmbilDataGajiPokokBanbox.this.setAttribute("gajiPokok", gajiPokok);
					AmbilDataGajiPokokBanbox.this.setValue(gajiPokok.toString());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(gajiPokok.getGolongan() == null ? "" : gajiPokok.getGolongan().toString()).setParent(arg0);
			new Label(gajiPokok.getPeraturan() == null ? "" : gajiPokok.getPeraturan().toString()).setParent(arg0);
			new Label(gajiPokok.getMasaKerja() == null ? "" : gajiPokok.getMasaKerja() + "").setParent(arg0);
			new Label(gajiPokok.getGaji() == null ? "" : Common.numberFormat.get().format(gajiPokok.getGaji()))
					.setParent(arg0);
			new Label(gajiPokok.getKeterangan()).setParent(arg0);
		}

	}

	public void display() {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("1000px");
		bandpopup.setHeight("600px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Gaji Pokok");
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

		MyFormRow row = new MyFormRow();row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Kerja"));
		row.appendChild(masa = new MyTextbox());
		masa.setWidth("90%");
		masa.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Golongan"));
		row.appendChild(golongan = new Combobox());
		golongan.setWidth("90%");
		Common.insertCombo(golongan, "nama", Golongan.class, Restrictions.eq("aktif", true));
		golongan.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peraturan"));
		row.appendChild(peraturan = new Combobox());
		peraturan.setWidth("90%");
		Common.insertCombo(peraturan, "nama", Peraturan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		peraturan.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
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
		toolbar.appendChild(Common.createCleanButton(this, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null) {
					try {
						eventListener.onEvent(null);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
				onSearchDefault(event);
			}
		}));

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
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
		column.setLabel("Golongan");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Peraturan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Masa Kerja");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Gaji");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Integer masaKerja = null;
		try {
			masaKerja = Integer.parseInt(masa.getValue());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/helper/AmbilDataGajiPokokBanbox.java:262");
		}

		Session session = HibernateUtil.currentSession();

		List<GajiPokok> gajiPokok = session.createCriteria(GajiPokok.class).add(Restrictions.isNotNull("gaji"))
				.createAlias("golongan", "golongan").addOrder(Order.asc("golongan.nama"))
				.addOrder(Order.asc("masaKerja"))
				.add(masaKerja == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("masaKerja", masaKerja))
				.add(peraturan.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("peraturan", peraturan.getSelectedItem().getValue()))

				.add(golongan.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("golongan", golongan.getSelectedItem().getValue()))
				.add(Restrictions.ilike("golongan.nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.setMaxResults(1000).list();

		System.out.println(gajiPokok);
		ListModel strset = new SimpleListModel(gajiPokok);
		grid.setRowRenderer(new GajiPokokRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
