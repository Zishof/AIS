package ais.action.servlet;

import java.io.IOException;
import java.util.List;

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
 * Servlet endpoint yang, diberi parameter {@code isbn}, memeriksa apakah sudah ada {@link Item}
 * dengan ISBN (13 atau 10 digit) tersebut di katalog perpustakaan; bila belum, mencari ISBN
 * tersebut lewat Google Books API ({@link BooksSample#queryGoogleBooks}) dan, bila ditemukan,
 * langsung membuat {@link Item} baru dari hasil pertama lewat {@link #simpanVolume(Volume, Item,
 * String)}. Respons HTTP servlet ini sendiri selalu kosong (lihat {@link #process}) — efek
 * utamanya adalah penulisan ke database, bukan badan respons.
 *
 * <p><b>Catatan ironis dokumentasi:</b> nama {@code CheckISBN} dan komentar
 * "Servlet implementation class CheckISBN" adalah stub baku generator Eclipse lama yang, karena
 * disalin-tempel, muncul sebagai Javadoc kelas pada BANYAK servlet lain di paket ini (termasuk
 * {@code BniForwarder} dan {@code BniForwarderLagi} sebelum diperbaiki) — meninggalkan jejak
 * artefak tersebut di file aslinya sendiri. Javadoc kelas ini kini sudah diperbaiki agar sesuai
 * isi sebenarnya.</p>
 *
 * <p><b>Kegunaan lebih luas:</b> selain sebagai servlet, kelas ini juga menyediakan method static
 * {@link #simpanVolume(Volume, Item, String)}/{@link #simpanVolume(Volume, ItemTemporary,
 * String)} dan overload-nya yang dipanggil dari alur lain di aplikasi (mis. dialog "Ambil Buku")
 * untuk mengonversi hasil pencarian Google Books ({@link Volume}) menjadi entitas
 * {@link Item}/{@link ItemTemporary} yang persisten — lihat {@link #itemDariItemTemporary(
 * ItemTemporary)} untuk jembatan konversi {@link ItemTemporary} → {@link Item}.</p>
 */
public class CheckISBN extends HttpServlet {
	/** Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} servlet ini. */
	private static final long serialVersionUID = 1L;

	/**
	 * Factory JSON Jackson yang dipakai {@link #process} untuk memanggil
	 * {@link BooksSample#queryGoogleBooks(JsonFactory, String, int, int)}. Field instance
	 * (bukan {@code static}) meski nilainya selalu sama untuk setiap instance servlet, karena
	 * mengikuti pola default generator servlet Eclipse.
	 */
	public JsonFactory jsonFactory = new JacksonFactory();

	/**
	 * Mengambil satu nilai {@code identifier} (mis. ISBN-10/ISBN-13/identifier industri lain)
	 * dari daftar {@code industryIdentifiers} milik {@link Volume.VolumeInfo} pada indeks
	 * tertentu, dengan penjagaan penuh terhadap {@code null}/indeks di luar batas sehingga
	 * PERNAH melempar exception.
	 *
	 * @param info  info volume Google Books; boleh {@code null}
	 * @param index indeks pada {@code info.getIndustryIdentifiers()} yang ingin diambil (Google
	 *              Books biasanya menaruh ISBN_10 pada indeks 0 dan ISBN_13 pada indeks 1)
	 * @return nilai {@code identifier} pada indeks tersebut, atau string kosong ({@code ""}) bila
	 *         {@code info}/daftar identifier/elemen pada indeks tersebut {@code null} atau indeks
	 *         di luar batas
	 */
	private static String ambilIdentifier(Volume.VolumeInfo info, int index) {
		if (info == null || info.getIndustryIdentifiers() == null || index < 0
				|| index >= info.getIndustryIdentifiers().size()
				|| info.getIndustryIdentifiers().get(index) == null
				|| info.getIndustryIdentifiers().get(index).getIdentifier() == null) {
			return "";
		}
		return info.getIndustryIdentifiers().get(index).getIdentifier();
	}

	/**
	 * Menggabungkan sebuah {@link List} nilai string (mis. daftar penulis/kategori dari Google
	 * Books) menjadi satu string tunggal dipisah koma, dengan membuang karakter pembungkus hasil
	 * {@link List#toString()} ({@code [ ] { } "}) apa adanya — BUKAN parsing/escaping yang benar
	 * secara umum, hanya cocok untuk teks yang sudah diketahui tidak memuat karakter-karakter
	 * tersebut secara alami.
	 *
	 * @param nilai daftar nilai yang akan digabung; boleh {@code null}
	 * @return string gabungan dipisah {@code ", "} tanpa karakter {@code [ ] { } "}, atau string
	 *         kosong ({@code ""}) bila {@code nilai} {@code null}
	 */
	private static String gabungkan(List<String> nilai) {
		return nilai == null ? "" : nilai.toString().replaceAll("\\[", "").replaceAll("\\]", "")
				.replaceAll("\"", "").replaceAll("\\{", "").replaceAll("\\}", "");
	}

	/**
	 * Konstruktor default tanpa argumen, hanya meneruskan ke {@link HttpServlet#HttpServlet()}.
	 * Tidak ada state khusus yang diinisialisasi di sini ({@link #jsonFactory} diinisialisasi
	 * langsung pada deklarasi field-nya).
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public CheckISBN() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani GET dengan mendelegasikan ke {@link #process}; kegagalan apa pun ditelan dan
	 * hanya ditampilkan ke pengguna bila konteks saat ini adalah administrator, lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @param request  request HTTP masuk; parameter {@code isbn} dibaca oleh {@link #process}
	 * @param response response HTTP keluar; badannya TIDAK pernah diisi oleh {@link #process}
	 *                 (efek utamanya adalah penulisan ke database)
	 * @throws ServletException tidak pernah dilempar keluar karena {@link #process} dibungkus
	 *                          try/catch di sini; dipertahankan hanya karena tanda tangan
	 *                          {@link HttpServlet#doGet}
	 * @throws IOException      idem, ditelan oleh blok catch
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
	 * Menangani POST dengan perilaku identik seperti {@link #doGet}: mendelegasikan ke
	 * {@link #process} dan menelan kegagalan lewat {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @param request  request HTTP masuk; parameter {@code isbn} dibaca oleh {@link #process}
	 * @param response response HTTP keluar; badannya TIDAK pernah diisi, lihat catatan pada
	 *                 {@link #doGet}
	 * @throws ServletException tidak pernah dilempar keluar, lihat catatan pada {@link #doGet}
	 * @throws IOException      idem
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

	/**
	 * Overload {@link #simpanVolume(Volume, Item, String)} tanpa kata kunci tambahan (memakai
	 * string kosong, sehingga {@link Item#getKewords()} yang sudah ada tidak diubah).
	 *
	 * @param volume    data volume Google Books yang akan disimpan/dicocokkan
	 * @param paramItem {@link Item} yang dipakai bila tidak ditemukan {@link Item} yang cocok
	 *                  berdasarkan {@code googleBookId}/ISBN; boleh {@code null}
	 * @return lihat {@link #simpanVolume(Volume, Item, String)}
	 */
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

	/**
	 * Mengonversi satu {@link Volume} hasil Google Books menjadi {@link Item} persisten di
	 * katalog perpustakaan: mencari {@link Item} yang sudah ada berdasarkan {@code googleBookId}
	 * lalu (bila ada) ISBN-13 lalu (bila masih belum ketemu) ISBN-10 — MEMAKAI baris yang
	 * ditemukan (update) bila cocok, atau {@code paramItem}/{@link Item} baru bila tidak ada yang
	 * cocok — kemudian menyalin seluruh field metadata (abstrak, bahasa, catatan, jumlah halaman,
	 * ISBN, jenis/tipe item, kategori, link, judul, penerbit, pengarang, tahun, subjudul, URL
	 * gambar, snippet, JSON volume utuh) dari {@link Volume.VolumeInfo}, lalu menyimpan
	 * {@link Penerbit} baru bila belum ada, serta membuat baris {@link ItemPunyaPengarang} dan
	 * {@link ItemPunyaKategoriItem} untuk setiap pengarang/kategori yang belum tertaut.
	 *
	 * <p>Setiap pembacaan field dari {@link Volume.VolumeInfo}/{@link Volume} (gambar, deskripsi,
	 * snippet, penerbit, judul, subjudul, tanggal terbit, bahasa, jumlah halaman, link info, JSON
	 * lengkap) dibungkus try/catch kosong terpisah — kegagalan pada satu field (mis. NPE karena
	 * sub-objek {@code null}) tidak menggagalkan pengambilan field lain, hanya membuat field
	 * tersebut default kosong/{@code 0}, dan dicatat ke {@link ais.common.ErrorAuditUtil}.</p>
	 *
	 * <p>Setiap operasi simpan (item, penerbit, pengarang, kategori, relasi) dijalankan sebagai
	 * transaksi Hibernate terpisah (begin/commit per baris) di dalam satu {@link Session}, bukan
	 * satu transaksi besar — kegagalan di tengah proses (mis. saat menyimpan relasi kategori
	 * ke-3 dari 5) dapat meninggalkan item beserta sebagian relasinya sudah tersimpan permanen
	 * sementara sisanya tidak, tanpa rollback ke keadaan semula. Method ini {@code synchronized}
	 * (mengunci pada instance kelas {@code CheckISBN}, bukan per baris/ISBN) untuk mencegah dua
	 * pemanggil membuat {@link Item} duplikat bagi ISBN/{@code googleBookId} yang sama secara
	 * konkuren.</p>
	 *
	 * @param volume    data volume Google Books yang akan disimpan/dicocokkan; {@code null}
	 *                  menyebabkan {@link NullPointerException} yang ditangkap dan menghasilkan
	 *                  {@code null} (lewat blok catch terluar)
	 * @param paramItem {@link Item} yang dipakai sebagai basis update bila tidak ditemukan
	 *                  {@link Item} lain yang cocok berdasarkan {@code googleBookId}/ISBN; boleh
	 *                  {@code null} (maka {@link Item} baru dibuat)
	 * @param kewords   kata kunci tambahan yang akan disisipkan ke {@link Item#getKewords()}
	 *                  (dipisah {@code ", "} dari kata kunci lama) bila panjangnya lebih dari 3
	 *                  karakter dan belum terkandung (case-insensitive) pada kata kunci lama;
	 *                  boleh {@code null}/kosong untuk tidak mengubah kata kunci
	 * @return {@link Item} yang sudah disimpan/dimutakhirkan; {@code null} bila terjadi exception
	 *         apa pun selama proses (ditangani lewat {@link Common#tampilErrorJikaAdmin(
	 *         Exception)} dan {@link HibernateUtil#rollbackTransaction()})
	 */
	public static synchronized Item simpanVolume(Volume volume, Item paramItem, String kewords) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Volume.VolumeInfo volumeInfo = volume.getVolumeInfo();

			String isbn10 = ambilIdentifier(volumeInfo, 0);
			String isbn13 = ambilIdentifier(volumeInfo, 1);
			String lain = ambilIdentifier(volumeInfo, 2);

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

			String authors = gabungkan(volumeInfo == null ? null : volumeInfo.getAuthors());
			String categories = gabungkan(volumeInfo == null ? null : volumeInfo.getCategories());

			Integer tahun = 0;
			if (publishedDate != null && publishedDate.matches("^[0-9]{4}.*")) {
				tahun = Integer.parseInt(publishedDate.substring(0, 4));
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

			String kataKunciLama = item.getKewords() == null ? "" : item.getKewords();
			if (kewords != null && !kewords.trim().isEmpty() && kewords.trim().length() > 3
					&& !kataKunciLama.toLowerCase().contains(kewords.trim().toLowerCase())) {
				String newKey = kataKunciLama.isEmpty() ? kewords.trim() : kataKunciLama + ", " + kewords.trim();
				item.setKewords(newKey);
			}

			session.getTransaction().begin();
			session.saveOrUpdate(item);
			session.getTransaction().commit();

			String[] pengs = authors.split(",");

			for (String p : pengs) {
				p = p.trim();
				if (p.isEmpty()) {
					continue;
				}
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
				if (p.isEmpty()) {
					continue;
				}
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


	/**
	 * Overload {@link #simpanVolume(Volume, ItemTemporary, String)} tanpa kata kunci tambahan
	 * (memakai string kosong, sehingga {@link ItemTemporary#getKewords()} yang sudah ada tidak
	 * diubah).
	 *
	 * @param volume            data volume Google Books yang akan disimpan/dicocokkan
	 * @param paramItemTemporary {@link ItemTemporary} yang dipakai bila tidak ditemukan
	 *                           {@link ItemTemporary} yang cocok berdasarkan
	 *                           {@code googleBookId}/ISBN; boleh {@code null}
	 * @return lihat {@link #simpanVolume(Volume, ItemTemporary, String)}
	 */
	public static ItemTemporary simpanVolume(Volume volume, ItemTemporary paramItemTemporary) {
		return simpanVolume(volume, paramItemTemporary, "");
	}

	/**
	 * Kembaran {@link #simpanVolume(Volume, Item, String)} untuk entitas {@link ItemTemporary}
	 * (buku sementara hasil pencarian, dipakai sebelum dikonfirmasi menjadi {@link Item}
	 * permanen lewat {@link #itemDariItemTemporary(ItemTemporary)}): mengonversi satu
	 * {@link Volume} hasil Google Books menjadi {@link ItemTemporary} persisten dengan logika
	 * pencarian/penyalinan field yang PERSIS SAMA seperti {@link #simpanVolume(Volume, Item,
	 * String)} — mencari berdasarkan {@code googleBookId} lalu ISBN-13 lalu ISBN-10, menyalin
	 * seluruh metadata volume, menyimpan {@link Penerbit} baru bila belum ada, dan membuat baris
	 * {@link ItemPunyaPengarang}/{@link ItemPunyaKategoriItem} bervariasi
	 * {@code setItemTemporary(...)} untuk setiap pengarang/kategori.
	 *
	 * <p>Sebagaimana kembarannya, setiap pembacaan field {@link Volume.VolumeInfo}/{@link Volume}
	 * dibungkus try/catch kosong terpisah (kegagalan satu field tidak menggagalkan yang lain,
	 * dicatat ke {@link ais.common.ErrorAuditUtil}), setiap operasi simpan berjalan sebagai
	 * transaksi Hibernate terpisah per baris (bukan satu transaksi besar, sehingga kegagalan di
	 * tengah proses dapat meninggalkan sebagian relasi tersimpan tanpa rollback penuh), dan
	 * method ini {@code synchronized} untuk mencegah {@link ItemTemporary} duplikat bagi
	 * ISBN/{@code googleBookId} yang sama secara konkuren.</p>
	 *
	 * @param volume             data volume Google Books yang akan disimpan/dicocokkan;
	 *                           {@code null} menyebabkan {@link NullPointerException} yang
	 *                           ditangkap dan menghasilkan {@code null}
	 * @param paramItemTemporary {@link ItemTemporary} yang dipakai sebagai basis update bila
	 *                           tidak ditemukan {@link ItemTemporary} lain yang cocok
	 *                           berdasarkan {@code googleBookId}/ISBN; boleh {@code null} (maka
	 *                           {@link ItemTemporary} baru dibuat)
	 * @param kewords            kata kunci tambahan yang akan disisipkan ke
	 *                           {@link ItemTemporary#getKewords()} (dipisah {@code ", "} dari
	 *                           kata kunci lama) bila panjangnya lebih dari 3 karakter dan belum
	 *                           terkandung (case-insensitive) pada kata kunci lama; boleh
	 *                           {@code null}/kosong untuk tidak mengubah kata kunci
	 * @return {@link ItemTemporary} yang sudah disimpan/dimutakhirkan; {@code null} bila terjadi
	 *         exception apa pun selama proses (ditangani lewat
	 *         {@link Common#tampilErrorJikaAdmin(Exception)} dan
	 *         {@link HibernateUtil#rollbackTransaction()})
	 */
	public static synchronized ItemTemporary simpanVolume(Volume volume, ItemTemporary paramItemTemporary, String kewords) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Volume.VolumeInfo volumeInfo = volume.getVolumeInfo();

			String isbn10 = ambilIdentifier(volumeInfo, 0);
			String isbn13 = ambilIdentifier(volumeInfo, 1);
			String lain = ambilIdentifier(volumeInfo, 2);

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

			String authors = gabungkan(volumeInfo == null ? null : volumeInfo.getAuthors());
			String categories = gabungkan(volumeInfo == null ? null : volumeInfo.getCategories());

			Integer tahun = 0;
			if (publishedDate != null && publishedDate.matches("^[0-9]{4}.*")) {
				tahun = Integer.parseInt(publishedDate.substring(0, 4));
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

			String kataKunciTemporary = itemTemporary.getKewords() == null ? "" : itemTemporary.getKewords();
			if (kewords != null && !kewords.trim().isEmpty() && kewords.trim().length() > 3
					&& !kataKunciTemporary.toLowerCase().contains(kewords.trim().toLowerCase())) {
				String newKey = kataKunciTemporary.isEmpty() ? kewords.trim()
						: kataKunciTemporary + ", " + kewords.trim();
				itemTemporary.setKewords(newKey);
			}

			session.getTransaction().begin();
			session.saveOrUpdate(itemTemporary);
			session.getTransaction().commit();

			String[] pengs = authors.split(",");

			for (String p : pengs) {
				p = p.trim();
				if (p.isEmpty()) {
					continue;
				}
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
				if (p.isEmpty()) {
					continue;
				}
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

	/**
	 * Memeriksa apakah sebuah ISBN sudah ada di katalog perpustakaan; bila belum, mencarinya lewat
	 * Google Books API dan langsung menyimpan hasil pertama sebagai {@link Item} baru.
	 *
	 * <p>Alur: (1) baca parameter {@code isbn} dari request; (2) hitung jumlah {@link Item} yang
	 * {@code isbn}-nya (ISBN-13) ATAU {@code isbn10}-nya cocok PERSIS dengan nilai tersebut; (3)
	 * bila jumlahnya 0 (belum ada di katalog), panggil {@link BooksSample#queryGoogleBooks(
	 * JsonFactory, String, int, int)} dengan query {@code "isbn:" + isbn} untuk 1 hasil teratas;
	 * (4) bila Google Books tidak mengembalikan hasil apa pun, method berhenti tanpa efek samping;
	 * (5) bila ada hasil, simpan sebagai {@link Item} baru lewat {@link #simpanVolume(Volume,
	 * Item, String)} (dengan kata kunci kosong). Kegagalan pada langkah pencarian Google Books
	 * ditelan (dicatat ke {@link ais.common.ErrorAuditUtil}) tanpa menggagalkan seluruh request.</p>
	 *
	 * <p>Method ini TIDAK PERNAH menulis apa pun ke {@code resp} (parameter response diterima
	 * tetapi tidak dipakai) — pemanggil (mis. skrip halaman yang memicu pengecekan ISBN saat
	 * input buku baru) tidak menerima indikasi hasil lewat badan HTTP; efek yang teramati hanya
	 * berupa baris {@link Item} baru yang muncul di database bila ISBN ditemukan di Google
	 * Books.</p>
	 *
	 * @param request request HTTP masuk; parameter {@code isbn} adalah satu-satunya input yang
	 *                dibaca
	 * @param resp    response HTTP keluar; TIDAK PERNAH diisi/ditulis oleh method ini
	 * @throws Exception bila query Hibernate untuk menghitung {@link Item} yang cocok gagal
	 *                    (kegagalan pencarian Google Books ditelan secara internal, lihat di atas)
	 */
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
