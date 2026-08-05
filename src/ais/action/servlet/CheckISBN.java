package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.books.model.Volume;
import com.google.api.services.books.model.Volumes;

import ais.action.master.library.util.BooksSample;
import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaKategoriItem;
import ais.database.model.library.ItemPunyaPengarang;
import ais.database.model.library.ItemTemporary;
import ais.database.model.library.KategoriItem;
import ais.database.model.library.Penerbit;
import ais.database.model.library.Pengarang;

/**
 * Servlet implementation class CheckISBN
 */
public class CheckISBN extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public JsonFactory jsonFactory = new JacksonFactory();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CheckISBN() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static Item simpanVolume(Volume volume, Item paramItem) {
		return simpanVolume(volume, paramItem, "");
	}

	/**
	 * Mengonversi sebuah {@link ItemTemporary} (buku yang sebelumnya sudah diambil dari Google
	 * Book) menjadi {@link Item} yang persisten. {@code ItemTemporary} menyimpan JSON Volume
	 * Google Book secara utuh pada {@code infoLain} (hasil {@code volume.toPrettyString()}),
	 * sehingga objek {@link Volume} dapat direkonstruksi lalu disimpan melalui
	 * {@link #simpanVolume(Volume, Item)} (yang akan MEMAKAI Item lama bila ISBN/googleBookId
	 * sudah ada di katalog, atau membuat Item baru bila belum).
	 *
	 * <p><b>Latar:</b> dialog "Ambil Buku" mengembalikan objek terpilih sebagai {@code Item} /
	 * {@code Volume} (tab "Cari buku") ATAU {@code ItemTemporary} (tab "Daftar buku yang
	 * sebelumnya sudah diambil"). Pemanggil yang menyimpan relasi {@code setItem(Item)} (mis.
	 * Perkuliahan/Kurikulum/JadwalPelajaran/DataPunyaItem) sebelumnya meng-cast PAKSA objek ke
	 * {@code Volume} → {@code ClassCastException} untuk {@code ItemTemporary}, sehingga buku dari
	 * tab tersebut GAGAL disimpan. Method ini menjembatani konversinya.</p>
	 *
	 * @param itemTemporary buku sementara hasil pencarian sebelumnya.
	 * @return {@link Item} persisten, atau {@code null} bila tidak dapat dikonversi.
	 */
	public static Item itemDariItemTemporary(ItemTemporary itemTemporary) {
		if (itemTemporary == null) {
			return null;
		}
		try {
			String infoLain = itemTemporary.getInfoLain();
			if (infoLain == null || infoLain.trim().isEmpty()) {
				return null;
			}
			Volume volume = new JacksonFactory().fromString(infoLain, Volume.class);
			return simpanVolume(volume, new Item());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	public static Item simpanVolume(Volume volume, Item paramItem, String kewords) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Volume.VolumeInfo volumeInfo = volume.getVolumeInfo();

			String isbn10 = "";
			try {
				isbn10 = volumeInfo.getIndustryIdentifiers().get(0).getIdentifier();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:121");

			}

			String isbn13 = "";
			try {
				isbn13 = volumeInfo.getIndustryIdentifiers().get(1).getIdentifier();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:128");

			}

			String lain = "";
			try {
				lain = volumeInfo.getIndustryIdentifiers().get(2).getIdentifier();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:135");

			}

			System.out.println("isbn10 = " + isbn10 + ", isbn13 = " + isbn13 + ", lain = " + lain);

			Item item = (Item) session.createCriteria(Item.class).add(Restrictions.eq("googleBookId", volume.getId()))
					.setMaxResults(1).uniqueResult();

			if (!isbn13.trim().isEmpty()) {
				item = (Item) session.createCriteria(Item.class).add(Restrictions.eq("isbn", isbn13)).setMaxResults(1)
						.uniqueResult();
			}

			if (item == null && !isbn10.trim().isEmpty()) {
				item = (Item) session.createCriteria(Item.class).add(Restrictions.eq("isbn10", isbn10)).setMaxResults(1)
						.uniqueResult();
			}

			if (item == null) {
				item = (paramItem == null ? new Item() : paramItem);
			}

			String imageUrl = "";
			try {
				imageUrl = volumeInfo.getImageLinks().getThumbnail();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:161");
				// TODO: handle exception
			}

			String description = "";
			try {
				description = volumeInfo.getDescription();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:168");
				// TODO: handle exception
			}

			String textSnippet = "";
			try {
				textSnippet = volume.getSearchInfo().getTextSnippet();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:175");
				// TODO: handle exception
			}

			String publisher = "";
			try {
				publisher = volumeInfo.getPublisher();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:182");
				// TODO: handle exception
			}

			String title = "";
			try {
				title = volumeInfo.getTitle();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:189");
				// TODO: handle exception
			}

			String subtitle = "";
			try {
				subtitle = volumeInfo.getSubtitle();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:196");
				// TODO: handle exception
			}

			String publishedDate = "";
			try {
				publishedDate = volumeInfo.getPublishedDate();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:203");
				// TODO: handle exception
			}

			String language = "";
			try {
				language = volumeInfo.getLanguage();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:210");
				// TODO: handle exception
			}

			Integer pageCount = 0;
			try {
				pageCount = volumeInfo.getPageCount();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:217");
				// TODO: handle exception
			}

			String infoLink = "";

			try {
				infoLink = volumeInfo.getInfoLink();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:225");
				// TODO: handle exception
			}

			String infoLain = "";

			try {
				infoLain = volume.toPrettyString();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:233");
				// TODO: handle exception
			}

			String authors = "";
			try {
				authors = volumeInfo.getAuthors().toString().replaceAll("\\[", "").replaceAll("\\]", "")
						.replaceAll("\"", "").replaceAll("\\{", "").replaceAll("\\}", "");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:241");

			}

			String categories = "";
			try {
				categories = volumeInfo.getCategories().toString().replaceAll("\\[", "").replaceAll("\\]", "")
						.replaceAll("\"", "").replaceAll("\\{", "").replaceAll("\\}", "");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:249");
				// TODO: handle exception
			}

			Integer tahun = 0;
			try {
				tahun = Integer.parseInt(publishedDate.substring(0, 4));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:256");
				// TODO: handle exception
			}

			Penerbit penerbit = (Penerbit) session.createCriteria(Penerbit.class).add(publisher == null
					? Restrictions.sqlRestriction("1!=1") : Restrictions.ilike("nama", publisher, MatchMode.EXACT))
					.setMaxResults(1).uniqueResult();

			if (penerbit == null && publisher != null) {
				penerbit = new Penerbit();
				penerbit.setNama(publisher);
				penerbit.setKeterangan(publisher);
				session.getTransaction().begin();
				session.save(penerbit);
				session.getTransaction().commit();
			}

			item.setAbstrak(description);
			item.setAbstrakEn(description);
			item.setBahasa(language);
			item.setCatatan(description);
			item.setDefaultItem(true);
			item.setHalaman(pageCount);
			item.setIsbn(isbn13 + "");
			item.setIsbn10(isbn10 + "");

			if ((isbn13 == null || isbn13.trim().isEmpty()) && (isbn10 == null || isbn10.trim().isEmpty())
					&& !lain.trim().isEmpty()) {
				item.setIsbn(lain);
			}

			item.setJenisItem(LibraryUtil.TEXT);
			item.setKategories(categories);
			item.setLink(infoLink);
			item.setNama(title);
			item.setPenerbit(penerbit);
			item.setPengarangs(authors);
			item.setTahun(tahun);
			item.setTema(subtitle);
			item.setTipeItem(LibraryUtil.TEXTBOOK);
			item.setImageUrl(imageUrl);
			item.setGoogleBookChecked(true);
			item.setInfoLain(infoLain);
			item.setTextSnippet(textSnippet);

			if (kewords != null && !kewords.trim().isEmpty() && kewords.trim().length() > 3
					&& !item.getKewords().toLowerCase().contains(kewords.trim().toLowerCase())) {
				String newKey = item.getKewords().isEmpty() ? kewords.trim()
						: item.getKewords() + ", " + kewords.trim();
				item.setKewords(newKey);
			}

			session.getTransaction().begin();
			session.saveOrUpdate(item);
			session.getTransaction().commit();

			String[] pengs = authors.split(",");

			for (String p : pengs) {
				Pengarang pengarang = (Pengarang) session.createCriteria(Pengarang.class)
						.add(Restrictions.ilike("nama", p, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
				if (pengarang == null) {
					pengarang = new Pengarang();
					pengarang.setNama(p);
					pengarang.setKeterangan(p);
					session.getTransaction().begin();
					session.save(pengarang);
					session.getTransaction().commit();
				}

				ItemPunyaPengarang itemPunyaPengarang = new ItemPunyaPengarang();
				itemPunyaPengarang.setItem(item);
				itemPunyaPengarang.setPengarang(pengarang);
				session.getTransaction().begin();
				session.save(itemPunyaPengarang);
				session.getTransaction().commit();
			}

			String[] cats = categories.split("&");

			for (String p : cats) {
				p = p.trim();
				KategoriItem kategoriItem = (KategoriItem) session.createCriteria(KategoriItem.class)
						.add(Restrictions.ilike("nama", p.trim(), MatchMode.EXACT)).setMaxResults(1).uniqueResult();
				if (kategoriItem == null) {
					kategoriItem = new KategoriItem();
					kategoriItem.setNama(p.trim());
					kategoriItem.setKeterangan(p.trim());
					kategoriItem.setDefaultItem(true);
					kategoriItem.setKode(p.trim());
					session.getTransaction().begin();
					session.save(kategoriItem);
					session.getTransaction().commit();
				}

				ItemPunyaKategoriItem itemPunyaKategoriItem = new ItemPunyaKategoriItem();
				itemPunyaKategoriItem.setItem(item);
				itemPunyaKategoriItem.setKategoriItem(kategoriItem);
				session.getTransaction().begin();
				session.save(itemPunyaKategoriItem);
				session.getTransaction().commit();

			}

			return item;

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			HibernateUtil.rollbackTransaction();
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:367");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:368");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:369");}
			}
		}

		return null;
	}


	public static ItemTemporary simpanVolume(Volume volume, ItemTemporary paramItemTemporary) {
		return simpanVolume(volume, paramItemTemporary, "");
	}

	public static ItemTemporary simpanVolume(Volume volume, ItemTemporary paramItemTemporary, String kewords) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Volume.VolumeInfo volumeInfo = volume.getVolumeInfo();

			String isbn10 = "";
			try {
				isbn10 = volumeInfo.getIndustryIdentifiers().get(0).getIdentifier();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:389");

			}

			String isbn13 = "";
			try {
				isbn13 = volumeInfo.getIndustryIdentifiers().get(1).getIdentifier();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:396");

			}

			String lain = "";
			try {
				lain = volumeInfo.getIndustryIdentifiers().get(2).getIdentifier();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:403");

			}

			System.out.println("isbn10 = " + isbn10 + ", isbn13 = " + isbn13 + ", lain = " + lain);

			ItemTemporary itemTemporary = (ItemTemporary) session.createCriteria(ItemTemporary.class).add(Restrictions.eq("googleBookId", volume.getId()))
					.setMaxResults(1).uniqueResult();

			if (!isbn13.trim().isEmpty()) {
				itemTemporary = (ItemTemporary) session.createCriteria(ItemTemporary.class).add(Restrictions.eq("isbn", isbn13)).setMaxResults(1)
						.uniqueResult();
			}

			if (itemTemporary == null && !isbn10.trim().isEmpty()) {
				itemTemporary = (ItemTemporary) session.createCriteria(ItemTemporary.class).add(Restrictions.eq("isbn10", isbn10)).setMaxResults(1)
						.uniqueResult();
			}

			if (itemTemporary == null) {
				itemTemporary = (paramItemTemporary == null ? new ItemTemporary() : paramItemTemporary);
			}

			String imageUrl = "";
			try {
				imageUrl = volumeInfo.getImageLinks().getThumbnail();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:429");
				// TODO: handle exception
			}

			String description = "";
			try {
				description = volumeInfo.getDescription();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:436");
				// TODO: handle exception
			}

			String textSnippet = "";
			try {
				textSnippet = volume.getSearchInfo().getTextSnippet();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:443");
				// TODO: handle exception
			}

			String publisher = "";
			try {
				publisher = volumeInfo.getPublisher();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:450");
				// TODO: handle exception
			}

			String title = "";
			try {
				title = volumeInfo.getTitle();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:457");
				// TODO: handle exception
			}

			String subtitle = "";
			try {
				subtitle = volumeInfo.getSubtitle();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:464");
				// TODO: handle exception
			}

			String publishedDate = "";
			try {
				publishedDate = volumeInfo.getPublishedDate();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:471");
				// TODO: handle exception
			}

			String language = "";
			try {
				language = volumeInfo.getLanguage();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:478");
				// TODO: handle exception
			}

			Integer pageCount = 0;
			try {
				pageCount = volumeInfo.getPageCount();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:485");
				// TODO: handle exception
			}

			String infoLink = "";

			try {
				infoLink = volumeInfo.getInfoLink();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:493");
				// TODO: handle exception
			}

			String infoLain = "";

			try {
				infoLain = volume.toPrettyString();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:501");
				// TODO: handle exception
			}

			String authors = "";
			try {
				authors = volumeInfo.getAuthors().toString().replaceAll("\\[", "").replaceAll("\\]", "")
						.replaceAll("\"", "").replaceAll("\\{", "").replaceAll("\\}", "");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:509");

			}

			String categories = "";
			try {
				categories = volumeInfo.getCategories().toString().replaceAll("\\[", "").replaceAll("\\]", "")
						.replaceAll("\"", "").replaceAll("\\{", "").replaceAll("\\}", "");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:517");
				// TODO: handle exception
			}

			Integer tahun = 0;
			try {
				tahun = Integer.parseInt(publishedDate.substring(0, 4));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:524");
				// TODO: handle exception
			}

			Penerbit penerbit = (Penerbit) session.createCriteria(Penerbit.class).add(publisher == null
					? Restrictions.sqlRestriction("1!=1") : Restrictions.ilike("nama", publisher, MatchMode.EXACT))
					.setMaxResults(1).uniqueResult();

			if (penerbit == null && publisher != null) {
				penerbit = new Penerbit();
				penerbit.setNama(publisher);
				penerbit.setKeterangan(publisher);
				session.getTransaction().begin();
				session.save(penerbit);
				session.getTransaction().commit();
			}

			itemTemporary.setAbstrak(description);
			itemTemporary.setAbstrakEn(description);
			itemTemporary.setBahasa(language);
			itemTemporary.setCatatan(description);
			itemTemporary.setDefaultItem(true);
			itemTemporary.setHalaman(pageCount);
			itemTemporary.setIsbn(isbn13 + "");
			itemTemporary.setIsbn10(isbn10 + "");

			if ((isbn13 == null || isbn13.trim().isEmpty()) && (isbn10 == null || isbn10.trim().isEmpty())
					&& !lain.trim().isEmpty()) {
				itemTemporary.setIsbn(lain);
			}

			itemTemporary.setJenisItem(LibraryUtil.TEXT);
			itemTemporary.setKategories(categories);
			itemTemporary.setLink(infoLink);
			itemTemporary.setNama(title);
			itemTemporary.setPenerbit(penerbit);
			itemTemporary.setPengarangs(authors);
			itemTemporary.setTahun(tahun);
			itemTemporary.setTema(subtitle);
			itemTemporary.setTipeItem(LibraryUtil.TEXTBOOK);
			itemTemporary.setImageUrl(imageUrl);
			itemTemporary.setGoogleBookChecked(true);
			itemTemporary.setInfoLain(infoLain);
			itemTemporary.setTextSnippet(textSnippet);

			if (kewords != null && !kewords.trim().isEmpty() && kewords.trim().length() > 3
					&& !itemTemporary.getKewords().toLowerCase().contains(kewords.trim().toLowerCase())) {
				String newKey = itemTemporary.getKewords().isEmpty() ? kewords.trim()
						: itemTemporary.getKewords() + ", " + kewords.trim();
				itemTemporary.setKewords(newKey);
			}

			session.getTransaction().begin();
			session.saveOrUpdate(itemTemporary);
			session.getTransaction().commit();

			String[] pengs = authors.split(",");

			for (String p : pengs) {
				Pengarang pengarang = (Pengarang) session.createCriteria(Pengarang.class)
						.add(Restrictions.ilike("nama", p, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
				if (pengarang == null) {
					pengarang = new Pengarang();
					pengarang.setNama(p);
					pengarang.setKeterangan(p);
					session.getTransaction().begin();
					session.save(pengarang);
					session.getTransaction().commit();
				}

				ItemPunyaPengarang itemTemporaryPunyaPengarang = new ItemPunyaPengarang();
				itemTemporaryPunyaPengarang.setItemTemporary(itemTemporary);
				itemTemporaryPunyaPengarang.setPengarang(pengarang);
				session.getTransaction().begin();
				session.save(itemTemporaryPunyaPengarang);
				session.getTransaction().commit();
			}

			String[] cats = categories.split("&");

			for (String p : cats) {
				p = p.trim();
				KategoriItem kategoriItemTemporary = (KategoriItem) session.createCriteria(KategoriItem.class)
						.add(Restrictions.ilike("nama", p.trim(), MatchMode.EXACT)).setMaxResults(1).uniqueResult();
				if (kategoriItemTemporary == null) {
					kategoriItemTemporary = new KategoriItem();
					kategoriItemTemporary.setNama(p.trim());
					kategoriItemTemporary.setKeterangan(p.trim());
					kategoriItemTemporary.setDefaultItem(true);
					kategoriItemTemporary.setKode(p.trim());
					session.getTransaction().begin();
					session.save(kategoriItemTemporary);
					session.getTransaction().commit();
				}

				ItemPunyaKategoriItem itemTemporaryPunyaKategoriItem = new ItemPunyaKategoriItem();
				itemTemporaryPunyaKategoriItem.setItemTemporary(itemTemporary);
				itemTemporaryPunyaKategoriItem.setKategoriItem(kategoriItemTemporary);
				session.getTransaction().begin();
				session.save(itemTemporaryPunyaKategoriItem);
				session.getTransaction().commit();

			}

			return itemTemporary;

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			HibernateUtil.rollbackTransaction();
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:635");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:636");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:637");}
			}
		}

		return null;
	}

	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse resp) throws Exception {

		String isbn = request.getParameter("isbn");

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Integer jumlah = ((Number) session.createCriteria(Item.class)
					.add(Restrictions.or(Restrictions.eq("isbn", isbn), Restrictions.eq("isbn10", isbn)))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (jumlah.equals(0)) {

				try {
					String query = "isbn:" + isbn;

					Volumes volumes = BooksSample.queryGoogleBooks(jsonFactory, query, 0, 1);

					if (volumes.getTotalItems() == 0 || volumes.getItems() == null) {
						return;
					}

					simpanVolume(volumes.getItems().get(0), new Item(), "");

				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:667");
				}

			}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:673");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:674");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/CheckISBN.java:675");}
			}
		}
	}

}
