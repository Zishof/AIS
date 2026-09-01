package ais.action.master.rab.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import ais.ui.util.MyRadioConfig;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.Proyek;
import ais.ui.util.GetEventListener;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.rab.Proyek} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum (constructor/display/onSearchDefault/
 * renderer/callback).
 *
 * <p>Proyek merepresentasikan kegiatan/proyek pada modul RAB (judul panel popup: "Daftar Kegiatan")
 * yang menaungi rencana anggaran dan terikat ke satu Satuan Kerja serta (opsional) satu Workspace
 * sebagai sumber anggarannya. Popup pencarian menyediakan kriteria {@code nama} ({@code ilike
 * ANYWHERE}) ditambah filter opsional lewat sub-picker {@link AmbilDataSatuanKerjaBanbox}: bila
 * satker dipilih, hasil dibatasi persis ke satker tersebut ({@code Restrictions.eq("satuanKerja",
 * ...)} — berbeda dari {@code AmbilDataMitraBanbox} yang menelusuri hierarki turunan satker, di sini
 * hanya kecocokan langsung); bila tidak dipilih, filter no-op lewat
 * {@code Restrictions.sqlRestriction("1=1")}. Baris grid menampilkan nama proyek, nama satuan kerja,
 * workspace (sumber anggaran) dan nilai anggarannya (hargaTotal, diformat sebagai angka), serta
 * keterangan, lewat {@code ProyekRenderer}; hasil dibungkus {@link org.zkoss.zul.Radiogroup} untuk
 * pemilihan tunggal.</p>
 *
 * <p>Constructor tanpa argumen langsung memanggil {@link #display()} dalam try-catch yang menampilkan
 * error ke admin lewat {@code Common.tampilErrorJikaAdmin(e)} (berbeda dari subclass RAB lain yang
 * memakai {@code ErrorAuditUtil.record} pada catch kosong).</p>
 *
 * @see Bandbox
 */
public class AmbilDataProyekBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;

	private EventListener eventListener;

	/**
	 * Membangun Bandbox picker Proyek dan langsung memanggil {@link #display()}, dibungkus try-catch
	 * yang menampilkan error ke admin lewat {@code Common.tampilErrorJikaAdmin(e)} bila gagal.
	 */
	public AmbilDataProyekBanbox() {
		super();
		try {
			display();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private Textbox nama;
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	/**
	 * Renderer satu baris grid hasil pencarian Proyek: menampilkan radio pilihan, nama proyek, nama
	 * satuan kerja, workspace (sumber anggaran) beserta nilai anggarannya (hargaTotal), dan
	 * keterangan. Listener {@code onCheck} pada radio adalah satu-satunya titik callback pola ini —
	 * lihat penjelasan umum di {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataProyekBanbox
	 */
	class ProyekRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Proyek proyek = (Proyek) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(proyek.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataProyekBanbox.this.setOpen(false);
					AmbilDataProyekBanbox.this.setAttribute("proyek", proyek);
					AmbilDataProyekBanbox.this.setAttribute("myValue", proyek);
					AmbilDataProyekBanbox.this.setValue(proyek.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(proyek.getNama()).setParent(arg0);
			new Label(proyek.getSatuanKerja() == null ? "" : proyek
					.getSatuanKerja().getNama()).setParent(arg0);
			new Label(proyek.getWorkspace() == null ? "" : proyek
					.getWorkspace().toString()).setParent(arg0);
			new Label(proyek.getWorkspace() == null
					|| proyek.getWorkspace().getHargaTotal() == null ? ""
					: Common.numberFormat.get().format(proyek.getWorkspace()
							.getHargaTotal())).setParent(arg0);
			new Label(proyek.getKeterangan()).setParent(arg0);

		}

	}

	/**
	 * Merakit popup pencarian Proyek: form kriteria nama plus sub-picker Satuan Kerja
	 * ({@link AmbilDataSatuanKerjaBanbox}), tombol Cari, dan grid hasil dalam
	 * {@link org.zkoss.zul.Radiogroup} pilih-tunggal, diakhiri memanggil
	 * {@link #onSearchDefault(Event)} agar grid terisi saat popup pertama tampil.
	 *
	 * @throws Exception diteruskan dari pembangunan komponen ZK
	 * @see ais.ui.util.GetEventListener
	 */
	public void display() throws Exception {
		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("600px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Kegiatan");
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

		MyGrid searchgrid = new MyGrid();searchgrid.setWidth("100%");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox());
		satuanKerja.setWidth("90%");

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

		grid = new MyGrid();//grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
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
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Satuan Kerja");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Anggaran dari");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai Anggaran");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		onSearchDefault(null);

	}

	/**
	 * Menjalankan pencarian Proyek berdasar {@code nama} ({@code ilike ANYWHERE}), ditambah filter
	 * satker: bila sub-picker {@link #satuanKerja} memiliki nilai terpilih, hasil dibatasi persis ke
	 * satker tersebut ({@code Restrictions.eq}, tanpa penelusuran turunan); bila kosong, filter ini
	 * no-op. Maksimum {@code Common.MAX_RESULT} baris, urut nama menaik, lalu grid diisi ulang dengan
	 * {@link ProyekRenderer}.
	 *
	 * @param event event pemicu; boleh {@code null} (dipanggil juga dari {@link #display()})
	 * @see ais.ui.util.GetEventListener
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Proyek.class);

		criteria.addOrder(Order.asc("nama"))
				.add(Restrictions.ilike("nama", nama.getText().trim(),
						MatchMode.ANYWHERE))
				.add(satuanKerja.getAttribute("satuanKerja") == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq("satuanKerja",
						satuanKerja.getAttribute("satuanKerja")));
		List<Proyek> proyek = criteria.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(proyek);
		grid.setRowRenderer(new ProyekRenderer());
		grid.setModelCheckMobile(strset);

		

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	public EventListener getEventListener() {
		return eventListener;
	}
}
