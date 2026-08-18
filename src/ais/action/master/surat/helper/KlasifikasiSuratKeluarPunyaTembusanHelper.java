package ais.action.master.surat.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.action.master.employ.helper.AmbilDataJenisJabatanBanyak;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.OnSaveListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.KlasifikasiSuratKeluar;
import ais.database.model.surat.KlasifikasiSuratKeluarPunyaTembusan;

public class KlasifikasiSuratKeluarPunyaTembusanHelper {

	private MyGrid gridJenisJabatan;
	private boolean add = false;
	private boolean delete = false;
	public KlasifikasiSuratKeluar klasifikasiSuratKeluar; 

	public KlasifikasiSuratKeluarPunyaTembusanHelper(MyGrid gridJenisJabatan) {
		this.gridJenisJabatan = gridJenisJabatan;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public Borderlayout initDetail(final KlasifikasiSuratKeluar klasifikasiSuratKeluar,
			final OnSaveListener onSaveListener) {
		this.klasifikasiSuratKeluar = klasifikasiSuratKeluar;
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Jenis Jabatan", "/img/new.gif");
		add.setVisible(KlasifikasiSuratKeluarPunyaTembusanHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSaveListener.onSave(event)) {
					List<JenisJabatan> jenisJabatans = new ArrayList<JenisJabatan>();
					List<Row> myrows = gridJenisJabatan.getRows().getChildren();
					for (Row row : myrows) {
						jenisJabatans.add(((KlasifikasiSuratKeluarPunyaTembusan) row
								.getAttribute("klasifikasiSuratKeluarPunyaTembusan")).getTembusan());
					}
					AmbilDataJenisJabatanBanyak ambilDataJenisJabatanBanyak = new AmbilDataJenisJabatanBanyak(
							jenisJabatans);
					ambilDataJenisJabatanBanyak.setHeight("95%");
					ambilDataJenisJabatanBanyak.setWidth("90%");
					ambilDataJenisJabatanBanyak
							.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					ambilDataJenisJabatanBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<JenisJabatan> jenisJabatans = (List<JenisJabatan>) arg0.getData();
							for (JenisJabatan jenisJabatan : jenisJabatans) {
								KlasifikasiSuratKeluarPunyaTembusan klasifikasiSuratKeluarPunyaTembusan = new KlasifikasiSuratKeluarPunyaTembusan();
								klasifikasiSuratKeluarPunyaTembusan.setKlasifikasiSuratKeluar(klasifikasiSuratKeluar);
								klasifikasiSuratKeluarPunyaTembusan.setTembusan(jenisJabatan);

								if (klasifikasiSuratKeluar.getId() != null) {
									Session session = HibernateUtil.currentSession();
									session.save(klasifikasiSuratKeluarPunyaTembusan);
								}

								Rows rows = gridJenisJabatan.getRows() == null ? new Rows()
										: gridJenisJabatan.getRows();
								rows.setParent(gridJenisJabatan);
								Row row = new Row();row.setValign("top");
								row.setParent(rows);
								initRow(row, klasifikasiSuratKeluarPunyaTembusan);
							}
						}
					});

					ambilDataJenisJabatanBanyak.onModal();
				}
			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridJenisJabatan);
		gridJenisJabatan.setParent(center);
		gridJenisJabatan.setWidth("100%");
		gridJenisJabatan.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridJenisJabatan);

		MyColumnConfig column = new MyColumnConfig("Jenis Jabatan");
		column.setParent(columns);

		column = new MyColumnConfig("Satuan Kerja");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("20%");

		loadDataDetail(klasifikasiSuratKeluar);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final KlasifikasiSuratKeluar klasifikasiSuratKeluar) {

		List<KlasifikasiSuratKeluarPunyaTembusan> klasifikasiSuratKeluarPunyaTembusans = klasifikasiSuratKeluar == null
				|| klasifikasiSuratKeluar.getId() == null ? new ArrayList<KlasifikasiSuratKeluarPunyaTembusan>()
						: HibernateUtil.currentSession().createCriteria(KlasifikasiSuratKeluarPunyaTembusan.class)
								.add(Restrictions.eq("klasifikasiSuratKeluar", klasifikasiSuratKeluar)).list();

		Rows rows = gridJenisJabatan.getRows() == null ? new Rows() : gridJenisJabatan.getRows();
		rows.setParent(gridJenisJabatan);

		for (KlasifikasiSuratKeluarPunyaTembusan klasifikasiSuratKeluarPunyaTembusan : klasifikasiSuratKeluarPunyaTembusans) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			try {
				initRow(row, klasifikasiSuratKeluarPunyaTembusan);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}
		}
	}

	public void initRow(final Row row, final KlasifikasiSuratKeluarPunyaTembusan klasifikasiSuratKeluarPunyaTembusan)
			throws Exception {
		row.setValign("top");row.setAttribute("klasifikasiSuratKeluarPunyaTembusan", klasifikasiSuratKeluarPunyaTembusan);

		new Label(klasifikasiSuratKeluarPunyaTembusan.getTembusan() == null ? ""
				: klasifikasiSuratKeluarPunyaTembusan.getTembusan().getNama()).setParent(row);

		final AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox();
		ambilDataSatuanKerjaBanbox.setParent(row);
		ambilDataSatuanKerjaBanbox.setWidth("90%");
		ambilDataSatuanKerjaBanbox.setAttribute("satuanKerja", klasifikasiSuratKeluarPunyaTembusan.getSatuanKerja());
		ambilDataSatuanKerjaBanbox.setValue(klasifikasiSuratKeluarPunyaTembusan.getSatuanKerja() == null ? ""
				: klasifikasiSuratKeluarPunyaTembusan.getSatuanKerja().toString());
		ambilDataSatuanKerjaBanbox.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				klasifikasiSuratKeluarPunyaTembusan
						.setSatuanKerja((SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja"));
				row.setValign("top");row.setAttribute("klasifikasiSuratKeluarPunyaTembusan", klasifikasiSuratKeluarPunyaTembusan);
				if (klasifikasiSuratKeluarPunyaTembusan.getId() != null) {
					Session session = HibernateUtil.currentSession();
					session.update(klasifikasiSuratKeluarPunyaTembusan);
				}
			}
		});

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (klasifikasiSuratKeluarPunyaTembusan.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.delete(klasifikasiSuratKeluarPunyaTembusan);
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

}
