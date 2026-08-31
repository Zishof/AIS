package ais.action.master.lkp.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.rab.helper.AmbilDataIndikatorBanyak;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.lkp.KegiatanTugasJabatan;
import ais.database.model.lkp.KegiatanTugasJabatanPunyaIndikator;
import ais.database.model.lkp.SatuanKegiatanTugasJabatan;
import ais.database.model.rab.Indikator;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper pengelola grid indikator kinerja pada satu kegiatan tugas jabatan
 * ({@link KegiatanTugasJabatan}, modul LKP/Laporan Kinerja Pegawai) — menghubungkan kegiatan
 * tersebut ke satu atau lebih {@link Indikator} RAB beserta target capaian
 * ({@link KegiatanTugasJabatanPunyaIndikator}: nilai target, satuan, output). Toolbar "Tambah
 * Indikator" membuka picker massal ({@link AmbilDataIndikatorBanyak}, mengecualikan indikator yang
 * sudah terpasang) agar beberapa indikator dapat ditambahkan sekaligus.
 */
public class KegiatanTugasJabatanPunyaIndikatorHelper {

	private MyGrid gridIndikator;
	private boolean add = false;
	private boolean edit = false;
	private boolean delete = false;

	/** Membuat helper yang akan mengelola isi {@code gridIndikator}; hak tambah/ubah/hapus ditentukan dari {@link CommonPrivilages} saat ini. */
	public KegiatanTugasJabatanPunyaIndikatorHelper(MyGrid gridIndikator) {
		this.gridIndikator = gridIndikator;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun panel indikator untuk {@code kegiatanTugasJabatan}: toolbar tambah indikator massal
	 * (bila berhak) dan grid menampilkan seluruh indikator yang sudah terpasang.
	 *
	 * @param kegiatanTugasJabatan kegiatan tugas jabatan target
	 * @return borderlayout siap ditambahkan sebagai panel tab/jendela
	 */
	public Borderlayout initDetail(final KegiatanTugasJabatan kegiatanTugasJabatan) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Indikator", "/img/new.gif");
		add.setVisible(KegiatanTugasJabatanPunyaIndikatorHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Indikator> indikators = new ArrayList<Indikator>();
				List<Row> myrows = gridIndikator.getRows().getChildren();
				for (Row row : myrows) {
					indikators.add(((KegiatanTugasJabatanPunyaIndikator) row
							.getAttribute("kegiatanTugasJabatanPunyaIndikator")).getIndikator());
				}
				AmbilDataIndikatorBanyak ambilDataIndikatorBanyak = new AmbilDataIndikatorBanyak(indikators);
				ambilDataIndikatorBanyak.setHeight("95%");
				ambilDataIndikatorBanyak.setWidth("750px");
				ambilDataIndikatorBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataIndikatorBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Indikator> indikators = (List<Indikator>) arg0.getData();
						for (Indikator indikator : indikators) {
							KegiatanTugasJabatanPunyaIndikator kegiatanTugasJabatanPunyaIndikator = new KegiatanTugasJabatanPunyaIndikator();
							kegiatanTugasJabatanPunyaIndikator.setKegiatanTugasJabatan(kegiatanTugasJabatan);
							kegiatanTugasJabatanPunyaIndikator.setIndikator(indikator);

							Rows rows = gridIndikator.getRows() == null ? new Rows() : gridIndikator.getRows();
							rows.setParent(gridIndikator);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, kegiatanTugasJabatanPunyaIndikator);
						}
					}
				});

				ambilDataIndikatorBanyak.onModal();

			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridIndikator);
		gridIndikator.setParent(center);
		gridIndikator.setWidth("100%");
		gridIndikator.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridIndikator);

		MyColumnConfig column = new MyColumnConfig("Kode");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Isi Indikator");
		column.setParent(columns);

		column = new MyColumnConfig("Target");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Satuan");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Output Indikator");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("10%");

		loadDataDetail(kegiatanTugasJabatan);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final KegiatanTugasJabatan kegiatanTugasJabatan) {

		List<KegiatanTugasJabatanPunyaIndikator> kegiatanTugasJabatanPunyaIndikators = kegiatanTugasJabatan == null
				|| kegiatanTugasJabatan.getId() == null ? new ArrayList<KegiatanTugasJabatanPunyaIndikator>()
						: HibernateUtil.currentSession().createCriteria(KegiatanTugasJabatanPunyaIndikator.class)
								.add(Restrictions.eq("kegiatanTugasJabatan", kegiatanTugasJabatan)).list();

		Rows rows = gridIndikator.getRows() == null ? new Rows() : gridIndikator.getRows();
		rows.setParent(gridIndikator);

		for (KegiatanTugasJabatanPunyaIndikator kegiatanTugasJabatanPunyaIndikator : kegiatanTugasJabatanPunyaIndikators) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, kegiatanTugasJabatanPunyaIndikator);
		}
	}

	/**
	 * Mengisi satu baris grid dengan kode dan nama indikator, nilai target (dapat diedit),
	 * combobox satuan, dan field output.
	 *
	 * @param row                                     baris grid target
	 * @param kegiatanTugasJabatanPunyaIndikator       data relasi indikator untuk baris ini
	 */
	public void initRow(final Row row, final KegiatanTugasJabatanPunyaIndikator kegiatanTugasJabatanPunyaIndikator) {
		row.setValign("top");row.setAttribute("kegiatanTugasJabatanPunyaIndikator", kegiatanTugasJabatanPunyaIndikator);

		new Label(kegiatanTugasJabatanPunyaIndikator.getIndikator() == null ? ""
				: kegiatanTugasJabatanPunyaIndikator.getIndikator().getKode()).setParent(row);

		new Label(kegiatanTugasJabatanPunyaIndikator.getIndikator() == null ? ""
				: kegiatanTugasJabatanPunyaIndikator.getIndikator().getNama()).setParent(row);

		final MyDoublebox target = new MyDoublebox(kegiatanTugasJabatanPunyaIndikator.getNilaiTarget());
		target.setParent(row);
		target.setDisabled(!edit);
		target.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				kegiatanTugasJabatanPunyaIndikator.setNilaiTarget(target.getValue());
				row.setValign("top");row.setAttribute("kegiatanTugasJabatanPunyaIndikator", kegiatanTugasJabatanPunyaIndikator);
			}
		});

		final Combobox satuan = new Combobox();
		satuan.setParent(row);
		satuan.setWidth("90%");
		Common.insertCombo(satuan, "nama", SatuanKegiatanTugasJabatan.class);
		Common.selectComboItem(satuan, kegiatanTugasJabatanPunyaIndikator.getSatuan());
		satuan.setDisabled(!edit);
		satuan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				kegiatanTugasJabatanPunyaIndikator
						.setSatuan((SatuanKegiatanTugasJabatan) (satuan.getSelectedItem() == null ? null
								: satuan.getSelectedItem().getValue()));
				row.setValign("top");row.setAttribute("kegiatanTugasJabatanPunyaIndikator", kegiatanTugasJabatanPunyaIndikator);
			}
		});

		final MyTextbox output = new MyTextbox(kegiatanTugasJabatanPunyaIndikator.getOutput());
		output.setParent(row);
		output.setRows(2);
		output.setDisabled(!edit);
		output.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				kegiatanTugasJabatanPunyaIndikator.setOutput(output.getValue());
				row.setValign("top");row.setAttribute("kegiatanTugasJabatanPunyaIndikator", kegiatanTugasJabatanPunyaIndikator);
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
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							if (kegiatanTugasJabatanPunyaIndikator.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.delete(kegiatanTugasJabatanPunyaIndikator);
							}
							row.setVisible(false);
						}

					}
				});

			}
		});
	}

}
