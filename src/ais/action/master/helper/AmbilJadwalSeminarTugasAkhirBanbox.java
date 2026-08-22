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

import ais.action.master.JadwalSeminarTugasAkhirAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.JadwalSeminarTugasAkhir;
import ais.database.model.Jurusan;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyPanel;

public class AmbilJadwalSeminarTugasAkhirBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;

	private EventListener eventListener;
	private Jurusan jurusan;
	private boolean edit = false;
	private boolean delete = false;

	public AmbilJadwalSeminarTugasAkhirBanbox() {
		this(null);
	}

	public AmbilJadwalSeminarTugasAkhirBanbox(Jurusan jurusan) {
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
		setAttribute("jadwalSeminarTugasAkhir", null);
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
	private Fakultas selectedFakultas;
	private Combobox searchtahunakademik;

	class JadwalSeminarTugasAkhirRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final JadwalSeminarTugasAkhir jadwalSeminarTugasAkhir = (JadwalSeminarTugasAkhir) arg1;
			MyRadioConfig checkbox = new MyRadioConfig();
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilJadwalSeminarTugasAkhirBanbox.this.setOpen(false);
					AmbilJadwalSeminarTugasAkhirBanbox.this.setAttribute("jadwalSeminarTugasAkhir",
							jadwalSeminarTugasAkhir);
					AmbilJadwalSeminarTugasAkhirBanbox.this.setAttribute("myValue", jadwalSeminarTugasAkhir);
					AmbilJadwalSeminarTugasAkhirBanbox.this.setValue(jadwalSeminarTugasAkhir.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(jadwalSeminarTugasAkhir.getNama()).setParent(arg0);
			new Label(jadwalSeminarTugasAkhir.getMulai() == null ? ""
					: Common.dateFormat1.get().format(jadwalSeminarTugasAkhir.getMulai())).setParent(arg0);
			new Label(jadwalSeminarTugasAkhir.getSampai() == null ? ""
					: Common.dateFormat1.get().format(jadwalSeminarTugasAkhir.getSampai())).setParent(arg0);
			new Label(jadwalSeminarTugasAkhir.getTahunAkademik()).setParent(arg0);
			new Label(
					jadwalSeminarTugasAkhir.getJurusan() == null ? "" : jadwalSeminarTugasAkhir.getJurusan().getNama())
							.setParent(arg0);
			new Label(jadwalSeminarTugasAkhir.getFakultas() == null ? ""
					: jadwalSeminarTugasAkhir.getFakultas().getNama()).setParent(arg0);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					JadwalSeminarTugasAkhirAction.onAddExternal(event, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							JadwalSeminarTugasAkhir jadwalSeminarTugasAkhir = (JadwalSeminarTugasAkhir) arg0.getData();
							AmbilJadwalSeminarTugasAkhirBanbox.this.setOpen(false);
							AmbilJadwalSeminarTugasAkhirBanbox.this.setAttribute("jadwalSeminarTugasAkhir",
									jadwalSeminarTugasAkhir);
							AmbilJadwalSeminarTugasAkhirBanbox.this.setAttribute("myValue", jadwalSeminarTugasAkhir);
							AmbilJadwalSeminarTugasAkhirBanbox.this.setValue(jadwalSeminarTugasAkhir.getNama());
							if (eventListener != null) {
								eventListener.onEvent(arg0);
							}

						}
					}, jadwalSeminarTugasAkhir);
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
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											JadwalSeminarTugasAkhir currentJadwalSeminarTugasAkhir = (JadwalSeminarTugasAkhir) getAttribute(
													"jadwalSeminarTugasAkhir");

											if (currentJadwalSeminarTugasAkhir != null && jadwalSeminarTugasAkhir
													.getId().equals(currentJadwalSeminarTugasAkhir.getId())) {

												AmbilJadwalSeminarTugasAkhirBanbox.this
														.setAttribute("jadwalSeminarTugasAkhir", null);
												AmbilJadwalSeminarTugasAkhirBanbox.this.setAttribute("myValue", null);
												AmbilJadwalSeminarTugasAkhirBanbox.this.setValue("");

												if (eventListener != null) {
													eventListener.onEvent(event);
												}

											}

											JadwalSeminarTugasAkhirAction.onDelete(jadwalSeminarTugasAkhir);

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
		panel.setTitle("Daftar Masa Perkuliahan");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		// Form filter (Nama/TA/Fakultas/Prodi) + tombol Cari → beri tinggi cukup agar Cari tak terpotong.
		north.setHeight("130px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		// Listener pencarian bersama: tombol Cari + Enter (onOK).
		final EventListener listenerCari = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		};

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");
		nama.addEventListener("onOK", listenerCari); // Enter di Nama → cari

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(searchtahunakademik = new Combobox());
		searchtahunakademik.setWidth("90%");
		Common.generateTahunAjaranDanSemua(searchtahunakademik);

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
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", jurusan == null ? null : jurusan.getFakultas()));
		Common.selectComboItem(searchjurusan, jurusan);
		searchjurusan.setDisabled(jurusan != null);

		// Tombol "Cari" SEBARIS agar SELALU terlihat (tak tergantung tinggi North).
		MyToolbarbuttonConfig btnCariSebaris = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		btnCariSebaris.addEventListener("onClick", listenerCari);
		row.appendChild(btnCariSebaris);

		if (selectedFakultas != null) {
			Common.selectComboItem(this.searchfakultas, selectedFakultas);
		}

		if (selectedJurusan != null) {
			Common.selectComboItem(this.searchjurusan, selectedJurusan);
		}

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		toolbar.appendChild(Common.createCleanButton(this, this));

		button = new MyToolbarbuttonConfig("Tambah Jadwal", "/img/new.gif");
		button.setTooltiptext("Tambah Jadwal");
		button.setVisible(edit);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				JadwalSeminarTugasAkhir jadwalSeminarTugasAkhir = new JadwalSeminarTugasAkhir();
				jadwalSeminarTugasAkhir.setJurusan(jurusan);
				jadwalSeminarTugasAkhir.setFakultas(jurusan == null ? null : jurusan.getFakultas());

				JadwalSeminarTugasAkhirAction.onAddExternal(event, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						JadwalSeminarTugasAkhir jadwalSeminarTugasAkhir = (JadwalSeminarTugasAkhir) arg0.getData();
						AmbilJadwalSeminarTugasAkhirBanbox.this.setOpen(false);
						AmbilJadwalSeminarTugasAkhirBanbox.this.setAttribute("jadwalSeminarTugasAkhir",
								jadwalSeminarTugasAkhir);
						AmbilJadwalSeminarTugasAkhirBanbox.this.setAttribute("myValue", jadwalSeminarTugasAkhir);
						AmbilJadwalSeminarTugasAkhirBanbox.this.setValue(jadwalSeminarTugasAkhir.getNama());
						if (eventListener != null) {
							eventListener.onEvent(arg0);
						}
						onSearchDefault(arg0);
					}
				}, jadwalSeminarTugasAkhir);
			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

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
		column.setLabel("Mulai");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sampai");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Akademik");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ubah");
		column.setWidth("10%");

		onSearchDefault(null);

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(JadwalSeminarTugasAkhir.class)
				.add(searchtahunakademik.getSelectedItem() == null
						|| searchtahunakademik.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchtahunakademik.getSelectedItem().getValue()));

		criteria.addOrder(Order.desc("mulai")).addOrder(Order.desc("sampai"))
				.add(Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))
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
		List<JadwalSeminarTugasAkhir> jadwalSeminarTugasAkhir = criteria.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(jadwalSeminarTugasAkhir);
		grid.setRowRenderer(new JadwalSeminarTugasAkhirRenderer());
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
