package ais.action.master.employ.helper;

import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
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
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.employ.Keluarga;
import ais.database.model.file.FotoLampiranPegawai;
import ais.database.model.payroll.AsuransiPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KeluargaPegawaiHelper implements DataCriteria, DataSearchDefault {

	private MyGrid grid = new MyGrid();
	private Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

	public Pegawai pegawai;
	public Keluarga keluarga;
	private AmbilDataPegawaiBanbox ambilDataPegawaiBanbox;
	private AmbilDataPegawaiBanbox searchPegawai;
	private Combobox searchstatus;

	private Combobox hubungan;
	private Textbox nama;
	private Textbox tempatLahir;
	private MyDatebox tanggalLahir;
	private MyDatebox tanggalNikah;
	private Combobox jenisKelamin;
	private Textbox alamat;
	private Textbox pekerjaan;
	private Textbox keterangan;
	private Textbox keteranganTambahan;
	private MyCheckboxConfig status;
	private MyGrid gridFotoGambar;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Combobox asuransiPegawai1;
	private Textbox nomorAsuransiPegawai1;
	private MyDoublebox premiAsuransi1;

	public KeluargaPegawaiHelper(final Pegawai pegawai) {
		this.pegawai = pegawai;

	}

	class KeluargaPegawaiHelperRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Keluarga keluarga = (Keluarga) arg1;

			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">"
					+ (keluarga.getPegawai() == null ? "" : keluarga.getPegawai().getNama()) + "</font>")
					.setParent(row);

			new Label(keluarga.getHubungan() == null ? "" : keluarga.getHubungan()).setParent(row);

			if (keluarga.getTanggalLahir() != null) {

			}

			new Label(keluarga.getTanggalLahir() == null ? ""
					: Common.dateFormat1.get().format(keluarga.getTanggalLahir()) + " ("
							+ Common.hitungUmur(keluarga.getTanggalLahir()) + " thn)")
					.setParent(row);

			new Label(keluarga.getNama() == null ? "" : keluarga.getNama()).setParent(row);
			new Label(keluarga.getJenisKelamin() == null ? "" : keluarga.getJenisKelamin()).setParent(row);
			new Label(keluarga.getAlamat()).setParent(row);

			new Label(keluarga.getAsuransiPegawai1() == null ? "" : keluarga.getAsuransiPegawai1().getNama())
					.setParent(row);

			new Label(keluarga.getKeterangan()).setParent(row);

			new Image(keluarga.getStatus() ? "/img/svg/check2.svg" : "/img/svg/warning-outline.svg").setParent(row);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(keluarga);

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(!keluarga.getStatus());
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

											Common.refreshDelete(keluarga);
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
		row.appendChild(searchPegawai = new AmbilDataPegawaiBanbox(true));
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

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Tambah Data", "/img/new.gif");
		toolbar.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				init(new Keluarga());
			}
		});

		String[] contents = new String[] { "id", "pegawai", "hubungan", "nama", "tempatLahir", "tanggalLahir",
				"tanggalNikah", "jenisKelamin", "alamat", "pekerjaan", "keteranganTambahan", "pendidikan",
				"jurusanPendidikan", "status", "menikah", "asuransiPegawai1", "nomorAsuransiPegawai1" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Keluarga.class, this, contents);
		toolbar.appendChild(cetakToolbarbutton);

		if (CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE)) {
			MyToolbarbuttonConfig upload = Common.uploadData(this, Keluarga.class, contents);
			toolbar.appendChild(upload);
		}

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
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
		column.setLabel("Hubungan");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tgl. Lahir / Usia");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis Kelamin");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Alamat");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Asuransi");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

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

		List<Keluarga> riwayatPendidikanPegawai = initCriteria(true)

				.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(riwayatPendidikanPegawai);

		grid.setRowRenderer(new KeluargaPegawaiHelperRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void init(final Keluarga keluarga) throws Exception {
		this.keluarga = keluarga;

		hubungan = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Keluarga.SUAMI);
		comboitem.setValue(Keluarga.SUAMI);
		hubungan.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Keluarga.ISTRI);
		comboitem.setValue(Keluarga.ISTRI);
		hubungan.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Keluarga.ANAK);
		comboitem.setValue(Keluarga.ANAK);
		hubungan.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Keluarga.MERTUA);
		comboitem.setValue(Keluarga.MERTUA);
		hubungan.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Keluarga.ORANG_TUA);
		comboitem.setValue(Keluarga.ORANG_TUA);
		hubungan.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Keluarga.SAUDARA);
		comboitem.setValue(Keluarga.SAUDARA);
		hubungan.appendChild(comboitem);

		jenisKelamin = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Laki-laki");
		comboitem.setValue("Laki-laki");
		jenisKelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Perempuan");
		comboitem.setValue("Perempuan");
		jenisKelamin.appendChild(comboitem);

		South south = new South();
		Center center = new Center();

		Toolbar toolbar = new Toolbar();
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		MyToolbarbuttonConfig kembali = new MyToolbarbuttonConfig("Kembali", "/img/cancel.gif");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		final MyWindow window = new MyWindow("Pendataan Keluarga", "none", true);
		window.setWidth("90%");
		window.setHeight("97%");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.appendChild(borderlayout);

		Common.clear(borderlayout);
		borderlayout.setWidth("100%");

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("60%");

		east.appendChild(
				new FotoLampiranPegawaiHelper(gridFotoGambar = new MyGrid()).initDetail(keluarga, Keluarga.class));

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
		row.appendChild(ambilDataPegawaiBanbox = new AmbilDataPegawaiBanbox(true));
		ambilDataPegawaiBanbox.setValue(keluarga.getPegawai() == null ? "" : keluarga.getPegawai().getNama());
		ambilDataPegawaiBanbox.setAttribute("pegawai", keluarga.getPegawai());
		ambilDataPegawaiBanbox.setWidth("90%");

		if (pegawai != null) {
			ambilDataPegawaiBanbox.setValue(pegawai.toString());
			ambilDataPegawaiBanbox.setAttribute("pegawai", pegawai);
			ambilDataPegawaiBanbox.setDisabled(!Common.getApakahAdmin());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hubungan *"));
		row.appendChild(hubungan);
		Common.selectComboItem(hubungan, keluarga.getHubungan() == null ? null : keluarga.getHubungan());
		hubungan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Hubungan (Nikah)"));
		row.appendChild(tanggalNikah = new MyDatebox(keluarga.getTanggalNikah()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama *"));
		row.appendChild(nama = new Textbox(keluarga.getNama() == null ? "" : keluarga.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat Lahir"));
		row.appendChild(tempatLahir = new Textbox(keluarga.getTempatLahir() == null ? "" : keluarga.getTempatLahir()));
		tempatLahir.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Lahir"));
		row.appendChild(tanggalLahir = new MyDatebox(keluarga.getTanggalLahir()));
		// tanggalLahir.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kelamin"));
		row.appendChild(jenisKelamin);
		Common.selectComboItem(jenisKelamin, keluarga.getJenisKelamin() == null ? null : keluarga.getJenisKelamin());
		jenisKelamin.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat"));
		row.appendChild(alamat = new Textbox(keluarga.getAlamat() == null ? "" : keluarga.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pekerjaan"));
		row.appendChild(pekerjaan = new Textbox(keluarga.getPekerjaan() == null ? "" : keluarga.getPekerjaan()));
		pekerjaan.setWidth("90%");

		asuransiPegawai1 = new Combobox();
		Common.insertComboDanSemua(asuransiPegawai1, new String[] { "nama" }, "keterangan", AsuransiPegawai.class,
				"=Tidak Ada Asuransi=",
				Restrictions.and(
						Restrictions.or(Restrictions.isNull("jenis"),
								Restrictions.or(Restrictions.eq("jenis", AsuransiPegawai.JENIS_KHUSUS_UNTUK_KELUARGA),
										Restrictions.eq("jenis", AsuransiPegawai.JENIS_UNTUK_KEDUANYA))),
						Restrictions.eq("aktif", true)));
		Common.selectComboItem(asuransiPegawai1, keluarga.getAsuransiPegawai1());

		nomorAsuransiPegawai1 = new Textbox(keluarga.getNomorAsuransiPegawai1());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Asuransi"));
		row.appendChild(asuransiPegawai1);
		asuransiPegawai1.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Asuransi"));
		row.appendChild(nomorAsuransiPegawai1);
		nomorAsuransiPegawai1.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Premi Asuransi"));
		row.appendChild(premiAsuransi1 = new MyDoublebox(keluarga.getPremiAsuransi1()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(keluarga.getKeterangan() == null ? "" : keluarga.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Tambahan"));
		row.appendChild(keteranganTambahan = new Textbox(
				keluarga.getKeteranganTambahan() == null ? "" : keluarga.getKeteranganTambahan()));
		keteranganTambahan.setWidth("90%");
		keteranganTambahan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));
		row.appendChild(status = new MyCheckboxConfig());
		status.setAttribute("janganDisabled", true);
		row.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE));
		status.setChecked(keluarga.getStatus());
		status.setDisabled(!CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE));

		status.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.freeze(grid, status.isChecked());
			}
		});
		if (keluarga.getStatus()) {
			Common.freeze(grid, true);
		}

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

		kembali.setAttribute("janganDisabled", true);
		simpan.setAttribute("janganDisabled", true);

		window.onModal();
	}

	@SuppressWarnings("unchecked")
	public boolean save(Event event) throws Exception {
		if (ambilDataPegawaiBanbox.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Mohon maaf, Data Pegawai belum dipilih. Langkah yang dapat dilakukan: (1) cari dan pilih Pegawai menggunakan kolom pencarian; (2) pastikan data pegawai sudah terdaftar di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK,
					"");
			return false;
		}

		if (hubungan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Hubungan Keluarga belum dipilih. Langkah yang dapat dilakukan: (1) pilih Hubungan Keluarga dari dropdown; (2) pastikan pilihan sudah tersedia; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION,
					MyMessageboxConfig.OK, "");
			return false;
		}

		if (nama.getValue() == "") {
			MyMessageboxConfig.show("Mohon maaf, Nama belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama pada form; (2) pastikan nama tidak kosong; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
			return false;
		}

		if (jenisKelamin.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Kelamin belum dipilih. Langkah yang dapat dilakukan: (1) pilih Jenis Kelamin dari dropdown; (2) pastikan pilihan sudah tersedia; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION,
					MyMessageboxConfig.OK, "");
			return false;
		}

		if (alamat.getValue() == "") {
			MyMessageboxConfig.show("Mohon maaf, Alamat belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Alamat pada form; (2) pastikan alamat tidak kosong; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
			return false;
		}

		if (pekerjaan.getValue() == "") {
			MyMessageboxConfig.show("Mohon maaf, Pekerjaan belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Pekerjaan pada form; (2) pastikan pekerjaan tidak kosong; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
			return false;
		}

		List<Row> rowsDocument = gridFotoGambar.getRows().getChildren();
		for (Row row : rowsDocument) {
			FotoLampiranPegawai fotoLampiranPegawai = (FotoLampiranPegawai) row.getAttribute("fotoLampiranPegawai");
			if (fotoLampiranPegawai.getItem() == null) {
				MyMessageboxConfig.show("Mohon maaf, File lampiran belum diunggah. Langkah yang dapat dilakukan: (1) klik tombol unggah dan pilih file dokumen yang sesuai; (2) pastikan file dalam format yang didukung (PDF/JPG/PNG); (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (keluarga.getId() != null) {
			keluarga = (Keluarga) session.load(Keluarga.class, keluarga.getId());
		}
		keluarga.setTanggalNikah(tanggalNikah.getValue());
		keluarga.setStatus(status.isChecked());
		keluarga.setPegawai((Pegawai) ambilDataPegawaiBanbox.getAttribute("pegawai"));
		keluarga.setNama(nama.getValue());
		keluarga.setHubungan((String) hubungan.getSelectedItem().getValue());
		keluarga.setTempatLahir(tempatLahir.getValue());
		keluarga.setTanggalLahir(tanggalLahir.getValue());
		keluarga.setJenisKelamin((String) jenisKelamin.getSelectedItem().getValue());
		keluarga.setAlamat(alamat.getValue());
		keluarga.setPekerjaan(pekerjaan.getValue());
		keluarga.setKeterangan(keterangan.getValue());
		keluarga.setKeteranganTambahan(keteranganTambahan.getValue());

		keluarga.setAsuransiPegawai1((AsuransiPegawai) (asuransiPegawai1.getSelectedItem() == null ? null
				: asuransiPegawai1.getSelectedItem().getValue()));
		keluarga.setNomorAsuransiPegawai1(nomorAsuransiPegawai1.getValue().trim());
		keluarga.setPremiAsuransi1(premiAsuransi1.getValue());

		if (keluarga.getId() != null) {
			Common.refreshUpdate(session, keluarga);
		} else {
			session.save(keluarga);
			session.flush();
		}

		Session mysession = StreamingHibernateUtil.getInstance().currentSession();
		try {
			mysession.getTransaction().begin();
			for (Row row : rowsDocument) {
				FotoLampiranPegawai fotoLampiranPegawai = (FotoLampiranPegawai) row.getAttribute("fotoLampiranPegawai");
				fotoLampiranPegawai.setItem(keluarga.getId());
				fotoLampiranPegawai.setClazz(Keluarga.class.getName());
				mysession.saveOrUpdate(fotoLampiranPegawai);
			}
			mysession.getTransaction().commit();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		StreamingHibernateUtil.getInstance().closeSession();

		return true;
	}

	@Override
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		return session.createCriteria(Keluarga.class)

				.createAlias("pegawai", "pegawai")
				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("pegawai.satuanKerja", satuanKerjas))

				.addOrder(Order.asc("hubungan"))
				.add(searchPegawai.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pegawai", searchPegawai.getAttribute("pegawai")))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()));
	}
}
