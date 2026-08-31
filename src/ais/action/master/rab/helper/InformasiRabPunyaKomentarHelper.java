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
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.InformasiRab;
import ais.database.model.rab.InformasiRabKomentar;

/**
 * Helper ZK untuk menampilkan dan mengelola grid komentar ({@link InformasiRabKomentar}) milik satu
 * {@link InformasiRab} (relasi "punya banyak") pada layar detail info RAB. Membangun kolom grid
 * (Komentar/Oleh/Email/Hapus), memuat baris komentar terkait, dan menyediakan aksi hapus per baris
 * (tombol hapus disembunyikan bila pengguna tidak punya hak {@link CommonPrivilages#DELETE}) lengkap
 * dengan dialog konfirmasi sebelum benar-benar menghapus dari database.
 */
public class InformasiRabPunyaKomentarHelper {

	private MyGrid gridKomentar;
	private boolean delete = false;

	/**
	 * Membuat helper terikat pada satu komponen grid target, sekaligus mengevaluasi hak akses hapus
	 * pengguna saat ini.
	 *
	 * @param gridKomentar komponen grid ZK tempat baris komentar dirender
	 */
	public InformasiRabPunyaKomentarHelper(MyGrid gridKomentar) {
		this.gridKomentar = gridKomentar;
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Membangun kerangka layout detail (kolom grid) dan langsung memuat data komentar untuk info RAB
	 * yang diberikan.
	 *
	 * @param informasiRab info RAB yang komentarnya ditampilkan
	 * @return komponen {@link Borderlayout} berisi grid komentar yang siap dipasang ke layar pemanggil
	 */
	public Borderlayout initDetail(
			final InformasiRab informasiRab) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridKomentar);
		gridKomentar.setParent(center);
		gridKomentar.setWidth("100%");
		gridKomentar.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridKomentar);

		MyColumnConfig column = new MyColumnConfig("Komentar");
		column.setParent(columns);

		column = new MyColumnConfig("Oleh");
		column.setParent(columns);

		column = new MyColumnConfig("Email");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("20%");

		loadDataDetail(informasiRab);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(
			final InformasiRab informasiRab) {

		List<InformasiRabKomentar> informasiRabKomentars = informasiRab == null
				|| informasiRab.getId() == null ? new ArrayList<InformasiRabKomentar>()
				: HibernateUtil
						.currentSession()
						.createCriteria(InformasiRabKomentar.class)
						.add(Restrictions.eq("informasiRab",
								informasiRab)).list();

		Rows rows = gridKomentar.getRows() == null ? new Rows() : gridKomentar
				.getRows();
		rows.setParent(gridKomentar);

		for (InformasiRabKomentar informasiRabKomentar : informasiRabKomentars) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, informasiRabKomentar);
		}
	}

	/**
	 * Mengisi satu baris grid dengan data komentar (nama, kontak, email) dan memasang tombol hapus
	 * beserta event handler-nya (menampilkan dialog konfirmasi, menghapus baris dari database dan
	 * dari grid bila dikonfirmasi).
	 *
	 * @param row                 baris grid yang diisi
	 * @param informasiRabKomentar data komentar yang direpresentasikan baris ini
	 */
	public void initRow(final Row row,
			final InformasiRabKomentar informasiRabKomentar) {
		row.setValign("top");row.setAttribute("informasiRabKomentar",
				informasiRabKomentar);

		new Label(informasiRabKomentar.getNama()).setParent(row);
		new Label(informasiRabKomentar.getKontak()).setParent(row);
		new Label(informasiRabKomentar.getEmail()).setParent(row);

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
									if (informasiRabKomentar.getId() != null) {
										Session session = HibernateUtil
												.currentSession();
										session.delete(informasiRabKomentar);
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

}
