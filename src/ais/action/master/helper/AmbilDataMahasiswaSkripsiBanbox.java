package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
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
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
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

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class AmbilDataMahasiswaSkripsiBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;

	public AmbilDataMahasiswaSkripsiBanbox() {
		super();
		setReadonly(true);

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {

					Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

					class SearchFakultasEventListener implements EventListener {

						@Override
						public void onEvent(Event event) throws Exception {
							// TODO Auto-generated method stub
							Common.clear(searchjurusan);
							searchjurusan.setSelectedItem(null);
							if (searchfakultas.getSelectedItem() == null
									|| searchfakultas.getSelectedItem().getValue() == null) {
								return;
							}
							Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang",
									Jurusan.class,
									CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
						}

					}

					searchfakultas.addEventListener("onChange", new SearchFakultasEventListener());

					// Apabila user berwenang hanya di fakultas tertentu, maka
					// user hanya
					// boleh mengakses data fakultas atau jurusan tertentu

					Tbmuser tbmuser = Common.getCurrentUser();
					if (tbmuser.ambilFakultas() != null) {
						Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
						Common.clear(searchjurusan);
						Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang",
								Jurusan.class, Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
						searchfakultas.setDisabled(true);
					} else {
						searchfakultas.setDisabled(false);
					}

					if (tbmuser.ambilJurusan() != null) {
						Common.selectComboItem(searchjurusan, tbmuser.ambilJurusan());
						searchjurusan.setDisabled(true);
					} else {
						searchjurusan.setDisabled(false);
					}

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

	private Textbox nim;
	private Textbox nama;
	private Decimalbox tahunangkatan;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		public MahasiswaRenderer() {

		}

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;
			final MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					AmbilDataMahasiswaSkripsiBanbox.this.setOpen(false);
					AmbilDataMahasiswaSkripsiBanbox.this.setAttribute("mahasiswa", mahasiswa);
					AmbilDataMahasiswaSkripsiBanbox.this.setAttribute("myValue", mahasiswa);
					AmbilDataMahasiswaSkripsiBanbox.this.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);

		}
	}

	public void display() {
		setReadonly(true);
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

		// Listener pencarian bersama: dipakai tombol Cari DAN tombol Enter (onOK) di tiap field.
		final EventListener listenerCari = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		};

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(nim = new Textbox());
		nim.setWidth("90%");
		nim.addEventListener("onOK", listenerCari); // Enter di NIM → cari

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");
		nama.addEventListener("onOK", listenerCari); // Enter di Nama → cari

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(tahunangkatan = new Decimalbox());
		tahunangkatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		// Tombol "Cari" SEBARIS pada baris filter agar SELALU terlihat (tak tergantung tinggi North).
		MyToolbarbuttonConfig btnCariSebaris = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		btnCariSebaris.addEventListener("onClick", listenerCari);
		row.appendChild(btnCariSebaris);

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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* setPageSize legacy dihapus: grid bukan mold "paging" sehingga setPageSize melempar IllegalStateException ("Available only the paging mold") dan daftar tidak pernah tampil. Paging ditangani AmbilDataPagingHelper. */
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
		column.setLabel("NIM");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Angkatan");
		column.setWidth("25%");

		onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		try {
			Session session = HibernateUtil.currentSession();
			List<Mahasiswa> mahasiswa =
					session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))
							.add(Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))
							.add(Restrictions.ilike("nim", nim.getText().trim(), MatchMode.ANYWHERE))
							.add(tahunangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("tahunangkatan", tahunangkatan.getValue().intValue()))

							.add(searchjurusan.getSelectedItem() == null
									|| searchjurusan.getSelectedItem().getValue() == null
									|| searchjurusan.getSelectedItem().getValue() == null
											? Restrictions.sqlRestriction("1=1")
											: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

							.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

							.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

							.setMaxResults(20).list();

			ListModel strset = new SimpleListModel(mahasiswa);
			grid.setRowRenderer(new MahasiswaRenderer());
			grid.setModelCheckMobile(strset);
		} catch (Exception e) {
			// FIX: pencarian ini memakai HibernateUtil.currentSession() (sesi kontekstual per-request
			// -- TIDAK boleh ditutup manual di sini, lihat finally di method lain yg pakai openSession()).
			// Kriteria .list() bisa memicu auto-flush entity Mahasiswa lain yang sedang kotor di sesi
			// yang sama; kalau baris itu sedang dikunci proses lain, auto-flush kena lock timeout
			// PostgreSQL (55P03) dan sebelumnya exception ini lolos mentah ke ZK, menampilkan stack
			// trace teknis ke pengguna. Tangkap & tampilkan pesan ramah, konsisten dgn pola messagebox
			// pencarian lain di codebase ini.
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/action/master/helper/AmbilDataMahasiswaSkripsiBanbox.java:onSearchDefault");
			try {
				// onSearchDefault() tidak mendeklarasikan "throws Exception" (dipanggil langsung dari
				// display() yang juga tanpa throws) -- MyMessageboxConfig.show() checked-throws
				// InterruptedException, jadi ditangkap lokal di sini alih-alih mengubah signature method.
				ais.ui.util.MyMessageboxConfig.show(
						"Data sedang digunakan pengguna lain, silakan coba lagi.", "Peringatan",
						ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.EXCLAMATION);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
