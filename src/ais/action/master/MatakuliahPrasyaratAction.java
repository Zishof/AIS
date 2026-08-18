package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Matakuliah;
import ais.database.model.MatakuliahPrasyarat;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class MatakuliahPrasyaratAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchprasyarat;
	private Checkbox searchaktif;

	private AmbilDataMatakuliahBanbox prasyarat;
	private AmbilDataMatakuliahBanbox matakuliah;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private MatakuliahPrasyarat matakuliahPrasyarat;
	private MyToolbarbuttonConfig add;
	private MyDoublebox minimalNilaiLulus;
	private AmbilDataMatakuliahBanbox prasyarat2;
	private AmbilDataMatakuliahBanbox prasyarat3;
	private AmbilDataMatakuliahBanbox prasyarat4;
	private AmbilDataMatakuliahBanbox prasyarat5;
	private MyIntbox minimalSks;
	private MyDoublebox minimalIpk;
	private AmbilDataMatakuliahBanbox prasyarat6;
	private AmbilDataMatakuliahBanbox prasyarat7;
	private AmbilDataMatakuliahBanbox prasyarat8;
	private AmbilDataMatakuliahBanbox prasyarat9;
	private AmbilDataMatakuliahBanbox prasyarat10;

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

		String[] contents = new String[] { "id", "matakuliah", "matakuliahPrasyarat", "matakuliahPrasyarat2",
				"matakuliahPrasyarat3", "matakuliahPrasyarat4", "matakuliahPrasyarat5", "matakuliahPrasyarat6",
				"matakuliahPrasyarat7", "matakuliahPrasyarat8", "matakuliahPrasyarat9", "matakuliahPrasyarat10",
				"minimalNilaiLulus", "minimalSks", "minimalIpk", "hanyaBerdasarkanKode", "aktif", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, MatakuliahPrasyarat.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class MatakuliahPrasyaratRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final MatakuliahPrasyarat matakuliahPrasyarat = (MatakuliahPrasyarat) arg1;

			RevisiHelper.createNewRevisi(MatakuliahPrasyarat.class, matakuliahPrasyarat,
					matakuliahPrasyarat.getMatakuliah() == null ? ""
							: matakuliahPrasyarat.getMatakuliah().getId() + "-"
									+ matakuliahPrasyarat.getMatakuliah().getKode() + "-"
									+ matakuliahPrasyarat.getMatakuliah().getNama() + " ("
									+ matakuliahPrasyarat.getMatakuliah().getJurusan().getNama() + ")")
					.setParent(arg0);
			new Label(matakuliahPrasyarat.getMatakuliahPrasyarat() == null ? ""
					: matakuliahPrasyarat.getMatakuliahPrasyarat().getId() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat().getNama() + " ("
							+ (matakuliahPrasyarat.getMatakuliahPrasyarat().getJurusan() == null ? ""
									: matakuliahPrasyarat.getMatakuliahPrasyarat().getJurusan().getNama() + ")"))
											.setParent(arg0);

			new Label(matakuliahPrasyarat.getMatakuliahPrasyarat2() == null ? ""
					: matakuliahPrasyarat.getMatakuliahPrasyarat2().getId() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat2().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat2().getNama() + " ("
							+ (matakuliahPrasyarat.getMatakuliahPrasyarat2().getJurusan() == null ? ""
									: matakuliahPrasyarat.getMatakuliahPrasyarat2().getJurusan().getNama() + ")"))
											.setParent(arg0);

			new Label(matakuliahPrasyarat.getMatakuliahPrasyarat3() == null ? ""
					: matakuliahPrasyarat.getMatakuliahPrasyarat3().getId() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat3().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat3().getNama() + " ("
							+ (matakuliahPrasyarat.getMatakuliahPrasyarat3().getJurusan() == null ? ""
									: matakuliahPrasyarat.getMatakuliahPrasyarat3().getJurusan().getNama() + ")"))
											.setParent(arg0);

			new Label(matakuliahPrasyarat.getMatakuliahPrasyarat4() == null ? ""
					: matakuliahPrasyarat.getMatakuliahPrasyarat4().getId() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat4().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat4().getNama() + " ("
							+ (matakuliahPrasyarat.getMatakuliahPrasyarat4().getJurusan() == null ? ""
									: matakuliahPrasyarat.getMatakuliahPrasyarat4().getJurusan().getNama() + ")"))
											.setParent(arg0);

			new Label(matakuliahPrasyarat.getMatakuliahPrasyarat5() == null ? ""
					: matakuliahPrasyarat.getMatakuliahPrasyarat5().getId() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat5().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat5().getNama() + " ("
							+ (matakuliahPrasyarat.getMatakuliahPrasyarat5().getJurusan() == null ? ""
									: matakuliahPrasyarat.getMatakuliahPrasyarat5().getJurusan().getNama() + ")"))
											.setParent(arg0);

			new Label(matakuliahPrasyarat.getMatakuliahPrasyarat6() == null ? ""
					: matakuliahPrasyarat.getMatakuliahPrasyarat6().getId() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat6().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat6().getNama() + " ("
							+ (matakuliahPrasyarat.getMatakuliahPrasyarat6().getJurusan() == null ? ""
									: matakuliahPrasyarat.getMatakuliahPrasyarat6().getJurusan().getNama() + ")"))
											.setParent(arg0);

			new Label(matakuliahPrasyarat.getMatakuliahPrasyarat7() == null ? ""
					: matakuliahPrasyarat.getMatakuliahPrasyarat7().getId() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat7().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat7().getNama() + " ("
							+ (matakuliahPrasyarat.getMatakuliahPrasyarat7().getJurusan() == null ? ""
									: matakuliahPrasyarat.getMatakuliahPrasyarat7().getJurusan().getNama() + ")"))
											.setParent(arg0);

			new Label(matakuliahPrasyarat.getMatakuliahPrasyarat8() == null ? ""
					: matakuliahPrasyarat.getMatakuliahPrasyarat8().getId() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat8().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat8().getNama() + " ("
							+ (matakuliahPrasyarat.getMatakuliahPrasyarat8().getJurusan() == null ? ""
									: matakuliahPrasyarat.getMatakuliahPrasyarat8().getJurusan().getNama() + ")"))
											.setParent(arg0);

			new Label(matakuliahPrasyarat.getMatakuliahPrasyarat9() == null ? ""
					: matakuliahPrasyarat.getMatakuliahPrasyarat9().getId() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat9().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat9().getNama() + " ("
							+ (matakuliahPrasyarat.getMatakuliahPrasyarat9().getJurusan() == null ? ""
									: matakuliahPrasyarat.getMatakuliahPrasyarat9().getJurusan().getNama() + ")"))
											.setParent(arg0);

			new Label(matakuliahPrasyarat.getMatakuliahPrasyarat10() == null ? ""
					: matakuliahPrasyarat.getMatakuliahPrasyarat10().getId() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat10().getKode() + "-"
							+ matakuliahPrasyarat.getMatakuliahPrasyarat10().getNama() + " ("
							+ (matakuliahPrasyarat.getMatakuliahPrasyarat10().getJurusan() == null ? ""
									: matakuliahPrasyarat.getMatakuliahPrasyarat10().getJurusan().getNama() + ")"))
											.setParent(arg0);

			new Label(Common.numberFormat.get().format(matakuliahPrasyarat.getMinimalNilaiLulus())).setParent(arg0);

			new Label(Common.numberFormat.get().format(matakuliahPrasyarat.getMinimalSks())).setParent(arg0);
			new Label(Common.numberFormat.get().format(matakuliahPrasyarat.getMinimalIpk())).setParent(arg0);

			final MyCheckboxConfig hanyaBerdasarkanKode = new MyCheckboxConfig("Hanya Berdasarkan Kode MK");
			hanyaBerdasarkanKode.setDisabled(!edit);
			hanyaBerdasarkanKode.setChecked(matakuliahPrasyarat.getHanyaBerdasarkanKode());
			hanyaBerdasarkanKode.setParent(arg0);
			hanyaBerdasarkanKode.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					matakuliahPrasyarat.setHanyaBerdasarkanKode(hanyaBerdasarkanKode.isChecked());
					Common.refreshSaveOrUpdate(matakuliahPrasyarat);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(matakuliahPrasyarat.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					matakuliahPrasyarat.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(matakuliahPrasyarat);
				}
			});

			new Label(matakuliahPrasyarat.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(matakuliahPrasyarat);
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
											Common.refreshDelete(matakuliahPrasyarat);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
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

	public void onAdd(Event event) throws Exception {
		init(new MatakuliahPrasyarat());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(MatakuliahPrasyarat matakuliahPrasyarat) {
		this.matakuliahPrasyarat = matakuliahPrasyarat;
		addWindow.setTitle(matakuliahPrasyarat.getId() == null ? "Tambah Matakuliah Prasyarat" : "Ubah Matakuliah Prasyarat");
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

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah *"));
		row.appendChild(matakuliah = new AmbilDataMatakuliahBanbox());
		matakuliah.setAttribute("matakuliah", matakuliahPrasyarat.getMatakuliah());
		matakuliah.setValue(matakuliahPrasyarat.getMatakuliah() == null ? ""
				: matakuliahPrasyarat.getMatakuliah().getKode() + "-" + matakuliahPrasyarat.getMatakuliah().getNama());
		matakuliah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prasyarat"));
		row.appendChild(prasyarat = new AmbilDataMatakuliahBanbox());
		prasyarat.setAttribute("matakuliah", matakuliahPrasyarat.getMatakuliahPrasyarat());
		prasyarat.setValue(matakuliahPrasyarat.getMatakuliahPrasyarat() == null ? ""
				: matakuliahPrasyarat.getMatakuliahPrasyarat().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat().getNama());
		prasyarat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Atau bisa Menggunakan Prasyarat "));
		row.appendChild(prasyarat2 = new AmbilDataMatakuliahBanbox());
		prasyarat2.setAttribute("matakuliah", matakuliahPrasyarat.getMatakuliahPrasyarat2());
		prasyarat2.setValue(matakuliahPrasyarat.getMatakuliahPrasyarat2() == null ? ""
				: matakuliahPrasyarat.getMatakuliahPrasyarat2().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat2().getNama());
		prasyarat2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Atau bisa Menggunakan Prasyarat "));
		row.appendChild(prasyarat3 = new AmbilDataMatakuliahBanbox());
		prasyarat3.setAttribute("matakuliah", matakuliahPrasyarat.getMatakuliahPrasyarat3());
		prasyarat3.setValue(matakuliahPrasyarat.getMatakuliahPrasyarat3() == null ? ""
				: matakuliahPrasyarat.getMatakuliahPrasyarat3().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat3().getNama());
		prasyarat3.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Atau bisa Menggunakan Prasyarat "));
		row.appendChild(prasyarat4 = new AmbilDataMatakuliahBanbox());
		prasyarat4.setAttribute("matakuliah", matakuliahPrasyarat.getMatakuliahPrasyarat4());
		prasyarat4.setValue(matakuliahPrasyarat.getMatakuliahPrasyarat4() == null ? ""
				: matakuliahPrasyarat.getMatakuliahPrasyarat4().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat4().getNama());
		prasyarat4.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Atau bisa Menggunakan Prasyarat "));
		row.appendChild(prasyarat5 = new AmbilDataMatakuliahBanbox());
		prasyarat5.setAttribute("matakuliah", matakuliahPrasyarat.getMatakuliahPrasyarat5());
		prasyarat5.setValue(matakuliahPrasyarat.getMatakuliahPrasyarat5() == null ? ""
				: matakuliahPrasyarat.getMatakuliahPrasyarat5().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat5().getNama());
		prasyarat5.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Atau bisa Menggunakan Prasyarat "));
		row.appendChild(prasyarat6 = new AmbilDataMatakuliahBanbox());
		prasyarat6.setAttribute("matakuliah", matakuliahPrasyarat.getMatakuliahPrasyarat6());
		prasyarat6.setValue(matakuliahPrasyarat.getMatakuliahPrasyarat6() == null ? ""
				: matakuliahPrasyarat.getMatakuliahPrasyarat6().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat6().getNama());
		prasyarat6.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Atau bisa Menggunakan Prasyarat "));
		row.appendChild(prasyarat7 = new AmbilDataMatakuliahBanbox());
		prasyarat7.setAttribute("matakuliah", matakuliahPrasyarat.getMatakuliahPrasyarat7());
		prasyarat7.setValue(matakuliahPrasyarat.getMatakuliahPrasyarat7() == null ? ""
				: matakuliahPrasyarat.getMatakuliahPrasyarat7().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat7().getNama());
		prasyarat7.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Atau bisa Menggunakan Prasyarat "));
		row.appendChild(prasyarat8 = new AmbilDataMatakuliahBanbox());
		prasyarat8.setAttribute("matakuliah", matakuliahPrasyarat.getMatakuliahPrasyarat8());
		prasyarat8.setValue(matakuliahPrasyarat.getMatakuliahPrasyarat8() == null ? ""
				: matakuliahPrasyarat.getMatakuliahPrasyarat8().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat8().getNama());
		prasyarat8.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Atau bisa Menggunakan Prasyarat "));
		row.appendChild(prasyarat9 = new AmbilDataMatakuliahBanbox());
		prasyarat9.setAttribute("matakuliah", matakuliahPrasyarat.getMatakuliahPrasyarat9());
		prasyarat9.setValue(matakuliahPrasyarat.getMatakuliahPrasyarat9() == null ? ""
				: matakuliahPrasyarat.getMatakuliahPrasyarat9().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat9().getNama());
		prasyarat9.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Atau bisa Menggunakan Prasyarat "));
		row.appendChild(prasyarat10 = new AmbilDataMatakuliahBanbox());
		prasyarat10.setAttribute("matakuliah", matakuliahPrasyarat.getMatakuliahPrasyarat10());
		prasyarat10.setValue(matakuliahPrasyarat.getMatakuliahPrasyarat10() == null ? ""
				: matakuliahPrasyarat.getMatakuliahPrasyarat10().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat10().getNama());
		prasyarat10.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal Nilai Lulus *"));
		row.appendChild(minimalNilaiLulus = new MyDoublebox(matakuliahPrasyarat.getMinimalNilaiLulus()));
		minimalNilaiLulus.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal SKS boleh ambil *"));
		row.appendChild(minimalSks = new MyIntbox(matakuliahPrasyarat.getMinimalSks()));
		minimalSks.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal IPK boleh ambil *"));
		row.appendChild(minimalIpk = new MyDoublebox(matakuliahPrasyarat.getMinimalIpk()));
		minimalIpk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(matakuliahPrasyarat.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (matakuliah.getAttribute("matakuliah") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Matakuliah",
					"Kolom Matakuliah belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Matakuliah.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		// Prasyarat kini OPSIONAL: boleh dikosongkan (hanya mengisi Minimal Nilai Lulus / SKS / IPK).
		// Tidak ada lagi validasi wajib-isi untuk prasyarat.

		Session session = HibernateUtil.currentSession();
		if (matakuliahPrasyarat.getId() != null) {
			matakuliahPrasyarat = (MatakuliahPrasyarat) session.load(MatakuliahPrasyarat.class,
					matakuliahPrasyarat.getId());
		}

		matakuliahPrasyarat.setMatakuliah((Matakuliah) matakuliah.getAttribute("matakuliah"));
		matakuliahPrasyarat.setMatakuliahPrasyarat((Matakuliah) prasyarat.getAttribute("matakuliah"));
		matakuliahPrasyarat.setMatakuliahPrasyarat2((Matakuliah) prasyarat2.getAttribute("matakuliah"));
		matakuliahPrasyarat.setMatakuliahPrasyarat3((Matakuliah) prasyarat3.getAttribute("matakuliah"));
		matakuliahPrasyarat.setMatakuliahPrasyarat4((Matakuliah) prasyarat4.getAttribute("matakuliah"));
		matakuliahPrasyarat.setMatakuliahPrasyarat5((Matakuliah) prasyarat5.getAttribute("matakuliah"));

		matakuliahPrasyarat.setMatakuliahPrasyarat6((Matakuliah) prasyarat6.getAttribute("matakuliah"));
		matakuliahPrasyarat.setMatakuliahPrasyarat7((Matakuliah) prasyarat7.getAttribute("matakuliah"));
		matakuliahPrasyarat.setMatakuliahPrasyarat8((Matakuliah) prasyarat8.getAttribute("matakuliah"));
		matakuliahPrasyarat.setMatakuliahPrasyarat9((Matakuliah) prasyarat9.getAttribute("matakuliah"));
		matakuliahPrasyarat.setMatakuliahPrasyarat10((Matakuliah) prasyarat10.getAttribute("matakuliah"));

		matakuliahPrasyarat.setMinimalNilaiLulus(minimalNilaiLulus.getValue());
		matakuliahPrasyarat.setKeterangan(keterangan.getValue());
		matakuliahPrasyarat.setMinimalSks(minimalSks.getValue());
		matakuliahPrasyarat.setMinimalIpk(minimalIpk.getValue());

		Common.refreshSaveOrUpdate(session, matakuliahPrasyarat);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(MatakuliahPrasyarat.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.createAlias("matakuliah", "matakuliah").createAlias("matakuliahPrasyarat", "matakuliahPrasyarat");

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.or(
						Restrictions.ilike("matakuliah.kode", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("matakuliah.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)))

				.add(searchprasyarat.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("matakuliahPrasyarat.kode", searchprasyarat.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("matakuliahPrasyarat.nama", searchprasyarat.getValue().trim(),
										MatchMode.ANYWHERE)));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<MatakuliahPrasyarat> matakuliahPrasyarat = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(matakuliahPrasyarat);
		grid.setRowRenderer(new MatakuliahPrasyaratRenderer());
		grid.setModelCheckMobile(strset);

	}

	public static void tampilPrasyarat(Component parent, Matakuliah matakuliah) {
		@SuppressWarnings("unchecked")
		List<MatakuliahPrasyarat> matakuliahPrasyarats = ConstantValues
				.simpleList(HibernateUtil.currentSession().createCriteria(MatakuliahPrasyarat.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("matakuliah", matakuliah)), MatakuliahPrasyarat.class);
		
		if (!matakuliahPrasyarats.isEmpty()) {
			new MyLabelKecil("Prasyarat :").setParent(parent);
		}
		int i = 1;
		for (MatakuliahPrasyarat matakuliahPrasyarat : matakuliahPrasyarats) {
			// Prasyarat opsional: lewati baris tanpa MK prasyarat (hanya gate Nilai/SKS/IPK).
			if (matakuliahPrasyarat.getMatakuliahPrasyarat() == null) {
				continue;
			}

			String mk = matakuliahPrasyarat.getMatakuliahPrasyarat().getKode() + "-"
					+ matakuliahPrasyarat.getMatakuliahPrasyarat().getNama();
			if (matakuliahPrasyarat.getMatakuliahPrasyarat2() != null) {
				mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat2().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat2().getNama();
			}
			if (matakuliahPrasyarat.getMatakuliahPrasyarat3() != null) {
				mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat3().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat3().getNama();
			}
			if (matakuliahPrasyarat.getMatakuliahPrasyarat4() != null) {
				mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat4().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat4().getNama();
			}
			if (matakuliahPrasyarat.getMatakuliahPrasyarat5() != null) {
				mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat5().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat5().getNama();
			}
			if (matakuliahPrasyarat.getMatakuliahPrasyarat6() != null) {
				mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat6().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat6().getNama();
			}
			if (matakuliahPrasyarat.getMatakuliahPrasyarat7() != null) {
				mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat7().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat7().getNama();
			}
			if (matakuliahPrasyarat.getMatakuliahPrasyarat8() != null) {
				mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat8().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat8().getNama();
			}
			if (matakuliahPrasyarat.getMatakuliahPrasyarat9() != null) {
				mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat9().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat9().getNama();
			}
			if (matakuliahPrasyarat.getMatakuliahPrasyarat10() != null) {
				mk += ", atau " + matakuliahPrasyarat.getMatakuliahPrasyarat10().getKode() + "-"
						+ matakuliahPrasyarat.getMatakuliahPrasyarat10().getNama();
			}



			new MyLabelKecil(i + ". " + mk + ", minimal "
					+ Common.numberFormat.get().format(matakuliahPrasyarat.getMinimalNilaiLulus())).setParent(parent);
			i++;
		}
	}

}
