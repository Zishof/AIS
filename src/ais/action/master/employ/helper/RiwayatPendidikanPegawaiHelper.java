package ais.action.master.employ.helper;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
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
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.employ.RiwayatPendidikanPegawaiDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.employ.Pendidikan;
import ais.database.model.employ.RiwayatPendidikanPegawai;
import ais.database.model.file.FotoLampiranPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class RiwayatPendidikanPegawaiHelper {

	private MyGrid grid = new MyGrid();
	private Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

	private Combobox pendidikan;
	private AmbilDataPegawaiBanbox ambilDataPegawaiBanbox;
	private AmbilDataPegawaiBanbox searchPegawai;
	private Combobox searchstatus;

	public Pegawai pegawai;
	private RiwayatPendidikanPegawai riwayatPendidikanPegawai;
	private Textbox alamatSekolah;
	private Textbox namaSekolah;
	private Combobox tahunLulus;
	private Combobox tahunMasuk;
	private Textbox jurusan;
	private Textbox noIjazah;
	private Textbox namaKepalaSekolah;
	private MyCheckboxConfig status;
	private MyGrid gridFotoGambar;
	private Boolean editable;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	public RiwayatPendidikanPegawaiHelper(Pegawai pegawai, Boolean editable) {
		this.pegawai = pegawai;
		this.editable = editable;

	}

	class RiwayatPendidikanPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final RiwayatPendidikanPegawai riwayatPendidikanPegawai = (RiwayatPendidikanPegawai) arg1;
			new ais.ui.util.MyHtml(
					"<font style=\"font-size: x-small;\">" + (riwayatPendidikanPegawai.getPegawai() == null ? ""
							: riwayatPendidikanPegawai.getPegawai().getNama()) + "</font>")
					.setParent(row);

			new Label(riwayatPendidikanPegawai.getTahunMasuk() == null ? ""
					: riwayatPendidikanPegawai.getTahunMasuk().toString()).setParent(row);
			new Label(riwayatPendidikanPegawai.getTahunLulus() == null ? ""
					: riwayatPendidikanPegawai.getTahunLulus().toString()).setParent(row);
			new Label(riwayatPendidikanPegawai.getPendidikan() == null ? ""
					: riwayatPendidikanPegawai.getPendidikan().getNama()).setParent(row);
			new Label(
					riwayatPendidikanPegawai.getNamaSekolah() == null ? "" : riwayatPendidikanPegawai.getNamaSekolah())
					.setParent(row);
			new Label(riwayatPendidikanPegawai.getJurusan()).setParent(row);
			new Label(riwayatPendidikanPegawai.getNoIjazah()).setParent(row);
			new Label(riwayatPendidikanPegawai.getNamaKepalaSekolah()).setParent(row);
			new Label(riwayatPendidikanPegawai.getAlamatSekolah() == null ? ""
					: riwayatPendidikanPegawai.getAlamatSekolah()).setParent(row);
			new Image(riwayatPendidikanPegawai.getStatus() ? "/img/svg/check2.svg" : "/img/svg/warning-outline.svg")
					.setParent(row);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(riwayatPendidikanPegawai);

				}

			});
			button.setParent(toolbar);
			button.setDisabled(editable == false);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(!riwayatPendidikanPegawai.getStatus());
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
											RiwayatPendidikanPegawaiDao riwayatPendidikanPegawaiDao = DaoFactory
													.getInstance().getRiwayatPendidikanPegawaiDao();
											riwayatPendidikanPegawaiDao.delete((riwayatPendidikanPegawai));
											onSearchDefault(null);
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
			button.setDisabled(editable == false);

			toolbar.setParent(row);
		}
	}

	public Borderlayout display() throws Exception {

		North north = new North();
		Center center = new Center();

		Common.clear(borderlayout);

		borderlayout.setWidth("100%");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Columns columns = new Columns();
		columns.setParent(searchgrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("15%");
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("15%");
		column = new MyColumnConfig();
		column.setParent(columns);

		final Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
		row.appendChild(searchPegawai = new AmbilDataPegawaiBanbox());
		searchPegawai.setWidth("90%");
		searchPegawai.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		searchparent = new AmbilDataSatuanKerjaBanbox();
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (pegawai != null) {
			searchPegawai.setValue(pegawai.toString());
			searchPegawai.setAttribute("pegawai", pegawai);
			searchPegawai.setDisabled(true);

		} else {
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("15%");
			column = new MyColumnConfig();
			column.setParent(columns);

			row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
			row.appendChild(searchparent);
			searchparent.setWidth("90%");
			searchparent.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});

		}

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));
		row.appendChild(searchstatus = new Combobox());
		MyComboitemConfig comboitem = new MyComboitemConfig("Disetujui");
		comboitem.setValue(true);
		searchstatus.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Belum Disetujui");
		comboitem.setValue(false);
		searchstatus.appendChild(comboitem);
		searchstatus.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(div);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Tambah Riwayat Pendidikan", "/img/new.gif");
		toolbarbutton.setDisabled(editable == false);
		toolbar.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				init(new RiwayatPendidikanPegawai());
			}
		});

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(center);

		columns = new Columns();
		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pegawai");
		column.setWidth(pegawai == null ? "15%" : "0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Masuk");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Lulus");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tingkat");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No Ijazah");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kep. Sek.");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Alamat");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		List<RiwayatPendidikanPegawai> riwayatPendidikanPegawai = session.createCriteria(RiwayatPendidikanPegawai.class)

				.createAlias("pegawai", "pegawai")
				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("pegawai.satuanKerja", satuanKerjas))

				.addOrder(Order.asc("tahunMasuk"))
				.add(searchPegawai.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pegawai", searchPegawai.getAttribute("pegawai")))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))

				.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(riwayatPendidikanPegawai);
		grid.setRowRenderer(new RiwayatPendidikanPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void init(final RiwayatPendidikanPegawai riwayatPendidikanPegawai) throws Exception {
		this.riwayatPendidikanPegawai = riwayatPendidikanPegawai;

		South south = new South();
		Toolbar toolbar = new Toolbar();
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		MyToolbarbuttonConfig kembali = new MyToolbarbuttonConfig("Kembali", "/img/cancel.gif");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		final MyWindow window = new MyWindow("Pendataan Riwayat Pendidikan", "none", true);
		window.setWidth("90%");
		window.setHeight("97%");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.appendChild(borderlayout);

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("60%");

		east.appendChild(new FotoLampiranPegawaiHelper(gridFotoGambar = new MyGrid())
				.initDetail(riwayatPendidikanPegawai, RiwayatPendidikanPegawai.class));

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		final MyGrid grid = new MyGrid();
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

		final Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
		row.appendChild(ambilDataPegawaiBanbox = new AmbilDataPegawaiBanbox());
		ambilDataPegawaiBanbox.setValue(
				riwayatPendidikanPegawai.getPegawai() == null ? "" : riwayatPendidikanPegawai.getPegawai().getNama());
		ambilDataPegawaiBanbox.setAttribute("pegawai", riwayatPendidikanPegawai.getPegawai());
		ambilDataPegawaiBanbox.setWidth("90%");

		if (pegawai != null) {
			ambilDataPegawaiBanbox.setValue(pegawai.toString());
			ambilDataPegawaiBanbox.setAttribute("pegawai", pegawai);
			ambilDataPegawaiBanbox.setDisabled(!Common.getApakahAdmin());
		}

		Common.insertCombo(pendidikan = new Combobox(), "nama", Pendidikan.class);
		tahunLulus = new Combobox();
		MyComboitemConfig comboitem;
		for (int i = (ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR))
				- 90; i < (Calendar.getInstance().get(Calendar.YEAR) + 20); i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahunLulus.appendChild(comboitem);
		}

		tahunLulus.setReadonly(true);

		tahunMasuk = new Combobox();
		for (int i = (ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR))
				- 90; i < (Calendar.getInstance().get(Calendar.YEAR) + 20); i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahunMasuk.appendChild(comboitem);
		}

		tahunMasuk.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tingkat *"));
		row.appendChild(pendidikan);
		Common.insertCombo(pendidikan, "nama", Pendidikan.class);
		Common.selectComboItem(pendidikan,
				riwayatPendidikanPegawai.getPendidikan() == null ? null : riwayatPendidikanPegawai.getPendidikan());
		pendidikan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Masuk *"));
		row.appendChild(tahunMasuk);
		Common.selectComboItem(tahunMasuk,
				riwayatPendidikanPegawai.getTahunMasuk() == null ? null : riwayatPendidikanPegawai.getTahunMasuk());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Lulus *"));
		row.appendChild(tahunLulus);
		Common.selectComboItem(tahunLulus,
				riwayatPendidikanPegawai.getTahunLulus() == null ? null : riwayatPendidikanPegawai.getTahunLulus());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Sekolah *"));
		row.appendChild(namaSekolah = new Textbox(
				riwayatPendidikanPegawai.getNamaSekolah() == null ? "" : riwayatPendidikanPegawai.getNamaSekolah()));
		namaSekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jurusan/Peminatan *"));
		row.appendChild(jurusan = new Textbox(riwayatPendidikanPegawai.getJurusan()));
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("STTB/Tanda Lulus/Ijazah"));
		row.appendChild(noIjazah = new Textbox(riwayatPendidikanPegawai.getNoIjazah()));
		noIjazah.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kepala Sekolah"));
		row.appendChild(namaKepalaSekolah = new Textbox(riwayatPendidikanPegawai.getNamaKepalaSekolah()));
		namaKepalaSekolah.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Sekolah"));
		row.appendChild(alamatSekolah = new Textbox(riwayatPendidikanPegawai.getAlamatSekolah() == null ? ""
				: riwayatPendidikanPegawai.getAlamatSekolah()));
		alamatSekolah.setWidth("90%");
		alamatSekolah.setRows(3);

		if (riwayatPendidikanPegawai.getStatus()) {
			Common.freeze(grid, true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));
		row.appendChild(status = new MyCheckboxConfig());
		row.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE));
		status.setChecked(riwayatPendidikanPegawai.getStatus());
		status.setDisabled(!CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE));
		status.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.freeze(grid, status.isChecked());
				status.setDisabled(false);
				if (pegawai != null) {
					ambilDataPegawaiBanbox.setValue(pegawai.toString());
					ambilDataPegawaiBanbox.setAttribute("pegawai", pegawai);
					ambilDataPegawaiBanbox.setDisabled(!Common.getApakahAdmin());
				}
			}
		});

		south.setParent(borderlayout);

		toolbar.setParent(south);

		kembali.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				window.detach();
			}
		});
		kembali.setParent(toolbar);

		simpan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				if (save(arg0)) {
					display();
					window.detach();
				}
			}
		});
		simpan.setParent(toolbar);

		window.onModal();
	}

	@SuppressWarnings("unchecked")
	public boolean save(Event event) throws Exception {

		if (ambilDataPegawaiBanbox.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Mohon maaf, Data Pegawai belum dipilih. Langkah yang dapat dilakukan: (1) cari dan pilih Pegawai menggunakan kolom pencarian; (2) pastikan data pegawai sudah terdaftar di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK,
					"");
			return false;
		}

		if (pendidikan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tingkat Pendidikan belum dipilih. Langkah yang dapat dilakukan: (1) pilih Tingkat Pendidikan dari dropdown; (2) pastikan data tingkat pendidikan tersedia; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
			return false;
		}

		if (tahunMasuk.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tahun Masuk belum dipilih. Langkah yang dapat dilakukan: (1) pilih Tahun Masuk dari dropdown; (2) pastikan tahun yang dipilih sudah benar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK,
					"");
			return false;
		}

		if (tahunLulus.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tahun Lulus belum dipilih. Langkah yang dapat dilakukan: (1) pilih Tahun Lulus dari dropdown; (2) pastikan tahun yang dipilih sudah benar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK,
					"");
			return false;
		}

		if (namaSekolah.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Nama Sekolah belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Sekolah/Institusi; (2) pastikan nama tidak kosong; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK,
					"");
			return false;
		}

//		if (alamatSekolah.getValue() == null) {
//			MyMessageboxConfig.show("Alamat Sekolah Harus Diisi", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK,
//					"");
//			return false;
//		}

		Integer masuk = (Integer) (tahunMasuk.getSelectedItem() == null ? null
				: tahunMasuk.getSelectedItem().getValue());
		Integer lulus = (Integer) (tahunLulus.getSelectedItem() == null ? 0 : tahunLulus.getSelectedItem().getValue());

		if (masuk > lulus) {
			MyMessageboxConfig.show("Tahun masuk tidak boleh lebih besar dari tahun lulus",
					MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
			return false;
		}

		List<Row> rowsDocument = gridFotoGambar.getRows().getChildren();
		for (Row row : rowsDocument) {
			FotoLampiranPegawai fotoLampiranPegawai = (FotoLampiranPegawai) row.getAttribute("fotoLampiranPegawai");
			if (fotoLampiranPegawai.getItem() == null) {
				MyMessageboxConfig.show("Mohon maaf, File lampiran belum diunggah. Langkah yang dapat dilakukan: (1) klik tombol unggah dan pilih file dokumen; (2) pastikan file dalam format yang didukung; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		RiwayatPendidikanPegawaiDao riwayatPendidikanPegawaiDao = DaoFactory.getInstance()
				.getRiwayatPendidikanPegawaiDao();
		if (riwayatPendidikanPegawai.getId() != null) {
			riwayatPendidikanPegawai = riwayatPendidikanPegawaiDao.load(riwayatPendidikanPegawai.getId());
		}

		riwayatPendidikanPegawai.setStatus(status.isChecked());
		riwayatPendidikanPegawai.setJurusan(jurusan.getValue());
		riwayatPendidikanPegawai.setNoIjazah(noIjazah.getValue());
		riwayatPendidikanPegawai.setNamaKepalaSekolah(namaKepalaSekolah.getValue());
		riwayatPendidikanPegawai.setPegawai((Pegawai) ambilDataPegawaiBanbox.getAttribute("pegawai"));
		riwayatPendidikanPegawai.setPendidikan(
				(Pendidikan) (pendidikan.getSelectedItem() == null ? null : pendidikan.getSelectedItem().getValue()));
		riwayatPendidikanPegawai.setTahunMasuk(masuk);
		riwayatPendidikanPegawai.setTahunLulus(lulus);
		riwayatPendidikanPegawai.setAlamatSekolah(alamatSekolah.getValue());
		riwayatPendidikanPegawai.setNamaSekolah(namaSekolah.getValue());

		if (riwayatPendidikanPegawai.getId() != null) {
			riwayatPendidikanPegawaiDao.update(riwayatPendidikanPegawai);
		} else {
			riwayatPendidikanPegawaiDao.save(riwayatPendidikanPegawai);
		}

		Session mysession = StreamingHibernateUtil.getInstance().currentSession();
		try {
			mysession.getTransaction().begin();
			for (Row row : rowsDocument) {
				FotoLampiranPegawai fotoLampiranPegawai = (FotoLampiranPegawai) row.getAttribute("fotoLampiranPegawai");
				fotoLampiranPegawai.setItem(riwayatPendidikanPegawai.getId());
				fotoLampiranPegawai.setClazz(RiwayatPendidikanPegawai.class.getName());
				mysession.saveOrUpdate(fotoLampiranPegawai);
			}
			mysession.getTransaction().commit();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (mysession != null && mysession.isOpen()) {
				try {
					mysession.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/helper/RiwayatPendidikanPegawaiHelper.java:680");
				}
				try {
					mysession.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/helper/RiwayatPendidikanPegawaiHelper.java:684");
				}
			}
			StreamingHibernateUtil.getInstance().closeSession();
		}

		return true;
	}
}
