package ais.action.master.helper;

import java.util.List;

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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.UploadLogInfo;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Tipe khusus untuk ambil data upload log banbox. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code
 * ais.ui.util.AmbilDataPagingHelper pagingHelper}, {@code EventListener eventListener}, {@code MyDatebox start},
 * {@code MyDatebox end}, {@code Tbmuser tbmuser}, {@code Textbox nama}; pembacaan/pencarian ({@code
 * onSearchDefault()}, {@code setEventListener()}, {@code getEventListener()}); operasi domain lain ({@code
 * display()}); konfigurasi constructor: {@code tbmuser}. Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Bandbox
 */
public class AmbilDataUploadLogBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private MyDatebox start;
	private MyDatebox end;
	private Tbmuser tbmuser = null;

	public AmbilDataUploadLogBanbox() {
		super();
		setReadonly(true);
		tbmuser = Common.getCurrentUser();
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

	class UploadLogRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final UploadLogInfo uploadLog = (UploadLogInfo) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataUploadLogBanbox.this.setOpen(false);
					AmbilDataUploadLogBanbox.this.setAttribute("uploadLog", uploadLog);
					AmbilDataUploadLogBanbox.this.setValue(uploadLog.toString());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(uploadLog.getNama()).setParent(arg0);
			if (uploadLog.getDiuploadOleh() != null) {
				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				CommonMedia.tampilkanGambarKecil(uploadLog.getDiuploadOleh()).setParent(vbox);
				new Label(uploadLog.getDiuploadOleh().getUserNama()).setParent(vbox);
			} else {
				new Label(uploadLog.getOlehId()).setParent(arg0);
			}
			new Label(Common.dateFormat5.get().format(uploadLog.getTanggal_dirubah())).setParent(arg0);

		}

	}

	public void display() {
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("800px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Upload Log");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama File"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Tgl. Upload"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(start = new MyDatebox());
		start.setCols(5);
		hbox.appendChild(end = new MyDatebox());
		end.setCols(5);

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
		/* setPageSize legacy dihapus: grid bukan mold "paging" sehingga setPageSize melempar IllegalStateException ("Available only the paging mold") dan daftar tidak pernah tampil. Paging ditangani AmbilDataPagingHelper. */
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
		column.setLabel("");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Oleh");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu");

		onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<UploadLogInfo> uploadLog = session.createCriteria(UploadLogInfo.class).addOrder(Order.desc("id"))
				.add(tbmuser == null ? Restrictions.sqlRestriction("false")
						: Restrictions.or(Restrictions.isNull("diuploadOleh"),
								Restrictions.eq("diuploadOleh", tbmuser)))
				.add(nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))
				.add(start == null || start.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("(this_.tanggal_dirubah) >= ('"
								+ Common.databaseDateFormat.get().format(start.getValue()) + " 00:00:00')"))

				.add(end == null || end.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("(this_.tanggal_dirubah) <= ('"
								+ Common.databaseDateFormat.get().format(end.getValue()) + " 23:59:59')"))
				.setMaxResults(Common.MAX_RESULT)

				.list();

		System.out.println(uploadLog);
		ListModel strset = new SimpleListModel(uploadLog);
		grid.setRowRenderer(new UploadLogRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}

