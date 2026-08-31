package ais.action.master.resources;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;




import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.sun.jersey.api.NotFoundException;

import com.sun.jersey.spi.resource.Singleton;

import ais.action.master.library.util.LibraryUtil;
import ais.action.master.resources.helper.PerpustakaanResourcesHelper;
import ais.action.master.resources.model.CommonID;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoItem;
import ais.database.model.library.Anggota;
import ais.database.model.library.DomainPenelitian;
import ais.database.model.library.Item;
import ais.database.model.library.ItemKomentar;
import ais.database.model.library.ItemPunyaPemeriksa;
import ais.database.model.library.ItemPunyaPengarang;
import ais.database.model.library.Penerbit;
import ais.database.model.library.PenerbitPunyaPemeriksa;
import ais.database.model.library.Pengarang;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.PesananAnggota;
import ais.ui.util.WaktuUtil;




/**
 * Endpoint REST (JAX-RS/Jersey, singleton) di jalur {@code /user_perpustakaan} yang menjembatani
 * aplikasi luar (portal repositori karya ilmiah/perpustakaan) dengan modul {@code library} AIS:
 * pengajuan jurnal/karya ilmiah, pemesanan item pustaka, pencarian/listing item dan pengarang, serta
 * penyediaan-otomatis (auto-provisioning) akun {@link Tbmuser}/{@link Anggota}/{@link Pengarang}
 * bila belum ada.
 *
 * <p>
 * Konvensi parameter path di seluruh method: nilai {@code "_"} atau string kosong berarti "tidak
 * difilter"/"kosong" (dipakai karena JAX-RS path segment tidak dapat benar-benar kosong), dan
 * hampir semua parameter teks di-decode dulu lewat {@link URLDecoder#decode(String, String)}
 * (UTF-8) sebelum dipakai pada query Hibernate. Pengguna diidentifikasi lewat {@code username}
 * (dicocokkan ke {@code email} ATAU id {@link Tbmuser}) tanpa mekanisme otentikasi/sesi lain — API
 * ini murni dipanggil server-to-server oleh sistem luar yang sudah dipercaya.
 * </p>
 *
 * <p>
 * <b>Catatan keamanan:</b> {@link #batalkanPesanItem(String)} sebelumnya menyusun perintah SQL
 * {@code DELETE} dengan menyambung parameter {@code id} LANGSUNG ke string SQL — celah SQL
 * injection ke perintah destruktif, DITUTUP 2026-09-01 lewat validasi numerik + parameter binding
 * (lihat javadoc method). Ketiadaan mekanisme otentikasi pada kelas ini secara keseluruhan (lihat
 * paragraf di atas) TIDAK diubah pada perbaikan ini.
 * </p>
 *
 * <p>
 * Method pencarian item ({@link #daftarItemJurnal}/{@link #daftarItemJurnalMereka}/
 * {@link #daftarItemKomentarSemua}/{@link #daftarItemKomentarSemuaMereka}) masing-masing punya tiga
 * overload dengan jumlah parameter path bertingkat (tanpa status/paging, dengan status, dengan
 * status+paging) sebagai default value berjenjang — pola umum JAX-RS untuk mendukung URL pendek dan
 * panjang pada resource yang sama. Setiap method menutup sesi Hibernate ({@link HibernateUtil#closeSession()})
 * sebelum return, termasuk pada jalur awal (early return) saat validasi user/data gagal.
 * </p>
 */
@Path("/user_perpustakaan")
@Singleton
public class UserPerpustakaanResource extends PerpustakaanResource {

	/** Konstruktor default, meneruskan inisialisasi ke {@link PerpustakaanResource}. */
	public UserPerpustakaanResource() {
		super();
	}

	/** Endpoint root ({@code GET /user_perpustakaan}) yang sekadar mengembalikan diri sendiri sebagai JSON — dipakai untuk verifikasi resource ter-mount. */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public UserPerpustakaanResource getXml() {
		return this;
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("/ajukanJurnal")
	@Produces("text/plain")
	/**
	 * Mengajukan (membuat/memperbarui) satu karya ilmiah/jurnal ({@link Item} bertipe
	 * {@code LibraryUtil.KARYA_ILMIAH}) beserta relasi pengarangnya, dipanggil setelah file jurnal
	 * diunggah lewat mekanisme lain (diverifikasi dari keberadaan {@link FotoItem} ber-{@code kodeUnik}
	 * yang sama). Parameter {@code item} adalah daftar nilai posisi-tetap: [0] id item (0/baru bila
	 * belum ada), [1] kode unik file terlampir, [2..7] judul/abstrak/keyword ID &amp; EN, [8] username
	 * pengaju utama, [9] id penerbit, [10] id domain penelitian (opsional), [11..] username pengaju
	 * tambahan. Bila item baru dan belum ada {@link ItemPunyaPemeriksa}, daftar pemeriksa
	 * disalin otomatis dari {@link PenerbitPunyaPemeriksa} penerbit+domain penelitian terkait.
	 *
	 * @param item daftar nilai form posisi-tetap (lihat javadoc method)
	 * @return {@code "OK"} bila berhasil, atau pesan error dalam Bahasa Indonesia bila validasi gagal
	 * @throws Exception ditangkap secara internal (rollback transaksi Hibernate); praktis tidak pernah keluar dari method
	 */
	public String ajukanJurnal(@DefaultValue("All") @QueryParam(value = "item") final List<String> item)
			throws Exception {

		try {
			System.out.println("item = " + item);

			Long itemId = Long.parseLong(item.get(0).trim());
			Long kodeUnik = Long.parseLong(item.get(1).trim());

			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			Integer attacedFile = ((Number) streamingSession.createCriteria(FotoItem.class)
					.add(Restrictions.eq("kodeUnik", kodeUnik)).setProjection(Projections.rowCount()).uniqueResult())
							.intValue();

			if (attacedFile.equals(0)) {
				StreamingHibernateUtil.getInstance().closeSession();
				return "Anda harus melampirkan file jurnal sebelum jurnal ini Anda ajukan";
			}

			String judul = item.get(2).trim();
			String judulEn = item.get(3).trim();
			String abstrak = item.get(4).trim();
			String abstrakEn = item.get(5).trim();
			String keyword = item.get(6).trim();
			String keywordEn = item.get(7).trim();
			String diajukanOleh = item.get(8).trim();
			Long penerbitId = Long.parseLong(item.get(9).trim());
			Long domain_penelitian = item.size() > 10 ? Long.parseLong(item.get(10).trim()) : -1L;

			List<String> pengajuLainnya = new ArrayList<String>();
			for (int i = 11; i < item.size(); i++) {
				pengajuLainnya.add(item.get(i));
			}

			Session session = HibernateUtil.currentNativeSession();

			Tbmuser tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(Restrictions.eq("email", diajukanOleh), Restrictions.idEq(diajukanOleh)))
					.setMaxResults(1).uniqueResult();
			if (tbmuser == null || tbmuser.getUserId() == null) {

				HibernateUtil.closeSession();
				return "Pengaju dengan id " + diajukanOleh + " tidak ditemukan";
			}

			Pengarang pengarang = (Pengarang) session.createCriteria(Pengarang.class)
					.add(Restrictions.eq("userIdPengarang", tbmuser)).setMaxResults(1).uniqueResult();
			if (pengarang == null) {

				HibernateUtil.closeSession();
				return "Pengarang dengan id " + diajukanOleh + " tidak ditemukan";
			}

			Penerbit penerbit = (Penerbit) session.createCriteria(Penerbit.class).add(Restrictions.idEq(penerbitId))
					.setMaxResults(1).uniqueResult();
			if (penerbit == null) {

				HibernateUtil.closeSession();
				return "Penerbit atau instansi harus diisi";
			}

			DomainPenelitian domainPenelitian = (DomainPenelitian) session.createCriteria(DomainPenelitian.class)
					.add(Restrictions.idEq(domain_penelitian)).setMaxResults(1).uniqueResult();
			// if (domainPenelitian == null) {
			// HibernateUtil.closeSession();
			// return "Domain Penelitian harus diisi";
			// }

			Item myItem = (Item) session.createCriteria(Item.class).add(Restrictions.idEq(itemId)).uniqueResult();
			if (myItem == null) {
				myItem = new Item();
			}

			myItem.setDomainPenelitian(domainPenelitian);
			myItem.setAbstrak(abstrak);
			myItem.setAbstrakEn(abstrakEn);
			myItem.setNama(judul);
			myItem.setTema(judulEn);
			myItem.setKewords(keyword);
			myItem.setKewordsEn(keywordEn);
			myItem.setDibuatOleh(tbmuser);
			myItem.setTipeItem(LibraryUtil.KARYA_ILMIAH);
			myItem.setPenerbit(penerbit);
			myItem.setDefaultSatuanKerja(penerbit.getSatuanKerja());
			myItem.setKodeUnik(kodeUnik);

			session.getTransaction().begin();
			if (myItem.getId() != null) {
				Common.refreshUpdate(session, (myItem));
			} else {
				myItem.setPengarangs(tbmuser.getUserNama());
				session.save(myItem);
			}

			ItemPunyaPengarang itemPunyaPengarang = (ItemPunyaPengarang) session
					.createCriteria(ItemPunyaPengarang.class).add(Restrictions.eq("pengarang", pengarang))
					.add(Restrictions.eq("item", myItem)).setMaxResults(1).uniqueResult();
			if (itemPunyaPengarang == null) {
				itemPunyaPengarang = new ItemPunyaPengarang();
				itemPunyaPengarang.setPengarang(pengarang);
				itemPunyaPengarang.setItem(myItem);
				session.save(itemPunyaPengarang);
			}

			for (String pengaju : pengajuLainnya) {
				tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.or(Restrictions.eq("email", pengaju), Restrictions.idEq(pengaju)))
						.setMaxResults(1).uniqueResult();
				pengarang = (Pengarang) session.createCriteria(Pengarang.class)
						.add(Restrictions.eq("userIdPengarang", tbmuser)).setMaxResults(1).uniqueResult();
				if (pengarang != null) {
					itemPunyaPengarang = (ItemPunyaPengarang) session.createCriteria(ItemPunyaPengarang.class)
							.add(Restrictions.eq("pengarang", pengarang)).add(Restrictions.eq("item", myItem))
							.setMaxResults(1).uniqueResult();
					if (itemPunyaPengarang == null) {
						itemPunyaPengarang = new ItemPunyaPengarang();
						itemPunyaPengarang.setPengarang(pengarang);
						itemPunyaPengarang.setItem(myItem);
						session.save(itemPunyaPengarang);
					}
				}
			}

			session.getTransaction().commit();

			Integer jumlahPemeriksa = ((Number) session.createCriteria(ItemPunyaPemeriksa.class)
					.add(Restrictions.eq("item", myItem)).setProjection(Projections.rowCount()).uniqueResult())
							.intValue();
			if (jumlahPemeriksa.equals(0)) {
				List<PenerbitPunyaPemeriksa> penerbitPunyaPemeriksas = session
						.createCriteria(PenerbitPunyaPemeriksa.class)
						.add(Restrictions.eq("penerbit", myItem.getPenerbit()))
						.add(Restrictions.eq("domainPenelitian", domainPenelitian)).list();
				session.getTransaction().begin();
				for (PenerbitPunyaPemeriksa penerbitPunyaPemeriksa : penerbitPunyaPemeriksas) {
					ItemPunyaPemeriksa itemPunyaPemeriksa = new ItemPunyaPemeriksa();
					itemPunyaPemeriksa.setItem(myItem);
					itemPunyaPemeriksa.setPemeriksa(penerbitPunyaPemeriksa.getPemeriksa());
					session.save(itemPunyaPemeriksa);
				}
				session.getTransaction().commit();
			}

			String updateQuery = "update foto_item set item = " + myItem.getId() + " where kode_unik = " + kodeUnik;
			streamingSession.getTransaction().begin();
			streamingSession.createSQLQuery(updateQuery).executeUpdate();
			streamingSession.getTransaction().commit();

			StreamingHibernateUtil.getInstance().closeSession();

			HibernateUtil.closeSession();
		} catch (Exception e) {
			HibernateUtil.rollbackTransaction();
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}
		return "OK";
	}

	@GET
	@Path("check_pesan_item/{username}/{item}/{perpustakaan}/")
	@Produces("text/plain")
	/** Mengecek apakah {@code username} (via {@link Anggota} aktifnya) sudah punya pesanan aktif ({@code status=PESAN}) untuk {@code item} di {@code perpustakaan} tertentu. Mengembalikan {@code "OK"}/{@code "NOT OK"}, atau pesan error bila user/anggota/perpustakaan/item tidak ditemukan. */
	public String checkPesanItem(@PathParam("username") String username, @PathParam("item") String item,
			@PathParam("perpustakaan") String perpustakaan) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		username = URLDecoder.decode(username, "UTF-8");
		username = username.trim().equals("_") ? "" : username.trim();
		Tbmuser tbmuser = (Tbmuser) (session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.eq("email", username), Restrictions.idEq(username))).uniqueResult());

		if (tbmuser == null || tbmuser.getUserId() == null) {

			HibernateUtil.closeSession();
			return ("Login pengguna gagal dilakukan");
		}

		Anggota anggota = (Anggota) session.createCriteria(Anggota.class)
				.add(Restrictions.or(Restrictions.eq("email", username), Restrictions.eq("tbmuser", tbmuser)))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1).uniqueResult();
		if (anggota == null) {

			HibernateUtil.closeSession();
			return ("Keanggotaan anda belum aktif, harap aktifkan kode keanggotaan anda di perpustakaan terdekat");
		}

		Perpustakaan myPerpustakaan = (Perpustakaan) session.createCriteria(Perpustakaan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.idEq(Long.parseLong(perpustakaan))).uniqueResult();

		if (myPerpustakaan == null) {

			HibernateUtil.closeSession();
			return ("Perpustakaan tidak ditemukan");
		}

		Item myItem = (Item) session.createCriteria(Item.class).add(Restrictions.idEq(Long.parseLong(item)))
				.uniqueResult();

		if (myItem == null) {

			HibernateUtil.closeSession();
			return ("Item tidak ditemukan");
		}

		Integer count = ((Number) session.createCriteria(PesananAnggota.class).add(Restrictions.eq("item", myItem))
				.add(Restrictions.eq("perpustakaan", myPerpustakaan)).add(Restrictions.eq("anggota", anggota))
				.add(Restrictions.eq("status", PesananAnggota.PESAN)).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();

		HibernateUtil.closeSession();

		return !count.equals(0) ? "OK" : "NOT OK";
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_pesanan_item/{username}/{cari}/{start}/{banyak}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/** Mengembalikan daftar pesanan {@link PesananAnggota} milik {@code username}, dipaginasi ({@code start}/{@code banyak}) dan difilter berdasarkan {@code cari} yang dicocokkan (ILIKE) terhadap nama/tema/keyword/ISBN/ISSN/pengarang/kode item. Setiap hasil dipetakan ke {@link CommonID} dengan info1..info8 berisi ISBN, ISSN, nama, pengarang, status, tanggal, URL gambar, dan kode. */
	public List<CommonID> daftarPesanItem(@PathParam("username") String username, @PathParam("cari") String cari,
			@PathParam("start") String start, @PathParam("banyak") String banyak) throws Exception {
		List<CommonID> commonIDs = new ArrayList<CommonID>();

		cari = URLDecoder.decode(cari, "UTF-8");

		Session session = HibernateUtil.currentNativeSession();
		username = URLDecoder.decode(username, "UTF-8");
		username = username.trim().equals("_") ? "" : username.trim();
		Tbmuser tbmuser = (Tbmuser) (session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.eq("email", username), Restrictions.idEq(username))).uniqueResult());

		if (tbmuser == null || tbmuser.getUserId() == null) {

			HibernateUtil.closeSession();
			return commonIDs;
		}

		Anggota anggota = (Anggota) session.createCriteria(Anggota.class)
				.add(Restrictions.or(Restrictions.eq("email", username), Restrictions.eq("tbmuser", tbmuser)))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1).uniqueResult();
		if (anggota == null) {

			HibernateUtil.closeSession();
			return commonIDs;
		}

		Criterion criterion = Restrictions.or(Restrictions.ilike("item.nama", cari, MatchMode.ANYWHERE),
				Restrictions.ilike("item.tema", cari, MatchMode.ANYWHERE));
		// criterion = Restrictions.or(criterion,
		// Restrictions.ilike("item.abstrak", cari, MatchMode.ANYWHERE));
		// criterion = Restrictions.or(criterion,
		// Restrictions.ilike("item.abstrakEn", cari, MatchMode.ANYWHERE));
		criterion = Restrictions.or(criterion, Restrictions.ilike("item.kewords", cari, MatchMode.ANYWHERE));
		criterion = Restrictions.or(criterion, Restrictions.ilike("item.kewordsEn", cari, MatchMode.ANYWHERE));
		criterion = Restrictions.or(criterion, Restrictions.ilike("item.isbn", cari, MatchMode.ANYWHERE));
		criterion = Restrictions.or(criterion, Restrictions.ilike("item.issn", cari, MatchMode.ANYWHERE));
		criterion = Restrictions.or(criterion, Restrictions.ilike("item.pengarangs", cari, MatchMode.ANYWHERE));
		criterion = Restrictions.or(criterion, Restrictions.ilike("kode", cari, MatchMode.ANYWHERE));

		List<PesananAnggota> pesananAnggotas = session.createCriteria(PesananAnggota.class)
				.createAlias("item", "item", Criteria.LEFT_JOIN)
				.createAlias("item.jenisItem", "jenisItem", Criteria.LEFT_JOIN)
				.add(cari.trim().equals("") || cari.trim().equals("_") || cari.trim().equals("-1")
						? Restrictions.sqlRestriction("1=1") : criterion)
				.add(Restrictions.eq("anggota", anggota)).addOrder(Order.desc("id"))
				.setFirstResult(Integer.parseInt(start)).setMaxResults(Integer.parseInt(banyak.trim())).list();

		for (PesananAnggota pesananAnggota : pesananAnggotas) {
			String imageUrl = CommonMedia.getMediaItem(pesananAnggota.getItem().getId(), 100, 120, false);
			CommonID commonID = new CommonID(pesananAnggota.getId());
			commonID.setInfo1(pesananAnggota.getItem().getIsbn());
			commonID.setInfo2(pesananAnggota.getItem().getIssn());
			commonID.setInfo3(pesananAnggota.getItem().getNama());
			commonID.setInfo4(pesananAnggota.getItem().getPengarangs());
			commonID.setInfo5(pesananAnggota.getStatus());
			commonID.setInfo6(Common.dateFormat5.get().format(pesananAnggota.getTanggal()));
			commonID.setInfo7(imageUrl);
			commonID.setInfo8(pesananAnggota.getKode());
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();
		return commonIDs;
	}

	@GET
	@Path("batalkan_pesan_item/{id}/")
	@Produces("text/plain")
	/**
	 * Menghapus permanen (SQL {@code delete}) satu baris {@code library.pesanan_anggota} berdasarkan
	 * id. Selalu mengembalikan {@code "OK"} bila query berhasil dieksekusi, tanpa memeriksa apakah
	 * baris tersebut sebelumnya ada.
	 *
	 * <p>
	 * <b>Catatan keamanan (CELAH SQL INJECTION DITUTUP 2026-09-01):</b> sebelumnya parameter
	 * {@code id} disambung LANGSUNG ke string SQL delete tanpa validasi/parameter binding — celah
	 * SQL injection ke perintah DELETE yang dapat dieksploitasi lewat path URL tanpa login (kelas
	 * ini sama sekali tidak memeriksa autentikasi). Kini {@code id} divalidasi sebagai angka
	 * ({@code Long.parseLong}) dan diikat lewat {@code SQLQuery#setParameter}. Ketiadaan autentikasi
	 * pada kelas ini secara keseluruhan TIDAK diubah pada perbaikan ini — lihat catatan keamanan
	 * pada javadoc kelas.
	 * </p>
	 */
	public String batalkanPesanItem(@PathParam("id") String id) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.createSQLQuery("delete from library.pesanan_anggota where id = :id")
				.setParameter("id", Long.parseLong(id.trim())).executeUpdate();
		session.getTransaction().commit();

		HibernateUtil.closeSession();
		return "OK";
	}

	@GET
	@Path("check_keanggotaan/{username}/")
	@Produces("text/plain")
	/** Memeriksa apakah {@code username} punya {@link Tbmuser} dan {@link Anggota} aktif; mengembalikan {@code "OK"} bila keduanya valid, atau pesan error Bahasa Indonesia bila salah satunya tidak ditemukan/tidak aktif. */
	public String checkKeanggotaan(@PathParam("username") String username) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		username = URLDecoder.decode(username, "UTF-8");
		username = username.trim().equals("_") ? "" : username.trim();
		Tbmuser tbmuser = (Tbmuser) (session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.eq("email", username), Restrictions.idEq(username))).uniqueResult());

		if (tbmuser == null || tbmuser.getUserId() == null) {

			HibernateUtil.closeSession();
			return ("Login pengguna gagal dilakukan");
		}

		Anggota anggota = (Anggota) session.createCriteria(Anggota.class)
				.add(Restrictions.or(Restrictions.eq("email", username), Restrictions.eq("tbmuser", tbmuser)))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1).uniqueResult();
		if (anggota == null) {

			HibernateUtil.closeSession();
			return ("Keanggotaan anda belum aktif, harap aktifkan kode keanggotaan anda di perpustakaan terdekat");
		}

		HibernateUtil.closeSession();
		return "OK";
	}

	@GET
	@Path("pesan_item/{username}/{item}/{perpustakaan}/")
	@Produces("text/plain")
	/**
	 * Membuat pesanan baru ({@link PesananAnggota}) untuk {@code item} di {@code perpustakaan} atas
	 * nama {@code username}, setelah memvalidasi keanggotaan, ketersediaan stok (dihitung via SQL
	 * native dari {@code library.detail_transaksi} dikalikan jenis transaksi), dan memastikan belum
	 * ada pesanan aktif (belum kadaluarsa) untuk kombinasi item+perpustakaan+anggota yang sama. Masa
	 * berlaku pesanan dihitung dari konfigurasi {@code kadaluarsa.pemesanan.item} (jam, default 24).
	 *
	 * @return {@code "OK"} bila pesanan berhasil dibuat, atau pesan error Bahasa Indonesia bila validasi gagal
	 */
	public String pesanItem(@PathParam("username") String username, @PathParam("item") String item,
			@PathParam("perpustakaan") String perpustakaan) throws Exception {

		Session session = HibernateUtil.currentNativeSession();
		username = URLDecoder.decode(username, "UTF-8");
		username = username.trim().equals("_") ? "" : username.trim();
		Tbmuser tbmuser = (Tbmuser) (session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.eq("email", username), Restrictions.idEq(username))).uniqueResult());

		if (tbmuser == null || tbmuser.getUserId() == null) {

			HibernateUtil.closeSession();
			return ("Login pengguna gagal dilakukan");
		}

		Anggota anggota = (Anggota) session.createCriteria(Anggota.class)
				.add(Restrictions.or(Restrictions.eq("email", username), Restrictions.eq("tbmuser", tbmuser)))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1).uniqueResult();
		if (anggota == null) {

			HibernateUtil.closeSession();
			return ("Keanggotaan anda belum aktif, harap aktifkan kode keanggotaan anda di perpustakaan terdekat");
		}

		Perpustakaan myPerpustakaan = (Perpustakaan) session.createCriteria(Perpustakaan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.idEq(Long.parseLong(perpustakaan))).uniqueResult();

		if (myPerpustakaan == null) {

			HibernateUtil.closeSession();
			return ("Perpustakaan tidak ditemukan");
		}

		Item myItem = (Item) session.createCriteria(Item.class).add(Restrictions.idEq(Long.parseLong(item)))
				.uniqueResult();

		if (myItem == null) {

			HibernateUtil.closeSession();
			return ("Item tidak ditemukan");
		}

		String sqlCheckStok = "select sum((a.qty+a.qtybonus)*b.jenis) as stok " + "from library.detail_transaksi a "
				+ "inner join library.kode_transaksi b on (a.kode_transaksi = b.id) " + "where a.item = "
				+ myItem.getId() + " and a.perpustakaan = " + myPerpustakaan.getId();

		Number jumlah = (Number) session.createSQLQuery(sqlCheckStok).uniqueResult();
		if (jumlah == null || jumlah.intValue() < 1) {

			HibernateUtil.closeSession();
			return ("Item " + myItem.getNama() + " tidak tersedia di " + myPerpustakaan.getNama());
		}

		jumlah = (Number) session.createCriteria(PesananAnggota.class).add(Restrictions.eq("anggota", anggota))
				.add(Restrictions.eq("item", myItem)).add(Restrictions.eq("perpustakaan", myPerpustakaan))
				.add(Restrictions.sqlRestriction(
						"date(kadaluarsa) > date('" + Common.databaseDateFormat1.get().format(WaktuUtil.getDate()) + "')"))
				.setProjection(Projections.rowCount()).uniqueResult();

		if (jumlah != null && jumlah.intValue() > 0) {

			HibernateUtil.closeSession();
			return ("Item " + myItem.getNama() + " sudah anda pesan di " + myPerpustakaan.getNama());
		}

		Konfigurasi kadaluarsa = Common.getKonfigurasi("kadaluarsa.pemesanan.item", "24");
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.HOUR_OF_DAY,
				calendar.get(Calendar.HOUR_OF_DAY) + (Integer.parseInt(kadaluarsa.getNilai().trim())));

		PesananAnggota pesananAnggota = new PesananAnggota();
		pesananAnggota.setAnggota(anggota);
		pesananAnggota.setItem(myItem);
		pesananAnggota.setKadaluarsa(calendar.getTime());
		pesananAnggota.setKeterangan("Pemesanan online");
		pesananAnggota.setPerpustakaan(myPerpustakaan);
		pesananAnggota.setStatus(PesananAnggota.PESAN);
		pesananAnggota.setTanggal(ais.ui.util.WaktuUtil.getDate());

		session.getTransaction().begin();
		session.save(pesananAnggota);
		session.getTransaction().commit();

		HibernateUtil.closeSession();

		return "OK";
	}

	@GET
	@Path("ubah_status/{id}/{status}/")
	@Produces("text/plain")
	/**
	 * Mengubah status satu {@link ItemPunyaPemeriksa} (proses review karya ilmiah oleh pemeriksa),
	 * lalu menyinkronkan status terbit {@link Item} induknya: bila sudah tidak ada pemeriksa lain
	 * dengan status selain {@code DISETUJUI}, item ditandai {@code LibraryUtil.APPROVE}; selain itu
	 * dikembalikan ke {@code LibraryUtil.DRAFT}.
	 */
	public String ubahStatus(@PathParam("id") String id, @PathParam("status") String status) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		ItemPunyaPemeriksa itemPunyaPemeriksa = (ItemPunyaPemeriksa) session.createCriteria(ItemPunyaPemeriksa.class)
				.add(Restrictions.idEq(Long.parseLong(id))).uniqueResult();
		if (itemPunyaPemeriksa != null) {
			itemPunyaPemeriksa.setStatus(status);
			session.getTransaction().begin();
			session.update(itemPunyaPemeriksa);
			session.getTransaction().commit();
		}

		Integer count = ((Number) session.createCriteria(ItemPunyaPemeriksa.class)
				.add(Restrictions.eq("item", itemPunyaPemeriksa.getItem()))
				.add(Restrictions.ne("status", ItemPunyaPemeriksa.DISETUJUI)).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count.equals(0)) {
			Item item = itemPunyaPemeriksa.getItem();
			item.setStatusTerbitItem(LibraryUtil.APPROVE);
			session.getTransaction().begin();
			session.update(item);
			session.getTransaction().commit();
		} else {
			Item item = itemPunyaPemeriksa.getItem();
			item.setStatusTerbitItem(LibraryUtil.DRAFT);
			session.getTransaction().begin();
			session.update(item);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return "OK";
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_penerbit/{cari}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/** Mengembalikan seluruh pasangan {@link PenerbitPunyaPemeriksa} (penerbit, domain penelitian, pemeriksa), terurut nama, dipetakan ke {@link CommonID}. Parameter {@code cari} saat ini tidak dipakai untuk memfilter hasil. */
	public List<CommonID> daftarPenerbit(@PathParam("cari") String cari) throws Exception {
		List<CommonID> commonIDs = new ArrayList<CommonID>();
		Session session = HibernateUtil.currentNativeSession();
		List<PenerbitPunyaPemeriksa> penerbitPunyaPemeriksas = session.createCriteria(PenerbitPunyaPemeriksa.class)
				.add(Restrictions.isNotNull("penerbit")).add(Restrictions.isNotNull("pemeriksa"))
				.createAlias("penerbit", "penerbit").createAlias("pemeriksa", "pemeriksa")
				.createAlias("domainPenelitian", "domainPenelitian").addOrder(Order.asc("penerbit.nama"))
				.addOrder(Order.asc("domainPenelitian.nama")).addOrder(Order.asc("pemeriksa.userNama")).list();
		for (PenerbitPunyaPemeriksa penerbitPunyaPemeriksa : penerbitPunyaPemeriksas) {
			CommonID commonID = new CommonID(penerbitPunyaPemeriksa.getId());
			commonID.setInfo1(penerbitPunyaPemeriksa.getPenerbit().getNama());
			commonID.setInfo2(penerbitPunyaPemeriksa.getPemeriksa().getUserId());
			commonID.setInfo3(penerbitPunyaPemeriksa.getPemeriksa().getUserNama());
			commonID.setInfo4(penerbitPunyaPemeriksa.getDomainPenelitian() == null ? ""
					: penerbitPunyaPemeriksa.getDomainPenelitian().getId() + "");
			commonID.setInfo5(penerbitPunyaPemeriksa.getDomainPenelitian() == null ? ""
					: penerbitPunyaPemeriksa.getDomainPenelitian().getNama());
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();
		return commonIDs;
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_pengarang/{cari}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/** Mencari {@link Pengarang} berdasarkan {@code cari} (ILIKE terhadap nama pengarang, atau id/nama user-nya), dibatasi {@link Common#MAX_RESULT_20} hasil, dipetakan ke {@link CommonID} (nama, userId, userNama). */
	public List<CommonID> daftarPengarang(@PathParam("cari") String cari) throws Exception {

		cari = URLDecoder.decode(cari, "UTF-8");
		cari = cari.trim().equals("_") ? "" : cari.trim();

		List<CommonID> commonIDs = new ArrayList<CommonID>();
		Session session = HibernateUtil.currentNativeSession();

		Criterion criterion = Restrictions.or(Restrictions.ilike("nama", cari, MatchMode.ANYWHERE),
				Restrictions.ilike("userIdPengarang.userId", cari, MatchMode.ANYWHERE));

		criterion = Restrictions.or(criterion,
				Restrictions.ilike("userIdPengarang.userNama", cari, MatchMode.ANYWHERE));

		List<Pengarang> pengarangs = session.createCriteria(Pengarang.class)
				.createAlias("userIdPengarang", "userIdPengarang")
				.add(cari.trim().equals("") ? Restrictions.sqlRestriction("1=1") : criterion)
				.addOrder(Order.asc("nama")).setMaxResults(Common.MAX_RESULT_20).list();
		for (Pengarang pengarang : pengarangs) {
			CommonID commonID = new CommonID(pengarang.getId());
			commonID.setInfo1(pengarang.getNama());
			commonID.setInfo2(pengarang.getUserIdPengarang().getUserId());
			commonID.setInfo3(pengarang.getUserIdPengarang().getUserNama());
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();
		return commonIDs;
	}

	@GET
	@Path("check_pengguna/{username}/{awal}/{tengah}/{akhir}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Titik masuk auto-provisioning: memastikan {@code username} punya {@link Tbmuser} (dibuat baru
	 * dengan role {@code ConstantValues.roleKomunitas} bila belum ada, password disimpan apa adanya
	 * tanpa enkripsi via {@code is_encripted=false}), {@link Anggota} (dibuat baru berjenis
	 * {@code ANGGOTA_REGULER}/{@code UMUM}, tidak aktif sampai diverifikasi manual di perpustakaan),
	 * dan {@link Pengarang}. Nama lengkap disusun dari {@code awal+" "+tengah+" "+akhir} dan
	 * disinkronkan ke ketiga entitas bila berbeda dari yang tersimpan.
	 *
	 * @return {@link CommonID} berisi userId, userNama, dan id {@link Pengarang} yang bersangkutan
	 */
	public CommonID checkPengguna(@PathParam("username") String username, @PathParam("awal") String awal,
			@PathParam("tengah") String tengah, @PathParam("akhir") String akhir) throws Exception {
		username = URLDecoder.decode(username, "UTF-8");
		awal = URLDecoder.decode(awal, "UTF-8");
		tengah = URLDecoder.decode(tengah, "UTF-8");
		akhir = URLDecoder.decode(akhir, "UTF-8");

		username = username.trim().equals("_") ? "" : username.trim();
		awal = username.trim().equals("_") ? "" : awal.trim();
		tengah = username.trim().equals("_") ? "" : tengah.trim();
		akhir = username.trim().equals("_") ? "" : akhir.trim();

		String userNama = awal + " " + tengah + " " + akhir;

		Session session = HibernateUtil.currentNativeSession();
		Tbmuser tbmuser = (Tbmuser) (session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.eq("email", username), Restrictions.idEq(username))).uniqueResult());

		if (tbmuser == null || tbmuser.getUserId() == null) {
			tbmuser = new Tbmuser();
			tbmuser.setUserId(username);
			tbmuser.setEmail(username);
			tbmuser.setUserShow(1);
			tbmuser.setKeterangan("Penggunak ini terdaftar lewat luar aplikasi");
			tbmuser.setUserRole(ConstantValues.roleKomunitas);
			tbmuser.setIs_encripted(false);
			tbmuser.setUserPassword(username);
			tbmuser.setUserNama(userNama);
			session.getTransaction().begin();
			session.save(tbmuser);
			session.getTransaction().commit();
		} else {
			if (tbmuser.getUserNama() == null || !tbmuser.getUserNama().trim().equals(userNama)) {
				tbmuser.setUserNama(userNama);
				session.getTransaction().begin();
				Common.refreshUpdate(session, (tbmuser));
				session.getTransaction().commit();
			}
		}

		Anggota anggota = (Anggota) session.createCriteria(Anggota.class)
				.add(Restrictions.or(Restrictions.eq("email", username), Restrictions.eq("tbmuser", tbmuser)))
				.setMaxResults(1).uniqueResult();
		if (anggota == null) {
			anggota = new Anggota();
			anggota.setAktif(false);
			anggota.setAlamat("");
			anggota.setEmail(username);
			anggota.setJenisAnggota(LibraryUtil.ANGGOTA_REGULER);
			anggota.setJenisIdentitasAnggota(LibraryUtil.EMAIL);
			anggota.setJenisIdentitas("Email");
			anggota.setKeterangan("Anggota ini mendaftar melalui pendaftaran online");
			anggota.setKodeIdentitas(username);
			anggota.setNama(userNama);
			anggota.setPerpustakaan(null);
			anggota.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
			anggota.setTbmuser(tbmuser);
			anggota.setTipeAnggota(LibraryUtil.UMUM);
			anggota.setTipe(LibraryUtil.UMUM.getNama());
			session.getTransaction().begin();
			session.save(anggota);
			session.getTransaction().commit();
		} else {
			anggota = (Anggota) session.createCriteria(Anggota.class).add(Restrictions.eq("email", username))
					.add(Restrictions.ne("tbmuser", tbmuser)).setMaxResults(1).uniqueResult();
			if (anggota != null) {
				anggota.setTbmuser(tbmuser);
				session.getTransaction().begin();
				Common.refreshUpdate(session, (anggota));
				session.getTransaction().commit();
			}
		}

		Pengarang pengarang = (Pengarang) session.createCriteria(Pengarang.class)
				.add(Restrictions.eq("userIdPengarang", tbmuser)).setMaxResults(1).uniqueResult();
		if (pengarang == null) {
			pengarang = new Pengarang();
			pengarang.setKeterangan("Pengarang ini daftar lewat aplikasi luar");
			pengarang.setNama(userNama);
			pengarang.setUserIdPengarang(tbmuser);
			session.getTransaction().begin();
			session.save(pengarang);
			session.getTransaction().commit();
		} else {
			if (pengarang.getNama() == null || !pengarang.getNama().trim().equals(userNama)) {
				pengarang.setNama(userNama);
				session.getTransaction().begin();
				Common.refreshUpdate(session, (pengarang));
				session.getTransaction().commit();
			}
		}

		CommonID commonID = new CommonID();
		commonID.setInfo1(tbmuser.getUserId());
		commonID.setInfo2(tbmuser.getUserNama());
		commonID.setInfo3(pengarang.getId() + "");

		HibernateUtil.closeSession();
		return commonID;
	}

	@GET
	@Path("daftar_item/{username}/{parent}/{nama}/{pengarang}/{keyword}/{abstrack}/{institusi}/{order}/{order1}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/** Varian tanpa {@code status}/paging dari {@link #daftarItemJurnal(String, String, String, String, String, String, String, String, String, String, String, String)}; default status {@code "PUBLISH"} (lihat catatan pada overload 10-parameter tentang bug default paging). */
	public List<CommonID> daftarItemJurnal(@PathParam("username") String username, @PathParam("parent") String parent,
			@PathParam("nama") String nama, @PathParam("pengarang") String pengarang,
			@PathParam("keyword") String keyword, @PathParam("abstrack") String abstrack,
			@PathParam("institusi") String institusi, @PathParam("order") String order,
			@PathParam("order1") String order1) throws Exception {
		return daftarItemJurnal(username, parent, nama, pengarang, keyword, abstrack, institusi, order, order1,
				"PUBLISH");
	}

	@GET
	@Path("daftar_item/{username}/{parent}/{nama}/{pengarang}/{keyword}/{abstrack}/{institusi}/{order}/{order1}/{status}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Varian dengan {@code status} tapi tanpa paging eksplisit dari
	 * {@link #daftarItemJurnal(String, String, String, String, String, String, String, String, String, String, String, String)}.
	 *
	 * <p>
	 * <b>Catatan:</b> implementasi ini mendelegasikan ke overload 9-parameter dengan
	 * {@code order="0", order1="10"} — {@code status} yang diterima TIDAK diteruskan (hilang), dan
	 * nilai {@code "0"}/{@code "10"} yang dimaksudkan sebagai default {@code start}/{@code banyak}
	 * pada overload 9-parameter justru dipakai sebagai nama kolom pengurutan
	 * ({@code order}/{@code order1}), bukan sebagai parameter paging. Perilaku ini dipertahankan
	 * apa adanya (tidak diubah) sesuai cakupan dokumentasi ini.
	 * </p>
	 */
	public List<CommonID> daftarItemJurnal(@PathParam("username") String username, @PathParam("parent") String parent,
			@PathParam("nama") String nama, @PathParam("pengarang") String pengarang,
			@PathParam("keyword") String keyword, @PathParam("abstrack") String abstrack,
			@PathParam("institusi") String institusi, @PathParam("order") String order,
			@PathParam("order1") String order1, @PathParam("status") String status) throws Exception {
		return daftarItemJurnal(username, parent, nama, pengarang, keyword, abstrack, institusi, "0", "10");
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_item/{username}/{parent}/{nama}/{pengarang}/{keyword}/{abstrack}/{institusi}/{order}/{order1}/{status}/{start}/{banyak}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Implementasi kanonik pencarian karya ilmiah ({@link Item} bertipe {@code KARYA_ILMIAH}) milik
	 * {@code username} (sebagai {@code dibuatOleh}). Mendukung filter nama/tema, keyword, abstrak,
	 * pengarang, institusi (dicocokkan ke penerbit1..5, ILIKE), status terbit, dan folder induk
	 * ({@code parent}, diperluas ke seluruh keturunannya lewat
	 * {@link PerpustakaanResourcesHelper#generateChildsByIds}), diurutkan oleh dua kolom
	 * ({@code order}/{@code order1}) dan dipaginasi ({@code start}/{@code banyak}). Setiap hasil
	 * dipetakan ke {@link CommonID} dengan info1..info21 mencakup nama, satuan kerja, tanggal,
	 * keyword, jumlah komentar/unduhan/dilihat, penerbit1..5, status terbit, domain penelitian,
	 * daftar pemeriksa, dan path folder induk.
	 *
	 * @throws com.sun.jersey.api.NotFoundException bila {@code username} tidak dikenali
	 */
	public List<CommonID> daftarItemJurnal(@PathParam("username") String username, @PathParam("parent") String parent,
			@PathParam("nama") String nama, @PathParam("pengarang") String pengarang,
			@PathParam("keyword") String keyword, @PathParam("abstrack") String abstrack,
			@PathParam("institusi") String institusi, @PathParam("order") String order,
			@PathParam("order1") String order1, @PathParam("status") String status, @PathParam("start") String start,
			@PathParam("banyak") String banyak) throws Exception {
		List<CommonID> commonIDs = new ArrayList<CommonID>();
		Session session = HibernateUtil.currentNativeSession();

		username = URLDecoder.decode(username, "UTF-8");
		username = username.trim().equals("_") ? "" : username.trim();
		Tbmuser tbmuser = (Tbmuser) (session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.eq("email", username), Restrictions.idEq(username))).uniqueResult());

		if (tbmuser == null || tbmuser.getUserId() == null) {

			HibernateUtil.closeSession();
			throw new NotFoundException("Login pengguna gagal dilakukan");
		}

		nama = URLDecoder.decode(nama, "UTF-8");
		pengarang = URLDecoder.decode(pengarang, "UTF-8");
		keyword = URLDecoder.decode(keyword, "UTF-8");
		abstrack = URLDecoder.decode(abstrack, "UTF-8");
		institusi = URLDecoder.decode(institusi, "UTF-8");

		Set<Long> parents = parent.trim().equals("") || parent.trim().equals("_") || parent.trim().equals("-1") ? null
				: new HashSet<Long>();

		if (parents != null) {
			parents.add(Long.parseLong(parent.trim()));
			PerpustakaanResourcesHelper perpustakaanResourcesHelper = new PerpustakaanResourcesHelper();
			perpustakaanResourcesHelper.generateChildsByIds(session, Long.parseLong(parent.trim()), parents);
		}

		System.out.println("parents = " + parents);

		Criterion criterion = Restrictions.or(Restrictions.ilike("penerbit.nama", institusi, MatchMode.ANYWHERE),
				Restrictions.ilike("penerbit2.nama", institusi, MatchMode.ANYWHERE));

		criterion = Restrictions.or(criterion, Restrictions.ilike("penerbit3.nama", institusi, MatchMode.ANYWHERE));

		criterion = Restrictions.or(criterion, Restrictions.ilike("penerbit4.nama", institusi, MatchMode.ANYWHERE));

		criterion = Restrictions.or(criterion, Restrictions.ilike("penerbit5.nama", institusi, MatchMode.ANYWHERE));

		List<Item> items = session.createCriteria(Item.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.createAlias("statusTerbitItem", "statusTerbitItem", Criteria.LEFT_JOIN)
				.add(Restrictions.eq("dibuatOleh", tbmuser)).createAlias("penerbit", "penerbit", Criteria.LEFT_JOIN)
				.createAlias("penerbit2", "penerbit2", Criteria.LEFT_JOIN)
				.createAlias("penerbit3", "penerbit3", Criteria.LEFT_JOIN)
				.createAlias("penerbit4", "penerbit4", Criteria.LEFT_JOIN)
				.createAlias("penerbit5", "penerbit5", Criteria.LEFT_JOIN)

				.add(Restrictions.eq("tipeItem", LibraryUtil.KARYA_ILMIAH))
				.add(status.trim().equals("") || status.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("statusTerbitItem.nama", status, MatchMode.ANYWHERE))

				.add(Restrictions.eq("folder", false))
				.addOrder(Order.desc(order.equals("_") || order.equals("") ? "tanggalterbit" : order))
				.addOrder(Order.desc(order1.equals("_") || order1.equals("") ? "tanggalterbit" : order1))
				.addOrder(Order.desc("id"))
				.add(nama.trim().equals("") || nama.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("nama", nama, MatchMode.ANYWHERE),
								Restrictions.ilike("tema", nama, MatchMode.ANYWHERE)))

				.add(keyword.trim().equals("") || keyword.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("kewords", keyword, MatchMode.ANYWHERE),
								Restrictions.ilike("kewordsEn", keyword, MatchMode.ANYWHERE)))

				.add(abstrack.trim().equals("") || abstrack.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("abstrak", abstrack, MatchMode.ANYWHERE),
								Restrictions.ilike("abstrakEn", abstrack, MatchMode.ANYWHERE)))

				.add(pengarang.trim().equals("") || pengarang.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("pengarangs", pengarang, MatchMode.ANYWHERE))

				.add(institusi.trim().equals("") || institusi.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: criterion)

				.add(parents == null || parents.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("parent.id", parents))
				.setFirstResult(Integer.parseInt(start)).setMaxResults(Integer.parseInt(banyak.trim())).list();

		for (Item item : items) {

			List<Tbmuser> tbmusers = session.createCriteria(ItemPunyaPemeriksa.class).add(Restrictions.eq("item", item))
					.setProjection(Projections.property("pemeriksa")).list();

			CommonID commonID = new CommonID();
			commonID.setId(item.getId());
			commonID.setInfo1(item.getNama());
			commonID.setInfo2(item.getDefaultSatuanKerja() == null ? "" : item.getDefaultSatuanKerja().toString());
			commonID.setInfo3(item.getTanggal() == null ? "" : Common.dateFormat6.get().format(item.getTanggal()));
			commonID.setInfo4(item.getCatatan());
			commonID.setInfo5(item.getKewords());
			commonID.setInfo6(item.getPengarangs());
			commonID.setInfo8(
					item.getTanggalterbit() == null ? "" : Common.dateFormat6.get().format(item.getTanggalterbit()));
			commonID.setInfo9(item.getAbstrak());

			int count = ((Number) session.createCriteria(ItemKomentar.class).setProjection(Projections.rowCount())
					.add(Restrictions.eq("item", item)).uniqueResult()).intValue();
			commonID.setInfo10(Common.numberFormat.get().format(count));

			// System.out.println("item.getJumlahDidownload() = "
			// + item.getJumlahDidownload());

			commonID.setInfo11(
					item.getJumlahDidownload() == null ? "0" : Common.numberFormat.get().format(item.getJumlahDidownload()));
			commonID.setInfo12(Common.numberFormat.get().format(item.getJumlahDilihat()));

			commonID.setInfo13(item.getPenerbit() == null ? "" : item.getPenerbit().getNama());
			commonID.setInfo14(item.getPenerbit2() == null ? "" : item.getPenerbit2().getNama());
			commonID.setInfo15(item.getPenerbit3() == null ? "" : item.getPenerbit3().getNama());
			commonID.setInfo16(item.getPenerbit4() == null ? "" : item.getPenerbit4().getNama());
			commonID.setInfo17(item.getPenerbit5() == null ? "" : item.getPenerbit5().getNama());
			commonID.setInfo19(item.getStatusTerbitItem() == null ? "" : item.getStatusTerbitItem().getNama());
			commonID.setInfo20(item.getDomainPenelitian() == null ? "" : item.getDomainPenelitian().getNama());
			commonID.setInfo21(tbmusers.toString());

			try {
				String directory = "";
				Item parentItem = item.getParent();
				while (parentItem != null) {
					directory = parentItem.getNama() + "/" + directory;
					parentItem = parentItem.getParent();
				}
				commonID.setInfo7("/" + directory);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();
		return commonIDs;
	}

	@GET
	@Path("daftar_item_mereka/{username}/{parent}/{nama}/{pengarang}/{keyword}/{abstrack}/{institusi}/{order}/{order1}/{status}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/** Varian tanpa paging eksplisit dari {@link #daftarItemJurnalMereka(String, String, String, String, String, String, String, String, String, String, String, String)}; default {@code start=0, banyak=10}. */
	public List<CommonID> daftarItemJurnalMereka(@PathParam("username") String username,
			@PathParam("parent") String parent, @PathParam("nama") String nama,
			@PathParam("pengarang") String pengarang, @PathParam("keyword") String keyword,
			@PathParam("abstrack") String abstrack, @PathParam("institusi") String institusi,
			@PathParam("order") String order, @PathParam("order1") String order1, @PathParam("status") String status)
			throws Exception {
		return daftarItemJurnalMereka(username, parent, nama, pengarang, keyword, abstrack, institusi, order, order1,
				status, "0", "10");
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_item_mereka/{username}/{parent}/{nama}/{pengarang}/{keyword}/{abstrack}/{institusi}/{order}/{order1}/{status}/{start}/{banyak}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Seperti {@link #daftarItemJurnal(String, String, String, String, String, String, String, String, String, String, String, String)},
	 * tapi hasilnya dibatasi pada karya ilmiah yang {@code username} bertindak sebagai
	 * {@link ItemPunyaPemeriksa} (pemeriksa/reviewer)-nya — dipanggil dari sisi query yang meng-query
	 * {@link ItemPunyaPemeriksa} lalu mengambil {@code item} terkait, bukan langsung dari {@link Item}.
	 *
	 * @throws com.sun.jersey.api.NotFoundException bila {@code username} tidak dikenali
	 */
	public List<CommonID> daftarItemJurnalMereka(@PathParam("username") String username,
			@PathParam("parent") String parent, @PathParam("nama") String nama,
			@PathParam("pengarang") String pengarang, @PathParam("keyword") String keyword,
			@PathParam("abstrack") String abstrack, @PathParam("institusi") String institusi,
			@PathParam("order") String order, @PathParam("order1") String order1, @PathParam("status") String status,
			@PathParam("start") String start, @PathParam("banyak") String banyak) throws Exception {
		List<CommonID> commonIDs = new ArrayList<CommonID>();
		Session session = HibernateUtil.currentNativeSession();

		username = URLDecoder.decode(username, "UTF-8");
		username = username.trim().equals("_") ? "" : username.trim();
		Tbmuser tbmuser = (Tbmuser) (session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.eq("email", username), Restrictions.idEq(username))).uniqueResult());

		if (tbmuser == null || tbmuser.getUserId() == null) {

			HibernateUtil.closeSession();
			throw new NotFoundException("Login pengguna gagal dilakukan");
		}

		nama = URLDecoder.decode(nama, "UTF-8");
		pengarang = URLDecoder.decode(pengarang, "UTF-8");
		keyword = URLDecoder.decode(keyword, "UTF-8");
		abstrack = URLDecoder.decode(abstrack, "UTF-8");
		institusi = URLDecoder.decode(institusi, "UTF-8");

		Set<Long> parents = parent.trim().equals("") || parent.trim().equals("_") || parent.trim().equals("-1") ? null
				: new HashSet<Long>();

		if (parents != null) {
			parents.add(Long.parseLong(parent.trim()));
			PerpustakaanResourcesHelper perpustakaanResourcesHelper = new PerpustakaanResourcesHelper();
			perpustakaanResourcesHelper.generateChildsByIds(session, Long.parseLong(parent.trim()), parents);
		}

		System.out.println("parents = " + parents);

		Criterion criterion = Restrictions.or(Restrictions.ilike("penerbit.nama", institusi, MatchMode.ANYWHERE),
				Restrictions.ilike("penerbit2.nama", institusi, MatchMode.ANYWHERE));

		criterion = Restrictions.or(criterion, Restrictions.ilike("penerbit3.nama", institusi, MatchMode.ANYWHERE));

		criterion = Restrictions.or(criterion, Restrictions.ilike("penerbit4.nama", institusi, MatchMode.ANYWHERE));

		criterion = Restrictions.or(criterion, Restrictions.ilike("penerbit5.nama", institusi, MatchMode.ANYWHERE));

		List<ItemPunyaPemeriksa> itemPunyaPemeriksas = session.createCriteria(ItemPunyaPemeriksa.class)
				.add(Restrictions.eq("pemeriksa", tbmuser)).createCriteria("item")
				.createAlias("statusTerbitItem", "statusTerbitItem", Criteria.LEFT_JOIN)
				.createAlias("penerbit", "penerbit", Criteria.LEFT_JOIN)
				.createAlias("penerbit2", "penerbit2", Criteria.LEFT_JOIN)
				.createAlias("penerbit3", "penerbit3", Criteria.LEFT_JOIN)
				.createAlias("penerbit4", "penerbit4", Criteria.LEFT_JOIN)
				.createAlias("penerbit5", "penerbit5", Criteria.LEFT_JOIN)

				.add(Restrictions.eq("tipeItem", LibraryUtil.KARYA_ILMIAH))
				.add(status.trim().equals("") || status.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("statusTerbitItem.nama", status, MatchMode.ANYWHERE))

				.add(Restrictions.eq("folder", false))
				.addOrder(Order.desc(order.equals("_") || order.equals("") ? "tanggalterbit" : order))
				.addOrder(Order.desc(order1.equals("_") || order1.equals("") ? "tanggalterbit" : order1))
				.addOrder(Order.desc("id"))
				.add(nama.trim().equals("") || nama.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("nama", nama, MatchMode.ANYWHERE),
								Restrictions.ilike("tema", nama, MatchMode.ANYWHERE)))

				.add(keyword.trim().equals("") || keyword.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("kewords", keyword, MatchMode.ANYWHERE),
								Restrictions.ilike("kewordsEn", keyword, MatchMode.ANYWHERE)))

				.add(abstrack.trim().equals("") || abstrack.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("abstrak", abstrack, MatchMode.ANYWHERE),
								Restrictions.ilike("abstrakEn", abstrack, MatchMode.ANYWHERE)))

				.add(pengarang.trim().equals("") || pengarang.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("pengarangs", pengarang, MatchMode.ANYWHERE))

				.add(institusi.trim().equals("") || institusi.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: criterion)

				.add(parents == null || parents.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("parent.id", parents))
				.setFirstResult(Integer.parseInt(start)).setMaxResults(Integer.parseInt(banyak.trim())).list();

		for (ItemPunyaPemeriksa itemPunyaPemeriksa : itemPunyaPemeriksas) {
			Item item = itemPunyaPemeriksa.getItem();
			CommonID commonID = new CommonID();
			commonID.setId(item.getId());
			commonID.setInfo1(item.getNama());
			commonID.setInfo2(item.getDefaultSatuanKerja() == null ? "" : item.getDefaultSatuanKerja().toString());
			commonID.setInfo3(item.getTanggal() == null ? "" : Common.dateFormat6.get().format(item.getTanggal()));
			commonID.setInfo4(item.getCatatan());
			commonID.setInfo5(item.getKewords());
			commonID.setInfo6(item.getPengarangs());
			commonID.setInfo8(
					item.getTanggalterbit() == null ? "" : Common.dateFormat6.get().format(item.getTanggalterbit()));
			commonID.setInfo9(item.getAbstrak());

			int count = ((Number) session.createCriteria(ItemKomentar.class).setProjection(Projections.rowCount())
					.add(Restrictions.eq("item", item)).uniqueResult()).intValue();
			commonID.setInfo10(Common.numberFormat.get().format(count));
			commonID.setInfo11(
					item.getJumlahDidownload() == null ? "0" : Common.numberFormat.get().format(item.getJumlahDidownload()));
			commonID.setInfo12(Common.numberFormat.get().format(item.getJumlahDilihat()));

			commonID.setInfo13(item.getPenerbit() == null ? "" : item.getPenerbit().getNama());
			commonID.setInfo14(item.getPenerbit2() == null ? "" : item.getPenerbit2().getNama());
			commonID.setInfo15(item.getPenerbit3() == null ? "" : item.getPenerbit3().getNama());
			commonID.setInfo16(item.getPenerbit4() == null ? "" : item.getPenerbit4().getNama());
			commonID.setInfo17(item.getPenerbit5() == null ? "" : item.getPenerbit5().getNama());
			commonID.setInfo19(item.getStatusTerbitItem() == null ? "" : item.getStatusTerbitItem().getNama());
			commonID.setInfo20(itemPunyaPemeriksa.getStatus());
			commonID.setInfo21(itemPunyaPemeriksa.getId() + "");

			commonID.setInfo22(item.getDomainPenelitian() == null ? "" : item.getDomainPenelitian().getNama());

			List<Tbmuser> tbmusers = session.createCriteria(ItemPunyaPemeriksa.class).add(Restrictions.eq("item", item))
					.setProjection(Projections.property("pemeriksa")).list();

			commonID.setInfo23(tbmusers.toString());

			try {
				String directory = "";
				Item parentItem = item.getParent();
				while (parentItem != null) {
					directory = parentItem.getNama() + "/" + directory;
					parentItem = parentItem.getParent();
				}
				commonID.setInfo7("/" + directory);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();
		return commonIDs;
	}

	@GET
	@Path("daftar_item_komentar_semua/{username}/{parent}/{satuanKerja}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/** Varian tanpa filter status dari {@link #daftarItemKomentarSemua(String, String, String, String, String, String, String)}. */
	public List<CommonID> daftarItemKomentarSemua(@PathParam("username") String username,
			@PathParam("parent") String parent, @PathParam("satuanKerja") String satuanKerja) throws Exception {
		return daftarItemKomentarSemua(username, parent, satuanKerja, "", "");
	}

	@GET
	@Path("daftar_item_komentar_semua/{username}/{parent}/{satuanKerja}/{status}/{tidakStatus}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Varian tanpa paging dari {@link #daftarItemKomentarSemua(String, String, String, String, String, String, String)}.
	 *
	 * <p>
	 * <b>Catatan:</b> delegasi ini meneruskan {@code tidakStatus} pada posisi parameter
	 * {@code status} maupun {@code tidakStatus} sekaligus (parameter {@code status} yang diterima
	 * TIDAK dipakai) — perilaku dipertahankan apa adanya sesuai cakupan dokumentasi ini.
	 * </p>
	 */
	public List<CommonID> daftarItemKomentarSemua(@PathParam("username") String username,
			@PathParam("parent") String parent, @PathParam("satuanKerja") String satuanKerja,
			@PathParam("status") String status, @PathParam("tidakStatus") String tidakStatus) throws Exception {
		return daftarItemKomentarSemua(username, parent, satuanKerja, tidakStatus, tidakStatus, "0", "10");
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_item_komentar_semua/{username}/{parent}/{satuanKerja}/{status}/{tidakStatus}/{start}/{banyak}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Implementasi kanonik: mengembalikan daftar {@link ItemKomentar} pada karya ilmiah milik
	 * {@code username} (sebagai {@code item.dibuatOleh}), difilter status terbit item pada saat
	 * komentar dibuat ({@code status} untuk menyertakan, {@code tidakStatus} untuk mengecualikan;
	 * kasus khusus {@code status="Terbit"} juga menyertakan item yang statusnya belum tercatat/null),
	 * folder induk ({@code parent}, diperluas ke keturunannya), dan satuan kerja, terurut komentar
	 * terbaru lebih dulu. Hasil dipetakan ke {@link CommonID}: nama pengomentar, kontak, tanggal,
	 * id item, id komentar.
	 *
	 * @throws com.sun.jersey.api.NotFoundException bila {@code username} tidak dikenali
	 */
	public List<CommonID> daftarItemKomentarSemua(@PathParam("username") String username,
			@PathParam("parent") String parent, @PathParam("satuanKerja") String satuanKerja,
			@PathParam("status") String status, @PathParam("tidakStatus") String tidakStatus,
			@PathParam("start") String start, @PathParam("banyak") String banyak) throws Exception {

		List<CommonID> commonIDs = new ArrayList<CommonID>();
		Session session = HibernateUtil.currentNativeSession();

		tidakStatus = URLDecoder.decode(tidakStatus, "UTF-8");
		status = URLDecoder.decode(status, "UTF-8");
		username = URLDecoder.decode(username, "UTF-8");
		username = username.trim().equals("_") ? "" : username.trim();
		Tbmuser tbmuser = (Tbmuser) (session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.eq("email", username), Restrictions.idEq(username))).uniqueResult());

		if (tbmuser == null || tbmuser.getUserId() == null) {

			HibernateUtil.closeSession();
			throw new NotFoundException("Login pengguna gagal dilakukan");
		}

		Set<Long> parents = parent.trim().equals("") || parent.trim().equals("_") || parent.trim().equals("-1") ? null
				: new HashSet<Long>();

		if (parents != null) {
			parents.add(Long.parseLong(parent.trim()));
			PerpustakaanResourcesHelper perpustakaanResourcesHelper = new PerpustakaanResourcesHelper();
			perpustakaanResourcesHelper.generateChildsByIds(session, Long.parseLong(parent.trim()), parents);
		}

		System.out.println("parents = " + parents);

		List<ItemKomentar> komentarItems = session.createCriteria(ItemKomentar.class).createAlias("item", "item")
				.createAlias("statusTerbitItemPadaSaatKomentar", "statusTerbitItemPadaSaatKomentar",
						Criteria.LEFT_JOIN)
				.add(status.trim().equalsIgnoreCase("Terbit")
						? Restrictions.or(Restrictions.isNull("statusTerbitItemPadaSaatKomentar"),
								Restrictions.ilike("statusTerbitItemPadaSaatKomentar.nama", status, MatchMode.EXACT))
						: status.trim().equals("") || status.trim().equals("_") || status.trim().equals("-1")
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.ilike("statusTerbitItemPadaSaatKomentar.nama", status, MatchMode.EXACT))
				.add(tidakStatus.trim().equals("") || tidakStatus.trim().equals("_") || tidakStatus.trim().equals("-1")
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.ilike("statusTerbitItemPadaSaatKomentar.nama", tidakStatus,
								MatchMode.EXACT)))
				.add(Restrictions.eq("item.dibuatOleh", tbmuser))
				.add(parents == null || parents.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("item.parent.id", parents))
				.add(satuanKerja.trim().equals("") || satuanKerja.trim().equals("_") || satuanKerja.trim().equals("-1")
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("item.defaultSatuanKerja.id", Long.parseLong(satuanKerja)))
				.addOrder(Order.desc("tanggal_dirubah")).setFirstResult(Integer.parseInt(start))
				.setMaxResults(Integer.parseInt(banyak.trim())).list();

		for (ItemKomentar komentarItem : komentarItems) {
			CommonID commonID = new CommonID();
			commonID.setInfo1(komentarItem.getNama());
			commonID.setInfo2(komentarItem.getKontak());
			commonID.setInfo3(komentarItem.getTanggal_dirubah() == null ? ""
					: Common.dateFormat6.get().format(komentarItem.getTanggal_dirubah()));
			commonID.setInfo4(komentarItem.getItem().getId() + "");
			commonID.setInfo5(komentarItem.getId() + "");
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();

		return commonIDs;
	}

	@GET
	@Path("daftar_item_komentar_semua_mereka/{username}/{parent}/{satuanKerja}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/** Varian tanpa filter status dari {@link #daftarItemKomentarSemuaMereka(String, String, String, String, String, String, String)}. */
	public List<CommonID> daftarItemKomentarSemuaMereka(@PathParam("username") String username,
			@PathParam("parent") String parent, @PathParam("satuanKerja") String satuanKerja) throws Exception {
		return daftarItemKomentarSemuaMereka(username, parent, satuanKerja, "", "");
	}

	@GET
	@Path("daftar_item_komentar_semua_mereka/{username}/{parent}/{satuanKerja}/{status}/{tidakStatus}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Varian tanpa paging dari {@link #daftarItemKomentarSemuaMereka(String, String, String, String, String, String, String)}.
	 * Catatan: sama seperti overload sejenis pada {@link #daftarItemKomentarSemua(String, String, String, String, String)},
	 * parameter {@code status} yang diterima tidak dipakai — {@code tidakStatus} diteruskan pada kedua posisi.
	 */
	public List<CommonID> daftarItemKomentarSemuaMereka(@PathParam("username") String username,
			@PathParam("parent") String parent, @PathParam("satuanKerja") String satuanKerja,
			@PathParam("status") String status, @PathParam("tidakStatus") String tidakStatus) throws Exception {
		return daftarItemKomentarSemuaMereka(username, parent, satuanKerja, tidakStatus, tidakStatus, "0", "10");
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_item_komentar_semua_mereka/{username}/{parent}/{satuanKerja}/{status}/{tidakStatus}/{start}/{banyak}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Seperti {@link #daftarItemKomentarSemua(String, String, String, String, String, String, String)},
	 * tapi dibatasi pada komentar atas item yang {@code username} bertindak sebagai
	 * {@link ItemPunyaPemeriksa}-nya, bukan item yang dibuatnya sendiri.
	 *
	 * @throws com.sun.jersey.api.NotFoundException bila {@code username} tidak dikenali
	 */
	public List<CommonID> daftarItemKomentarSemuaMereka(@PathParam("username") String username,
			@PathParam("parent") String parent, @PathParam("satuanKerja") String satuanKerja,
			@PathParam("status") String status, @PathParam("tidakStatus") String tidakStatus,
			@PathParam("start") String start, @PathParam("banyak") String banyak) throws Exception {

		List<CommonID> commonIDs = new ArrayList<CommonID>();
		Session session = HibernateUtil.currentNativeSession();

		tidakStatus = URLDecoder.decode(tidakStatus, "UTF-8");
		status = URLDecoder.decode(status, "UTF-8");
		username = URLDecoder.decode(username, "UTF-8");
		username = username.trim().equals("_") ? "" : username.trim();
		Tbmuser tbmuser = (Tbmuser) (session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.eq("email", username), Restrictions.idEq(username))).uniqueResult());

		if (tbmuser == null || tbmuser.getUserId() == null) {

			HibernateUtil.closeSession();
			throw new NotFoundException("Login pengguna gagal dilakukan");
		}

		Set<Long> parents = parent.trim().equals("") || parent.trim().equals("_") || parent.trim().equals("-1") ? null
				: new HashSet<Long>();

		if (parents != null) {
			parents.add(Long.parseLong(parent.trim()));
			PerpustakaanResourcesHelper perpustakaanResourcesHelper = new PerpustakaanResourcesHelper();
			perpustakaanResourcesHelper.generateChildsByIds(session, Long.parseLong(parent.trim()), parents);
		}

		List<Long> itemPunyaPemeriksas = session.createCriteria(ItemPunyaPemeriksa.class).createAlias("item", "item")
				.setProjection(Projections.property("item.id")).add(Restrictions.eq("pemeriksa", tbmuser)).list();

		List<ItemKomentar> komentarItems = session.createCriteria(ItemKomentar.class).createAlias("item", "item")
				.createAlias("statusTerbitItemPadaSaatKomentar", "statusTerbitItemPadaSaatKomentar",
						Criteria.LEFT_JOIN)
				.add(status.trim().equalsIgnoreCase("Terbit")
						? Restrictions.or(Restrictions.isNull("statusTerbitItemPadaSaatKomentar"),
								Restrictions.ilike("statusTerbitItemPadaSaatKomentar.nama", status, MatchMode.EXACT))
						: status.trim().equals("") || status.trim().equals("_") || status.trim().equals("-1")
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.ilike("statusTerbitItemPadaSaatKomentar.nama", status, MatchMode.EXACT))
				.add(tidakStatus.trim().equals("") || tidakStatus.trim().equals("_") || tidakStatus.trim().equals("-1")
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.ilike("statusTerbitItemPadaSaatKomentar.nama", tidakStatus,
								MatchMode.EXACT)))
				.add(itemPunyaPemeriksas.size() == 0 ? Restrictions.sqlRestriction("1!=1")
						: Restrictions.in("item.id", itemPunyaPemeriksas))
				.add(parents == null || parents.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("item.parent.id", parents))
				.add(satuanKerja.trim().equals("") || satuanKerja.trim().equals("_") || satuanKerja.trim().equals("-1")
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("item.defaultSatuanKerja.id", Long.parseLong(satuanKerja)))
				.addOrder(Order.desc("tanggal_dirubah")).setFirstResult(Integer.parseInt(start))
				.setMaxResults(Integer.parseInt(banyak.trim())).list();

		for (ItemKomentar komentarItem : komentarItems) {
			CommonID commonID = new CommonID();
			commonID.setInfo1(komentarItem.getNama());
			commonID.setInfo2(komentarItem.getKontak());
			commonID.setInfo3(komentarItem.getTanggal_dirubah() == null ? ""
					: Common.dateFormat6.get().format(komentarItem.getTanggal_dirubah()));
			commonID.setInfo4(komentarItem.getItem().getId() + "");
			commonID.setInfo5(komentarItem.getId() + "");
			ItemPunyaPemeriksa itemPunyaPemeriksa = (ItemPunyaPemeriksa) session
					.createCriteria(ItemPunyaPemeriksa.class).add(Restrictions.eq("item", komentarItem.getItem()))
					.add(Restrictions.eq("pemeriksa", tbmuser))
					// .setProjection(Projections.property("id"))
					.setMaxResults(1).uniqueResult();
			commonID.setInfo6(itemPunyaPemeriksa == null ? "-1" : itemPunyaPemeriksa.getId() + "");
			commonID.setInfo7(itemPunyaPemeriksa == null ? "" : itemPunyaPemeriksa.getStatus() + "");

			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();

		return commonIDs;
	}

}
