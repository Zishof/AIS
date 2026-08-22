package ais.action.master.sirs;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.detail.PesananPembelianDetailAction;
import ais.action.master.sirs.helper.AmbilDataPermintaanPembelianBanbox;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.library.Penyedia;
import ais.database.model.sirs.JenisBiayaLain;
import ais.database.model.sirs.PenerimaanOrder;
import ais.database.model.sirs.PermintaanPembelian;
import ais.database.model.sirs.PermintaanPembelianDetail;
import ais.database.model.sirs.PesananPembelian;
import ais.database.model.sirs.PesananPembelianDetail;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

public class PesananPembelianAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchkode;
	private Combobox searchlokasi;

	private AmbilDataPermintaanPembelianBanbox permintaanPembelian;
	private MyTextbox kode;
	private Combobox lokasi;
	private MyTextbox keterangan;
	private Combobox vendor;
	private Combobox jenisBiayaLain;
	private MyDatebox tanggalPembuatan;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private PesananPembelian pesananPembelian;
	private Toolbarbutton add;
	private Lokasi myLokasi;

	// private static long ids = 0L;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		myLokasi = Common.getCurrentLokasi();
		Common.insertCombo(searchlokasi, "nama", Lokasi.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(searchlokasi, myLokasi);
		if (searchlokasi != null) { searchlokasi.setDisabled(myLokasi != null); }

		Common.insertCombo(vendor = new Combobox(), "nama", "alamat", Penyedia.class);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class PesananPembelianRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final PesananPembelian pesananPembelian = (PesananPembelian) arg1;

			final PesananPembelianDetailAction detail;
			(detail = new PesananPembelianDetailAction(pesananPembelian)).setParent(arg0);

			RevisiHelper.createNewRevisi(PesananPembelian.class, pesananPembelian, pesananPembelian.getKode())
					.setParent(arg0);

			new Label(pesananPembelian.getPermintaanPembelian() == null ? ""
					: pesananPembelian.getPermintaanPembelian().getKode()).setParent(arg0);
			new Label(pesananPembelian.getPenyedia() == null ? "" : pesananPembelian.getPenyedia().getNama())
					.setParent(arg0);
			new Label(pesananPembelian.getLokasi() == null ? "" : pesananPembelian.getLokasi().getNama())
					.setParent(arg0);
			new Label(
					pesananPembelian.getJenisBiayaLain() == null ? "" : pesananPembelian.getJenisBiayaLain().getNama())
					.setParent(arg0);
			new Label(pesananPembelian.getDibuatOleh() == null ? ""
					: pesananPembelian.getDibuatOleh().getUserNama()).setParent(arg0);
			new Label(pesananPembelian.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(pesananPembelian.getTanggalPembuatan())).setParent(arg0);

			final Label disetujuiOleh;
			(disetujuiOleh = new Label(pesananPembelian.getDisetujuiOleh() == null ? ""
					: pesananPembelian.getDisetujuiOleh().getUserNama())).setParent(arg0);

			final Label disetujuiTanggal;
			(disetujuiTanggal = new Label(pesananPembelian.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(pesananPembelian.getTanggalPersetujuan()))).setParent(arg0);

			final Label dibatalkanOleh;
			(dibatalkanOleh = new Label(pesananPembelian.getDibatalkanOleh() == null ? ""
					: pesananPembelian.getDibatalkanOleh().getUserNama())).setParent(arg0);
			final Label dibatalkanTanggal;
			(dibatalkanTanggal = new Label(pesananPembelian.getTanggalPembatalan() == null ? ""
					: Common.dateFormat3.get().format(pesananPembelian.getTanggalPembatalan()))).setParent(arg0);
			new Label(pesananPembelian.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Pesanan Pembelian");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event event) throws Exception {

					Map parameters = new HashMap();
					parameters.put("id", pesananPembelian.getId());
					Report.generateWindowReport(Report.PDF, parameters, "sirs/purchase_order",
							pesananPembelian.getTanggalPembuatan());
				}

			});
			button.setParent(toolbar);

			final Toolbarbutton disetujui = new ais.ui.util.MyToolbarbuttonConfig("", "/img/check.png");

			final Toolbarbutton dibatalkan = new ais.ui.util.MyToolbarbuttonConfig("", "/img/cross.png");
			final Toolbarbutton hapus = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			final Toolbarbutton rubah = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");

			disetujui.setVisible(approve && pesananPembelian.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && pesananPembelian.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menyetujui Purchase Order ini? Setelah disetujui, data Purchase Order tidak dapat diubah kembali.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										Integer count = ((Number) session.createCriteria(PesananPembelianDetail.class)
												.add(Restrictions.eq("pesananPembelian", pesananPembelian))
												.add(Restrictions.lt("jumlah", 1.0))
												.setProjection(Projections.count("id")).uniqueResult()).intValue();

										if (!count.equals(0)) {
											MyMessageboxConfig.show("Data jumlah item belum lengkap. Mohon isi jumlah untuk setiap item terlebih dahulu. Langkah yang dapat dilakukan: (1) buka detail Purchase Order; (2) isi jumlah item minimal 1 pada setiap item; (3) simpan perubahan lalu ulangi proses persetujuan.", "Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										pesananPembelian.setDisetujuiOleh(Common.getCurrentUser());
										pesananPembelian.setTanggalPersetujuan(new Date());

										pesananPembelian.setDibatalkanOleh(null);
										pesananPembelian.setTanggalPembatalan(null);

										Common.refreshUpdate(session, (pesananPembelian));

										disetujuiTanggal.setValue(pesananPembelian.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(pesananPembelian.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pesananPembelian.getDisetujuiOleh() == null ? ""
												: pesananPembelian.getDisetujuiOleh().getUserNama());
										dibatalkanTanggal.setValue(pesananPembelian.getTanggalPembatalan() == null ? ""
												: Common.dateFormat3.get().format(pesananPembelian.getTanggalPembatalan()));
										dibatalkanOleh.setValue(pesananPembelian.getDibatalkanOleh() == null ? ""
												: pesananPembelian.getDibatalkanOleh().getUserNama());

										disetujui.setVisible(approve && pesananPembelian.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && pesananPembelian.getDisetujuiOleh() != null);
										rubah.setVisible(edit && pesananPembelian.getDisetujuiOleh() == null);
										hapus.setVisible(delete && pesananPembelian.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			disetujui.setParent(toolbar);

			dibatalkan.setTooltiptext("Dibatalkan");
			dibatalkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin membatalkan Purchase Order ini? Pembatalan akan menghapus persetujuan yang telah diberikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										Integer jml = ((Number) session.createCriteria(PenerimaanOrder.class)
												.add(Restrictions.eq("pesananPembelian", pesananPembelian))
												.setProjection(Projections.count("id"))
												.add(Restrictions.isNotNull("disetujuiOleh")).uniqueResult())
												.intValue();
										if (!jml.equals(0)) {
											MyMessageboxConfig.show(
													"Pesanan Pembelian ini tidak dapat dibatalkan karena Delivery Order-nya sudah disetujui. Langkah yang dapat dilakukan: (1) batalkan terlebih dahulu persetujuan Delivery Order yang terkait; (2) pastikan tidak ada Delivery Order aktif untuk Pesanan Pembelian ini; (3) ulangi proses pembatalan.",
													"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											return;
										}

										pesananPembelian.setDisetujuiOleh(null);
										pesananPembelian.setTanggalPersetujuan(null);

										pesananPembelian.setDibatalkanOleh(Common.getCurrentUser());
										pesananPembelian.setTanggalPembatalan(new Date());

										Common.refreshUpdate(session, (pesananPembelian));

										disetujuiTanggal.setValue(pesananPembelian.getTanggalPersetujuan() == null ? ""
												: Common.dateFormat3.get().format(pesananPembelian.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pesananPembelian.getDisetujuiOleh() == null ? ""
												: pesananPembelian.getDisetujuiOleh().getUserNama());

										dibatalkanTanggal.setValue(pesananPembelian.getTanggalPembatalan() == null ? ""
												: Common.dateFormat3.get().format(pesananPembelian.getTanggalPembatalan()));
										dibatalkanOleh.setValue(pesananPembelian.getDibatalkanOleh() == null ? ""
												: pesananPembelian.getDibatalkanOleh().getUserNama());

										disetujui.setVisible(approve && pesananPembelian.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && pesananPembelian.getDisetujuiOleh() != null);
										rubah.setVisible(edit && pesananPembelian.getDisetujuiOleh() == null);
										hapus.setVisible(delete && pesananPembelian.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			dibatalkan.setParent(toolbar);

			rubah.setTooltiptext("Rubah Data");
			rubah.setVisible(edit && pesananPembelian.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pesananPembelian);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			rubah.setParent(toolbar);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && pesananPembelian.getDisetujuiOleh() == null);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();

											Integer jml = ((Number) session.createCriteria(PenerimaanOrder.class)
													.add(Restrictions.eq("pesananPembelian", pesananPembelian))
													.setProjection(Projections.count("id")).uniqueResult()).intValue();
											if (!jml.equals(0)) {
												MyMessageboxConfig.show(
														"Pesanan Pembelian ini tidak dapat dihapus karena sudah dibuatkan Delivery Order. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu Delivery Order yang terkait; (2) pastikan tidak ada Delivery Order untuk Pesanan Pembelian ini; (3) ulangi proses penghapusan.",
														"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												return;
											}

											List<PesananPembelianDetail> pesananPembelianDetails = session
													.createCriteria(PesananPembelianDetail.class)
													.add(Restrictions.eq("pesananPembelian", pesananPembelian)).list();
											for (PesananPembelianDetail pesananPembelianDetail : pesananPembelianDetails) {
												Common.refreshDelete(session, pesananPembelianDetail);
											}

											Common.refreshDelete(session, pesananPembelian);
											onSearchDefault(event);
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berelasi; (2) pastikan tidak ada transaksi yang menggunakan data ini; (3) ulangi proses penghapusan.",
													e.getMessage()));
										}

									}

								}
							});

				}
			});
			hapus.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PesananPembelian());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final PesananPembelian pesananPembelian) throws Exception {
		this.pesananPembelian = pesananPembelian;
		addWindow.setTitle(pesananPembelian.getId() == null ? "Tambah Pesanan Pembelian" : "Ubah Pesanan Pembelian");
		Common.clear(addWindow);
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Pesanan Pembelian (Purchase Order)")));
		String mykode = pesananPembelian.getKode();
		// if (mykode == null || mykode.trim().equals("")) {
		// mykode = Common.generateCode(PesananPembelian.class, 6, "PO");
		// }
		row.appendChild(kode = new MyTextbox(pesananPembelian.getKode() == null ? mykode : pesananPembelian.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Permintaan Pembelian (Purchase Request)")));
		row.appendChild(permintaanPembelian = new AmbilDataPermintaanPembelianBanbox());
		permintaanPembelian.setValue(pesananPembelian.getPermintaanPembelian() == null ? ""
				: pesananPembelian.getPermintaanPembelian().getKode());
		permintaanPembelian.setAttribute("permintaanPembelian", pesananPembelian.getPermintaanPembelian());
		permintaanPembelian.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Lokasi")));
		row.appendChild(lokasi = new Combobox());
		Common.insertCombo(lokasi, "nama", Lokasi.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(lokasi, pesananPembelian.getLokasi() == null ? null : pesananPembelian.getLokasi());
		lokasi.setDisabled(true);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Supplier")));
		row.appendChild(vendor);
		Common.selectComboItem(vendor, pesananPembelian.getPenyedia());
		vendor.setWidth("90%");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Penyedia myPenyedia = (Penyedia) permintaanPembelian.getAttribute("vendor");
				Common.selectComboItem(vendor, myPenyedia == null ? pesananPembelian.getPenyedia() : myPenyedia);

				PermintaanPembelian myPermintaanPembelian = (PermintaanPembelian) permintaanPembelian
						.getAttribute("permintaanPembelian");
				if (myPermintaanPembelian != null) {
					Common.selectComboItem(lokasi, myPermintaanPembelian.getLokasi());

					myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null : lokasi.getSelectedItem().getValue());
					String mykode = Common.generateCode(PesananPembelian.class, 8, "PO", myLokasi);
					kode.setValue(mykode);
				}

				vendor.setDisabled(true);
			}
		};

		permintaanPembelian.setEventListener(eventListener);
		eventListener.onEvent(null);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Pembuatan")));
		row.appendChild(tanggalPembuatan = new MyDatebox(
				pesananPembelian.getTanggalPembuatan() == null ? new Date() : pesananPembelian.getTanggalPembuatan()));
		tanggalPembuatan.setFormat(Common.dateFormat3.get().toPattern());
		tanggalPembuatan.setCols(30);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Cara Pembelian")));
		row.appendChild(jenisBiayaLain = new Combobox());
		Common.insertCombo(jenisBiayaLain, "nama", "akun", JenisBiayaLain.class,
				Restrictions.eq("jenis", JenisBiayaLain.PEMBELIAN));
		Common.selectComboItem(jenisBiayaLain, pesananPembelian.getJenisBiayaLain());
		jenisBiayaLain.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(
				pesananPembelian.getKeterangan() == null ? "" : pesananPembelian.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Pesanan Pembelian belum diisi. Mohon lengkapi kode terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Permintaan Pembelian agar kode dapat dibuat otomatis; (2) pastikan kolom kode tidak kosong; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (permintaanPembelian.getAttribute("permintaanPembelian") == null) {
			MyMessageboxConfig.show("Kode Permintaan Pembelian (Purchase Request) belum dipilih. Mohon pilih Permintaan Pembelian terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar Permintaan Pembelian; (2) pilih Permintaan Pembelian yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (lokasi.getSelectedItem() == null) {
			MyMessageboxConfig.show("Lokasi belum dipilih. Mohon pilih lokasi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Permintaan Pembelian agar lokasi terisi otomatis; (2) pastikan lokasi tidak kosong; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (vendor.getSelectedItem() == null) {
			MyMessageboxConfig.show("Supplier belum dipilih. Mohon pilih supplier terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar pilihan Supplier; (2) pilih supplier yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (jenisBiayaLain.getSelectedItem() == null) {
			MyMessageboxConfig.show("Cara pembelian belum dipilih. Mohon pilih cara pembelian terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar pilihan Cara Pembelian; (2) pilih cara pembelian yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		boolean i = checkKodePesananPembelian();
		if (i) {
			MyMessageboxConfig.show("Kode Pesanan Pembelian sudah terdaftar di dalam basis data. Mohon gunakan kode yang berbeda. Langkah yang dapat dilakukan: (1) periksa kembali kode yang digunakan; (2) buat ulang kode melalui pemilihan Permintaan Pembelian; (3) simpan kembali data.", "Peringatan", 1, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pesananPembelian.getId() != null) {
			pesananPembelian = (PesananPembelian) session.load(PesananPembelian.class, pesananPembelian.getId());

		}
		pesananPembelian.setJenisBiayaLain((JenisBiayaLain) jenisBiayaLain.getSelectedItem().getValue());
		pesananPembelian.setLokasi((Lokasi) lokasi.getSelectedItem().getValue());
		pesananPembelian.setPenyedia((Penyedia) vendor.getSelectedItem().getValue());
		pesananPembelian
				.setPermintaanPembelian((PermintaanPembelian) permintaanPembelian.getAttribute("permintaanPembelian"));
		pesananPembelian.setKode(kode.getValue());
		pesananPembelian.setKeterangan(keterangan.getValue());
		pesananPembelian.setTanggalPembuatan(tanggalPembuatan.getValue());

		if (pesananPembelian.getId() != null) {
			Common.refreshUpdate(session, pesananPembelian);
		} else {
			pesananPembelian.setDibuatOleh(Common.getCurrentUser());
			myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null : lokasi.getSelectedItem().getValue());
			pesananPembelian.setIndex(Common.generateMaxByLokasi(PesananPembelian.class, myLokasi) + 1);
			String mykode = Common.generateCode(PesananPembelian.class, 8, "PO", myLokasi);
			kode.setValue(mykode);
			pesananPembelian.setKode(mykode);
			session.save(pesananPembelian);
			List<PermintaanPembelianDetail> permintaanPembelianDetails = session
					.createCriteria(PermintaanPembelianDetail.class)
					.add(Restrictions.eq("vendor", pesananPembelian.getPenyedia()))
					.add(Restrictions.eq("permintaanPembelian", pesananPembelian.getPermintaanPembelian())).list();

			for (PermintaanPembelianDetail permintaanPembelianDetail : permintaanPembelianDetails) {
				PesananPembelianDetail pesananPembelianDetail = new PesananPembelianDetail();
				pesananPembelianDetail.setItem(permintaanPembelianDetail.getItem());
				pesananPembelianDetail.setJumlah(permintaanPembelianDetail.getJumlah());
				pesananPembelianDetail.setKeterangan(permintaanPembelianDetail.getKeterangan());
				pesananPembelianDetail.setPesananPembelian(pesananPembelian);
				pesananPembelianDetail.setSatuanItem(permintaanPembelianDetail.getSatuanItem());
				pesananPembelianDetail.setPermintaanPembelianDetail(permintaanPembelianDetail);
				pesananPembelianDetail.setHargaBeli(permintaanPembelianDetail.getHargaBeli());
				session.save(pesananPembelianDetail);
			}

		}
		return true;
	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PesananPembelian.class)

				.add(searchlokasi.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("lokasi", searchlokasi.getSelectedItem().getValue()))
				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE)));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PesananPembelian> pesananPembelian = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pesananPembelian);
		grid.setRowRenderer(new PesananPembelianRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	public Boolean checkKodePesananPembelian() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(PesananPembelian.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.pesananPembelian.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.pesananPembelian.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
