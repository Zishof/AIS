package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.SessionCounter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.AbsenPiket;
import ais.database.model.sekolah.AbsenPiketDetail;
import ais.database.model.sekolah.KunjunganSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KunjunganSiswaAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private AmbilDataSiswaBanbox searchsiswa;

	private MyDatebox tanggal;
	private AmbilDataSiswaBanbox siswa;
	private Textbox nama;
	private Textbox alamat;
	private Combobox yayasan;
	private Combobox sekolah;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private MyToolbarbuttonConfig add;

	private Textbox kodeSiswa;
	private Row rowSiswa;
	private Row rowNama;
	private Row rowAlamat;
	private MyCheckboxConfig checkbox;

	private Label label_universitas;
	private Tbmuser tbmuser = null;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initBahasaParameter(execution.getParameter("lang"));
		HttpServletRequest request = (HttpServletRequest) execution.getNativeRequest();
		SessionCounter.initSessionTimeout(request.getSession(), null, false);
		if (label_universitas != null) {
			label_universitas.setValue(Common.getKonfigurasi("label_universitas", "Universitas").getNilai());
		}
		Common.ROOT = execution.getContextPath();
		tbmuser = Common.getCurrentUser();
		if (tbmuser != null) {
			if (add != null) {
			add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
			add.setTooltiptext("Tambah");
			}

			edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		if (add != null) { add.setVisible(false); }

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		
		String[] contents = new String[] { "id", "siswa.nis", "siswa.nama", "sekolah.nama", "tanggal", "tgl",
				"jam", "kode", "nama", "alamat", "keterangan" };
		Common.appendDownloadButton(add, KunjunganSiswa.class, this, contents);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		if (searchsiswa != null) {
			searchsiswa.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			});
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

				kodeSiswa.focus();
			}
		});
	}

	class KunjunganSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KunjunganSiswa kunjunganSiswa = (KunjunganSiswa) arg1;

			CommonMedia.tampilkanGambarKecil(kunjunganSiswa.getSiswa()).setParent(arg0);

			new Label(kunjunganSiswa.getKode() + " " + kunjunganSiswa.getNama()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(kunjunganSiswa.getSiswa().getNomorInduk()).setParent(vbox);
			new Label(kunjunganSiswa.getSiswa().getNomorIndukNasional()).setParent(vbox);

			new Label(
					kunjunganSiswa.getSiswa().getKelas() == null ? "" : kunjunganSiswa.getSiswa().getKelas().getNama())
					.setParent(arg0);

			RevisiHelper.createNewRevisi(KunjunganSiswa.class, kunjunganSiswa,
					kunjunganSiswa.getTanggal() == null ? "" : Common.dateFormat5.get().format(kunjunganSiswa.getTanggal()))
					.setParent(arg0);

			new Label(kunjunganSiswa.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kunjunganSiswa);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
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

											Common.refreshDelete(kunjunganSiswa);

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
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KunjunganSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KunjunganSiswa kunjunganSiswa) throws Exception {
		addWindow.setTitle(kunjunganSiswa.getId() == null ? "Tambah Kunjungan Siswa" : "Ubah Kunjungan Siswa");
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

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu/Tanggal Kunjungan"));
		row.appendChild(tanggal = new MyDatebox(kunjunganSiswa.getTanggal()));
		tanggal.setFormat(Common.dateFormat.get().toPattern());

		tanggal.setWidth("90%");
		tanggal.setDisabled(tbmuser == null);
		checkbox = new MyCheckboxConfig("Bukan Siswa Sekolah");

		rowSiswa = new MyFormRow();
		rowSiswa.setVisible(checkbox.isChecked());
		rowSiswa.setStyle("border:0px;background: transparent;");
		rowSiswa.setParent(rows);
		rowSiswa.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa")));
		rowSiswa.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setAttribute("siswa", kunjunganSiswa.getSiswa());
		siswa.setValue(kunjunganSiswa.getSiswa() == null ? "" : kunjunganSiswa.getSiswa().toString());
		siswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(checkbox);
		checkbox.setChecked(kunjunganSiswa.getSiswa() == null);

		rowNama = new MyFormRow();
		rowNama.setVisible(checkbox.isChecked());
		rowNama.setStyle("border:0px;background: transparent;");
		rowNama.setParent(rows);
		rowNama.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Pengunjung")));
		rowNama.appendChild(nama = new Textbox(kunjunganSiswa.getNama()));
		nama.setWidth("90%");

		rowAlamat = new MyFormRow();
		rowAlamat.setVisible(checkbox.isChecked());
		rowAlamat.setStyle("border:0px;background: transparent;");
		rowAlamat.setParent(rows);
		rowAlamat.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat Pengunjung")));
		rowAlamat.appendChild(alamat = new Textbox(kunjunganSiswa.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(5);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowSiswa.setVisible(!checkbox.isChecked());
				rowNama.setVisible(checkbox.isChecked());
				rowAlamat.setVisible(checkbox.isChecked());
			}
		};

		checkbox.addEventListener("onClick", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan = new Combobox());
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah = new Combobox());
		sekolah.setWidth("90%");

		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(kunjunganSiswa.getKeterangan() == null ? "" : kunjunganSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(3);

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
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Berkunjung", "/img/save.gif");
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

	public void onKodeSiswa(Event event) throws Exception {
		if (searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sekolah harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			kodeSiswa.select();
			kodeSiswa.focus();
			return;
		}
		Session session = HibernateUtil.currentNativeSession();
		Siswa siswa = (Siswa) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
				.add(CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(Restrictions.or(Restrictions.eq("idfinger", kodeSiswa.getValue().trim()),
						Restrictions.eq("nomorInduk", kodeSiswa.getValue().trim()))

				)

				.setMaxResults(1).uniqueResult();

		if (siswa == null) {
			MyMessageboxConfig.show("Kode siswa tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			kodeSiswa.setValue("");
			kodeSiswa.select();
			kodeSiswa.focus();
			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
			return;
		}

		KunjunganSiswa kunjunganSiswaPertama = (KunjunganSiswa) session.createCriteria(KunjunganSiswa.class)

				.add(Restrictions.and(Restrictions.and(

						siswa == null
								? Restrictions.and(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.EXACT),
										Restrictions.ilike("alamat", alamat.getValue().trim(), MatchMode.EXACT))
								:

								Restrictions.eq("siswa", siswa),
						Restrictions.eq("sekolah", (Sekolah) searchsekolah.getSelectedItem().getValue())),
						Restrictions.eq("tgl", ais.ui.util.WaktuUtil.getDate())))

				.addOrder(Order.desc("id"))

				.setMaxResults(1).uniqueResult();

		KunjunganSiswa kunjunganSiswa = (KunjunganSiswa) session.createCriteria(KunjunganSiswa.class)

				.add(Restrictions.and(Restrictions.and(Restrictions.and(

						siswa == null
								? Restrictions.and(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.EXACT),
										Restrictions.ilike("alamat", alamat.getValue().trim(), MatchMode.EXACT))
								:

								Restrictions.eq("siswa", siswa),
						Restrictions.eq("sekolah", (Sekolah) searchsekolah.getSelectedItem().getValue())),
						Restrictions.eq("tgl", ais.ui.util.WaktuUtil.getDate())),
						Restrictions.eq("jam", Calendar.getInstance().get(Calendar.HOUR_OF_DAY))))

				.setMaxResults(1).uniqueResult();

		if (kunjunganSiswa == null) {
			kunjunganSiswa = new KunjunganSiswa();
			kunjunganSiswa.setSekolah((Sekolah) searchsekolah.getSelectedItem().getValue());
			kunjunganSiswa.setSiswa(siswa);
			kunjunganSiswa.setTanggal(ais.ui.util.WaktuUtil.getDate());
			session.getTransaction().begin();
			session.save(kunjunganSiswa);
			session.getTransaction().commit();
		}

		String awal = Common.timeFormat.get().format(kunjunganSiswa.getTanggal());
		if (kunjunganSiswaPertama != null) {
			awal = Common.timeFormat.get().format(kunjunganSiswaPertama.getTanggal());
		}

		AbsenPiket absenPiket = (AbsenPiket) session
				.createCriteria(AbsenPiket.class).add(
						Restrictions.and(
								Restrictions.and(Restrictions.eq("kelas", siswa.getKelas()),
										Restrictions.eq("sekolah",
												(Sekolah) searchsekolah.getSelectedItem().getValue())),
								Restrictions.sqlRestriction("date(this_.tanggal)=date('"
										+ Common.databaseDateFormat.get().format(kunjunganSiswa.getTanggal()) + "')")))
				.setMaxResults(1).uniqueResult();

		if (absenPiket == null) {
			absenPiket = new AbsenPiket();
			absenPiket.setKelas(siswa.getKelas());
			absenPiket.setGuru(siswa.getKelas().getGuruPembina());
			absenPiket.setSekolah((Sekolah) searchsekolah.getSelectedItem().getValue());
			absenPiket.setTanggal(kunjunganSiswa.getTanggal());
			session.getTransaction().begin();
			session.save(absenPiket);
			session.getTransaction().commit();
		}

		Statusabsensi statusabsensi = ConstantValues.MASUK;

		AbsenPiketDetail absenPiketDetail = AbsenPiketDetail.ambil(null, siswa, absenPiket,
				absenPiket.getKelas().getAbsensi(), session);

		absenPiketDetail.populate(siswa.getId() + "_" + absenPiket.getId(), statusabsensi,
				"Absen siswa pada " + Common.dateFormat.get().format(kunjunganSiswa.getTanggal()), awal,
				Common.timeFormat.get().format(kunjunganSiswa.getTanggal()), "AbsenPiket");

		session.getTransaction().begin();
		session.update(absenPiketDetail);
		session.getTransaction().commit();

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

				kodeSiswa.setValue("");
				kodeSiswa.select();
				kodeSiswa.focus();
			}
		});

	}

	public boolean onSave(Event event) throws Exception {
		if (tanggal.getValue() == null) {
			MyMessageboxConfig.show("Tanggal Kunjungan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Siswa siswa = (Siswa) this.siswa.getAttribute("siswa");

		if (!checkbox.isChecked()) {
			if (siswa == null) {
				MyMessageboxConfig.show("Kode siswa tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				kodeSiswa.setValue("");
				kodeSiswa.select();
				kodeSiswa.focus();
				return false;
			}
		} else {
			if (nama.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Nama pengunjung harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				nama.focus();
				return false;
			}
			if (alamat.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Alamat pengunjung harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				nama.focus();
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		KunjunganSiswa kunjunganSiswa = (KunjunganSiswa) session.createCriteria(KunjunganSiswa.class)

				.add(Restrictions.and(Restrictions.and(Restrictions.and(

						siswa == null
								? Restrictions.and(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.EXACT),
										Restrictions.ilike("alamat", alamat.getValue().trim(), MatchMode.EXACT))
								:

								Restrictions.eq("siswa", siswa),
						Restrictions.eq("sekolah", (Sekolah) searchsekolah.getSelectedItem().getValue())),
						Restrictions.eq("tgl", ais.ui.util.WaktuUtil.getDate())),
						Restrictions.eq("jam", Calendar.getInstance().get(Calendar.HOUR_OF_DAY))))

				.setMaxResults(1).uniqueResult();

		if (kunjunganSiswa == null) {
			kunjunganSiswa = new KunjunganSiswa();
			kunjunganSiswa.setSekolah((Sekolah) searchsekolah.getSelectedItem().getValue());
			kunjunganSiswa.setSiswa(siswa);
			kunjunganSiswa.setAlamat(alamat.getValue());
			kunjunganSiswa.setNama(nama.getValue());
			kunjunganSiswa.setTanggal(tanggal.getValue());
			session.save(kunjunganSiswa);
		} else {
			kunjunganSiswa.setSekolah((Sekolah) searchsekolah.getSelectedItem().getValue());
			kunjunganSiswa.setSiswa(siswa);
			kunjunganSiswa.setAlamat(alamat.getValue());
			kunjunganSiswa.setNama(nama.getValue());
			kunjunganSiswa.setTanggal(tanggal.getValue());
			Common.refreshUpdate(session, kunjunganSiswa);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Tbmuser userSiswa = Common.getCurrentUser();
		Siswa siswa = userSiswa == null ? null : userSiswa.getSiswa();

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KunjunganSiswa.class).createAlias("sekolah", "sekolah");
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("sekolah.yayasan", searchyayasan, false))

				.add(siswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("siswa", siswa))

				.add(searchsiswa == null || searchsiswa.getAttribute("siswa") == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("siswa", searchsiswa.getAttribute("siswa")));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KunjunganSiswa> kunjunganSiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kunjunganSiswa);
		grid.setRowRenderer(new KunjunganSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
