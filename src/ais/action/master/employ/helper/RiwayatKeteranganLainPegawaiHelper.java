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
import ais.database.model.employ.RiwayatKeteranganLainPegawai;
import ais.database.model.file.FotoLampiranPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper UI pengelola riwayat "Keterangan Lain" pegawai ({@link RiwayatKeteranganLainPegawai} —
 * mis. surat keterangan kerja, referensi, atau dokumen serupa yang diterbitkan pejabat), dengan
 * alur persetujuan dua tahap: baris baru dibuat dalam status belum disetujui, lalu pengguna
 * berhak {@link CommonPrivilages#APPROVE} dapat mencentang "Status Persetujuan" untuk mengunci
 * (membekukan) form ({@code Common.freeze}) — setelah disetujui, tombol hapus baris disembunyikan.
 * Bekerja dalam dua mode: terikat pada satu {@link Pegawai} tertentu (constructor diberi pegawai
 * non-null, dropdown pegawai dikunci) atau lintas pegawai dengan filter satuan kerja
 * hierarkis (lewat {@link SatuanKerjaTreeModel}, mencakup seluruh anak satuan kerja terpilih) dan
 * filter status persetujuan. Setiap entri dapat memiliki satu atau lebih lampiran foto/dokumen
 * lewat {@code FotoLampiranPegawaiHelper}, wajib diunggah sebelum entri dapat disimpan.
 */
public class RiwayatKeteranganLainPegawaiHelper {

	private MyGrid grid = new MyGrid();
	private Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

	private AmbilDataPegawaiBanbox ambilDataPegawaiBanbox;
	private AmbilDataPegawaiBanbox searchPegawai;
	private Combobox searchstatus;

	public Pegawai pegawai;
	private RiwayatKeteranganLainPegawai riwayatKeteranganLainPegawai;

	private Textbox nama;
	private MyDatebox tanggal;
	private Textbox pejabat;
	private Textbox nomor;

	private Textbox alamat;
	private MyCheckboxConfig status;
	private MyGrid gridFotoGambar;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/** Membuat helper; bila {@code pegawai} tidak {@code null}, tampilan terikat pada satu pegawai tersebut (dropdown pegawai dikunci), selain itu menampilkan lintas pegawai dengan filter satuan kerja. */
	public RiwayatKeteranganLainPegawaiHelper(final Pegawai pegawai) {
		this.pegawai = pegawai;

	}

	/** Renderer baris grid: nama pegawai, nama keterangan, pejabat, nomor, tanggal, tempat, ikon status persetujuan, dan tombol edit (membuka {@link #init}) + hapus (disembunyikan bila sudah disetujui, dengan dialog konfirmasi). */
	class RiwayatKeteranganLainPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final RiwayatKeteranganLainPegawai riwayatKeteranganLainPegawai = (RiwayatKeteranganLainPegawai) arg1;

			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">" + (riwayatKeteranganLainPegawai.getPegawai() == null ? ""
					: riwayatKeteranganLainPegawai.getPegawai().getNama()) + "</font>").setParent(row);

			new Label(riwayatKeteranganLainPegawai.getNama() == null ? "" : riwayatKeteranganLainPegawai.getNama())
					.setParent(row);
			new Label(riwayatKeteranganLainPegawai.getPejabat()).setParent(row);
			new Label(riwayatKeteranganLainPegawai.getNomor()).setParent(row);
			new Label(riwayatKeteranganLainPegawai.getTanggal() == null ? ""
					: Common.dateFormat1.get().format(riwayatKeteranganLainPegawai.getTanggal())).setParent(row);

			new Label(riwayatKeteranganLainPegawai.getAlamat() == null ? "" : riwayatKeteranganLainPegawai.getAlamat())
					.setParent(row);

			new Image(riwayatKeteranganLainPegawai.getStatus() ? "/img/svg/check2.svg" : "/img/svg/warning-outline.svg").setParent(row);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(riwayatKeteranganLainPegawai);

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(!riwayatKeteranganLainPegawai.getStatus());
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
											Common.refreshDelete((riwayatKeteranganLainPegawai));
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

	/**
	 * Membangun tata letak lengkap panel riwayat keterangan lain: baris filter (pegawai terkunci
	 * atau satuan kerja hierarkis bila lintas pegawai, plus status persetujuan), tombol "Tambah
	 * Data", dan grid berpaginasi (10 baris/halaman) yang dimuat lewat timer default (memberi
	 * kesempatan komponen filter selesai dirakit lebih dulu) memanggil {@link #onSearchDefault}.
	 *
	 * @return {@link Borderlayout} siap ditempelkan ke jendela detail pegawai
	 * @throws Exception diteruskan dari kegagalan pembangunan komponen
	 */
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
				init(new RiwayatKeteranganLainPegawai());
			}
		});

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pegawai");
		column.setWidth(pegawai == null ? "15%" : "0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pejabat");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nomor");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tempat");

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

	/** Menjalankan pencarian riwayat keterangan sesuai filter satuan kerja (termasuk seluruh turunannya)/pegawai/status persetujuan saat ini dan menyegarkan grid dengan {@link RiwayatKeteranganLainPegawaiRenderer}. */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		List<RiwayatKeteranganLainPegawai> riwayatKeteranganLainPegawai = session
				.createCriteria(RiwayatKeteranganLainPegawai.class)
				
				.createAlias("pegawai", "pegawai")
				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("pegawai.satuanKerja", satuanKerjas))
				
				.addOrder(Order.asc("tanggal"))
				.add(searchPegawai.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pegawai", searchPegawai.getAttribute("pegawai")))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))
				.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(riwayatKeteranganLainPegawai);

		grid.setRowRenderer(new RiwayatKeteranganLainPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membuka jendela modal tambah/edit satu riwayat keterangan lain: panel timur berisi pengelola
	 * lampiran foto/dokumen ({@code FotoLampiranPegawaiHelper}), panel tengah berisi form (pegawai,
	 * nama keterangan, pejabat, nomor, tanggal, tempat, dan checkbox status persetujuan — hanya
	 * tampil bagi pengguna berhak {@link CommonPrivilages#APPROVE}). Form dibekukan
	 * ({@code Common.freeze}) bila entri sudah berstatus disetujui, dan otomatis dibekukan/
	 * dibuka ulang saat checkbox status diubah. Tombol Simpan memicu {@link #save(Event)}.
	 *
	 * @param riwayatKeteranganLainPegawai entitas baru atau tersimpan yang akan diedit
	 * @throws Exception diteruskan dari kegagalan pembangunan komponen
	 */
	public void init(final RiwayatKeteranganLainPegawai riwayatKeteranganLainPegawai) throws Exception {
		this.riwayatKeteranganLainPegawai = riwayatKeteranganLainPegawai;

		South south = new South();
		Toolbar toolbar = new Toolbar();
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		MyToolbarbuttonConfig kembali = new MyToolbarbuttonConfig("Kembali", "/img/cancel.gif");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		final MyWindow window = new MyWindow("Pendataan Riwayat Keterangan Lain", "none", true);
		window.setWidth("90%");
		window.setHeight("97%");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.appendChild(borderlayout);

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("60%");

		east.appendChild(new FotoLampiranPegawaiHelper(gridFotoGambar = new MyGrid())
				.initDetail(riwayatKeteranganLainPegawai, RiwayatKeteranganLainPegawai.class));

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
		ambilDataPegawaiBanbox.setValue(riwayatKeteranganLainPegawai.getPegawai() == null ? ""
				: riwayatKeteranganLainPegawai.getPegawai().getNama());
		ambilDataPegawaiBanbox.setAttribute("pegawai", riwayatKeteranganLainPegawai.getPegawai());
		ambilDataPegawaiBanbox.setWidth("90%");

		if (pegawai != null) {
			ambilDataPegawaiBanbox.setValue(pegawai.toString());
			ambilDataPegawaiBanbox.setAttribute("pegawai", pegawai);
			ambilDataPegawaiBanbox.setDisabled(!Common.getApakahAdmin());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Keterangan"));
		row.appendChild(nama = new Textbox(riwayatKeteranganLainPegawai.getNama()));
		nama.setWidth("90%");
		nama.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pejabat"));
		row.appendChild(pejabat = new Textbox(riwayatKeteranganLainPegawai.getPejabat()));
		pejabat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor"));
		row.appendChild(nomor = new Textbox(riwayatKeteranganLainPegawai.getNomor()));
		nomor.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		row.appendChild(tanggal = new MyDatebox(riwayatKeteranganLainPegawai.getTanggal()));
		tanggal.setFormat(Common.dateFormat1.get().toPattern());
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat"));
		row.appendChild(alamat = new Textbox(riwayatKeteranganLainPegawai.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(3);

		if (riwayatKeteranganLainPegawai.getStatus()) {
			Common.freeze(grid, true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));
		row.appendChild(status = new MyCheckboxConfig());
		row.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE));
		status.setChecked(riwayatKeteranganLainPegawai.getStatus());
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

	/**
	 * Memvalidasi (pegawai, nama keterangan, dan tanggal wajib diisi; setiap baris lampiran pada
	 * grid foto harus sudah memiliki berkas terunggah) lalu menyimpan/memperbarui entitas
	 * {@link RiwayatKeteranganLainPegawai} beserta seluruh lampiran {@link FotoLampiranPegawai}
	 * terkait (dikaitkan lewat {@code item}/{@code clazz}) dalam sesi Hibernate streaming
	 * terpisah.
	 *
	 * @param event event pemicu (tidak dipakai langsung)
	 * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal
	 * @throws Exception diteruskan dari kegagalan Hibernate di luar penanganan lampiran (yang ditangkap dan ditampilkan hanya untuk admin)
	 */
	@SuppressWarnings("unchecked")
	public boolean save(Event event) throws Exception {

		if (ambilDataPegawaiBanbox.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Mohon maaf, Data Pegawai belum dipilih. Langkah yang dapat dilakukan: (1) cari dan pilih Pegawai menggunakan kolom pencarian; (2) pastikan data pegawai sudah terdaftar di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK,
					"");
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Keterangan belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Keterangan pada form; (2) pastikan nama tidak kosong; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION,
					MyMessageboxConfig.OK, "");
			return false;
		}

		if (tanggal.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal belum diisi. Langkah yang dapat dilakukan: (1) pilih Tanggal menggunakan datepicker; (2) pastikan tanggal yang dipilih sudah benar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
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
		if (riwayatKeteranganLainPegawai.getId() != null) {
			riwayatKeteranganLainPegawai = (RiwayatKeteranganLainPegawai) session
					.load(RiwayatKeteranganLainPegawai.class, riwayatKeteranganLainPegawai.getId());
		}

		riwayatKeteranganLainPegawai.setStatus(status.isChecked());
		riwayatKeteranganLainPegawai.setNomor(nomor.getValue());
		riwayatKeteranganLainPegawai.setPejabat(pejabat.getValue());
		riwayatKeteranganLainPegawai.setTanggal(tanggal.getValue());
		riwayatKeteranganLainPegawai.setPegawai((Pegawai) ambilDataPegawaiBanbox.getAttribute("pegawai"));
		riwayatKeteranganLainPegawai.setAlamat(alamat.getValue());
		riwayatKeteranganLainPegawai.setNama(nama.getValue());

		if (riwayatKeteranganLainPegawai.getId() != null) {
			session.update(riwayatKeteranganLainPegawai);
		} else {
			session.save(riwayatKeteranganLainPegawai);
		}

		Session mysession = StreamingHibernateUtil.getInstance().currentSession();
		try {
			mysession.getTransaction().begin();
			for (Row row : rowsDocument) {
				FotoLampiranPegawai fotoLampiranPegawai = (FotoLampiranPegawai) row.getAttribute("fotoLampiranPegawai");
				fotoLampiranPegawai.setItem(riwayatKeteranganLainPegawai.getId());
				fotoLampiranPegawai.setClazz(RiwayatKeteranganLainPegawai.class.getName());
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
