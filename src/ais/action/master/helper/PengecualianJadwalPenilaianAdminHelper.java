package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.PengecualianJadwalPenilaianDosen;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class PengecualianJadwalPenilaianAdminHelper implements DataLoader {

	private MyGrid grid;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private Textbox nama;

	public PengecualianJadwalPenilaianAdminHelper() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
	}

	class PengecualianJadwalPenilaianDosenRenderer extends ais.ui.util.MyRowRenderer {

		public PengecualianJadwalPenilaianDosenRenderer() {

		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PengecualianJadwalPenilaianDosen pengecualianJadwalPenilaianDosen = (PengecualianJadwalPenilaianDosen) arg1;

			Tbmuser tbmuser = pengecualianJadwalPenilaianDosen.getTbmuser();
			Dosen dosen = pengecualianJadwalPenilaianDosen.getDosen();
			if (tbmuser != null) {
				RevisiHelper.createNewRevisi(PengecualianJadwalPenilaianDosen.class, pengecualianJadwalPenilaianDosen,
						tbmuser.getUserId()).setParent(arg0);

				new Label(tbmuser.getNama()).setParent(arg0);
				new Label(tbmuser == null || tbmuser.ambilJurusan() == null ? "" : tbmuser.ambilJurusan().getNama())
						.setParent(arg0);
				new Label(tbmuser == null || tbmuser.ambilFakultas() == null ? "" : tbmuser.ambilFakultas().getNama())
						.setParent(arg0);
			} else if (dosen != null) {
				RevisiHelper.createNewRevisi(PengecualianJadwalPenilaianDosen.class, pengecualianJadwalPenilaianDosen,
						dosen.getNidn()).setParent(arg0);

				new Label(dosen.getNama()).setParent(arg0);
				new Label(dosen == null || dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama())
						.setParent(arg0);
				new Label(dosen == null || dosen.getFakultas() == null ? "" : dosen.getFakultas().getNama())
						.setParent(arg0);
			} else {
				new Label("").setParent(arg0);
				new Label("").setParent(arg0);
				new Label("").setParent(arg0);
				new Label("").setParent(arg0);
			}

			final Combobox tahunAkademik = Common.generateTahunAjaran(null);
			Common.selectComboItem(true, tahunAkademik, pengecualianJadwalPenilaianDosen.getTahunAkademik());
			tahunAkademik.setParent(arg0);
			tahunAkademik.setWidth("90%");
			tahunAkademik.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String tahun = (String) (tahunAkademik.getSelectedItem() == null
							|| tahunAkademik.getSelectedItem().getValue() == null ? ""
									: tahunAkademik.getSelectedItem().getValue());
					pengecualianJadwalPenilaianDosen.setTahunAkademik(tahun);
					Common.refreshUpdate(pengecualianJadwalPenilaianDosen);
				}
			});

			final Combobox semester = new Combobox();
			MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			semester.appendChild(comboitem);
			comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			semester.appendChild(comboitem);
			comboitem = new MyComboitemConfig(Perkuliahan.SP);
			comboitem.setValue(Perkuliahan.SP);
			semester.appendChild(comboitem);
			semester.setReadonly(true);

			semester.setWidth("90%");
			Common.selectComboItem(semester, pengecualianJadwalPenilaianDosen.getJenisSemester());
			semester.setParent(arg0);
			semester.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String mysemester = (String) (semester.getSelectedItem() == null ? ""
							: semester.getSelectedItem().getValue());

					pengecualianJadwalPenilaianDosen.setJenisSemester(mysemester);
					Common.refreshUpdate(pengecualianJadwalPenilaianDosen);
				}
			});

			final MyDatebox mulai = new MyDatebox(pengecualianJadwalPenilaianDosen.getTanggalMulai());
			mulai.setWidth("90%");
			mulai.setParent(arg0);
			mulai.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Date mymulai = mulai.getValue();

					Session session = HibernateUtil.currentSession();
					session.refresh(pengecualianJadwalPenilaianDosen);
					pengecualianJadwalPenilaianDosen.setTanggalMulai(mymulai);
					Common.refreshSaveOrUpdate(session, pengecualianJadwalPenilaianDosen);
				}
			});

			final MyDatebox sampai = new MyDatebox(pengecualianJadwalPenilaianDosen.getTanggalSampai());
			sampai.setWidth("90%");
			sampai.setParent(arg0);
			sampai.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Date mysampai = sampai.getValue();

					Session session = HibernateUtil.currentSession();
					session.refresh(pengecualianJadwalPenilaianDosen);
					pengecualianJadwalPenilaianDosen.setTanggalSampai(mysampai);
					Common.refreshSaveOrUpdate(session, pengecualianJadwalPenilaianDosen);
				}
			});

			final Combobox status = new Combobox();

			final EventListener semesterEventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String mystatus = (String) (status.getSelectedItem() == null
							|| status.getSelectedItem().getValue() == null ? "" : status.getSelectedItem().getValue());

					tahunAkademik.setDisabled(!mystatus.equals(PengecualianJadwalPenilaianDosen.PENGAJUAN));
					semester.setDisabled(!mystatus.equals(PengecualianJadwalPenilaianDosen.PENGAJUAN));
					mulai.setDisabled(!mystatus.equals(PengecualianJadwalPenilaianDosen.PENGAJUAN));
					sampai.setDisabled(!mystatus.equals(PengecualianJadwalPenilaianDosen.PENGAJUAN));

				}
			};

			comboitem = new MyComboitemConfig(PengecualianJadwalPenilaianDosen.PENGAJUAN);
			comboitem.setValue(PengecualianJadwalPenilaianDosen.PENGAJUAN);
			status.appendChild(comboitem);
			comboitem = new MyComboitemConfig(PengecualianJadwalPenilaianDosen.DISETUJU);
			comboitem.setValue(PengecualianJadwalPenilaianDosen.DISETUJU);
			status.appendChild(comboitem);
			comboitem = new MyComboitemConfig(PengecualianJadwalPenilaianDosen.DITOLAK);
			comboitem.setValue(PengecualianJadwalPenilaianDosen.DITOLAK);
			status.appendChild(comboitem);
			status.setReadonly(true);

			if (pengecualianJadwalPenilaianDosen.getDisposisiSop() != null) {
				status.setDisabled(true);
			}

			status.setWidth("90%");
			Common.selectComboItem(status, pengecualianJadwalPenilaianDosen.getStatus());
			status.setParent(arg0);
			status.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String mystatus = (String) (status.getSelectedItem() == null ? ""
							: status.getSelectedItem().getValue());
					if (mystatus.equals(PengecualianJadwalPenilaianDosen.DISETUJU)) {
						pengecualianJadwalPenilaianDosen.setDisetujuiOleh(Common.getCurrentUser());
						pengecualianJadwalPenilaianDosen.setTanggalPersetujuanManual(WaktuUtil.getDate());
					}
					pengecualianJadwalPenilaianDosen.setStatus(mystatus);
					Common.refreshUpdate(pengecualianJadwalPenilaianDosen);
					semesterEventListener.onEvent(null);
				}
			});

			semesterEventListener.onEvent(null);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			
			

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/printer.svg");
			button.setOrient("vertical");
			button.setStyle("font-size:9px;");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportHelper.onCetakPengecualianJadwalPenilaianDosen(pengecualianJadwalPenilaianDosen);
				}
			});
			button.setParent(hbox);

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			toolbarbutton.setOrient("vertical");
			toolbarbutton.setTooltiptext("Hapus Data");
			toolbarbutton.setParent(hbox);
			toolbarbutton.addEventListener("onClick", new EventListener() {
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
											Session session = HibernateUtil.currentSession();

											Common.refreshDelete(session, pengecualianJadwalPenilaianDosen);

											loadData(null);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException(
													"menghapus data pengecualian jadwal penilaian dosen",
													e,
													new String[] {
															"Periksa apakah data ini masih berelasi dengan data lain (misalnya data penilaian atau jadwal) sehingga tidak dapat dihapus.",
															"Hapus atau lepaskan terlebih dahulu data terkait yang masih berelasi, lalu ulangi proses penghapusan.",
															"Jika data tetap tidak dapat dihapus, konfirmasikan kebutuhan penghapusan ini kepada Administrator." });
										}

									}

								}
							});

				}

			});
		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = Common.getManualSession();
		List<Tbmuser> tbmuser = session.createCriteria(PengecualianJadwalPenilaianDosen.class)
				.addOrder(Order.desc("id")).createCriteria("tbmuser")
				.add(Restrictions.ilike("userId", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.setMaxResults(Common.MAX_RESULT_50).list();

		ListModel strset = new SimpleListModel(tbmuser);
		grid.setRowRenderer(new PengecualianJadwalPenilaianDosenRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() throws InterruptedException {

		final MyWindow window = new MyWindow("Daftar pengecualian jadwal penilaian admin", "none", true);
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		window.setWidth("90%");
		window.setHeight("90%");

		MyPanel panel = new MyPanel();
		panel.setParent(window);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);

		North north = new North();
		north.setParent(borderlayout);
		// FIX toolbar/tombol tidak tampil: pada ZK5 region North memakai tinggi bawaan
		// (+-100px); dengan flex=true isinya diregangkan ke tinggi tersebut sehingga
		// Toolbar yang diletakkan DI BAWAH grid filter ikut terpotong. Disamakan dengan
		// layar sejenis yang sudah benar (DownloadMahasiswa, DownloadKrs, DownloadNilai):
		// flex dimatikan + tinggi eksplisit. Autoscroll sebagai pengaman bila isi bertambah.
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

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

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Admin", "/img/new.gif");

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				AmbilDataTbmuserBanyak window = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>(),
						ConstantValues.roleDosen);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("700px");
				window.setHeight("90%");

				window.setEventListener(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						if (tbmusers != null) {

							String current = Common.getCurrentTahunAkademik();

							Session session = HibernateUtil.currentSession();

							for (Tbmuser tbmuser : tbmusers) {
								PengecualianJadwalPenilaianDosen pengecualianJadwalPenilaianDosen = new PengecualianJadwalPenilaianDosen();
								pengecualianJadwalPenilaianDosen.setTbmuser(tbmuser);
								pengecualianJadwalPenilaianDosen.setJenisSemester(
										Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
								pengecualianJadwalPenilaianDosen.setKeterangan("");
								pengecualianJadwalPenilaianDosen.setTahunAkademik(current);
								pengecualianJadwalPenilaianDosen.setTanggalMulai(ais.ui.util.WaktuUtil.getDate());
								pengecualianJadwalPenilaianDosen.setTanggalSampai(ais.ui.util.WaktuUtil.getDate());

								session.save(pengecualianJadwalPenilaianDosen);
							}

							loadData(null);

						}
					}
				});

				window.onModal();

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.setTooltiptext("Cari");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});
		button.setParent(toolbar);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("ID");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Akademik");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Semester");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mulai");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sampai");

		column = new MyColumnConfig("Status");
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("6%");

		loadData(null);

		South south = new South();
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		button.setParent(toolbar);

		window.onModal();
	}

}
