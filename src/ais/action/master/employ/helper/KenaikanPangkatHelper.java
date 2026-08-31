package ais.action.master.employ.helper;

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
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.AmbilDataGolonganBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Jabatan;
import ais.database.model.Pegawai;
import ais.database.model.employ.Golongan;
import ais.database.model.employ.JabatanFungsional;
import ais.database.model.employ.JabatanStruktural;
import ais.database.model.employ.JenisKenaikanPangkat;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.employ.Peraturan;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoLampiranPegawai;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper UI ZK untuk mengelola riwayat kenaikan pangkat/golongan/jabatan ({@link KenaikanPangkat})
 * milik satu {@link Pegawai}: daftar riwayat dalam grid berpencarian (baris yang mencerminkan
 * jabatan yang sedang dijabat — {@code menjabat=true} — disorot hijau muda), form tambah/ubah
 * lengkap mencakup jenis kenaikan pangkat, jabatan (fungsional/struktural/umum — hanya salah
 * satu yang relevan per baris), golongan/gaji pokok, dasar peraturan, nomor dan tanggal surat
 * usul serta surat keputusan beserta pejabat penandatangan, rentang berlaku, dan lampiran
 * dokumen pendukung lewat {@link LampiranLain}.
 */
public class KenaikanPangkatHelper {

	private MyGrid grid = new MyGrid();
	private Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

	private AmbilDataPegawaiBanbox ambilDataPegawaiBanbox;
	private AmbilDataPegawaiBanbox searchPegawai;
	private Combobox searchstatus;

	public Pegawai pegawai;
	private KenaikanPangkat kenaikanPangkat;

	private Textbox namaPejabat;
	private Textbox nomorSuratkeputusan;
	private MyDatebox tanggalSuratkeputusan;

	private MyDatebox mulai;
	private MyDatebox sampai;

	private MyDatebox tanggalSuratUsul;
	private Textbox noSuratUsul;
	private Textbox keterangan;
	private Combobox peraturan;

	private MyCheckboxConfig kenaikanJabatan;
	private Combobox jenis;
	private Combobox jabatan;
	private Combobox jabatanFungsional;
	private Combobox jabatanStruktural;
	private MyCheckboxConfig menjabat;
	private MyCheckboxConfig status;
	private MyGrid gridFotoGambar;
	private Combobox jenisKenaikanPangkat;
	private AmbilDataGolonganBanbox golongan;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/** Menyiapkan helper untuk {@code pegawai} tertentu, atau untuk semua pegawai bila {@code null}. */
	public KenaikanPangkatHelper(final Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/** Renderer baris grid untuk {@link KenaikanPangkat}: jabatan (fungsional/struktural/umum, salah satu yang terisi), jenis kenaikan pangkat, nomor/tanggal surat usul, golongan/gaji pokok, peraturan, nomor/tanggal SK dan pejabat penandatangan, ditandai warna hijau muda bila mencerminkan jabatan yang sedang dijabat. */
	class KenaikanPangkatRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KenaikanPangkat kenaikanPangkat = (KenaikanPangkat) arg1;

			if (kenaikanPangkat.getMenjabat()) {
				arg0.setStyle("background-color: rgba(144,238,144,0.4);");
			}

			RevisiHelper
					.createNewRevisi(KenaikanPangkat.class, kenaikanPangkat, kenaikanPangkat.getPegawai().getNama())
					.setParent(arg0);

			String jabatan = "";
			if (kenaikanPangkat.getJabatanFungsional() != null) {
				jabatan = kenaikanPangkat.getJabatanFungsional().getNama();
			} else if (kenaikanPangkat.getJabatanStruktural() != null) {
				jabatan = kenaikanPangkat.getJabatanStruktural().getNama();
			} else if (kenaikanPangkat.getJabatan() != null) {
				jabatan = kenaikanPangkat.getJabatan().getNama();
			}

			new Label(jabatan).setParent(arg0);

			new Label(kenaikanPangkat.getJenisKenaikanPangkat() == null ? ""
					: kenaikanPangkat.getJenisKenaikanPangkat().getNama()).setParent(arg0);

			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">"
					+ ((kenaikanPangkat.getNoSuratUsul() == null ? "" : kenaikanPangkat.getNoSuratUsul()) + ""
							+ (kenaikanPangkat.getTanggalSuratUsul() == null ? ""
									: "<br>" + Common.dateFormat2.get().format(kenaikanPangkat.getTanggalSuratUsul())))
					+ "</font>").setParent(arg0);

			String gaji = (kenaikanPangkat.getGolongan() == null ? "" : kenaikanPangkat.getGolongan().toString());
			if (kenaikanPangkat.getGajiPokok() != null) {
				gaji = kenaikanPangkat.getGajiPokok().toString();
			}

			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">" + gaji + "</font>").setParent(arg0);

			new Label(kenaikanPangkat.getPeraturan() == null ? "" : kenaikanPangkat.getPeraturan().getNama())
					.setParent(arg0);

			new ais.ui.util.MyHtml(
					"<font style=\"font-size: x-small;\">" + ((kenaikanPangkat.getNomorSuratkeputusan() == null
							|| kenaikanPangkat.getNomorSuratkeputusan().trim().equals("")
									? ""
									: kenaikanPangkat.getNomorSuratkeputusan())
							+ ""
							+ (kenaikanPangkat.getTanggalSuratkeputusan() == null
									? ""
									: "<br>" + Common.dateFormat2.get().format(kenaikanPangkat.getTanggalSuratkeputusan()))
							+ (kenaikanPangkat.getNamaPejabat() == null
									|| kenaikanPangkat.getNamaPejabat().trim().equals("") ? ""
											: "<br>Pejabat: " + kenaikanPangkat.getNamaPejabat()))
							+ "</font>")
					.setParent(arg0);

			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">"
					+ (kenaikanPangkat.getMulai() == null ? "" : Common.dateFormat1.get().format(kenaikanPangkat.getMulai()))
					+ " s.d " + (kenaikanPangkat.getSampai() == null ? ""
							: Common.dateFormat1.get().format(kenaikanPangkat.getSampai()))
					+ "</font>").setParent(arg0);

			new Label(kenaikanPangkat.getMenjabat() ? "Ya" : "Tidak").setParent(arg0);

			new Image(kenaikanPangkat.getStatus() ? "/img/svg/check2.svg" : "/img/svg/warning-outline.svg")
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kenaikanPangkat);

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(!kenaikanPangkat.getStatus());
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
											Common.refreshDelete((kenaikanPangkat));
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
			toolbar.setParent(arg0);
		}
	}

	/** Membangun kerangka layar daftar kenaikan pangkat: panel filter (pegawai, status, satuan kerja) di utara dan grid rincian di tengah, lalu langsung memuat datanya. */
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
				init(new KenaikanPangkat());
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
		column.setLabel("Jabatan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Usul");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Golongan/Gaji");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Peraturan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Surat Keputusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mulai/Sampai");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Menjabat");
		column.setWidth("5%");

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
	/** Memuat ulang daftar {@link KenaikanPangkat} sesuai filter aktif ke grid. */
	public void onSearchDefault(Event event) {
		
		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		List<KenaikanPangkat> kenaikanPangkat = session.createCriteria(KenaikanPangkat.class)
				
				.createAlias("pegawai", "pegawai")
				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("pegawai.satuanKerja", satuanKerjas))
				
				.addOrder(Order.desc("tanggalSuratkeputusan")).addOrder(Order.desc("tanggalSuratUsul"))
				.addOrder(Order.asc("pegawai"))
				.add(searchPegawai.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pegawai", searchPegawai.getAttribute("pegawai")))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))

				.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(kenaikanPangkat);

		grid.setRowRenderer(new KenaikanPangkatRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** Membangun form tambah/ubah untuk {@code kenaikanPangkat} (baru atau sudah ada): jenis kenaikan, jabatan (fungsional/struktural/umum), golongan/gaji pokok, peraturan dasar, data surat usul dan SK, rentang berlaku, serta lampiran dokumen, menggantikan tampilan daftar sementara. */
	public void init(final KenaikanPangkat kenaikanPangkat) throws Exception {
		this.kenaikanPangkat = kenaikanPangkat;

		South south = new South();
		Toolbar toolbar = new Toolbar();
		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		MyToolbarbuttonConfig kembali = new MyToolbarbuttonConfig("Kembali", "/img/cancel.gif");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		final MyWindow window = new MyWindow("Pendataan Kepangkatan / Golongan", "none", true);
		window.setWidth("90%");
		window.setHeight("97%");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.appendChild(borderlayout);

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("60%");

		east.appendChild(new FotoLampiranPegawaiHelper(gridFotoGambar = new MyGrid()).initDetail(kenaikanPangkat,
				KenaikanPangkat.class));

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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		final Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
		row.appendChild(ambilDataPegawaiBanbox = new AmbilDataPegawaiBanbox(true));
		ambilDataPegawaiBanbox
				.setValue(kenaikanPangkat.getPegawai() == null ? "" : kenaikanPangkat.getPegawai().getNama());
		ambilDataPegawaiBanbox.setAttribute("pegawai", kenaikanPangkat.getPegawai());
		ambilDataPegawaiBanbox.setWidth("90%");

		if (this.pegawai != null) {
			ambilDataPegawaiBanbox.setAttribute("pegawai", pegawai);
			ambilDataPegawaiBanbox.setValue(pegawai.toString());
			ambilDataPegawaiBanbox.setDisabled(!Common.getApakahAdmin());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(
				kenaikanJabatan = new MyCheckboxConfig("Merupakan perubahan jabatan fungsional atau struktural"));
		kenaikanJabatan.setChecked(kenaikanPangkat.getKenaikanJabatan());

		final MyFormRow jenisjabatanrow = new MyFormRow();
		jenisjabatanrow.setVisible(false);
		jenisjabatanrow.setParent(rows);
		jenisjabatanrow.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Jabatan")));
		jenis = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Pegawai.JENIS_STRUKTURAL);
		comboitem.setValue(Pegawai.JENIS_STRUKTURAL);
		jenis.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Pegawai.JENIS_FUNGSIONAL);
		comboitem.setValue(Pegawai.JENIS_FUNGSIONAL);
		jenis.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Pegawai.JENIS_HONORER);
		comboitem.setValue(Pegawai.JENIS_HONORER);
		jenis.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Pegawai.JENIS_OUTSOURCHING);
		comboitem.setValue(Pegawai.JENIS_OUTSOURCHING);
		jenis.appendChild(comboitem);
		Common.selectComboItem(jenis, kenaikanPangkat.getJenis());
		jenisjabatanrow.appendChild(jenis);
		jenis.setWidth("90%");
		jenis.setReadonly(true);

		final MyFormRow jabatanrow = new MyFormRow();
		jabatanrow.setVisible(false);
		jabatanrow.setParent(rows);
		jabatanrow.appendChild(new MyLabelConfig("Jabatan"));
		Common.insertCombo(jabatan = new Combobox(), "nama", Jabatan.class);
		Common.selectComboItem(jabatan, kenaikanPangkat.getJabatan());
		jabatanrow.appendChild(jabatan);
		jabatan.setWidth("90%");
		jabatan.setReadonly(true);

		final MyFormRow jabatanfungsionalrow = new MyFormRow();
		jabatanfungsionalrow.setVisible(false);
		jabatanfungsionalrow.setParent(rows);
		jabatanfungsionalrow.appendChild(new MyLabelConfig("Jabatan Fungsional"));
		Common.insertComboDanSemua(jabatanFungsional = new Combobox(), new String[] { "kode", "nama" }, "keterangan",
				JabatanFungsional.class, "=Jabatan Fungsional=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jabatanFungsional, kenaikanPangkat.getJabatanFungsional());
		jabatanfungsionalrow.appendChild(jabatanFungsional);
		jabatanFungsional.setWidth("90%");
		jabatanFungsional.setReadonly(true);

		final MyFormRow jabatanstrukturalrow = new MyFormRow();
		jabatanstrukturalrow.setVisible(false);
		jabatanstrukturalrow.setParent(rows);
		jabatanstrukturalrow.appendChild(new MyLabelConfig("Jabatan Struktural"));
		Common.insertComboDanSemua(jabatanStruktural = new Combobox(), new String[] { "kode", "nama" }, "keterangan",
				JabatanStruktural.class, "=Jabatan Struktural=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jabatanStruktural, kenaikanPangkat.getJabatanStruktural());
		jabatanstrukturalrow.appendChild(jabatanStruktural);
		jabatanStruktural.setWidth("90%");
		jabatanStruktural.setReadonly(true);

		final EventListener jabatanEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				jabatanrow.setVisible(false);
				jabatanfungsionalrow.setVisible(false);
				jabatanstrukturalrow.setVisible(false);
				String myjenis = (String) (jenis.getSelectedItem() == null ? null : jenis.getSelectedItem().getValue());

				if (myjenis != null) {
					if (myjenis.equals(Pegawai.JENIS_FUNGSIONAL)) {
						jabatanfungsionalrow.setVisible(true);
					} else if (myjenis.equals(Pegawai.JENIS_STRUKTURAL)) {
						jabatanstrukturalrow.setVisible(true);
					} else {
						jabatanrow.setVisible(true);
					}
				}
			}
		};

		jenis.addEventListener("onChange", jabatanEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kenaikan Pangkat"));
		row.appendChild(jenisKenaikanPangkat = new Combobox());
		jenisKenaikanPangkat.setWidth("90%");
		Common.insertCombo(jenisKenaikanPangkat, "nama", JenisKenaikanPangkat.class);
		jenisKenaikanPangkat.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No Surat Usul"));
		row.appendChild(noSuratUsul = new Textbox(
				kenaikanPangkat.getNoSuratUsul() == null ? "" : kenaikanPangkat.getNoSuratUsul()));
		noSuratUsul.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Surat Usul"));
		row.appendChild(tanggalSuratUsul = new MyDatebox(
				kenaikanPangkat.getTanggalSuratUsul() == null ? ais.ui.util.WaktuUtil.getDate()
						: kenaikanPangkat.getTanggalSuratUsul()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peraturan"));
		row.appendChild(peraturan = new Combobox());
		Common.insertComboDanSemua(peraturan, new String[] { "nama", "kode" }, "keterangan", Peraturan.class,
				"== Tanpa Peraturan ==", Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(peraturan, kenaikanPangkat.getPeraturan());
		peraturan.setWidth("90%");

		final MyFormRow rowFile = new MyFormRow();

		rowFile.setParent(rows);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowFile);
				rowFile.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Dokumen Peraturan"));
				rowFile.setVisible(false);
				Peraturan jp = (Peraturan) (peraturan.getSelectedItem() == null ? null
						: peraturan.getSelectedItem().getValue());
				if (jp != null) {

					FileFotoLain fileFotoLain = FileFotoLain.ambil(false, jp.getId(), Peraturan.class.getName(),
							LampiranLain.class);

					rowFile.setVisible(fileFotoLain != null);
					Vbox myvbox = new Vbox();
					myvbox.setParent(rowFile);

					Hbox hbox = new Hbox();
					hbox.setParent(myvbox);
					LampiranLain.createDownloadUploadFileLain(hbox, jp.getId(), Peraturan.class.getName(),
							"Peraturan Dokumen", false, null, null, false, false, false, false);
				}
			}
		};
		peraturan.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Golongan"));
		row.appendChild(golongan = new AmbilDataGolonganBanbox());
		golongan.setValue(kenaikanPangkat.getGolongan()==null?"":kenaikanPangkat.getGolongan().getNama()); 
		golongan.setAttribute("golongan", kenaikanPangkat.getGolongan());
		golongan.setWidth("90%");
		golongan.setReadonly(true);


		EventListener jenisjabatanEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				jabatanrow.setVisible(false);
				jabatanfungsionalrow.setVisible(false);
				jabatanstrukturalrow.setVisible(false);
				jenisjabatanrow.setVisible(kenaikanJabatan.isChecked());
				if (kenaikanJabatan.isChecked()) {
					jabatanEventListener.onEvent(arg0);
				}
			}
		};

		kenaikanJabatan.addEventListener("onCheck", jenisjabatanEventListener);
		jenisjabatanEventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No Surat Keputusan"));
		row.appendChild(nomorSuratkeputusan = new Textbox(kenaikanPangkat.getNomorSuratkeputusan()));
		nomorSuratkeputusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Surat Keputusan"));
		row.appendChild(tanggalSuratkeputusan = new MyDatebox(kenaikanPangkat.getTanggalSuratkeputusan()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai menjabat"));
		row.appendChild(mulai = new MyDatebox(kenaikanPangkat.getMulai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai menjabat"));
		row.appendChild(sampai = new MyDatebox(kenaikanPangkat.getSampai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(menjabat = new MyCheckboxConfig("Jabatan atau golongan ini sedang aktif / dijabat"));
		menjabat.setChecked(kenaikanPangkat.getMenjabat());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Pejabat"));
		row.appendChild(namaPejabat = new Textbox(kenaikanPangkat.getNamaPejabat()));
		namaPejabat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kenaikanPangkat.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		if (kenaikanPangkat.getStatus()) {
			Common.freeze(grid, true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));
		row.appendChild(status = new MyCheckboxConfig());
		row.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE));
		status.setChecked(kenaikanPangkat.getStatus());
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
	/** Memvalidasi dan menyimpan data {@link KenaikanPangkat} dari form; mengembalikan {@code false} bila validasi gagal (pesan sudah ditampilkan ke pengguna), {@code true} bila berhasil disimpan. */
	public boolean save(Event event) throws Exception {

		if (ambilDataPegawaiBanbox.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Mohon maaf, Pegawai belum dipilih. Langkah yang dapat dilakukan: (1) cari dan pilih Pegawai menggunakan kolom pencarian; (2) pastikan data pegawai sudah terdaftar di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

//		if (peraturan.getSelectedItem() == null) {
//			MyMessageboxConfig.show("Peraturan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}

//		if (mulai.getValue() == null) {
//			MyMessageboxConfig.show("Mulai menjabat harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}

		List<Row> rowsDocument = gridFotoGambar.getRows().getChildren();
		for (Row row : rowsDocument) {
			FotoLampiranPegawai fotoLampiranPegawai = (FotoLampiranPegawai) row.getAttribute("fotoLampiranPegawai");
			if (fotoLampiranPegawai.getItem() == null) {
				MyMessageboxConfig.show("Mohon maaf, File lampiran belum diunggah. Langkah yang dapat dilakukan: (1) klik tombol unggah dan pilih file dokumen yang sesuai; (2) pastikan file dalam format yang didukung; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (kenaikanPangkat.getId() != null) {
			kenaikanPangkat = (KenaikanPangkat) session.load(KenaikanPangkat.class, kenaikanPangkat.getId());
		}

		kenaikanPangkat
				.setJenisKenaikanPangkat((JenisKenaikanPangkat) (jenisKenaikanPangkat.getSelectedItem() == null ? null
						: jenisKenaikanPangkat.getSelectedItem().getValue()));

		kenaikanPangkat.setMulai(mulai.getValue());
		kenaikanPangkat.setSampai(sampai.getValue());

		kenaikanPangkat.setStatus(status.isChecked());
		kenaikanPangkat.setMenjabat(menjabat.isChecked());
		kenaikanPangkat
				.setJenis((String) (jenis.getSelectedItem() == null ? null : jenis.getSelectedItem().getValue()));
		kenaikanPangkat.setJabatanFungsional((JabatanFungsional) (kenaikanPangkat.getJenis() != null
				&& kenaikanPangkat.getJenis().equals(Pegawai.JENIS_FUNGSIONAL)
						? jabatanFungsional.getSelectedItem() == null ? null
								: jabatanFungsional.getSelectedItem().getValue()
						: null));
		kenaikanPangkat.setJabatanStruktural((JabatanStruktural) (kenaikanPangkat.getJenis() != null
				&& kenaikanPangkat.getJenis().equals(Pegawai.JENIS_STRUKTURAL)
						? jabatanStruktural.getSelectedItem() == null ? null
								: jabatanStruktural.getSelectedItem().getValue()
						: null));

		kenaikanPangkat.setJabatan((Jabatan) (kenaikanPangkat.getJenis() != null
				&& !kenaikanPangkat.getJenis().equals(Pegawai.JENIS_STRUKTURAL)
				&& !kenaikanPangkat.getJenis().equals(Pegawai.JENIS_FUNGSIONAL)
						? jabatan.getSelectedItem() == null ? null : jabatan.getSelectedItem().getValue()
						: null));

		kenaikanPangkat.setKenaikanJabatan(kenaikanJabatan.isChecked());

		kenaikanPangkat.setPeraturan(
				(Peraturan) (peraturan.getSelectedItem() == null ? null : peraturan.getSelectedItem().getValue()));
		kenaikanPangkat.setPegawai((Pegawai) ambilDataPegawaiBanbox.getAttribute("pegawai"));
		kenaikanPangkat.setNomorSuratkeputusan(nomorSuratkeputusan.getValue());
		kenaikanPangkat.setTanggalSuratkeputusan(tanggalSuratkeputusan.getValue());
		kenaikanPangkat.setNamaPejabat(namaPejabat.getValue());
		kenaikanPangkat.setKeterangan(keterangan.getValue());
		kenaikanPangkat.setTanggalSuratUsul(tanggalSuratUsul.getValue());
		kenaikanPangkat.setNoSuratUsul(noSuratUsul.getValue());
		kenaikanPangkat.setGolongan((Golongan) (golongan.getAttribute("golongan")));

		if (kenaikanPangkat.getId() != null) {
			session.update(kenaikanPangkat);
		} else {
			session.save(kenaikanPangkat);
		}

		Session mysession = StreamingHibernateUtil.getInstance().currentSession();
		try {
			mysession.getTransaction().begin();
			for (Row row : rowsDocument) {
				FotoLampiranPegawai fotoLampiranPegawai = (FotoLampiranPegawai) row.getAttribute("fotoLampiranPegawai");
				fotoLampiranPegawai.setItem(kenaikanPangkat.getId());
				fotoLampiranPegawai.setClazz(KenaikanPangkat.class.getName());
				mysession.saveOrUpdate(fotoLampiranPegawai);
			}
			mysession.getTransaction().commit();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		StreamingHibernateUtil.getInstance().closeSession();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				Pegawai pegawai = kenaikanPangkat.getPegawai();
				session.refresh(pegawai);
				Common.refreshUpdate(session, pegawai, true);
			}
		});

		return true;
	}
}
