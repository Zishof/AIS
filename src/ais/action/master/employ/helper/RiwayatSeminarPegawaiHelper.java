package ais.action.master.employ.helper;

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
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.employ.Seminar;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class RiwayatSeminarPegawaiHelper {

	private MyGrid grid = new MyGrid();
	private Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

	private AmbilDataPegawaiBanbox ambilDataPegawaiBanbox;
	private AmbilDataPegawaiBanbox searchPegawai;
	private Combobox searchstatus;

	public Pegawai pegawai;
	private Seminar riwayatSeminarPegawai;

	private Textbox judulSeminar;
	private MyDatebox tanggalMulai;
	private MyDatebox tanggalSelesai;
	private Textbox keterangan;

	private Textbox lokasi;
	private Textbox pembicaraUtama;
	private Textbox sebagai;

	private MyCheckboxConfig status;
	protected LampiranLain lainMahasiswa;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	public RiwayatSeminarPegawaiHelper(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	class RiwayatSeminarPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Seminar riwayatSeminarPegawai = (Seminar) arg1;

			Vbox a = RevisiHelper.createNewRevisi(Seminar.class, riwayatSeminarPegawai,
					(riwayatSeminarPegawai.getPegawai() == null ? "" : riwayatSeminarPegawai.getPegawai().getNama()));
			a.setParent(row);
			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, riwayatSeminarPegawai.getId(), Seminar.class.getName(),
					"Lampiran/Sertifikat", false, null, null, false, false, false, false);

			new Label(riwayatSeminarPegawai.getTanggalMulai() == null ? ""
					: Common.dateFormat6.get().format(riwayatSeminarPegawai.getTanggalMulai())).setParent(row);
			new Label(riwayatSeminarPegawai.getTanggalSelesai() == null ? ""
					: Common.dateFormat6.get().format(riwayatSeminarPegawai.getTanggalSelesai())).setParent(row);

			new Label(riwayatSeminarPegawai.getJudulSeminar() == null ? "" : riwayatSeminarPegawai.getJudulSeminar())
					.setParent(row);
			new Label(riwayatSeminarPegawai.getLokasi() == null ? "" : riwayatSeminarPegawai.getLokasi())
					.setParent(row);
			new Label(
					riwayatSeminarPegawai.getPembicaraUtama() == null ? "" : riwayatSeminarPegawai.getPembicaraUtama())
					.setParent(row);
			new Label(riwayatSeminarPegawai.getSebagai() == null ? "" : riwayatSeminarPegawai.getSebagai())
					.setParent(row);
			new Label(riwayatSeminarPegawai.getKeterangan()).setParent(row);
			new Image(riwayatSeminarPegawai.getStatus() ? "/img/svg/check2.svg" : "/img/svg/warning-outline.svg")
					.setParent(row);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(riwayatSeminarPegawai);

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(!riwayatSeminarPegawai.getStatus());
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
											Session session = HibernateUtil.currentSession();
											session.delete((riwayatSeminarPegawai));
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

		MyFormRow row = new MyFormRow();row.setValign("top");
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

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Tambah Data", "/img/new.gif");
		toolbar.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				init(new Seminar());
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
		column.setLabel("Tanggal Mulai");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal Selesai");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Judul Seminar");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Lokasi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pembicara Utama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sebagai");

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
		column.setWidth("7%");

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
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		List<Seminar> riwayatSeminarPegawai = session.createCriteria(Seminar.class)

				.createAlias("pegawai", "pegawai")
				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("pegawai.satuanKerja", satuanKerjas))

				.addOrder(Order.asc("tanggalMulai"))
				.add(searchPegawai.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pegawai", searchPegawai.getAttribute("pegawai")))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))

				.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(riwayatSeminarPegawai);

		grid.setRowRenderer(new RiwayatSeminarPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void init(final Seminar riwayatSeminarPegawai) throws Exception {
		this.riwayatSeminarPegawai = riwayatSeminarPegawai;

		South south = new South();
		Toolbar toolbar = new Toolbar();
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		MyToolbarbuttonConfig kembali = new MyToolbarbuttonConfig("Kembali", "/img/cancel.gif");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		final MyWindow window = new MyWindow("Pendataan Riwayat Seminar", "none", true);
		window.setWidth("90%");
		window.setHeight("97%");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.appendChild(borderlayout);

		// East east = new East();
		// east.setParent(borderlayout);
		// ais.ui.util.ZkCompat.setFlex(east, true);
		// east.setWidth("60%");
		//
		// east.appendChild(new FotoLampiranPegawaiHelper(
		// gridFotoGambar = new MyGrid()).initDetail(riwayatSeminarPegawai,
		// Seminar.class));

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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
		row.appendChild(ambilDataPegawaiBanbox = new AmbilDataPegawaiBanbox());
		ambilDataPegawaiBanbox.setValue(
				riwayatSeminarPegawai.getPegawai() == null ? "" : riwayatSeminarPegawai.getPegawai().getNama());
		ambilDataPegawaiBanbox.setAttribute("pegawai", riwayatSeminarPegawai.getPegawai());
		ambilDataPegawaiBanbox.setWidth("90%");

		if (pegawai != null) {
			ambilDataPegawaiBanbox.setValue(pegawai.toString());
			ambilDataPegawaiBanbox.setAttribute("pegawai", pegawai);
			ambilDataPegawaiBanbox.setDisabled(!Common.getApakahAdmin());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		row.appendChild(tanggalMulai = new MyDatebox(riwayatSeminarPegawai.getTanggalMulai()));
		tanggalMulai.setFormat(Common.dateFormat1.get().toPattern());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Selesai"));
		row.appendChild(tanggalSelesai = new MyDatebox(riwayatSeminarPegawai.getTanggalSelesai()));
		tanggalSelesai.setFormat(Common.dateFormat1.get().toPattern());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Seminar"));
		row.appendChild(judulSeminar = new Textbox(riwayatSeminarPegawai.getJudulSeminar()));
		judulSeminar.setWidth("90%");
		judulSeminar.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi"));
		row.appendChild(lokasi = new Textbox(riwayatSeminarPegawai.getLokasi()));
		lokasi.setWidth("90%");
		lokasi.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pembicara Utama"));
		row.appendChild(pembicaraUtama = new Textbox(riwayatSeminarPegawai.getPembicaraUtama()));
		pembicaraUtama.setWidth("90%");
		pembicaraUtama.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sebagai"));
		row.appendChild(sebagai = new Textbox(riwayatSeminarPegawai.getSebagai()));
		sebagai.setWidth("90%");
		sebagai.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(riwayatSeminarPegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran / Sertifikat Seminar"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, riwayatSeminarPegawai.getId(), Seminar.class.getName(),
				"Lampiran / Sertifikat Seminar", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows,
				"Jika file Lampiran / Sertifikat Seminar lebih dari satu file, zip dulu semua file tersebut");

		if (riwayatSeminarPegawai.getStatus()) {
			Common.freeze(grid, true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));
		row.appendChild(status = new MyCheckboxConfig());
		row.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE));
		status.setChecked(riwayatSeminarPegawai.getStatus());
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

	public boolean save(Event event) throws Exception {

		if (ambilDataPegawaiBanbox.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Mohon maaf, Data Pegawai belum dipilih. Langkah yang dapat dilakukan: (1) cari dan pilih Pegawai menggunakan kolom pencarian; (2) pastikan data pegawai sudah terdaftar di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK,
					"");
			return false;
		}

		if (tanggalMulai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Mulai belum diisi. Langkah yang dapat dilakukan: (1) pilih Tanggal Mulai menggunakan datepicker; (2) pastikan tanggal sudah benar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
			return false;
		}

		if (tanggalSelesai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Sampai/Selesai belum diisi. Langkah yang dapat dilakukan: (1) pilih Tanggal Selesai menggunakan datepicker; (2) pastikan tanggal sudah benar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
			return false;
		}

		if (judulSeminar.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Pelatihan/Diklat/Kursus belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Pelatihan/Seminar; (2) pastikan nama tidak kosong; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION,
					MyMessageboxConfig.OK, "");
			return false;
		}

		if (lokasi.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Lokasi Pelatihan/Diklat/Kursus belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Lokasi Pelatihan/Seminar; (2) pastikan nama lokasi tidak kosong; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION,
					MyMessageboxConfig.OK, "");
			return false;
		}

		// List<Row> rowsDocument = gridFotoGambar.getRows().getChildren();
		// for (Row row : rowsDocument) {
		// FotoLampiranPegawai fotoLampiranPegawai = (FotoLampiranPegawai) row
		// .getAttribute("fotoLampiranPegawai");
		// if (fotoLampiranPegawai.getItem() == null) {
		// MyMessageboxConfig.show("File harus diisi", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		// return false;
		// }
		// }

		Session session = HibernateUtil.currentSession();
		if (riwayatSeminarPegawai.getId() != null) {
			riwayatSeminarPegawai = (Seminar) session.load(Seminar.class, riwayatSeminarPegawai.getId());
		}

		riwayatSeminarPegawai.setStatus(status.isChecked());
		riwayatSeminarPegawai.setJudulSeminar(judulSeminar.getValue());
		riwayatSeminarPegawai.setLokasi(lokasi.getValue());

		riwayatSeminarPegawai.setPegawai((Pegawai) ambilDataPegawaiBanbox.getAttribute("pegawai"));
		riwayatSeminarPegawai.setTanggalMulai(tanggalMulai.getValue());
		riwayatSeminarPegawai.setTanggalSelesai(tanggalSelesai.getValue());
		riwayatSeminarPegawai.setPembicaraUtama(pembicaraUtama.getValue());
		riwayatSeminarPegawai.setSebagai(sebagai.getValue());
		riwayatSeminarPegawai.setKeterangan(keterangan.getValue());

		if (riwayatSeminarPegawai.getId() != null) {
			session.update(riwayatSeminarPegawai);
		} else {
			session.save(riwayatSeminarPegawai);
		}

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(riwayatSeminarPegawai.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		return true;
	}
}
