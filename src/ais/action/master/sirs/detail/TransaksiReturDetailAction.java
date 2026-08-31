package ais.action.master.sirs.detail;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.TransaksiMedisDetail;
import ais.database.model.sirs.TransaksiRetur;
import ais.database.model.sirs.TransaksiReturDetail;

/**
 * Controller/action ZK untuk transaksi retur detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code TransaksiRetur transaksiRetur}, {@code
 * Footer total}, {@code Footer totalQty}, {@code Grid grid}; pembacaan/pencarian ({@code loadData()}); operasi
 * domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyDetail
 */
public class TransaksiReturDetailAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private TransaksiRetur transaksiRetur;
	private Footer total;
	private Footer totalQty;
	private Grid grid;

	public TransaksiReturDetailAction(TransaksiRetur transaksiRetur) {
		super();
		this.transaksiRetur = transaksiRetur;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(TransaksiReturDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class TransaksiReturDetailRenderer extends ais.ui.util.MyRowRenderer {

		public TransaksiReturDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final TransaksiReturDetail transaksiReturDetail = (TransaksiReturDetail) data;
			final TransaksiMedisDetail transaksiDetail = transaksiReturDetail.getTransaksiDetail();
			if (transaksiDetail.getRacikan() == null) {
				new Label(transaksiReturDetail.getItem() == null ? "" : transaksiReturDetail.getItem().getKode())
						.setParent(row);
				RevisiHelper
						.createNewRevisi(TransaksiReturDetail.class, transaksiReturDetail,
								transaksiReturDetail.getItem() == null ? "" : transaksiReturDetail.getItem().getNama())
						.setParent(row);
				new Label(
						transaksiReturDetail.getItem() == null || transaksiReturDetail.getItem().getSatuanItem() == null
								? ""
								: transaksiReturDetail.getItem().getSatuanItem().getNama())
						.setParent(row);
			} else {
				new Label(transaksiDetail.getRacikan() == null ? "" : transaksiDetail.getRacikan().getKode())
						.setParent(row);
				RevisiHelper
						.createNewRevisi(TransaksiReturDetail.class, transaksiReturDetail,
								transaksiDetail.getRacikan() == null ? "" : transaksiDetail.getRacikan().getNama())
						.setParent(row);

				new Label(ais.common.Common.getBahasaConfig("Racikan")).setParent(row);

			}

			new Label(transaksiDetail.getQty() == null ? "" : Common.numberFormat.get().format(transaksiDetail.getQty()))
					.setParent(row);
			new Label(transaksiReturDetail.getQty() == null ? ""
					: Common.numberFormat.get().format(transaksiReturDetail.getQty())).setParent(row);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<TransaksiReturDetail> transaksiReturDetails = session.createCriteria(TransaksiReturDetail.class)
				.addOrder(Order.desc("id")).add(Restrictions.eq("transaksiRetur", transaksiRetur)).list();

		ListModel strset = new SimpleListModel(transaksiReturDetails);
		grid.setRowRenderer(new TransaksiReturDetailRenderer());
		grid.setModel(strset);
		grid.renderAll();

		Double mytotal = 0.0;
		Double mytotalRtr = 0.0;
		for (TransaksiReturDetail transaksiReturDetail : transaksiReturDetails) {
			mytotal += (transaksiReturDetail.getTransaksiDetail() == null
					|| transaksiReturDetail.getTransaksiDetail().getQty() == null ? 0.0
							: transaksiReturDetail.getTransaksiDetail().getQty());

			mytotalRtr += (transaksiReturDetail.getQty() == null ? 0.0 : transaksiReturDetail.getQty());
		}

		total.setLabel(Common.numberFormat.get().format(mytotal));
		totalQty.setLabel(Common.numberFormat.get().format(mytotalRtr));
	}

	private void display() {

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(this);
		groupbox.appendChild(new Caption("Daftar Item Transaksi Retur"));

		grid = new Grid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(groupbox);

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Kode Item");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama Item");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Satuan");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Qty Trns");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Qty Retur");
		column.setAlign("right");
		column.setWidth("10%");

		Foot foot = new Foot();
		foot.setParent(grid);

		foot.appendChild(new Footer());
		foot.appendChild(new Footer("Total Qty"));
		foot.appendChild(new Footer());

		total = new Footer();
		total.setParent(foot);
		total.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		totalQty = new Footer();
		totalQty.setParent(foot);
		totalQty.setStyle("font-weight:bold;font-size:15px;text-align:right;");

		loadData(null);
	}

}
