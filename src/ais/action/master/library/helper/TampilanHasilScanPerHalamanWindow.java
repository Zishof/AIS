package ais.action.master.library.helper;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import ais.ui.util.MyButtonConfig;
import org.zkoss.zul.Center;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.South;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoImagePerHalamanItem;
import ais.database.model.library.Item;

/**
 * Tipe khusus untuk tampilan hasil scan per halaman window. Kelas ini memberi nama dan batas
 * tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah inisialisasi/lifecycle ({@code init()}); pembacaan/pencarian
 * ({@code loadImage()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class TampilanHasilScanPerHalamanWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5318050237585436165L;

	public TampilanHasilScanPerHalamanWindow() {
		super();
	}

	public TampilanHasilScanPerHalamanWindow(String title, String border,
			boolean closable) {
		super(title, border, closable);
	}

	public void init(Item item) throws Exception {

		Session session = StreamingHibernateUtil.getInstance().currentSession();

		FotoImagePerHalamanItem fotoImagePerHalamanItemPertama = (FotoImagePerHalamanItem) session
				.createCriteria(FotoImagePerHalamanItem.class)
				.add(Restrictions.eq("item", item.getId())).setMaxResults(1)
				.addOrder(Order.asc("halamanIndex")).uniqueResult();

		StreamingHibernateUtil.getInstance().closeSession();

		if (fotoImagePerHalamanItemPertama == null) {
			MyMessageboxConfig.show("Isi (content) dari item ini belum tersedia",
					"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			detach();
			return;
		}

		setWidth("550px");
		setHeight("99%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		final Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		loadImage(item, fotoImagePerHalamanItemPertama, center);

	}

	private void loadImage(final Item item,
			final FotoImagePerHalamanItem fotoImagePerHalamanItem,
			final Center parent) throws Exception {

		Common.clear(parent);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(parent);

		final Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		Long myitem = fotoImagePerHalamanItem.getItem();

		Image image = new Image(CommonMedia.getImageItemPerHalaman(
				myitem == null ? -1L : myitem, fotoImagePerHalamanItem.getId(),
				fotoImagePerHalamanItem.getHalaman(), 400, 650, false));
		image.setWidth("100%");
		image.setParent(center);

		South south = new South();
		south.setParent(borderlayout);
		south.setHeight("25px");

		Hbox hbox = new Hbox();
		hbox.setWidth("100%");
		hbox.setAlign("center");
		hbox.setPack("center");
		hbox.setParent(south);

		MyButtonConfig back = new MyButtonConfig("Back", "/img/Actions-go-back-icon.png");
		back.setParent(hbox);
		hbox.appendChild(new Label(" Halaman : "
				+ fotoImagePerHalamanItem.getHalaman() + " "));
		MyButtonConfig next = new MyButtonConfig("Next", "/img/Actions-go-next-icon.png");
		next.setParent(hbox);

		back.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = StreamingHibernateUtil.getInstance()
						.currentSession();

				FotoImagePerHalamanItem foto = (FotoImagePerHalamanItem) session
						.createCriteria(FotoImagePerHalamanItem.class)
						.add(Restrictions.eq("item", item.getId()))
						.add(Restrictions.lt("halamanIndex",
								fotoImagePerHalamanItem.getHalamanIndex()))
						.setMaxResults(1).addOrder(Order.desc("halamanIndex"))
						.uniqueResult();
				StreamingHibernateUtil.getInstance().closeSession();

				if (foto == null) {
					MyMessageboxConfig.show("Halaman sebelumnya tidak ditemuan",
							"Pemberitahuan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
				} else {
					loadImage(item, foto, parent);
				}

			}
		});

		next.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = StreamingHibernateUtil.getInstance()
						.currentSession();

				FotoImagePerHalamanItem foto = (FotoImagePerHalamanItem) session
						.createCriteria(FotoImagePerHalamanItem.class)
						.add(Restrictions.eq("item", item.getId()))
						.add(Restrictions.gt("halamanIndex",
								fotoImagePerHalamanItem.getHalamanIndex()))
						.setMaxResults(1).addOrder(Order.asc("halamanIndex"))
						.uniqueResult();
				StreamingHibernateUtil.getInstance().closeSession();

				if (foto == null) {
					MyMessageboxConfig.show("Halaman selanjutnya tidak ditemuan",
							"Pemberitahuan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
				} else {
					loadImage(item, foto, parent);
				}

			}
		});

	}

}
