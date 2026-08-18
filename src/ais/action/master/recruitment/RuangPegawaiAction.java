package ais.action.master.recruitment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
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
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.recruitment.helper.CommonReportPegawai;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Gedung;
import ais.database.model.recruitment.CalonPegawai;
import ais.database.model.recruitment.GelombangPendaftaranPegawai;
import ais.database.model.recruitment.RuangGelombangPendaftaranPegawaiPegawai;
import ais.database.model.recruitment.RuangPegawai;
import ais.database.model.recruitment.UjianPegawai;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class RuangPegawaiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Textbox searchnama;
	private Textbox nama;
	private Textbox searchkodeRuangan;
	private Textbox kodeRuangan;
	private Textbox searchkapasitasruangan;

	private Decimalbox kapasitasRuangan;
	private Combobox searchgedung;
	private Combobox searchUjianPegawai;
	private Combobox gedung;
	private Combobox gelombangPendaftaranPegawai;
	private Combobox searchgelombangPendaftaranPegawai;
	private MyToolbarbuttonConfig add;
	private RuangPegawai ruangPegawai;
	private Combobox ujianPegawai;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}
		Common.insertCombo(gedung = new Combobox(), "nama", Gedung.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchgedung, "nama", Gedung.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(gelombangPendaftaranPegawai = new Combobox(), "nama", GelombangPendaftaranPegawai.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchgelombangPendaftaranPegawai, "nama", GelombangPendaftaranPegawai.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (!searchgelombangPendaftaranPegawai.getChildren().isEmpty()) {
			searchgelombangPendaftaranPegawai.setSelectedIndex(0);
		}
		if (searchgelombangPendaftaranPegawai != null) { searchgelombangPendaftaranPegawai.setReadonly(true); }

		if (execution.getParameter("gelombangPendaftaranPegawai") != null) {
			GelombangPendaftaranPegawai gel = (GelombangPendaftaranPegawai) HibernateUtil.currentSession()
					.createCriteria(GelombangPendaftaranPegawai.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("gelombangPendaftaranPegawai"))))
					.uniqueResult();
			if (gel != null) {
				Common.selectComboItem(true, searchgelombangPendaftaranPegawai, gel);
				searchgelombangPendaftaranPegawai.setDisabled(true);
			}
		}

		Common.insertCombo(searchUjianPegawai, "nama", "gelombangPendaftaranPegawai", UjianPegawai.class);

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Perbaiki Urutan Nomor Ujian di Ruang Ujian",
				"/img/svg/check2-circle.svg");
		if (button != null) { button.setParent(add.getParent()); }
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<RuangPegawai> ruangPegawais = initCriteria(true).list();
						Session session = HibernateUtil.currentSession();
						for (RuangPegawai ruangPegawai : ruangPegawais) {
							int count = ((Number) session.createCriteria(RuangGelombangPendaftaranPegawaiPegawai.class)
									.add(Restrictions.eq("ruangPegawai", ruangPegawai))
									.createAlias("calonPegawai", "calonPegawai")
									.add(Restrictions.ne("calonPegawai.nomorInduk", ""))
									.add(Restrictions.isNotNull("calonPegawai.nomorInduk"))
									.setProjection(Projections.rowCount()).uniqueResult()).intValue();
							if (ruangPegawai.getKapasitasRuangan().intValue() != count) {
								List<RuangPegawai> ruangIniDanSelanjutnya = initCriteria(false)
										.addOrder(Order.asc("id")).add(Restrictions.ge("id", ruangPegawai.getId()))
										.list();
								if (!ruangIniDanSelanjutnya.isEmpty()) {
									List<CalonPegawai> calonPegawais = session
											.createCriteria(RuangGelombangPendaftaranPegawaiPegawai.class)
											.createAlias("calonPegawai", "calonPegawai")
											.add(Restrictions.ne("calonPegawai.nomorInduk", ""))
											.add(Restrictions.isNotNull("calonPegawai.nomorInduk"))
											.setProjection(Projections.property("calonPegawai"))
											.add(Restrictions.in("ruangPegawai", ruangIniDanSelanjutnya))
											.addOrder(Order.asc("calonPegawai.nomorInduk")).list();
									int jumlahTotal = 0;
									for (RuangPegawai pegawai : ruangIniDanSelanjutnya) {
										for (int i = 0; i < pegawai.getKapasitasRuangan(); i++) {
											if (jumlahTotal < calonPegawais.size()) {
												CalonPegawai calonPegawai = calonPegawais.get(jumlahTotal);
												RuangGelombangPendaftaranPegawaiPegawai ruangGelombangPendaftaranPegawaiPegawai = (RuangGelombangPendaftaranPegawaiPegawai) session
														.createCriteria(RuangGelombangPendaftaranPegawaiPegawai.class)
														.add(Restrictions.eq("calonPegawai", calonPegawai))
														.setMaxResults(1).uniqueResult();
												if (ruangGelombangPendaftaranPegawaiPegawai != null) {
													ruangGelombangPendaftaranPegawaiPegawai.setRuangPegawai(pegawai);
													Common.refreshSaveOrUpdate(session,
															ruangGelombangPendaftaranPegawaiPegawai);
												}
											}
											jumlahTotal++;
										}
									}
								}
								break;
							}
						}
						onSearchDefault(arg0);
					}
				});
			}
		});
	}

	class RuangPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final RuangPegawai ruangPegawai = (RuangPegawai) arg1;

			Integer isi = cekRuanganIsi(ruangPegawai);

			if (ruangPegawai.getPenuh().equals(0) && isi.equals(ruangPegawai.getKapasitasRuangan())) {
				ruangPegawai.setPenuh(1);
				Common.refreshUpdate(ruangPegawai);
			}

			RevisiHelper.createNewRevisi(RuangPegawai.class, ruangPegawai, ruangPegawai.getNama()).setParent(arg0);

			new Label(ruangPegawai.getKodeRuangan()).setParent(arg0);
			new Label(ruangPegawai.getGedung().getNama()).setParent(arg0);
			new Label(ruangPegawai.getKapasitasRuangan() == null ? ""
					: ruangPegawai.getKapasitasRuangan().toString() + "/" + isi).setParent(arg0);
			new Label(ruangPegawai.getGelombangPendaftaranPegawai() == null ? ""
					: ruangPegawai.getGelombangPendaftaranPegawai().getNama()).setParent(arg0);
			new Label(ruangPegawai.getUjianPegawai() == null ? "" : ruangPegawai.getUjianPegawai().getNama())
					.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(vbox);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit-box-line.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Ubah Data");
			button.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(ruangPegawai);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Hapus Data");
			button.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE));
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
											Common.refreshDelete(ruangPegawai);
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

			button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
			button.setOrient("vertical");
			button.setTooltiptext("Absensi");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportPegawai.onCetakAbsensiPegawai(ruangPegawai);
				}
			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Verifikasi", "/img/print.png");
			button.setOrient("vertical");
			button.setTooltiptext("Verifikasi");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportPegawai.onCetakVerifikasiPegawai(ruangPegawai);
				}
			});
			button.setParent(toolbar);

			toolbar = new Hbox();
			toolbar.setParent(vbox);
			button = new MyToolbarbuttonConfig("Berita Acara", "/img/album.png");
			button.setOrient("vertical");
			button.setTooltiptext("Berita Acara Ujian");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub
					onCetakBau(ruangPegawai);
				}
			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Cover Album", "/img/album_pmb.png");
			button.setOrient("vertical");
			button.setTooltiptext("Cover Album");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub
					onCetakAbsensi(ruangPegawai);
				}
			});

			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Album Absensi", "/img/absensi_pmb.png");
			button.setOrient("vertical");
			button.setTooltiptext("Album Absensi");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onCetakAlbum(ruangPegawai);
				}
			});
			button.setParent(toolbar);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new RuangPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(RuangPegawai ruangPegawai) {
		this.ruangPegawai = ruangPegawai;
		addWindow.setTitle(ruangPegawai.getId() == null ? "Tambah Ruang Pegawai" : "Ubah Ruang Pegawai");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang untuk ujian"));
		row.appendChild(ujianPegawai = new Combobox());
		Common.insertCombo(ujianPegawai, "nama", "gelombangPendaftaranPegawai", UjianPegawai.class,
				searchgelombangPendaftaranPegawai.getSelectedItem() == null
						|| searchgelombangPendaftaranPegawai.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("gelombangPendaftaranPegawai",
										searchgelombangPendaftaranPegawai.getSelectedItem().getValue()));
		Common.selectComboItem(ujianPegawai, ruangPegawai.getUjianPegawai());
		ujianPegawai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Ruangan"));
		row.appendChild(
				kodeRuangan = new Textbox(ruangPegawai.getKodeRuangan() == null ? "" : ruangPegawai.getKodeRuangan()));
		kodeRuangan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ruangan"));
		row.appendChild(nama = new Textbox(ruangPegawai.getNama() == null ? "" : ruangPegawai.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gedung"));
		Common.selectComboItem(gedung, ruangPegawai.getGedung());
		row.appendChild(gedung);
		gedung.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kapasitas Ruangan"));
		row.appendChild(kapasitasRuangan = new Decimalbox(
				new BigDecimal(ruangPegawai.getKapasitasRuangan() == null ? 30 : ruangPegawai.getKapasitasRuangan())));
		kapasitasRuangan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang"));
		gelombangPendaftaranPegawai.setDisabled(false);
		Common.selectComboItem(gelombangPendaftaranPegawai, ruangPegawai.getGelombangPendaftaranPegawai() == null ? null
				: ruangPegawai.getGelombangPendaftaranPegawai());
		row.appendChild(gelombangPendaftaranPegawai);
		gelombangPendaftaranPegawai.setWidth("90%");
		gelombangPendaftaranPegawai.setReadonly(true);

		if (ruangPegawai.getId() != null) {
			if (cekRuanganIsi(ruangPegawai) > 0) {
				gelombangPendaftaranPegawai.setDisabled(true);
			} else {
				gelombangPendaftaranPegawai.setDisabled(false);
			}
		}

		if (searchgelombangPendaftaranPegawai.getSelectedItem() != null
				&& searchgelombangPendaftaranPegawai.getSelectedItem().getValue() != null) {
			Common.selectComboItem(gelombangPendaftaranPegawai,
					searchgelombangPendaftaranPegawai.getSelectedItem().getValue());
			gelombangPendaftaranPegawai.setDisabled(searchgelombangPendaftaranPegawai.isDisabled());

		}

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
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {

		if (ujianPegawai.getSelectedItem() == null) {
			MyMessageboxConfig.show("Ujian harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (kodeRuangan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Ruangan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (gedung.getSelectedItem() == null) {
			MyMessageboxConfig.show("Gedung harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kapasitasRuangan.getValue() == null) {
			MyMessageboxConfig.show("Kapasitas Ruangan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (ruangPegawai.getId() != null) {
			ruangPegawai = (RuangPegawai) session.load(RuangPegawai.class, ruangPegawai.getId());
		}
		ruangPegawai.setUjianPegawai((UjianPegawai) ujianPegawai.getSelectedItem().getValue());
		ruangPegawai.setNama(nama.getValue());
		ruangPegawai.setKodeRuangan(kodeRuangan.getValue());
		ruangPegawai
				.setGedung((Gedung) (gedung.getSelectedItem() == null ? null : gedung.getSelectedItem().getValue()));
		ruangPegawai.setKapasitasRuangan(
				kapasitasRuangan.getValue() == null ? null : Integer.parseInt(kapasitasRuangan.getValue().toString()));
		ruangPegawai.setGelombangPendaftaranPegawai(
				(GelombangPendaftaranPegawai) (gelombangPendaftaranPegawai.getSelectedItem() == null ? null
						: gelombangPendaftaranPegawai.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, ruangPegawai);
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(RuangPegawai.class);
		if (order)
			criteria.addOrder(Order.asc("id"));
		criteria

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kodeRuangan", searchkodeRuangan.getValue(), MatchMode.ANYWHERE))
				.add(searchkapasitasruangan.getValue().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kapasitasRuangan",
								Integer.parseInt(searchkapasitasruangan.getValue().toString())))
				.add(searchgelombangPendaftaranPegawai.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("gelombangPendaftaranPegawai",
								searchgelombangPendaftaranPegawai.getSelectedItem().getValue()))
				.add(searchgedung.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("gedung", searchgedung.getSelectedItem().getValue()))
				.add(searchUjianPegawai.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ujianPegawai", searchUjianPegawai.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging(initCriteria(false), paging);

		List<RuangPegawai> ruangPegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(ruangPegawai);
		grid.setRowRenderer(new RuangPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Integer cekRuanganIsi(RuangPegawai ruangPegawai) {
		Integer count = 0;
		Session session = HibernateUtil.currentSession();
		session.refresh(ruangPegawai);
		// List<RuangGelombangPendaftaranPegawaiPegawai>
		// ruangGelombangPendaftaranPegawaiPegawais = session.createCriteria(
		// RuangGelombangPendaftaranPegawaiPegawai.class).add(Restrictions.eq("ruangPegawai",
		// ruangPegawai))
		// .list();

		count = ((Number) session.createCriteria(RuangGelombangPendaftaranPegawaiPegawai.class)
				.add(Restrictions.eq("ruangPegawai", ruangPegawai)).createAlias("calonPegawai", "calonPegawai")
				.add(Restrictions.ne("calonPegawai.nomorInduk", ""))
				.add(Restrictions.isNotNull("calonPegawai.nomorInduk")).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		// System.out.println(ruangPegawai.getNama() + " ruang bawah");
		// isi = ruangGelombangPendaftaranPegawaiPegawais.size();
		System.out.println("Jumlah isi ruang : " + count);
		return count;

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakAbsensi(RuangPegawai ruang) throws Exception {

		this.ruangPegawai = ruang;
		final Map parameters = ais.common.HashMapGenerator.getRand();

		System.out.println("ruang cetak absensi " + ruang.getId());
		parameters.put("ruang", ruang.getId());
		Report.generatePDFReport(Report.PDF, parameters, "Coverspegawaii", ais.ui.util.WaktuUtil.getDate());

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakBau(RuangPegawai ruang) throws Exception {

		this.ruangPegawai = ruang;
		final Map parameters = ais.common.HashMapGenerator.getRand();

		System.out.println("ruang cetak Bau " + ruang.getId());
		parameters.put("ruang", ruang.getId());
		Report.generatePDFReport(Report.PDF, parameters, "BeritaAcaraUjianPegawai", ais.ui.util.WaktuUtil.getDate());

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakAlbum(RuangPegawai ruang) throws Exception {

		this.ruangPegawai = ruang;
		// final Map<String, Long> parameters = new HashMap<String, Long>();
		final Map parameters = ais.common.HashMapGenerator.getRand();
		List<Map<String, Object>> maps = getDataAlbumPegawaiAdmin(ruang);
		parameters.put("ujian", ruang.getUjianPegawai() == null ? -1L : ruang.getUjianPegawai().getId());
		parameters.put("ruang", ruang.getId());
		parameters.put("gelombang_pendaftaran",
				ruang.getUjianPegawai() == null || ruang.getUjianPegawai().getGelombangPendaftaranPegawai() == null ? ""
						: ruang.getUjianPegawai().getGelombangPendaftaranPegawai().getNama());
		parameters.put("ket_ruang", ruang.getNama() + " ( " + ruang.getGedung().getNama() + " )");
		System.out.println("Cetak Album Pegawai gelombangPendaftaranPegawai "
				+ ruang.getGelombangPendaftaranPegawai().getNama() + " ruang " + ruang.getNama());

		parameters.put("gelombangPendaftaranPegawai", ruang.getGelombangPendaftaranPegawai().getNama());
		Report.generatePDFReport("pdf", parameters, "AlbumPegawaiHari", ais.ui.util.WaktuUtil.getDate(), maps);

	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> getDataAlbumPegawaiAdmin(RuangPegawai ruang) throws Exception {
		this.ruangPegawai = ruang;
		Session session = HibernateUtil.currentSession();
		List<RuangGelombangPendaftaranPegawaiPegawai> listPendaftaranWisuda = session
				.createCriteria(RuangGelombangPendaftaranPegawaiPegawai.class)
				.createAlias("calonPegawai", "calonPegawai").add(Restrictions.ne("calonPegawai.nomorInduk", ""))
				.add(Restrictions.isNotNull("calonPegawai.nomorInduk")).addOrder(Order.asc("id"))
				.add(Restrictions.eq("ruangPegawai", ruang)).list();

		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		Iterator<?> itr = listPendaftaranWisuda.iterator();

		try {

			while (itr.hasNext()) {
				RuangGelombangPendaftaranPegawaiPegawai beanPendaftaranWisuda = (RuangGelombangPendaftaranPegawaiPegawai) itr
						.next();
				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("nama", beanPendaftaranWisuda.getCalonPegawai().getNama().toUpperCase());
				map.put("no_ujian", beanPendaftaranWisuda.getCalonPegawai().getNomorInduk());
				map.put("ttl", beanPendaftaranWisuda.getCalonPegawai().getTempatLahir().toUpperCase() + " / "
						+ Common.dateFormat2.get().format(beanPendaftaranWisuda.getCalonPegawai().getTanggalLahir()));
				map.put("kelamin", beanPendaftaranWisuda.getCalonPegawai().getJenisKelamin());

				map.put("gelombang_pendaftaran",
						beanPendaftaranWisuda.getCalonPegawai().getGelombangPendaftaranPegawai().getNama());
				map.put("alamat", beanPendaftaranWisuda.getCalonPegawai().getAlamatPegawai());

				beanPendaftaranWisuda.getCalonPegawai().putPhoto(map);

				maps.add(map);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return maps;
	}

}
