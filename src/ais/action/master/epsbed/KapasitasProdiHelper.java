package ais.action.master.epsbed;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

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
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.epsbed.KapasitasMahasiswaBaru;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class KapasitasProdiHelper {

	private MyGrid grid = new MyGrid();

	private Combobox searchTahunAkademik = new Combobox();
	private Jurusan jurusan;
	private Decimalbox jumlahTarget;
	private MyDatebox tanggalAwalPerkuliahanGanjil;
	private MyDatebox tanggalAkhirPerkuliahanGanjil;
	private Decimalbox jumlahMingguGanjil;
	private Decimalbox angkatanKe;
	private MyDatebox tanggalAwalPerkuliahanGenap;
	private MyDatebox tanggalAkhirPerkuliahanGenap;
	private Decimalbox jumlahMingguGenap;
	private Decimalbox jumlahSP;
	Combobox metodeSP = new Combobox();
	Combobox adaSP = new Combobox();
	Combobox metodeHariPerkuliahan = new Combobox();
	Combobox metodeHariPerkuliahanEkstensi = new Combobox();
	private Combobox tahunakademik;

	private Combobox genapGanjil;
	private KapasitasMahasiswaBaru kapasitasMahasiswaBaru;

	public KapasitasProdiHelper() {

		MyComboitemConfig comboitem = new MyComboitemConfig("A");
		comboitem.setValue("A");
		metodeHariPerkuliahan.appendChild(comboitem);
		comboitem = new MyComboitemConfig("B");
		comboitem.setValue("B");
		metodeHariPerkuliahan.appendChild(comboitem);
		comboitem = new MyComboitemConfig("C");
		comboitem.setValue("C");
		metodeHariPerkuliahan.appendChild(comboitem);

		comboitem = new MyComboitemConfig("A");
		comboitem.setValue("A");
		metodeHariPerkuliahanEkstensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig("B");
		comboitem.setValue("B");
		metodeHariPerkuliahanEkstensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig("C");
		comboitem.setValue("C");
		metodeHariPerkuliahanEkstensi.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Ya");
		comboitem.setValue("Ya");
		adaSP.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Tidak");
		comboitem.setValue("Tidak");
		adaSP.appendChild(comboitem);

		comboitem = new MyComboitemConfig("A");
		comboitem.setValue("A");
		metodeSP.appendChild(comboitem);
		comboitem = new MyComboitemConfig("B");
		comboitem.setValue("B");
		metodeSP.appendChild(comboitem);

		genapGanjil = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		genapGanjil.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		genapGanjil.appendChild(comboitem);

	}

	class KapasitasProdiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final KapasitasMahasiswaBaru kapasitasProdi = (KapasitasMahasiswaBaru) arg1;
			new Label(kapasitasProdi.getJurusan().getNama()).setParent(row);
			new Label(kapasitasProdi.getTahunAkademik()).setParent(row);
			new Label(kapasitasProdi.getGanjilGenap()).setParent(row);
			new Label(Common.numberFormat.get().format(kapasitasProdi.getJumlahTargetMahasiswaBaru())).setParent(row);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kapasitasProdi);
				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
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
									Common.refreshDelete(kapasitasProdi);
									onSearchDefault(jurusan);
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
		}

	}

	public Borderlayout display(final Jurusan jurusan) {
		this.jurusan = jurusan;
		Center center = new Center();
		North north = new North();

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
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

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		searchTahunAkademik = new Combobox();
		Common.generateTahunAjaran(searchTahunAkademik);

		row.appendChild(searchTahunAkademik);
		searchTahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				onSearchDefault(jurusan);
			}
		});
		searchTahunAkademik.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah", "/img/new.gif");
		button.setTooltiptext("Ubah Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				init(new KapasitasMahasiswaBaru(jurusan));

			}

		});

		toolbar.appendChild(button);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Prodi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Akademik");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ganjil/Genap");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Daya Tampung");

		column = new MyColumnConfig();
		column.setWidth("10%");
		column.setParent(columns);
		column.setLabel("");

		onSearchDefault(jurusan);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Jurusan jurusan) {

		Session session = HibernateUtil.currentSession();
		List<KapasitasMahasiswaBaru> kapasitasProdi = session.createCriteria(KapasitasMahasiswaBaru.class)
				.addOrder(Order.asc("tahunAkademik")).add(Restrictions.eq("jurusan", jurusan))
				.add(searchTahunAkademik.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", searchTahunAkademik.getSelectedItem().getValue()))

		.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(kapasitasProdi);

		grid.setRowRenderer(new KapasitasProdiRenderer());
		grid.setModelCheckMobile(strset);

	}

	@SuppressWarnings("deprecation")
	public void init(final KapasitasMahasiswaBaru kapasitasMahasiswaBaru) throws Exception {
		this.kapasitasMahasiswaBaru = kapasitasMahasiswaBaru;

		final Window window = new Window();
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setWidth("500px");
		window.setHeight("400px");
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);
		South south = new South();
		south.setParent(borderlayout);

		borderlayout.setWidth("100%");
		Common.clear(center);

		Common.generateTahunAjaran(searchTahunAkademik);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new ais.ui.util.MyLabelConfig(kapasitasMahasiswaBaru.getJurusan().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunakademik = Common.generateTahunAjaran(new Combobox()));
		Common.selectComboItem(tahunakademik, kapasitasMahasiswaBaru.getTahunAkademik());
		tahunakademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(genapGanjil);
		Common.selectComboItem(genapGanjil, kapasitasMahasiswaBaru.getGanjilGenap());
		genapGanjil.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan mahasiswa baru"));
		row.appendChild(angkatanKe = new Decimalbox(new BigDecimal(
				kapasitasMahasiswaBaru.getAngkatanKe() == null ? 0 : kapasitasMahasiswaBaru.getAngkatanKe())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Daya Tampung"));
		row.appendChild(
				jumlahTarget = new Decimalbox(new BigDecimal(kapasitasMahasiswaBaru.getJumlahTargetMahasiswaBaru())));

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Awal Perkuliahan Semester Ganjil"));
		row.appendChild(
				tanggalAwalPerkuliahanGanjil = new MyDatebox(kapasitasMahasiswaBaru.getAwalPerkuliahanGanjil() == null
						? ais.ui.util.WaktuUtil.getDate() : kapasitasMahasiswaBaru.getAwalPerkuliahanGanjil()));

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Akhir Perkuliahan Semester Ganjil"));
		row.appendChild(
				tanggalAkhirPerkuliahanGanjil = new MyDatebox(kapasitasMahasiswaBaru.getAkhirPerkuliahanGanjil() == null
						? ais.ui.util.WaktuUtil.getDate() : kapasitasMahasiswaBaru.getAkhirPerkuliahanGanjil()));

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Minggu Kuliah Semester Ganjil"));
		row.appendChild(jumlahMingguGanjil = new Decimalbox(
				new BigDecimal(kapasitasMahasiswaBaru.getJumlahMingguKuliahGanjil() == null ? 0
						: kapasitasMahasiswaBaru.getJumlahMingguKuliahGanjil())));

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Awal Perkuliahan Semester Genap"));
		row.appendChild(
				tanggalAwalPerkuliahanGenap = new MyDatebox(kapasitasMahasiswaBaru.getAwalPerkuliahanGenap() == null
						? ais.ui.util.WaktuUtil.getDate() : kapasitasMahasiswaBaru.getAwalPerkuliahanGenap()));

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Akhir Perkuliahan Semester Genap"));
		row.appendChild(
				tanggalAkhirPerkuliahanGenap = new MyDatebox(kapasitasMahasiswaBaru.getAkhirPerkuliahanGenap() == null
						? ais.ui.util.WaktuUtil.getDate() : kapasitasMahasiswaBaru.getAkhirPerkuliahanGenap()));

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Minggu Kuliah Semester Genap"));
		row.appendChild(jumlahMingguGenap = new Decimalbox(
				new BigDecimal(kapasitasMahasiswaBaru.getJumlahMingguKuliahGenap() == null ? 0
						: kapasitasMahasiswaBaru.getJumlahMingguKuliahGenap())));

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Metode Hari Perkuliahan"));

		row.appendChild(metodeHariPerkuliahan);
		Common.selectComboItem(metodeHariPerkuliahan, kapasitasMahasiswaBaru.getMetodeHariPerkuliahan() == null ? null
				: kapasitasMahasiswaBaru.getMetodeHariPerkuliahan());

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Metode Hari Perkuliahan Ekstensi"));
		row.appendChild(metodeHariPerkuliahanEkstensi);
		Common.selectComboItem(metodeHariPerkuliahanEkstensi,
				kapasitasMahasiswaBaru.getMetodeHariPerkuliahanEkstensi() == null ? null
						: kapasitasMahasiswaBaru.getMetodeHariPerkuliahanEkstensi());

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ada Semester Pendek ?"));
		row.appendChild(adaSP);
		Common.selectComboItem(adaSP,
				kapasitasMahasiswaBaru.getAdaSP() == null ? null : kapasitasMahasiswaBaru.getAdaSP());

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Semester Pendek"));
		row.appendChild(jumlahSP = new Decimalbox(new BigDecimal(
				kapasitasMahasiswaBaru.getJumlahSP() == null ? 0 : kapasitasMahasiswaBaru.getJumlahSP())));

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Metode Pelaksanaan Semester Pendek"));

		row.appendChild(metodeSP);
		Common.selectComboItem(metodeSP, kapasitasMahasiswaBaru.getMetodePelaksanaanSP() == null ? null
				: kapasitasMahasiswaBaru.getMetodePelaksanaanSP());
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan dan Kembali", "/img/box-icon64x64.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				if (save()) {
					window.detach();
					onSearchDefault(jurusan);
				}
			}
		});
		button.setParent(toolbar);
		window.onModal();
	}

	public boolean save() throws Exception {

		if (tahunakademik.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun akademik harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			tahunakademik.focus();
			return false;
		}

		if (genapGanjil.getSelectedItem() == null) {
			MyMessageboxConfig.show("Semester harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			genapGanjil.focus();
			return false;
		}

		Session session = HibernateUtil.currentSession();

		System.out.println("kapasitasMahasiswaBaru.getId() = " + kapasitasMahasiswaBaru.getId());

		if (kapasitasMahasiswaBaru.getId() != null) {
			kapasitasMahasiswaBaru = (KapasitasMahasiswaBaru) session.load(KapasitasMahasiswaBaru.class,
					kapasitasMahasiswaBaru.getId());
		} else {
			kapasitasMahasiswaBaru = new KapasitasMahasiswaBaru();
		}

		kapasitasMahasiswaBaru.setAngkatanKe(angkatanKe.getValue() == null ? 0 : angkatanKe.getValue().intValue());
		kapasitasMahasiswaBaru.setGanjilGenap((String) genapGanjil.getSelectedItem().getValue());
		kapasitasMahasiswaBaru.setTahunAkademik((String) tahunakademik.getSelectedItem().getValue());
		kapasitasMahasiswaBaru.setJurusan(jurusan);
		kapasitasMahasiswaBaru
				.setJumlahTargetMahasiswaBaru(jumlahTarget.getValue() == null ? 0 : jumlahTarget.getValue().intValue());
		kapasitasMahasiswaBaru.setAwalPerkuliahanGanjil(tanggalAwalPerkuliahanGanjil.getValue());
		kapasitasMahasiswaBaru.setAkhirPerkuliahanGanjil(tanggalAkhirPerkuliahanGanjil.getValue());
		kapasitasMahasiswaBaru.setJumlahMingguKuliahGanjil(
				jumlahMingguGanjil.getValue() == null ? 0 : jumlahMingguGanjil.getValue().intValue());
		kapasitasMahasiswaBaru.setAwalPerkuliahanGenap(tanggalAwalPerkuliahanGenap.getValue());
		kapasitasMahasiswaBaru.setAkhirPerkuliahanGenap(tanggalAkhirPerkuliahanGenap.getValue());
		kapasitasMahasiswaBaru.setJumlahMingguKuliahGenap(
				jumlahMingguGenap.getValue() == null ? 0 : jumlahMingguGenap.getValue().intValue());
		kapasitasMahasiswaBaru.setMetodeHariPerkuliahan((String) (metodeHariPerkuliahan.getSelectedItem() == null ? null
				: metodeHariPerkuliahan.getSelectedItem().getValue()));

		kapasitasMahasiswaBaru
				.setMetodeHariPerkuliahanEkstensi((String) (metodeHariPerkuliahanEkstensi.getSelectedItem() == null
						? null : metodeHariPerkuliahanEkstensi.getSelectedItem().getValue()));
		kapasitasMahasiswaBaru
				.setAdaSP((String) (adaSP.getSelectedItem() == null ? null : adaSP.getSelectedItem().getValue()));
		kapasitasMahasiswaBaru.setJumlahSP(jumlahSP.getValue() == null ? 0 : jumlahSP.getValue().intValue());
		kapasitasMahasiswaBaru.setMetodePelaksanaanSP(
				(String) (metodeSP.getSelectedItem() == null ? null : metodeSP.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, kapasitasMahasiswaBaru);
		return true;
	}
}
