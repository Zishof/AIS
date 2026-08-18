package ais.action.master.sirs;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataPasienBanbox;
import ais.action.report.Report;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSirs;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.CetakKartuPasien;
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyTextbox;

public class CetakKartuPasienAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Tabpanel tambahData;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchkode;
	private MyTextbox searchnama;
	private MyTextbox searchmr;

	private MyTextbox kode;
	private AmbilDataPasienBanbox pasien;
	private MyTextbox keterangan;
	private MyDatebox tanggal;

	private Label nama;
	private Label umur;
	private Label alamat;
	private Label ttl;
	private Combobox jenisPasien;
	private Label jenisKelamin;

	private boolean edit = false;
	private boolean delete = false;

	private CetakKartuPasien cetakKartuPasien;
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

		Common.insertCombo(jenisPasien = new Combobox(), "nama", JenisPasien.class);

		add = new ais.ui.util.MyToolbarbuttonConfig("Cetak Kartu Pasien Baru", "/img/user_male_add.png");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				init(new CetakKartuPasien());
			}
		});
		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		init(new CetakKartuPasien());
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class CetakKartuPasienRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final CetakKartuPasien cetakKartuPasien = (CetakKartuPasien) arg1;
			if (cetakKartuPasien.getPasien() == null) {
				arg0.detach();
				return;
			}

			Pasien pasien = cetakKartuPasien.getPasien();

			new Label(cetakKartuPasien.getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(CetakKartuPasien.class, cetakKartuPasien, pasien.getNama()).setParent(arg0);
			new Label(Common.dateFormat3.get().format(cetakKartuPasien.getTanggal())).setParent(arg0);

			new Label(pasien.getAlamatLengkap()).setParent(arg0);
			new Label(cetakKartuPasien.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Tracer");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onCetakKartu(cetakKartuPasien.getPasien());
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(cetakKartuPasien);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang sudah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(cetakKartuPasien);
											onSearchDefault(event);
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, mohon hubungi administrator sistem. Rincian kesalahan: {V1}"
															, e.getMessage()));
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
		init(new CetakKartuPasien());
	}

	private EventListener perubahanPasienListener = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			Pasien pasien = (Pasien) CetakKartuPasienAction.this.pasien.getAttribute("pasien");
			if (pasien == null) {
				return;
			}
			CetakKartuPasienAction.this.pasien.setValue(pasien == null ? "" : pasien.getKode().trim());
			nama.setValue(pasien == null ? "" : pasien.getNama());

			if (pasien.getTanggalLahir() != null) {
				Calendar tahunSkr = Calendar.getInstance();
				Calendar tahunLahir = Calendar.getInstance();
				tahunLahir.setTime(pasien.getTanggalLahir());
				Integer myumur = tahunSkr.get(Calendar.YEAR) - tahunLahir.get(Calendar.YEAR);
				umur.setValue(myumur + " thn");
			} else {
				umur.setValue("");
			}

			alamat.setValue(pasien == null ? "" : pasien.getAlamatLengkap());
			ttl.setValue(pasien == null ? ""
					: (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir()) + "/"
							+ (pasien.getTanggalLahir() == null ? ""
									: Common.dateFormat2.get().format(pasien.getTanggalLahir())));

			jenisKelamin
					.setValue(pasien == null ? "" : pasien.getJenisKelamin() == null ? "" : pasien.getJenisKelamin());

			Common.selectComboItem(jenisPasien, pasien.getJenisPasien());

		}
	};

	private void init(final CetakKartuPasien cetakKartuPasien) throws Exception {
		this.cetakKartuPasien = cetakKartuPasien;
		Common.clear(tambahData);
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Cetak Kartu")));
		String mykode = cetakKartuPasien.getKode();
		row.appendChild(kode = new MyTextbox(mykode));
		kode.setWidth("90%");
		kode.setReadonly(true);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pasien")));
		row.appendChild(pasien = new AmbilDataPasienBanbox());
		pasien.setValue(cetakKartuPasien.getPasien() == null ? "" : cetakKartuPasien.getPasien().getKode());
		pasien.setAttribute("pasien", cetakKartuPasien.getPasien());
		pasien.setCols(15);
		pasien.setEventListener(perubahanPasienListener);
		pasien.setDisabled(cetakKartuPasien.getId() != null);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Cetak Kartu")));
		row.appendChild(tanggal = new MyDatebox(
				cetakKartuPasien.getTanggal() == null ? new Date() : cetakKartuPasien.getTanggal()));
		tanggal.setFormat(Common.dateFormat3.get().toPattern());
		tanggal.setCols(30);
		tanggal.setDisabled(true);

		Pasien pasien = cetakKartuPasien.getPasien();

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Pasien")));
		row.appendChild(nama = new Label(pasien == null ? "" : pasien.getNama()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Umur")));
		row.appendChild(umur = new Label(cetakKartuPasien == null || cetakKartuPasien.getUmur() == null ? ""
				: cetakKartuPasien.getUmur() + " thn"));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Kelamin")));
		row.appendChild(jenisKelamin = new Label(
				pasien == null ? "" : pasien.getJenisKelamin() == null ? "" : pasien.getJenisKelamin()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat")));
		row.appendChild(alamat = new Label(pasien == null ? "" : pasien.getAlamatLengkap()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("TTL")));
		row.appendChild(ttl = new Label());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pasien")));
		row.appendChild(jenisPasien);
		Common.selectComboItem(jenisPasien,
				cetakKartuPasien.getPendaftaran() == null ? null : cetakKartuPasien.getPendaftaran().getJenisPasien());
		jenisPasien.setWidth("90%");
		jenisPasien.setDisabled(true);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(
				cetakKartuPasien.getKeterangan() == null ? "" : cetakKartuPasien.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(row);

		add.setParent(toolbar);
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Cetak dan Simpan Data Kartu Pasien",
				"/img/save.gif");
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
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(tambahData);
		tambahData.getLinkedTab().setSelected(true);

		perubahanPasienListener.onEvent(null);
		if (kode.getValue().trim().equals("")) {
			mykode = Common.generateCode(CetakKartuPasien.class, 10, "CETAK-KARTU", myLokasi);
			kode.setValue(mykode);
		}

	}

	public boolean onSave(Event event) throws Exception {
		// if (kode.getValue().trim().equals("")) {
		// Messagebox.show("Kode CetakKartuPasien harus diisi", "Peringatan",
		// Messagebox.OK, Messagebox.EXCLAMATION);
		// return false;
		// }
		if (tanggal.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, tanggal cetak kartu pasien wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) tentukan tanggal cetak kartu pasien pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (pasien.getAttribute("pasien") == null) {
			MyMessageboxConfig.show("Mohon maaf, pasien wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih pasien pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();

		Pendaftaran pendaftaran = (Pendaftaran) session.createCriteria(Pendaftaran.class)
				.add(Restrictions.eq("pasien", pasien.getAttribute("pasien"))).addOrder(Order.desc("id"))
				.setMaxResults(1).uniqueResult();
		if (pendaftaran == null) {
			MyMessageboxConfig.show("Mohon maaf, data pendaftaran pasien tidak ditemukan. Langkah yang dapat dilakukan: (1) pastikan pasien yang dipilih telah memiliki data pendaftaran; (2) lakukan pendaftaran pasien terlebih dahulu apabila belum tersedia; (3) apabila kendala berlanjut, mohon hubungi administrator sistem.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (cetakKartuPasien.getId() != null) {
			cetakKartuPasien = (CetakKartuPasien) session.load(CetakKartuPasien.class, cetakKartuPasien.getId());

		}
		if (kode.getValue().trim().equals("")) {
			kode.setValue(Common.generateCode(CetakKartuPasien.class, 8));
		}

		cetakKartuPasien.setPendaftaran(pendaftaran);
		cetakKartuPasien.setTanggal(tanggal.getValue());
		cetakKartuPasien.setPasien((Pasien) pasien.getAttribute("pasien"));
		cetakKartuPasien.setKode(kode.getValue());
		cetakKartuPasien.setKeterangan(keterangan.getValue());
		if (cetakKartuPasien.getLokasi() == null) {
			cetakKartuPasien.setLokasi(myLokasi);
		}

		if (cetakKartuPasien.getId() != null) {
			Common.refreshUpdate(session, cetakKartuPasien);
		} else {
			cetakKartuPasien.setIndex(Common.generateMaxByLokasi(CetakKartuPasien.class, myLokasi) + 1);
			String mykode = Common.generateCode(CetakKartuPasien.class, 10, "CETAK-KARTU", myLokasi);
			kode.setValue(mykode);
			cetakKartuPasien.setKode(mykode);
			session.save(cetakKartuPasien);

			CommonSirs.simpanTransaksiTindakan(cetakKartuPasien.getPasien(), ConstantValues.PEMBUATAN_KARTU,
					ConstantValues.kelasNormal, myLokasi, 1.0, pendaftaran, cetakKartuPasien);
		}

		MyMessageboxConfig.show("Data cetak kartu pasien telah berhasil disimpan. Terima kasih, Bapak/Ibu.", "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.freeze(tambahData, true);
						add.setDisabled(false);
						onCetakKartu(cetakKartuPasien.getPasien());
					}
				});

		Common.freeze(tambahData, true);
		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<CetakKartuPasien> cetakKartuPasien = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(cetakKartuPasien);
		grid.setRowRenderer(new CetakKartuPasienRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(CetakKartuPasien.class)
				.createAlias("pasien", "pasien", Criteria.LEFT_JOIN)
				.add((searchmr == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pasien.kode", searchmr.getValue(), MatchMode.ANYWHERE)))
				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pasien.nama", searchnama.getValue(), MatchMode.ANYWHERE)))
				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE)));
		if (order)
			criteria.addOrder(Order.asc("id"));

		return criteria;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakKartu(Pasien pasien) {

		try {

			File myfile = new File(Sessions.getCurrent().getWebApp().getRealPath("/report/temp") + "/barcode_"
					+ pasien.getKode() + ".png");
			myfile.getParentFile().mkdirs();
			myfile.createNewFile();

			BarcodeCommon.generateCRCode(pasien.getKode(), myfile);

			String barcode = myfile.getAbsolutePath();
			System.out.println("barcode = " + barcode);

			Map parameters = new HashMap();
			Common.insertProperty(Pasien.class, pasien, parameters, "");
			parameters.put("nip", pasien.getNip() == null ? "" : pasien.getNip().trim());
			parameters.put("mybarcode", barcode);
			parameters.put("nama", pasien.getNama());
			parameters.put("alamat", pasien.getAlamatLengkap());
			parameters.put("wkt_reg", pasien.getTanggalRegistrasi() == null ? ""
					: Common.dateFormat3.get().format(pasien.getTanggalRegistrasi()));

			File file = Report.generateFileReport("sirs/kartu_pasien", Report.PDF, parameters, "sirs/kartu_pasien",
					new Date(), Sessions.getCurrent().getWebApp());

			Report.tampil(file, parameters, "sirs/kartu_pasien");

		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

}
