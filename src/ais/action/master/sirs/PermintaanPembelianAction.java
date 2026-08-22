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
import ais.action.master.sirs.detail.PermintaanPembelianDetailAction;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.PermintaanPembelian;
import ais.database.model.sirs.PermintaanPembelianDetail;
import ais.database.model.sirs.PesananPembelian;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

public class PermintaanPembelianAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchkode;
	private Combobox searchlokasi;

	private Combobox lokasi;
	private MyTextbox kode;
	private MyTextbox keterangan;
	private MyDatebox tanggalPembuatan;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private PermintaanPembelian permintaanPembelian;
	private Toolbarbutton add;
	private Lokasi myLokasi;

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

	class PermintaanPembelianRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final PermintaanPembelian permintaanPembelian = (PermintaanPembelian) arg1;

			final PermintaanPembelianDetailAction detail;
			(detail = new PermintaanPembelianDetailAction(permintaanPembelian)).setParent(arg0);

			RevisiHelper.createNewRevisi(PermintaanPembelian.class, permintaanPembelian, permintaanPembelian.getKode())
					.setParent(arg0);

			new Label(permintaanPembelian.getLokasi() == null ? "" : permintaanPembelian.getLokasi().getNama())
					.setParent(arg0);

			new Label(permintaanPembelian.getDibuatOleh() == null ? ""
					: permintaanPembelian.getDibuatOleh().getUserNama()).setParent(arg0);
			new Label(permintaanPembelian.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(permintaanPembelian.getTanggalPembuatan())).setParent(arg0);

			final Label disetujuiOleh;
			(disetujuiOleh = new Label(permintaanPembelian.getDisetujuiOleh() == null ? ""
					: permintaanPembelian.getDisetujuiOleh().getUserNama())).setParent(arg0);

			final Label disetujuiTanggal;
			(disetujuiTanggal = new Label(permintaanPembelian.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(permintaanPembelian.getTanggalPersetujuan()))).setParent(arg0);
			new Label(permintaanPembelian.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Purchase request");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event event) throws Exception {

					Map parameters = new HashMap();
					parameters.put("id", permintaanPembelian.getId());
					Report.generateWindowReport(Report.PDF, parameters, "sirs/purchase_request",
							permintaanPembelian.getTanggalPembuatan());
				}

			});
			button.setParent(toolbar);

			final Toolbarbutton disetujui = new ais.ui.util.MyToolbarbuttonConfig("", "/img/check.png");

			final Toolbarbutton dibatalkan = new ais.ui.util.MyToolbarbuttonConfig("", "/img/cross.png");
			final Toolbarbutton hapus = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			final Toolbarbutton rubah = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");

			disetujui.setVisible(approve && permintaanPembelian.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && permintaanPembelian.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menyetujui Purchase Request ini? Setelah disetujui, data Purchase Request tidak dapat diubah kembali.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										Integer count = ((Number) session
												.createCriteria(PermintaanPembelianDetail.class)
												.add(Restrictions.eq("permintaanPembelian", permintaanPembelian))
												.add(Restrictions.isNull("vendor"))
												.setProjection(Projections.count("id")).uniqueResult()).intValue();

										if (!count.equals(0)) {
											MyMessageboxConfig.show("Data supplier belum lengkap. Mohon lengkapi data supplier terlebih dahulu sebelum menyetujui Purchase Request. Langkah yang dapat dilakukan: (1) buka detail Purchase Request; (2) isi supplier pada setiap item yang masih kosong; (3) simpan perubahan lalu ulangi proses persetujuan.", "Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										count = ((Number) session.createCriteria(PermintaanPembelianDetail.class)
												.add(Restrictions.eq("permintaanPembelian", permintaanPembelian))
												.add(Restrictions.or(Restrictions.isNull("satuanItem"),
														Restrictions.eq("satuanItem", ConstantValues.DEFAULT_SATUAN)))
												.setProjection(Projections.count("id")).uniqueResult()).intValue();

										if (!count.equals(0)) {
											MyMessageboxConfig.show("Data satuan belum lengkap. Mohon lengkapi data satuan pada setiap item terlebih dahulu. Langkah yang dapat dilakukan: (1) buka detail Purchase Request; (2) pilih satuan yang sesuai pada setiap item; (3) simpan perubahan lalu ulangi proses persetujuan.", "Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										count = ((Number) session.createCriteria(PermintaanPembelianDetail.class)
												.add(Restrictions.eq("permintaanPembelian", permintaanPembelian))
												.add(Restrictions.lt("jumlah", 1.0))
												.setProjection(Projections.count("id")).uniqueResult()).intValue();

										if (!count.equals(0)) {
											MyMessageboxConfig.show("Data jumlah item belum lengkap. Mohon isi jumlah untuk setiap item terlebih dahulu. Langkah yang dapat dilakukan: (1) buka detail Purchase Request; (2) isi jumlah item minimal 1 pada setiap item; (3) simpan perubahan lalu ulangi proses persetujuan.", "Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										permintaanPembelian.setDisetujuiOleh(Common.getCurrentUser());
										permintaanPembelian.setTanggalPersetujuan(new Date());
										Common.refreshUpdate(session, (permintaanPembelian));

										disetujuiTanggal
												.setValue(permintaanPembelian.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get()
																.format(permintaanPembelian.getTanggalPersetujuan()));
										disetujuiOleh.setValue(permintaanPembelian.getDisetujuiOleh() == null ? ""
												: permintaanPembelian.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(approve && permintaanPembelian.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && permintaanPembelian.getDisetujuiOleh() != null);
										rubah.setVisible(edit && permintaanPembelian.getDisetujuiOleh() == null);
										hapus.setVisible(delete && permintaanPembelian.getDisetujuiOleh() == null);
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

					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin membatalkan Purchase Request ini? Pembatalan akan menghapus persetujuan yang telah diberikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										Integer jml = ((Number) session.createCriteria(PesananPembelian.class)
												.add(Restrictions.eq("permintaanPembelian", permintaanPembelian))
												.setProjection(Projections.count("id"))
												.add(Restrictions.isNotNull("disetujuiOleh")).uniqueResult())
												.intValue();
										if (!jml.equals(0)) {
											MyMessageboxConfig.show(
													"Permintaan Pembelian ini tidak dapat dibatalkan karena Purchase Order-nya sudah disetujui. Langkah yang dapat dilakukan: (1) batalkan terlebih dahulu persetujuan Purchase Order yang terkait; (2) pastikan tidak ada Purchase Order aktif untuk Permintaan Pembelian ini; (3) ulangi proses pembatalan.",
													"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											return;
										}

										permintaanPembelian.setDisetujuiOleh(null);
										permintaanPembelian.setTanggalPersetujuan(null);
										Common.refreshUpdate(session, (permintaanPembelian));

										disetujuiTanggal
												.setValue(permintaanPembelian.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get()
																.format(permintaanPembelian.getTanggalPersetujuan()));
										disetujuiOleh.setValue(permintaanPembelian.getDisetujuiOleh() == null ? ""
												: permintaanPembelian.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(approve && permintaanPembelian.getDisetujuiOleh() == null);
										dibatalkan.setVisible(reject && permintaanPembelian.getDisetujuiOleh() != null);
										rubah.setVisible(edit && permintaanPembelian.getDisetujuiOleh() == null);
										hapus.setVisible(delete && permintaanPembelian.getDisetujuiOleh() == null);
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
			rubah.setVisible(edit && permintaanPembelian.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(permintaanPembelian);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			rubah.setParent(toolbar);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && permintaanPembelian.getDisetujuiOleh() == null);
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

											Integer jml = ((Number) session.createCriteria(PesananPembelian.class)
													.add(Restrictions.eq("permintaanPembelian", permintaanPembelian))
													.setProjection(Projections.count("id")).uniqueResult()).intValue();
											if (!jml.equals(0)) {
												MyMessageboxConfig.show(
														"Permintaan Pembelian ini tidak dapat dihapus karena sudah dibuatkan Purchase Order. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu Purchase Order yang terkait; (2) pastikan tidak ada Purchase Order untuk Permintaan Pembelian ini; (3) ulangi proses penghapusan.",
														"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												return;
											}

											List<PermintaanPembelianDetail> permintaanPembelianDetails = session
													.createCriteria(PermintaanPembelianDetail.class)
													.add(Restrictions.eq("permintaanPembelian", permintaanPembelian))
													.list();
											for (PermintaanPembelianDetail permintaanPembelianDetail : permintaanPembelianDetails) {
												Common.refreshDelete(session, permintaanPembelianDetail);
											}

											Common.refreshDelete(session, permintaanPembelian);
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
		init(new PermintaanPembelian());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PermintaanPembelian permintaanPembelian) throws Exception {
		this.permintaanPembelian = permintaanPembelian;
		addWindow.setTitle(permintaanPembelian.getId() == null ? "Tambah Permintaan Pembelian" : "Ubah Permintaan Pembelian");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Permintaan Pembelian (Purchase Request)")));
		String mykode = permintaanPembelian.getKode();
		row.appendChild(
				kode = new MyTextbox(permintaanPembelian.getKode() == null ? mykode : permintaanPembelian.getKode()));
		kode.setWidth("90%");
		kode.setDisabled(true);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Pembuatan")));
		row.appendChild(tanggalPembuatan = new MyDatebox(permintaanPembelian.getTanggalPembuatan() == null ? new Date()
				: permintaanPembelian.getTanggalPembuatan()));
		tanggalPembuatan.setFormat(Common.dateFormat3.get().toPattern());
		tanggalPembuatan.setCols(30);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Lokasi")));
		row.appendChild(lokasi = new Combobox());
		Common.insertCombo(lokasi, "nama", Lokasi.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(lokasi,
				permintaanPembelian.getLokasi() == null ? myLokasi : permintaanPembelian.getLokasi());
		lokasi.setDisabled(myLokasi != null);
		lokasi.setWidth("90%");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null : lokasi.getSelectedItem().getValue());
				String mykode = Common.generateCode(PermintaanPembelian.class, 8, "PR", myLokasi);
				kode.setValue(mykode);
			}
		};
		lokasi.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(
				permintaanPembelian.getKeterangan() == null ? "" : permintaanPembelian.getKeterangan()));
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

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Permintaan Pembelian belum diisi. Mohon lengkapi kode terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih lokasi agar kode dapat dibuat otomatis; (2) pastikan kolom kode tidak kosong; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (lokasi.getSelectedItem() == null) {
			MyMessageboxConfig.show("Lokasi belum dipilih. Mohon pilih lokasi terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar pilihan Lokasi; (2) pilih lokasi yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		/*
		 * if (keterangan.getValue().trim().equals("")) {
		 * Messagebox.show("Keterangan harus diisi", "Peringatan", Messagebox.OK,
		 * Messagebox.EXCLAMATION); return false; }
		 */

		boolean i = checkKodePermintaanPembelian();
		if (i) {
			MyMessageboxConfig.show("Kode Permintaan Pembelian sudah terdaftar di dalam basis data. Mohon gunakan kode yang berbeda. Langkah yang dapat dilakukan: (1) periksa kembali kode yang digunakan; (2) buat ulang kode melalui pemilihan lokasi; (3) simpan kembali data.", "Peringatan", 1, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (permintaanPembelian.getId() != null) {
			permintaanPembelian = (PermintaanPembelian) session.load(PermintaanPembelian.class, permintaanPembelian.getId());

		}

		permintaanPembelian.setLokasi((Lokasi) lokasi.getSelectedItem().getValue());
		permintaanPembelian.setKode(kode.getValue());
		permintaanPembelian.setKeterangan(keterangan.getValue());
		permintaanPembelian.setTanggalPembuatan(tanggalPembuatan.getValue());

		if (permintaanPembelian.getId() != null) {
			Common.refreshUpdate(session, permintaanPembelian);
		} else {
			permintaanPembelian.setDibuatOleh(Common.getCurrentUser());
			myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null : lokasi.getSelectedItem().getValue());
			permintaanPembelian.setIndex(Common.generateMaxByLokasi(PermintaanPembelian.class, myLokasi) + 1);
			String mykode = Common.generateCode(PermintaanPembelian.class, 8, "PR", myLokasi);
			kode.setValue(mykode);
			permintaanPembelian.setKode(mykode);
			session.save(permintaanPembelian);
		}
		return true;
	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PermintaanPembelian.class)

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
		List<PermintaanPembelian> permintaanPembelian = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(permintaanPembelian);
		grid.setRowRenderer(new PermintaanPembelianRenderer());
		grid.setModel(strset);

		grid.renderAll();

	}

	public Boolean checkKodePermintaanPembelian() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(PermintaanPembelian.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.permintaanPembelian.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.permintaanPembelian.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
