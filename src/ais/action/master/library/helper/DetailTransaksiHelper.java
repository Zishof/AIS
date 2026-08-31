package ais.action.master.library.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;

import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.Perpustakaan;

/**
 * Helper terfokus untuk detail transaksi. Tipe ini membungkus satu variasi kecil dari alur yang
 * lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi domain lain ({@code dapatkanInfo()}, {@code display()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyDetail
 */
public class DetailTransaksiHelper extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	public DetailTransaksiHelper(final ItemPunyaBarcode itemPunyaBarcode, final Item item,
			final Perpustakaan perpustakaan) {
		super();

		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(DetailTransaksiHelper.this);
				if (isOpen()) {
					display(itemPunyaBarcode, item, perpustakaan);
				}
			}
		});
	}

	public static String dapatkanInfo(DetailTransaksi detailTransaksi) {
		String info = "";
		if (detailTransaksi.getSaldoAwalDetail() != null) {
			info = detailTransaksi.getSaldoAwalDetail().getSaldoAwal().getKode() + " "
					+ detailTransaksi.getSaldoAwalDetail().getKeterangan() + " "
					+ detailTransaksi.getSaldoAwalDetail().getSaldoAwal().getKeterangan();
		} else if (detailTransaksi.getPenerimaanPengadaanItemDetail() != null) {
			info = detailTransaksi.getPenerimaanPengadaanItemDetail().getPenerimaanPengadaanItem().getKode() + " "
					+ detailTransaksi.getPenerimaanPengadaanItemDetail().getKeterangan() + " "
					+ detailTransaksi.getPenerimaanPengadaanItemDetail().getPenerimaanPengadaanItem().getKeterangan();
		} else if (detailTransaksi.getReturPengadaanItemDetail() != null) {
			info = detailTransaksi.getReturPengadaanItemDetail().getReturPengadaanItem().getKode() + " "
					+ detailTransaksi.getReturPengadaanItemDetail().getKeterangan() + " "
					+ detailTransaksi.getReturPengadaanItemDetail().getReturPengadaanItem().getKeterangan();
		} else if (detailTransaksi.getTransferPengadaanItemDetail() != null) {
			info = detailTransaksi.getTransferPengadaanItemDetail().getTransferPengadaanItem().getKode() + " "
					+ detailTransaksi.getTransferPengadaanItemDetail().getKeterangan() + " "
					+ detailTransaksi.getTransferPengadaanItemDetail().getTransferPengadaanItem().getKeterangan();
		} else if (detailTransaksi.getPeminjamanPengadaanItemDetail() != null) {
			info = detailTransaksi.getPeminjamanPengadaanItemDetail().getPeminjamanPengadaanItem().getKode() + " "
					+ detailTransaksi.getPeminjamanPengadaanItemDetail().getKeterangan() + " "
					+ detailTransaksi.getPeminjamanPengadaanItemDetail().getPeminjamanPengadaanItem().getKeterangan()
					+ " "
					+ (detailTransaksi.getAnggota() == null ? ""
							: (detailTransaksi.getAnggota().getKode() + " - "
									+ detailTransaksi.getAnggota().getNama()));
		} else if (detailTransaksi.getKembaliPengadaanItemDetail() != null) {
			info = detailTransaksi.getKembaliPengadaanItemDetail().getKembaliPengadaanItem().getKode() + " "
					+ detailTransaksi.getKembaliPengadaanItemDetail().getKeterangan() + " "
					+ detailTransaksi.getKembaliPengadaanItemDetail().getKembaliPengadaanItem().getKeterangan() + " "
					+ (detailTransaksi.getAnggota() == null ? ""
							: (detailTransaksi.getAnggota().getKode() + " - "
									+ detailTransaksi.getAnggota().getNama()));
		} else if (detailTransaksi.getKoreksiItemDetail() != null) {
			info = detailTransaksi.getKoreksiItemDetail().getKoreksiItem().getKode() + " "
					+ detailTransaksi.getKoreksiItemDetail().getKeterangan() + " "
					+ detailTransaksi.getKoreksiItemDetail().getKoreksiItem().getKeterangan();
		}
		return info;
	}

	public void display(ItemPunyaBarcode itemPunyaBarcode, Item item, Perpustakaan perpustakaan) {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.appendChild(new MyCaptionStyled("Sejarah Transaksi "
				+ (itemPunyaBarcode == null ? "" : itemPunyaBarcode.getBarcode()) + " " + item.getNama()));
		groupbox.setParent(this);

		Session session = HibernateUtil.currentSession();
		@SuppressWarnings("unchecked")
		List<DetailTransaksi> detailTransaksis = session.createCriteria(DetailTransaksi.class)
				.add(itemPunyaBarcode == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("itemPunyaBarcode", itemPunyaBarcode))
				.add(item == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("item", item))
				.add(perpustakaan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("perpustakaan", perpustakaan))

		.addOrder(Order.desc("tanggal")).list();

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(groupbox);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Transaksi");
		column.setParent(columns);

		column = new MyColumnConfig("Tanggal/Waktu");
		column.setParent(columns);

		column = new MyColumnConfig("Qty");
		column.setParent(columns);
		column.setWidth("5%");
		column.setAlign("right");

		column = new MyColumnConfig("Informasi");
		column.setParent(columns);
		column.setWidth("60%");

		Rows rows = new Rows();
		rows.setParent(grid);

		for (DetailTransaksi detailTransaksi : detailTransaksis) {
			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(detailTransaksi.getKodeTransaksi() == null ? ""
					: (detailTransaksi.getKodeTransaksi().getKode() + " - "
							+ detailTransaksi.getKodeTransaksi().getNama())));
			row.appendChild(
					new ais.ui.util.MyLabelConfig(Common.dateFormat3.get().format(detailTransaksi.getTanggalDanWaktu())));

			new Label(Common.numberFormat.get().format(detailTransaksi.getKodeTransaksi().getJenis()
					* (detailTransaksi.getQty() + detailTransaksi.getQtyBonus()))).setParent(row);
			new Label(dapatkanInfo(detailTransaksi)).setParent(row);
		}
	}

}
