package ais.action.master;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataBuktiPembayaranBanyak;
import ais.action.report.CommonReportHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BuktiPembayaran;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisPembayaran;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class BuktiPembayaranAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataMahasiswaBanbox searchmahasiswa;
	private MyDatebox searchmulai;
	private MyDatebox searchsampai;
	private Combobox searchJenisKegiatan;
	private Combobox searchjurusan;

	private MyCheckboxConfig belum;
	private MyCheckboxConfig sudah;

	private Textbox nama;
	private MyDatebox tanggal;
	private AmbilDataMahasiswaBanbox mahasiswa;
	private Combobox jenisKegiatan;
	private Combobox jenisPembayaran;
	private Combobox semester;
	private MyDoublebox nilai;
	// private AmbilDataCalonMahasiswaBanbox biodataCalonMahasiswa;
	private Textbox keterangan;

	private Mahasiswa dataMahasiswa = null;

	private boolean edit = false;
	private boolean delete = false;

	private BuktiPembayaran buktiPembayaran;
	private MyToolbarbuttonConfig add;
	protected LampiranLain lainMahasiswa;
	// private Combobox itemBiaya;
	private MyGrid gripRincianBiaya;
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private EventListener eventListener;

	private Tbmuser tbmuser;

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

		tbmuser = Common.getCurrentUser();
		dataMahasiswa = tbmuser.getMahasiswa();

		searchmahasiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		Common.insertComboDanSemua(searchJenisKegiatan, "namaKegiatan", JenisKegiatan.class);
		// FIX NPE: Jurusan TIDAK punya properti Hibernate-mapped "keterangan"
		// (lihat perbaikan sama di CalonSiswaAction.java/SiswaAction.java) --
		// deskripsi "" aman (fallback ke toString() entity).
		Common.insertComboDanSemua(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "", Jurusan.class,
				Restrictions.eq("aktif", true));

		if (tbmuser != null && tbmuser.ambilJurusan() != null) {
			Common.selectComboItem(true, searchjurusan, tbmuser.ambilJurusan());
			searchjurusan.setDisabled(true);
		}

		// add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		if (add != null) { add.setTooltiptext("Tambah"); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "nama", "tanggal", "mahasiswa", "biodataCalonMahasiswa",
				"jenisKegiatan", "itemBiaya", "semester", "nilai", "cicilanPembayaran", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(BuktiPembayaran.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		if (dataMahasiswa == null) {
			MyToolbarbuttonConfig upload = Common.uploadData(this, BuktiPembayaran.class, contents);
			upload.setVisible((add != null && add.isVisible()) && edit && delete);
			Common.appendKeToolbar(upload, add, comp);
		}
	        FilterLanjutHelper.setup(comp);
}

	class BuktiPembayaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BuktiPembayaran buktiPembayaran = (BuktiPembayaran) arg1;

			new Label(buktiPembayaran.getMahasiswa() == null
					? (buktiPembayaran.getBiodataCalonMahasiswa() == null ? "-"
							: buktiPembayaran.getBiodataCalonMahasiswa().toString())
					: buktiPembayaran.getMahasiswa().getNim() + "-" + buktiPembayaran.getMahasiswa().getNama())
					.setParent(arg0);

			Vbox vbox = RevisiHelper.createNewRevisi(BuktiPembayaran.class, buktiPembayaran, buktiPembayaran.getNama());
			vbox.setParent(arg0);

			Vbox myvbox = new Vbox();
			myvbox.setParent(vbox);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, buktiPembayaran.getIdLampiran(),
					BuktiPembayaran.class.getName(), "Bukti Pembayaran", true, null, null, false, false, true, false);

			myvbox = new Vbox();
			myvbox.setParent(vbox);

			new Label(Common.dateFormat6.get().format(buktiPembayaran.getTanggal())).setParent(arg0);

			new Label(buktiPembayaran.getJenisKegiatan().getNamaKegiatan()).setParent(arg0);
			new Label(buktiPembayaran.getItemBiaya() == null ? "" : buktiPembayaran.getItemBiaya().getNama())
					.setParent(arg0);
			new Label(
					buktiPembayaran.getJenisPembayaran() == null ? "" : buktiPembayaran.getJenisPembayaran().getNama())
					.setParent(arg0);
			new Label(buktiPembayaran.getSemester() + "").setParent(arg0);
			new Label(Common.numberFormat.get().format(buktiPembayaran.getNilai())).setParent(arg0);
			new Label(buktiPembayaran.getKeterangan()).setParent(arg0);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dikumpulkan lalu dibungkus
			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			if (buktiPembayaran.getCicilanPembayaran() == null) {

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Validasi",
						"/img/Actions-view-calendar-tasks-icon.png");
				button.setTooltiptext("Ubah Data");
				button.setVisible(edit && dataMahasiswa == null);
				button.setOrient("vertical");
				button.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Mahasiswa mhs = buktiPembayaran.getMahasiswa();
						BiodataCalonMahasiswa bio = buktiPembayaran.getBiodataCalonMahasiswa();
						if (mhs != null) {
							Common.displayWindow(
									"/common/daftarulang_mahasiswa_lama.zul?buktiPembayaran=" + buktiPembayaran.getId(),
									true, "95%", "95%", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											onSearchDefault(arg0);
										}
									}, "Validasi Pembayaran", true);
						} else if (bio != null) {

							if (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null && buktiPembayaran.getJenisKegiatan()
									.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
								Common.displayWindow("/pages/master/daftarulang_mahasiswa_calon.zul?buktiPembayaran="
										+ buktiPembayaran.getId(), true, "95%", "95%", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												onSearchDefault(arg0);
											}
										}, "Validasi Pembayaran", true);
							} else {

								Common.displayWindow("/common/daftarulang_mahasiswa_baru.zul?buktiPembayaran="
										+ buktiPembayaran.getId(), true, "95%", "95%", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												onSearchDefault(arg0);
											}
										}, "Validasi Pembayaran", true);
							}
						}
					}
				});
				aksiButtons.add(button);

				button = new MyToolbarbuttonConfig("Edit", "/img/svg/edit-box-line.svg");
				button.setTooltiptext("Ubah Data");
				button.setVisible(edit);
				button.setOrient("vertical");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						init(buktiPembayaran, null, null, null);
						addWindow.setVisible(true);
						addWindow.onModal();
					}

				});
				aksiButtons.add(button);

				button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.setVisible(delete);
				button.setOrient("vertical");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show(
								"Apakah Bapak/Ibu yakin ingin menghapus data bukti pembayaran ini? Data yang telah dihapus tidak dapat dikembalikan lagi. Silakan tekan OK untuk melanjutkan penghapusan, atau Batal untuk membatalkan.",
								"Konfirmasi Penghapusan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												Common.refreshDelete(buktiPembayaran);
												onSearchDefault(event);
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.showFormat(
														"Mohon maaf, data bukti pembayaran ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) pastikan tidak ada data pembayaran atau kegiatan yang masih menggunakan bukti ini; (2) hapus terlebih dahulu data yang berkaitan; (3) apabila masih berlanjut, mohon hubungi Administrator sistem.",
														"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
														e.getMessage());
											}

										}

									}
								});

					}
				});
				aksiButtons.add(button);
			} else {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
				button.setTooltiptext("Cetak");
				button.setOrient("vertical");
				button.addEventListener("onClick", new EventListener() {
					@SuppressWarnings({})
					@Override
					public void onEvent(Event event) throws Exception {
						Kegiatan kegiatan = buktiPembayaran.getCicilanPembayaran().getKegiatan();
						if (kegiatan.getMahasiswa() != null) {
							CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, false);
						} else {
							CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, false);
						}
					}
				});
				aksiButtons.add(button);
			}
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public static void onAddExternal(EventListener eventListener, BuktiPembayaran buktiPembayaran,
			BiodataCalonMahasiswa biodataCalonMahasiswa, LampiranLain lainMahasiswa,
			JenisKegiatan jenisKegiatanPembayaran) throws Exception {
		BuktiPembayaranAction buktiPembayaranAction = new BuktiPembayaranAction();
		buktiPembayaranAction.eventListener = eventListener;
		buktiPembayaranAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(buktiPembayaranAction.addWindow);

		buktiPembayaranAction.init(buktiPembayaran, biodataCalonMahasiswa, lainMahasiswa, jenisKegiatanPembayaran);

		buktiPembayaranAction.addWindow.setHeight("95%");
		buktiPembayaranAction.addWindow.setWidth("850px");
		buktiPembayaranAction.addWindow.setVisible(true);
		buktiPembayaranAction.addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new BuktiPembayaran(), null, null, null);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final BuktiPembayaran buktiPembayaran, final BiodataCalonMahasiswa biodataCalonMahasiswa,
			final LampiranLain lainMahasiswa, final JenisKegiatan jenisKegiatanPembayaran) throws Exception {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		this.lainMahasiswa = lainMahasiswa;
		this.buktiPembayaran = buktiPembayaran;
		addWindow.setTitle(buktiPembayaran.getId() == null ? "Tambah Bukti Pembayaran" : "Ubah Bukti Pembayaran");
		Common.clear(addWindow);

		if (buktiPembayaran.getId() == null) {
			addWindow.setHeight("95%");
		} else {
			addWindow.setHeight("440px");
		}

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
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal bayar *"));
		row.appendChild(tanggal = new MyDatebox(buktiPembayaran.getTanggal()));
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Bukti Pembayaran *"));
		row.appendChild(nama = new Textbox(buktiPembayaran.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);
		nama.setMaxlength(255);

		Common.initKeterangan(rows,
				"* Contoh : Bukti pembayaran untuk membayar biaya .... dengan cara ... senilai ...");

		if (biodataCalonMahasiswa == null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_mahasiswa") + " *"));
			row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());

			if (dataMahasiswa != null) {
				mahasiswa.setValue(
						dataMahasiswa == null ? "" : (dataMahasiswa.getNim() + " - " + dataMahasiswa.getNama()));
				mahasiswa.setId("" + dataMahasiswa == null ? "mhs_-1" : "mhs_" + dataMahasiswa.getId());
				mahasiswa.setAttribute("mahasiswa", dataMahasiswa);
				mahasiswa.setDisabled(true);
			} else {

				mahasiswa.setValue(buktiPembayaran.getMahasiswa() == null ? ""
						: (buktiPembayaran.getMahasiswa().getNim() + " - " + buktiPembayaran.getMahasiswa().getNama()));
				mahasiswa.setId(
						"" + buktiPembayaran.getMahasiswa() == null ? "mhs_-1" : "mhs_" + buktiPembayaran.getId());
				mahasiswa.setAttribute("mahasiswa", buktiPembayaran.getMahasiswa());
			}
			mahasiswa.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
			row.appendChild(semester = new Combobox());
			int maxSemesterPilihan = 25;
			try {
				maxSemesterPilihan = Integer
						.parseInt(Common.getKonfigurasi("max_semester_pilihan", "25").getNilai().trim());
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			for (int i = 1; i < maxSemesterPilihan; i++) {
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(i + "");
				comboitem.setValue(i);
				semester.appendChild(comboitem);
			}

			Common.selectComboItem(semester, buktiPembayaran.getSemester());
			semester.setWidth("90%");
			semester.setReadonly(true);

		}

		final EventListener itemBiayaEventListener = new EventListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(gripRincianBiaya);

				JenisKegiatan jk = (JenisKegiatan) (jenisKegiatan.getSelectedItem() == null ? null
						: jenisKegiatan.getSelectedItem().getValue());
				Mahasiswa mhs = (Mahasiswa) (mahasiswa == null ? null : mahasiswa.getAttribute("mahasiswa"));

				if ((mhs != null || biodataCalonMahasiswa != null) && jk != null) {

					Columns columns = new Columns();
					columns.setParent(gripRincianBiaya);

					MyColumnConfig column = new MyColumnConfig("Item Biaya");
					column.setParent(columns);

					column = new MyColumnConfig("Tagihan");
					column.setParent(columns);
					column.setWidth("15%");
					column.setAlign("right");

					column = new MyColumnConfig("Dibayar");
					column.setParent(columns);
					column.setWidth("15%");
					column.setAlign("right");

					column = new MyColumnConfig("Telah Dibayar");
					column.setParent(columns);
					column.setWidth("18%");
					column.setAlign("right");

					column = new MyColumnConfig("Belum Dibayar");
					column.setParent(columns);
					column.setWidth("18%");
					column.setAlign("right");

					// Kegiatan kegiatan = mhs == null
					// ?
					// PembayaranUtil.getInstance().checkKegiatanCalonMahasiswa(biodataCalonMahasiswa,
					// jk,
					// (Integer) (semester == null ||
					// semester.getSelectedItem() == null ? null
					// : semester.getSelectedItem().getValue()),
					// false)
					// : PembayaranUtil.getInstance()
					// .checkKegiatanMahasiswa(
					// mhs, (Integer) (semester == null ||
					// semester.getSelectedItem() == null
					// ? null : semester.getSelectedItem().getValue()),
					// jk, null, false);

					Kegiatan kegiatan = mhs == null
							? biodataCalonMahasiswa.ambilKegiatans(
									(Integer) (semester == null || semester.getSelectedItem() == null ? null
											: semester.getSelectedItem().getValue()),
									jk)
							: mhs.ambilKegiatans(
									(Integer) (semester == null || semester.getSelectedItem() == null ? null
											: semester.getSelectedItem().getValue()),
									jk);

					if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getPembayaranDaftarUlang() != null
							&& jk != null && ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
							&& jk.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) {
						kegiatan = biodataCalonMahasiswa.getPembayaranDaftarUlang();
					} else if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getPembayaranRegistrasi() != null
							&& jk != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
							&& jk.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
						kegiatan = biodataCalonMahasiswa.getPembayaranRegistrasi();
					}

					Jurusan jurusan = null;
					if (biodataCalonMahasiswa != null) {
						jurusan = biodataCalonMahasiswa.getProdiLulus();
						if (jurusan == null || jurusan.getId() == null) {
							jurusan = biodataCalonMahasiswa.getProdi1() == null ? biodataCalonMahasiswa.getProdi2()
									: biodataCalonMahasiswa.getProdi1();
						}
					}

					PembayaranUtil.getInstance();
					PembayaranUtil.getInstance();
					Collection detailBiayas = mhs == null
							? PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jk,
									jurusan,
									(Integer) (semester == null || semester.getSelectedItem() == null ? null
											: semester.getSelectedItem().getValue()),
									true)
							: PembayaranUtilHelper.getDetailBiayaMahasiswa(mhs,
									(Integer) (semester == null || semester.getSelectedItem() == null ? null
											: semester.getSelectedItem().getValue()),
									jk, true);

					if (detailBiayas.isEmpty()) {
						// MyMessageboxConfig.show("Tagihan atau item biaya
						// tidak ditemukan", "Pemberitahuan",
						// MyMessageboxConfig.OK,
						// MyMessageboxConfig.INFORMATION, new
						// EventListener() {
						//
						// @Override
						// public void onEvent(Event arg0) throws Exception
						// {
						// Common.clear(gripRincianBiaya);
						// }
						// });

						return;
					}

					Session session = HibernateUtil.currentSession();

					if (mhs != null) {
						Integer smt = (Integer) (semester == null || semester.getSelectedItem() == null ? null
								: semester.getSelectedItem().getValue());
						int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, mhs, jk, smt,
								detailBiayas, false, false);
						detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mhs, smt, jk,
								countPengaturanBulanan > 0 ? "-1" : null, true);
					}

					final Rows rows = new Rows();
					gripRincianBiaya.appendChild(rows);

					final MyLabelBold totalNilai = new MyLabelBold();

					EventListener eventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Row> myRows = rows.getChildren();
							Double tot = 0.0;
							for (Row myRow : myRows) {
								if (myRow.getChildren().get(2) instanceof MyDoublebox) {
									MyDoublebox nilai = (MyDoublebox) myRow.getChildren().get(2);
									tot += nilai.getValue() == null ? 0.0 : nilai.getValue();
								}
							}
							totalNilai.setValue(Common.numberFormat.get().format(tot));
						}
					};

					Double totalTagihan = 0.0;
					Double totalDibayar = 0.0;
					Double totalBelumDibayar = 0.0;

					List<Object[]> dataCicilan = kegiatan == null || kegiatan.getId() == null
							|| kegiatan.getAmount() < 0.01
									? new ArrayList<Object[]>()
									: session.createCriteria(CicilanPembayaran.class)
											.createAlias("pengaturanPembayaranBulanan", "pengaturanPembayaranBulanan",
													Criteria.LEFT_JOIN)
											.setProjection(Projections.projectionList().add(Projections.property("id"))
													.add(Projections.property("itemBiaya.id"))
													.add(Projections.property("pengaturanPembayaranBulanan.realBulan"))
													.add(Projections.property("nilai"))
													.add(Projections.property("kegiatan.id")))

											.add(Restrictions.eq("kegiatan", kegiatan)).list();

					for (Object o : detailBiayas) {
						if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
							DetailBiaya detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
							Double jumlah = pengaturanPembayaranBulanan.ambilNominalModifikasi(mhs,
									(Integer) (semester == null || semester.getSelectedItem() == null ? null
											: semester.getSelectedItem().getValue()));

							Double telahDibayar = 0.0;
							for (Object[] oo : dataCicilan) {
								try {
									if (pengaturanPembayaranBulanan.getRealBulan()
											.equals(Integer.parseInt(oo[2].toString()))
											&& detailBiaya.getItemBiaya().getId()
													.equals(Long.parseLong(oo[1].toString()))
											&& kegiatan.getId().equals(Long.parseLong(oo[4].toString()))) {
										telahDibayar += Double.parseDouble(oo[3].toString());
									}
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
							}

							if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
									.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
								jumlah = -Math.abs(jumlah);
								telahDibayar = (telahDibayar == null ? 0.0 : -Math.abs(telahDibayar.doubleValue()));
							}

							totalTagihan += jumlah;

							totalDibayar += (telahDibayar == null ? 0.0 : telahDibayar.doubleValue());

							Double belumDibayar = jumlah - (telahDibayar == null ? 0.0 : telahDibayar.doubleValue());

							totalBelumDibayar += belumDibayar;

							// int tot = (int) (jumlah.intValue()
							// + (telahDibayar == null ? 0.0 :
							// telahDibayar.doubleValue()));
							// if (tot == 0) {
							// continue;
							// }

							MyFormRow row = new MyFormRow();row.setValign("top");
							row.setValign("top");row.setAttribute("pengaturanPembayaranBulanan", pengaturanPembayaranBulanan);
							row.setValign("top");row.setAttribute("itemBiaya", pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya());
							row.setValign("top");row.setAttribute("detailBiaya", pengaturanPembayaranBulanan.getDetailBiaya());
							row.setParent(rows);
							new Label(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + ", Bulan "
									+ pengaturanPembayaranBulanan.getNamaBulan()).setParent(row);
							row.appendChild(new Label(jumlah == null ? "" : Common.numberFormat.get().format(jumlah)));

							MyDoublebox nilai = new MyDoublebox();
							nilai.setWidth("90%");
							row.appendChild(nilai);
							nilai.addEventListener("onChange", eventListener);

							row.appendChild(new MyLabelBoldAja(telahDibayar == null ? ""
									: Common.numberFormat.get().format(telahDibayar.doubleValue())));

							row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(belumDibayar)));

						} else if (o instanceof DetailBiaya) {
							DetailBiaya detailBiaya = (DetailBiaya) o;
							ItemBiaya itemBiaya = detailBiaya.getItemBiaya();

							DetailKegiatan detailKegiatan = !detailBiaya.getItemBiaya().getNilaiBisaDiubah() ? null
									: kegiatan == null ? null
											: (DetailKegiatan) session.createCriteria(DetailKegiatan.class)
													.add(Restrictions.eq("detailBiaya", detailBiaya))
													.add(Restrictions.eq("kegiatan", kegiatan)).setMaxResults(1)
													.uniqueResult();

							Double jumlah = detailBiaya.getItemBiaya().getPenghitungan()
									.equals(ItemBiaya.HITUNG_TUNGGAKAN_SMT_LALU)
											? detailBiaya.getTunggakanLalu()
											: (detailKegiatan != null ? detailKegiatan.getBiaya()
													: (detailBiaya.getNilaiBiayaBaru() == null
															? detailBiaya.getNilaiBiaya()
															: detailBiaya.getNilaiBiayaBaru()));

							Double telahDibayar = 0.0;
							for (Object[] oo : dataCicilan) {
								try {
									if (itemBiaya.getId().equals(Long.parseLong(oo[1].toString()))) {
										telahDibayar += Double.parseDouble(oo[3].toString());
									}
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
							}

							if (detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
								jumlah = -Math.abs(jumlah);
								telahDibayar = (telahDibayar == null ? 0.0 : -Math.abs(telahDibayar.doubleValue()));
							}

							totalTagihan += jumlah;

							totalDibayar += (telahDibayar == null ? 0.0 : telahDibayar.doubleValue());

							Double belumDibayar = jumlah - (telahDibayar == null ? 0.0 : telahDibayar.doubleValue());

							totalBelumDibayar += belumDibayar;

							MyFormRow row = new MyFormRow();row.setValign("top");
							row.setValign("top");row.setAttribute("itemBiaya", itemBiaya);
							row.setValign("top");row.setAttribute("detailBiaya", detailBiaya);
							row.setParent(rows);
							new Label(detailBiaya.getKeterangan()).setParent(row);
							row.appendChild(new Label(jumlah == null ? "" : Common.numberFormat.get().format(jumlah)));

							MyDoublebox nilai = new MyDoublebox();
							nilai.setWidth("90%");
							row.appendChild(nilai);
							nilai.addEventListener("onChange", eventListener);

							row.appendChild(new MyLabelBoldAja(telahDibayar == null ? ""
									: Common.numberFormat.get().format(telahDibayar.doubleValue())));

							row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(belumDibayar)));
						}
					}

					Foot foot = new Foot();
					gripRincianBiaya.appendChild(foot);

					MyLabelBold total = new MyLabelBold("Total");
					Footer footer = new Footer();
					foot.appendChild(footer);
					footer.appendChild(total);

					total = new MyLabelBold(Common.numberFormat.get().format(totalTagihan));
					total.setStyle("text-align: right;");
					footer = new Footer();
					foot.appendChild(footer);
					footer.setAlign("right");
					footer.appendChild(total);

					footer = new Footer();
					foot.appendChild(footer);
					footer.setAlign("right");
					footer.appendChild(totalNilai);
					totalNilai.setStyle("text-align: right;");
					eventListener.onEvent(null);

					total = new MyLabelBold(Common.numberFormat.get().format(totalDibayar));
					total.setStyle("text-align: right;");
					footer = new Footer();
					foot.appendChild(footer);
					footer.setAlign("right");
					footer.appendChild(total);

					total = new MyLabelBold(Common.numberFormat.get().format(totalBelumDibayar));
					total.setStyle("text-align: right;");
					footer = new Footer();
					foot.appendChild(footer);
					footer.setAlign("right");
					footer.appendChild(total);

				}
			}

		};

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (biodataCalonMahasiswa == null) {
					if (mahasiswa != null) {
						Mahasiswa mhs = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");
						if (mhs != null) {
							Common.selectComboItem(semester, mhs.currentSemester());
						} else {
							semester.setSelectedIndex(-1);
						}
					}
				}

				itemBiayaEventListener.onEvent(null);
			}
		};

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran *"));
		row.appendChild(jenisKegiatan = new Combobox());
		Common.insertCombo(jenisKegiatan, "namaKegiatan", JenisKegiatan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (jenisKegiatanPembayaran != null) {
			Common.selectComboItem(jenisKegiatan, jenisKegiatanPembayaran);
			jenisKegiatan.setDisabled(true);
		} else {
			Common.selectComboItem(jenisKegiatan, buktiPembayaran.getJenisKegiatan());
		}
		jenisKegiatan.setWidth("90%");
		jenisKegiatan.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(buktiPembayaran.getId() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Biaya"));
		gripRincianBiaya = new MyGrid();
		gripRincianBiaya.setWidth("100%");
		row.appendChild(gripRincianBiaya);
		if (buktiPembayaran.getId() == null) {
			Common.initKeterangan(rows, "* Kosongkan rincian biaya jika tidak dibayar");
		}

		row = new MyFormRow();
		row.setVisible(buktiPembayaran.getId() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Item Biaya *"));
		row.appendChild(
				new Label(buktiPembayaran.getItemBiaya() == null ? "" : buktiPembayaran.getItemBiaya().getNama()));

		jenisKegiatan.addEventListener("onChange", itemBiayaEventListener);

		if (semester != null) {
			semester.addEventListener("onChange", itemBiayaEventListener);
		}

		if (mahasiswa != null) {
			mahasiswa.setEventListener(eventListener);
		}

		eventListener.onEvent(null);

		if (buktiPembayaran != null && buktiPembayaran.getJenisPembayaran() == null) {
			JenisPembayaran jenisPembayaranDefault = (JenisPembayaran) HibernateUtil.currentSession()
					.createCriteria(JenisPembayaran.class).add(Restrictions.eq("defaultPembayaran", true))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
					.uniqueResult();
			buktiPembayaran.setJenisPembayaran(jenisPembayaranDefault);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cara Pembayaran *"));
		row.appendChild(jenisPembayaran = new Combobox());
		SatuanKerja satuanKerja = Common.getSatuanKerja();
		Common.insertCombo(jenisPembayaran, "nama", "akun", JenisPembayaran.class,
				Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		Common.selectComboItem(jenisPembayaran, buktiPembayaran.getJenisPembayaran());
		jenisPembayaran.setWidth("90%");
		jenisPembayaran.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(buktiPembayaran.getId() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Pembayaran *"));
		row.appendChild(nilai = new MyDoublebox(buktiPembayaran.getNilai()));
		nilai.setWidth("90%");

		if (lainMahasiswa == null || lainMahasiswa.getId() == null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Scan / foto bukti pembayaran *"));
			Hbox hbox = new Hbox();
			if (buktiPembayaran.getId() == null) {
				LampiranLain.createDownloadUploadFileLain(hbox, buktiPembayaran.getId(),
						BuktiPembayaran.class.getName(), "Bukti Pembayaran", false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								BuktiPembayaranAction.this.lainMahasiswa = (LampiranLain) arg0.getData();
							}
						});
			} else {
				LampiranLain.createDownloadUploadFileLain(hbox, buktiPembayaran.getIdLampiran(),
						BuktiPembayaran.class.getName(), "Bukti Pembayaran", false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								BuktiPembayaranAction.this.lainMahasiswa = (LampiranLain) arg0.getData();
							}
						}, null, false, false, true, false);
			}
			hbox.setParent(row);
		} else {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Donwload Scan / foto bukti pembayaran *"));
			Hbox hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, lainMahasiswa.getId(), BuktiPembayaran.class.getName(),
					"Bukti Pembayaran", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							BuktiPembayaranAction.this.lainMahasiswa = (LampiranLain) arg0.getData();
						}
					}, null, false, false, true, false);
			hbox.setParent(row);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(buktiPembayaran.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setMaxlength(254);

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
					addWindow.setVisible(false);

				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu melengkapi Nama Bukti Pembayaran terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Bukti Pembayaran pada kolom yang tersedia; (2) periksa kembali kelengkapan data; (3) tekan tombol Simpan untuk melanjutkan.",
					"Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (jenisKegiatan.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu memilih Jenis Pembayaran terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Jenis Pembayaran yang sesuai pada kolom yang tersedia; (2) periksa kembali kelengkapan data; (3) tekan tombol Simpan untuk melanjutkan.",
					"Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (jenisPembayaran.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu memilih Cara Pembayaran terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Cara Pembayaran yang sesuai pada kolom yang tersedia; (2) periksa kembali kelengkapan data; (3) tekan tombol Simpan untuk melanjutkan.",
					"Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Mahasiswa mhs = null;
		if (biodataCalonMahasiswa == null) {
			mhs = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");
			if (mhs == null) {
				MyMessageboxConfig.show(
						"Mohon Bapak/Ibu memilih Mahasiswa terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Mahasiswa yang bersangkutan pada kolom yang tersedia; (2) periksa kembali kelengkapan data; (3) tekan tombol Simpan untuk melanjutkan.",
						"Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		try {

			if (buktiPembayaran.getIdLampiran() == null) {
				if (buktiPembayaran != null && buktiPembayaran.getId() != null) {

					LampiranLain lam = LampiranLain.ambil(buktiPembayaran.getId(), BuktiPembayaran.class.getName());

					if (lam == null) {
						MyMessageboxConfig.show(
								"Mohon Bapak/Ibu mengunggah berkas hasil pindai (scan) atau foto bukti pembayaran terlebih dahulu. Langkah yang dapat dilakukan: (1) siapkan berkas bukti pembayaran dalam bentuk gambar atau PDF; (2) tekan tombol unggah dan pilih berkas tersebut; (3) tekan tombol Simpan untuk melanjutkan.",
								"Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return false;
					}
				} else {
					if (lainMahasiswa == null) {
						MyMessageboxConfig.show(
								"Mohon Bapak/Ibu mengunggah berkas hasil pindai (scan) atau foto bukti pembayaran terlebih dahulu. Langkah yang dapat dilakukan: (1) siapkan berkas bukti pembayaran dalam bentuk gambar atau PDF; (2) tekan tombol unggah dan pilih berkas tersebut; (3) tekan tombol Simpan untuk melanjutkan.",
								"Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return false;
					}
				}
			}

		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/BuktiPembayaranAction.java:1046");
		}

		Session session = HibernateUtil.currentSession();

		BiodataCalonMahasiswa biodataCalonMahasiswa = this.biodataCalonMahasiswa != null ? this.biodataCalonMahasiswa
				: (BiodataCalonMahasiswa) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("mahasiswa", mhs)).addOrder(Order.desc("id")).setMaxResults(1)
						.uniqueResult();

		if (buktiPembayaran.getId() != null) {

			if (nilai.getValue() == null || nilai.getValue() < 0.01) {
				MyMessageboxConfig.show(
						"Mohon Bapak/Ibu mengisi Nilai Pembayaran dengan benar. Nilai pembayaran harus berupa angka dan lebih besar dari nol. Langkah yang dapat dilakukan: (1) periksa kembali Nilai Pembayaran yang dimasukkan; (2) pastikan tidak mengandung huruf atau tanda yang tidak sesuai; (3) tekan tombol Simpan untuk melanjutkan.",
						"Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			buktiPembayaran = (BuktiPembayaran) session.load(BuktiPembayaran.class, buktiPembayaran.getId());
			buktiPembayaran.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
			buktiPembayaran.setJenisPembayaran((JenisPembayaran) jenisPembayaran.getSelectedItem().getValue());
			buktiPembayaran.setMahasiswa(mhs);
			buktiPembayaran.setTanggal(tanggal.getValue());
			buktiPembayaran.setNama(nama.getValue());
			buktiPembayaran.setJenisKegiatan((JenisKegiatan) jenisKegiatan.getSelectedItem().getValue());
			buktiPembayaran.setSemester((Integer) (semester == null || semester.getSelectedItem() == null ? 1
					: semester.getSelectedItem().getValue()));
			buktiPembayaran.setNilai(nilai.getValue());
			buktiPembayaran.setKeterangan(keterangan.getValue());

			if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
				buktiPembayaran.setIdLampiran(lainMahasiswa.getId());
			}

			Common.refreshSaveOrUpdate(session, buktiPembayaran);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
						try {
							Session session = StreamingHibernateUtil.getInstance().currentSession();

							session.refresh(lainMahasiswa);
							lainMahasiswa.setRef(buktiPembayaran.getId());

							session.getTransaction().begin();
							session.update(lainMahasiswa);
							session.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();

						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);

							if (eventListener != null) {
								eventListener
										.onEvent(new Event("", addWindow, BuktiPembayaranAction.this.buktiPembayaran));
							}
						}
					});
				}
			});

		} else {

			Rows rows = gripRincianBiaya.getRows();
			if (rows != null) {
				List<Row> myRows = rows.getChildren();
				BuktiPembayaran buktiPembayaran = null;
				List<BuktiPembayaran> buktiPembayarans = new ArrayList<BuktiPembayaran>();
				Double totSemua = 0.0;
				for (Row myRow : myRows) {
					if (myRow.getChildren().get(2) instanceof MyDoublebox) {
						MyDoublebox nilai = (MyDoublebox) myRow.getChildren().get(2);
						Double tot = nilai.getValue() == null ? 0.0 : nilai.getValue();

						totSemua += tot;

						if (tot > 0.1) {
							buktiPembayaran = new BuktiPembayaran();
							buktiPembayaran.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
							buktiPembayaran.setMahasiswa(mhs);
							buktiPembayaran.setTanggal(tanggal.getValue());
							buktiPembayaran.setNama(nama.getValue());
							buktiPembayaran
									.setJenisKegiatan((JenisKegiatan) jenisKegiatan.getSelectedItem().getValue());
							buktiPembayaran
									.setJenisPembayaran((JenisPembayaran) jenisPembayaran.getSelectedItem().getValue());
							buktiPembayaran
									.setSemester((Integer) (semester == null || semester.getSelectedItem() == null ? 1
											: semester.getSelectedItem().getValue()));
							buktiPembayaran.setNilai(tot);
							buktiPembayaran.setKeterangan(keterangan.getValue());
							buktiPembayaran.setItemBiaya((ItemBiaya) myRow.getAttribute("itemBiaya"));
							buktiPembayaran.setPengaturanPembayaranBulanan(
									(PengaturanPembayaranBulanan) myRow.getAttribute("pengaturanPembayaranBulanan"));

							if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
								buktiPembayaran.setIdLampiran(lainMahasiswa.getId());
							}

							Common.refreshSaveOrUpdate(session, buktiPembayaran);
							buktiPembayarans.add(buktiPembayaran);
						}
					}
				}

				if (totSemua < 0.01) {
					MyMessageboxConfig.show(
							"Mohon Bapak/Ibu mengisi Nilai Pembayaran dengan benar. Nilai pembayaran harus berupa angka dan lebih besar dari nol. Langkah yang dapat dilakukan: (1) periksa kembali Nilai Pembayaran yang dimasukkan; (2) pastikan tidak mengandung huruf atau tanda yang tidak sesuai; (3) tekan tombol Simpan untuk melanjutkan.",
							"Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}

				if (lainMahasiswa != null && lainMahasiswa.getId() != null) {

					if (buktiPembayarans.size() == 1) {
						try {
							session = StreamingHibernateUtil.getInstance().currentSession();

							session.refresh(lainMahasiswa);
							lainMahasiswa.setRef(buktiPembayaran.getId());

							session.getTransaction().begin();
							session.update(lainMahasiswa);
							session.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();

						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}
					} else {
						for (BuktiPembayaran bukti : buktiPembayarans) {
							try {
								LampiranLain lampiran = (LampiranLain) lainMahasiswa.clone();
								lampiran.setId(null);
								lampiran.setCopyDari(lainMahasiswa);
								lampiran.setRef(bukti.getId());

								session = StreamingHibernateUtil.getInstance().currentSession();
								session.getTransaction().begin();
								session.save(lampiran);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}
						}
					}

				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(null);

						if (eventListener != null) {
							eventListener.onEvent(new Event("", addWindow, BuktiPembayaranAction.this.buktiPembayaran));
						}
					}
				});
			}

		}

		return true;
	}

	private static java.util.Date safeDateboxValue(MyDatebox db) {
		if (db == null) {
			return null;
		}
		try {
			return db.getValue();
		} catch (Exception eNilaiTanggal) {
			// Datebox.getValue() melempar WrongValueException bila teks tanggal yang diketik user
			// TIDAK LENGKAP/tidak valid (bukan sekadar kosong). Perlakukan sama seperti "tidak diisi"
			// (tanpa filter) alih-alih membiarkan exception ini menjatuhkan seluruh proses inisialisasi
			// Criteria (termasuk saat dipanggil dari thread download/laporan latar belakang).
			ais.common.ErrorAuditUtil.record(eNilaiTanggal,
					"auto-audit(empty-catch) src/ais/action/master/BuktiPembayaranAction.java:safeDateboxValue");
			return null;
		}
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(BuktiPembayaran.class);

		java.util.Date mulaiVal = safeDateboxValue(searchmulai);
		java.util.Date sampaiVal = safeDateboxValue(searchsampai);

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(belum != null && belum.isChecked() ? Restrictions.isNull("cicilanPembayaran")
				: Restrictions.sqlRestriction("true"))
				.add(sudah != null && sudah.isChecked() ? Restrictions.isNotNull("cicilanPembayaran")
						: Restrictions.sqlRestriction("true"))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add((searchmahasiswa == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmahasiswa.getAttribute("mahasiswa") == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("mahasiswa", searchmahasiswa.getAttribute("mahasiswa"))))

				.add(searchJenisKegiatan.getSelectedItem() == null
						|| searchJenisKegiatan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jenisKegiatan", searchJenisKegiatan.getSelectedItem().getValue()))

				.add(mulaiVal == null ? Restrictions.sqlRestriction("true")
						: Restrictions.ge("tanggal", mulaiVal))

				.add(sampaiVal == null ? Restrictions.sqlRestriction("true")
						: Restrictions.le("tanggal", sampaiVal))

				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa", Criteria.LEFT_JOIN)

				.add(searchjurusan == null || searchjurusan.getSelectedItem() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1") :

								Restrictions.or(
										Restrictions.eq("biodataCalonMahasiswa.prodi3",
												searchjurusan.getSelectedItem().getValue()),
										Restrictions.or(
												Restrictions.eq("biodataCalonMahasiswa.prodi2",
														searchjurusan.getSelectedItem().getValue()),
												Restrictions.or(
														Restrictions.eq("biodataCalonMahasiswa.prodi1",
																searchjurusan.getSelectedItem().getValue()),
														Restrictions.or(
																CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", searchjurusan, false),
																Restrictions.eq("biodataCalonMahasiswa.prodiLulus",
																		searchjurusan.getSelectedItem().getValue()))))))

		;

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchnama == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		List<BuktiPembayaran> buktiPembayaran = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(buktiPembayaran);
		grid.setRowRenderer(new BuktiPembayaranRenderer());
		grid.setModelCheckMobile(strset);

	}

	@SuppressWarnings("unchecked")
	public static MyToolbarbuttonConfig ambilBukti(final Mahasiswa mahasiswa, final Integer semester,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final JenisKegiatan jenisKegiatan, final Hbox bukti,
			final EventListener eventListenerBukti) {
		Session session = HibernateUtil.currentSession();
		int myBuktiPembayaran = ((Number) session.createCriteria(BuktiPembayaran.class)
				.add(mahasiswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("mahasiswa", mahasiswa))
				.add(biodataCalonMahasiswa == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
				.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
				.add(semester == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("semester", semester))
				.add(Restrictions.isNull("cicilanPembayaran"))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
				"Bukti Pembayaran (terdapat " + Common.numberFormat.get().format(myBuktiPembayaran) + " bukti)",
				"/img/new.gif");
		button.setVisible(myBuktiPembayaran > 0);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				List<BuktiPembayaran> buktiPembayarans = HibernateUtil.currentSession()
						.createCriteria(CicilanPembayaran.class).createAlias("kegiatan", "kegiatan")

						.add(mahasiswa == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("kegiatan.mahasiswa", mahasiswa))

						.add(biodataCalonMahasiswa == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("kegiatan.calonMahasiswa", biodataCalonMahasiswa))

						.add(Restrictions.eq("kegiatan.jenisKegiatan", jenisKegiatan))
						.add(semester == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("kegiatan.semster", semester))
						.add(Restrictions.isNotNull("buktiPembayaran"))
						.setProjection(Projections.property("buktiPembayaran")).list();

				AmbilDataBuktiPembayaranBanyak.tampilkan(buktiPembayarans, mahasiswa, biodataCalonMahasiswa,
						jenisKegiatan, semester, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<BuktiPembayaran> buktiPembayarans = (List<BuktiPembayaran>) arg0.getData();
						if (buktiPembayarans.size() == 1) {
							Common.clear(bukti);
							for (BuktiPembayaran buktiPembayaran : buktiPembayarans) {
								LampiranLain.createDownloadUploadFileLain(bukti, buktiPembayaran.getIdLampiran(),
										BuktiPembayaran.class.getName(), "Bukti Pembayaran", false,
										new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

											}
										}, null, false, false, true, false);

								eventListenerBukti.onEvent(new Event("", null, buktiPembayaran));
							}
						} else {
							MyMessageboxConfig.show(
									"Mohon maaf, untuk pembayaran ini hanya satu bukti pembayaran yang dapat diambil. Langkah yang dapat dilakukan: (1) gunakan bukti pembayaran yang telah dipilih sebelumnya; (2) apabila bukti yang diambil keliru, batalkan terlebih dahulu lalu pilih bukti yang benar; (3) apabila memerlukan bantuan, mohon hubungi Administrator sistem.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
					}

				});

			}

		});

		return button;
	}

	@SuppressWarnings("unchecked")
	private static void masukkanBukti(final Grid gridCicilan, final BuktiPembayaran buktiPembayaran) throws Exception {

		Rows rows = gridCicilan.getRows();

		List<Row> listRow = rows.getChildren();
		for (Row row : listRow) {

			MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");

			if (jumlahCicilan.getValue() == null || jumlahCicilan.getValue().intValue() == 0) {
				row.setVisible(true);
				MyDatebox tanggal = (MyDatebox) row.getAttribute("tanggal");
				Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
				Combobox myCaraBayar = (Combobox) row.getAttribute("caraBayar");

				List<Comboitem> comboitems = myItemBiaya.getChildren();
				for (Comboitem comboitem : comboitems) {
					Object val = comboitem.getValue();
					if (val instanceof PengaturanPembayaranBulanan
							&& buktiPembayaran.getPengaturanPembayaranBulanan() != null) {
						PengaturanPembayaranBulanan bulanan = (PengaturanPembayaranBulanan) val;
						if (bulanan.getId().equals(buktiPembayaran.getPengaturanPembayaranBulanan().getId())) {
							Mahasiswa mahasiswa = buktiPembayaran.getMahasiswa();
							Double nominalModifikasi = mahasiswa != null
									? bulanan.ambilNominalModifikasi(mahasiswa, buktiPembayaran.getSemester())
									: bulanan.getNominal();

							if (buktiPembayaran.getNilai().intValue() != nominalModifikasi.intValue()) {
								String bln = Common.BULAN[bulanan.getRealBulan() - 1];
								MyMessageboxConfig.showFormat(
										"Mohon maaf, nominal pembayaran tidak sesuai dengan tagihan untuk item biaya \"{V1}\" pada bulan {V2}. Nominal tagihan yang seharusnya adalah {V3}, sedangkan nominal pada bukti pembayaran adalah {V4}. Langkah yang dapat dilakukan: (1) periksa kembali nominal pada bukti pembayaran; (2) sesuaikan nominal agar sama dengan nominal tagihan; (3) apabila nominal bukti memang berbeda, mohon konfirmasikan kepada bagian keuangan.",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
										buktiPembayaran.getItemBiaya().getNama(), bln,
										Common.numberFormat.get().format(nominalModifikasi),
										Common.numberFormat.get().format(buktiPembayaran.getNilai()));
								return;
							}

							myItemBiaya.setSelectedItem(comboitem);

							jumlahCicilan.setValue(buktiPembayaran.getNilai());
							tanggal.setValue(buktiPembayaran.getTanggal());
							if (!myCaraBayar.getChildren().isEmpty()) {
								myCaraBayar.setSelectedIndex(0);
							}

							EventListener itemBiayaEventListener = (EventListener) myItemBiaya
									.getAttribute("itemBiayaEventListener");
							try {
								itemBiayaEventListener.onEvent(null);
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}

							tanggal.setDisabled(false);

							break;
						}
					} else if (val instanceof ItemBiaya) {
						ItemBiaya selectedItemBiaya = (ItemBiaya) val;
						if (selectedItemBiaya.getId().equals(buktiPembayaran.getItemBiaya().getId())) {

							myItemBiaya.setSelectedItem(comboitem);

							jumlahCicilan.setValue(buktiPembayaran.getNilai());
							tanggal.setValue(buktiPembayaran.getTanggal());
							if (!myCaraBayar.getChildren().isEmpty()) {
								myCaraBayar.setSelectedIndex(0);
							}

							EventListener itemBiayaEventListener = (EventListener) myItemBiaya
									.getAttribute("itemBiayaEventListener");
							try {
								itemBiayaEventListener.onEvent(null);
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}

							EventListener jumlahCicilanEventListener = (EventListener) jumlahCicilan
									.getAttribute("jumlahCicilanEventListener");
							try {
								jumlahCicilanEventListener.onEvent(null);
							} catch (Exception e) {
								// TODO Auto-generated catch
								// block
								Common.tampilErrorJikaAdmin(e);
							}

							jumlahCicilan.setDisabled(false);
							tanggal.setDisabled(false);

							break;
						}
					}
				}

				if (buktiPembayaran.getJenisPembayaran() != null) {
					Common.selectComboItem(myCaraBayar, buktiPembayaran.getJenisPembayaran());
					myCaraBayar.setDisabled(true);
				}

				Hbox hboxLampiran = (Hbox) row.getAttribute("hboxLampiran");
				if (hboxLampiran != null) {
					Common.clear(hboxLampiran);
					Vbox myvbox = new Vbox();
					myvbox.setParent(hboxLampiran);

					Hbox hbox = new Hbox();
					hbox.setParent(myvbox);
					LampiranLain.createDownloadUploadFileLain(hbox, buktiPembayaran.getIdLampiran(),
							BuktiPembayaran.class.getName(), "Bukti Pembayaran", true, null, null, false, true, true,
							false);
				}

				row.setValign("top");row.setAttribute("jumlahCicilan", jumlahCicilan);
				Long idLampiran = buktiPembayaran.getIdLampiran() == null ? -Common.randLong()
						: buktiPembayaran.getIdLampiran();
				row.setValign("top");row.setAttribute("idLampiran", idLampiran);
				row.setValign("top");row.setAttribute("buktiPembayaran", buktiPembayaran);
				row.setValign("top");row.setAttribute("tanggal", tanggal);
				row.setValign("top");row.setAttribute("itemBiaya", myItemBiaya);
				row.setValign("top");row.setAttribute("caraBayar", myCaraBayar);

				break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static MyToolbarbuttonConfig ambilBukti(final Mahasiswa mahasiswa, final Integer semester,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final JenisKegiatan jenisKegiatan,
			final Grid gridCicilan, final BuktiPembayaran buktiPembayaranParam) {
		Session session = HibernateUtil.currentSession();
		int myBuktiPembayaran = buktiPembayaranParam != null && buktiPembayaranParam.getId() != null ? 0
				: ((Number) session.createCriteria(BuktiPembayaran.class)
						.add(mahasiswa == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("mahasiswa", mahasiswa))
						.add(biodataCalonMahasiswa == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
						.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
						.add(semester == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("semester", semester))
						.add(Restrictions.isNull("cicilanPembayaran")).setProjection(Projections.rowCount())
						.uniqueResult()).intValue();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
				"Bukti Pembayaran (terdapat " + Common.numberFormat.get().format(myBuktiPembayaran) + " bukti)",
				"/img/new.gif");
		button.setVisible(myBuktiPembayaran > 0);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				List<BuktiPembayaran> buktiPembayarans = HibernateUtil.currentSession()
						.createCriteria(CicilanPembayaran.class).createAlias("kegiatan", "kegiatan")

						.add(mahasiswa == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("kegiatan.mahasiswa", mahasiswa))

						.add(biodataCalonMahasiswa == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("kegiatan.calonMahasiswa", biodataCalonMahasiswa))

						.add(Restrictions.eq("kegiatan.jenisKegiatan", jenisKegiatan))
						.add(semester == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("kegiatan.semster", semester))
						.add(Restrictions.isNotNull("buktiPembayaran"))
						.setProjection(Projections.property("buktiPembayaran")).list();

				AmbilDataBuktiPembayaranBanyak.tampilkan(buktiPembayarans, mahasiswa, biodataCalonMahasiswa,
						jenisKegiatan, semester, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<BuktiPembayaran> buktiPembayarans = (List<BuktiPembayaran>) arg0.getData();

						for (BuktiPembayaran buktiPembayaran : buktiPembayarans) {
							masukkanBukti(gridCicilan, buktiPembayaran);
						}

					}

				});

			}

		});

		if (buktiPembayaranParam != null) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					masukkanBukti(gridCicilan, buktiPembayaranParam);
				}
			}, "harap tunggu..", false, 800);
		}

		return button;
	}

}
