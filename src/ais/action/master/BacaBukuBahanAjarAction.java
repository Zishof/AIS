package ais.action.master;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BukuBahanAjar;
import ais.database.model.file.FileBukuBahanAjar;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHboxToolbar;
import ais.ui.util.MyPanel;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk baca buku bahan ajar. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Paging paging}, {@code Textbox
 * searchnama}, {@code Textbox searchpengarang}, {@code Textbox searchpenerbit}, {@code Textbox searchisbn},
 * {@code Doublebox searchtahun}, {@code MyGrid grid}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code initCriteria()}); pembacaan/pencarian ({@code tampilBuku()}, {@code loadImage()},
 * {@code onSearchDefault()}); operasi domain lain ({@code convertToImage()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class BacaBukuBahanAjarAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private Textbox searchnama;
	private Textbox searchpengarang;
	private Textbox searchpenerbit;
	private Textbox searchisbn;
	private Doublebox searchtahun;

	private MyGrid grid;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class BukuBahanAjarRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BukuBahanAjar bukuBahanAjar = (BukuBahanAjar) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						tampilBuku(bukuBahanAjar, detail);
					}

				}
			});

			new Label(bukuBahanAjar.getNama()).setParent(arg0);

			new Label((bukuBahanAjar.getPengarang1() != null && !bukuBahanAjar.getPengarang1().trim().equals("")
					? bukuBahanAjar.getPengarang1().trim() + ", "
					: "")
					+ (bukuBahanAjar.getPengarang2() != null && !bukuBahanAjar.getPengarang2().trim().equals("")
							? bukuBahanAjar.getPengarang2().trim() + ", "
							: "")
					+ (bukuBahanAjar.getPengarang3() != null && !bukuBahanAjar.getPengarang3().trim().equals("")
							? bukuBahanAjar.getPengarang3().trim() + ", "
							: "")).setParent(arg0);
			new Label(bukuBahanAjar.getIsbn()).setParent(arg0);
			new Label(bukuBahanAjar.getPenerbit()).setParent(arg0);
			new Label(bukuBahanAjar.getLink()).setParent(arg0);
			new Label(bukuBahanAjar.getTahun() == null ? "" : bukuBahanAjar.getTahun() + "").setParent(arg0);
			new Label(bukuBahanAjar.getKeterangan()).setParent(arg0);

		}

	}

	@SuppressWarnings("unchecked")
	private void tampilBuku(final BukuBahanAjar bukuBahanAjar, MyDetail detail) {

		try {

			// final ais.ui.util.MyDiv groupbox = new
			// ais.ui.util.MyDiv();groupbox.setStyle("min-height: 200px;");
			// // groupbox.setHeight("450px");
			// groupbox.setParent(detail);

			MyPanel myPanel = new MyPanel();
			myPanel.setHeight("650px");
			myPanel.setParent(detail);

			final Panelchildren panelchildren = new Panelchildren();
			panelchildren.setParent(myPanel);

			// Panel ZK hanya menerima Panelchildren sebagai child langsung. Toolbar lama
			// dipasang langsung ke Panel sehingga melempar Unsupported child for Panel.
			MyHboxToolbar toolbar = new MyHboxToolbar();
			toolbar.setParent(panelchildren);

			toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Halaman")));
			final Intbox halaman = new Intbox(0);
			toolbar.appendChild(halaman);

			toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
			final Intbox halamansampai = new Intbox(5);
			toolbar.appendChild(halamansampai);

			MyToolbarbutton toolbarbutton = new MyToolbarbutton("fa-book", "Download");

			toolbarbutton.setParent(toolbar);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Session session = StreamingHibernateUtil.getInstance().currentSession();

					final List<FileBukuBahanAjar> fileBukuBahanAjars = session.createCriteria(FileBukuBahanAjar.class)
							.add(Restrictions.eq("bukuBahanAjar", bukuBahanAjar.getId())).addOrder(Order.desc("id"))
							.list();
					if (fileBukuBahanAjars.size() == 0) {
						return;
					}

					FileBukuBahanAjar fileBukuBahanAjar = fileBukuBahanAjars.get(0);

					File file = new File(CommonMedia.getMediaDirectory().getAbsolutePath() + "/"
							+ fileBukuBahanAjar.getId() + "_" + fileBukuBahanAjar.getNama());

					if (file != null && file.exists()) {
						Filedownload.save(CommonMedia.getFileFotoDenganFile(fileBukuBahanAjar, file),
								fileBukuBahanAjar.getType());
					} else {

						try {

							FileBukuBahanAjar myfileBukuBahanAjar = (FileBukuBahanAjar) session
									.createCriteria(FileBukuBahanAjar.class)
									.add(Restrictions.idEq(fileBukuBahanAjar.getId())).uniqueResult();

							Filedownload.save(CommonMedia.getFileFotoDenganFile(myfileBukuBahanAjar, file),
									myfileBukuBahanAjar.getType());

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}
					}

					StreamingHibernateUtil.getInstance().closeSession();
				}

			});

			Session session = StreamingHibernateUtil.getInstance().currentSession();
			final List<FileBukuBahanAjar> fileBukuBahanAjars = session.createCriteria(FileBukuBahanAjar.class)
					.add(Restrictions.eq("bukuBahanAjar", bukuBahanAjar.getId())).addOrder(Order.desc("id")).list();
			if (fileBukuBahanAjars.size() == 0) {
				Html html = new ais.ui.util.MyHtml("<b>Buku tidak ditemukan</b>");
				panelchildren.appendChild(html);
			} else {
				try {
					loadImage(panelchildren, fileBukuBahanAjars.get(0), halaman.getValue(), halamansampai.getValue());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BacaBukuBahanAjarAction.java:224");
					// Common.tampilErrorJikaAdmin(e);
				}
			}

			EventListener myEventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					try {

						if (halamansampai.getValue() < halaman.getValue()) {
							halamansampai.setValue(halaman.getValue() + 5);
						}

						loadImage(panelchildren, fileBukuBahanAjars.get(0), halaman.getValue(),
								halamansampai.getValue());
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

				}
			};

			halaman.addEventListener("onChange", myEventListener);
			halamansampai.addEventListener("onChange", myEventListener);

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void loadImage(Component groupbox, FileBukuBahanAjar fileBukuBahanAjar, int halaman, int sampai)
			throws Exception {

		Common.clear(groupbox);

		File pdfFile = fileBukuBahanAjar.ambilFile();
		File image = convertToImage(fileBukuBahanAjar.getId(), pdfFile, halaman, sampai);

		System.out.println("image = " + image);

		Image myImage = new Image("/tmp/" + image.getName());

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(groupbox);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		row.appendChild(myImage);
	}

	@SuppressWarnings("rawtypes")
	private File convertToImage(Long id, File pdfFile, int startPage, int endPage) {
		File imageFile = new File(
				application.getRealPath("/tmp/" + id + "_" + pdfFile.getName().replaceAll(",", "").replaceAll(" ", "_")
						+ "_" + startPage + "_" + endPage + ".jpg"));
		if (imageFile.exists()) {
			return imageFile;
		}
		System.out.println("Convert to " + imageFile);
		PDDocument document = null;
		int imageType = BufferedImage.TYPE_INT_RGB;
		// String imageFormat = "jpg";
		// String password = "";
		// String outputPrefix = imageFile.getAbsolutePath();
		// String color = "rgb";
		int resolution = 96;

		try {
			imageFile.createNewFile();
			document = PDDocument.load(pdfFile);

			if (startPage > document.getNumberOfPages()) {
				startPage = document.getNumberOfPages();
			}

			if (endPage > document.getNumberOfPages()) {
				endPage = document.getNumberOfPages();
			}

			List pages = document.getDocumentCatalog().getAllPages();

			List<BufferedImage> bufferedImages = new ArrayList<BufferedImage>();
			for (int i = startPage; i <= endPage; i++) {
				try {
					System.out.println("Convert halaman = " + i);
					PDPage page = (PDPage) pages.get(i);

					BufferedImage image = page.convertToImage(imageType, resolution);
					bufferedImages.add(image);
				} catch (Exception e) {
					System.out.println("Convert halaman = " + i + " gagal dilakukan");
					Common.tampilErrorJikaAdmin(e);
				}

				// pdStream.

				// PDJpeg jpeg = new

				// PDFImageWriter imageWriter = new PDFImageWriter();
				//
				//
				//
				// boolean success = imageWriter.writeImage(document,
				// imageFormat,
				// password, i, i, outputPrefix, imageType, resolution);
				// System.out.println("Convert = " + success);
			}

			int rows = bufferedImages.size();
			int cols = 1;
			int chunkWidth, chunkHeight;
			chunkWidth = bufferedImages.get(0).getWidth();
			chunkHeight = bufferedImages.get(0).getHeight();

			BufferedImage finalImg = new BufferedImage(chunkWidth * cols, chunkHeight * rows,
					BufferedImage.TYPE_INT_RGB);

			for (int i = 0; i < rows; i++) {
				for (int j = 0; j < cols; j++) {
					finalImg.createGraphics().drawImage(bufferedImages.get(i), chunkWidth * j, chunkHeight * i, null);
				}
			}

			FileOutputStream out = new FileOutputStream(imageFile);
			ImageIO.write(finalImg, "jpeg", out);
			out.close();

			document.close();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		return imageFile;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(BukuBahanAjar.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(searchpenerbit.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("penerbit", searchpenerbit.getValue(), MatchMode.ANYWHERE))
				.add(searchisbn.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("isbn", searchisbn.getValue(), MatchMode.ANYWHERE))
				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahun", searchtahun.getValue().intValue()))
				.add(searchpengarang.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("pengarang1", searchpengarang.getValue(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("pengarang2", searchpengarang.getValue(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("pengarang3", searchpengarang.getValue(),
												MatchMode.ANYWHERE))));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		if (searchnama == null) {
			return;
		}

		List<BukuBahanAjar> bukuBahanAjar = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(bukuBahanAjar);
		grid.setRowRenderer(new BukuBahanAjarRenderer());
		grid.setModelCheckMobile(strset);

	}

}
