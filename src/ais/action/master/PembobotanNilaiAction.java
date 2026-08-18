package ais.action.master;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.RevisiPembobotanNilaiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.PembobotanNilaiDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.PembombotanNilai;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PembobotanNilaiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1247117510422121057L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchdibuat;
	private MyCheckboxConfig hanyaDibuatOlehAdmin;
	private Checkbox searchaktif;

	private MyDoublebox absen;
	private MyDoublebox form;

	private MyDoublebox tugas1;
	private MyDoublebox tugas2;
	private MyDoublebox tugas3;
	private MyDoublebox tugas4;
	private MyDoublebox tugas5;

	private MyDoublebox quiz1;
	private MyDoublebox quiz2;
	private MyDoublebox quiz3;
	private MyDoublebox quiz4;
	private MyDoublebox quiz5;

	private MyDoublebox uts;
	private MyDoublebox uas;

	private Textbox absenLabel;
	private Textbox formLabel;
	private Textbox utsLabel;
	private Textbox uasLabel;

	private Textbox tugas1Label;
	private Textbox tugas2Label;
	private Textbox tugas3Label;
	private Textbox tugas4Label;
	private Textbox tugas5Label;

	private Textbox quiz1Label;
	private Textbox quiz2Label;
	private Textbox quiz3Label;
	private Textbox quiz4Label;
	private Textbox quiz5Label;

	private MyDoublebox dosen1;
	private MyDoublebox dosen2;
	private MyDoublebox dosen3;
	private MyDoublebox dosen4;
	private MyDoublebox dosen5;

	private Combobox jenisPembobotan = new Combobox();

	private PembombotanNilai pembombotanNilai;
	private MyToolbarbuttonConfig add;

	private boolean edit;
	private boolean delete;

	private MyCheckboxConfig defaultPembobotan;
	private EventListener eventListener;

	private Tabpanel statusPertemuan;
	private MyCheckboxConfig wajibDitahunAkademikDanSemesterTertentu;
	private Combobox tahunAkadmeik;
	private Combobox semester;
	private JSONObject jsonData;
	private MyTextbox keterangan;

	public void onStatusPertemuan(Event event) {

		if (statusPertemuan.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(statusPertemuan);
			MyInclude iframe = new MyInclude("/pages/master/status_pertemuan.zul");
			iframe.setParent(window);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		if (button != null) { button.setDisabled(!edit); }
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiPembobotanNilaiHelper revisiHelper = new RevisiPembobotanNilaiHelper(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});
					}
				});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		if (button != null) { button.setParent(add.getParent()); }

	}

	class PembobotanNilaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PembombotanNilai pembombotanNilai = (PembombotanNilai) arg1;
			RevisiHelper.createNewRevisi(PembombotanNilai.class, pembombotanNilai, pembombotanNilai.getNama())
					.setParent(arg0);

			new Label(pembombotanNilai.getDimilikiOleh() == null ? "" : pembombotanNilai.getDimilikiOleh().getNama())
					.setParent(arg0);

			new Label(pembombotanNilai.getDefaultPembobotan() ? "Ya" : "Tidak").setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(pembombotanNilai.getWajibDitahunAkademikDanSemesterTertentu() ? "Ya" : "Tidak").setParent(vbox);
			if (pembombotanNilai.getTahunAkadmeik() != null) {
				new Label(pembombotanNilai.getTahunAkadmeik()).setParent(vbox);
			}
			if (pembombotanNilai.getSemester() != null) {
				new Label(pembombotanNilai.getSemester()).setParent(vbox);
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(pembombotanNilai.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pembombotanNilai.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(pembombotanNilai);
				}
			});

			new Label(pembombotanNilai.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);

			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pembombotanNilai);
					addWindow.setVisible(true);
					addWindow.onModal();
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

											// PembombotanNilai bisa masih direferensikan perkuliahan (kolom
											// pembombotan_nilai / pembombotan_nilai_backup). Menghapus paksa memicu
											// ConstraintViolation saat flush -> merusak sesi ZK sehingga onSearchDefault
											// berikutnya gagal ("createCriteria not valid without active transaction").
											// Cek dulu; bila masih dipakai, tampilkan pesan & JANGAN hapus (sesi tetap
											// sehat, data perkuliahan aman).
											Session sesiCek = HibernateUtil.currentNativeSession();
											Number dipakai = (Number) sesiCek
													.createSQLQuery("select count(*) from perkuliahan where "
															+ "pembombotan_nilai = :id or pembombotan_nilai_backup = :id")
													.setLong("id", pembombotanNilai.getId()).uniqueResult();
											if (dipakai != null && dipakai.longValue() > 0) {
												MyMessageboxConfig.show("Data ini tidak dapat dihapus karena masih "
														+ "dipakai pada " + dipakai + " perkuliahan.");
												return;
											}

											Common.refreshDelete(pembombotanNilai);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public static void onAddExternal(Event event, EventListener eventListener, PembombotanNilai pembombotanNilai)
			throws Exception {
		PembobotanNilaiAction pembombotanNilaiAction = new PembobotanNilaiAction();
		pembombotanNilaiAction.eventListener = eventListener;
		pembombotanNilaiAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(pembombotanNilaiAction.addWindow);
		pembombotanNilaiAction.addWindow.setHeight("440px");
		pembombotanNilaiAction.addWindow.setWidth("850px");

		pembombotanNilaiAction.init(pembombotanNilai);

		pembombotanNilaiAction.addWindow.setVisible(true);
		pembombotanNilaiAction.addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new PembombotanNilai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(PembombotanNilai pembombotanNilai) throws Exception {
		this.pembombotanNilai = pembombotanNilai;
		addWindow.setHeight("95%");
		addWindow.setWidth("550px");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Urut");
		// Diperlebar dari 10% → 15% agar angka urut pada input tidak terpotong/tak terlihat.
		column.setWidth("15%");
		column.setParent(columns);

		column = new MyColumnConfig("Nama Format Nilai");
		column.setWidth("60%");
		column.setParent(columns);

		column = new MyColumnConfig("Persentase");
		// Diperlebar dari 10% → 20% agar angka persentase (mis. "35") tidak terpotong.
		column.setWidth("20%");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setWidth("5%");
		column.setParent(columns);

		jsonData = new JSONObject(pembombotanNilai.getNomorUrutFormat());

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Ket."));
		row.appendChild(keterangan = new MyTextbox((pembombotanNilai.getKeterangan())));
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		keterangan.setWidth("95%");
		keterangan.setRows(2);

		row = new MyFormRow();
		row.setValign("top");
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembobotan"));
		MyComboitemConfig comboitem = new MyComboitemConfig(PembombotanNilai.JENIS_PERKULIAHAN_BELAJAR_MENGAJAR);
		comboitem.setValue(PembombotanNilai.JENIS_PERKULIAHAN_BELAJAR_MENGAJAR);
		jenisPembobotan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PembombotanNilai.JENIS_PERKULIAHAN_TUGAS_AKHIR);
		comboitem.setValue(PembombotanNilai.JENIS_PERKULIAHAN_TUGAS_AKHIR);
		jenisPembobotan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PembombotanNilai.JENIS_PERKULIAHAN_KKN);
		comboitem.setValue(PembombotanNilai.JENIS_PERKULIAHAN_KKN);
		jenisPembobotan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PembombotanNilai.JENIS_PERKULIAHAN_PKL);
		comboitem.setValue(PembombotanNilai.JENIS_PERKULIAHAN_PKL);
		jenisPembobotan.appendChild(comboitem);
		row.appendChild(jenisPembobotan);
		Common.selectComboItem(jenisPembobotan,
				pembombotanNilai.getJenisPembobotan() == null ? PembombotanNilai.JENIS_PERKULIAHAN_BELAJAR_MENGAJAR
						: pembombotanNilai.getJenisPembobotan());
		jenisPembobotan.setWidth("95%");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (jenisPembobotan.getSelectedItem() == null)
					return;
				List<String> values = PembombotanNilai.keterhubungan.get(jenisPembobotan.getSelectedItem().getValue());
				uts.setDisabled(!values.contains("uts"));
				uas.setDisabled(!values.contains("uas"));
				form.setDisabled(!values.contains("form"));
				dosen1.setDisabled(!values.contains("dosen1"));
				dosen2.setDisabled(!values.contains("dosen2"));
				dosen3.setDisabled(!values.contains("dosen3"));
				dosen4.setDisabled(!values.contains("dosen4"));
				dosen5.setDisabled(!values.contains("dosen5"));
			}
		};
		jenisPembobotan.addEventListener(Events.ON_CHANGE, eventListener);

		row = new MyFormRow();
		row.setVisible(ConstantValues.ABSEN.getAktif());
		row.setParent(rows);
		Decimalbox d = new Decimalbox(jsonData.isNull(ConstantValues.ABSEN.getId().toString()) ? null
				: new BigDecimal(ais.common.CommonJSONUtil.ambilLong(jsonData,ConstantValues.ABSEN.getId().toString())));
		row.appendChild(d);
		d.setWidth("95%");
		d.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Decimalbox d = (Decimalbox) arg0.getTarget();
				jsonData.put(ConstantValues.ABSEN.getId().toString(),
						d.getValue() == null ? null : d.getValue().longValue());
			}
		});

		row.appendChild(absenLabel = new Textbox((pembombotanNilai.getAbsenLabel())));
		absenLabel.setWidth("95%");
		row.appendChild(absen = new MyDoublebox((pembombotanNilai.getAbsen())));
		absen.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(ConstantValues.FORM.getAktif());
		row.setParent(rows);

		d = new Decimalbox(jsonData.isNull(ConstantValues.FORM.getId().toString()) ? null
				: new BigDecimal(ais.common.CommonJSONUtil.ambilLong(jsonData,ConstantValues.FORM.getId().toString())));
		row.appendChild(d);
		d.setWidth("95%");
		d.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Decimalbox d = (Decimalbox) arg0.getTarget();
				jsonData.put(ConstantValues.FORM.getId().toString(),
						d.getValue() == null ? null : d.getValue().longValue());
			}
		});

		row.appendChild(formLabel = new Textbox((pembombotanNilai.getFormLabel())));
		formLabel.setWidth("95%");
		row.appendChild(form = new MyDoublebox((pembombotanNilai.getForm() == null ? 0 : pembombotanNilai.getForm())));
		form.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(ConstantValues.TUGAS_1.getAktif());
		row.setParent(rows);
//		row.appendChild(new Label(ConstantValues.TUGAS_1.getNomorUrut() + ""));

		d = new Decimalbox(jsonData.isNull(ConstantValues.TUGAS_1.getId().toString()) ? null
				: new BigDecimal(ais.common.CommonJSONUtil.ambilLong(jsonData,ConstantValues.TUGAS_1.getId().toString())));
		row.appendChild(d);
		d.setWidth("95%");
		d.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Decimalbox d = (Decimalbox) arg0.getTarget();
				jsonData.put(ConstantValues.TUGAS_1.getId().toString(),
						d.getValue() == null ? null : d.getValue().longValue());
			}
		});

		row.appendChild(tugas1Label = new Textbox((pembombotanNilai.getTugas1Label())));
		tugas1Label.setWidth("95%");
		row.appendChild(tugas1 = new MyDoublebox((pembombotanNilai.getTugas1())));
		tugas1.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(ConstantValues.TUGAS_2.getAktif());
		row.setParent(rows);
//		row.appendChild(new Label(ConstantValues.TUGAS_2.getNomorUrut() + ""));

		d = new Decimalbox(jsonData.isNull(ConstantValues.TUGAS_2.getId().toString()) ? null
				: new BigDecimal(ais.common.CommonJSONUtil.ambilLong(jsonData,ConstantValues.TUGAS_2.getId().toString())));
		row.appendChild(d);
		d.setWidth("95%");
		d.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Decimalbox d = (Decimalbox) arg0.getTarget();
				jsonData.put(ConstantValues.TUGAS_2.getId().toString(),
						d.getValue() == null ? null : d.getValue().longValue());
			}
		});

		row.appendChild(tugas2Label = new Textbox((pembombotanNilai.getTugas2Label())));
		tugas2Label.setWidth("95%");
		row.appendChild(tugas2 = new MyDoublebox((pembombotanNilai.getTugas2())));
		tugas2.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(ConstantValues.TUGAS_3.getAktif());
		row.setParent(rows);
//		row.appendChild(new Label(ConstantValues.TUGAS_3.getNomorUrut() + ""));

		d = new Decimalbox(jsonData.isNull(ConstantValues.TUGAS_3.getId().toString()) ? null
				: new BigDecimal(ais.common.CommonJSONUtil.ambilLong(jsonData,ConstantValues.TUGAS_3.getId().toString())));
		row.appendChild(d);
		d.setWidth("95%");
		d.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Decimalbox d = (Decimalbox) arg0.getTarget();
				jsonData.put(ConstantValues.TUGAS_3.getId().toString(),
						d.getValue() == null ? null : d.getValue().longValue());
			}
		});

		row.appendChild(tugas3Label = new Textbox((pembombotanNilai.getTugas3Label())));
		tugas3Label.setWidth("95%");
		row.appendChild(tugas3 = new MyDoublebox((pembombotanNilai.getTugas3())));
		tugas3.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(ConstantValues.TUGAS_4.getAktif());
		row.setParent(rows);
//		row.appendChild(new Label(ConstantValues.TUGAS_4.getNomorUrut() + ""));

		d = new Decimalbox(jsonData.isNull(ConstantValues.TUGAS_4.getId().toString()) ? null
				: new BigDecimal(ais.common.CommonJSONUtil.ambilLong(jsonData,ConstantValues.TUGAS_4.getId().toString())));
		row.appendChild(d);
		d.setWidth("95%");
		d.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Decimalbox d = (Decimalbox) arg0.getTarget();
				jsonData.put(ConstantValues.TUGAS_4.getId().toString(),
						d.getValue() == null ? null : d.getValue().longValue());
			}
		});

		row.appendChild(tugas4Label = new Textbox((pembombotanNilai.getTugas4Label())));
		tugas4Label.setWidth("95%");
		row.appendChild(tugas4 = new MyDoublebox((pembombotanNilai.getTugas4())));
		tugas4.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(ConstantValues.TUGAS_5.getAktif());
		row.setParent(rows);
//		row.appendChild(new Label(ConstantValues.TUGAS_5.getNomorUrut() + ""));

		d = new Decimalbox(jsonData.isNull(ConstantValues.TUGAS_5.getId().toString()) ? null
				: new BigDecimal(ais.common.CommonJSONUtil.ambilLong(jsonData,ConstantValues.TUGAS_5.getId().toString())));
		row.appendChild(d);
		d.setWidth("95%");
		d.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Decimalbox d = (Decimalbox) arg0.getTarget();
				jsonData.put(ConstantValues.TUGAS_5.getId().toString(),
						d.getValue() == null ? null : d.getValue().longValue());
			}
		});

		row.appendChild(tugas5Label = new Textbox((pembombotanNilai.getTugas5Label())));
		tugas5Label.setWidth("95%");
		row.appendChild(tugas5 = new MyDoublebox((pembombotanNilai.getTugas5())));
		tugas5.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(ConstantValues.QUIZ_1.getAktif());
		row.setParent(rows);
//		row.appendChild(new Label(ConstantValues.QUIZ_1.getNomorUrut() + ""));

		d = new Decimalbox(jsonData.isNull(ConstantValues.QUIZ_1.getId().toString()) ? null
				: new BigDecimal(ais.common.CommonJSONUtil.ambilLong(jsonData,ConstantValues.QUIZ_1.getId().toString())));
		row.appendChild(d);
		d.setWidth("95%");
		d.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Decimalbox d = (Decimalbox) arg0.getTarget();
				jsonData.put(ConstantValues.QUIZ_1.getId().toString(),
						d.getValue() == null ? null : d.getValue().longValue());
			}
		});

		row.appendChild(quiz1Label = new Textbox((pembombotanNilai.getQuiz1Label())));
		quiz1Label.setWidth("95%");
		row.appendChild(quiz1 = new MyDoublebox((pembombotanNilai.getQuiz1())));
		quiz1.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(ConstantValues.QUIZ_2.getAktif());
		row.setParent(rows);
//		row.appendChild(new Label(ConstantValues.QUIZ_2.getNomorUrut() + ""));

		d = new Decimalbox(jsonData.isNull(ConstantValues.QUIZ_2.getId().toString()) ? null
				: new BigDecimal(ais.common.CommonJSONUtil.ambilLong(jsonData,ConstantValues.QUIZ_2.getId().toString())));
		row.appendChild(d);
		d.setWidth("95%");
		d.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Decimalbox d = (Decimalbox) arg0.getTarget();
				jsonData.put(ConstantValues.QUIZ_2.getId().toString(),
						d.getValue() == null ? null : d.getValue().longValue());
			}
		});

		row.appendChild(quiz2Label = new Textbox((pembombotanNilai.getQuiz2Label())));
		quiz2Label.setWidth("95%");
		row.appendChild(quiz2 = new MyDoublebox((pembombotanNilai.getQuiz2())));
		quiz2.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(ConstantValues.QUIZ_3.getAktif());
		row.setParent(rows);
//		row.appendChild(new Label(ConstantValues.QUIZ_3.getNomorUrut() + ""));

		d = new Decimalbox(jsonData.isNull(ConstantValues.QUIZ_3.getId().toString()) ? null
				: new BigDecimal(ais.common.CommonJSONUtil.ambilLong(jsonData,ConstantValues.QUIZ_3.getId().toString())));
		row.appendChild(d);
		d.setWidth("95%");
		d.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Decimalbox d = (Decimalbox) arg0.getTarget();
				jsonData.put(ConstantValues.QUIZ_3.getId().toString(),
						d.getValue() == null ? null : d.getValue().longValue());
			}
		});

		row.appendChild(quiz3Label = new Textbox((pembombotanNilai.getQuiz3Label())));
		quiz3Label.setWidth("95%");
		row.appendChild(quiz3 = new MyDoublebox((pembombotanNilai.getQuiz3())));
		quiz3.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(ConstantValues.QUIZ_4.getAktif());
		row.setParent(rows);
//		row.appendChild(new Label(ConstantValues.QUIZ_4.getNomorUrut() + ""));

		d = new Decimalbox(jsonData.isNull(ConstantValues.QUIZ_4.getId().toString()) ? null
				: new BigDecimal(ais.common.CommonJSONUtil.ambilLong(jsonData,ConstantValues.QUIZ_4.getId().toString())));
		row.appendChild(d);
		d.setWidth("95%");
		d.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Decimalbox d = (Decimalbox) arg0.getTarget();
				jsonData.put(ConstantValues.QUIZ_4.getId().toString(),
						d.getValue() == null ? null : d.getValue().longValue());
			}
		});

		row.appendChild(quiz4Label = new Textbox((pembombotanNilai.getQuiz4Label())));
		quiz4Label.setWidth("95%");
		row.appendChild(quiz4 = new MyDoublebox((pembombotanNilai.getQuiz4())));
		quiz4.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(ConstantValues.QUIZ_5.getAktif());
		row.setParent(rows);
//		row.appendChild(new Label(ConstantValues.QUIZ_5.getNomorUrut() + ""));

		d = new Decimalbox(jsonData.isNull(ConstantValues.QUIZ_5.getId().toString()) ? null
				: new BigDecimal(ais.common.CommonJSONUtil.ambilLong(jsonData,ConstantValues.QUIZ_5.getId().toString())));
		row.appendChild(d);
		d.setWidth("95%");
		d.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Decimalbox d = (Decimalbox) arg0.getTarget();
				jsonData.put(ConstantValues.QUIZ_5.getId().toString(),
						d.getValue() == null ? null : d.getValue().longValue());
			}
		});

		row.appendChild(quiz5Label = new Textbox((pembombotanNilai.getQuiz5Label())));
		quiz5Label.setWidth("95%");
		row.appendChild(quiz5 = new MyDoublebox((pembombotanNilai.getQuiz5())));
		quiz5.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(ConstantValues.UTS.getAktif());
		row.setParent(rows);
//		row.appendChild(new Label(ConstantValues.UTS.getNomorUrut() + ""));

		d = new Decimalbox(jsonData.isNull(ConstantValues.UTS.getId().toString()) ? null
				: new BigDecimal(ais.common.CommonJSONUtil.ambilLong(jsonData,ConstantValues.UTS.getId().toString())));
		row.appendChild(d);
		d.setWidth("95%");
		d.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Decimalbox d = (Decimalbox) arg0.getTarget();
				jsonData.put(ConstantValues.UTS.getId().toString(),
						d.getValue() == null ? null : d.getValue().longValue());
			}
		});

		row.appendChild(utsLabel = new Textbox((pembombotanNilai.getUtsLabel())));
		utsLabel.setWidth("95%");
		row.appendChild(uts = new MyDoublebox((pembombotanNilai.getUts() == null ? 0 : pembombotanNilai.getUts())));
		uts.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(ConstantValues.UAS.getAktif());
		row.setParent(rows);
//		row.appendChild(new Label(ConstantValues.UAS.getNomorUrut() + ""));

		d = new Decimalbox(jsonData.isNull(ConstantValues.UAS.getId().toString()) ? null
				: new BigDecimal(ais.common.CommonJSONUtil.ambilLong(jsonData,ConstantValues.UAS.getId().toString())));
		row.appendChild(d);
		d.setWidth("95%");
		d.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Decimalbox d = (Decimalbox) arg0.getTarget();
				jsonData.put(ConstantValues.UAS.getId().toString(),
						d.getValue() == null ? null : d.getValue().longValue());
			}
		});

		row.appendChild(uasLabel = new Textbox((pembombotanNilai.getUasLabel())));
		uasLabel.setWidth("95%");
		row.appendChild(uas = new MyDoublebox((pembombotanNilai.getUas() == null ? 0 : pembombotanNilai.getUas())));
		uas.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_dosen_1")));
		row.appendChild(
				dosen1 = new MyDoublebox((pembombotanNilai.getDosen1() == null ? 0 : pembombotanNilai.getDosen1())));
		dosen1.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_dosen_2")));
		row.appendChild(
				dosen2 = new MyDoublebox((pembombotanNilai.getDosen2() == null ? 0 : pembombotanNilai.getDosen2())));
		dosen2.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen 3"));
		row.appendChild(
				dosen3 = new MyDoublebox((pembombotanNilai.getDosen3() == null ? 0 : pembombotanNilai.getDosen4())));
		dosen3.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen 4"));
		row.appendChild(
				dosen4 = new MyDoublebox((pembombotanNilai.getDosen4() == null ? 0 : pembombotanNilai.getDosen4())));
		dosen4.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen 5"));
		row.appendChild(
				dosen5 = new MyDoublebox((pembombotanNilai.getDosen5() == null ? 0 : pembombotanNilai.getDosen5())));
		dosen5.setWidth("95%");
		row.appendChild(new ais.ui.util.MyLabelConfig("%"));

		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setVisible(Common.getCurrentUser().getDosen() == null);
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(defaultPembobotan = new MyCheckboxConfig("Merupakan format nilai default"));
		defaultPembobotan.setChecked(pembombotanNilai.getDefaultPembobotan());

		row = new MyFormRow();
		row.setVisible(Common.getCurrentUser().getDosen() == null);
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(wajibDitahunAkademikDanSemesterTertentu = new MyCheckboxConfig(
				"Diwajibkan di tahun akademik dan semester tertentu"));
		wajibDitahunAkademikDanSemesterTertentu
				.setChecked(pembombotanNilai.getWajibDitahunAkademikDanSemesterTertentu());

		row = new MyFormRow();
		row.setVisible(Common.getCurrentUser().getDosen() == null);
		row.setParent(rows);
		row.appendChild(new Label());
		final MyLabelConfig lbl;
		row.appendChild(lbl = new ais.ui.util.MyLabelConfig("Pilih tahun akademik di-wajibkan"));

		row = new MyFormRow();
		row.setVisible(Common.getCurrentUser().getDosen() == null);
		row.setParent(rows);
		row.appendChild(tahunAkadmeik = new Combobox());
		Common.generateTahunAjaran(tahunAkadmeik);
		Common.selectComboItem(tahunAkadmeik,
				pembombotanNilai.getTahunAkadmeik() == null ? Common.getCurrentTahunAkademik()
						: pembombotanNilai.getTahunAkadmeik());
		tahunAkadmeik.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(Common.getCurrentUser().getDosen() == null);
		row.setParent(rows);
		row.appendChild(new Label());
		final MyLabelConfig lbl1;
		row.appendChild(lbl1 = new ais.ui.util.MyLabelConfig("Pilih semester di-wajibkan"));

		row = new MyFormRow();
		row.appendChild(new Label());
		row.setVisible(Common.getCurrentUser().getDosen() == null);
		row.setParent(rows);
		row.appendChild(semester = new Combobox());
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semester.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semester.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Perkuliahan.SP);
		comboitem.setValue(Perkuliahan.SP);
		semester.appendChild(comboitem);

		Common.selectComboItem(semester,
				pembombotanNilai.getSemester() == null
						? (Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP)
						: pembombotanNilai.getSemester());

		EventListener wajibDitahunAkademikDanSemesterTertentuEven = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				lbl.getParent().setVisible(wajibDitahunAkademikDanSemesterTertentu.isChecked());
				lbl1.getParent().setVisible(wajibDitahunAkademikDanSemesterTertentu.isChecked());
				tahunAkadmeik.getParent().setVisible(wajibDitahunAkademikDanSemesterTertentu.isChecked());
				semester.getParent().setVisible(wajibDitahunAkademikDanSemesterTertentu.isChecked());
			}
		};

		wajibDitahunAkademikDanSemesterTertentu.addEventListener("onClick",
				wajibDitahunAkademikDanSemesterTertentuEven);
		wajibDitahunAkademikDanSemesterTertentuEven.onEvent(null);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "4");
		row.setAlign("center");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyHtml("<hr>"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "4");
		row.setAlign("center");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("* Berikan persentase 0% jika format nilai tidak digunakan."));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "4");
		row.setAlign("center");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("* Total persentase yang dimasukkan harus sama dengan 100%."));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "4");
		row.setAlign("center");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyHtml("<hr>"));

		// row = new MyFormRow();
		//		// row.setParent(rows);
		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);

					if (PembobotanNilaiAction.this.eventListener != null) {
						PembobotanNilaiAction.this.eventListener
								.onEvent(new Event("", addWindow, PembobotanNilaiAction.this.pembombotanNilai));
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

		if (pembombotanNilai.getId() != null) {
			absen.setDisabled(true);
			absen.setDisabled(true);
			form.setDisabled(true);

			tugas1.setDisabled(true);
			tugas2.setDisabled(true);
			tugas3.setDisabled(true);
			tugas4.setDisabled(true);
			tugas5.setDisabled(true);

			quiz1.setDisabled(true);
			quiz2.setDisabled(true);
			quiz3.setDisabled(true);
			quiz4.setDisabled(true);
			quiz5.setDisabled(true);

			uts.setDisabled(true);
			uas.setDisabled(true);
		}

		if (!Common.bolehKonfigurasi("nilai_UTS_dan_UAS_default_bisa_diubah")) {
			uas.setDisabled(true);
			uts.setDisabled(true);
		}

	}

	/** Nilai aman dari Doublebox: kosong/null dianggap 0.0 (cegah NPE saat penjumlahan bobot). */
	private static double nz(org.zkoss.zul.Doublebox b) {
		Double d = (b == null) ? null : b.getValue();
		return d == null ? 0.0 : d.doubleValue();
	}

	public boolean onSave(Event event) throws Exception {
		if (jenisPembobotan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Pembobotan",
					"Kolom Jenis Pembobotan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Pembobotan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (absen.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Absen",
					"Kolom Absen belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Absen.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (form.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tugas",
					"Kolom Tugas belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tugas.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (uts.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data UTS",
					"Kolom UTS belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu UTS.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (uas.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data UAS",
					"Kolom UAS belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu UAS.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (dosen1.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Dosen 1",
					"Kolom Dosen 1 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Dosen 1.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (dosen2.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Dosen 1",
					"Kolom Dosen 1 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Dosen 1.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (dosen3.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Dosen 1",
					"Kolom Dosen 1 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Dosen 1.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (dosen4.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Dosen 1",
					"Kolom Dosen 1 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Dosen 1.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (dosen5.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Dosen 1",
					"Kolom Dosen 1 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Dosen 1.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		// getValue() pada Doublebox kosong mengembalikan null → auto-unboxing pada penjumlahan
		// melempar NullPointerException. nz() menganggap nilai kosong sebagai 0.0.
		Double temp_total;
		temp_total = nz(absen) + nz(form) + nz(uts) + nz(uas) + nz(dosen1)
				+ nz(dosen2) + nz(dosen3) + nz(dosen4) + nz(dosen5) + nz(tugas1)
				+ nz(tugas2) + nz(tugas3) + nz(tugas4) + nz(tugas5) + nz(quiz1)
				+ nz(quiz2) + nz(quiz3) + nz(quiz4) + nz(quiz5);

		System.out.println("total : " + temp_total);
		if (temp_total < 0.0) {
			MyMessageboxConfig.show("Jumlah Pembobotan tidak boleh negatif", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (temp_total > 100.0) {
			MyMessageboxConfig.show("Jumlah Pembobotan tidak boleh lebih besar dari 100", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (!temp_total.equals(100.0)) {
			MyMessageboxConfig.show("Jumlah Pembobotan harus berjumlah 100", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

//		boolean i = checkBobot();
//		if (i) {
//			MyMessageboxConfig.show("Pembobotan sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}

		if (defaultPembobotan.isChecked()) {
			HibernateUtil.currentSession().createSQLQuery("update pembombotan_nilai set defaultpembobotan = false")
					.executeUpdate();
		}

		PembobotanNilaiDao pembobotanNilaiDao = DaoFactory.getInstance().getPembobotanNilaiDao();
		if (pembombotanNilai.getId() != null) {
			pembombotanNilai = pembobotanNilaiDao.load(pembombotanNilai.getId());
		}
		pembombotanNilai.setJenisPembobotan((String) jenisPembobotan.getSelectedItem().getValue());
		pembombotanNilai.setAbsen(absen.getValue());
		pembombotanNilai.setForm(form.getValue());
		pembombotanNilai.setUts(uts.getValue());
		pembombotanNilai.setUas(uas.getValue());

		pembombotanNilai.setAbsenLabel(absenLabel.getValue());
		pembombotanNilai.setFormLabel(formLabel.getValue());
		pembombotanNilai.setUasLabel(uasLabel.getValue());
		pembombotanNilai.setUtsLabel(utsLabel.getValue());

		pembombotanNilai.setDosen1(dosen1.getValue());
		pembombotanNilai.setDosen2(dosen2.getValue());
		pembombotanNilai.setDosen3(dosen3.getValue());
		pembombotanNilai.setDosen4(dosen4.getValue());
		pembombotanNilai.setDosen5(dosen5.getValue());

		pembombotanNilai.setTugas1(tugas1.getValue());
		pembombotanNilai.setTugas2(tugas2.getValue());
		pembombotanNilai.setTugas3(tugas3.getValue());
		pembombotanNilai.setTugas4(tugas4.getValue());
		pembombotanNilai.setTugas5(tugas5.getValue());

		pembombotanNilai.setQuiz1(quiz1.getValue());
		pembombotanNilai.setQuiz2(quiz2.getValue());
		pembombotanNilai.setQuiz3(quiz3.getValue());
		pembombotanNilai.setQuiz4(quiz4.getValue());
		pembombotanNilai.setQuiz5(quiz5.getValue());

		pembombotanNilai.setTugas1Label(tugas1Label.getValue());
		pembombotanNilai.setTugas2Label(tugas2Label.getValue());
		pembombotanNilai.setTugas3Label(tugas3Label.getValue());
		pembombotanNilai.setTugas4Label(tugas4Label.getValue());
		pembombotanNilai.setTugas5Label(tugas5Label.getValue());

		pembombotanNilai.setQuiz1Label(quiz1Label.getValue());
		pembombotanNilai.setQuiz2Label(quiz2Label.getValue());
		pembombotanNilai.setQuiz3Label(quiz3Label.getValue());
		pembombotanNilai.setQuiz4Label(quiz4Label.getValue());
		pembombotanNilai.setQuiz5Label(quiz5Label.getValue());
		pembombotanNilai.setDefaultPembobotan(defaultPembobotan.isChecked());
		pembombotanNilai.setDimilikiOleh(Common.getCurrentUser().getDosen());
		pembombotanNilai.setKeterangan(keterangan.getValue());
		pembombotanNilai
				.setWajibDitahunAkademikDanSemesterTertentu(wajibDitahunAkademikDanSemesterTertentu.isChecked());
		pembombotanNilai.setTahunAkadmeik(
				(String) (tahunAkadmeik.getSelectedItem() == null ? null : tahunAkadmeik.getSelectedItem().getValue()));
		pembombotanNilai.setSemester(
				(String) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue()));

		pembombotanNilai.setNomorUrutFormat(jsonData.toString());

		if (pembombotanNilai.getId() != null) {
			pembobotanNilaiDao.update(pembombotanNilai);
		} else {
			pembobotanNilaiDao.save(pembombotanNilai);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		// Pakai native session: createCriteria pada currentSession() ZK MEMBUTUHKAN transaksi
		// aktif ("createCriteria is not valid without active transaction"). Setelah operasi lain
		// (mis. hapus) gagal/menutup transaksi, currentSession() tidak lagi punya tx aktif.
		// currentNativeSession() boleh createCriteria tanpa tx & self-healing (dikelola thread-local).
		Session session = HibernateUtil.currentNativeSession();

		Criteria criteria = session.createCriteria(PembombotanNilai.class)

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(hanyaDibuatOlehAdmin.isChecked() ? Restrictions.isNull("dimilikiOleh")
						: Restrictions.sqlRestriction("true"))
				.add(searchnama != null && searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));

		if (!searchdibuat.getValue().trim().isEmpty()) {
			criteria.createAlias("dimilikiOleh", "dimilikiOleh")
					.add(Restrictions.ilike("dimilikiOleh.nama", searchdibuat.getValue().trim(), MatchMode.ANYWHERE));
		}

		if (order)
			criteria.addOrder(Order.asc("form"));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		if (grid == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);

		List<PembombotanNilai> pembombotanNilai = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
				PembombotanNilai.class);
		ListModel strset = new SimpleListModel(pembombotanNilai);
		grid.setRowRenderer(new PembobotanNilaiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
