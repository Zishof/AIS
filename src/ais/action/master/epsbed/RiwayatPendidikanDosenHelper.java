package ais.action.master.epsbed;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.employ.helper.RiwayatPendidikanPegawaiHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.RiwayatPendidikanDosenDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jenjang;
import ais.database.model.Kota;
import ais.database.model.Negara;
import ais.database.model.Pegawai;
import ais.database.model.Propinsi;
import ais.database.model.RiwayatPendidikanDosen;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class RiwayatPendidikanDosenHelper {

	private MyGrid grid = new MyGrid();
	private Center center = new Center();
	North north = new North();
	South south = new South();

	Toolbar toolbar = new Toolbar();
	MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
	MyToolbarbuttonConfig kembali = new MyToolbarbuttonConfig("Kembali", "/img/cancel.gif");
	private Combobox jenjang;

	private Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
	public Dosen dosen;
	private RiwayatPendidikanDosen riwayatPendidikanDosen;
	private Textbox gelarAkademik;
	private Textbox kodePerguruanTinggi;
	private Textbox namaSekolah;
	private Textbox bidangIlmu;
	private Combobox negara;
	private Combobox propinsi;
	private Combobox kota;
	private Textbox kotaLain;
	private Row rowPropinsi;
	private Row rowKota;
	private Combobox tahunLulus;
	private Combobox tahunMasuk;
	private Doublebox nilaiAkhir;

	public RiwayatPendidikanDosenHelper() {

		rowPropinsi = new Row();
		rowKota = new Row();
		rowPropinsi.setVisible(false);

		Common.insertCombo(jenjang = new Combobox(), "nama", Jenjang.class,
				Restrictions.or(Restrictions.eq("aktifDipilih", true), Restrictions.isNull("aktifDipilih")));
		Common.insertCombo(negara = new Combobox(), "namaNegara", Negara.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		propinsi = new Combobox();
		kota = new Combobox();
		kotaLain = new Textbox();
		tahunLulus = new Combobox();
		MyComboitemConfig comboitem;
		for (int i = (ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)) - 90; i < (ais.ui.util.WaktuUtil
				.getCalendar().get(Calendar.YEAR)); i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahunLulus.appendChild(comboitem);
		}

		tahunMasuk = new Combobox();
		for (int i = (ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)) - 90; i < (ais.ui.util.WaktuUtil
				.getCalendar().get(Calendar.YEAR)); i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahunMasuk.appendChild(comboitem);
		}

	}

	class PublikasiDosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final RiwayatPendidikanDosen riwayatPendidikanDosen = (RiwayatPendidikanDosen) arg1;
			new Label(riwayatPendidikanDosen.getTahunMasuk() == null ? ""
					: riwayatPendidikanDosen.getTahunMasuk().toString()).setParent(row);
			new Label(riwayatPendidikanDosen.getTahunKeluar() == null ? ""
					: riwayatPendidikanDosen.getTahunKeluar().toString()).setParent(row);
			new Label(riwayatPendidikanDosen.getJenjangPendidikan() == null ? ""
					: riwayatPendidikanDosen.getJenjangPendidikan().getNama()).setParent(row);
			new Label(riwayatPendidikanDosen.getNamaSekolah() == null ? "" : riwayatPendidikanDosen.getNamaSekolah())
					.setParent(row);
			String kota = riwayatPendidikanDosen.getKota() == null ? riwayatPendidikanDosen.getKotaLain()
					: riwayatPendidikanDosen.getKota().getNama();
			String propinsi = riwayatPendidikanDosen.getKota() == null
					? riwayatPendidikanDosen.getNegara().getNamaNegara()
					: riwayatPendidikanDosen.getKota().getPropinsi().getNama();
			new Label(kota + " / " + propinsi).setParent(row);
			new Label(riwayatPendidikanDosen.getNilaiAkhir() == null ? ""
					: riwayatPendidikanDosen.getNilaiAkhir().toString()).setParent(row);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(riwayatPendidikanDosen);

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");

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
											RiwayatPendidikanDosenDao riwayatPendidikanDosenDao = DaoFactory
													.getInstance().getRiwayatPendidikanDosenDao();
											riwayatPendidikanDosenDao
													.delete(riwayatPendidikanDosenDao.merge(riwayatPendidikanDosen));
											// agamaDao.commitTransaction();
											onSearchDefault(dosen);
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

	public Borderlayout display(Dosen dosen) throws Exception {
		this.dosen = dosen;

		Pegawai pegawai = (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), dosen.getPegawaiId(), true);
		System.out.println("pegawai -> " + pegawai);
		if (pegawai == null) {
			pegawai = (Pegawai) HibernateUtil.currentSession().createCriteria(Pegawai.class)
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.add(Restrictions.eq("dosen", dosen)).uniqueResult();
		}
		if (pegawai != null) {
			RiwayatPendidikanPegawaiHelper riwayatPendidikanPegawaiHelper = new RiwayatPendidikanPegawaiHelper(pegawai,
					true);

			return riwayatPendidikanPegawaiHelper.display();
		} else {

			borderlayout.setWidth("100%");
			Common.clear(center);
			Common.clear(north);
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
			column.setWidth("120px");
			column.setParent(columns);
			column = new MyColumnConfig();
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(searchgrid);

			Row row = new Row();
			row.setValign("top");
			row.setStyle("border:0px;background: transparent;spans=2;");
			row.setParent(rows);
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Tambah Data", "/img/new.gif");
			row.appendChild(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub
					init(new RiwayatPendidikanDosen());
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
			column.setLabel("Tahun Masuk");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Tahun Lulus");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Jenjang");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Perguruan Tinggi");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kota");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nilai Akhir");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");

			onSearchDefault(dosen);

			return borderlayout;
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Dosen dosen) {

		Session session = HibernateUtil.currentSession();
		List<RiwayatPendidikanDosen> riwayatPendidikanDosen = session.createCriteria(RiwayatPendidikanDosen.class)
				.addOrder(Order.asc("tahunMasuk")).add(Restrictions.eq("dosen", dosen)).setMaxResults(Common.MAX_RESULT)
				.list();

		ListModel strset = new SimpleListModel(riwayatPendidikanDosen);

		grid.setRowRenderer(new PublikasiDosenRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void init(final RiwayatPendidikanDosen riwayatPendidikanDosen) throws Exception {
		this.riwayatPendidikanDosen = riwayatPendidikanDosen;
		Common.clear(borderlayout);
		borderlayout.setWidth("100%");
		Common.clear(center);
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		final Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				riwayatPendidikanDosen.getDosen() == null ? "" : riwayatPendidikanDosen.getDosen().getNama()));

		row = new Row();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		row.appendChild(jenjang);
		Common.selectComboItem(jenjang, riwayatPendidikanDosen.getJenjangPendidikan() == null ? null
				: riwayatPendidikanDosen.getJenjangPendidikan());

		row = new Row();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelar Akademik"));
		row.appendChild(gelarAkademik = new Textbox(
				riwayatPendidikanDosen.getGelarAkademik() == null ? "" : riwayatPendidikanDosen.getGelarAkademik()));

		row = new Row();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Perguruan Tinggi"));
		row.appendChild(kodePerguruanTinggi = new Textbox(riwayatPendidikanDosen.getKodePerguruanTinggi() == null ? ""
				: riwayatPendidikanDosen.getKodePerguruanTinggi()));

		row = new Row();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Perguruan Tinggi"));
		row.appendChild(namaSekolah = new Textbox(
				riwayatPendidikanDosen.getNamaSekolah() == null ? "" : riwayatPendidikanDosen.getNamaSekolah()));

		row = new Row();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bidang Keilmuan"));
		row.appendChild(bidangIlmu = new Textbox(
				riwayatPendidikanDosen.getBidangIlmu() == null ? "" : riwayatPendidikanDosen.getBidangIlmu()));

		row = new Row();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Negara"));
		row.appendChild(negara);
		Common.selectComboItem(negara,
				riwayatPendidikanDosen.getNegara() == null ? null : riwayatPendidikanDosen.getNegara());

		rowPropinsi = new Row();
		rowPropinsi.setParent(rows);
		rowPropinsi.appendChild(new Label(ais.common.Common.getBahasaConfig("Propinsi")));
		rowPropinsi.setVisible(false);

		rowKota = new Row();
		rowKota.setParent(rows);
		rowKota.appendChild(new Label(ais.common.Common.getBahasaConfig("Kota")));
		rowKota.setVisible(false);

		if (riwayatPendidikanDosen.getKota() != null) {
			insertPropinsi(rows, (Negara) negara.getSelectedItem().getValue());
			Common.insertCombo(kota, "nama", Kota.class,
					Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
							Restrictions.eq("propinsi", riwayatPendidikanDosen.getKota().getPropinsi())));
			rowKota.appendChild(kota);
			Common.selectComboItem(kota, riwayatPendidikanDosen.getKota());
			rowKota.setVisible(true);
		} else if (riwayatPendidikanDosen.getKotaLain() != null) {
			rowKota.appendChild(kotaLain);
			kotaLain.setValue(riwayatPendidikanDosen.getKotaLain());
			rowKota.setVisible(true);
		}

		negara.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				// rowKota.setParent(rows);
				// TODO Auto-generated method stub
				Common.clear(propinsi);
				Common.clear(kota);
				Negara negaraSelected = (Negara) negara.getSelectedItem().getValue();

				if (negaraSelected.getId().equals(1L)) {
					Common.clear(rowPropinsi);
					Common.clear(rowKota);
					// rowKota.detach();
					rowPropinsi.appendChild(new Label(ais.common.Common.getBahasaConfig("Propinsi")));
					rowKota.appendChild(new Label(ais.common.Common.getBahasaConfig("Kota")));
					insertPropinsi(rows, negaraSelected);
					insertKota(rows, null);
				}

				else {
					Common.clear(rowPropinsi);
					Common.clear(rowKota);
					removePropinsi();
					System.out.println("Luar Indonesia");
					rowKota.appendChild(new Label(ais.common.Common.getBahasaConfig("Kota")));
					rowKota.appendChild(kotaLain = new Textbox(
							riwayatPendidikanDosen.getKotaLain() == null ? "" : riwayatPendidikanDosen.getKotaLain()));
					rowKota.setVisible(true);

				}
			}
		});

		row = new Row();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Ijazah"));
		row.appendChild(
				new MyDatebox(riwayatPendidikanDosen.getTanggalIjazah() == null ? ais.ui.util.WaktuUtil.getDate()
						: riwayatPendidikanDosen.getTanggalIjazah()));

		row = new Row();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Masuk *"));
		row.appendChild(tahunMasuk);
		Common.selectComboItem(tahunMasuk,
				riwayatPendidikanDosen.getTahunMasuk() == null ? null : riwayatPendidikanDosen.getTahunMasuk());

		row = new Row();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Lulus *"));
		row.appendChild(tahunLulus);
		Common.selectComboItem(tahunLulus,
				riwayatPendidikanDosen.getTahunKeluar() == null ? null : riwayatPendidikanDosen.getTahunKeluar());

		tahunMasuk.setReadonly(true);
		tahunLulus.setReadonly(true);

		row = new Row();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Akhir"));
		row.appendChild(nilaiAkhir = new MyDoublebox(
				riwayatPendidikanDosen.getNilaiAkhir() == null ? 0.0 : riwayatPendidikanDosen.getNilaiAkhir()));

		south.setParent(borderlayout);

		toolbar.setParent(south);

		simpan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				if (save(arg0)) {
					display(dosen);
					south.detach();
				}
			}
		});
		simpan.setParent(toolbar);

		kembali.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				display(dosen);
				south.detach();
			}
		});
		kembali.setParent(toolbar);

	}

	public boolean save(Event event) throws Exception {

		if (jenjang.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenjang Harus Dipilih", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
			return false;
		}

		if (negara.getSelectedItem() == null) {
			MyMessageboxConfig.show("Negara Harus Dipilih", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
			return false;
		}

		if (tahunMasuk.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Masuk Harus Dipilih", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK,
					"");
			return false;
		}

		if (tahunLulus.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Lulus Harus Dipilih", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK,
					"");
			return false;
		}

		Integer masuk = (Integer) (tahunMasuk.getSelectedItem() == null ? null
				: tahunMasuk.getSelectedItem().getValue());
		Integer lulus = (Integer) (tahunLulus.getSelectedItem() == null ? 0 : tahunLulus.getSelectedItem().getValue());

		if (masuk > lulus) {
			MyMessageboxConfig.show("Tahun masuk tidak boleh lebih besar dari tahun lulus",
					MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
			return false;
		}

		RiwayatPendidikanDosenDao riwayatPendidikanDao = DaoFactory.getInstance().getRiwayatPendidikanDosenDao();
		if (riwayatPendidikanDosen.getId() != null) {
			riwayatPendidikanDosen = riwayatPendidikanDao.load(riwayatPendidikanDosen.getId());
		}
		riwayatPendidikanDosen.setDosen(dosen);
		riwayatPendidikanDosen.setBidangIlmu(bidangIlmu.getValue());
		riwayatPendidikanDosen.setGelarAkademik(gelarAkademik.getValue());
		riwayatPendidikanDosen.setJenjangPendidikan(
				(Jenjang) (jenjang.getSelectedItem() == null ? null : jenjang.getSelectedItem().getValue()));
		riwayatPendidikanDosen.setKodePerguruanTinggi(kodePerguruanTinggi.getValue());
		riwayatPendidikanDosen
				.setKota((Kota) (kota.getSelectedItem() == null ? null : kota.getSelectedItem().getValue()));
		riwayatPendidikanDosen.setKotaLain(kotaLain.getValue());
		riwayatPendidikanDosen.setNamaSekolah(namaSekolah.getValue());
		riwayatPendidikanDosen
				.setNegara((Negara) (negara.getSelectedItem() == null ? null : negara.getSelectedItem().getValue()));
		riwayatPendidikanDosen.setNilaiAkhir(nilaiAkhir.getValue());
		riwayatPendidikanDosen.setTahunMasuk((Integer) tahunMasuk.getSelectedItem().getValue());
		riwayatPendidikanDosen.setTahunKeluar((Integer) tahunLulus.getSelectedItem().getValue());

		if (riwayatPendidikanDosen.getId() != null) {
			riwayatPendidikanDao.update(riwayatPendidikanDosen);
		} else {
			riwayatPendidikanDao.save(riwayatPendidikanDosen);
		}
		return true;
	}

	public void insertPropinsi(final Rows rows, Negara negara) {
		// rowPropinsi = new Row();
		// rowPropinsi.setParent(rows);
		Common.insertCombo(propinsi = new Combobox(), "nama", Propinsi.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.eq("negara", negara)));
		Common.selectComboItem(propinsi,
				riwayatPendidikanDosen.getKota() == null ? null : riwayatPendidikanDosen.getKota().getPropinsi());
		rowPropinsi.appendChild(propinsi);
		rowPropinsi.setVisible(true);

		propinsi.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(rowKota);
				Common.clear(kota);
				Common.clear(kotaLain);

				insertKota(rows, (Propinsi) propinsi.getSelectedItem().getValue());
			}
		});

	}

	public void removePropinsi() {
		rowPropinsi.setVisible(false);
	}

	public void insertKota(Rows rows, Propinsi propinsi) {
		Common.clear(rowKota);
		rowKota.appendChild(new Label(ais.common.Common.getBahasaConfig("Kota")));
		Common.insertCombo(kota = new Combobox(), "nama", Kota.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.eq("propinsi", propinsi == null ? null : propinsi)));
		Common.selectComboItem(kota,
				riwayatPendidikanDosen.getKota() == null ? null : riwayatPendidikanDosen.getKota());
		rowKota.appendChild(kota);

		// rowKota.setParent(rows);
		rowKota.setVisible(true);

	}
}
