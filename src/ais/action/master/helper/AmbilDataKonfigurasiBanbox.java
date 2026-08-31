package ais.action.master.helper;

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
import org.zkoss.zul.Combobox;
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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.ui.util.GetEventListener;

/**
 * Tipe khusus untuk ambil data konfigurasi banbox. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code Textbox nilai},
 * {@code Textbox nama}, {@code Combobox ta}, {@code Textbox info}; pembacaan/pencarian ({@code
 * onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code
 * display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataKonfigurasiBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	public AmbilDataKonfigurasiBanbox() {
		super();

		display();
	}

	private Textbox nilai;
	private Textbox nama;
	private Combobox ta;
	private Textbox info;

	class KonfigurasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Konfigurasi konfigurasi = (Konfigurasi) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataKonfigurasiBanbox.this.setOpen(false);
					AmbilDataKonfigurasiBanbox.this.setAttribute("konfigurasi", konfigurasi);
					AmbilDataKonfigurasiBanbox.this.setAttribute("myValue", konfigurasi);
					AmbilDataKonfigurasiBanbox.this.setValue(konfigurasi == null ? ""
							: konfigurasi.getNama() + "-" + konfigurasi.getNilai() + ""
									+ (konfigurasi.getTahunAkademik() == null ? ""
											: "-" + konfigurasi.getTahunAkademik()));
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(konfigurasi.getNama()).setParent(arg0);
			new Label(konfigurasi.getNilai()).setParent(arg0);
			new Label(konfigurasi.getTahunAkademik()).setParent(arg0);
			new Label(konfigurasi.getInfo1()).setParent(arg0);
			new Label(konfigurasi.getInfo2()).setParent(arg0);
			new Label(konfigurasi.getInfo3()).setParent(arg0);
			new Label(konfigurasi.getInfo4()).setParent(arg0);
			new Label(konfigurasi.getInfo5()).setParent(arg0);
			new Label(konfigurasi.getKeterangan()).setParent(arg0);

		}

	}

	public void display() {
		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("90%");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Konfigurasi");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai"));
		row.appendChild(nilai = new Textbox());
		nilai.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Info"));
		row.appendChild(info = new Textbox());
		info.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(ta = new Combobox());
		Common.generateTahunAjaranDanSemua(ta);
		Common.selectComboItem(ta, null);
		ta.setWidth("90%");

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
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("TA");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Info1");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Info2");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Info3");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Info4");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Info5");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ket.");

		onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Konfigurasi.class).addOrder(Order.desc("id"));

		criteria.addOrder(Order.asc("nama"))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(info.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("info5", info.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("info4", info.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("info3", info.getValue().trim(), MatchMode.ANYWHERE),
												Restrictions.or(
														Restrictions.ilike("info1", info.getValue().trim(),
																MatchMode.ANYWHERE),
														Restrictions.ilike("info2", info.getValue().trim(),
																MatchMode.ANYWHERE)))))

				)

				.add(ta.getSelectedItem() == null || ta.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", ta.getSelectedItem().getValue()))
				.add(nilai.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nilai", nilai.getValue().trim(), MatchMode.ANYWHERE));

		List<Konfigurasi> konfigurasi = criteria.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(konfigurasi);
		grid.setRowRenderer(new KonfigurasiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
