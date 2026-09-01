package ais.action.master.rab.helper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.Sasaran;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.GetEventListener;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.rab.Sasaran} —
 * lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum (constructor/display/
 * onSearchDefault/renderer/callback). {@code Sasaran} adalah sasaran/target dalam struktur RAB
 * (Rencana Anggaran Biaya) yang menjadi acuan penyusunan rencana anggaran suatu satuan kerja.
 *
 * <p>
 * Kriteria pencarian: {@code kode} dan {@code nama} (masing-masing {@code ilike} kontains, tidak
 * peka huruf besar/kecil). Pemilihan bersifat tunggal, ditampilkan lewat {@link Radiogroup}.
 * </p>
 * <p>
 * <b>Filter non-trivial — scoping per Satuan Kerja:</b> popup pencarian menyertakan sub-picker
 * {@link AmbilDataSatuanKerjaBanbox} ({@code satuanKerja}). Bila pengguna memilih satuan kerja di
 * sana, hasil sasaran dibatasi pada satuan kerja tersebut BESERTA seluruh anak-cucunya (dihitung
 * lewat {@link SatuanKerjaTreeModel#getChildsSet}), dan baris {@code Sasaran} tanpa satuan kerja
 * (global) ikut disembunyikan. Bila tidak ada satuan kerja yang dipilih, pencarian dibatasi pada
 * scope satuan kerja pengguna saat ini ({@code SekolahUtil.ambilSatuanKerjas()}) ditambah
 * {@code Sasaran} yang satuan kerjanya null (sasaran global tanpa pemilik satker). Memilih satuan
 * kerja pada sub-picker memicu {@code onSearchDefault(null)} ulang lewat listener miliknya sendiri.
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataSasaranBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;

	private EventListener eventListener;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * Constructor mengikuti kerangka standar (lihat {@link ais.ui.util.GetEventListener}), dengan
	 * tambahan menyiapkan {@link SatuanKerjaTreeModel} (mode non-lazy, {@code false}) yang dipakai
	 * {@link #onSearchDefault(Event)} untuk menghitung anak-cucu satuan kerja saat scoping filter.
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	public AmbilDataSasaranBanbox() {
		super();
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		try {
			display();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private Textbox kode;
	private Textbox nama;
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	/**
	 * Renderer baris grid hasil pencarian {@link Sasaran}, mengikuti kerangka renderer batin standar
	 * pola Bandbox picker. Merender kolom Kode, Nama, Keterangan, plus radio pilihan yang saat
	 * dicentang menutup popup, menyimpan entity {@code Sasaran} terpilih ke atribut
	 * {@code "sasaran"}/{@code "myValue"} dan teks tampil {@code "kode - nama"} pada Bandbox, lalu
	 * meneruskan event ke {@link #eventListener} bila terpasang.
	 *
	 * @see ais.ui.util.GetEventListener
	 */
	class SasaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Sasaran sasaran = (Sasaran) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			// checkbox.setId(sasaran.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataSasaranBanbox.this.setOpen(false);
					AmbilDataSasaranBanbox.this.setAttribute("sasaran", sasaran);
					AmbilDataSasaranBanbox.this.setAttribute("myValue", sasaran);
					AmbilDataSasaranBanbox.this.setValue(sasaran.getKode() + " - " + sasaran.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(sasaran.getKode()).setParent(arg0);
			new Label(sasaran.getNama()).setParent(arg0);
			new Label(sasaran.getKeterangan()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (form Kode/Nama/Satuan Kerja + tombol Cari + grid hasil ber-radio)
	 * mengikuti kerangka standar, lalu memanggil {@link #onSearchDefault(Event)} agar grid terisi
	 * saat popup pertama dibuka.
	 *
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

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(this.satuanKerja = new AmbilDataSatuanKerjaBanbox());
		satuanKerja.setWidth("90%");
		satuanKerja.setEventListener(new EventListener() {

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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.getPagingChild().setMold("os");
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
	 * Menjalankan pencarian {@link Sasaran} berdasarkan {@code kode}/{@code nama} (ilike, opsional)
	 * diurutkan berdasar nama, dengan filter scoping satuan kerja seperti dijelaskan di Javadoc
	 * kelas (menyertakan anak-cucu satuan kerja terpilih via {@link SatuanKerjaTreeModel}, atau
	 * scope satuan kerja pengguna + sasaran global bila belum ada satuan kerja dipilih). Hasil
	 * dipasang ke {@link #grid} lewat {@link SasaranRenderer}.
	 *
	 * @param event event pemicu (tidak dipakai isinya; boleh {@code null} untuk pencarian awal)
	 * @see ais.ui.util.GetEventListener
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		SatuanKerja parent = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Sasaran.class);

		criteria.addOrder(Order.asc("nama")).add(Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", kode.getText().trim(), MatchMode.ANYWHERE))
				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas)));
		List<Sasaran> sasaran = criteria.setMaxResults(Common.MAX_RESULT).list();

		// // System.out.println(sasaran);
		ListModel strset = new SimpleListModel(sasaran);
		grid.setRowRenderer(new SasaranRenderer());
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
