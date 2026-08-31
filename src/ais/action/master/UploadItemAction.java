package ais.action.master;

import java.util.List;
import java.util.Random;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoItem;
import ais.database.model.library.Item;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFileUploadConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk upload item. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Long kodeUnik},
 * {@code MyWindow window}, {@code Long item}, {@code Boolean delete}, {@code Boolean add};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initDetail()}, {@code
 * initRow()}); pembacaan/pencarian ({@code loadDataDetail()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
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
public class UploadItemAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9101904795532419466L;

	private MyGrid grid;
	private Long kodeUnik;
	private MyWindow window;
	private Long item;

	private Boolean delete = true;
	private Boolean add = true;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		// Themes.setTheme(execution, "silvertail");
		kodeUnik = execution.getParameter("kodeUnik") == null ? null
				: Long.parseLong(execution.getParameter("kodeUnik").trim());
		item = execution.getParameter("item") == null ? null : Long.parseLong(execution.getParameter("item").trim());
		delete = execution.getParameter("delete") == null ? false
				: Boolean.parseBoolean(execution.getParameter("delete").trim());
		add = execution.getParameter("add") == null ? false
				: Boolean.parseBoolean(execution.getParameter("add").trim());
		window.appendChild(initDetail(new Item(item == null || item < 0L ? null : item)));
	}

	public Borderlayout initDetail(final Item item) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setVisible(add);
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		Fileupload fileupload = new MyFileUploadConfig("Tambah File", "/img/new.gif");
		fileupload.setParent(toolbar);
		fileupload.setTooltiptext("Tambah");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				try {
					UploadEvent uploadEvent = (UploadEvent) event;
					if (uploadEvent != null) {

						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

						FotoItem fotoItem = new FotoItem();
						fotoItem.setNama(uploadEvent.getMedia().getName());
						fotoItem.setKeterangan(uploadEvent.getMedia().getContentType());
						fotoItem.setItem(item.getId() == null ? new Random(Long.MIN_VALUE).nextLong() : item.getId());

						fotoItem.setFoto(Common.getBlobFromMedia(uploadEvent.getMedia()));
						fotoItem.setKodeUnik(kodeUnik);

						streamingSession.getTransaction().begin();
						streamingSession.save(fotoItem);
						streamingSession.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();

						LibraryUtil.convertLampiranToText(fotoItem);

						Rows rows = grid.getRows() == null ? new Rows() : grid.getRows();
						rows.setParent(grid);
						Row row = new Row();row.setValign("top");
						row.setParent(rows);
						initRow(row, fotoItem);
					}
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
				}

			}
		};
		fileupload.addEventListener("onUpload", eventListener);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Nama");
		column.setParent(columns);

		column = new MyColumnConfig("Jenis");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig("Tanggal");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig("Tampil");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("");
		column.setParent(columns);
		column.setWidth("10%");

		loadDataDetail(item);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Item item) throws Exception {

		Session session = StreamingHibernateUtil.getInstance().currentSession();
		List<FotoItem> fotoItems = session.createCriteria(FotoItem.class)
				.add(Restrictions.or(Restrictions.eq("kodeUnik", kodeUnik), Restrictions.eq("item", item.getId())))
				.addOrder(Order.desc("id")).list();

		Rows rows = grid.getRows() == null ? new Rows() : grid.getRows();
		rows.setParent(grid);

		for (FotoItem fotoItem : fotoItems) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, fotoItem);
		}
		StreamingHibernateUtil.getInstance().closeSession();
	}

	public void initRow(final Row row, final FotoItem fotoItem) throws Exception {
		row.setValign("top");row.setAttribute("fotoItem", fotoItem);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					Session session = StreamingHibernateUtil.getInstance().currentSession();

					FotoItem myfotoItem = (FotoItem) session.createCriteria(FotoItem.class)
							.add(Restrictions.idEq(fotoItem.getId())).uniqueResult();
					Filedownload.save(myfotoItem.ambilFile(), myfotoItem.getKeterangan());

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e); 
				}
			}
		};

		A a = new A(fotoItem.getNama());
		a.setParent(row);
		a.addEventListener("onClick", eventListener);

		a = new A(fotoItem.getKeterangan());
		a.setParent(row);
		a.addEventListener("onClick", eventListener);

		a = new A(Common.dateFormat5.get().format(fotoItem.getTanggal_dirubah()));
		a.setParent(row);
		a.addEventListener("onClick", eventListener);

		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		checkbox.setDisabled(!(add && delete));
		checkbox.setChecked(fotoItem.getDitampilkan());
		checkbox.setParent(row);row.setValign("top");row.setAttribute("checkbox", checkbox);
		checkbox.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				fotoItem.setDitampilkan(checkbox.isChecked());
				if (fotoItem.getId() != null) {
					Session session = StreamingHibernateUtil.getInstance().currentSession();
					session.getTransaction().begin();
					Common.refreshUpdate(session, (fotoItem));
					session.getTransaction().commit();
					StreamingHibernateUtil.getInstance().closeSession();
				}
				row.setValign("top");row.setAttribute("fotoItem", fotoItem);
			}
		});

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/upload.gif");
		button.setTooltiptext("Download");
		button.setParent(hbox);
		button.addEventListener("onClick", eventListener);

		button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
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
							if (fotoItem.getId() != null) {
								Session session = StreamingHibernateUtil.getInstance().currentSession();
								session.getTransaction().begin();
								session.delete(fotoItem);
								session.getTransaction().commit();
								StreamingHibernateUtil.getInstance().closeSession();
							}
							row.setVisible(false);
						}

					}
				});

			}
		});
	}

}
