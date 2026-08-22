package ais.action.master.helper;
import ais.common.PesanFormalHelper;


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
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
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

import ais.action.master.JamPerkuliahanAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.JamPerkuliahan;
import ais.database.model.Jurusan;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyPanel;

public class AmbilDataJamPerkuliahanBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;

	private EventListener eventListener;
	private Jurusan jurusan;
	private boolean edit = false;
	private boolean delete = false;

	public AmbilDataJamPerkuliahanBanbox(Jurusan jurusan) {
		super();
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.jurusan = jurusan;
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		setReadonly(true);
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

	public void setJurusan(Jurusan jurusan) throws Exception {

		setValue("");
		setAttribute("jamPerkuliahan", null);
		setAttribute("myValue", null);
		eventListener.onEvent(null);
		Common.clear(this);
		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		this.jurusan = jurusan;
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		display();
	}

	private Textbox nama;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Jurusan selectedJurusan;
	private Combobox searchprogram;
	private Fakultas selectedFakultas;
	private boolean admin;

	class JamPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JamPerkuliahan jamPerkuliahan = (JamPerkuliahan) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataJamPerkuliahanBanbox.this.setOpen(false);
					AmbilDataJamPerkuliahanBanbox.this.setAttribute("jamPerkuliahan", jamPerkuliahan);
					AmbilDataJamPerkuliahanBanbox.this.setAttribute("myValue", jamPerkuliahan);
					AmbilDataJamPerkuliahanBanbox.this.setValue(jamPerkuliahan.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(jamPerkuliahan.getNama()).setParent(arg0);
			new Label(jamPerkuliahan.getWaktuMulai()).setParent(arg0);
			new Label(jamPerkuliahan.getWaktuSelesai()).setParent(arg0);
			new Label(jamPerkuliahan.getJurusan() == null ? "Semua" : jamPerkuliahan.getJurusan().getNama())
					.setParent(arg0);
			new Label(jamPerkuliahan.getFakultas() == null ? "Semua" : jamPerkuliahan.getFakultas().getNama())
					.setParent(arg0);
			new Label(jamPerkuliahan.getProgram() == null ? "Semua" : jamPerkuliahan.getProgram()).setParent(arg0);

			if (!admin) {
				new Label().setParent(arg0);
			} else {
				Hbox toolbar = new Hbox();

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
				button.setTooltiptext("Ubah Data");
				button.setVisible(edit);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						JamPerkuliahanAction.onAddExternal(event, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								JamPerkuliahan jamPerkuliahan = (JamPerkuliahan) arg0.getData();
								AmbilDataJamPerkuliahanBanbox.this.setOpen(false);
								AmbilDataJamPerkuliahanBanbox.this.setAttribute("jamPerkuliahan", jamPerkuliahan);
								AmbilDataJamPerkuliahanBanbox.this.setAttribute("myValue", jamPerkuliahan);
								AmbilDataJamPerkuliahanBanbox.this.setValue(jamPerkuliahan.getNama());
								if (eventListener != null) {
									eventListener.onEvent(arg0);
								}

							}
						}, jamPerkuliahan);
					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.setVisible(delete);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = new Integer(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												JamPerkuliahan currentJamPerkuliahan = (JamPerkuliahan) getAttribute(
														"jamPerkuliahan");

												if (currentJamPerkuliahan != null && jamPerkuliahan.getId()
														.equals(currentJamPerkuliahan.getId())) {

													AmbilDataJamPerkuliahanBanbox.this.setAttribute("jamPerkuliahan",
															null);
													AmbilDataJamPerkuliahanBanbox.this.setAttribute("myValue", null);
													AmbilDataJamPerkuliahanBanbox.this.setValue("");

													if (eventListener != null) {
														eventListener.onEvent(event);
													}

												}

												JamPerkuliahanAction.onDelete(jamPerkuliahan);

												onSearchDefault(event);
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
											}

										}

									}
								});

					}
				});
				button.setParent(toolbar);
				ais.ui.util.MenuAksiBaris.pasang(toolbar);
				toolbar.setParent(arg0);
			}
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

		MyPanel panel = new MyPanel();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Jam Perkuliahan");
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
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		Common.selectComboItem(searchfakultas, jurusan == null ? null : jurusan.getFakultas());
		searchfakultas.setDisabled(jurusan != null);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		Common.insertCombo(searchjurusan, "nama", Jurusan.class,
				Restrictions.eq("fakultas", jurusan == null ? null : jurusan.getFakultas()));
		Common.selectComboItem(searchjurusan, jurusan);
		searchjurusan.setDisabled(jurusan != null);

		if (selectedFakultas != null) {
			Common.selectComboItem(this.searchfakultas, selectedFakultas);
		}

		if (selectedJurusan != null) {
			Common.selectComboItem(this.searchjurusan, selectedJurusan);
		}

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram = new Combobox());
		searchprogram.setWidth("90%");

		Common.initPrograms(searchprogram);
		Common.checkProgramString(searchprogram, true);

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
		admin = Common.getApakahAdmin();
		if (admin) {
			button = new MyToolbarbuttonConfig("Tambah Jam Perkuliahan", "/img/new.gif");
			button.setTooltiptext("Tambah Jam Perkuliahan");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					JamPerkuliahan jamPerkuliahan = new JamPerkuliahan();
					jamPerkuliahan.setJurusan(jurusan);
					jamPerkuliahan.setFakultas(jurusan == null ? null : jurusan.getFakultas());

					JamPerkuliahanAction.onAddExternal(event, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							JamPerkuliahan jamPerkuliahan = (JamPerkuliahan) arg0.getData();
							AmbilDataJamPerkuliahanBanbox.this.setOpen(false);
							AmbilDataJamPerkuliahanBanbox.this.setAttribute("jamPerkuliahan", jamPerkuliahan);
							AmbilDataJamPerkuliahanBanbox.this.setAttribute("myValue", jamPerkuliahan);
							AmbilDataJamPerkuliahanBanbox.this.setValue(jamPerkuliahan.getNama());
							if (eventListener != null) {
								eventListener.onEvent(arg0);
							}
							onSearchDefault(arg0);
						}
					}, jamPerkuliahan);
				}

			});
			button.setParent(toolbar);
		}

		grid = new MyGrid();
		// FIX tampilan: grid daftar jam WAJIB lebar 100% agar mengisi penuh popup (sebelumnya baris ini
		// ter-komentar sehingga grid hanya selebar konten → area kanan popup tampak kosong/putih).
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
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
		column.setLabel("Nama");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mulai");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sampai");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Program");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ubah");
		column.setWidth("10%");

		onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(JamPerkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		criteria.addOrder(Order.asc("mulai")).addOrder(Order.asc("sampai"))

				.add(nama.getText().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						|| searchprogram.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(Restrictions.or(Restrictions.isNull("jurusan"),
						searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)));

		if (searchfakultas.getSelectedItem() != null) {
			criteria.add(Restrictions.or(Restrictions.isNull("fakultas"),
					searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
							|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)));
		}
		List<JamPerkuliahan> jamPerkuliahan = criteria.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(jamPerkuliahan);
		grid.setRowRenderer(new JamPerkuliahanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public void setJurusanSelected(Jurusan jurusan) {
		Common.clear(this);
		this.selectedJurusan = jurusan;
	}

	public void setFakultasSelected(Fakultas fakultas) {
		Common.clear(this);
		this.selectedFakultas = fakultas;
	}
}
