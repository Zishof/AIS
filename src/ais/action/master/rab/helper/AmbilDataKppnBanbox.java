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
import ais.database.model.rab.Kppn;
import ais.ui.util.GetEventListener;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.rab.Kppn} — lihat
 * {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum (constructor/display/onSearchDefault/
 * renderer/callback).
 *
 * <p>KPPN (Kantor Pelayanan Perbendaharaan Negara) adalah unit vertikal Ditjen Perbendaharaan yang menjadi
 * mitra kerja satuan kerja dalam pencairan anggaran; pada modul RAB, KPPN dipilih sebagai referensi satker
 * saat menyusun dokumen anggaran/pengadaan. Popup pencarian menyediakan dua kriteria teks yang dicocokkan
 * dengan {@code Restrictions.ilike(..., MatchMode.ANYWHERE)}: {@code kode} dan {@code nama} KPPN (tanpa
 * penjagaan "kosong = 1=1" — nilai kosong tetap lolos ke {@code ilike} karena pola ANYWHERE terhadap string
 * kosong otomatis mencocokkan semua baris). Hasil dibungkus {@link org.zkoss.zul.Radiogroup} sehingga
 * pemilihan bersifat tunggal (satu KPPN per pemilihan), dengan baris grid menampilkan kode, nama, dan
 * keterangan lewat {@code KppnRenderer}. Paging memakai mode client-side lama ({@code grid.setMold("paging")}
 * + {@code setPageSize(50)}) dengan hasil query dibatasi {@code Common.MAX_RESULT}, bukan
 * {@code AmbilDataPagingHelper} server-side.</p>
 *
 * <p><b>Catatan penyimpangan kecil dari kerangka standar:</b> constructor kelas ini memanggil
 * {@code display()} langsung, TANPA listener {@code onOpen} lazy-build + {@code Common#createDefaultTimer}
 * yang dipakai kebanyakan subclass sejenis lain — popup karena itu dibangun sekali saat instance dibuat,
 * bukan ditunda sampai Bandbox pertama kali diklik.</p>
 *
 * @see Bandbox
 */
public class AmbilDataKppnBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;

	private EventListener eventListener;

	/**
	 * Membangun Bandbox picker KPPN dan langsung memanggil {@link #display()} untuk merakit popup
	 * pencarian (lihat catatan penyimpangan pola pada Javadoc kelas — tidak memakai lazy-build via
	 * {@code onOpen}). Blok kode terkomentari di bawah adalah sisa percobaan pra-isi nilai default
	 * dari KPPN pertama dan sengaja tidak aktif.
	 */
	public AmbilDataKppnBanbox() {
		super();
		// Kppn kppn = (Kppn) HibernateUtil.currentSession()
		// .createCriteria(Kppn.class)
		// MaxResults(1)
		// .uniqueResult();
		//
		// if (kppn != null) {
		// setValue(kppn.getKode() + " - " + kppn.getNama());
		// setAttribute("kppn", kppn);
		// setAttribute("myValue", kppn);
		// }
		display();
	}

	private Textbox kode;
	private Textbox nama;

	/**
	 * Renderer satu baris grid hasil pencarian KPPN: menampilkan radio pilihan, kode, nama, dan
	 * keterangan. Listener {@code onCheck} pada radio adalah satu-satunya titik callback pola ini —
	 * lihat penjelasan umum di {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataKppnBanbox
	 */
	class KppnRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Kppn kppn = (Kppn) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(kppn.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataKppnBanbox.this.setOpen(false);
					AmbilDataKppnBanbox.this.setAttribute("kppn", kppn);
					AmbilDataKppnBanbox.this.setAttribute("myValue", kppn);
					AmbilDataKppnBanbox.this.setValue(kppn.getKode() + " - "
							+ kppn.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(kppn.getKode()).setParent(arg0);
			new Label(kppn.getNama()).setParent(arg0);
			new Label(kppn.getKeterangan()).setParent(arg0);

		}

	}

	/**
	 * Merakit popup pencarian KPPN (form kriteria kode/nama, tombol Cari, grid hasil dalam
	 * {@link org.zkoss.zul.Radiogroup} pilih-tunggal) lalu memanggil {@link #onSearchDefault(Event)}
	 * agar grid terisi saat popup pertama tampil.
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	public void display() {
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
		panel.setTitle("Daftar Satuan Kerja");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox());
		kode.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

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
		column.setLabel("Kode");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("40%");

		onSearchDefault(null);

	}

	/**
	 * Menjalankan pencarian KPPN berdasar {@code kode} dan {@code nama} (keduanya
	 * {@code ilike ANYWHERE}, maks {@code Common.MAX_RESULT} baris, urut nama menaik) lalu mengisi
	 * ulang grid dengan {@link KppnRenderer}.
	 *
	 * @param event event pemicu; boleh {@code null} (dipanggil juga dari {@link #display()})
	 * @see ais.ui.util.GetEventListener
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Kppn.class);

		criteria.addOrder(Order.asc("nama"))
				.add(Restrictions.ilike("nama", nama.getText().trim(),
						MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kode.getText().trim(),
						MatchMode.ANYWHERE));
		List<Kppn> kppn = criteria.setMaxResults(Common.MAX_RESULT).list();

		// // System.out.println(kppn);
		ListModel strset = new SimpleListModel(kppn);
		grid.setRowRenderer(new KppnRenderer());
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
