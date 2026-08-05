package ais.action.master.resources;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.books.model.Volume;
import com.google.api.services.books.model.Volumes;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.spi.resource.Singleton;

import ais.action.master.helper.util.GoogleBookSynchronized;
import ais.action.master.helper.util.OpenLibrarySyncronizer;
import ais.action.master.library.util.BooksSample;
import ais.action.master.library.util.LibraryUtil;
import ais.action.master.resources.helper.PerpustakaanResourcesHelper;
import ais.action.master.resources.model.CommonID;
import ais.action.master.resources.model.PeminjamanItem;
import ais.action.master.resources.model.StokItem;
import ais.action.servlet.CheckISBN;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoInformasiPerpustakaan;
import ais.database.model.file.FotoItem;
import ais.database.model.library.Anggota;
import ais.database.model.library.BatasWaktuPeminjamanItem;
import ais.database.model.library.DataDdcItemDetail;
import ais.database.model.library.DdcItem;
import ais.database.model.library.DendaKeterlambatanItem;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.library.DomainPenelitian;
import ais.database.model.library.InformasiPerpustakaan;
import ais.database.model.library.InformasiPerpustakaanKomentar;
import ais.database.model.library.Item;
import ais.database.model.library.ItemKomentar;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.ItemPunyaTerbit;
import ais.database.model.library.ItemPunyaTerbitKomentar;
import ais.database.model.library.KategoriItem;
import ais.database.model.library.KembaliPengadaanItem;
import ais.database.model.library.KembaliPengadaanItemDetail;
import ais.database.model.library.KunjunganAnggota;
import ais.database.model.library.PeminjamanPengadaanItem;
import ais.database.model.library.PeminjamanPengadaanItemDetail;
import ais.database.model.library.Penerbit;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.PesananAnggota;
import ais.database.model.library.Pustakawan;
import ais.database.model.library.StatusTerbitItem;
import ais.database.model.library.UdcItem;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.WaktuUtil;

@Path("/perpustakaan")
@Singleton



public class PerpustakaanResource extends DataResource<Perpustakaan> {

	public PerpustakaanResource() {
		super(Perpustakaan.class);
	}

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public PerpustakaanResource getXml() {
		return this;
	}

	@GET
	@Path("load/{username}/{password}/{id}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public Perpustakaan getData(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("id") String id) {
		return super.getData(username, password, id);
	}

	@GET
	@Path("search/{username}/{password}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<Perpustakaan> getAllData(@PathParam("username") String username,
			@PathParam("password") String password) {
		return super.getAllData(username, password);
	}

	@GET
	@Path("search/{username}/{password}/{search}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<Perpustakaan> getAllData(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("search") String search) {
		return super.getAllData(username, password, search);
	}

	@GET
	@Path("search/{username}/{password}/{search}/{search1}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<Perpustakaan> getAllData(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("search") String search, @PathParam("search1") String search1) {
		return super.getAllData(username, password, search, search1);
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("satuan_kerja/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarSatuanKerja() {
		Session session = HibernateUtil.currentNativeSession();
		List<SatuanKerja> satuanKerjas = session.createCriteria(SatuanKerja.class).addOrder(Order.asc("id"))
				.add(Restrictions.eq("defaultItem", true)).list();
		List<CommonID> commonIDs = new ArrayList<CommonID>();
		for (SatuanKerja satuanKerja : satuanKerjas) {
			CommonID commonID = new CommonID(satuanKerja.getId());
			commonID.setInfo1(satuanKerja.getNama());
			commonID.setInfo2(satuanKerja.getParent() == null ? "" : satuanKerja.getParent().getId() + "");
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();
		return commonIDs;
	}

	@GET
	@Path("penerbit/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarPenerbit() throws Exception {
		return daftarPenerbit("");
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("penerbit/{nama}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarPenerbit(@PathParam("nama") String nama) throws Exception {
		nama = URLDecoder.decode(nama, "UTF-8");
		Session session = HibernateUtil.currentNativeSession();
		List<Penerbit> penerbits = session.createCriteria(Penerbit.class)
				.add(nama.trim().equals("") || nama.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama, MatchMode.ANYWHERE))
				.add(Restrictions.isNotNull("satuanKerja")).setMaxResults(Common.MAX_RESULT_20)
				.addOrder(Order.asc("nama")).list();
		List<CommonID> commonIDs = new ArrayList<CommonID>();
		for (Penerbit penerbit : penerbits) {
			CommonID commonID = new CommonID(penerbit.getId());
			commonID.setInfo1(penerbit.getNama());
			commonID.setInfo2(penerbit.getSatuanKerja() == null ? "" : penerbit.getSatuanKerja().toString());
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();
		return commonIDs;
	}

	@GET
	@Path("login/{username}/{password}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public Perpustakaan login(@PathParam("username") String username, @PathParam("password") String password)
			throws Exception {

		username = URLDecoder.decode(username, "UTF-8");
		password = URLDecoder.decode(password, "UTF-8");

		Session session = HibernateUtil.currentNativeSession();

		Tbmuser tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("userId", username))
				.add(Restrictions.eq("userPassword", Common.desEncrypter.get().encrypt(password))).setMaxResults(1)
				.uniqueResult();
		Pustakawan pustakawan = null;

		if (tbmuser != null) {
			pustakawan = (Pustakawan) session.createCriteria(Pustakawan.class).add(Restrictions.eq("tbmuser", tbmuser))
					.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
		}

		HibernateUtil.closeSession();

		if (pustakawan == null || pustakawan.getPerpustakaan() == null) {
			throw new NotFoundException("Login pengguna gagal dilakukan, karena data pustakawan tidak ditemukan");
		}

		if (tbmuser == null || tbmuser.getUserId() == null) {
			throw new NotFoundException("Login pengguna gagal dilakukan");
		}

		return pustakawan.getPerpustakaan();
	}

	@GET
	@Path("get_peminjaman/{kode}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public PeminjamanItem getPeminjaman(@PathParam("kode") String kode) throws Exception {
		return getPeminjaman(kode, null);
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("get_peminjaman_perpustakaan/{kode}/{perpustakaan}")
	@Produces({ MediaType.APPLICATION_JSON })
	public PeminjamanItem getPeminjaman(@PathParam("kode") String kode, @PathParam("perpustakaan") String perpustakaan)
			throws Exception {
		kode = URLDecoder.decode(kode, "UTF-8");
		Session session = HibernateUtil.currentNativeSession();
		PeminjamanPengadaanItem peminjamanPengadaanItem = (PeminjamanPengadaanItem) session
				.createCriteria(PeminjamanPengadaanItem.class)
				.add(perpustakaan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("perpustakaan.id", Long.parseLong(perpustakaan)))
				.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
		Anggota anggota = null;
		if (peminjamanPengadaanItem == null) {
			anggota = (Anggota) session.createCriteria(Anggota.class)
					.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
					.createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
					.add(Restrictions.or(
							Restrictions.or(Restrictions.eq("kode", kode.trim()),
									Restrictions.eq("mahasiswa.nim", kode.trim())),
							Restrictions.eq("dosen.mycode", kode.trim())))
					.setMaxResults(1).uniqueResult();
			peminjamanPengadaanItem = (PeminjamanPengadaanItem) session.createCriteria(PeminjamanPengadaanItem.class)
					.add(perpustakaan == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("perpustakaan.id", Long.parseLong(perpustakaan)))
					.add(Restrictions.eq("anggota", anggota)).addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();
		}

		PeminjamanItem peminjamanItem = new PeminjamanItem();
		if (peminjamanPengadaanItem != null) {
			anggota = peminjamanPengadaanItem.getAnggota();

			peminjamanItem.id = (peminjamanPengadaanItem.getId());
			peminjamanItem.alamat = (anggota.getAlamat());
			peminjamanItem.email = (anggota.getEmail());
			peminjamanItem.hp = (anggota.getHp());
			peminjamanItem.jenisIdentitas = (anggota.getJenisIdentitas());
			peminjamanItem.jenisIdentitasAnggota = (anggota.getJenisIdentitasAnggota());
			peminjamanItem.keterangan = (anggota.getKeterangan());
			peminjamanItem.kode = (anggota.getKode());
			peminjamanItem.kodePeminjaman = (peminjamanPengadaanItem.getKode());
			peminjamanItem.kodeIdentitas = (anggota.getKodeIdentitas());
			peminjamanItem.nama = (anggota.getNama());
			peminjamanItem.telp = (anggota.getNama());
			peminjamanItem.tipe = (anggota.getTipe());
			peminjamanItem.tipeAnggota = (anggota.getTipeAnggota());
			peminjamanItem.info = LibraryUtil.tampilanSummaryPeminjamanFormatDesktop(
					peminjamanPengadaanItem.getKembaliPengadaanItem(), peminjamanPengadaanItem);

			Mahasiswa mahasiswa = anggota.getMahasiswa();
			Integer jumlahMaksimalPerpanjanganPeminjaman = LibraryUtil
					.getJumlahMaksimalPerpanjanganPeminjaman(peminjamanPengadaanItem);

			peminjamanItem.perpanjang = jumlahMaksimalPerpanjanganPeminjaman == null ? 0
					: jumlahMaksimalPerpanjanganPeminjaman.intValue();

			Number jumlahMaksimalPeminjaman = (Number) session.createCriteria(BatasWaktuPeminjamanItem.class)

					.add(mahasiswa != null ? Restrictions.le("berlakuUntukSemester", mahasiswa.currentSemester())
							: Restrictions.isNull("berlakuUntukSemester"))

					.add(Restrictions.eq("perpustakaan", perpustakaan))
					.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
					.setProjection(Projections.property("jumlahMaksimalItemYangDipinjam"))
					.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();

			if (jumlahMaksimalPeminjaman == null) {
				jumlahMaksimalPeminjaman = (Number) session.createCriteria(BatasWaktuPeminjamanItem.class)

						.add(Restrictions.isNull("berlakuUntukSemester"))

						.add(Restrictions.eq("perpustakaan", perpustakaan))
						.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
						.setProjection(Projections.property("jumlahMaksimalItemYangDipinjam"))
						.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();
			}
			peminjamanItem.maksimal = jumlahMaksimalPeminjaman == null ? 0 : jumlahMaksimalPeminjaman.intValue();

			List<CommonVO> data = new ArrayList<CommonVO>();
			List<PeminjamanPengadaanItemDetail> peminjamanPengadaanItemDetails = session
					.createCriteria(PeminjamanPengadaanItemDetail.class)
					.add(Restrictions.eq("peminjamanPengadaanItem", peminjamanPengadaanItem)).list();
			for (PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail : peminjamanPengadaanItemDetails) {

				KembaliPengadaanItemDetail kembaliPengadaanItemDetail = peminjamanPengadaanItemDetail
						.getKembaliPengadaanItemDetail();
				peminjamanPengadaanItemDetail.setTanggalKembali(kembaliPengadaanItemDetail.getTanggal());
				DendaKeterlambatanItem dendaPerItem = LibraryUtil.hitungDendaItem(peminjamanPengadaanItemDetail);

				Double denda = dendaPerItem == null ? 0.0 : dendaPerItem.getDenda();
				denda = denda * peminjamanPengadaanItemDetail.getJumlah();

				data.add(new CommonVO(peminjamanPengadaanItemDetail.getId() + "",
						peminjamanPengadaanItemDetail.getItem().getIsbn(),
						peminjamanPengadaanItemDetail.getItem().getNama(),
						"Rp. " + Common.numberFormat.get().format(denda) + ",-",
						(kembaliPengadaanItemDetail == null ? "" : kembaliPengadaanItemDetail.getKeterangan()),
						(kembaliPengadaanItemDetail == null ? "0"
								: kembaliPengadaanItemDetail.getDikembali().intValue() + ""),
						peminjamanPengadaanItemDetail.getJumlahPerpanjangan().toString()));
			}
			peminjamanItem.jumlah = (double) peminjamanPengadaanItemDetails.size();
			peminjamanItem.data = data;
		} else {
			peminjamanItem.error = ("Kode " + kode + " tidak ditemukan");
		}

		HibernateUtil.closeSession();
		return peminjamanItem;
	}

	@GET
	@Path("cari_peminjaman/{kode}/{identitas}/{nama}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<PeminjamanItem> cariPeminjaman(@PathParam("kode") String kode, @PathParam("identitas") String identitas,
			@PathParam("nama") String nama) throws Exception {
		return cariPeminjaman(kode, identitas, nama, null);
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("cari_peminjaman_perpustakaan/{kode}/{identitas}/{nama}/{perpustakaan}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<PeminjamanItem> cariPeminjaman(@PathParam("kode") String kode, @PathParam("identitas") String identitas,
			@PathParam("nama") String nama, @PathParam("perpustakaan") String perpustakaan) throws Exception {
		List<PeminjamanItem> peminjamanItems = new ArrayList<PeminjamanItem>();

		kode = URLDecoder.decode(kode, "UTF-8");
		identitas = URLDecoder.decode(identitas, "UTF-8");
		nama = URLDecoder.decode(nama, "UTF-8");
		System.out.println("kode = " + kode + ", identitas = " + identitas + ", nama = " + nama);
		Session session = HibernateUtil.currentNativeSession();

		List<PeminjamanPengadaanItem> peminjamanPengadaanItems = session.createCriteria(PeminjamanPengadaanItem.class)
				.addOrder(Order.desc("id"))

				.add(perpustakaan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("perpustakaan.id", Long.parseLong(perpustakaan)))
				// .add(Restrictions.isNull("kembaliPengadaanItem"))
				.createAlias("anggota", "anggota")

				.add(kode.trim().equals("") || kode.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", kode, MatchMode.ANYWHERE))
				.add(identitas.trim().equals("") || identitas.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("anggota.kodeIdentitas", identitas, MatchMode.ANYWHERE))
				.add(nama.trim().equals("") || nama.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("anggota.nama", nama, MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT_20).list();

		// peminjamanPengadaanItems.addAll(peminjamanPengadaanItemsBelum);

		for (PeminjamanPengadaanItem peminjamanPengadaanItem : peminjamanPengadaanItems) {
			Anggota anggota = peminjamanPengadaanItem.getAnggota();
			PeminjamanItem peminjamanItem = new PeminjamanItem();
			peminjamanItem.id = (peminjamanPengadaanItem.getId());
			peminjamanItem.alamat = (anggota.getAlamat());
			peminjamanItem.email = (anggota.getEmail());
			peminjamanItem.hp = (anggota.getHp());
			peminjamanItem.jenisIdentitas = (anggota.getJenisIdentitas());
			peminjamanItem.jenisIdentitasAnggota = (anggota.getJenisIdentitasAnggota());
			peminjamanItem.keterangan = (anggota.getKeterangan());
			peminjamanItem.kode = (anggota.getKode());
			peminjamanItem.kodePeminjaman = (peminjamanPengadaanItem.getKode());
			peminjamanItem.kodeIdentitas = (anggota.getKodeIdentitas());
			peminjamanItem.nama = (anggota.getNama());
			peminjamanItem.telp = (anggota.getNama());
			peminjamanItem.tipe = (anggota.getTipe());
			peminjamanItem.tipeAnggota = (anggota.getTipeAnggota());
			peminjamanItem.info = LibraryUtil.tampilanSummaryPeminjamanFormatDesktop(
					peminjamanPengadaanItem.getKembaliPengadaanItem(), peminjamanPengadaanItem);

			Mahasiswa mahasiswa = anggota.getMahasiswa();

			Number jumlahMaksimalPerpanjanganPeminjaman = (Number) session
					.createCriteria(ais.database.model.library.BatasWaktuPeminjamanItem.class)

					.add(Restrictions.or(
							Restrictions.eq("jenisAnggota", peminjamanPengadaanItem.getAnggota().getJenisAnggota()),
							Restrictions.isNull("jenisAnggota")))
					.add(Restrictions.or(
							Restrictions.eq("tipeAnggota", peminjamanPengadaanItem.getAnggota().getTipeAnggota()),
							Restrictions.isNull("tipeAnggota")))
					.add(Restrictions
							.or(Restrictions.eq("fakultas", peminjamanPengadaanItem.getAnggota().getMahasiswa() != null
									? peminjamanPengadaanItem.getAnggota().getMahasiswa().getJurusan().getFakultas()
									: (peminjamanPengadaanItem.getAnggota().getDosen() != null
											? peminjamanPengadaanItem.getAnggota().getDosen().getFakultas() : null)),
									Restrictions.isNull("fakultas")))

					.add(Restrictions
							.or(Restrictions.eq("jurusan", peminjamanPengadaanItem.getAnggota().getMahasiswa() != null
									? peminjamanPengadaanItem.getAnggota().getMahasiswa().getJurusan()
									: (peminjamanPengadaanItem.getAnggota().getDosen() != null
											? peminjamanPengadaanItem.getAnggota().getDosen().getJurusan() : null)),
									Restrictions.isNull("jurusan")))

					.add(mahasiswa != null ? Restrictions.le("berlakuUntukSemester", mahasiswa.currentSemester())
							: Restrictions.isNull("berlakuUntukSemester"))
					.add(Restrictions.eq("perpustakaan.id", Long.parseLong(perpustakaan)))
					.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
					.setProjection(Projections.property("jumlahMaksimalPerpanjanganPeminjaman"))
					.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();
			if (jumlahMaksimalPerpanjanganPeminjaman == null) {
				jumlahMaksimalPerpanjanganPeminjaman = (Number) session
						.createCriteria(ais.database.model.library.BatasWaktuPeminjamanItem.class)
						.add(Restrictions.or(Restrictions.eq("jenisAnggota", anggota.getJenisAnggota()),
								Restrictions.isNull("jenisAnggota")))
						.add(Restrictions
								.or(Restrictions
										.eq("tipeAnggota",
												anggota.getTipeAnggota()),
										Restrictions.isNull("tipeAnggota")))
						.add(Restrictions.or(Restrictions.eq("fakultas",
								anggota.getMahasiswa() != null ? anggota.getMahasiswa().getJurusan().getFakultas()
										: (anggota.getDosen() != null ? anggota.getDosen().getFakultas() : null)),
								Restrictions.isNull("fakultas")))

						.add(Restrictions.or(Restrictions.eq("jurusan",
								anggota.getMahasiswa() != null ? anggota.getMahasiswa().getJurusan()
										: (anggota.getDosen() != null ? anggota.getDosen().getJurusan() : null)),
								Restrictions.isNull("jurusan")))

						.add(Restrictions.isNull("berlakuUntukSemester"))
						.add(Restrictions.eq("perpustakaan.id", Long.parseLong(perpustakaan)))
						.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
						.setProjection(Projections.property("jumlahMaksimalPerpanjanganPeminjaman"))
						.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();
			}

			peminjamanItem.perpanjang = jumlahMaksimalPerpanjanganPeminjaman == null ? 0
					: jumlahMaksimalPerpanjanganPeminjaman.intValue();

			Number jumlahMaksimalPeminjaman = (Number) session.createCriteria(BatasWaktuPeminjamanItem.class)

					.add(mahasiswa != null ? Restrictions.le("berlakuUntukSemester", mahasiswa.currentSemester())
							: Restrictions.isNull("berlakuUntukSemester"))

					.add(Restrictions.eq("perpustakaan", perpustakaan))
					.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
					.setProjection(Projections.property("jumlahMaksimalItemYangDipinjam"))
					.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();

			if (jumlahMaksimalPeminjaman == null) {
				jumlahMaksimalPeminjaman = (Number) session.createCriteria(BatasWaktuPeminjamanItem.class)

						.add(Restrictions.isNull("berlakuUntukSemester"))

						.add(Restrictions.eq("perpustakaan", perpustakaan))
						.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
						.setProjection(Projections.property("jumlahMaksimalItemYangDipinjam"))
						.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();
			}
			peminjamanItem.maksimal = jumlahMaksimalPeminjaman == null ? 0 : jumlahMaksimalPeminjaman.intValue();

			List<CommonVO> data = new ArrayList<CommonVO>();
			List<PeminjamanPengadaanItemDetail> peminjamanPengadaanItemDetails = session
					.createCriteria(PeminjamanPengadaanItemDetail.class)
					.add(Restrictions.eq("peminjamanPengadaanItem", peminjamanPengadaanItem)).list();
			for (PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail : peminjamanPengadaanItemDetails) {

				KembaliPengadaanItemDetail kembaliPengadaanItemDetail = peminjamanPengadaanItemDetail
						.getKembaliPengadaanItemDetail();

				peminjamanPengadaanItemDetail.setTanggalKembali(kembaliPengadaanItemDetail.getTanggal());
				DendaKeterlambatanItem dendaPerItem = LibraryUtil.hitungDendaItem(peminjamanPengadaanItemDetail);

				Double denda = dendaPerItem == null ? 0.0 : dendaPerItem.getDenda();
				denda = denda * peminjamanPengadaanItemDetail.getJumlah();

				data.add(new CommonVO(peminjamanPengadaanItemDetail.getId() + "",
						peminjamanPengadaanItemDetail.getItem().getIsbn(),
						peminjamanPengadaanItemDetail.getItem().getNama(),
						"Rp. " + Common.numberFormat.get().format(denda) + ",-",
						(kembaliPengadaanItemDetail == null ? "" : kembaliPengadaanItemDetail.getKeterangan()),
						(kembaliPengadaanItemDetail == null ? "0"
								: kembaliPengadaanItemDetail.getDikembali().intValue() + ""),
						peminjamanPengadaanItemDetail.getJumlahPerpanjangan().toString()));
			}
			peminjamanItem.jumlah = (double) peminjamanPengadaanItemDetails.size();
			peminjamanItem.data = data;
			peminjamanItems.add(peminjamanItem);
		}

		HibernateUtil.closeSession();
		return peminjamanItems;
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("cari_buku/{judul}/{isbn}/{pengarang}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> cariBuku(@PathParam("judul") String judul, @PathParam("isbn") String isbn,
			@PathParam("pengarang") String pengarang) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		judul = URLDecoder.decode(judul, "UTF-8");
		isbn = URLDecoder.decode(isbn, "UTF-8");
		pengarang = URLDecoder.decode(pengarang, "UTF-8");
		System.out.println("judul = " + judul + ", isbn = " + isbn + ", pengarang = " + pengarang);
		List<Item> items = new ArrayList<Item>();

		items = session.createCriteria(Item.class).addOrder(Order.asc("nama"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(judul.trim().equals("") || judul.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("pengarangs", pengarang, MatchMode.ANYWHERE))
				.add(judul.trim().equals("") || judul.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", judul, MatchMode.ANYWHERE))
				.add(isbn.trim().equals("") || isbn.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("isbn", isbn, MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT_20).list();

		List<CommonID> commonIDs = new ArrayList<CommonID>();
		for (Item item : items) {
			CommonID commonID = new CommonID(item.getId());
			commonID.setInfo1(item.getNama());
			commonID.setInfo2(item.getIsbn());
			commonID.setInfo3(item.getIssn());
			commonID.setInfo4(item.getAbstrak() == null || item.getAbstrak().trim().equals("") ? item.getCatatan()
					: item.getAbstrak());
			commonID.setInfo5(item.getPengarangs());
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();

		return commonIDs;
	}

	@GET
	@Path("anggota/{kode}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public Anggota getAnggota(@PathParam("kode") String kode) {
		Session session = HibernateUtil.currentNativeSession();
		Anggota anggota = (Anggota) session
				.createCriteria(Anggota.class).add(kode.trim().equals("") || kode.trim().equals("_")
						? Restrictions.sqlRestriction("1!=1") : Restrictions.ilike("kode", kode, MatchMode.EXACT))
				.setMaxResults(1).uniqueResult();
		if (anggota == null) {
			anggota = (Anggota) session.createCriteria(Anggota.class)
					.add(kode.trim().equals("") || kode.trim().equals("_") ? Restrictions.sqlRestriction("1!=1")
							: Restrictions.ilike("kodeIdentitas", kode, MatchMode.EXACT))
					.setMaxResults(1).uniqueResult();
		}

		HibernateUtil.closeSession();

		if (anggota != null && anggota.getMahasiswa() != null) {
			Mahasiswa mahasiswa = anggota.getMahasiswa();
			Number jumlahMaksimalPerpanjanganPeminjaman = (Number) session
					.createCriteria(ais.database.model.library.BatasWaktuPeminjamanItem.class)
					.add(Restrictions.or(Restrictions.eq("jenisAnggota", anggota.getJenisAnggota()),
							Restrictions.isNull("jenisAnggota")))
					.add(Restrictions.or(Restrictions.eq("tipeAnggota", anggota.getTipeAnggota()),
							Restrictions.isNull("tipeAnggota")))
					.add(Restrictions.or(Restrictions.eq("fakultas",
							anggota.getMahasiswa() != null ? anggota.getMahasiswa().getJurusan().getFakultas()
									: (anggota.getDosen() != null ? anggota.getDosen().getFakultas() : null)),
							Restrictions.isNull("fakultas")))

					.add(Restrictions.or(
							Restrictions.eq("jurusan",
									anggota.getMahasiswa() != null ? anggota.getMahasiswa().getJurusan()
											: (anggota.getDosen() != null ? anggota.getDosen().getJurusan() : null)),
							Restrictions.isNull("jurusan")))

					.add(mahasiswa != null ? Restrictions.le("berlakuUntukSemester", mahasiswa.currentSemester())
							: Restrictions.isNull("berlakuUntukSemester"))
					.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
					.setProjection(Projections.property("jumlahMaksimalPerpanjanganPeminjaman"))
					.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();
			if (jumlahMaksimalPerpanjanganPeminjaman == null) {
				jumlahMaksimalPerpanjanganPeminjaman = (Number) session
						.createCriteria(ais.database.model.library.BatasWaktuPeminjamanItem.class)
						.add(Restrictions.or(Restrictions.eq("jenisAnggota", anggota.getJenisAnggota()),
								Restrictions.isNull("jenisAnggota")))
						.add(Restrictions
								.or(Restrictions
										.eq("tipeAnggota",
												anggota.getTipeAnggota()),
										Restrictions.isNull("tipeAnggota")))
						.add(Restrictions.or(Restrictions.eq("fakultas",
								anggota.getMahasiswa() != null ? anggota.getMahasiswa().getJurusan().getFakultas()
										: (anggota.getDosen() != null ? anggota.getDosen().getFakultas() : null)),
								Restrictions.isNull("fakultas")))

						.add(Restrictions.or(Restrictions.eq("jurusan",
								anggota.getMahasiswa() != null ? anggota.getMahasiswa().getJurusan()
										: (anggota.getDosen() != null ? anggota.getDosen().getJurusan() : null)),
								Restrictions.isNull("jurusan")))

						.add(Restrictions.isNull("berlakuUntukSemester"))
						.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
						.setProjection(Projections.property("jumlahMaksimalPerpanjanganPeminjaman"))
						.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();
			}

			anggota.setPerpanjang(
					jumlahMaksimalPerpanjanganPeminjaman == null ? 0 : jumlahMaksimalPerpanjanganPeminjaman.intValue());

			Number jumlahMaksimalPeminjaman = (Number) session.createCriteria(BatasWaktuPeminjamanItem.class)

					.add(mahasiswa != null ? Restrictions.le("berlakuUntukSemester", mahasiswa.currentSemester())
							: Restrictions.isNull("berlakuUntukSemester"))

					.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
					.setProjection(Projections.property("jumlahMaksimalItemYangDipinjam"))
					.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();

			if (jumlahMaksimalPeminjaman == null) {
				jumlahMaksimalPeminjaman = (Number) session.createCriteria(BatasWaktuPeminjamanItem.class)

						.add(Restrictions.isNull("berlakuUntukSemester"))

						.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
						.setProjection(Projections.property("jumlahMaksimalItemYangDipinjam"))
						.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();
			}
			anggota.setMaksimal(jumlahMaksimalPeminjaman == null ? 0 : jumlahMaksimalPeminjaman.intValue());
		}

		anggota.setTbmuser(null);
		anggota.setMahasiswa(null);
		anggota.setPegawai(null);
		anggota.setDosen(null);
		return anggota;
	}

	@GET
	@Path("anggotaPerpustakaan/{kode}/{perpustakaan}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public Anggota getAnggota(@PathParam("kode") String kode, @PathParam("perpustakaan") String p) {
		Session session = HibernateUtil.currentNativeSession();
		Anggota anggota = (Anggota) session
				.createCriteria(Anggota.class).add(kode.trim().equals("") || kode.trim().equals("_")
						? Restrictions.sqlRestriction("1!=1") : Restrictions.ilike("kode", kode, MatchMode.EXACT))
				.setMaxResults(1).uniqueResult();
		if (anggota == null) {
			anggota = (Anggota) session.createCriteria(Anggota.class)
					.add(kode.trim().equals("") || kode.trim().equals("_") ? Restrictions.sqlRestriction("1!=1")
							: Restrictions.ilike("kodeIdentitas", kode, MatchMode.EXACT))
					.setMaxResults(1).uniqueResult();
		}

		Perpustakaan perpustakaan = (Perpustakaan) session.createCriteria(Perpustakaan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(p.trim().equals("") || p.trim().equals("_")

						? Restrictions.sqlRestriction("1!=1") : Restrictions.idEq(Long.parseLong(p.trim())))
				.setMaxResults(1).uniqueResult();

		if (anggota != null && anggota.getMahasiswa() != null) {
			Mahasiswa mahasiswa = anggota.getMahasiswa();
			Number jumlahMaksimalPerpanjanganPeminjaman = (Number) session
					.createCriteria(ais.database.model.library.BatasWaktuPeminjamanItem.class)

					.add(Restrictions.or(Restrictions.eq("jenisAnggota", anggota.getJenisAnggota()),
							Restrictions.isNull("jenisAnggota")))
					.add(Restrictions.or(Restrictions.eq("tipeAnggota", anggota.getTipeAnggota()),
							Restrictions.isNull("tipeAnggota")))
					.add(Restrictions.or(Restrictions.eq("fakultas",
							anggota.getMahasiswa() != null ? anggota.getMahasiswa().getJurusan().getFakultas()
									: (anggota.getDosen() != null ? anggota.getDosen().getFakultas() : null)),
							Restrictions.isNull("fakultas")))

					.add(Restrictions.or(
							Restrictions.eq("jurusan",
									anggota.getMahasiswa() != null ? anggota.getMahasiswa().getJurusan()
											: (anggota.getDosen() != null ? anggota.getDosen().getJurusan() : null)),
							Restrictions.isNull("jurusan")))

					.add(mahasiswa != null ? Restrictions.le("berlakuUntukSemester", mahasiswa.currentSemester())
							: Restrictions.isNull("berlakuUntukSemester"))
					.add(Restrictions.eq("perpustakaan", perpustakaan))
					.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
					.setProjection(Projections.property("jumlahMaksimalPerpanjanganPeminjaman"))
					.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();
			if (jumlahMaksimalPerpanjanganPeminjaman == null) {
				jumlahMaksimalPerpanjanganPeminjaman = (Number) session
						.createCriteria(ais.database.model.library.BatasWaktuPeminjamanItem.class)
						.add(Restrictions.or(Restrictions.eq("jenisAnggota", anggota.getJenisAnggota()),
								Restrictions.isNull("jenisAnggota")))
						.add(Restrictions
								.or(Restrictions
										.eq("tipeAnggota",
												anggota.getTipeAnggota()),
										Restrictions.isNull("tipeAnggota")))
						.add(Restrictions.or(Restrictions.eq("fakultas",
								anggota.getMahasiswa() != null ? anggota.getMahasiswa().getJurusan().getFakultas()
										: (anggota.getDosen() != null ? anggota.getDosen().getFakultas() : null)),
								Restrictions.isNull("fakultas")))

						.add(Restrictions.or(Restrictions.eq("jurusan",
								anggota.getMahasiswa() != null ? anggota.getMahasiswa().getJurusan()
										: (anggota.getDosen() != null ? anggota.getDosen().getJurusan() : null)),
								Restrictions.isNull("jurusan")))

						.add(Restrictions.isNull("berlakuUntukSemester"))
						.add(Restrictions.eq("perpustakaan", perpustakaan))
						.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
						.setProjection(Projections.property("jumlahMaksimalPerpanjanganPeminjaman"))
						.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();
			}

			anggota.setPerpanjang(
					jumlahMaksimalPerpanjanganPeminjaman == null ? 0 : jumlahMaksimalPerpanjanganPeminjaman.intValue());

			Number jumlahMaksimalPeminjaman = (Number) session.createCriteria(BatasWaktuPeminjamanItem.class)

					.add(mahasiswa != null ? Restrictions.le("berlakuUntukSemester", mahasiswa.currentSemester())
							: Restrictions.isNull("berlakuUntukSemester"))

					.add(Restrictions.eq("perpustakaan", perpustakaan))
					.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
					.setProjection(Projections.property("jumlahMaksimalItemYangDipinjam"))
					.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();

			if (jumlahMaksimalPeminjaman == null) {
				jumlahMaksimalPeminjaman = (Number) session.createCriteria(BatasWaktuPeminjamanItem.class)

						.add(Restrictions.isNull("berlakuUntukSemester"))

						.add(Restrictions.eq("perpustakaan", perpustakaan))
						.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
						.setProjection(Projections.property("jumlahMaksimalItemYangDipinjam"))
						.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();
			}
			anggota.setMaksimal(jumlahMaksimalPeminjaman == null ? 0 : jumlahMaksimalPeminjaman.intValue());
		}

		HibernateUtil.closeSession();
		anggota.setTbmuser(null);
		anggota.setMahasiswa(null);
		anggota.setPegawai(null);
		anggota.setDosen(null);
		return anggota;
	}

	@GET
	@Path("kunjunganAnggota/{kode}/{perpustakaan}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public KunjunganAnggota getKunjunganAnggota(@PathParam("kode") String kode, @PathParam("perpustakaan") String p) {
		Session session = HibernateUtil.currentNativeSession();
		Anggota anggota = (Anggota) session
				.createCriteria(Anggota.class).add(kode.trim().equals("") || kode.trim().equals("_")
						? Restrictions.sqlRestriction("1!=1") : Restrictions.ilike("kode", kode, MatchMode.EXACT))
				.setMaxResults(1).uniqueResult();
		if (anggota == null) {
			anggota = (Anggota) session.createCriteria(Anggota.class)
					.add(kode.trim().equals("") || kode.trim().equals("_") ? Restrictions.sqlRestriction("1!=1")
							: Restrictions.ilike("kodeIdentitas", kode, MatchMode.EXACT))
					.setMaxResults(1).uniqueResult();
		}

		Perpustakaan perpustakaan = (Perpustakaan) session
				.createCriteria(Perpustakaan.class).add(p.trim().equals("") || p.trim().equals("_")
						? Restrictions.sqlRestriction("1!=1") : Restrictions.idEq(Long.parseLong(p.trim())))
				.setMaxResults(1).uniqueResult();

		if (anggota == null || perpustakaan == null) {
			return null;
		}

		KunjunganAnggota kunjunganAnggota = (KunjunganAnggota) session.createCriteria(KunjunganAnggota.class)
				.add(Restrictions.and(
						Restrictions.and(Restrictions.eq("anggota", anggota),
								Restrictions.eq("perpustakaan", perpustakaan)),
						Restrictions.eq("tgl", ais.ui.util.WaktuUtil.getDate())))
				.setMaxResults(1).uniqueResult();

		if (kunjunganAnggota == null) {
			kunjunganAnggota = new KunjunganAnggota();
			kunjunganAnggota.setKeterangan("Berkunjung via app desktop");
			kunjunganAnggota.setAnggota(anggota);
			kunjunganAnggota.setPerpustakaan(perpustakaan);

			session.getTransaction().begin();
			session.save(kunjunganAnggota);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();

		kunjunganAnggota.getAnggota().setTbmuser(null);
		kunjunganAnggota.getAnggota().setMahasiswa(null);
		kunjunganAnggota.getAnggota().setPegawai(null);
		kunjunganAnggota.getAnggota().setDosen(null);

		return kunjunganAnggota;
	}

	@GET
	@Path("item_by_id/{id}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public Item getItemById(@PathParam("id") String id) {
		Session session = HibernateUtil.currentNativeSession();
		Item item = null;
		item = (Item) session.createCriteria(Item.class).add(Restrictions.idEq(Long.parseLong(id))).uniqueResult();

		Long jumlahDilihat = item.getJumlahDilihat();
		jumlahDilihat++;
		item.setJumlahDilihat(jumlahDilihat);
		session.getTransaction().begin();
		Common.refreshUpdate(session, (item));
		session.getTransaction().commit();

		HibernateUtil.closeSession();
		Item myItem = (Item) item.clone();
		myItem.setDibuatOleh(null);
		myItem.setParent(null);
		item = null;
		return myItem;
	}

	@GET
	@Path("item/{kode}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public Item getItem(@PathParam("kode") String kode) {
		Session session = HibernateUtil.currentNativeSession();
		ItemPunyaBarcode itemPunyaBarcode = (ItemPunyaBarcode) session.createCriteria(ItemPunyaBarcode.class)
				.add(Restrictions.ilike("barcode", kode.trim(), MatchMode.EXACT)).setMaxResults(1).uniqueResult();

		PesananAnggota pesananAnggota = null;
		if (itemPunyaBarcode == null) {
			pesananAnggota = (PesananAnggota) session.createCriteria(PesananAnggota.class)
					.add(Restrictions.sqlRestriction("date(kadaluarsa) > date('"
							+ Common.databaseDateFormat1.get().format(WaktuUtil.getDate()) + "')"))
					.add(Restrictions.eq("status", PesananAnggota.PESAN))
					.add(Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT)).setMaxResults(1).uniqueResult();
		}

		Item item = null;
		if (itemPunyaBarcode != null) {
			item = itemPunyaBarcode.getItem();
			item.setTemporaryBarcode(itemPunyaBarcode.getBarcode());
		} else if (pesananAnggota != null) {
			item = pesananAnggota.getItem();
			item.setTemporaryBarcode(pesananAnggota.getKode());
		} else {
			item = (Item) session
					.createCriteria(Item.class).add(kode.trim().equals("") || kode.trim().equals("_")
							? Restrictions.sqlRestriction("1!=1") : Restrictions.ilike("isbn", kode, MatchMode.EXACT))
					.setMaxResults(1).uniqueResult();
			if (item == null) {
				item = (Item) session.createCriteria(Item.class).add(kode.trim().equals("") || kode.trim().equals("_")
						? Restrictions.sqlRestriction("1!=1") : Restrictions.ilike("issn", kode, MatchMode.EXACT))
						.setMaxResults(1).uniqueResult();
			}
		}

		HibernateUtil.closeSession();
		Item myItem = (Item) item.clone();
		myItem.setDibuatOleh(null);
		myItem.setParent(null);
		item = null;
		return myItem;
	}

	@GET
	@Path("new_daftar_anggota/{kode}/{identitas}/{nama}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> getNewAnggotas(@PathParam("kode") String kode, @PathParam("identitas") String identitas,
			@PathParam("nama") String nama) {

		List<Anggota> anggotas = getAnggotas(kode, identitas, nama);
		List<CommonID> commonIDs = new ArrayList<CommonID>();

		for (Anggota anggota : anggotas) {
			CommonID commonID = new CommonID(anggota.getId());
			commonID.setInfo1(anggota.getKode());
			commonID.setInfo2(anggota.getNama());
			commonID.setInfo3(anggota.getJenisIdentitas());
			commonID.setInfo4(anggota.getKodeIdentitas());
			commonID.setInfo5(anggota.getTipe());
			commonID.setInfo6(anggota.getTelp());
			commonID.setInfo7(anggota.getHp());
			commonIDs.add(commonID);
		}

		return commonIDs;
	}

	@GET
	@Path("daftarKunjunganAnggota/{kode}/{identitas}/{nama}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> getKunjunganAnggotas(@PathParam("kode") String kode, @PathParam("identitas") String identitas,
			@PathParam("nama") String nama) {

		List<KunjunganAnggota> anggotas = getDataKunjunganAnggotas(kode, identitas, nama);
		List<CommonID> commonIDs = new ArrayList<CommonID>();

		for (KunjunganAnggota kunjunganAnggota : anggotas) {
			Anggota anggota = kunjunganAnggota.getAnggota();
			CommonID commonID = new CommonID(anggota.getId());
			commonID.setInfo1(anggota.getKode());
			commonID.setInfo2(anggota.getNama());
			commonID.setInfo3(anggota.getJenisIdentitas());
			commonID.setInfo4(anggota.getKodeIdentitas());
			commonID.setInfo5(anggota.getTipe());
			commonID.setInfo6(anggota.getTelp());
			commonID.setInfo7(anggota.getHp());
			commonID.setInfo8(Common.dateFormat5.get().format(kunjunganAnggota.getTanggal()));
			commonID.setInfo9(kunjunganAnggota.getId() + "");
			commonIDs.add(commonID);
		}

		return commonIDs;
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_anggota/{kode}/{identitas}/{nama}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<Anggota> getAnggotas(@PathParam("kode") String kode, @PathParam("identitas") String identitas,
			@PathParam("nama") String nama) {
		Session session = HibernateUtil.currentNativeSession();
		List<Anggota> anggotas = session.createCriteria(Anggota.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(kode.trim().equals("") || kode.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", kode, MatchMode.ANYWHERE))
				.add(identitas.trim().equals("") || identitas.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kodeIdentitas", identitas, MatchMode.ANYWHERE))
				.add(nama.trim().equals("") || nama.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama, MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT_20).list();

		HibernateUtil.closeSession();
		List<Anggota> newAnggotas = new ArrayList<Anggota>();
		for (Anggota a : anggotas) {
			a.setTbmuser(null);
			newAnggotas.add(a);
		}
		return newAnggotas;
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_kunjungan_anggota/{kode}/{identitas}/{nama}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<KunjunganAnggota> getDataKunjunganAnggotas(@PathParam("kode") String kode,
			@PathParam("identitas") String identitas, @PathParam("nama") String nama) {
		Session session = HibernateUtil.currentNativeSession();
		List<KunjunganAnggota> anggotas = session.createCriteria(KunjunganAnggota.class).addOrder(Order.desc("id"))
				.createCriteria("anggota").add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(kode.trim().equals("") || kode.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", kode, MatchMode.ANYWHERE))
				.add(identitas.trim().equals("") || identitas.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kodeIdentitas", identitas, MatchMode.ANYWHERE))
				.add(nama.trim().equals("") || nama.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama, MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT_20).list();

		HibernateUtil.closeSession();
		List<KunjunganAnggota> newAnggotas = new ArrayList<KunjunganAnggota>();
		for (KunjunganAnggota a : anggotas) {
			a.getAnggota().setTbmuser(null);
			newAnggotas.add(a);
		}
		return newAnggotas;
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("kembalikan/{userid}/{peminjaman}/{items}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID kembalikan(@PathParam("userid") String userid, @PathParam("peminjaman") String peminjaman,
			@PathParam("items") String items) {

		Session session = HibernateUtil.currentNativeSession();
		PeminjamanPengadaanItem peminjamanPengadaanItem = (PeminjamanPengadaanItem) session
				.createCriteria(PeminjamanPengadaanItem.class).add(Restrictions.idEq(Long.parseLong(peminjaman.trim())))
				.uniqueResult();
		Tbmuser tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.idEq(userid.trim()))
				.uniqueResult();
		String mykode = LibraryUtil.generateCode(KembaliPengadaanItem.class, 8, "KMB",
				peminjamanPengadaanItem.getPerpustakaan());

		KunjunganAnggota kunjunganAnggota = (KunjunganAnggota) session.createCriteria(KunjunganAnggota.class)
				.add(Restrictions.and(
						Restrictions.and(Restrictions.eq("anggota", peminjamanPengadaanItem.getAnggota()),
								Restrictions.eq("perpustakaan", peminjamanPengadaanItem.getPerpustakaan())),
						Restrictions.eq("tgl", ais.ui.util.WaktuUtil.getDate())))
				.setMaxResults(1).uniqueResult();
		if (kunjunganAnggota == null) {
			kunjunganAnggota = new KunjunganAnggota();
			kunjunganAnggota.setKeterangan("Berkunjung via app desktop");
			kunjunganAnggota.setAnggota(peminjamanPengadaanItem.getAnggota());
			kunjunganAnggota.setPerpustakaan(peminjamanPengadaanItem.getPerpustakaan());

			session.getTransaction().begin();
			session.save(kunjunganAnggota);
			session.getTransaction().commit();
		}

		KembaliPengadaanItem kembaliPengadaanItem = (KembaliPengadaanItem) session
				.createCriteria(KembaliPengadaanItem.class)
				.add(Restrictions.eq("peminjamanPengadaanItem", peminjamanPengadaanItem)).setMaxResults(1)
				.uniqueResult();
		if (kembaliPengadaanItem == null) {
			kembaliPengadaanItem = new KembaliPengadaanItem();
		}
		kembaliPengadaanItem.setDibuatOleh(tbmuser);
		kembaliPengadaanItem.setDisetujuiOleh(tbmuser);
		kembaliPengadaanItem.setKeterangan(items);
		kembaliPengadaanItem.setKode(mykode);
		kembaliPengadaanItem.setPeminjamanPengadaanItem(peminjamanPengadaanItem);
		kembaliPengadaanItem.setPerpustakaan(peminjamanPengadaanItem.getPerpustakaan());
		kembaliPengadaanItem.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
		kembaliPengadaanItem.setTanggalPembuatan(ais.ui.util.WaktuUtil.getDate());
		kembaliPengadaanItem.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
		kembaliPengadaanItem.setKunjunganAnggota(kunjunganAnggota);

		session.getTransaction().begin();
		session.saveOrUpdate(kembaliPengadaanItem);
		session.getTransaction().commit();

		session.getTransaction().begin();
		peminjamanPengadaanItem.setKembaliPengadaanItem(kembaliPengadaanItem);
		Common.refreshUpdate(session, (peminjamanPengadaanItem));
		session.getTransaction().commit();

		String[] itemids = items.split("\\|");
		Map<Long, Object[]> map = new java.util.HashMap<Long, Object[]>();
		for (String strId : itemids) {
			String[] ss = strId.split(",", 4);
			Long id = Long.parseLong(ss[0].trim());
			String check = ss[1];
			String perpanjang = ss[2];
			String action = ss[3];
			map.put(id, new Object[] { check, perpanjang, action });
		}

		List<PeminjamanPengadaanItemDetail> peminjamanPengadaanItemDetails = session
				.createCriteria(PeminjamanPengadaanItemDetail.class)
				.add(Restrictions.eq("peminjamanPengadaanItem", peminjamanPengadaanItem)).list();
		List<KembaliPengadaanItemDetail> kembaliPengadaanItemDetails = new ArrayList<KembaliPengadaanItemDetail>();
		for (PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail : peminjamanPengadaanItemDetails) {

			Object[] objects = map.get(peminjamanPengadaanItemDetail.getId());

			if (objects != null) {
				String check = (String) objects[0];
				Integer jumlahPerpanjangan = Integer.parseInt((String) objects[1]);
				String action = (String) objects[2];

				peminjamanPengadaanItemDetail.setJumlahPerpanjangan(jumlahPerpanjangan);

				KembaliPengadaanItemDetail kembaliPengadaanItemDetail = peminjamanPengadaanItemDetail
						.getKembaliPengadaanItemDetail();

				if (kembaliPengadaanItemDetail == null) {
					kembaliPengadaanItemDetail = new KembaliPengadaanItemDetail();
				}
				kembaliPengadaanItemDetail.setPeminjamanPengadaanItemDetail(peminjamanPengadaanItemDetail);
				kembaliPengadaanItemDetail.setKembaliPengadaanItem(kembaliPengadaanItem);
				peminjamanPengadaanItemDetail.setTanggalKembali(kembaliPengadaanItemDetail.getTanggal());

				DendaKeterlambatanItem dendaPerItem = LibraryUtil.hitungDendaItem(peminjamanPengadaanItemDetail);

				Double denda = dendaPerItem == null ? 0.0 : dendaPerItem.getDenda();
				denda = denda * peminjamanPengadaanItemDetail.getJumlah();
				kembaliPengadaanItemDetail.setDenda(denda);

				kembaliPengadaanItemDetail.setDikembali(check.equalsIgnoreCase("true") ? 1.0 : 0.0);
				kembaliPengadaanItemDetail.setItem(peminjamanPengadaanItemDetail.getItem());

				try {
					kembaliPengadaanItemDetail.setKeterangan(URLDecoder.decode(action, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

				session.getTransaction().begin();
				session.saveOrUpdate(kembaliPengadaanItemDetail);
				session.getTransaction().commit();

				session.getTransaction().begin();
				peminjamanPengadaanItemDetail.setKembaliPengadaanItemDetail(kembaliPengadaanItemDetail);
				session.saveOrUpdate(peminjamanPengadaanItemDetail);
				if (kembaliPengadaanItemDetail.getPeminjamanPengadaanItemDetail().getPesananAnggota() != null) {
					kembaliPengadaanItemDetail.getPeminjamanPengadaanItemDetail().getPesananAnggota()
							.setStatus(PesananAnggota.DIKEMBALIKAN);
					session.update(kembaliPengadaanItemDetail.getPeminjamanPengadaanItemDetail().getPesananAnggota());
				}
				session.getTransaction().commit();

				kembaliPengadaanItemDetails.add(kembaliPengadaanItemDetail);
			}
		}

		for (KembaliPengadaanItemDetail kembaliPengadaanItemDetail : kembaliPengadaanItemDetails) {
			DetailTransaksi detailTransaksi = (DetailTransaksi) session.createCriteria(DetailTransaksi.class)
					.add(Restrictions.eq("kembaliPengadaanItemDetail", kembaliPengadaanItemDetail)).setMaxResults(1)
					.uniqueResult();
			if (detailTransaksi == null) {
				detailTransaksi = new DetailTransaksi();
			}
			detailTransaksi.setAnggota(peminjamanPengadaanItem.getAnggota());
			detailTransaksi.setKembaliPengadaanItemDetail(kembaliPengadaanItemDetail);
			detailTransaksi.setQtyBonus(0.0);

			detailTransaksi.setItem(kembaliPengadaanItemDetail.getItem());
			detailTransaksi.setKeterangan("Transaksi Kembali");
			detailTransaksi.setKodeTransaksi(LibraryUtil.PENGEMBALIAN_MASUK);
			detailTransaksi.setPerpustakaan(kembaliPengadaanItem.getPeminjamanPengadaanItem().getPerpustakaan());
			detailTransaksi.setQty(kembaliPengadaanItemDetail.getDikembali());
			detailTransaksi.setTanggal(ais.ui.util.WaktuUtil.getDate());

			session.getTransaction().begin();
			session.saveOrUpdate(detailTransaksi);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return new CommonID(kembaliPengadaanItem.getId());
	}

	@GET
	@Path("pinjam/{userid}/{perpustakaan}/{anggota}/{items}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID pinjam(@PathParam("userid") String userid, @PathParam("perpustakaan") String perpustakaan,
			@PathParam("anggota") String anggota, @PathParam("items") String items) throws Exception {

		items = URLDecoder.decode(items, "UTF-8");
		items = items.trim().equals("_") ? "" : items;

		System.out.println("======= pinjam => userid " + userid + ", perpustakaan " + perpustakaan + ", anggota "
				+ anggota + ", items " + items);
		Session session = HibernateUtil.currentNativeSession();
		Anggota myAnggota = (Anggota) session.createCriteria(Anggota.class)
				.add(Restrictions.idEq(Long.parseLong(anggota.trim()))).uniqueResult();

		Tbmuser tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.idEq(userid.trim()))
				.uniqueResult();
		Perpustakaan myPerpustakaan = (Perpustakaan) session.createCriteria(Perpustakaan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.idEq(Long.parseLong(perpustakaan.trim()))).uniqueResult();

		if (myAnggota != null && myPerpustakaan != null) {
			if (Common.bolehKonfigurasi("anggota_tidak_boleh_meminjam_lagi_meskipun_peminjaman_sebelumnya_belum_dikembalikan")) {
				Number count = (Number) session.createCriteria(PeminjamanPengadaanItemDetail.class)
						.add(Restrictions.isNull("kembaliPengadaanItemDetail"))
						.createAlias("peminjamanPengadaanItem", "peminjamanPengadaanItem")
						.add(Restrictions.eq("peminjamanPengadaanItem.perpustakaan", myPerpustakaan))
						.add(Restrictions.eq("peminjamanPengadaanItem.anggota", myAnggota))
						.setProjection(Projections.rowCount()).uniqueResult();
				if (count.intValue() > 0) {
					HibernateUtil.closeSession();
					throw new Exception("Anggota " + myAnggota.getNama() + " masih ada peminjaman item di perpustakaan "
							+ myPerpustakaan.getNama());
				}
			}
		}

		String mykode = LibraryUtil.generateCode(PeminjamanPengadaanItem.class, 8, "PNJ", myPerpustakaan);

		KunjunganAnggota kunjunganAnggota = (KunjunganAnggota) session.createCriteria(KunjunganAnggota.class)
				.add(Restrictions.and(
						Restrictions.and(Restrictions.eq("anggota", myAnggota),
								Restrictions.eq("perpustakaan", myPerpustakaan)),
						Restrictions.eq("tgl", ais.ui.util.WaktuUtil.getDate())))
				.setMaxResults(1).uniqueResult();
		if (kunjunganAnggota == null) {
			kunjunganAnggota = new KunjunganAnggota();
			kunjunganAnggota.setKeterangan("Berkunjung via app desktop");
			kunjunganAnggota.setAnggota(myAnggota);
			kunjunganAnggota.setPerpustakaan(myPerpustakaan);

			session.getTransaction().begin();
			session.save(kunjunganAnggota);
			session.getTransaction().commit();
		}

		PeminjamanPengadaanItem peminjamanPengadaanItem = new PeminjamanPengadaanItem();
		peminjamanPengadaanItem.setAnggota(myAnggota);
		peminjamanPengadaanItem.setDibuatOleh(tbmuser);
		peminjamanPengadaanItem.setDisetujuiOleh(tbmuser);
		peminjamanPengadaanItem.setKeterangan("");
		peminjamanPengadaanItem.setPerpustakaan(myPerpustakaan);
		peminjamanPengadaanItem.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
		peminjamanPengadaanItem.setTanggalPembuatan(ais.ui.util.WaktuUtil.getDate());
		peminjamanPengadaanItem.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
		peminjamanPengadaanItem.setKunjunganAnggota(kunjunganAnggota);

		peminjamanPengadaanItem.setKode(mykode);

		// peminjamanPengadaanItem.hitungBatasWaktupengembalian(myPerpustakaan);

		session.getTransaction().begin();
		session.save(peminjamanPengadaanItem);
		session.getTransaction().commit();

		String[] itemids = items.split("\\|");
		Map<Long, String> map = new java.util.HashMap<Long, String>();
		for (String strId : itemids) {
			String[] ss = strId.split(",", 2);
			Long id = Long.parseLong(ss[0].trim());
			String barcode = ss[1];
			map.put(id, barcode);
		}

		System.out.println("map = " + map);

		List<PeminjamanPengadaanItemDetail> peminjamanPengadaanItemDetails = new ArrayList<PeminjamanPengadaanItemDetail>();
		for (Long itemId : map.keySet()) {
			String barcode = map.get(itemId);

			System.out.println("barcode" + barcode + ", itemId = " + itemId);

			Item item = (Item) session.createCriteria(Item.class).add(Restrictions.idEq(itemId)).uniqueResult();
			ItemPunyaBarcode itemPunyaBarcode = barcode == null || barcode.trim().equals("")
					|| barcode.trim().equals("null")
							? null
							: (ItemPunyaBarcode) session.createCriteria(ItemPunyaBarcode.class)
									.add(Restrictions.ilike("barcode", barcode.trim(), MatchMode.EXACT))
									.setMaxResults(1).uniqueResult();

			PesananAnggota pesananAnggota = null;
			if (itemPunyaBarcode == null) {
				pesananAnggota = barcode == null || barcode.trim().equals("") || barcode.trim().equals("null") ? null
						: (PesananAnggota) session.createCriteria(PesananAnggota.class)
								.add(Restrictions.sqlRestriction("date(kadaluarsa) > date('"
										+ Common.databaseDateFormat1.get().format(WaktuUtil.getDate()) + "')"))
								.add(Restrictions.eq("status", PesananAnggota.PESAN))
								.add(Restrictions.ilike("kode", barcode, MatchMode.EXACT)).setMaxResults(1)
								.uniqueResult();
			}

			PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail = new PeminjamanPengadaanItemDetail();
			peminjamanPengadaanItemDetail.setItem(item);
			peminjamanPengadaanItemDetail.setJumlah(1.0);
			peminjamanPengadaanItemDetail.setKeterangan("");
			peminjamanPengadaanItemDetail.setPeminjamanPengadaanItem(peminjamanPengadaanItem);
			peminjamanPengadaanItemDetail.setItemPunyaBarcode(itemPunyaBarcode);
			peminjamanPengadaanItemDetail.setPesananAnggota(pesananAnggota);

			session.getTransaction().begin();
			session.save(peminjamanPengadaanItemDetail);
			if (pesananAnggota != null) {
				pesananAnggota.setStatus(PesananAnggota.PINJAM);
				session.update(pesananAnggota);
			}
			session.getTransaction().commit();
			peminjamanPengadaanItemDetails.add(peminjamanPengadaanItemDetail);

		}

		for (PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail : peminjamanPengadaanItemDetails) {
			DetailTransaksi detailTransaksi = new DetailTransaksi();
			detailTransaksi.setAnggota(peminjamanPengadaanItem.getAnggota());
			detailTransaksi.setPeminjamanPengadaanItemDetail(peminjamanPengadaanItemDetail);
			detailTransaksi.setQtyBonus(0.0);

			detailTransaksi.setItem(peminjamanPengadaanItemDetail.getItem());
			detailTransaksi.setKeterangan("Transaksi Peminjaman");
			detailTransaksi.setKodeTransaksi(LibraryUtil.PINJAM_KELUAR);
			detailTransaksi.setPerpustakaan(peminjamanPengadaanItem.getPerpustakaan());
			detailTransaksi.setQty(peminjamanPengadaanItemDetail.getJumlah());
			detailTransaksi.setTanggal(ais.ui.util.WaktuUtil.getDate());
			detailTransaksi.setItemPunyaBarcode(peminjamanPengadaanItemDetail.getItemPunyaBarcode());

			session.getTransaction().begin();
			session.save(detailTransaksi);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return new CommonID(peminjamanPengadaanItem.getId());
	}

	@GET
	@Path("cari_buku_stok/{perpustakaan}/{judul}/{isbn}/{pengarang}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<StokItem> cariBukuStok(@PathParam("perpustakaan") String perpustakaan, @PathParam("judul") String judul,
			@PathParam("isbn") String isbn, @PathParam("pengarang") String pengarang) throws Exception {
		return cariBukuStok(perpustakaan, judul, isbn, pengarang, "desc");
	}

	@GET
	@Path("cari_buku_stok/{perpustakaan}/{judul}/{isbn}/{pengarang}/{order}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<StokItem> cariBukuStok(@PathParam("perpustakaan") String perpustakaan, @PathParam("judul") String judul,
			@PathParam("isbn") String isbn, @PathParam("pengarang") String pengarang, @PathParam("order") String order)
			throws Exception {
		return cariBukuStok(perpustakaan, judul, isbn, pengarang, order, "");
	}

	@GET
	@Path("cari_buku_stok/{perpustakaan}/{judul}/{isbn}/{pengarang}/{order}/{namaPerpustakaan}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<StokItem> cariBukuStok(@PathParam("perpustakaan") String perpustakaan, @PathParam("judul") String judul,
			@PathParam("isbn") String isbn, @PathParam("pengarang") String pengarang, @PathParam("order") String order,
			@PathParam("namaPerpustakaan") String namaPerpustakaan) throws Exception {
		return cariBukuStok(perpustakaan, judul, isbn, pengarang, order, namaPerpustakaan, "");
	}

	@GET
	@Path("cari_buku_stok/{perpustakaan}/{judul}/{isbn}/{pengarang}/{order}/{namaPerpustakaan}/{kategori}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<StokItem> cariBukuStok(@PathParam("perpustakaan") String perpustakaan, @PathParam("judul") String judul,
			@PathParam("isbn") String isbn, @PathParam("pengarang") String pengarang, @PathParam("order") String order,
			@PathParam("namaPerpustakaan") String namaPerpustakaan, @PathParam("kategori") String kategori)
			throws Exception {
		return cariBukuStok(perpustakaan, judul, isbn, pengarang, order, namaPerpustakaan, kategori, "");
	}

	@GET
	@Path("cari_buku_stok/{perpustakaan}/{judul}/{isbn}/{pengarang}/{order}/{namaPerpustakaan}/{kategori}/{jenis}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<StokItem> cariBukuStok(@PathParam("perpustakaan") String perpustakaan, @PathParam("judul") String judul,
			@PathParam("isbn") String isbn, @PathParam("pengarang") String pengarang, @PathParam("order") String order,
			@PathParam("namaPerpustakaan") String namaPerpustakaan, @PathParam("kategori") String kategori,
			@PathParam("jenis") String jenis) throws Exception {
		return cariBukuStok(perpustakaan, judul, isbn, pengarang, order, namaPerpustakaan, kategori, jenis, "0", "10");
	}

	@GET
	@Path("cari_buku_stok/{perpustakaan}/{judul}/{isbn}/{pengarang}/{order}/{namaPerpustakaan}/{kategori}/{jenis}/{start}/{banyak}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<StokItem> cariBukuStok(@PathParam("perpustakaan") String perpustakaan, @PathParam("judul") String judul,
			@PathParam("isbn") String isbn, @PathParam("pengarang") String pengarang, @PathParam("order") String order,
			@PathParam("namaPerpustakaan") String namaPerpustakaan, @PathParam("kategori") String kategori,
			@PathParam("jenis") String jenis, @PathParam("start") String start, @PathParam("banyak") String banyak)
			throws Exception {
		return cariBukuStok(perpustakaan, judul, isbn, pengarang, order, namaPerpustakaan, kategori, jenis, "", start,
				banyak);
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("cari_buku_stok/{perpustakaan}/{judul}/{isbn}/{pengarang}/{order}/{namaPerpustakaan}/{kategori}/{jenis}/{ddc}/{start}/{banyak}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<StokItem> cariBukuStok(@PathParam("perpustakaan") String perpustakaan, @PathParam("judul") String judul,
			@PathParam("isbn") String isbn, @PathParam("pengarang") String pengarang, @PathParam("order") String order,
			@PathParam("namaPerpustakaan") String namaPerpustakaan, @PathParam("kategori") String kategori,
			@PathParam("jenis") String jenis, @PathParam("ddc") String ddc, @PathParam("start") String start,
			@PathParam("banyak") String banyak) throws Exception {
		judul = URLDecoder.decode(judul, "UTF-8");
		isbn = URLDecoder.decode(isbn, "UTF-8");
		pengarang = URLDecoder.decode(pengarang, "UTF-8");
		namaPerpustakaan = URLDecoder.decode(namaPerpustakaan, "UTF-8");
		kategori = URLDecoder.decode(kategori, "UTF-8");
		jenis = URLDecoder.decode(jenis, "UTF-8");
		ddc = URLDecoder.decode(ddc, "UTF-8");

		ddc = ddc.trim().equals("_") ? "" : ddc;
		jenis = jenis.trim().equals("_") ? "" : jenis;
		judul = judul.trim().equals("_") ? "" : judul;
		isbn = isbn.trim().equals("_") ? "" : isbn;
		pengarang = pengarang.trim().equals("_") ? "" : pengarang;
		kategori = kategori.trim().equals("_") ? "" : kategori;
		perpustakaan = perpustakaan.trim().equals("_") || perpustakaan.trim().equals("-1") ? "" : perpustakaan;
		namaPerpustakaan = namaPerpustakaan.trim().equals("_") || namaPerpustakaan.trim().equals("-1") ? ""
				: namaPerpustakaan;

		String sql = "select a.item, max(c.nama) as nama_item, " + "max(a.tanggal) as tanggal_terakhir_pengadaan, "
				+ "sum((a.qty+a.qtybonus)*b.jenis) as stok, max(c.pengarangs) as pengarangs, max(d.nama) as perpustakaan, "
				+ "max(c.isbn) as isbn, "
				+ "max(case when c.abstrak is not null or trim(c.abstrak) != '' then c.abstrak when c.abstract_en is not null or trim(c.abstract_en) != '' then c.abstract_en else c.catatan end) as catatan, max(c.issn) as issn, "
				+ "max(j.nama) as jenis, " + "max(i.nama) as tipe, "
				+ "(select count(*) from library.item_komentar as aa where aa.item = a.item) as komentar, "
				+ "a.perpustakaan perpustakaan_id, " + "max(c.image_url) as image_url "
				+ "from library.detail_transaksi a "
				+ "inner join library.kode_transaksi b on (a.kode_transaksi = b.id) "
				+ "inner join library.perpustakaan d on (a.perpustakaan = d.id) "
				+ "left join library.item c on (a.item = c.id) "
				+ "left join library.jenis_item j on (c.jenis_item = j.id) "
				+ "left join library.tipe_item i on (c.tipe_item = i.id) where a.perpustakaan = "
				+ (perpustakaan.equals("") ? "a.perpustakaan" : perpustakaan) + " and c.jenis_item = "
				+ (jenis.equals("") ? "c.jenis_item" : jenis) + (ddc.equals("") ? ""
						: (" and a.item in (select aa.item from library.data_ddc_item_detail aa inner join library.data_ddc_item bb on (aa.data_ddc_item = bb.id) where bb.ddc_item = "
								+ ddc + ")"))
				+ " " + (namaPerpustakaan.trim().equals("") ? ""
						: " and d.nama ilike '%" + namaPerpustakaan.trim() + "%'")

				+ (kategori.trim().equals("") ? "" : " and c.kategories ilike '%" + kategori.trim() + "%'")

				+ (isbn.trim().equals("") ? "" : " and c.isbn ilike '%" + isbn.trim() + "%'")
				+ (judul.trim().equals("") ? "" : " and c.nama ilike '%" + judul.trim() + "%'")
				+ (pengarang.trim().equals("") ? ""
						: " and c.pengarangs ilike '%" + pengarang.trim() + "%'")
				+ " group by a.item, a.perpustakaan order by stok " + order + " limit " + banyak + "  offset " + start;
		System.out.println(sql);

		List<Object[]> myItem = new ArrayList<Object[]>();

		try {
			Session session = HibernateUtil.currentNativeSession();
			myItem = session.createSQLQuery(sql).list();

			HibernateUtil.closeSession();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			HibernateUtil.rollbackTransaction();
		}

		List<StokItem> stokItems = new ArrayList<StokItem>();
		for (Object[] objects : myItem) {
			StokItem stokItem = new StokItem();
			stokItem.id = objects[0] == null ? null : Long.parseLong(objects[0].toString());
			stokItem.nama = objects[1] == null ? null : objects[1].toString();
			stokItem.tanggal = (Date) objects[2];
			stokItem.stok = objects[3] == null ? null : Double.parseDouble(objects[3].toString());
			stokItem.pengarang = objects[4] == null ? null : objects[4].toString();
			stokItem.perpustakaan = objects[5] == null ? null : objects[5].toString();
			stokItem.isbn = objects[6] == null ? null : objects[6].toString();
			stokItem.catatan = objects[7] == null ? null : objects[7].toString();
			stokItem.issn = objects[8] == null ? null : objects[8].toString();
			stokItem.jenis = objects[9] == null ? null : objects[9].toString();
			stokItem.tipe = objects[10] == null ? null : objects[10].toString();
			stokItem.komentar = objects[11] == null ? null : Integer.parseInt(objects[11].toString());
			stokItem.perpustakaanId = objects[12] == null ? null : Long.parseLong(objects[12].toString());

			String imageUrl = objects[13] == null ? null : objects[13].toString();
			if (imageUrl == null || imageUrl.trim().equals("")) {
				imageUrl = CommonMedia.getMediaItem(stokItem.id, 100, 120, false);
			}

			stokItem.gambar = imageUrl;

			stokItems.add(stokItem);
		}
		return stokItems;
	}

	@GET
	@Path("cari_buku_populer_per_perpustakaan/{perpustakaan}/{judul}/{isbn}/{pengarang}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<StokItem> cariBukuPopulerPerPerpustakaan(@PathParam("perpustakaan") String perpustakaan,
			@PathParam("judul") String judul, @PathParam("isbn") String isbn, @PathParam("pengarang") String pengarang)
			throws Exception {
		return cariBukuPopulerPerPerpustakaan(perpustakaan, judul, isbn, pengarang, "");
	}

	@GET
	@Path("cari_buku_populer_per_perpustakaan/{perpustakaan}/{judul}/{isbn}/{pengarang}/{namaPerpustakaan}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<StokItem> cariBukuPopulerPerPerpustakaan(@PathParam("perpustakaan") String perpustakaan,
			@PathParam("judul") String judul, @PathParam("isbn") String isbn, @PathParam("pengarang") String pengarang,
			@PathParam("namaPerpustakaan") String namaPerpustakaan) throws Exception {
		return cariBukuPopulerPerPerpustakaan(perpustakaan, judul, isbn, pengarang, namaPerpustakaan, "");
	}

	@GET
	@Path("cari_buku_populer_per_perpustakaan/{perpustakaan}/{judul}/{isbn}/{pengarang}/{namaPerpustakaan}/{kategori}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<StokItem> cariBukuPopulerPerPerpustakaan(@PathParam("perpustakaan") String perpustakaan,
			@PathParam("judul") String judul, @PathParam("isbn") String isbn, @PathParam("pengarang") String pengarang,
			@PathParam("namaPerpustakaan") String namaPerpustakaan, @PathParam("kategori") String kategori)
			throws Exception {
		return cariBukuPopulerPerPerpustakaan(perpustakaan, judul, isbn, pengarang, namaPerpustakaan, kategori, "");
	}

	@GET
	@Path("cari_buku_populer_per_perpustakaan/{perpustakaan}/{judul}/{isbn}/{pengarang}/{namaPerpustakaan}/{kategori}/{jenis}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<StokItem> cariBukuPopulerPerPerpustakaan(@PathParam("perpustakaan") String perpustakaan,
			@PathParam("judul") String judul, @PathParam("isbn") String isbn, @PathParam("pengarang") String pengarang,
			@PathParam("namaPerpustakaan") String namaPerpustakaan, @PathParam("kategori") String kategori,
			@PathParam("jenis") String jenis) throws Exception {
		return cariBukuPopulerPerPerpustakaan(perpustakaan, judul, isbn, pengarang, namaPerpustakaan, kategori, jenis,
				"0", "10");
	}

	@GET
	@Path("cari_buku_populer_per_perpustakaan/{perpustakaan}/{judul}/{isbn}/{pengarang}/{namaPerpustakaan}/{kategori}/{jenis}/{start}/{banyak}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<StokItem> cariBukuPopulerPerPerpustakaan(@PathParam("perpustakaan") String perpustakaan,
			@PathParam("judul") String judul, @PathParam("isbn") String isbn, @PathParam("pengarang") String pengarang,
			@PathParam("namaPerpustakaan") String namaPerpustakaan, @PathParam("kategori") String kategori,
			@PathParam("jenis") String jenis, @PathParam("start") String start, @PathParam("banyak") String banyak)
			throws Exception {
		return cariBukuPopulerPerPerpustakaan(perpustakaan, judul, isbn, pengarang, namaPerpustakaan, kategori, jenis,
				"", start, banyak);
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("cari_buku_populer_per_perpustakaan/{perpustakaan}/{judul}/{isbn}/{pengarang}/{namaPerpustakaan}/{kategori}/{jenis}/{ddc}/{start}/{banyak}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<StokItem> cariBukuPopulerPerPerpustakaan(@PathParam("perpustakaan") String perpustakaan,
			@PathParam("judul") String judul, @PathParam("isbn") String isbn, @PathParam("pengarang") String pengarang,
			@PathParam("namaPerpustakaan") String namaPerpustakaan, @PathParam("kategori") String kategori,
			@PathParam("jenis") String jenis, @PathParam("ddc") String ddc, @PathParam("start") String start,
			@PathParam("banyak") String banyak) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		judul = URLDecoder.decode(judul, "UTF-8");
		isbn = URLDecoder.decode(isbn, "UTF-8");
		pengarang = URLDecoder.decode(pengarang, "UTF-8");
		namaPerpustakaan = URLDecoder.decode(namaPerpustakaan, "UTF-8");
		kategori = URLDecoder.decode(kategori, "UTF-8");
		jenis = URLDecoder.decode(jenis, "UTF-8");
		ddc = URLDecoder.decode(ddc, "UTF-8");

		ddc = ddc.trim().equals("_") ? "" : ddc;
		jenis = jenis.trim().equals("_") ? "" : jenis;
		judul = judul.trim().equals("_") ? "" : judul;
		isbn = isbn.trim().equals("_") ? "" : isbn;
		pengarang = pengarang.trim().equals("_") ? "" : pengarang;
		namaPerpustakaan = namaPerpustakaan.trim().equals("_") ? "" : namaPerpustakaan;
		kategori = kategori.trim().equals("_") ? "" : kategori;
		perpustakaan = perpustakaan.trim().equals("_") || perpustakaan.trim().equals("-1") ? "" : perpustakaan;

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);

		String sql = "select " + "a.item, " + "max(c.nama) as nama_item, " + "max(a.tanggal) as tanggal,  "
				+ "sum(case when a.peminjaman_pengadaan_item_detail is not null then qty else 0 end) as jumlah, "
				+ "max(c.pengarangs) as pengarangs,  " + "max(d.nama) as perpustakaan,  " + "max(c.isbn) as isbn,  "
				+ "max(case when c.abstrak is not null or trim(c.abstrak) != '' then c.abstrak when c.abstract_en is not null or trim(c.abstract_en) != '' then c.abstract_en else c.catatan end) as catatan,  "
				+ "max(c.issn) as issn,"
				+ "sum((a.qty+a.qtybonus)*b.jenis) as stok, max(j.nama) as jenis, max(i.nama) as tipe, (select count(*) from library.item_komentar as aa where aa.item = a.item) as komentar,"
				+ "max(c.image_url) as image_url " +

				"from library.detail_transaksi a " + "inner join library.kode_transaksi b on (a.kode_transaksi = b.id) "
				+ "inner join library.perpustakaan d on (a.perpustakaan = d.id) "
				+ "left join library.item c on (a.item = c.id) "
				+ "left join library.jenis_item j on (c.jenis_item = j.id) "
				+ "left join library.tipe_item i on (c.tipe_item = i.id) where date(a.tanggal) between date('"
				+ Common.databaseDateFormat.get().format(calendar.getTime()) + "') and date('"
				+ Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate()) + "') "
				+ (kategori.trim().equals("") ? ""
						: " and c.kategories ilike '%" + kategori.trim() + "%'")
				+ "and a.perpustakaan = " + (perpustakaan.equals("") ? "a.perpustakaan" : perpustakaan)
				+ " and c.jenis_item = " + (jenis.equals("") ? "c.jenis_item" : jenis) + (ddc.equals("") ? ""
						: (" and a.item in (select aa.item from library.data_ddc_item_detail aa inner join library.data_ddc_item bb on (aa.data_ddc_item = bb.id) where bb.ddc_item = "
								+ ddc + ")"))
				+ " " + (namaPerpustakaan.trim().equals("") ? ""
						: " and d.nama ilike '%" + namaPerpustakaan.trim() + "%'")
				+ (isbn.trim().equals("") ? ""
						: " and c.isbn ilike '%" + isbn.trim() + "%'")
				+ (judul.trim().equals("") ? ""
						: " and c.nama ilike '%" + judul.trim() + "%'")
				+ (pengarang.trim().equals("") ? ""
						: " and c.pengarangs ilike '%" + pengarang.trim() + "%'")
				+ " group by a.perpustakaan,a.item order by jumlah desc limit " + banyak + "  offset " + start;
		System.out.println(sql);
		List<Object[]> myItem = session.createSQLQuery(sql).list();

		HibernateUtil.closeSession();

		List<StokItem> stokItems = new ArrayList<StokItem>();
		for (Object[] objects : myItem) {
			StokItem stokItem = new StokItem();
			stokItem.id = objects[0] == null ? null : Long.parseLong(objects[0].toString());
			stokItem.nama = objects[1] == null ? null : objects[1].toString();
			stokItem.tanggal = (Date) objects[2];
			stokItem.jumlah = objects[3] == null ? null : Double.parseDouble(objects[3].toString());
			stokItem.pengarang = objects[4] == null ? null : objects[4].toString();
			stokItem.perpustakaan = objects[5] == null ? null : objects[5].toString();
			stokItem.isbn = objects[6] == null ? null : objects[6].toString();
			stokItem.catatan = objects[7] == null ? null : objects[7].toString();
			stokItem.issn = objects[8] == null ? null : objects[8].toString();
			stokItem.stok = objects[9] == null ? null : Double.parseDouble(objects[9].toString());
			stokItem.jenis = objects[10] == null ? null : objects[10].toString();
			stokItem.tipe = objects[11] == null ? null : objects[11].toString();
			stokItem.komentar = objects[12] == null ? null : Integer.parseInt(objects[12].toString());
			String imageUrl = objects[13] == null ? null : objects[13].toString();
			if (imageUrl == null || imageUrl.trim().equals("")) {
				imageUrl = CommonMedia.getMediaItem(stokItem.id, 100, 120, false);
			}

			stokItem.gambar = imageUrl;

			stokItems.add(stokItem);
		}
		return stokItems;
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("get_lampiran/{item}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> getLampiran(@PathParam("item") String item) throws Exception {
		List<CommonID> commonIDs = new ArrayList<CommonID>();
		Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

		List<FotoItem> fotoItems = item == null ? new ArrayList<FotoItem>()
				: streamingSession.createCriteria(FotoItem.class)
						.add(Restrictions.or(Restrictions.isNull("ditampilkan"), Restrictions.eq("ditampilkan", true)))
						.add(Restrictions.eq("item", Long.parseLong(item))).addOrder(Order.desc("id")).list();

		for (FotoItem fotoItem : fotoItems) {
			CommonID commonID = new CommonID();
			commonID.setId(fotoItem.getId());
			commonID.setInfo1(fotoItem.getNama());
			commonID.setInfo2(fotoItem.getKeterangan());
			commonIDs.add(commonID);
		}
		fotoItems = null;
		StreamingHibernateUtil.getInstance().closeSession();
		return commonIDs;
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_perpustakaan/{nama}/{satuan_kerja}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarPerpustakaan(@PathParam("nama") String nama,
			@PathParam("satuan_kerja") String satuan_kerja) {
		List<CommonID> commonIDs = new ArrayList<CommonID>();
		Session session = HibernateUtil.currentNativeSession();

		List<Perpustakaan> perpustakaans = session.createCriteria(Perpustakaan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.createAlias("satuanKerja", "satuanKerja")
				.add(nama.trim().equals("") || nama.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama, MatchMode.ANYWHERE))
				.add(satuan_kerja.trim().equals("") || satuan_kerja.trim().equals("_")
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("satuanKerja.nama", satuan_kerja, MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT_20).list();

		for (Perpustakaan perpustakaan : perpustakaans) {
			CommonID commonID = new CommonID();
			commonID.setId(perpustakaan.getId());
			commonID.setInfo1(perpustakaan.getNama());
			commonID.setInfo2(perpustakaan.getSatuanKerja() == null ? "" : perpustakaan.getSatuanKerja().toString());
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();
		return commonIDs;
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_folder_item/{parent}/{satuan_kerja}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarFolderItem(@PathParam("parent") String parent,
			@PathParam("satuan_kerja") String satuan_kerja) {
		List<CommonID> commonIDs = new ArrayList<CommonID>();
		Session session = HibernateUtil.currentNativeSession();

		List<Item> items = session.createCriteria(Item.class).add(Restrictions.isNotNull("defaultSatuanKerja"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("tipeItem", LibraryUtil.KARYA_ILMIAH)).add(Restrictions.eq("folder", true))
				.addOrder(Order.asc("nama"))
				.add(parent.trim().equals("") || parent.trim().equals("_") || parent.trim().equals("-1")
						? Restrictions.isNull("parent") : Restrictions.eq("parent.id", Long.parseLong(parent)))
				.add(satuan_kerja.trim().equals("") || satuan_kerja.trim().equals("_")
						|| satuan_kerja.trim().equals("-1") ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("defaultSatuanKerja.id", Long.parseLong(satuan_kerja.trim())))
				.setMaxResults(Common.MAX_RESULT_20).list();

		for (Item item : items) {
			CommonID commonID = new CommonID();
			commonID.setId(item.getId());
			commonID.setInfo1(item.getNama());
			commonID.setInfo2(item.getDefaultSatuanKerja() == null ? "" : item.getDefaultSatuanKerja().toString());
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();
		return commonIDs;
	}

	@GET
	@Path("daftar_item/{parent}/{nama}/{pengarang}/{keyword}/{abstrack}/{institusi}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarItemJurnal(@PathParam("parent") String parent, @PathParam("nama") String nama,
			@PathParam("pengarang") String pengarang, @PathParam("keyword") String keyword,
			@PathParam("abstrack") String abstrack, @PathParam("institusi") String institusi) throws Exception {
		return daftarItemJurnal(parent, nama, pengarang, keyword, abstrack, institusi, "");
	}

	@GET
	@Path("daftar_item/{parent}/{nama}/{pengarang}/{keyword}/{abstrack}/{institusi}/{order}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarItemJurnal(@PathParam("parent") String parent, @PathParam("nama") String nama,
			@PathParam("pengarang") String pengarang, @PathParam("keyword") String keyword,
			@PathParam("abstrack") String abstrack, @PathParam("institusi") String institusi,
			@PathParam("order") String order) throws Exception {
		return daftarItemJurnal(parent, nama, pengarang, keyword, abstrack, institusi, order, "");
	}

	@GET
	@Path("daftar_item/{parent}/{nama}/{pengarang}/{keyword}/{abstrack}/{institusi}/{order}/{order1}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarItemJurnal(@PathParam("parent") String parent, @PathParam("nama") String nama,
			@PathParam("pengarang") String pengarang, @PathParam("keyword") String keyword,
			@PathParam("abstrack") String abstrack, @PathParam("institusi") String institusi,
			@PathParam("order") String order, @PathParam("order1") String order1) throws Exception {
		return daftarItemJurnal(parent, nama, pengarang, keyword, abstrack, institusi, order, order1, "0", "10");
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_item/{parent}/{nama}/{pengarang}/{keyword}/{abstrack}/{institusi}/{order}/{order1}/{start}/{banyak}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarItemJurnal(@PathParam("parent") String parent, @PathParam("nama") String nama,
			@PathParam("pengarang") String pengarang, @PathParam("keyword") String keyword,
			@PathParam("abstrack") String abstrack, @PathParam("institusi") String institusi,
			@PathParam("order") String order, @PathParam("order1") String order1, @PathParam("start") String start,
			@PathParam("banyak") String banyak) throws Exception {
		List<CommonID> commonIDs = new ArrayList<CommonID>();
		Session session = HibernateUtil.currentNativeSession();
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

		List<Item> items = session.createCriteria(Item.class).createAlias("penerbit", "penerbit", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.createAlias("penerbit2", "penerbit2", Criteria.LEFT_JOIN)
				.createAlias("penerbit3", "penerbit3", Criteria.LEFT_JOIN)
				.createAlias("penerbit4", "penerbit4", Criteria.LEFT_JOIN)
				.createAlias("penerbit5", "penerbit5", Criteria.LEFT_JOIN)

				.add(Restrictions.eq("tipeItem", LibraryUtil.KARYA_ILMIAH))
				.add(Restrictions.eq("statusTerbitItem", LibraryUtil.PUBLISH)).add(Restrictions.eq("folder", false))
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

			Integer count = ((Number) session.createCriteria(ItemKomentar.class).setProjection(Projections.rowCount())
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

			String imageUrl = item.getImageUrl();
			if (imageUrl == null || imageUrl.trim().equals("")) {
				imageUrl = CommonMedia.getMediaItem(item.getId(), 100, 120, false);
			}

			commonID.setInfo18(imageUrl);

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

	@SuppressWarnings("unchecked")
	private List<Item> loadDataItem(String perpustakaan, String parent, String nama, String isbn, String pengarang,
			String keyword, String abstrack, String institusi, String kategori, String tahun, String order,
			String order1, String start, String banyak) {
		Session session = HibernateUtil.currentNativeSession();
		Perpustakaan myPerpustakaan = null;

		if (!perpustakaan.trim().equals("") && !perpustakaan.trim().equals("_") && !perpustakaan.trim().equals("-1")) {
			myPerpustakaan = (Perpustakaan) session.createCriteria(Perpustakaan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(perpustakaan.trim()))).uniqueResult();
		}

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

		Criteria criteria = session.createCriteria(Item.class);

		if (myPerpustakaan != null) {

			criteria = session.createCriteria(DetailTransaksi.class)
					.add(Restrictions.eq("perpustakaan", myPerpustakaan))
					.setProjection(Projections.groupProperty("item")).createCriteria("item");

		} else {
			criteria.addOrder(Order.desc(order.equals("_") || order.equals("") ? "id" : order))
					.addOrder(Order.desc(order1.equals("_") || order1.equals("") ? "id" : order1));
		}

		List<Item> items = criteria.createAlias("penerbit", "penerbit", Criteria.LEFT_JOIN)
				.createAlias("penerbit2", "penerbit2", Criteria.LEFT_JOIN)
				.createAlias("penerbit3", "penerbit3", Criteria.LEFT_JOIN)
				.createAlias("penerbit4", "penerbit4", Criteria.LEFT_JOIN)
				.createAlias("penerbit5", "penerbit5", Criteria.LEFT_JOIN)
				
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(Common.isNumber(tahun) ? Restrictions.eq("tahun", (int) Double.parseDouble(tahun.trim()))
						: Restrictions.sqlRestriction("1=1"))

				.add(kategori.trim().equals("") || kategori.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kategories", kategori, MatchMode.ANYWHERE))

				.add(nama.trim().equals("") || nama.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("nama", nama, MatchMode.ANYWHERE),
								Restrictions.ilike("tema", nama, MatchMode.ANYWHERE)))

				.add(isbn.trim().equals("") || isbn.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.or(Restrictions.ilike("isbn", isbn, MatchMode.ANYWHERE),
										Restrictions.ilike("isbn10", isbn, MatchMode.ANYWHERE)),
								Restrictions.ilike("issn", isbn, MatchMode.ANYWHERE)))

				.add(keyword.trim().equals("") || keyword.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("kewords", keyword, MatchMode.ANYWHERE),
								Restrictions.ilike("kewordsEn", keyword, MatchMode.ANYWHERE)))

				.add(abstrack.trim().equals("") || abstrack.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.or(Restrictions.ilike("abstrak", abstrack, MatchMode.ANYWHERE),
										Restrictions.ilike("abstrakEn", abstrack, MatchMode.ANYWHERE)),
								Restrictions.ilike("catatan", abstrack, MatchMode.ANYWHERE)))

				.add(pengarang.trim().equals("") || pengarang.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("pengarangs", pengarang, MatchMode.ANYWHERE))

				.add(institusi.trim().equals("") || institusi.trim().equals("_") ? Restrictions.sqlRestriction("1=1")
						: criterion)

				.add(parents == null || parents.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("parent.id", parents))
				.setFirstResult(Integer.parseInt(start)).setMaxResults(Integer.parseInt(banyak.trim())).list();

		HibernateUtil.closeSession();

		return items;
	}

	public List<Item> daftarItemWithStart(@PathParam("perpustakaan") String perpustakaan,
			@PathParam("parent") String parent, @PathParam("nama") String nama, @PathParam("isbn") String isbn,
			@PathParam("pengarang") String pengarang, @PathParam("keyword") String keyword,
			@PathParam("abstrack") String abstrack, @PathParam("institusi") String institusi,
			@PathParam("kategori") String kategori, @PathParam("tahun") String tahun, @PathParam("order") String order,
			@PathParam("order1") String order1, @PathParam("start") String start, @PathParam("banyak") String banyak,
			@PathParam("sync") String sync, Boolean manual) throws Exception {
		nama = URLDecoder.decode(nama, "UTF-8");
		pengarang = URLDecoder.decode(pengarang, "UTF-8");
		keyword = URLDecoder.decode(keyword, "UTF-8");
		abstrack = URLDecoder.decode(abstrack, "UTF-8");
		institusi = URLDecoder.decode(institusi, "UTF-8");
		kategori = URLDecoder.decode(kategori, "UTF-8");
		isbn = URLDecoder.decode(isbn, "UTF-8");
		perpustakaan = URLDecoder.decode(perpustakaan, "UTF-8");
		sync = URLDecoder.decode(sync, "UTF-8");
		tahun = URLDecoder.decode(tahun, "UTF-8");

		System.out.println("perpustakaan = " + perpustakaan);
		System.out.println("isbn = " + isbn);
		System.out.println("nama = " + nama);
		System.out.println("pengarang = " + pengarang);
		System.out.println("keyword = " + keyword);
		System.out.println("abstrack = " + abstrack);
		System.out.println("institusi = " + institusi);
		System.out.println("kategori = " + kategori);
		System.out.println("sync = " + sync);
		System.out.println("tahun = " + tahun);
		System.out.println("start = " + start);
		System.out.println("banyak = " + banyak);

		List<Item> items = loadDataItem(perpustakaan, parent, nama, isbn, pengarang, keyword, abstrack, institusi,
				kategori, tahun, order, order1, start, banyak);

		if (!sync.trim().equals("true") && (tahun.trim().equals("_") || tahun.trim().isEmpty())) {

			if (perpustakaan.trim().equals("_") || perpustakaan.trim().isEmpty()) {
				if ((Integer.parseInt(banyak.trim()) > items.size() && (isbn.trim().equals("_"))
						&& ((!pengarang.trim().equals("_")) || (!nama.trim().equals("_"))
								|| (!institusi.trim().equals("_"))))

						||

						(items.isEmpty() && (!isbn.trim().equals("_")))

				) {

					try {

						String query = "";

						if (!isbn.trim().equals("") && !isbn.trim().equals("_")) {
							String prefix = null;
							prefix = "isbn:";
							isbn = org.apache.commons.lang3.StringUtils.replace(isbn, "-", "");
							query += query.equals("") ? (prefix + isbn.trim()) : ", " + (prefix + isbn.trim());
						} else {

							if (!pengarang.trim().equals("") && !pengarang.trim().equals("_")) {
								String prefix = null;
								prefix = "inauthor:";
								query += query.equals("") ? (prefix + pengarang.trim())
										: ", " + (prefix + pengarang.trim());
							}

							if (!nama.trim().equals("") && !nama.trim().equals("_")) {
								String prefix = null;
								prefix = "intitle:";
								query += query.equals("") ? (prefix + nama.trim()) : ", " + (prefix + nama.trim());
							}

							if (!institusi.trim().equals("") && !institusi.trim().equals("_")) {
								String prefix = null;
								prefix = "inpublisher:";
								query += query.equals("") ? (prefix + institusi.trim())
										: ", " + (prefix + institusi.trim());
							}

							if (!keyword.trim().equals("") && !keyword.trim().equals("_")) {
								String prefix = null;
								prefix = "subject:";
								query += query.equals("") ? (prefix + keyword.trim())
										: ", " + (prefix + keyword.trim());
							}
						}

						if (!query.trim().isEmpty()) {
							try {
								JsonFactory jsonFactory = new JacksonFactory();

								Volumes myvolumes = manual != null && manual
										? BooksSample.queryGoogleBooks(jsonFactory, query, Integer.parseInt(banyak),
												Integer.parseInt(start))
										: BooksSample.queryGoogleBooks(jsonFactory, query, Integer.parseInt(banyak));

								if (myvolumes != null && myvolumes.getItems() != null) {
									for (final Volume volume : myvolumes.getItems()) {
										Item paramItem = null;
										Item i = CheckISBN.simpanVolume(volume, paramItem);
										if (i != null) {
											items.add(i);
										}
									}
								}
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/resources/PerpustakaanResource.java:2037");

					}
				}

			}
		}

		for (final Item item : items) {
			item.setDibuatOleh(null);
			item.setParent(null);

			if (!item.getGoogleBookChecked()) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							GoogleBookSynchronized.process(item);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/resources/PerpustakaanResource.java:2056");
						}
					}
				}).start();
			}

			if (!item.getOpenLibraryBookChecked()) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							OpenLibrarySyncronizer.process(item);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/resources/PerpustakaanResource.java:2069");
						}
					}
				}).start();
			}

			String imageUrl = item.getImageUrl();
			if (imageUrl == null || imageUrl.trim().equals("")) {
				imageUrl = CommonMedia.getMediaItem(item.getId(), 917, 575, false);
			}
			item.setImageUrl(imageUrl);
			item.setTersediaDi(LibraryUtil.tersediaDi(item));
		}

		return items;
	}

	@GET
	@Path("items/{perpustakaan}/{parent}/{nama}/{isbn}/{pengarang}/{keyword}/{abstrack}/{institusi}/{kategori}/{tahun}/{order}/{order1}/{start}/{banyak}/{sync}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<Item> daftarItem(@PathParam("perpustakaan") String perpustakaan, @PathParam("parent") String parent,
			@PathParam("nama") String nama, @PathParam("isbn") String isbn, @PathParam("pengarang") String pengarang,
			@PathParam("keyword") String keyword, @PathParam("abstrack") String abstrack,
			@PathParam("institusi") String institusi, @PathParam("kategori") String kategori,
			@PathParam("tahun") String tahun, @PathParam("order") String order, @PathParam("order1") String order1,
			@PathParam("start") String start, @PathParam("banyak") String banyak, @PathParam("sync") String sync)
			throws Exception {
		return daftarItemWithStart(perpustakaan, parent, nama, isbn, pengarang, keyword, abstrack, institusi, kategori,
				tahun, order, order1, start, banyak, sync, false);
	}

	@GET
	@Path("items_manual/{perpustakaan}/{parent}/{nama}/{isbn}/{pengarang}/{keyword}/{abstrack}/{institusi}/{kategori}/{tahun}/{order}/{order1}/{start}/{banyak}/{sync}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<Item> daftarItemManual(@PathParam("perpustakaan") String perpustakaan,
			@PathParam("parent") String parent, @PathParam("nama") String nama, @PathParam("isbn") String isbn,
			@PathParam("pengarang") String pengarang, @PathParam("keyword") String keyword,
			@PathParam("abstrack") String abstrack, @PathParam("institusi") String institusi,
			@PathParam("kategori") String kategori, @PathParam("tahun") String tahun, @PathParam("order") String order,
			@PathParam("order1") String order1, @PathParam("start") String start, @PathParam("banyak") String banyak,
			@PathParam("sync") String sync) throws Exception {
		return daftarItemWithStart(perpustakaan, parent, nama, isbn, pengarang, keyword, abstrack, institusi, kategori,
				tahun, order, order1, start, banyak, sync, true);
	}

	@GET
	@Path("tambah_komentar_item/{item}/{nama}/{alamat}/{kontak}/{email}/{perpustakaan}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public ItemKomentar tambahKomentarItem(@PathParam("item") String item, @PathParam("nama") String nama,
			@PathParam("alamat") String alamat, @PathParam("kontak") String kontak, @PathParam("email") String email,
			@PathParam("perpustakaan") String perpustakaan) throws Exception {
		ItemKomentar komentar = new ItemKomentar();
		try {
			nama = URLDecoder.decode(nama, "UTF-8");
			alamat = URLDecoder.decode(alamat, "UTF-8");
			kontak = URLDecoder.decode(kontak, "UTF-8");
			email = URLDecoder.decode(email, "UTF-8");
			perpustakaan = URLDecoder.decode(perpustakaan, "UTF-8");

			nama = nama.trim().equals("_") ? "" : nama;
			alamat = alamat.trim().equals("_") ? "" : alamat;
			kontak = kontak.trim().equals("_") ? "" : kontak;
			email = email.trim().equals("_") ? "" : email;
			perpustakaan = perpustakaan.trim().equals("_") ? "" : perpustakaan;

			Session session = HibernateUtil.currentNativeSession();

			StatusTerbitItem statusTerbitItem = (StatusTerbitItem) session.createCriteria(Item.class)
					.add(Restrictions.idEq(Long.parseLong(item)))
					.setProjection(Projections.property("statusTerbitItem")).uniqueResult();
			komentar.setAlamat(alamat);
			komentar.setEmail(email);
			komentar.setItem(new Item(Long.parseLong(item)));
			komentar.setKontak(kontak);
			komentar.setNama(nama);
			komentar.setStatusTerbitItemPadaSaatKomentar(statusTerbitItem);

			if (!perpustakaan.equals("-1") && !perpustakaan.equals("_") && !perpustakaan.equals("")) {
				komentar.setPerpustakaan(new Perpustakaan(Long.parseLong(perpustakaan)));
			}
			session.getTransaction().begin();
			session.save(komentar);
			session.getTransaction().commit();

			HibernateUtil.closeSession();
		} catch (Exception e) {
			HibernateUtil.rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}
		return komentar;
	}

	@GET
	@Path("daftar_item_komentar/{item}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarItemKomentar(@PathParam("item") String item) throws Exception {
		return daftarItemKomentar(item, "Terbit", "");
	}

	@GET
	@Path("daftar_item_komentar/{item}/{status}/{tidakStatus}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarItemKomentar(@PathParam("item") String item, @PathParam("status") String status,
			@PathParam("tidakStatus") String tidakStatus) throws Exception {
		return daftarItemKomentar(item, tidakStatus, tidakStatus, "0", "10");
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_item_komentar/{item}/{status}/{tidakStatus}/{start}/{banyak}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarItemKomentar(@PathParam("item") String item, @PathParam("status") String status,
			@PathParam("tidakStatus") String tidakStatus, @PathParam("start") String start,
			@PathParam("banyak") String banyak) throws Exception {

		status = URLDecoder.decode(status, "UTF-8");
		tidakStatus = URLDecoder.decode(tidakStatus, "UTF-8");

		Session session = HibernateUtil.currentNativeSession();
		List<ItemKomentar> komentarItems = session.createCriteria(ItemKomentar.class)
				.createAlias("statusTerbitItemPadaSaatKomentar", "statusTerbitItemPadaSaatKomentar", Criteria.LEFT_JOIN)
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
				.add(Restrictions.eq("item.id", Long.parseLong(item))).addOrder(Order.desc("tanggal_dirubah"))
				.setFirstResult(Integer.parseInt(start)).setMaxResults(Integer.parseInt(banyak.trim())).list();

		List<CommonID> commonIDs = new ArrayList<CommonID>();
		for (ItemKomentar komentarItem : komentarItems) {
			CommonID commonID = new CommonID();
			commonID.setInfo1(komentarItem.getNama());
			commonID.setInfo2(komentarItem.getKontak());
			commonID.setInfo3(komentarItem.getTanggal_dirubah() == null ? ""
					: Common.dateFormat6.get().format(komentarItem.getTanggal_dirubah()));
			commonID.setInfo4(komentarItem.getItem().getId() + "");
			commonID.setInfo5(komentarItem.getId() + "");
			commonID.setInfo6(komentarItem.getEmail());
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();

		return commonIDs;
	}

	@GET
	@Path("daftar_item_komentar_semua/{parent}/{satuanKerja}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarItemKomentarSemua(@PathParam("parent") String parent,
			@PathParam("satuanKerja") String satuanKerja) throws Exception {
		return daftarItemKomentarSemua(parent, satuanKerja, "0", "10");
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_item_komentar_semua/{parent}/{satuanKerja}/{start}/{banyak}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarItemKomentarSemua(@PathParam("parent") String parent,
			@PathParam("satuanKerja") String satuanKerja, @PathParam("start") String start,
			@PathParam("banyak") String banyak) throws Exception {

		Session session = HibernateUtil.currentNativeSession();
		Set<Long> parents = parent.trim().equals("") || parent.trim().equals("_") || parent.trim().equals("-1") ? null
				: new HashSet<Long>();

		if (parents != null) {
			parents.add(Long.parseLong(parent.trim()));
			PerpustakaanResourcesHelper perpustakaanResourcesHelper = new PerpustakaanResourcesHelper();
			perpustakaanResourcesHelper.generateChildsByIds(session, Long.parseLong(parent.trim()), parents);
		}

		System.out.println("parents = " + parents);

		List<ItemKomentar> komentarItems = session.createCriteria(ItemKomentar.class)
				.add(Restrictions.or(Restrictions.isNull("statusTerbitItemPadaSaatKomentar"),
						Restrictions.eq("statusTerbitItemPadaSaatKomentar", LibraryUtil.PUBLISH)))
				.createAlias("item", "item")
				.add(parents == null || parents.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("item.parent.id", parents))
				.add(satuanKerja.trim().equals("") || satuanKerja.trim().equals("_") || satuanKerja.trim().equals("-1")
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("item.defaultSatuanKerja.id", Long.parseLong(satuanKerja)))
				.addOrder(Order.desc("tanggal_dirubah")).setFirstResult(Integer.parseInt(start))
				.setMaxResults(Integer.parseInt(banyak.trim())).list();

		List<CommonID> commonIDs = new ArrayList<CommonID>();
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
	@Path("daftar_publikasi_item/{satuanKerja}/{perpustakaan}/{cari}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarPublikasiItem(@PathParam("satuanKerja") String satuanKerja,
			@PathParam("perpustakaan") String perpustakaan, @PathParam("cari") String cari) throws Exception {
		return daftarPublikasiItem(satuanKerja, perpustakaan, cari, "");
	}

	@GET
	@Path("daftar_publikasi_item/{satuanKerja}/{perpustakaan}/{cari}/{kategori}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarPublikasiItem(@PathParam("satuanKerja") String satuanKerja,
			@PathParam("perpustakaan") String perpustakaan, @PathParam("cari") String cari,
			@PathParam("kategori") String kategori) throws Exception {
		return daftarPublikasiItem(satuanKerja, perpustakaan, cari, kategori, "");
	}

	@GET
	@Path("daftar_publikasi_item/{satuanKerja}/{perpustakaan}/{cari}/{kategori}/{jenis}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarPublikasiItem(@PathParam("satuanKerja") String satuanKerja,
			@PathParam("perpustakaan") String perpustakaan, @PathParam("cari") String cari,
			@PathParam("kategori") String kategori, @PathParam("jenis") String jenis) throws Exception {
		return daftarPublikasiItem(satuanKerja, perpustakaan, cari, kategori, jenis, "");
	}

	@GET
	@Path("daftar_publikasi_item/{satuanKerja}/{perpustakaan}/{cari}/{kategori}/{jenis}/{publikasi}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarPublikasiItem(@PathParam("satuanKerja") String satuanKerja,
			@PathParam("perpustakaan") String perpustakaan, @PathParam("cari") String cari,
			@PathParam("kategori") String kategori, @PathParam("jenis") String jenis,
			@PathParam("publikasi") String publikasi) throws Exception {
		return daftarPublikasiItem(satuanKerja, perpustakaan, cari, kategori, jenis, publikasi, "0", "10");
	}

	@SuppressWarnings("unchecked")
	private String createLayoutItemTerbit(ItemPunyaTerbit itemPunyaTerbit, Session session, Session streamingSession,
			CommonID commonID) throws Exception {
		String penerbit = "";
		if (itemPunyaTerbit.getItem().getPenerbit() != null) {
			penerbit += penerbit.equals("") ? itemPunyaTerbit.getItem().getPenerbit().getNama()
					: ", " + itemPunyaTerbit.getItem().getPenerbit().getNama();
		}
		if (itemPunyaTerbit.getItem().getPenerbit2() != null) {
			penerbit += penerbit.equals("") ? itemPunyaTerbit.getItem().getPenerbit2().getNama()
					: ", " + itemPunyaTerbit.getItem().getPenerbit2().getNama();
		}
		if (itemPunyaTerbit.getItem().getPenerbit3() != null) {
			penerbit += penerbit.equals("") ? itemPunyaTerbit.getItem().getPenerbit3().getNama()
					: ", " + itemPunyaTerbit.getItem().getPenerbit3().getNama();
		}
		if (itemPunyaTerbit.getItem().getPenerbit4() != null) {
			penerbit += penerbit.equals("") ? itemPunyaTerbit.getItem().getPenerbit4().getNama()
					: ", " + itemPunyaTerbit.getItem().getPenerbit4().getNama();
		}
		if (itemPunyaTerbit.getItem().getPenerbit5() != null) {
			penerbit += penerbit.equals("") ? itemPunyaTerbit.getItem().getPenerbit5().getNama()
					: ", " + itemPunyaTerbit.getItem().getPenerbit5().getNama();
		}

		List<FotoItem> fotoItems = streamingSession.createCriteria(FotoItem.class)
				.add(Restrictions.or(Restrictions.isNull("ditampilkan"), Restrictions.eq("ditampilkan", true)))
				.add(Restrictions.eq("item", itemPunyaTerbit.getItem().getId())).addOrder(Order.desc("id")).list();
		String lampiran = "";
		for (FotoItem fotoItem : fotoItems) {
			String url = CommonMedia.getLampiranItem(fotoItem.getId());
			lampiran += lampiran.equals("") ? ("<a href=" + url + ">" + fotoItem.getNama() + "</a>")
					: (", <a href=" + url + ">" + fotoItem.getNama() + "</a>");
		}
		fotoItems = null;

		String keyword = "";
		if (itemPunyaTerbit.getItem().getKewords() != null) {
			keyword += keyword.equals("") ? itemPunyaTerbit.getItem().getKewords()
					: ", " + itemPunyaTerbit.getItem().getKewords();
		}
		if (itemPunyaTerbit.getItem().getKewordsEn() != null) {
			keyword += keyword.equals("") ? itemPunyaTerbit.getItem().getKewordsEn()
					: ", " + itemPunyaTerbit.getItem().getKewordsEn();
		}

		String imageUrl = itemPunyaTerbit.getItem().getImageUrl();
		if (imageUrl == null || imageUrl.trim().equals("")) {
			imageUrl = CommonMedia.getMediaItem(itemPunyaTerbit.getItem().getId(), 100, 120, false);
		}

		String sqlSedia = "select max(c.nama) as perpustakaan, sum((a.qty+a.qtybonus)*b.jenis) as stok "
				+ "from library.detail_transaksi a "
				+ "inner join library.kode_transaksi b on (a.kode_transaksi = b.id) "
				+ "left join library.perpustakaan c on (c.id = a.perpustakaan) " + "where a.item = "
				+ itemPunyaTerbit.getItem().getId() + " " + "group by a.perpustakaan "
				+ "having sum((a.qty+a.qtybonus)*b.jenis) > 0";

		List<Object[]> objects = session.createSQLQuery(sqlSedia).list();
		String tersediaDi = "";
		for (Object[] myObjects : objects) {
			try {
				tersediaDi += tersediaDi.equals("")
						? (myObjects[0] + " (" + Common.numberFormat.get().format(((Number) myObjects[1]).intValue()) + ")")
						: (", " + myObjects[0] + " (" + Common.numberFormat.get().format(((Number) myObjects[1]).intValue())
								+ ")");
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		commonID.setInfo6(itemPunyaTerbit.getContent());
		commonID.setInfo7(imageUrl);

		String kategori = itemPunyaTerbit.getItem().getKategories().replaceAll("\\[", "").replaceAll("\\]", "");

		String html = "<table border=\"0\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 100%;\"> " + "<tbody> "
				+ "	<tr> " + "		<td valign=\"top\" width=\"10%\"> " + "			<img alt=\"\" src=\"" + imageUrl
				+ "\" style=\"width: 100px; height: 120px;\" /></td> " + "		<td width=\"3%\"> "
				+ "			&nbsp;</td> " + "		<td valign=\"top\"> " + "			" + itemPunyaTerbit.getContent()
				+ "</p> " + "			<table border=\"0\" cellpadding=\"0\" cellspacing=\"8\"> "
				+ "				<tbody> "
				+ (itemPunyaTerbit.getItem().getIsbn() != null && !itemPunyaTerbit.getItem().getIsbn().trim().equals("")
						? ("					<tr> " + "						<td style=\"width:150px;\"> "
								+ "							 " + "								<strong>ISBN:</strong> "
								+ "						</td> " + "						<td style=\"width:610px;\"> "
								+ "							 " + itemPunyaTerbit.getItem().getIsbn() + " "
								+ "						</td> " + "					</tr> ")
						: "")
				+ (itemPunyaTerbit.getItem().getIssn() != null && !itemPunyaTerbit.getItem().getIssn().trim().equals("")
						? ("					<tr> " + "						<td style=\"width:150px;\"> "
								+ "							 " + "								<strong>ISSN:</strong> "
								+ "						</td> " + "						<td style=\"width:610px;\"> "
								+ "							 " + itemPunyaTerbit.getItem().getIssn() + " "
								+ "						</td> " + "					</tr> ")
						: "")
				+ "					<tr> " + "						<td style=\"width:150px;\"> "
				+ "							 " + "								<strong>Pengarang:</strong> "
				+ "						</td> " + "						<td style=\"width:390px;\"> "
				+ "							 "
				+ itemPunyaTerbit.getItem()
						.getPengarangs()
				+ " " + "						</td> " + "					</tr> "
				+ (!penerbit.trim().equals("") ? ("					<tr> "
						+ "						<td style=\"width:150px;\"> " + "							 "
						+ "								<strong>Penerbit:</strong> " + "						</td> "
						+ "						<td style=\"width:610px;\"> " + "							 "
						+ penerbit + " " + "						</td> " + "					</tr> ") : "")
				+ (itemPunyaTerbit.getPerpustakaan() != null ? ("					<tr> "
						+ "						<td style=\"width:150px;\"> " + "							 "
						+ "								<strong>Publikasi:</strong> " + "						</td> "
						+ "						<td style=\"width:610px;\"> " + "							 "
						+ itemPunyaTerbit.getPerpustakaan().getNama() + " ("
						+ (Common.dateFormat6.get().format(itemPunyaTerbit.getMulai()) + (itemPunyaTerbit.getSampai() == null
								? "" : (" s.d " + Common.dateFormat6.get().format(itemPunyaTerbit.getSampai()))))
						+ ")" + " " + "						</td> " + "					</tr> ") : "")
				+ (!keyword.trim().equals("") ? ("					<tr> "
						+ "						<td style=\"width:150px;\"> " + "							 "
						+ "								<strong>Kata Kunci:</strong> " + "						</td> "
						+ "						<td style=\"width:610px;\"> " + "							 " + keyword
						+ " " + "						</td> " + "					</tr> ") : "")
				+ (!kategori.trim().equals("") ? ("					<tr> "
						+ "						<td style=\"width:150px;\"> " + "							 "
						+ "								<strong>Kategori:</strong> " + "						</td> "
						+ "						<td style=\"width:610px;\"> " + "							 "
						+ kategori + " " + "						</td> " + "					</tr> ") : "")
				+ "					<tr> " + "						<td style=\"width:150px;\"> "
				+ "							 " + "								<strong>Bahasa:</strong> "
				+ "						</td> " + "						<td style=\"width:390px;\"> "
				+ "							 " + itemPunyaTerbit.getItem().getBahasa() + " "
				+ "						</td> " + "					</tr> " + "					<tr> "
				+ "						<td style=\"width:150px;\"> " + "							 "
				+ "								<strong>Tahun:</strong> " + "						</td> "
				+ "						<td style=\"width:390px;\"> " + "							 "
				+ itemPunyaTerbit.getItem().getTahun() + " " + "						</td> "
				+ "					</tr> " + "					<tr> "
				+ "						<td style=\"width:150px;\"> " + "							 "
				+ "								<strong>Jenis:</strong> " + "						</td> "
				+ "						<td style=\"width:390px;\"> " + "							 "
				+ (itemPunyaTerbit.getItem().getJenisItem() == null ? ""
						: itemPunyaTerbit.getItem().getJenisItem().getNama())
				+ " " + "						</td> " + "					</tr> " + "					<tr> "
				+ "						<td style=\"width:150px;\"> " + "							 "
				+ "								<strong>Tipe:</strong> " + "						</td> "
				+ "						<td style=\"width:390px;\"> " + "							 "
				+ (itemPunyaTerbit.getItem().getTipeItem() == null ? ""
						: itemPunyaTerbit.getItem().getTipeItem().getNama())
				+ " " + "						</td> " + "					</tr> "
				+ (itemPunyaTerbit.getItem().getHalaman() != null && !itemPunyaTerbit.getItem().getHalaman().equals(0)
						? ("					<tr> " + "						<td style=\"width:150px;\"> "
								+ "							 "
								+ "								<strong>Jumlah halaman:</strong> "
								+ "						</td> " + "						<td style=\"width:390px;\"> "
								+ "							 " + itemPunyaTerbit.getItem().getHalaman() + " "
								+ "						</td> " + "					</tr> ")
						: "")
				+ (itemPunyaTerbit.getItem().getEdisi() != null
						&& !itemPunyaTerbit.getItem().getEdisi().trim().equals("") ? ("					<tr> "
								+ "						<td style=\"width:150px;\"> " + "							 "
								+ "								<strong>Edisi:</strong> "
								+ "						</td> " + "						<td style=\"width:390px;\"> "
								+ "							 " + itemPunyaTerbit.getItem().getEdisi() + " "
								+ "						</td> " + "					</tr> ") : "")
				+ (itemPunyaTerbit.getItem().getPenaklikan() != null
						&& !itemPunyaTerbit.getItem().getPenaklikan().trim().equals("") ? ("					<tr> "
								+ "						<td style=\"width:150px;\"> " + "							 "
								+ "								<strong>Penaklikan:</strong> "
								+ "						</td> " + "						<td style=\"width:390px;\"> "
								+ "							 " + itemPunyaTerbit.getItem().getPenaklikan() + " "
								+ "						</td> " + "					</tr> ") : "")
				+ (!lampiran.trim().equals("") ? ("					<tr> "
						+ "						<td style=\"width:150px;\"> " + "							 "
						+ "								<strong>Lampiran:</strong> " + "						</td> "
						+ "						<td style=\"width:610px;\"> " + "							 "
						+ lampiran + " " + "						</td> " + "					</tr> ") : "")
				+ (!tersediaDi.trim().equals("") ? ("					<tr> "
						+ "						<td style=\"width:150px;\"> " + "							 "
						+ "								<strong>Tersedia di:</strong> " + "						</td> "
						+ "						<td style=\"width:610px;\"> " + "							 "
						+ tersediaDi + " " + "						</td> " + "					</tr> ") : "")
				+ "				</tbody> " + "			</table> " + "		</td> " + "	</tr> " + "</tbody> "
				+ "</table> " + "<br />";

		return html;
	}

	@GET
	@Path("publikasi_item_by_item_id/{id}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID publikasiItemByItemId(@PathParam("id") String id) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

		ItemPunyaTerbit itemPunyaTerbit = (ItemPunyaTerbit) session.createCriteria(ItemPunyaTerbit.class)
				.createAlias("item", "item").setMaxResults(1).addOrder(Order.desc("id"))
				.add(Restrictions.eq("item.id", Long.parseLong(id))).uniqueResult();

		if (itemPunyaTerbit != null) {
			CommonID commonID = new CommonID();
			commonID.setId(itemPunyaTerbit.getId());
			commonID.setInfo1(itemPunyaTerbit.getItem().getNama());
			commonID.setInfo2(itemPunyaTerbit.getItem().getTema());
			commonID.setInfo3(itemPunyaTerbit.getItem().getKewords());

			commonID.setInfo4(createLayoutItemTerbit(itemPunyaTerbit, session, streamingSession, commonID));

			Integer count = ((Number) session.createCriteria(ItemPunyaTerbitKomentar.class)
					.setProjection(Projections.rowCount()).add(Restrictions.eq("itemPunyaTerbit", itemPunyaTerbit))
					.uniqueResult()).intValue();
			commonID.setInfo5(Common.numberFormat.get().format(count));

			StreamingHibernateUtil.getInstance().closeSession();

			HibernateUtil.closeSession();

			return commonID;
		} else {
			return new CommonID(Long.parseLong(id));
		}
	}

	@GET
	@Path("publikasi_item/{id}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID publikasiItem(@PathParam("id") String id) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

		ItemPunyaTerbit itemPunyaTerbit = (ItemPunyaTerbit) session.createCriteria(ItemPunyaTerbit.class)
				.add(Restrictions.idEq(Long.parseLong(id))).uniqueResult();

		CommonID commonID = new CommonID();
		commonID.setId(itemPunyaTerbit.getId());
		commonID.setInfo1(itemPunyaTerbit.getItem().getNama());
		commonID.setInfo2(itemPunyaTerbit.getItem().getTema());
		commonID.setInfo3(itemPunyaTerbit.getItem().getKewords());

		commonID.setInfo4(createLayoutItemTerbit(itemPunyaTerbit, session, streamingSession, commonID));

		Integer count = ((Number) session.createCriteria(ItemPunyaTerbitKomentar.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("itemPunyaTerbit", itemPunyaTerbit))
				.uniqueResult()).intValue();
		commonID.setInfo5(Common.numberFormat.get().format(count));

		StreamingHibernateUtil.getInstance().closeSession();

		HibernateUtil.closeSession();

		return commonID;
	}

	@GET
	@Path("daftar_publikasi_item/{satuanKerja}/{perpustakaan}/{cari}/{kategori}/{jenis}/{publikasi}/{start}/{banyak}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarPublikasiItem(@PathParam("satuanKerja") String satuanKerja,
			@PathParam("perpustakaan") String perpustakaan, @PathParam("cari") String cari,
			@PathParam("kategori") String kategori, @PathParam("jenis") String jenis,
			@PathParam("publikasi") String publikasi, @PathParam("start") String start,
			@PathParam("banyak") String banyak) throws Exception {
		return daftarPublikasiItem(satuanKerja, perpustakaan, cari, kategori, jenis, publikasi, "", start, banyak);
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_publikasi_item/{satuanKerja}/{perpustakaan}/{cari}/{kategori}/{jenis}/{publikasi}/{ddc}/{start}/{banyak}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarPublikasiItem(@PathParam("satuanKerja") String satuanKerja,
			@PathParam("perpustakaan") String perpustakaan, @PathParam("cari") String cari,
			@PathParam("kategori") String kategori, @PathParam("jenis") String jenis,
			@PathParam("publikasi") String publikasi, @PathParam("ddc") String ddc, @PathParam("start") String start,
			@PathParam("banyak") String banyak) throws Exception {
		List<CommonID> commonIDs = new ArrayList<CommonID>();
		try {
			cari = URLDecoder.decode(cari, "UTF-8");
			kategori = URLDecoder.decode(kategori, "UTF-8");
			jenis = URLDecoder.decode(jenis, "UTF-8");
			publikasi = URLDecoder.decode(publikasi, "UTF-8");
			ddc = URLDecoder.decode(ddc, "UTF-8");

			System.out.println("ddc = " + ddc);

			Session session = HibernateUtil.currentNativeSession();
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

			List<Long> items = new ArrayList<Long>();
			if (!ddc.trim().equals("") && !ddc.trim().equals("_") && !ddc.trim().equals("-1")) {
				items = session.createCriteria(DataDdcItemDetail.class).createAlias("dataDdcItem", "dataDdcItem")
						.createAlias("dataDdcItem.ddcItem", "ddcItem")
						.add(Restrictions.eq("ddcItem.id", Long.parseLong(ddc))).createAlias("item", "item")
						.setProjection(Projections.property("item.id")).list();
			}

			Criterion criterion = Restrictions.or(Restrictions.ilike("item.nama", cari, MatchMode.ANYWHERE),
					Restrictions.ilike("item.tema", cari, MatchMode.ANYWHERE));
			criterion = Restrictions.or(criterion, Restrictions.ilike("item.abstrak", cari, MatchMode.ANYWHERE));
			criterion = Restrictions.or(criterion, Restrictions.ilike("item.abstrakEn", cari, MatchMode.ANYWHERE));
			criterion = Restrictions.or(criterion, Restrictions.ilike("item.kewords", cari, MatchMode.ANYWHERE));
			criterion = Restrictions.or(criterion, Restrictions.ilike("item.kewordsEn", cari, MatchMode.ANYWHERE));
			criterion = Restrictions.or(criterion, Restrictions.ilike("item.isbn", cari, MatchMode.ANYWHERE));
			criterion = Restrictions.or(criterion, Restrictions.ilike("item.issn", cari, MatchMode.ANYWHERE));
			criterion = Restrictions.or(criterion, Restrictions.ilike("item.pengarangs", cari, MatchMode.ANYWHERE));
			criterion = Restrictions.or(criterion, Restrictions.ilike("content", cari, MatchMode.ANYWHERE));

			List<ItemPunyaTerbit> itemPunyaTerbits = session.createCriteria(ItemPunyaTerbit.class)
					.createAlias("item", "item", Criteria.INNER_JOIN)
					.createAlias("item.jenisItem", "jenisItem", Criteria.LEFT_JOIN)

					.add(publikasi.trim().equals("") || publikasi.trim().equals("_") || publikasi.trim().equals("-1")
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("perpustakaan.id", Long.parseLong(publikasi.trim())))

					.add(ddc.trim().equals("") || ddc.trim().equals("_") || ddc.trim().equals("-1")
							? Restrictions.sqlRestriction("1=1")
							: items.size() == 0 ? Restrictions.sqlRestriction("1!=1")
									: Restrictions.in("item.id", items))

					.add(cari.trim().equals("") || cari.trim().equals("_") || cari.trim().equals("-1")
							? Restrictions.sqlRestriction("1=1") : criterion)

					.add(kategori.trim().equals("") || kategori.trim().equals("_") || kategori.trim().equals("-1")
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.ilike("item.kategories", "[" + kategori + "]", MatchMode.ANYWHERE))

					.add(jenis.trim().equals("") || jenis.trim().equals("_") || jenis.trim().equals("-1")
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenisItem.id", Long.parseLong(jenis)))

					.add(satuanKerja.trim().equals("") || satuanKerja.trim().equals("_")
							|| satuanKerja.trim().equals("-1") ? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("satuanKerja.id", Long.parseLong(satuanKerja)))

					.add(perpustakaan.trim().equals("") || perpustakaan.trim().equals("_")
							|| perpustakaan.trim().equals("-1") ? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("perpustakaan.id", Long.parseLong(perpustakaan)))

					.add(Restrictions.sqlRestriction(
							"date(mulai) <= date('" + Common.databaseDateFormat1.get().format(WaktuUtil.getDate())
									+ "') and (sampai is null or date(sampai) >= date('"
									+ Common.databaseDateFormat1.get().format(WaktuUtil.getDate()) + "'))"))

					// .add(Restrictions.or(Restrictions.isNull("sampai"),
					// Restrictions.lt("sampai",
					// ais.ui.util.WaktuUtil.getDate())))
					// .add(Restrictions.ge("mulai", calendar.getTime()))
					.addOrder(Order.desc("id")).setFirstResult(Integer.parseInt(start))
					.setMaxResults(Integer.parseInt(banyak.trim())).list();
			for (ItemPunyaTerbit itemPunyaTerbit : itemPunyaTerbits) {

				CommonID commonID = new CommonID();
				commonID.setId(itemPunyaTerbit.getId());
				commonID.setInfo1(itemPunyaTerbit.getItem().getNama());
				commonID.setInfo2(itemPunyaTerbit.getItem().getTema());
				commonID.setInfo3(itemPunyaTerbit.getItem().getKewords());

				commonID.setInfo4(createLayoutItemTerbit(itemPunyaTerbit, session, streamingSession, commonID));

				Integer count = ((Number) session.createCriteria(ItemPunyaTerbitKomentar.class)
						.setProjection(Projections.rowCount()).add(Restrictions.eq("itemPunyaTerbit", itemPunyaTerbit))
						.uniqueResult()).intValue();
				commonID.setInfo5(Common.numberFormat.get().format(count));
				commonID.setInfo6(itemPunyaTerbit.getItem().getIsbn());
				commonID.setInfo7(itemPunyaTerbit.getItem().getIsbn10());
				commonID.setInfo8(itemPunyaTerbit.getItem().getIssn());

				commonIDs.add(commonID);
			}
			StreamingHibernateUtil.getInstance().closeSession();

			HibernateUtil.closeSession();
		} catch (Exception e) {
			HibernateUtil.rollbackTransaction();
		}
		return commonIDs;
	}

	@GET
	@Path("/tambah_komentar_item_terbit")
	@Produces("text/plain")
	public String tambahKomentarItemterbit(@DefaultValue("All") @QueryParam(value = "item") final List<String> item)
			throws Exception {
		try {
			String id = item.get(0);
			String nama = item.get(1);
			String kontak = item.get(2);
			String email = item.get(3);

			nama = nama.trim().equals("_") ? "" : nama;
			kontak = kontak.trim().equals("_") ? "" : kontak;
			email = email.trim().equals("_") ? "" : email;

			Session session = HibernateUtil.currentNativeSession();

			ItemPunyaTerbit itemPunyaTerbit = (ItemPunyaTerbit) session.createCriteria(ItemPunyaTerbit.class)
					.add(Restrictions.idEq(Long.parseLong(id))).uniqueResult();
			if (itemPunyaTerbit == null) {

				HibernateUtil.closeSession();
				return "ERROR";
			}

			ItemPunyaTerbitKomentar komentar = new ItemPunyaTerbitKomentar();
			komentar.setAlamat("");
			komentar.setEmail(email);
			komentar.setItemPunyaTerbit(itemPunyaTerbit);
			komentar.setKontak(kontak);
			komentar.setNama(nama);

			session.getTransaction().begin();
			session.save(komentar);
			session.getTransaction().commit();

			HibernateUtil.closeSession();
		} catch (Exception e) {
			HibernateUtil.rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}
		return "OK";
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_item_terbit_komentar/{item}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarItemTerbitKomentar(@PathParam("item") String item) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<ItemPunyaTerbitKomentar> komentarItems = session.createCriteria(ItemPunyaTerbitKomentar.class)
				.add(Restrictions.eq("itemPunyaTerbit.id", Long.parseLong(item)))
				.addOrder(Order.desc("tanggal_dirubah")).setMaxResults(Common.MAX_RESULT_20).list();

		List<CommonID> commonIDs = new ArrayList<CommonID>();
		for (ItemPunyaTerbitKomentar komentarItem : komentarItems) {
			CommonID commonID = new CommonID();
			commonID.setInfo1(komentarItem.getNama());
			commonID.setInfo2(komentarItem.getKontak());
			commonID.setInfo3(komentarItem.getTanggal_dirubah() == null ? ""
					: Common.dateFormat6.get().format(komentarItem.getTanggal_dirubah()));
			commonID.setInfo4(komentarItem.getItemPunyaTerbit().getId() + "");
			commonID.setInfo5(komentarItem.getId() + "");
			commonID.setInfo6(komentarItem.getEmail());
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();

		return commonIDs;
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_item_terbit_komentar_semua/{start}/{banyak}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarItemTerbitKomentarSemua(@PathParam("start") String start,
			@PathParam("banyak") String banyak) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<ItemPunyaTerbitKomentar> komentarItems = session.createCriteria(ItemPunyaTerbitKomentar.class)
				.addOrder(Order.desc("id")).setFirstResult(Integer.parseInt(start))
				.setMaxResults(Integer.parseInt(banyak.trim())).list();

		List<CommonID> commonIDs = new ArrayList<CommonID>();
		for (ItemPunyaTerbitKomentar komentarItem : komentarItems) {
			CommonID commonID = new CommonID();
			commonID.setInfo1(komentarItem.getNama());
			commonID.setInfo2(komentarItem.getKontak());
			commonID.setInfo3(komentarItem.getTanggal_dirubah() == null ? ""
					: Common.dateFormat6.get().format(komentarItem.getTanggal_dirubah()));
			commonID.setInfo4(komentarItem.getItemPunyaTerbit().getId() + "");
			commonID.setInfo5(komentarItem.getId() + "");
			commonID.setInfo6(komentarItem.getEmail());
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();

		return commonIDs;
	}

	@GET
	@Path("daftar_item_terbit_jumlah_komentar/{item}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID daftarItemTerbitJumlahKomentar(@PathParam("item") String item) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		Integer jumlahKomentarItems = ((Number) session.createCriteria(ItemPunyaTerbitKomentar.class)
				.add(Restrictions.eq("itemPunyaTerbit.id", Long.parseLong(item))).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		CommonID commonID = new CommonID(Long.parseLong(item));
		commonID.setInfo1(Common.numberFormat.get().format(jumlahKomentarItems));

		HibernateUtil.closeSession();
		return commonID;
	}

	@GET
	@Path("/tambah_komentar_informasi_perpustakaan")
	@Produces("text/plain")
	public String tambahKomentarInformasiPerpustakaan(
			@DefaultValue("All") @QueryParam(value = "item") final List<String> item) throws Exception {
		try {
			String id = item.get(0);
			String nama = item.get(1);
			String kontak = item.get(2);
			String email = item.get(3);

			nama = nama.trim().equals("_") ? "" : nama;
			kontak = kontak.trim().equals("_") ? "" : kontak;
			email = email.trim().equals("_") ? "" : email;

			Session session = HibernateUtil.currentNativeSession();

			InformasiPerpustakaan informasiPerpustakaan = (InformasiPerpustakaan) session
					.createCriteria(InformasiPerpustakaan.class).add(Restrictions.idEq(Long.parseLong(id)))
					.uniqueResult();
			if (informasiPerpustakaan == null) {

				HibernateUtil.closeSession();
				return "ERROR";
			}

			InformasiPerpustakaanKomentar komentar = new InformasiPerpustakaanKomentar();
			komentar.setAlamat("");
			komentar.setEmail(email);
			komentar.setInformasiPerpustakaan(informasiPerpustakaan);
			komentar.setKontak(kontak);
			komentar.setNama(nama);
			session.getTransaction().begin();
			session.save(komentar);
			session.getTransaction().commit();

			HibernateUtil.closeSession();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			HibernateUtil.rollbackTransaction();
		}
		return "OK";
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_domain_penelitian/{penerbit}/{parent}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarDomainPenelitian(@PathParam("penerbit") String penerbit,
			@PathParam("parent") String parent) {
		List<CommonID> commonIDs = new ArrayList<CommonID>();

		parent = parent.trim().equals("_") || parent.trim().equals("-1") ? "" : parent.trim();

		penerbit = penerbit.trim().equals("_") || penerbit.trim().equals("-1") ? "-1" : penerbit.trim();

		Session session = HibernateUtil.currentNativeSession();
		List<DomainPenelitian> domainPenelitians = session.createCriteria(DomainPenelitian.class)
				.add(penerbit.equals("-1") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("penerbit.id", Long.parseLong(penerbit)))
				.add(Restrictions.eq("defaultItem", true)).add(parent.trim().equals("") ? Restrictions.isNull("parent")
						: Restrictions.eq("parent.id", Long.parseLong(parent)))
				.addOrder(Order.asc("nama")).list();
		for (DomainPenelitian domainPenelitian : domainPenelitians) {
			CommonID commonID = new CommonID(domainPenelitian.getId());
			commonID.setInfo1(domainPenelitian.getNama());
			commonID.setInfo2(domainPenelitian.getPenerbit().getId() + "");
			commonID.setInfo3(domainPenelitian.getPenerbit().getNama() + "");
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();
		return commonIDs;
	}

	@GET
	@Path("daftar_kategori_item/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarKategoriItem() {
		return daftarKategoriItem("");
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_kategori_item/{parent}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarKategoriItem(@PathParam("parent") String parent) {
		List<CommonID> commonIDs = new ArrayList<CommonID>();

		parent = parent.trim().equals("_") || parent.trim().equals("-1") ? "" : parent.trim();

		Session session = HibernateUtil.currentNativeSession();
		List<KategoriItem> kategoriItems = session.createCriteria(KategoriItem.class)
				.add(Restrictions.eq("defaultItem", true)).add(parent.trim().equals("") ? Restrictions.isNull("parent")
						: Restrictions.eq("parent.id", Long.parseLong(parent)))
				.addOrder(Order.asc("nama")).list();
		for (KategoriItem kategoriItem : kategoriItems) {
			CommonID commonID = new CommonID(kategoriItem.getId());
			commonID.setInfo1(kategoriItem.getNama());
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();
		return commonIDs;
	}

	@GET
	@Path("daftar_ddc_item/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarDdcItem() {
		return daftarDdcItem("");
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_ddc_item/{parent}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarDdcItem(@PathParam("parent") String parent) {
		List<CommonID> commonIDs = new ArrayList<CommonID>();

		parent = parent.trim().equals("_") || parent.trim().equals("-1") ? "" : parent.trim();

		Session session = HibernateUtil.currentNativeSession();
		List<DdcItem> ddcItems = session
				.createCriteria(DdcItem.class).add(Restrictions.eq("defaultItem", true)).add(parent.trim().equals("")
						? Restrictions.isNull("parent") : Restrictions.eq("parent.id", Long.parseLong(parent)))
				.addOrder(Order.asc("id")).list();
		for (DdcItem ddcItem : ddcItems) {
			CommonID commonID = new CommonID(ddcItem.getId());
			commonID.setInfo1(ddcItem.getNama());
			commonID.setInfo2(ddcItem.getKode());
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();
		return commonIDs;
	}

	@GET
	@Path("daftar_udc_item/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarUdcItem() {
		return daftarUdcItem("");
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_udc_item/{parent}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarUdcItem(@PathParam("parent") String parent) {
		List<CommonID> commonIDs = new ArrayList<CommonID>();

		parent = parent.trim().equals("_") || parent.trim().equals("-1") ? "" : parent.trim();

		Session session = HibernateUtil.currentNativeSession();
		List<UdcItem> udcItems = session
				.createCriteria(UdcItem.class).add(Restrictions.eq("defaultItem", true)).add(parent.trim().equals("")
						? Restrictions.isNull("parent") : Restrictions.eq("parent.id", Long.parseLong(parent)))
				.addOrder(Order.asc("kode")).list();
		for (UdcItem udcItem : udcItems) {
			CommonID commonID = new CommonID(udcItem.getId());
			commonID.setInfo1(udcItem.getNama());
			commonID.setInfo2(udcItem.getKode());
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();
		return commonIDs;
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_jenis_item/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarJenisItem() {
		List<CommonID> commonIDs = new ArrayList<CommonID>();
		Session session = HibernateUtil.currentNativeSession();
		List<ais.database.model.library.JenisItem> kategoriItems = session
				.createCriteria(ais.database.model.library.JenisItem.class).addOrder(Order.asc("nama")).list();
		for (ais.database.model.library.JenisItem kategoriItem : kategoriItems) {
			CommonID commonID = new CommonID(kategoriItem.getId());
			commonID.setInfo1(kategoriItem.getNama());
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();
		return commonIDs;
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_perpustakaan/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarPerpustakkan() {
		List<CommonID> commonIDs = new ArrayList<CommonID>();
		Session session = HibernateUtil.currentNativeSession();
		List<Perpustakaan> perpustakaans = session.createCriteria(Perpustakaan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		for (Perpustakaan perpustakaan : perpustakaans) {
			CommonID commonID = new CommonID(perpustakaan.getId());
			commonID.setInfo1(perpustakaan.getNama());
			commonIDs.add(commonID);
		}
		return commonIDs;
	}

	@GET
	@Path("daftar_informasi_perpustakaan/{satuanKerja}/{perpustakaan}/{cari}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarInformasiPerpustakaan(@PathParam("satuanKerja") String satuanKerja,
			@PathParam("perpustakaan") String perpustakaan, @PathParam("cari") String cari) throws Exception {
		return daftarInformasiPerpustakaan(satuanKerja, perpustakaan, cari, "0", "10");
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_informasi_perpustakaan/{satuanKerja}/{perpustakaan}/{cari}/{start}/{banyak}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarInformasiPerpustakaan(@PathParam("satuanKerja") String satuanKerja,
			@PathParam("perpustakaan") String perpustakaan, @PathParam("cari") String cari,
			@PathParam("start") String start, @PathParam("banyak") String banyak) throws Exception {
		List<CommonID> commonIDs = new ArrayList<CommonID>();

		cari = URLDecoder.decode(cari, "UTF-8");

		Session session = HibernateUtil.currentNativeSession();
		Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

		// Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		// // calendar.set(Calendar.HOUR_OF_DAY,
		// // calendar.get(Calendar.HOUR_OF_DAY) + 3);

		List<InformasiPerpustakaan> informasiPerpustakaans = session.createCriteria(InformasiPerpustakaan.class)
				.add(cari.trim().equals("") || cari.trim().equals("_") || cari.trim().equals("-1")
						? Restrictions.sqlRestriction("1=1") : Restrictions.ilike("content", cari, MatchMode.ANYWHERE))

				.add(satuanKerja.trim().equals("") || satuanKerja.trim().equals("_") || satuanKerja.trim().equals("-1")
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("satuanKerja.id", Long.parseLong(satuanKerja)))

				.add(perpustakaan.trim().equals("") || perpustakaan.trim().equals("_")
						|| perpustakaan.trim().equals("-1") ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("perpustakaan.id", Long.parseLong(perpustakaan)))

				.add(Restrictions
						.sqlRestriction("date(mulai) <= date('" + Common.databaseDateFormat1.get().format(WaktuUtil.getDate())
								+ "') and (sampai is null or date(sampai) >= date('"
								+ Common.databaseDateFormat1.get().format(WaktuUtil.getDate()) + "'))"))
				// .add(Restrictions.or(Restrictions.isNull("sampai"),
				// Restrictions.lt("sampai", ais.ui.util.WaktuUtil.getDate())))
				// .add(Restrictions.gt("mulai", calendar.getTime()))
				.addOrder(Order.desc("mulai")).setFirstResult(Integer.parseInt(start))
				.setMaxResults(Integer.parseInt(banyak.trim())).list();

		for (InformasiPerpustakaan informasiPerpustakaan : informasiPerpustakaans) {
			CommonID commonID = new CommonID();

			List<FotoInformasiPerpustakaan> fotoItems = streamingSession.createCriteria(FotoInformasiPerpustakaan.class)
					.add(Restrictions.or(Restrictions.isNull("ditampilkan"), Restrictions.eq("ditampilkan", true)))
					.add(Restrictions.eq("informasiPerpustakaan", informasiPerpustakaan.getId()))
					.addOrder(Order.desc("id")).list();
			String lampiran = "";
			for (FotoInformasiPerpustakaan fotoItem : fotoItems) {
				String url = CommonMedia.getLampiranInformasiPerpustakaan(fotoItem.getId());
				lampiran += lampiran.equals("") ? ("<a href=" + url + ">" + fotoItem.getNama() + "</a>")
						: (", <a href=" + url + ">" + fotoItem.getNama() + "</a>");
			}
			fotoItems = null;

			commonID.setId(informasiPerpustakaan.getId());
			String html = informasiPerpustakaan.getContent();
			commonID.setInfo1(html);
			commonID.setInfo2(lampiran);
			commonID.setInfo3(informasiPerpustakaan.getSatuanKerja() == null ? ""
					: informasiPerpustakaan.getSatuanKerja().getNama());
			commonID.setInfo4(informasiPerpustakaan.getPerpustakaan() == null ? ""
					: informasiPerpustakaan.getPerpustakaan().getNama());
			commonID.setInfo5(informasiPerpustakaan.getMulai() == null ? ""
					: Common.dateFormat2.get().format(informasiPerpustakaan.getMulai()));
			commonID.setInfo6(informasiPerpustakaan.getSampai() == null ? ""
					: Common.dateFormat2.get().format(informasiPerpustakaan.getSampai()));
			Integer count = ((Number) session.createCriteria(InformasiPerpustakaanKomentar.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("informasiPerpustakaan", informasiPerpustakaan)).uniqueResult()).intValue();
			commonID.setInfo7(Common.numberFormat.get().format(count));
			commonID.setInfo8(informasiPerpustakaan.getJenisInformasiPerpustakaan() == null ? ""
					: informasiPerpustakaan.getJenisInformasiPerpustakaan().getNama());
			commonIDs.add(commonID);
		}
		StreamingHibernateUtil.getInstance().closeSession();

		HibernateUtil.closeSession();
		return commonIDs;
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("daftar_informasi_perpustakaan_komentar/{item}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<CommonID> daftarInformasiPerpustakaanKomentar(@PathParam("item") String item) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<InformasiPerpustakaanKomentar> komentarItems = session.createCriteria(InformasiPerpustakaanKomentar.class)
				.add(Restrictions.eq("informasiPerpustakaan.id", Long.parseLong(item)))
				.addOrder(Order.desc("tanggal_dirubah")).setMaxResults(Common.MAX_RESULT_20).list();

		List<CommonID> commonIDs = new ArrayList<CommonID>();
		for (InformasiPerpustakaanKomentar komentarItem : komentarItems) {
			CommonID commonID = new CommonID();
			commonID.setInfo1(komentarItem.getNama());
			commonID.setInfo2(komentarItem.getKontak());
			commonID.setInfo3(komentarItem.getTanggal_dirubah() == null ? ""
					: Common.dateFormat6.get().format(komentarItem.getTanggal_dirubah()));
			commonID.setInfo4(komentarItem.getInformasiPerpustakaan().getId() + "");
			commonID.setInfo5(komentarItem.getId() + "");
			commonID.setInfo6(komentarItem.getEmail());
			commonIDs.add(commonID);
		}

		HibernateUtil.closeSession();

		return commonIDs;
	}

	@GET
	@Path("daftar_informasi_perpustakaan_jumlah_komentar/{item}/")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID daftarInformasiPerpustakaanJumlahKomentar(@PathParam("item") String item) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		Integer jumlahKomentarItems = ((Number) session.createCriteria(InformasiPerpustakaanKomentar.class)
				.add(Restrictions.eq("informasiPerpustakaan.id", Long.parseLong(item)))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		CommonID commonID = new CommonID(Long.parseLong(item));
		commonID.setInfo1(Common.numberFormat.get().format(jumlahKomentarItems));

		HibernateUtil.closeSession();
		return commonID;
	}
}
