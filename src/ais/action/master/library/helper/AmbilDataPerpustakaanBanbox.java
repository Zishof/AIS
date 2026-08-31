package ais.action.master.library.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
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
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Perpustakaan;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Tipe khusus untuk ambil data perpustakaan banbox. Kelas ini memberi nama dan batas tanggung
 * jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code Boolean
 * satuanKerjaAktif}, {@code MyTextbox kodePerpustakaanan}, {@code MyTextbox nama}, {@code
 * AmbilDataSatuanKerjaBanbox cariSatuanKerja}; inisialisasi/lifecycle ({@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()}, {@code
 * getSatuanKerja()}); mutasi data ({@code setSatuanKerja()}); operasi domain lain ({@code display()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataPerpustakaanBanbox extends Bandbox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private Boolean satuanKerjaAktif = false;

	private void init(Boolean defaultPerpustakaan) throws Exception {
		Perpustakaan currentPerpustakaan = Common.getCurrentPerpustakaan(true);
		if (defaultPerpustakaan && currentPerpustakaan != null) {
			setAttribute("perpustakaan", currentPerpustakaan);
			setValue(currentPerpustakaan.toString());
			setDisabled(true);
		} else {
			this.addEventListener(Events.ON_OK, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (getValue().trim().equals("")) {
						setAttribute("perpustakaan", null);
						setValue("");
						return;
					}

					Perpustakaan perpustakaan = (Perpustakaan) HibernateUtil.currentSession()
							.createCriteria(Perpustakaan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.ilike("kode", AmbilDataPerpustakaanBanbox.this.getValue().trim(),
									MatchMode.EXACT))
							.setMaxResults(1).uniqueResult();
					if (perpustakaan == null) {
						MyMessageboxConfig.show(
								"Perpustakaan dengan kode = " + AmbilDataPerpustakaanBanbox.this.getValue().trim()
										+ " tidak dperpustakaanukan",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}
					AmbilDataPerpustakaanBanbox.this.setOpen(false);
					AmbilDataPerpustakaanBanbox.this.setAttribute("perpustakaan", perpustakaan);
					AmbilDataPerpustakaanBanbox.this.setValue(perpustakaan.toString());
					if (eventListener != null) {
						eventListener.onEvent(arg0);
					}
				}
			});

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

			int s = ((Number) initCriteria(false).setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (s == 1) {
				currentPerpustakaan = (Perpustakaan) ConstantValues.simpleObject(initCriteria(false).setMaxResults(1),
						Perpustakaan.class);
				if (currentPerpustakaan != null) {
					setAttribute("perpustakaan", currentPerpustakaan);
					setValue(currentPerpustakaan.toString());
					setDisabled(true);
				}
			}
		}
	}

	public AmbilDataPerpustakaanBanbox() throws Exception {
		super();
		init(true);
	}

	public AmbilDataPerpustakaanBanbox(Boolean defaultPerpustakaan) throws Exception {
		init(defaultPerpustakaan);
	}

	public AmbilDataPerpustakaanBanbox(Boolean defaultPerpustakaan, Boolean satuanKerjaAktif) throws Exception {
		this.satuanKerjaAktif = satuanKerjaAktif;
		init(defaultPerpustakaan);
	}

	private MyTextbox kodePerpustakaanan;
	private MyTextbox nama;
	private AmbilDataSatuanKerjaBanbox cariSatuanKerja;

	class PerpustakaanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Perpustakaan perpustakaan = (Perpustakaan) arg1;

			arg0.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataPerpustakaanBanbox.this.setOpen(false);
					AmbilDataPerpustakaanBanbox.this.setAttribute("perpustakaan", perpustakaan);
					AmbilDataPerpustakaanBanbox.this.setValue(perpustakaan.toString());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(perpustakaan.getKode()).setParent(arg0);
			new Label(perpustakaan.getNama()).setParent(arg0);
			new Label(perpustakaan.getSatuanKerja() == null ? "" : perpustakaan.getSatuanKerja().toString())
					.setParent(arg0);

		}

	}

	public void display() throws Exception {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("800px");
		bandpopup.setHeight("600px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(bandpopup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Perpustakaan");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kodePerpustakaanan = new MyTextbox());
		kodePerpustakaanan.setWidth("90%");
		kodePerpustakaanan.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(cariSatuanKerja = new AmbilDataSatuanKerjaBanbox(true));
		cariSatuanKerja.setWidth("90%");
		cariSatuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		if (satuanKerjaAktif) {
			cariSatuanKerja.setValue("");
			cariSatuanKerja.setAttribute("satuanKerja", null);
			cariSatuanKerja.setAttribute("myValue", null);
			cariSatuanKerja.setDisabled(false);
		}

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
		column.setLabel("Kode");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Satuan Kerja");
		column.setWidth("25%");

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Perpustakaan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(kodePerpustakaanan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", kodePerpustakaanan.getValue().trim(), MatchMode.ANYWHERE))
				.add(cariSatuanKerja.getAttribute("satuanKerja") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("satuanKerja", cariSatuanKerja.getAttribute("satuanKerja")));

		if (order) {
			criteria.addOrder(Order.asc("nama"));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		List<Perpustakaan> perpustakaan = initCriteria(true).setMaxResults(Common.MAX_RESULT).list();

		System.out.println(perpustakaan);
		ListModel strset = new SimpleListModel(perpustakaan);
		grid.setRowRenderer(new PerpustakaanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public SatuanKerja getSatuanKerja() {
		return (SatuanKerja) cariSatuanKerja.getAttribute("satuanKerja");
	}

	public void setSatuanKerja(SatuanKerja satuanKerja) {
		if (cariSatuanKerja == null) {
			return;
		}
		cariSatuanKerja.setAttribute("satuanKerja", satuanKerja);
		cariSatuanKerja.setValue(satuanKerja == null ? "" : satuanKerja.toString());
		cariSatuanKerja.setDisabled(satuanKerja != null);
		setValue("");
		setAttribute("perpustakaan", null);
		onSearchDefault(null);
	}

}
