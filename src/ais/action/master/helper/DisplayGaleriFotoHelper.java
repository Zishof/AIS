package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GaleriFoto;
import ais.database.model.file.GaleriFotoImage;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;

/**
 * Helper tampilan galeri foto: merender seluruh {@link GaleriFotoImage} milik satu {@link GaleriFoto}
 * ke dalam grid tiga kolom (setiap gambar+keterangan mengisi satu sel, baris baru dimulai setiap
 * kelipatan tiga gambar), diurutkan berdasarkan nomor halaman. Data gambar diambil lewat sesi
 * Hibernate terpisah {@link StreamingHibernateUtil}, konsisten dengan entitas lain yang memuat data
 * biner besar.
 */
public class DisplayGaleriFotoHelper {

	private MyGrid gridGaleriFotoImage;

	private Groupbox groupbox;

	/** @param gridGaleriFotoImage grid ZK yang akan diisi kolom dan baris gambar galeri */
	public DisplayGaleriFotoHelper(MyGrid gridGaleriFotoImage) {
		this.gridGaleriFotoImage = gridGaleriFotoImage;
	}

	/**
	 * Membangun groupbox berjudul nama galeri berisi grid tiga kolom, lalu memuat gambar-gambar
	 * galeri ke dalamnya.
	 *
	 * @param galeriFoto galeri foto yang gambarnya ditampilkan
	 * @return groupbox siap ditambahkan ke komponen lain
	 * @throws Exception diteruskan dari kegagalan Hibernate saat memuat gambar
	 */
	public Groupbox initDetail(final GaleriFoto galeriFoto) throws Exception {

		groupbox = new ais.ui.util.MyGroupboxStyled();
		groupbox.appendChild(new MyCaptionStyled(galeriFoto.getNama()));
		Common.clear(gridGaleriFotoImage);
		gridGaleriFotoImage.setParent(groupbox);
		gridGaleriFotoImage.setWidth("100%");
		gridGaleriFotoImage.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridGaleriFotoImage);

		MyColumnConfig column = new MyColumnConfig("");
		column.setParent(columns);

		column = new MyColumnConfig("");
		column.setParent(columns);

		column = new MyColumnConfig("");
		column.setParent(columns);

		loadDataDetail(galeriFoto);

		return groupbox;
	}

	/** Memuat seluruh {@link GaleriFotoImage} milik galeri (terurut nomor halaman) dan menyusunnya ke grid, tiga gambar per baris. */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final GaleriFoto galeriFoto) throws Exception {

		Session session = StreamingHibernateUtil.getInstance().currentSession();
		List<GaleriFotoImage> galeriFotoImages = session.createCriteria(GaleriFotoImage.class)
				.add(Restrictions.eq("galeriFoto", galeriFoto.getId())).addOrder(Order.asc("halaman")).list();

		Common.clear(gridGaleriFotoImage);
		Rows rows = new Rows();
		rows.setParent(gridGaleriFotoImage);
		Row row = new Row();
		row.setValign("top");
		int index = 0;
		for (GaleriFotoImage galeriFotoImage : galeriFotoImages) {
			row = index % 3 == 0 ? new Row() : row;
			row.setHeight("300px");
			row.setParent(rows);
			initRow(row, galeriFotoImage);

			index++;
		}
		StreamingHibernateUtil.getInstance().closeSession();

	}

	/** Mengisi satu sel {@code row} dengan gambar galeri dan keterangannya (HTML). */
	public void initRow(final Row row, final GaleriFotoImage galeriFotoImage) throws Exception {
		row.setValign("top");
		row.setAttribute("galeriFotoImage", galeriFotoImage);

		Vbox vbox = new Vbox();
		vbox.setParent(row);

		Long galeriFoto = galeriFotoImage.getGaleriFoto();

		Image image = new Image(CommonMedia.getGaleriFotoImage(galeriFoto == null ? -1L : galeriFoto,
				galeriFotoImage.getId(), null, null, false));
		image.setWidth("100%");
		image.setParent(vbox);

		String keteranganAman = galeriFotoImage.getKeterangan() == null ? ""
				: org.jsoup.Jsoup.clean(galeriFotoImage.getKeterangan(), org.jsoup.safety.Safelist.basic());
		vbox.appendChild(new ais.ui.util.MyHtml(keteranganAman));

	}

}
