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
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
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
import ais.database.model.employ.RiwayatOrganisasiKampusPegawai;
import ais.database.model.file.FotoLampiranPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper UI ZK untuk mengelola riwayat organisasi kampus ({@link RiwayatOrganisasiKampusPegawai})
 * milik satu {@link Pegawai} — pola dan struktur identik dengan {@link RiwayatKerjaPegawaiHelper},
 * hanya berbeda entitas dan field tambahan {@code periode}: daftar riwayat dalam grid
 * berpencarian, form tambah/ubah (nama organisasi, kedudukan, rentang tahun, periode, alamat,
 * pimpinan, status — status {@code true} tidak dapat dihapus), serta lampiran foto/dokumen
 * pendukung lewat {@link FotoLampiranPegawai}.
 */
public class RiwayatOrganisasiKampusPegawaiHelper {

	private MyGrid grid = new MyGrid();
	private Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

	private AmbilDataPegawaiBanbox ambilDataPegawaiBanbox;
	private AmbilDataPegawaiBanbox searchPegawai;
	private Combobox searchstatus;

	public Pegawai pegawai;
	private RiwayatOrganisasiKampusPegawai riwayatOrganisasiKampusPegawai;

	private Textbox nama;
	private Textbox kedudukan;
	private Intbox tahunMulai;
	private Intbox tahunSelesai;
	private Textbox pimpinan;
	private Textbox periode;

	private Textbox alamat;
	private MyCheckboxConfig status;
	private MyGrid gridFotoGambar;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/** Menyiapkan helper untuk {@code pegawai} tertentu, atau untuk semua pegawai bila {@code null}. */
	public RiwayatOrganisasiKampusPegawaiHelper(Pegawai pegawai) {
		this.pegawai = pegawai;

	}

	/** Renderer baris grid untuk {@link RiwayatOrganisasiKampusPegawai}: pegawai, nama organisasi, kedudukan, rentang tahun, alamat, pimpinan, ikon status (centang/peringatan), dan tombol ubah/hapus (hapus hanya untuk status belum ok). */
	class RiwayatOrganisasiKampusPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final RiwayatOrganisasiKampusPegawai riwayatOrganisasiKampusPegawai = (RiwayatOrganisasiKampusPegawai) arg1;

			new ais.ui.util.MyHtml(
					"<font style=\"font-size: x-small;\">" + (riwayatOrganisasiKampusPegawai.getPegawai() == null ? ""
							: riwayatOrganisasiKampusPegawai.getPegawai().toString()) + "</font>")
					.setParent(row);

			new Label(riwayatOrganisasiKampusPegawai.getNama() == null ? "" : riwayatOrganisasiKampusPegawai.getNama())
					.setParent(row);
			new Label(riwayatOrganisasiKampusPegawai.getKedudukan()).setParent(row);
			new Label(riwayatOrganisasiKampusPegawai.getTahunMulai() + "").setParent(row);
			new Label(riwayatOrganisasiKampusPegawai.getTahunSelesai() + "").setParent(row);
			new Label(riwayatOrganisasiKampusPegawai.getAlamat() == null ? ""
					: riwayatOrganisasiKampusPegawai.getAlamat()).setParent(row);
			new Label(riwayatOrganisasiKampusPegawai.getPimpinan()).setParent(row);
			new Label(riwayatOrganisasiKampusPegawai.getPeriode()).setParent(row);

			new Image(
					riwayatOrganisasiKampusPegawai.getStatus() ? "/img/svg/check2.svg" : "/img/svg/warning-outline.svg")
					.setParent(row);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(riwayatOrganisasiKampusPegawai);

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(!riwayatOrganisasiKampusPegawai.getStatus());
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
											Session session = HibernateUtil.currentSession();
											session.delete((riwayatOrganisasiKampusPegawai));
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

			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(row);
		}
	}

	/** Membangun kerangka layar daftar riwayat organisasi: panel filter (pegawai, status, satuan kerja) di utara dan grid rincian di tengah, lalu langsung memuat datanya. */
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
				init(new RiwayatOrganisasiKampusPegawai());
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
		column.setLabel("Nama Organisasi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kedudukan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Mulai");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Selesai");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tempat");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pimpinan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Periode");

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
	/** Memuat ulang daftar {@link RiwayatOrganisasiKampusPegawai} sesuai filter aktif ke grid. */
	public void onSearchDefault(Event event) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		List<RiwayatOrganisasiKampusPegawai> riwayatOrganisasiKampusPegawai = session
				.createCriteria(RiwayatOrganisasiKampusPegawai.class)

				.createAlias("pegawai", "pegawai")
				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("pegawai.satuanKerja", satuanKerjas))

				.addOrder(Order.asc("tahunMulai"))
				.add(searchPegawai.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pegawai", searchPegawai.getAttribute("pegawai")))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))
				.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(riwayatOrganisasiKampusPegawai);

		grid.setRowRenderer(new RiwayatOrganisasiKampusPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** Membangun form tambah/ubah untuk {@code riwayatOrganisasiKampusPegawai} (baru atau sudah ada): nama organisasi, kedudukan, rentang tahun, periode, alamat, pimpinan, status, dan lampiran foto/dokumen, menggantikan tampilan daftar sementara. */
	public void init(final RiwayatOrganisasiKampusPegawai riwayatOrganisasiKampusPegawai) throws Exception {
		this.riwayatOrganisasiKampusPegawai = riwayatOrganisasiKampusPegawai;

		South south = new South();
		Toolbar toolbar = new Toolbar();
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		MyToolbarbuttonConfig kembali = new MyToolbarbuttonConfig("Kembali", "/img/cancel.gif");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		final MyWindow window = new MyWindow("Pendataan Riwayat Organisasi Kampus", "none", true);
		window.setWidth("90%");
		window.setHeight("97%");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.appendChild(borderlayout);

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("60%");

		east.appendChild(new FotoLampiranPegawaiHelper(gridFotoGambar = new MyGrid())
				.initDetail(riwayatOrganisasiKampusPegawai, RiwayatOrganisasiKampusPegawai.class));

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
		ambilDataPegawaiBanbox.setValue(riwayatOrganisasiKampusPegawai.getPegawai() == null ? ""
				: riwayatOrganisasiKampusPegawai.getPegawai().getNama());
		ambilDataPegawaiBanbox.setAttribute("pegawai", riwayatOrganisasiKampusPegawai.getPegawai());
		ambilDataPegawaiBanbox.setWidth("90%");

		if (pegawai != null) {
			ambilDataPegawaiBanbox.setValue(pegawai.toString());
			ambilDataPegawaiBanbox.setAttribute("pegawai", pegawai);
			ambilDataPegawaiBanbox.setDisabled(!Common.getApakahAdmin());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Organisasi"));
		row.appendChild(nama = new Textbox(riwayatOrganisasiKampusPegawai.getNama()));
		nama.setWidth("90%");
		nama.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kedudukan"));
		row.appendChild(kedudukan = new Textbox(riwayatOrganisasiKampusPegawai.getKedudukan()));
		kedudukan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Mulai"));
		row.appendChild(tahunMulai = new Intbox(riwayatOrganisasiKampusPegawai.getTahunMulai()));
		tahunMulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Sampai"));
		row.appendChild(tahunSelesai = new Intbox(riwayatOrganisasiKampusPegawai.getTahunSelesai()));
		tahunSelesai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat"));
		row.appendChild(alamat = new Textbox(riwayatOrganisasiKampusPegawai.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pimpinan"));
		row.appendChild(pimpinan = new Textbox(riwayatOrganisasiKampusPegawai.getPimpinan()));
		pimpinan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Periode"));
		row.appendChild(periode = new Textbox(riwayatOrganisasiKampusPegawai.getPeriode()));
		periode.setWidth("90%");

		if (riwayatOrganisasiKampusPegawai.getStatus()) {
			Common.freeze(grid, true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));
		row.appendChild(status = new MyCheckboxConfig());
		row.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE));
		status.setChecked(riwayatOrganisasiKampusPegawai.getStatus());
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
	/** Memvalidasi dan menyimpan data {@link RiwayatOrganisasiKampusPegawai} dari form; mengembalikan {@code false} bila validasi gagal (pesan sudah ditampilkan ke pengguna), {@code true} bila berhasil disimpan. */
	public boolean save(Event event) throws Exception {

		if (ambilDataPegawaiBanbox.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Mohon maaf, Data Pegawai belum dipilih. Langkah yang dapat dilakukan: (1) cari dan pilih Pegawai menggunakan kolom pencarian; (2) pastikan data pegawai sudah terdaftar di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK,
					"");
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Organisasi belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Organisasi; (2) pastikan nama tidak kosong; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION,
					MyMessageboxConfig.OK, "");
			return false;
		}

		if (tahunMulai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tahun Mulai belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Tahun Mulai; (2) pastikan nilai berupa tahun yang valid; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK,
					"");
			return false;
		}

		if (tahunSelesai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tahun Selesai belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Tahun Selesai; (2) pastikan nilai berupa tahun yang valid; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK,
					"");
			return false;
		}

		if (alamat.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Tempat belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Tempat pada form; (2) pastikan nama tempat tidak kosong; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
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

		Session session = HibernateUtil.currentSession();
		if (riwayatOrganisasiKampusPegawai.getId() != null) {
			riwayatOrganisasiKampusPegawai = (RiwayatOrganisasiKampusPegawai) session
					.load(RiwayatOrganisasiKampusPegawai.class, riwayatOrganisasiKampusPegawai.getId());
		}

		riwayatOrganisasiKampusPegawai.setPeriode(periode.getValue());
		riwayatOrganisasiKampusPegawai.setStatus(status.isChecked());
		riwayatOrganisasiKampusPegawai.setKedudukan(kedudukan.getValue());
		riwayatOrganisasiKampusPegawai.setPimpinan(pimpinan.getValue());
		riwayatOrganisasiKampusPegawai.setTahunSelesai(tahunSelesai.getValue());
		riwayatOrganisasiKampusPegawai.setTahunMulai(tahunMulai.getValue());
		riwayatOrganisasiKampusPegawai.setPegawai((Pegawai) ambilDataPegawaiBanbox.getAttribute("pegawai"));
		riwayatOrganisasiKampusPegawai.setAlamat(alamat.getValue());
		riwayatOrganisasiKampusPegawai.setNama(nama.getValue());

		if (riwayatOrganisasiKampusPegawai.getId() != null) {
			session.update(riwayatOrganisasiKampusPegawai);
		} else {
			session.save(riwayatOrganisasiKampusPegawai);
		}

		Session mysession = StreamingHibernateUtil.getInstance().currentSession();
		try {
			mysession.getTransaction().begin();
			for (Row row : rowsDocument) {
				FotoLampiranPegawai fotoLampiranPegawai = (FotoLampiranPegawai) row.getAttribute("fotoLampiranPegawai");
				fotoLampiranPegawai.setItem(riwayatOrganisasiKampusPegawai.getId());
				fotoLampiranPegawai.setClazz(RiwayatOrganisasiKampusPegawai.class.getName());
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
}
