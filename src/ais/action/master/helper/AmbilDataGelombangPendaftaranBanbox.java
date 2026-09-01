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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity
 * {@link ais.database.model.GelombangPendaftaran} — lihat {@link ais.ui.util.GetEventListener}
 * untuk arsitektur kerangka umum (constructor/display/onSearchDefault/renderer/callback). {@code
 * GelombangPendaftaran} adalah gelombang/periode pendaftaran mahasiswa baru (PMB) — mis.
 * "Gelombang 1", "Gelombang 2" — masing-masing terikat ke satu tahun akademik dan jenis semester.
 * <p>
 * Popup menampilkan grid pilih-tunggal (via {@link Radiogroup}/{@link Radio}) dengan filter "Nama"
 * (ILIKE ANYWHERE) dan "TA" (combobox tahun akademik, diisi lewat {@code Common.generateTahunAjaran}
 * dan otomatis dipraseleksi ke konfigurasi {@code tahunAkademikPenerimaanMahasiswaBaru} atau tahun
 * akademik berjalan bila konfigurasi kosong). Bila komponen dibuat dalam konteks satu
 * {@link PerguruanTinggi}, hasil dibatasi ke gelombang milik perguruan tinggi tersebut (atau tanpa
 * perguruan tinggi). Diurutkan menurun berdasarkan id, dibatasi {@link Common#MAX_RESULT_50} baris.
 *
 * @see Bandbox
 */
public class AmbilDataGelombangPendaftaranBanbox extends Bandbox implements GetEventListener {

	/**
	 * Serial version UID standar untuk kompatibilitas serialisasi komponen ZK.
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private PerguruanTinggi perguruanTinggi;

	/**
	 * Membangun komponen: mendeteksi {@link PerguruanTinggi} aktif untuk scoping hasil, lalu
	 * memasang listener {@code onOpen} yang, pada pembukaan pertama, membangun popup
	 * ({@link #display()}), mengikuti kerangka umum di {@link ais.ui.util.GetEventListener}.
	 */
	public AmbilDataGelombangPendaftaranBanbox() {
		super();

		setReadonly(true);

		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
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
	private Combobox ta;
	private EventListener eventListener;

	/** @return listener pemilihan gelombang pendaftaran yang sedang terpasang, boleh {@code null} */
	public EventListener getEventListener() {
		return eventListener;
	}

	/** @param eventListener dipanggil setiap kali user memilih satu gelombang pendaftaran */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * Merender satu baris grid: radio pilih berlabel nama gelombang, tahun akademik, dan jenis
	 * semester. Memilih baris menutup popup, menyimpan entity {@link GelombangPendaftaran}
	 * terpilih ke attribute {@code "gelombangPendaftaran"}/{@code "myValue"} pada Bandbox,
	 * mengisi teks tampilan dengan namanya, lalu memicu {@link #eventListener} bila terpasang —
	 * mengikuti kerangka callback standar di {@link ais.ui.util.GetEventListener}.
	 *
	 * @see AmbilDataGelombangPendaftaranBanbox
	 */
	class CalonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final GelombangPendaftaran gelombangPendaftaran = (GelombangPendaftaran) arg1;
			Radio checkbox = new Radio(gelombangPendaftaran.getNama());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", gelombangPendaftaran);
			// checkbox.setId(gelombangPendaftaran.getId() + "");

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataGelombangPendaftaranBanbox.this.setOpen(false);
					AmbilDataGelombangPendaftaranBanbox.this.setAttribute("gelombangPendaftaran", gelombangPendaftaran);
					AmbilDataGelombangPendaftaranBanbox.this.setAttribute("myValue", gelombangPendaftaran);
					AmbilDataGelombangPendaftaranBanbox.this.setValue(gelombangPendaftaran.getNama());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(gelombangPendaftaran.getTahunAkademik()).setParent(arg0);
			new Label(gelombangPendaftaran.getJenisSemester()).setParent(arg0);

		}

	}

	/**
	 * Membangun popup pencarian (dipanggil sekali saat pertama dibuka): form filter Nama/TA
	 * (dengan TA otomatis dipraseleksi ke konfigurasi tahun akademik PMB), grid hasil bermold
	 * "paging", lalu memuat data awal lewat {@link #onSearchDefault(Event)}.
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
		panel.setTitle("Daftar Gelombang");
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

		row.appendChild(new ais.ui.util.MyLabelConfig("TA"));
		row.appendChild(ta = new Combobox());
		ta.setWidth("90%");
		Common.generateTahunAjaran(ta);

		Common.selectComboItem(ta, Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai());

		nama.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		ta.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		Toolbar toolbar = new Toolbar();
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

		grid = new MyGrid();
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
		column.setLabel("Nama");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Akademik");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Semester");
		column.setWidth("10%");

		onSearchDefault(null);

	}

	/**
	 * Menyusun dan menjalankan kriteria pencarian {@link GelombangPendaftaran}: cocok nama (ILIKE
	 * ANYWHERE), cocok tahun akademik terpilih, dan (bila {@link #perguruanTinggi} diset)
	 * dibatasi ke gelombang milik perguruan tinggi tersebut atau tanpa perguruan tinggi;
	 * diurutkan menurun berdasarkan id, dibatasi {@link Common#MAX_RESULT_50} baris. Mengisi
	 * ulang grid dengan hasilnya beserta {@link CalonMahasiswaRenderer}.
	 *
	 * @param event tidak dipakai, hanya mengikuti signature standar listener pencarian
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		System.out.println("onSearchDefault");
		Criteria criteria = session.createCriteria(GelombangPendaftaran.class).addOrder(Order.desc("id"))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(ta.getSelectedItem() == null || ta.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunAkademik", ta.getSelectedItem().getValue()))

				.setMaxResults(Common.MAX_RESULT_50);

		if (perguruanTinggi != null) {
			criteria.add(Restrictions.or(Restrictions.isNull("perguruanTinggi"),
					Restrictions.eq("perguruanTinggi", perguruanTinggi)));
		}

		List<GelombangPendaftaran> gelombangPendaftaran = criteria.list();

		ListModel strset = new SimpleListModel(gelombangPendaftaran);
		grid.setRowRenderer(new CalonMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}
}
