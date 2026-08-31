package ais.action.master.rab.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
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

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.RenstraProgram;
import ais.database.model.rab.RenstraProgramPunyaIndikator;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

/**
 * Helper UI untuk mengelola daftar indikator (dengan target dan anggaran per tahun, 5 tahun
 * rencana strategis) satu {@link RenstraProgram} modul RAB, ditampilkan sebagai grid tambah/hapus
 * di dalam layar detail program renstra. Setiap baris menyunting langsung di grid (bukan dialog
 * terpisah): perubahan pada kolom target/anggaran/indikator/lokasi/keterangan langsung ditulis ke
 * objek {@link RenstraProgramPunyaIndikator} pada event {@code onChange} dan kolom total anggaran
 * (jumlah anggaran1..5) dihitung ulang otomatis. <b>Catatan</b>: penulisan ke objek in-memory ini
 * TIDAK langsung menyimpan ke database (baris kode {@code session.update(...)} dikomentari) —
 * persistensi baru terjadi saat pemanggil layar induk menyimpan formulir secara keseluruhan. Hapus
 * baris langsung menghapus dari database (bila sudah tersimpan) setelah konfirmasi. Visibilitas
 * tombol tambah/hapus mengikuti hak akses pengguna.
 */
public class RenstraProgramPunyaIndikatorHelper {

	private MyGrid gridParameter;
	private boolean add = false;
	private boolean delete = false;

	/** Membuat helper terikat ke {@code gridParameter}, menentukan visibilitas tombol tambah/hapus dari hak akses pengguna saat ini. */
	public RenstraProgramPunyaIndikatorHelper(MyGrid gridParameter) {
		this.gridParameter = gridParameter;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun tata letak grid indikator lengkap dengan toolbar tambah dan kolom-kolom indikator,
	 * lokasi, target tahun 1-5, anggaran tahun 1-5, total, keterangan, dan hapus; langsung memuat
	 * data indikator {@code renstraProgram} yang sudah ada.
	 *
	 * @param renstraProgram program renstra yang indikatornya akan ditampilkan/dikelola
	 * @return tata letak {@link Borderlayout} siap ditempel ke komponen induk
	 */
	public Borderlayout initDetail(final RenstraProgram renstraProgram) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Indikator",
				"/img/new.gif");
		add.setVisible(RenstraProgramPunyaIndikatorHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				RenstraProgramPunyaIndikator renstraProgramPunyaIndikator = new RenstraProgramPunyaIndikator();
				renstraProgramPunyaIndikator.setRenstraProgram(renstraProgram);

				Rows rows = gridParameter.getRows() == null ? new Rows()
						: gridParameter.getRows();
				rows.setParent(gridParameter);
				Row row = new Row();row.setValign("top");
				row.setParent(rows);
				initRow(row, renstraProgramPunyaIndikator);
			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridParameter);
		gridParameter.setParent(center);
		gridParameter.setWidth("100%");
		gridParameter.setHeight("100%");
		// ais.ui.util.ZkCompat.setFixedLayout(gridParameter, false);
		Columns columns = new Columns();
		columns.setParent(gridParameter);

		MyColumnConfig column = new MyColumnConfig("Indikator");
		column.setParent(columns);
		column.setWidth("150px");

		column = new MyColumnConfig("Lokasi");
		column.setParent(columns);
		column.setWidth("150px");

		column = new MyColumnConfig("Tahun 1");
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig("Tahun 2");
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig("Tahun 3");
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig("Tahun 4");
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig("Tahun 5");
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig("Anggaran 1");
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig("Anggaran 2");
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig("Anggaran 3");
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig("Anggaran 4");
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig("Anggaran 5");
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig("Total");
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig("keterangan");
		column.setParent(columns);
		column.setWidth("200px");

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("50px");

		loadDataDetail(renstraProgram);

		return borderlayout;
	}

	/** Memuat seluruh baris indikator tersimpan milik {@code renstraProgram} ke grid, atau tidak menambah baris apa pun bila program belum tersimpan. */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final RenstraProgram renstraProgram) {

		List<RenstraProgramPunyaIndikator> renstraProgramPunyaIndikators = renstraProgram == null
				|| renstraProgram.getId() == null ? new ArrayList<RenstraProgramPunyaIndikator>()
				: HibernateUtil.currentSession()
						.createCriteria(RenstraProgramPunyaIndikator.class)
						.add(Restrictions.eq("renstraProgram", renstraProgram))
						.list();

		Rows rows = gridParameter.getRows() == null ? new Rows()
				: gridParameter.getRows();
		rows.setParent(gridParameter);

		for (RenstraProgramPunyaIndikator renstraProgramPunyaIndikator : renstraProgramPunyaIndikators) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, renstraProgramPunyaIndikator);
		}
	}

	/**
	 * Mengisi satu baris grid dengan field-field indikator (indikator, lokasi, target tahun 1-5,
	 * anggaran tahun 1-5, total dihitung otomatis, keterangan) dan tombol hapus. Setiap field
	 * memasang listener {@code onChange} yang menulis nilai barunya ke objek
	 * {@code renstraProgramPunyaIndikator} in-memory (belum tentu langsung tersimpan ke database —
	 * lihat catatan javadoc kelas) dan memperbarui tampilan total anggaran. Tombol hapus meminta
	 * konfirmasi lalu, bila disetujui, menghapus baris dari database (bila sudah tersimpan) dan
	 * melepas baris dari grid.
	 */
	public void initRow(final Row row,
			final RenstraProgramPunyaIndikator renstraProgramPunyaIndikator) {
		row.setValign("top");row.setAttribute("renstraProgramPunyaIndikator",
				renstraProgramPunyaIndikator);

		final MyTextbox indikator = new MyTextbox(
				renstraProgramPunyaIndikator.getIndikator());
		indikator.setWidth("90%");
		indikator.setParent(row);
		indikator.setRows(2);

		final MyTextbox lokasi = new MyTextbox(
				renstraProgramPunyaIndikator.getLokasi());
		lokasi.setWidth("90%");
		lokasi.setParent(row);
		lokasi.setRows(2);

		final MyDoublebox target1 = new MyDoublebox(
				renstraProgramPunyaIndikator.getTarget1());
		target1.setWidth("90%");
		target1.setParent(row);

		final MyDoublebox target2 = new MyDoublebox(
				renstraProgramPunyaIndikator.getTarget2());
		target2.setWidth("90%");
		target2.setParent(row);

		final MyDoublebox target3 = new MyDoublebox(
				renstraProgramPunyaIndikator.getTarget3());
		target3.setWidth("90%");
		target3.setParent(row);

		final MyDoublebox target4 = new MyDoublebox(
				renstraProgramPunyaIndikator.getTarget4());
		target4.setWidth("90%");
		target4.setParent(row);

		final MyDoublebox target5 = new MyDoublebox(
				renstraProgramPunyaIndikator.getTarget5());
		target5.setWidth("90%");
		target5.setParent(row);

		final MyDoublebox anggaran1 = new MyDoublebox(
				renstraProgramPunyaIndikator.getAnggaran1());
		anggaran1.setWidth("90%");
		anggaran1.setParent(row);

		final MyDoublebox anggaran2 = new MyDoublebox(
				renstraProgramPunyaIndikator.getAnggaran2());
		anggaran2.setWidth("90%");
		anggaran2.setParent(row);

		final MyDoublebox anggaran3 = new MyDoublebox(
				renstraProgramPunyaIndikator.getAnggaran3());
		anggaran3.setWidth("90%");
		anggaran3.setParent(row);

		final MyDoublebox anggaran4 = new MyDoublebox(
				renstraProgramPunyaIndikator.getAnggaran4());
		anggaran4.setWidth("90%");
		anggaran4.setParent(row);

		final MyDoublebox anggaran5 = new MyDoublebox(
				renstraProgramPunyaIndikator.getAnggaran5());
		anggaran5.setWidth("90%");
		anggaran5.setParent(row);

		Double mytotal = renstraProgramPunyaIndikator.getAnggaran1()
				+ renstraProgramPunyaIndikator.getAnggaran2()
				+ renstraProgramPunyaIndikator.getAnggaran3()
				+ renstraProgramPunyaIndikator.getAnggaran4()
				+ renstraProgramPunyaIndikator.getAnggaran5();

		final Label total = new Label(Common.numberFormat.get().format(mytotal));
		total.setParent(row);

		final MyTextbox keterangan = new MyTextbox(
				renstraProgramPunyaIndikator.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setRows(2);
		keterangan.setParent(row);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				renstraProgramPunyaIndikator.setAnggaran1(anggaran1.getValue());
				renstraProgramPunyaIndikator.setAnggaran2(anggaran2.getValue());
				renstraProgramPunyaIndikator.setAnggaran3(anggaran3.getValue());
				renstraProgramPunyaIndikator.setAnggaran4(anggaran4.getValue());
				renstraProgramPunyaIndikator.setAnggaran5(anggaran5.getValue());

				renstraProgramPunyaIndikator.setTarget1(target1.getValue());
				renstraProgramPunyaIndikator.setTarget2(target2.getValue());
				renstraProgramPunyaIndikator.setTarget3(target3.getValue());
				renstraProgramPunyaIndikator.setTarget4(target4.getValue());
				renstraProgramPunyaIndikator.setTarget5(target5.getValue());

				renstraProgramPunyaIndikator.setLokasi(lokasi.getValue());
				renstraProgramPunyaIndikator.setIndikator(indikator.getValue());
				renstraProgramPunyaIndikator.setKeterangan(keterangan
						.getValue());

				Double mytotal = renstraProgramPunyaIndikator.getAnggaran1()
						+ renstraProgramPunyaIndikator.getAnggaran2()
						+ renstraProgramPunyaIndikator.getAnggaran3()
						+ renstraProgramPunyaIndikator.getAnggaran4()
						+ renstraProgramPunyaIndikator.getAnggaran5();
				total.setValue(Common.numberFormat.get().format(mytotal));
				//
				// Session session = HibernateUtil.currentSession();
				// session.update(renstraProgramPunyaIndikator);

				row.setValign("top");row.setAttribute("renstraProgramPunyaIndikator",
						renstraProgramPunyaIndikator);
			}
		};

		anggaran1.addEventListener("onChange", eventListener);
		anggaran2.addEventListener("onChange", eventListener);
		anggaran3.addEventListener("onChange", eventListener);
		anggaran4.addEventListener("onChange", eventListener);
		anggaran5.addEventListener("onChange", eventListener);

		target1.addEventListener("onChange", eventListener);
		target2.addEventListener("onChange", eventListener);
		target3.addEventListener("onChange", eventListener);
		target4.addEventListener("onChange", eventListener);
		target5.addEventListener("onChange", eventListener);

		indikator.addEventListener("onChange", eventListener);
		lokasi.addEventListener("onChange", eventListener);
		keterangan.addEventListener("onChange", eventListener);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									if (renstraProgramPunyaIndikator.getId() != null) {
										Session session = HibernateUtil
												.currentSession();
										session.delete(renstraProgramPunyaIndikator);
									}
	row.setVisible(false);row.detach();
								}

							}
						});
			}
		});
	}

}
