package ais.action.master.library.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.net.URLEncoder;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Paging;

import ais.action.master.helper.util.GoogleBookSynchronized;
import ais.action.master.helper.util.OpenLibrarySyncronizer;
import ais.action.master.library.ItemAction;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KunjunganTamu;
import ais.database.model.Mahasiswa;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoImagePerHalamanItem;
import ais.database.model.file.FotoGambarItem;
import ais.database.model.file.FotoItem;
import ais.database.model.file.LampiranLain;
import ais.database.model.library.Anggota;
import ais.database.model.library.BatasWaktuPeminjamanItem;
import ais.database.model.library.DendaKeterlambatanItem;
import ais.database.model.library.DomainPenelitian;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaPengarang;
import ais.database.model.library.ItemTemporary;
import ais.database.model.library.JenisAnggota;
import ais.database.model.library.JenisIdentitasAnggota;
import ais.database.model.library.JenisInformasiPerpustakaan;
import ais.database.model.library.JenisItem;
import ais.database.model.library.KategoriItem;
import ais.database.model.library.KembaliPengadaanItem;
import ais.database.model.library.KembaliPengadaanItemDetail;
import ais.database.model.library.KodeTransaksi;
import ais.database.model.library.PeminjamanPengadaanItem;
import ais.database.model.library.PeminjamanPengadaanItemDetail;
import ais.database.model.library.Pengarang;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.StatusItem;
import ais.database.model.library.StatusTerbitItem;
import ais.database.model.library.TipeAnggota;
import ais.database.model.library.TipeItem;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sirkulasisurat.KembaliSuratItem;
import ais.database.model.sirkulasisurat.KembaliSuratItemDetail;
import ais.database.model.sirkulasisurat.PeminjamanSuratItem;
import ais.database.model.sirkulasisurat.PeminjamanSuratItemDetail;
import ais.database.model.surat.SuratMasuk;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LibraryUtil {

	public static KodeTransaksi adjustmentPenambahan;
	public static KodeTransaksi adjustmentPengurangan;

	public static KodeTransaksi SALDO_AWAL;
	public static KodeTransaksi BELI_MASUK;
	public static KodeTransaksi HIBAH_MASUK;
	public static KodeTransaksi MASUK_LAIN;
	public static KodeTransaksi RETUR_BELI;
	public static KodeTransaksi PINJAM_KELUAR;
	public static KodeTransaksi PENGEMBALIAN_MASUK;
	public static KodeTransaksi HILANG;
	public static KodeTransaksi KELUAR_LAIN;
	public static KodeTransaksi PEMAKAIAN;
	public static KodeTransaksi TRANSFER;
	public static KodeTransaksi TERIMA;

	public static StatusItem AKTIF;

	public static TipeItem KARYA_ILMIAH;
	public static TipeItem SKRIPSI;
	public static TipeItem THESIS;
	public static TipeItem DISERTASI;

	public static StatusTerbitItem DRAFT;
	public static StatusTerbitItem APPROVE;
	public static StatusTerbitItem REJECT;
	public static StatusTerbitItem PUBLISH;

	public static JenisAnggota ANGGOTA_REGULER;
	public static JenisIdentitasAnggota EMAIL;
	public static JenisIdentitasAnggota NIM;
	public static JenisIdentitasAnggota NIDN;
	public static JenisIdentitasAnggota NIK;
	public static JenisIdentitasAnggota NIS;
	public static TipeAnggota UMUM;
	public static TipeAnggota MAHASISWA;
	public static TipeAnggota DOSEN;
	public static TipeAnggota PEGAWAI;
	public static TipeAnggota SISWA;

	public static KategoriItem HUMANIORA;
	public static KategoriItem PENDIDIKAN;
	public static KategoriItem FIKSI;
	public static KategoriItem NON_FIKSI;
	public static KategoriItem BAHASA_DAN_SASTRA;
	public static KategoriItem AGAMA;
	public static KategoriItem SENI_DAN_BUDAYA;

	public static JenisInformasiPerpustakaan INFORMASI;
	public static JenisInformasiPerpustakaan PENGUMUMAN;
	public static JenisInformasiPerpustakaan PERINGATAN;

	public static TipeItem TEXTBOOK;
	public static JenisItem TEXT;
	private static TipeItem TUGAS_AKHIR;

	static {
		Session session = HibernateUtil.currentSession();

		TEXT = (JenisItem) session.createCriteria(JenisItem.class).add(Restrictions.eq("nama", "Text")).setMaxResults(1)
				.uniqueResult();
		if (TEXT == null) {
			TEXT = new JenisItem();
			TEXT.setKeterangan("Text");
			TEXT.setNama("Text");
			session.save(TEXT);
		}

		TEXTBOOK = (TipeItem) session.createCriteria(TipeItem.class).add(Restrictions.eq("nama", "Textbook"))
				.setMaxResults(1).uniqueResult();
		if (TEXTBOOK == null) {
			TEXTBOOK = new TipeItem();
			TEXTBOOK.setKeterangan("Textbook");
			TEXTBOOK.setNama("Textbook");
			session.save(TEXTBOOK);
		}

		PENGUMUMAN = (JenisInformasiPerpustakaan) session.createCriteria(JenisInformasiPerpustakaan.class)
				.add(Restrictions.eq("nama", "Pengumuman")).setMaxResults(1).uniqueResult();
		if (PENGUMUMAN == null) {
			PENGUMUMAN = new JenisInformasiPerpustakaan();
			PENGUMUMAN.setKeterangan("Pengumuman");
			PENGUMUMAN.setNama("Pengumuman");
			session.save(PENGUMUMAN);
		}

		INFORMASI = (JenisInformasiPerpustakaan) session.createCriteria(JenisInformasiPerpustakaan.class)
				.add(Restrictions.eq("nama", "Informasi")).setMaxResults(1).uniqueResult();
		if (INFORMASI == null) {
			INFORMASI = new JenisInformasiPerpustakaan();
			INFORMASI.setKeterangan("Informasi");
			INFORMASI.setNama("Informasi");
			session.save(INFORMASI);
		}

		PERINGATAN = (JenisInformasiPerpustakaan) session.createCriteria(JenisInformasiPerpustakaan.class)
				.add(Restrictions.eq("nama", "Peringatan")).setMaxResults(1).uniqueResult();
		if (PERINGATAN == null) {
			PERINGATAN = new JenisInformasiPerpustakaan();
			PERINGATAN.setKeterangan("Peringatan");
			PERINGATAN.setNama("Peringatan");
			session.save(PERINGATAN);
		}

		HUMANIORA = (KategoriItem) session.createCriteria(KategoriItem.class).add(Restrictions.eq("nama", "Humaniora"))
				.setMaxResults(1).uniqueResult();
		if (HUMANIORA == null) {
			HUMANIORA = new KategoriItem();
			HUMANIORA.setKeterangan("Humaniora");
			HUMANIORA.setNama("Humaniora");
			session.save(HUMANIORA);
		}

		PENDIDIKAN = (KategoriItem) session.createCriteria(KategoriItem.class)
				.add(Restrictions.eq("nama", "Pendidikan")).setMaxResults(1).uniqueResult();
		if (PENDIDIKAN == null) {
			PENDIDIKAN = new KategoriItem();
			PENDIDIKAN.setKeterangan("Pendidikan");
			PENDIDIKAN.setNama("Pendidikan");
			session.save(PENDIDIKAN);
		}

		FIKSI = (KategoriItem) session.createCriteria(KategoriItem.class).add(Restrictions.eq("nama", "Fiksi"))
				.setMaxResults(1).uniqueResult();
		if (FIKSI == null) {
			FIKSI = new KategoriItem();
			FIKSI.setKeterangan("Fiksi");
			FIKSI.setNama("Fiksi");
			session.save(FIKSI);
		}

		NON_FIKSI = (KategoriItem) session.createCriteria(KategoriItem.class).add(Restrictions.eq("nama", "Non Fiksi"))
				.setMaxResults(1).uniqueResult();
		if (NON_FIKSI == null) {
			NON_FIKSI = new KategoriItem();
			NON_FIKSI.setKeterangan("Non Fiksi");
			NON_FIKSI.setNama("Non Fiksi");
			session.save(NON_FIKSI);
		}

		BAHASA_DAN_SASTRA = (KategoriItem) session.createCriteria(KategoriItem.class)
				.add(Restrictions.eq("nama", "Bahasa dan Sastra")).setMaxResults(1).uniqueResult();
		if (BAHASA_DAN_SASTRA == null) {
			BAHASA_DAN_SASTRA = new KategoriItem();
			BAHASA_DAN_SASTRA.setKeterangan("Bahasa dan Sastra");
			BAHASA_DAN_SASTRA.setNama("Bahasa dan Sastra");
			session.save(BAHASA_DAN_SASTRA);
		}

		AGAMA = (KategoriItem) session.createCriteria(KategoriItem.class).add(Restrictions.eq("nama", "Agama"))
				.setMaxResults(1).uniqueResult();
		if (AGAMA == null) {
			AGAMA = new KategoriItem();
			AGAMA.setKeterangan("Agama");
			AGAMA.setNama("Agama");
			session.save(AGAMA);
		}

		SENI_DAN_BUDAYA = (KategoriItem) session.createCriteria(KategoriItem.class)
				.add(Restrictions.eq("nama", "Seni dan Budaya")).setMaxResults(1).uniqueResult();
		if (SENI_DAN_BUDAYA == null) {
			SENI_DAN_BUDAYA = new KategoriItem();
			SENI_DAN_BUDAYA.setKeterangan("Seni dan Budaya");
			SENI_DAN_BUDAYA.setNama("Seni dan Budaya");
			session.save(SENI_DAN_BUDAYA);
		}

		UMUM = (TipeAnggota) session.createCriteria(TipeAnggota.class).add(Restrictions.eq("nama", "Lain-lain"))
				.setMaxResults(1).uniqueResult();
		if (UMUM == null) {
			UMUM = new TipeAnggota();
			UMUM.setKeterangan("Lain-lain");
			UMUM.setNama("Lain-lain");
			session.save(UMUM);
		}

		MAHASISWA = (TipeAnggota) session.createCriteria(TipeAnggota.class).add(Restrictions.eq("nama", "Mahasiswa"))
				.setMaxResults(1).uniqueResult();
		if (MAHASISWA == null) {
			MAHASISWA = new TipeAnggota();
			MAHASISWA.setKeterangan("Mahasiswa");
			MAHASISWA.setNama("Mahasiswa");
			session.save(MAHASISWA);
		}

		DOSEN = (TipeAnggota) session.createCriteria(TipeAnggota.class).add(Restrictions.eq("nama", "Dosen"))
				.setMaxResults(1).uniqueResult();
		if (DOSEN == null) {
			DOSEN = new TipeAnggota();
			DOSEN.setKeterangan("Dosen");
			DOSEN.setNama("Dosen");
			session.save(DOSEN);
		}

		PEGAWAI = (TipeAnggota) session.createCriteria(TipeAnggota.class).add(Restrictions.eq("nama", "Pegawai"))
				.setMaxResults(1).uniqueResult();
		if (PEGAWAI == null) {
			PEGAWAI = new TipeAnggota();
			PEGAWAI.setKeterangan("Pegawai");
			PEGAWAI.setNama("Pegawai");
			session.save(PEGAWAI);
		}

		SISWA = (TipeAnggota) session.createCriteria(TipeAnggota.class).add(Restrictions.eq("nama", "Siswa"))
				.setMaxResults(1).uniqueResult();
		if (SISWA == null) {
			SISWA = new TipeAnggota();
			SISWA.setKeterangan("Siswa");
			SISWA.setNama("Siswa");
			session.save(SISWA);
		}

		EMAIL = (JenisIdentitasAnggota) session.createCriteria(JenisIdentitasAnggota.class)
				.add(Restrictions.eq("nama", "Email")).setMaxResults(1).uniqueResult();
		if (EMAIL == null) {
			EMAIL = new JenisIdentitasAnggota();
			EMAIL.setKeterangan("Email");
			EMAIL.setNama("Email");
			session.save(EMAIL);
		}

		NIM = (JenisIdentitasAnggota) session.createCriteria(JenisIdentitasAnggota.class)
				.add(Restrictions.eq("nama", "NIM")).setMaxResults(1).uniqueResult();
		if (NIM == null) {
			NIM = new JenisIdentitasAnggota();
			NIM.setKeterangan("NIM");
			NIM.setNama("NIM");
			session.save(NIM);
		}

		NIS = (JenisIdentitasAnggota) session.createCriteria(JenisIdentitasAnggota.class)
				.add(Restrictions.eq("nama", "NIS")).setMaxResults(1).uniqueResult();
		if (NIS == null) {
			NIS = new JenisIdentitasAnggota();
			NIS.setKeterangan("NIS");
			NIS.setNama("NIS");
			session.save(NIS);
		}

		NIDN = (JenisIdentitasAnggota) session.createCriteria(JenisIdentitasAnggota.class)
				.add(Restrictions.eq("nama", "NIDN")).setMaxResults(1).uniqueResult();
		if (NIDN == null) {
			NIDN = new JenisIdentitasAnggota();
			NIDN.setKeterangan("NIDN");
			NIDN.setNama("NIDN");
			session.save(NIDN);
		}

		NIK = (JenisIdentitasAnggota) session.createCriteria(JenisIdentitasAnggota.class)
				.add(Restrictions.eq("nama", "NIK")).setMaxResults(1).uniqueResult();
		if (NIK == null) {
			NIK = new JenisIdentitasAnggota();
			NIK.setKeterangan("NIK");
			NIK.setNama("NIK");
			session.save(NIK);
		}

		ANGGOTA_REGULER = (JenisAnggota) session.createCriteria(JenisAnggota.class)
				.add(Restrictions.eq("nama", "Anggota Reguler")).setMaxResults(1).uniqueResult();
		if (ANGGOTA_REGULER == null) {
			ANGGOTA_REGULER = new JenisAnggota();
			ANGGOTA_REGULER.setKeterangan("Anggota Reguler");
			ANGGOTA_REGULER.setNama("Anggota Reguler");
			session.save(ANGGOTA_REGULER);
		}

		DRAFT = (StatusTerbitItem) session.createCriteria(StatusTerbitItem.class).add(Restrictions.eq("nama", "Draft"))
				.setMaxResults(1).uniqueResult();
		if (DRAFT == null) {
			DRAFT = new StatusTerbitItem();
			DRAFT.setKeterangan("Draft");
			DRAFT.setNama("Draft");
			session.save(DRAFT);
		}

		APPROVE = (StatusTerbitItem) session.createCriteria(StatusTerbitItem.class)
				.add(Restrictions.eq("nama", "Disetujui")).setMaxResults(1).uniqueResult();
		if (APPROVE == null) {
			APPROVE = new StatusTerbitItem();
			APPROVE.setKeterangan("Disetujui");
			APPROVE.setNama("Disetujui");
			session.save(APPROVE);
		}

		REJECT = (StatusTerbitItem) session.createCriteria(StatusTerbitItem.class)
				.add(Restrictions.eq("nama", "Ditolak")).setMaxResults(1).uniqueResult();
		if (REJECT == null) {
			REJECT = new StatusTerbitItem();
			REJECT.setKeterangan("Ditolak");
			REJECT.setNama("Ditolak");
			session.save(REJECT);
		}

		PUBLISH = (StatusTerbitItem) session.createCriteria(StatusTerbitItem.class)
				.add(Restrictions.eq("nama", "Terbit")).setMaxResults(1).uniqueResult();
		if (PUBLISH == null) {
			PUBLISH = new StatusTerbitItem();
			PUBLISH.setKeterangan("Terbit");
			PUBLISH.setNama("Terbit");
			session.save(PUBLISH);
		}

		KARYA_ILMIAH = (TipeItem) session.createCriteria(TipeItem.class).add(Restrictions.eq("nama", "Karya Ilmiah"))
				.setMaxResults(1).uniqueResult();
		if (KARYA_ILMIAH == null) {
			KARYA_ILMIAH = new TipeItem();
			KARYA_ILMIAH.setKeterangan("Karya Ilmiah");
			KARYA_ILMIAH.setNama("Karya Ilmiah");
			// KARYA_ILMIAH.setDefaultItem(true);
			session.save(KARYA_ILMIAH);
		}

		DISERTASI = (TipeItem) session.createCriteria(TipeItem.class).add(Restrictions.eq("nama", "Disertasi"))
				.setMaxResults(1).uniqueResult();
		if (DISERTASI == null) {
			DISERTASI = new TipeItem();
			DISERTASI.setKeterangan("Disertasi");
			DISERTASI.setNama("Disertasi");
			session.save(DISERTASI);
		}

		THESIS = (TipeItem) session.createCriteria(TipeItem.class).add(Restrictions.eq("nama", "Thesis"))
				.setMaxResults(1).uniqueResult();
		if (THESIS == null) {
			THESIS = new TipeItem();
			THESIS.setKeterangan("Thesis");
			THESIS.setNama("Thesis");
			session.save(THESIS);
		}

		SKRIPSI = (TipeItem) session.createCriteria(TipeItem.class).add(Restrictions.eq("nama", "Skripsi"))
				.setMaxResults(1).uniqueResult();
		if (SKRIPSI == null) {
			SKRIPSI = new TipeItem();
			SKRIPSI.setKeterangan("Skripsi");
			SKRIPSI.setNama("Skripsi");
			session.save(SKRIPSI);
		}

		TUGAS_AKHIR = (TipeItem) session.createCriteria(TipeItem.class).add(Restrictions.eq("nama", "Tugas Akhir"))
				.setMaxResults(1).uniqueResult();
		if (TUGAS_AKHIR == null) {
			TUGAS_AKHIR = new TipeItem();
			TUGAS_AKHIR.setKeterangan("Tugas Akhir");
			TUGAS_AKHIR.setNama("Tugas Akhir");
			session.save(TUGAS_AKHIR);
		}

		AKTIF = (StatusItem) session.createCriteria(StatusItem.class).add(Restrictions.eq("nama", "Aktif"))
				.setMaxResults(1).uniqueResult();
		if (AKTIF == null) {
			AKTIF = new StatusItem();
			AKTIF.setKeterangan("Aktif");
			AKTIF.setNama("Aktif");
			session.save(AKTIF);
		}

		SALDO_AWAL = (KodeTransaksi) session.createCriteria(KodeTransaksi.class).add(Restrictions.eq("kode", "SA"))
				.setMaxResults(1).uniqueResult();
		if (SALDO_AWAL == null) {
			SALDO_AWAL = new KodeTransaksi();
			SALDO_AWAL.setKode("SA");
			SALDO_AWAL.setJenis(KodeTransaksi.PENAMBAHAN);
			SALDO_AWAL.setKeterangan("Saldo Awal");
			SALDO_AWAL.setNama("Saldo Awal");
			//
			session.save(SALDO_AWAL);
			//
		}

		BELI_MASUK = (KodeTransaksi) session.createCriteria(KodeTransaksi.class).add(Restrictions.eq("kode", "BM"))
				.setMaxResults(1).uniqueResult();
		if (BELI_MASUK == null) {
			BELI_MASUK = new KodeTransaksi();
			BELI_MASUK.setKode("BM");
			BELI_MASUK.setJenis(KodeTransaksi.PENAMBAHAN);
			BELI_MASUK.setKeterangan("Beli Masuk");
			BELI_MASUK.setNama("Beli Masuk");

			session.save(BELI_MASUK);

		}

		HIBAH_MASUK = (KodeTransaksi) session.createCriteria(KodeTransaksi.class).add(Restrictions.eq("kode", "HM"))
				.setMaxResults(1).uniqueResult();
		if (HIBAH_MASUK == null) {
			HIBAH_MASUK = new KodeTransaksi();
			HIBAH_MASUK.setKode("HM");
			HIBAH_MASUK.setJenis(KodeTransaksi.PENAMBAHAN);
			HIBAH_MASUK.setKeterangan("Hibah Masuk");
			HIBAH_MASUK.setNama("Hibah Masuk");

			session.save(HIBAH_MASUK);

		}

		MASUK_LAIN = (KodeTransaksi) session.createCriteria(KodeTransaksi.class).add(Restrictions.eq("kode", "ML"))
				.setMaxResults(1).uniqueResult();
		if (MASUK_LAIN == null) {
			MASUK_LAIN = new KodeTransaksi();
			MASUK_LAIN.setKode("ML");
			MASUK_LAIN.setJenis(KodeTransaksi.PENAMBAHAN);
			MASUK_LAIN.setKeterangan("Masuk Lain");
			MASUK_LAIN.setNama("Masuk Lain");

			session.save(MASUK_LAIN);

		}

		RETUR_BELI = (KodeTransaksi) session.createCriteria(KodeTransaksi.class).add(Restrictions.eq("kode", "RB"))
				.setMaxResults(1).uniqueResult();
		if (RETUR_BELI == null) {
			RETUR_BELI = new KodeTransaksi();
			RETUR_BELI.setKode("RB");
			RETUR_BELI.setJenis(KodeTransaksi.PENGURANGAN);
			RETUR_BELI.setKeterangan("Retur Beli");
			RETUR_BELI.setNama("Retur Beli");

			session.save(RETUR_BELI);

		}

		PINJAM_KELUAR = (KodeTransaksi) session.createCriteria(KodeTransaksi.class).add(Restrictions.eq("kode", "PK"))
				.setMaxResults(1).uniqueResult();
		if (PINJAM_KELUAR == null) {
			PINJAM_KELUAR = new KodeTransaksi();
			PINJAM_KELUAR.setKode("PK");
			PINJAM_KELUAR.setJenis(KodeTransaksi.PENGURANGAN);
			PINJAM_KELUAR.setKeterangan("Pinjam Keluar");
			PINJAM_KELUAR.setNama("Pinjam Keluar");

			session.save(PINJAM_KELUAR);

		}

		PENGEMBALIAN_MASUK = (KodeTransaksi) session.createCriteria(KodeTransaksi.class)
				.add(Restrictions.eq("kode", "PM")).setMaxResults(1).uniqueResult();
		if (PENGEMBALIAN_MASUK == null) {
			PENGEMBALIAN_MASUK = new KodeTransaksi();
			PENGEMBALIAN_MASUK.setKode("PM");
			PENGEMBALIAN_MASUK.setJenis(KodeTransaksi.PENAMBAHAN);
			PENGEMBALIAN_MASUK.setKeterangan("Pengembalian Masuk");
			PENGEMBALIAN_MASUK.setNama("Pengembalian Masuk");

			session.save(PENGEMBALIAN_MASUK);

		}

		HILANG = (KodeTransaksi) session.createCriteria(KodeTransaksi.class).add(Restrictions.eq("kode", "HL"))
				.setMaxResults(1).uniqueResult();
		if (HILANG == null) {
			HILANG = new KodeTransaksi();
			HILANG.setKode("HL");
			HILANG.setJenis(KodeTransaksi.PENGURANGAN);
			HILANG.setKeterangan("Hilang");
			HILANG.setNama("Hilang");

			session.save(HILANG);

		}

		KELUAR_LAIN = (KodeTransaksi) session.createCriteria(KodeTransaksi.class).add(Restrictions.eq("kode", "HL"))
				.setMaxResults(1).uniqueResult();
		if (KELUAR_LAIN == null) {
			KELUAR_LAIN = new KodeTransaksi();
			KELUAR_LAIN.setKode("HL");
			KELUAR_LAIN.setJenis(KodeTransaksi.PENGURANGAN);
			KELUAR_LAIN.setKeterangan("Keluar Lain");
			KELUAR_LAIN.setNama("Keluar Lain");

			session.save(KELUAR_LAIN);

		}

		PEMAKAIAN = (KodeTransaksi) session.createCriteria(KodeTransaksi.class).add(Restrictions.eq("kode", "PEM"))
				.setMaxResults(1).uniqueResult();
		if (PEMAKAIAN == null) {
			PEMAKAIAN = new KodeTransaksi();
			PEMAKAIAN.setKode("PEM");
			PEMAKAIAN.setJenis(KodeTransaksi.PENGURANGAN);
			PEMAKAIAN.setKeterangan("Pemakaian");
			PEMAKAIAN.setNama("Pemakaian");

			session.save(PEMAKAIAN);

		}

		TRANSFER = (KodeTransaksi) session.createCriteria(KodeTransaksi.class).add(Restrictions.eq("kode", "TRF"))
				.setMaxResults(1).uniqueResult();
		if (TRANSFER == null) {
			TRANSFER = new KodeTransaksi();
			TRANSFER.setKode("TRF");
			TRANSFER.setJenis(KodeTransaksi.PENGURANGAN);
			TRANSFER.setKeterangan("Transfer");
			TRANSFER.setNama("Transfer");
			session.save(TRANSFER);

		}

		TERIMA = (KodeTransaksi) session.createCriteria(KodeTransaksi.class).add(Restrictions.eq("kode", "TRM"))
				.setMaxResults(1).uniqueResult();
		if (TERIMA == null) {
			TERIMA = new KodeTransaksi();
			TERIMA.setKode("TRM");
			TERIMA.setJenis(KodeTransaksi.PENAMBAHAN);
			TERIMA.setKeterangan("Terima");
			TERIMA.setNama("Terima");

			session.save(TERIMA);

		}

		adjustmentPenambahan = (KodeTransaksi) session.createCriteria(KodeTransaksi.class)
				.add(Restrictions.eq("kode", "ADT")).setMaxResults(1).uniqueResult();

		if (adjustmentPenambahan == null) {
			adjustmentPenambahan = new KodeTransaksi();
			adjustmentPenambahan.setKode("ADT");
			adjustmentPenambahan.setJenis(KodeTransaksi.PENAMBAHAN);
			adjustmentPenambahan.setKeterangan("Koreksi Penambahan");
			adjustmentPenambahan.setNama("Koreksi Penambahan");
			session.save(adjustmentPenambahan);
		}

		adjustmentPengurangan = (KodeTransaksi) session.createCriteria(KodeTransaksi.class)
				.add(Restrictions.eq("kode", "ADK")).setMaxResults(1).uniqueResult();

		if (adjustmentPengurangan == null) {
			adjustmentPengurangan = new KodeTransaksi();
			adjustmentPengurangan.setKode("ADK");
			adjustmentPengurangan.setJenis(KodeTransaksi.PENGURANGAN);
			adjustmentPengurangan.setKeterangan("Koreksi Pengurangan");
			adjustmentPengurangan.setNama("Koreksi Pengurangan");
			session.save(adjustmentPengurangan);
		}

	}

	public static Long generateMaxByPerpustakaan(Class<?> class1, Perpustakaan perpustakaan) {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			String strmax = (String) session.createCriteria(class1)
					.add(perpustakaan == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("perpustakaan", perpustakaan))
					.setProjection(Projections.max("kode")).uniqueResult();
			Long max = parseKodeUrut(strmax);
			if (max != null) {
				return max;
			}

			Number n = (Number) session.createCriteria(class1)
					.add(perpustakaan == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("perpustakaan", perpustakaan))
					.setProjection(Projections.rowCount()).uniqueResult();
			return Long.valueOf(n == null ? 0L : n.longValue());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return Long.valueOf(0L);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static Long generateMaxByPerpustakaan(Class<?> class1) {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			String strmax = (String) session.createCriteria(class1).setProjection(Projections.max("kode")).uniqueResult();
			Long max = parseKodeUrut(strmax);
			if (max != null) {
				return max;
			}

			Number n = (Number) session.createCriteria(class1).setProjection(Projections.rowCount()).uniqueResult();
			return Long.valueOf(n == null ? 0L : n.longValue());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return Long.valueOf(0L);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static String generateCode(Class<?> class1, int panjang, String awalan,
			Perpustakaan perpustakaan) {
		return generateCode(class1, panjang, awalan, perpustakaan, 1L);
	}

	public static String generateCode(Class<?> class1, int panjang, String awalan,
			Perpustakaan perpustakaan, Long penambahan) {
		Long max = null;
		if (perpustakaan == null) {
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				max = (Long) session.createCriteria(class1).setProjection(Projections.max("id"))
						.uniqueResult();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				HibernateUtil.closeSessionQuietly(session);
			}
		} else {
			max = generateMaxByPerpustakaan(class1, perpustakaan);
		}
		if (max == null) {
			max = Long.valueOf(0L);
		}
		long tambahan = penambahan == null ? 1L : penambahan.longValue();
		for (int i = 0; i < 100000; i++) {
			String mykode = buildKode(panjang, awalan, perpustakaan, max.longValue() + tambahan + i);
			if (countKode(class1, mykode) == 0) {
				return mykode;
			}
		}
		return buildKode(panjang, awalan, perpustakaan, max.longValue() + tambahan + 100000L);
	}

	public static String generateCode(Class<?> class1, int panjang, String awalan) {
		return generateCode(class1, panjang, awalan, 1L);
	}

	public static String generateCode(Class<?> class1, int panjang, String awalan, Long penambahan) {
		Long max = null;
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			max = (Long) session.createCriteria(class1).setProjection(Projections.max("id"))
					.uniqueResult();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}

		if (max == null) {
			max = Long.valueOf(0L);
		}
		long tambahan = penambahan == null ? 1L : penambahan.longValue();
		for (int i = 0; i < 100000; i++) {
			String mykode = buildKode(panjang, awalan, null, max.longValue() + tambahan + i);
			if (countKode(class1, mykode) == 0) {
				return mykode;
			}
		}
		return buildKode(panjang, awalan, null, max.longValue() + tambahan + 100000L);
	}

	private static Long parseKodeUrut(String strmax) {
		if (strmax == null || strmax.trim().length() == 0) {
			return Long.valueOf(0L);
		}
		String str = strmax.trim();
		if (str.indexOf('-') >= 0) {
			String[] strSplit = str.split("-");
			str = strSplit.length > 0 ? strSplit[strSplit.length - 1] : "0";
		}
		str = str == null ? "" : str.trim();
		if (str.length() == 0) {
			return Long.valueOf(0L);
		}
		for (int i = 0; i < str.length(); i++) {
			if (!Character.isDigit(str.charAt(i))) {
				return null;
			}
		}
		try {
			return Long.valueOf(Long.parseLong(str));
		} catch (Exception e) {
			return null;
		}
	}

	private static String buildKode(int panjang, String awalan, Perpustakaan perpustakaan, long nomor) {
		String mykode = "00000000000000000000000000000" + nomor;
		String nomorAkhir = mykode.substring(mykode.length() - panjang, mykode.length());
		return (awalan == null || awalan.trim().equals("") ? "" : awalan + "-")
				+ (perpustakaan == null || perpustakaan.getKode() == null || perpustakaan.getKode().trim().equals("")
						? ""
						: perpustakaan.getKode() + "-")
				+ nomorAkhir;
	}

	private static int countKode(Class<?> class1, String mykode) {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Number count = (Number) session.createCriteria(class1).add(Restrictions.eq("kode", mykode))
					.setProjection(Projections.rowCount()).uniqueResult();
			return count == null ? 0 : count.intValue();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 0;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void convertLampiranToText(FotoItem fotoItem) {
		if (fotoItem.getNama().toLowerCase().endsWith("pdf")) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

			try {
				Blob blob = (Blob) streamingSession.createCriteria(FotoItem.class)
						.add(Restrictions.idEq(fotoItem.getId())).setProjection(Projections.property("foto"))
						.uniqueResult();

				PDFParser parser = null;
				try {
					parser = new PDFParser(blob.getBinaryStream());
				} catch (Exception e) {
					System.out.println("Unable to open PDF Parser.");
				}

				if (parser != null) {
					COSDocument cosDoc = null;
					PDFTextStripper pdfStripper = null;
					PDDocument pdDoc = null;
					String parsedText = "";
					try {
						parser.parse();
						cosDoc = parser.getDocument();
						pdfStripper = new PDFTextStripper();
						pdDoc = new PDDocument(cosDoc);
						parsedText = pdfStripper.getText(pdDoc);
					} catch (Exception e) {
						System.out.println("An exception occured in parsing the PDF Document.");
						Common.tampilErrorJikaAdmin(e);
						try {
							if (cosDoc != null)
								cosDoc.close();
							if (pdDoc != null)
								pdDoc.close();
						} catch (Exception e1) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
					fotoItem = (FotoItem) streamingSession.createCriteria(FotoItem.class)
							.add(Restrictions.idEq(fotoItem.getId())).uniqueResult();
					parsedText = parsedText.replaceAll("\n", " ");
					fotoItem.setContent(parsedText);
					streamingSession.getTransaction().begin();
					streamingSession.update(fotoItem);
					streamingSession.getTransaction().commit();
				}
				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				StreamingHibernateUtil.getInstance().rollbackTransaction();
			}
		}
	}

	public static void convertLampiranToText(FotoImagePerHalamanItem fotoImagePerHalamanItem) {
		if (fotoImagePerHalamanItem.getNama().toLowerCase().endsWith("pdf")) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

			try {
				Blob blob = (Blob) streamingSession.createCriteria(FotoImagePerHalamanItem.class)
						.add(Restrictions.idEq(fotoImagePerHalamanItem.getId()))
						.setProjection(Projections.property("foto")).uniqueResult();

				PDFParser parser = null;
				try {
					parser = new PDFParser(blob.getBinaryStream());
				} catch (Exception e) {
					System.out.println("Unable to open PDF Parser.");
				}

				if (parser != null) {
					COSDocument cosDoc = null;
					PDFTextStripper pdfStripper = null;
					PDDocument pdDoc = null;
					String parsedText = "";
					try {
						parser.parse();
						cosDoc = parser.getDocument();
						pdfStripper = new PDFTextStripper();
						pdDoc = new PDDocument(cosDoc);
						parsedText = pdfStripper.getText(pdDoc);
					} catch (Exception e) {
						System.out.println("An exception occured in parsing the PDF Document.");
						Common.tampilErrorJikaAdmin(e);
						try {
							if (cosDoc != null)
								cosDoc.close();
							if (pdDoc != null)
								pdDoc.close();
						} catch (Exception e1) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
					fotoImagePerHalamanItem = (FotoImagePerHalamanItem) streamingSession
							.createCriteria(FotoImagePerHalamanItem.class)
							.add(Restrictions.idEq(fotoImagePerHalamanItem.getId())).uniqueResult();
					parsedText = parsedText.replaceAll("\n", " ");
					fotoImagePerHalamanItem.setContent(parsedText);
					streamingSession.getTransaction().begin();
					streamingSession.update(fotoImagePerHalamanItem);
					streamingSession.getTransaction().commit();
				}
				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				StreamingHibernateUtil.getInstance().rollbackTransaction();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void checkDirectory() {

		SatuanKerja satuanKerja = Common.getCurrentUser() == null ? null : Common.getCurrentUser().ambilSatuanKerja();
		Perpustakaan currentPerpustakaan = Common.getCurrentPerpustakaan();
		if (satuanKerja == null && currentPerpustakaan != null) {
			satuanKerja = currentPerpustakaan.getSatuanKerja();
		}

		if (satuanKerja == null) {
			return;
		}

		Session session = HibernateUtil.currentSession();
		Item parent = (Item) session.createCriteria(Item.class).add(Restrictions.eq("folder", true))
				.add(Restrictions.eq("defaultSatuanKerja", satuanKerja))
				.add(Restrictions.eq("tipeItem", LibraryUtil.KARYA_ILMIAH)).add(Restrictions.isNull("parent"))
				.addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();
		if (parent == null) {
			parent = new Item();
			parent.setUrutan(0);
			parent.setFolder(true);
			parent.setNama(satuanKerja.getNama());
			parent.setKeterangan(satuanKerja.getNama());
			parent.setDefaultSatuanKerja(satuanKerja);
			parent.setTipeItem(LibraryUtil.KARYA_ILMIAH);
			session.save(parent);
		}

		List<DomainPenelitian> domainPenelitians = session.createCriteria(DomainPenelitian.class)
				.createAlias("penerbit", "penerbit").add(Restrictions.eq("penerbit.satuanKerja", satuanKerja)).list();

		for (DomainPenelitian domainPenelitian : domainPenelitians) {
			Item myitem = (Item) session.createCriteria(Item.class).add(Restrictions.eq("folder", true))
					.add(Restrictions.eq("parent", parent)).add(Restrictions.eq("defaultSatuanKerja", satuanKerja))
					.add(Restrictions.eq("tipeItem", LibraryUtil.KARYA_ILMIAH))
					.add(Restrictions.eq("domainPenelitian", domainPenelitian)).addOrder(Order.asc("id"))
					.setMaxResults(1).uniqueResult();
			if (myitem == null) {
				myitem = new Item();
				myitem.setParent(parent);
				myitem.setDomainPenelitian(domainPenelitian);
				myitem.setUrutan(0);
				myitem.setFolder(true);
				myitem.setNama(domainPenelitian.getNama());
				myitem.setKeterangan(domainPenelitian.getNama());
				myitem.setDefaultSatuanKerja(satuanKerja);
				myitem.setTipeItem(LibraryUtil.KARYA_ILMIAH);
				session.save(myitem);
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	public static String tersediaDi(Item item) {
		String sql = "select max(d.nama) as perpustakaan,count(*) as stok from library.item_punya_barcode a "
				+ "left join library.item c on (a.item = c.id) "
				+ "left join library.perpustakaan d on (a.perpustakaan = d.id) " + " where c.id=" + item.getId() + ""
				+ " group by a.perpustakaan having count(*) > 0 ";
		List<Object[]> data = new ArrayList<Object[]>();
		try {
			Session session = HibernateUtil.currentSession();
			data = session.createSQLQuery(sql).list();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		String hasil = "<ol style='font-size:8px;'>";
		for (Object[] o : data) {
			hasil += "<li>" + (o[0] + " -> " + o[1] + " buah") + "</li>";
		}
		return hasil + "</ol>";
	}

	public static File laporanHTML(String isbn, String title) throws Exception {
		File myFile = new File(Sessions.getCurrent().getWebApp().getRealPath("/"));
		myFile.getParentFile().mkdirs();
		myFile = new File(myFile + "/share/baca_" + isbn + ".html");
		myFile.getParentFile().mkdirs();
		if (myFile.exists()) {
			myFile.delete();
			myFile.createNewFile();
		}

		String html = "<!DOCTYPE html \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
				+ "  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\"> "
				+ "<html xmlns=\"http://www.w3.org/1999/xhtml\"> " + "  <head> "
				+ "    <meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\"/> "
				+ "    <title>Google Books Embedded Viewer API Example</title> "
				+ "    <script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script> "
				+ "    <script type=\"text/javascript\"> " + "      google.load(\"books\", \"0\"); "
				+ "      function initialize() { "
				+ "        var viewer = new google.books.DefaultViewer(document.getElementById('viewerCanvas')); "
				+ "        viewer.load('ISBN:" + isbn + "'); " + "      } "
				+ "      google.setOnLoadCallback(initialize); " + "    </script> " + "  </head> "
				+ "  <body style='overflow: hidden;' align='center'> "
				+ "    <div id=\"viewerCanvas\" style=\"width: 1330px; height: 640px\"></div> " + "  </body> "
				+ "</html>";

		myFile.getParentFile().mkdirs();
		FileWriter fileWriter = new FileWriter(myFile);
		fileWriter.write(html);
		fileWriter.close();

		System.out.println(myFile);

		MyWindow addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
		addWindow.setHeight("670px");
		addWindow.setWidth("1350px");

		Iframe iframe = new Iframe("/share/" + myFile.getName());
		iframe.setHeight("95%");
		iframe.setWidth("80%");
		addWindow.appendChild(iframe);

		addWindow.setClosable(true);
		addWindow.setTitle(title);
		addWindow.setVisible(true);
		addWindow.onModal();

		return myFile;
	}

	public static Integer[] getKuota(PeminjamanPengadaanItem peminjamanPengadaanItem) {
		int jumlahMaksimalPeminjaman = LibraryUtil.getJumlahMaksimalPeminjaman(peminjamanPengadaanItem);
		if (peminjamanPengadaanItem == null || peminjamanPengadaanItem.getAnggota() == null
				|| peminjamanPengadaanItem.getAnggota().getId() == null
				|| peminjamanPengadaanItem.getPerpustakaan() == null
				|| peminjamanPengadaanItem.getPerpustakaan().getId() == null) {
			return new Integer[] { jumlahMaksimalPeminjaman, 0, 0 };
		}

		Long anggotaId = peminjamanPengadaanItem.getAnggota().getId();
		Long perpustakaanId = peminjamanPengadaanItem.getPerpustakaan().getId();
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			int totalPeminjaman = ((Number) session.createCriteria(PeminjamanPengadaanItemDetail.class)
					.createAlias("peminjamanPengadaanItem", "peminjamanPengadaanItem")
					.createAlias("peminjamanPengadaanItem.anggota", "anggota")
					.createAlias("peminjamanPengadaanItem.perpustakaan", "perpustakaan")
					.add(Restrictions.eq("anggota.id", anggotaId))
					.add(Restrictions.eq("perpustakaan.id", perpustakaanId))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			int totalKembali = ((Number) session.createCriteria(KembaliPengadaanItemDetail.class)
					.createAlias("peminjamanPengadaanItemDetail", "peminjamanPengadaanItemDetail")
					.createAlias("peminjamanPengadaanItemDetail.peminjamanPengadaanItem", "peminjamanPengadaanItem")
					.createAlias("peminjamanPengadaanItem.anggota", "anggota")
					.createAlias("peminjamanPengadaanItem.perpustakaan", "perpustakaan")
					.add(Restrictions.eq("anggota.id", anggotaId))
					.add(Restrictions.eq("perpustakaan.id", perpustakaanId))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			return new Integer[] { jumlahMaksimalPeminjaman, totalPeminjaman, totalKembali };
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return new Integer[] { jumlahMaksimalPeminjaman, 0, 0 };
		} finally {
			if (session != null) {
				try {
					session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/util/LibraryUtil.java:1063");
				}
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/util/LibraryUtil.java:1067");
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/util/LibraryUtil.java:1071");
				}
			}
		}
	}

	public static Image generateImage(Item item) throws Exception {
		String url = generateImageString(item);
		return url == null || url.trim().length() == 0 ? new Image() : new Image(url);
	}

	public static String generateImageString(Item item) throws Exception {
		if (item == null) {
			return "";
		}

		String imageUrl = item.getImageUrl();
		if (imageUrl != null && imageUrl.trim().length() > 0 && imageUrl.trim().startsWith("http")) {
			return imageUrl.trim();
		}

		/*
		 * Cover item yang di-upload lewat ItemPunyaGambarFotoHelper disimpan pada
		 * FotoGambarItem dengan kolom item berisi id item. Ambil gambar pertama
		 * yang di-upload (id ASC) agar cover utama stabil dan tidak acak.
		 */
		FotoGambarItem fotoGambarItem = ambilFotoGambarItemPertama(item);
		if (fotoGambarItem != null && fotoGambarItem.getId() != null) {
			return CommonMedia.getUrlFotoItem(fotoGambarItem.getId(), fotoGambarItem.getItem(), 152, 114);
		}

		if (item.getId() == null) {
			return "";
		}

		return Common.getRequestHostWithProtocol() + "/AmbilMedia?id=" + item.getId()
				+ "&name=nama&foto=foto&clazz=ais.database.model.file.FotoGambarItem&property=item&height=152&width=114";
	}

	private static FotoGambarItem ambilFotoGambarItemPertama(Item item) {
		if (item == null || item.getId() == null) {
			return null;
		}
		Session session = null;
		try {
			session = StreamingHibernateUtil.getInstance().currentSession();
			return (FotoGambarItem) session.createCriteria(FotoGambarItem.class)
					.add(Restrictions.eq("item", item.getId())).addOrder(Order.asc("id")).setMaxResults(1)
					.uniqueResult();
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/library/util/LibraryUtil.java:1123");
			}
			return null;
		} finally {
			try {
				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/util/LibraryUtil.java:1129");
			}
		}
	}

	public static Image generateImage(final ItemTemporary item) throws Exception {
		Image image = new Image();
		if (item.getImageUrl() != null && !item.getImageUrl().trim().isEmpty()
				&& item.getImageUrl().trim().startsWith("http")) {
			image = new Image(item.getImageUrl());
		} else if (item.getImageUrl() == null || item.getImageUrl().trim().equals("")) {

			image = new Image(Common.getRequestHostWithProtocol() + "/AmbilMedia?id=" + item.getId()
					+ "&name=nama&foto=foto&clazz=ais.database.model.file.FotoGambarItem&property=item&height=152&width=114");
		}
		return image;
	}

	public static Iframe tampilkanBacaFrame(String isbn, String title, WebApp application) throws Exception {
		File myFile = new File(application.getRealPath("/"));
		myFile.getParentFile().mkdirs();
		myFile = new File(myFile + "/share/baca_" + isbn + ".html");
		myFile.getParentFile().mkdirs();
		if (myFile.exists()) {
			myFile.delete();
			myFile.createNewFile();
		}

		String myisbn = "ISBN:" + isbn;
		if (isbn != null && (isbn.trim().startsWith("OCLC") || isbn.trim().startsWith("LCCN"))) {
			myisbn = isbn;
		}

		String html = "<!DOCTYPE html \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
				+ "  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\"> "
				+ "<html xmlns=\"http://www.w3.org/1999/xhtml\"> " + "  <head> "
				+ "    <meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\"/> "
				+ "    <title>Google Books Embedded Viewer API Example</title> "
				+ "    <script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script> "
				+ "    <script type=\"text/javascript\"> " + "      google.load(\"books\", \"0\"); "
				+ "      function initialize() { "
				+ "        var viewer = new google.books.DefaultViewer(document.getElementById('viewerCanvas')); "
				+ "        viewer.load('" + myisbn + "'); " + "      } "
				+ "      google.setOnLoadCallback(initialize); " + "    </script> " + "  </head> "
				+ "  <body align='center'> "
				+ "    <div id=\"viewerCanvas\" style=\"min-width: 260px;width: 100%; min-height: 430px;height:100%;\"></div> "
				+ "  </body> " + "</html>";

		myFile.getParentFile().mkdirs();
		FileWriter fileWriter = new FileWriter(myFile);
		fileWriter.write(html);
		fileWriter.close();

		System.out.println(myFile);

		Iframe iframe = new Iframe("/share/" + myFile.getName());
		iframe.setHeight("430px");
		iframe.setWidth("100%");
		iframe.setScrolling("no");

		return iframe;
	}

	public static void checkRef(final Item item) {

		if (Common.bolehKonfigurasi("terintegrasi_dengan_google_book_baru", Konfigurasi.TIDAK_AKTIF)) {
			if (!item.getGoogleBookChecked()) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							GoogleBookSynchronized.process(item);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/util/LibraryUtil.java:1202");
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
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/util/LibraryUtil.java:1215");
						}
					}
				}).start();
			}
		}
	}

	// public static Double hitungDendaItem(
	// KembaliPengadaanItemDetail kembaliPengadaanItemDetail,
	// Date tanggalDikembalikan) {
	// PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail =
	// kembaliPengadaanItemDetail
	// .getPeminjamanPengadaanItemDetail();
	// return hitungDendaItem(peminjamanPengadaanItemDetail,
	// tanggalDikembalikan);
	// }

	public static DendaKeterlambatanItem hitungDendaItem(PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail) {
		return hitungDendaItem(peminjamanPengadaanItemDetail, ais.ui.util.WaktuUtil.getDate());
	}

	public static DendaKeterlambatanItem hitungDendaItem(PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail,
			Date tanggal) {

		// if (peminjamanPengadaanItemDetail.getTanggalKembali() == null) {
		// return 0.0;
		// }

		PeminjamanPengadaanItem peminjamanPengadaanItem = peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem();
		Session session = HibernateUtil.currentNativeSession();

		DendaKeterlambatanItem dendaPerItem = (DendaKeterlambatanItem)

		ConstantValues.simpleObject(

				session.createCriteria(DendaKeterlambatanItem.class)
						.add(Restrictions.or(
								Restrictions.eq("jenisAnggota", peminjamanPengadaanItem.getAnggota().getJenisAnggota()),
								Restrictions.isNull("jenisAnggota")))
						.add(Restrictions.or(
								Restrictions.eq("tipeAnggota", peminjamanPengadaanItem.getAnggota().getTipeAnggota()),
								Restrictions.isNull("tipeAnggota")))
						.add(Restrictions.or(Restrictions.eq("fakultas",
								peminjamanPengadaanItem.getAnggota().getMahasiswa() != null
										? peminjamanPengadaanItem.getAnggota().getMahasiswa().getJurusan().getFakultas()
										: (peminjamanPengadaanItem.getAnggota().getDosen() != null
												? peminjamanPengadaanItem.getAnggota().getDosen().getFakultas()
												: null)),
								Restrictions.isNull("fakultas")))

						.add(Restrictions.or(Restrictions.eq("jurusan",
								peminjamanPengadaanItem.getAnggota().getMahasiswa() != null
										? peminjamanPengadaanItem.getAnggota().getMahasiswa().getJurusan()
										: (peminjamanPengadaanItem.getAnggota().getDosen() != null
												? peminjamanPengadaanItem.getAnggota().getDosen().getJurusan()
												: null)),
								Restrictions.isNull("jurusan")))
						.add(Restrictions.eq("perpustakaan", peminjamanPengadaanItem.getPerpustakaan()))
						.add(Restrictions.sqlRestriction(
								"mulaiberlaku <= date('" + Common.databaseDateFormat.get().format(tanggal) + "')"))

//				.setProjection(Projections.property("denda"))

						.add(Restrictions.eq("dendaPerItem", true))
						.add(Restrictions.sqlRestriction(
								"(jumlah_hari) <= " + peminjamanPengadaanItemDetail.getJumlahHariTerlambat()))
						.addOrder(Order.desc("jumlahHari")).addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1),
				DendaKeterlambatanItem.class);

//		Double denda = dendaPerItem == null ? 0.0 : dendaPerItem;
//		denda = denda * peminjamanPengadaanItemDetail.getJumlah();

//		System.out.println("terlambat => " + peminjamanPengadaanItemDetail.getJumlahHariTerlambat()
//				+ ", peminjamanPengadaanItemDetail = " + peminjamanPengadaanItemDetail.getItem() + ", denda = "
//				+ dendaPerItem);
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
		return dendaPerItem;
	}

	public static Double hitungDendaPerItem(KembaliPengadaanItem kembaliPengadaanItem) {

		Session session = HibernateUtil.currentNativeSession();

		Number totalDenda = kembaliPengadaanItem == null || kembaliPengadaanItem.getId() == null ? 0.0
				: (Number) session.createCriteria(KembaliPengadaanItemDetail.class)
						.add(Restrictions.eq("kembaliPengadaanItem", kembaliPengadaanItem))
						.setProjection(Projections.sum("denda")).uniqueResult();
		HibernateUtil.closeSession();
		return totalDenda == null ? 0.0 : totalDenda.doubleValue();
	}

	public static Integer getJumlahHariBatas(Anggota anggota, Perpustakaan perpustakaan) {
		if (anggota == null || perpustakaan == null) {
			return 0;
		}
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Mahasiswa mahasiswa = anggota.getMahasiswa();
			Fakultas fakultas = null;
			Jurusan jurusan = null;
			try {
				if (anggota.getMahasiswa() != null && anggota.getMahasiswa().getJurusan() != null) {
					jurusan = anggota.getMahasiswa().getJurusan();
					fakultas = jurusan.getFakultas();
				} else if (anggota.getDosen() != null) {
					jurusan = anggota.getDosen().getJurusan();
					fakultas = anggota.getDosen().getFakultas();
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			Number jumlahHariBatas = (Number) session.createCriteria(BatasWaktuPeminjamanItem.class)
					.add(Restrictions.or(Restrictions.eq("jenisAnggota", anggota.getJenisAnggota()),
							Restrictions.isNull("jenisAnggota")))
					.add(Restrictions.or(Restrictions.eq("tipeAnggota", anggota.getTipeAnggota()),
							Restrictions.isNull("tipeAnggota")))
					.add(Restrictions.or(fakultas == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas", fakultas),
							Restrictions.isNull("fakultas")))
					.add(Restrictions.or(jurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", jurusan),
							Restrictions.isNull("jurusan")))
					.add(mahasiswa != null ? Restrictions.le("berlakuUntukSemester", mahasiswa.currentSemester())
							: Restrictions.isNull("berlakuUntukSemester"))
					.add(Restrictions.eq("perpustakaan", perpustakaan))
					.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
					.setProjection(Projections.property("jumlahHari")).addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1)
					.uniqueResult();
			if (jumlahHariBatas == null) {
				jumlahHariBatas = (Number) session.createCriteria(BatasWaktuPeminjamanItem.class)
						.add(Restrictions.or(Restrictions.eq("jenisAnggota", anggota.getJenisAnggota()),
								Restrictions.isNull("jenisAnggota")))
						.add(Restrictions.or(Restrictions.eq("tipeAnggota", anggota.getTipeAnggota()),
								Restrictions.isNull("tipeAnggota")))
						.add(Restrictions.or(fakultas == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas", fakultas),
								Restrictions.isNull("fakultas")))
						.add(Restrictions.or(jurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", jurusan),
								Restrictions.isNull("jurusan")))
						.add(Restrictions.isNull("berlakuUntukSemester"))
						.add(Restrictions.eq("perpustakaan", perpustakaan))
						.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
						.setProjection(Projections.property("jumlahHari")).addOrder(Order.desc("mulaiBerlaku"))
						.setMaxResults(1).uniqueResult();
			}

			return jumlahHariBatas == null ? 0 : jumlahHariBatas.intValue();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 0;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static Double hitungDenda(PeminjamanPengadaanItem peminjamanPengadaanItem, Date tanggalDikembalikan) {
		Double totalDenda = 0.0;
		Session session = HibernateUtil.currentNativeSession();

		Integer jumlahHariBatas = LibraryUtil.getJumlahHariBatas(peminjamanPengadaanItem.getAnggota(),
				peminjamanPengadaanItem.getPerpustakaan());
		peminjamanPengadaanItem.setJumlahHariBatas(jumlahHariBatas.intValue());

		int workingDays = Common.getWorkingDaysBetweenTwoDates(peminjamanPengadaanItem.getTanggalPembuatan(),
				tanggalDikembalikan);

		Double denda = (Double) session.createCriteria(DendaKeterlambatanItem.class)
				.add(Restrictions.or(
						Restrictions.eq("jenisAnggota", peminjamanPengadaanItem.getAnggota().getJenisAnggota()),
						Restrictions.isNull("jenisAnggota")))
				.add(Restrictions.or(
						Restrictions.eq("tipeAnggota", peminjamanPengadaanItem.getAnggota().getTipeAnggota()),
						Restrictions.isNull("tipeAnggota")))
				.add(Restrictions.or(Restrictions.eq("fakultas",
						peminjamanPengadaanItem.getAnggota().getMahasiswa() != null
								? peminjamanPengadaanItem.getAnggota().getMahasiswa().getJurusan().getFakultas()
								: (peminjamanPengadaanItem.getAnggota().getDosen() != null
										? peminjamanPengadaanItem.getAnggota().getDosen().getFakultas()
										: null)),
						Restrictions.isNull("fakultas")))

				.add(Restrictions.or(Restrictions.eq("jurusan",
						peminjamanPengadaanItem.getAnggota().getMahasiswa() != null
								? peminjamanPengadaanItem.getAnggota().getMahasiswa().getJurusan()
								: (peminjamanPengadaanItem.getAnggota().getDosen() != null
										? peminjamanPengadaanItem.getAnggota().getDosen().getJurusan()
										: null)),
						Restrictions.isNull("jurusan")))
				.add(Restrictions.eq("perpustakaan", peminjamanPengadaanItem.getPerpustakaan()))
				.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
				.setProjection(Projections.property("denda")).add(Restrictions.eq("dendaPerItem", false))
				.add(Restrictions.sqlRestriction("(jumlah_hari+" + jumlahHariBatas + ") < " + workingDays))
				.addOrder(Order.desc("jumlahHari")).setMaxResults(1).uniqueResult();

		System.out.println("Total denda " + totalDenda);

		HibernateUtil.closeSession();
		return denda == null ? 0.0 : denda;
	}

	public static Component gambarAnggota(Anggota anggota) throws Exception {
		Component image = new Label("");
		if (anggota != null) {
			if (anggota.getMahasiswa() != null) {
				image = CommonMedia.tampilkanGambarKecil(anggota.getMahasiswa());
			} else if (anggota.getSiswa() != null) {
				image = CommonMedia.tampilkanGambarKecil(anggota.getSiswa());
			} else if (anggota.getGuru() != null) {
				image = CommonMedia.tampilkanGambarKecil(anggota.getGuru());
			} else if (anggota.getDosen() != null) {
				image = CommonMedia.tampilkanGambarKecil(anggota.getDosen());
			} else if (anggota.getPegawai() != null) {
				image = CommonMedia.tampilkanGambarKecil(anggota.getPegawai());
			} else if (anggota.getTbmuser() != null) {
				image = CommonMedia.tampilkanGambarKecil(anggota.getTbmuser());
			}
		}
		return image;
	}

	/**
	 * Mencari anggota dari identitas yang lazim dipakai pada pemindai kartu/buku
	 * tamu. Pencarian tidak hanya bergantung pada kode anggota karena pengguna
	 * sering memasukkan NIM/NPM, NIDN, NIP, NIK, atau user ID.
	 */
	public static Anggota cariAnggotaDariIdentitas(Session session, String identitas) {
		String kode = identitas == null ? "" : identitas.trim().toLowerCase();
		if (session == null || kode.isEmpty()) {
			return null;
		}
		String hql = "select a from Anggota a "
				+ "left join a.mahasiswa m "
				+ "left join a.dosen d "
				+ "left join a.siswa s "
				+ "left join a.guru g "
				+ "left join a.pegawai p "
				+ "left join a.tbmuser u "
				+ "where lower(a.kode) = :kode "
				+ "or lower(m.nim) = :kode "
				+ "or lower(d.code) = :kode "
				+ "or lower(d.nidn) = :kode "
				+ "or lower(s.nim) = :kode "
				+ "or lower(s.nomorInduk) = :kode "
				+ "or lower(s.nik) = :kode "
				+ "or lower(g.kode) = :kode "
				+ "or lower(g.nip) = :kode "
				+ "or lower(g.nik) = :kode "
				+ "or lower(p.code) = :kode "
				+ "or lower(u.userId) = :kode";
		return (Anggota) session.createQuery(hql).setString("kode", kode).setMaxResults(1).uniqueResult();
	}
	
	

	public static Component gambarTamu(KunjunganTamu anggota) throws Exception {
		Component image = new Label("");
		if (anggota != null) {
			if (anggota.getMahasiswa() != null) {
				image = CommonMedia.tampilkanGambarKecil(anggota.getMahasiswa());
			} else if (anggota.getSiswa() != null) {
				image = CommonMedia.tampilkanGambarKecil(anggota.getSiswa());
			} else if (anggota.getGuru() != null) {
				image = CommonMedia.tampilkanGambarKecil(anggota.getGuru());
			} else if (anggota.getDosen() != null) {
				image = CommonMedia.tampilkanGambarKecil(anggota.getDosen());
			} else if (anggota.getPegawai() != null) {
				image = CommonMedia.tampilkanGambarKecil(anggota.getPegawai());
			} 
		}
		return image;
	}

	@SuppressWarnings("unchecked")
	public static String tampilanSummaryPeminjaman(KembaliPengadaanItem kembaliPengadaanItem,
			PeminjamanPengadaanItem peminjamanPengadaanItem) {

		Session session = HibernateUtil.currentNativeSession();
		List<PeminjamanPengadaanItemDetail> objects;
		if (kembaliPengadaanItem != null && kembaliPengadaanItem.getId() != null) {
			objects = session.createCriteria(PeminjamanPengadaanItemDetail.class)
					.createAlias("kembaliPengadaanItemDetail", "kembaliPengadaanItemDetail")
					.add(Restrictions.eq("kembaliPengadaanItemDetail.kembaliPengadaanItem", kembaliPengadaanItem))
					.list();

		} else {
			objects = session.createCriteria(PeminjamanPengadaanItemDetail.class)
					.add(Restrictions.eq("peminjamanPengadaanItem", peminjamanPengadaanItem)).list();
		}

		Double totalDenda = 0.0;
		String jumlahHariTerlambat = "<ol>";
		for (PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail : objects) {

			String kembali = "Lama dipinjam: "
					+ Common.numberFormat.get().format(peminjamanPengadaanItemDetail.getJumlahSelisihHari()) + " hari";
			kembali += ", Perpanjang "
					+ Common.numberFormat.get().format(peminjamanPengadaanItemDetail.getJumlahPerpanjangan()) + " kali";
			kembali += ", Terlambat "
					+ Common.numberFormat.get().format(peminjamanPengadaanItemDetail.getJumlahHariTerlambat()) + " hari";

			if (peminjamanPengadaanItemDetail.getTanggalKembali() != null) {
				kembali = "Tgl. Kembali:" + Common.dateFormat4.get().format(peminjamanPengadaanItemDetail.getTanggalKembali())
						+ " ";
			} else {
				kembali = "Belum dikembalikan, ";
			}

			KembaliPengadaanItemDetail kembaliPengadaanItemDetail = peminjamanPengadaanItemDetail
					.getKembaliPengadaanItemDetail();
			if (kembaliPengadaanItemDetail != null) {
				totalDenda += kembaliPengadaanItemDetail.getDenda();
				if (!kembaliPengadaanItemDetail.getKetDenda().isEmpty()) {
					kembali += kembaliPengadaanItemDetail.getKetDenda();
				} else {
					kembali += ", Denda: Rp. " + Common.numberFormat.get().format(kembaliPengadaanItemDetail.getDenda())
							+ " ";
				}
			} else {
				DendaKeterlambatanItem dendaPerItem = LibraryUtil.hitungDendaItem(peminjamanPengadaanItemDetail);

				Double denda = dendaPerItem == null ? 0.0 : dendaPerItem.getDenda();
				denda = denda * peminjamanPengadaanItemDetail.getJumlah();

				totalDenda += denda;

				if (dendaPerItem != null && !dendaPerItem.getKeterangan().isEmpty()) {
					kembali += dendaPerItem.getKeterangan();
				} else {
					kembali += ", Denda: Rp. " + Common.numberFormat.get().format(denda) + " ";
				}
			}

			jumlahHariTerlambat += "<li><font style=\"font-size:6pt;\">"
					+ peminjamanPengadaanItemDetail.getItem().getNama() + " = " + kembali + ", harus kembali sebelum : "
					+ Common.dateFormat4.get().format(peminjamanPengadaanItemDetail.getBatasWaktupengembalian())
					+ "</font></li>";
		}
		jumlahHariTerlambat += "</ol>";

		HibernateUtil.closeSession();

		objects = null;

		String denda = "<font style='font-size:6pt;'>" + "<ul>";
		denda += "<li><font style=\"font-size:6pt;\">Tgl. Pinjam:"
				+ Common.dateFormat4.get().format(peminjamanPengadaanItem.getTanggalPembuatan()) + "</font></li>";
		denda += "<li><font style=\"font-size:6pt;\">Batas Kembali: "
				+ Common.numberFormat.get().format(peminjamanPengadaanItem.getJumlahHariBatas()) + " hari</font></li>";

		denda += "<li><font style=\"font-size:6pt;\">Rinci:" + jumlahHariTerlambat + "</font></li>";

		denda += "<li><font style=\"font-size:6pt;color:red;\">Total Denda:Rp." + Common.numberFormat.get().format(totalDenda)
				+ "</font></li>";

		if (kembaliPengadaanItem != null) {
			session = HibernateUtil.currentNativeSession();
			List<KembaliPengadaanItemDetail> kembaliPengadaanItemDetails = session
					.createCriteria(KembaliPengadaanItemDetail.class)
					.add(Restrictions.eq("kembaliPengadaanItem", kembaliPengadaanItem))
					.add(Restrictions.gt("dibayarSejumlah", 0.1)).list();
			if (!kembaliPengadaanItemDetails.isEmpty()) {
				denda += "<li><font style=\"font-size:6pt;\">Dibayar:<br>";
			}
			for (KembaliPengadaanItemDetail kembaliPengadaanItemDetail : kembaliPengadaanItemDetails) {
				denda += "" + kembaliPengadaanItemDetail.getItem().getNama() + "=>Rp."
						+ Common.numberFormat.get().format(kembaliPengadaanItemDetail.getDibayarSejumlah()) + "<br>";
			}
			if (!kembaliPengadaanItemDetails.isEmpty()) {
				denda += "</font></li>";
			}
			HibernateUtil.closeSession();
		}

		denda += "</ul>" + "</font>";
		return denda;
	}

	@SuppressWarnings("unchecked")
	public static String tampilanSummaryPeminjaman(KembaliSuratItem kembaliSuratItem,
			PeminjamanSuratItem peminjamanSuratItem) {

		Session session = HibernateUtil.currentNativeSession();
		List<PeminjamanSuratItemDetail> objects;
		if (kembaliSuratItem != null && kembaliSuratItem.getId() != null) {
			objects = session.createCriteria(PeminjamanSuratItemDetail.class)
					.createAlias("kembaliSuratItemDetail", "kembaliSuratItemDetail")
					.add(Restrictions.eq("kembaliSuratItemDetail.kembaliSuratItem", kembaliSuratItem)).list();

		} else {
			objects = session.createCriteria(PeminjamanSuratItemDetail.class)
					.add(Restrictions.eq("peminjamanSuratItem", peminjamanSuratItem)).list();
		}

		Double totalDenda = 0.0;
		String jumlahHariTerlambat = "<ol>";
		for (PeminjamanSuratItemDetail peminjamanSuratItemDetail : objects) {
			SuratMasuk suratMasuk = peminjamanSuratItemDetail.getSuratMasuk();
			if (suratMasuk != null) {
				peminjamanSuratItemDetail.setJumlahMaxPerpanjangan(
						suratMasuk.getKlasifikasiSuratMasuk().getMaksimalJumlahPerpanjaangan());
			}

			String kembali = "Lama dipinjam: "
					+ Common.numberFormat.get().format(peminjamanSuratItemDetail.getJumlahSelisihHari()) + " hari";
			kembali += ", Perpanjang " + Common.numberFormat.get().format(peminjamanSuratItemDetail.getJumlahPerpanjangan())
					+ " kali";
			kembali += ", Terlambat " + Common.numberFormat.get().format(peminjamanSuratItemDetail.getJumlahHariTerlambat())
					+ " hari";

			if (peminjamanSuratItemDetail.getTanggalKembali() != null) {
				kembali = "Tgl. Kembali:" + Common.dateFormat4.get().format(peminjamanSuratItemDetail.getTanggalKembali())
						+ " ";
			} else {
				kembali = "Belum dikembalikan, ";
			}

			KembaliSuratItemDetail kembaliSuratItemDetail = peminjamanSuratItemDetail.getKembaliSuratItemDetail();
			if (kembaliSuratItemDetail != null) {
				totalDenda += kembaliSuratItemDetail.getDenda();
				if (!kembaliSuratItemDetail.getKetDenda().isEmpty()) {
					kembali += kembaliSuratItemDetail.getKetDenda();
				} else {
					kembali += ", Denda: Rp. " + Common.numberFormat.get().format(kembaliSuratItemDetail.getDenda()) + " ";
				}
			}

			jumlahHariTerlambat += "<li><font style=\"font-size:6pt;\">"
					+ peminjamanSuratItemDetail.getSuratMasuk().getKode() + " "
					+ peminjamanSuratItemDetail.getSuratMasuk().getPerihal() + " = " + kembali
					+ ", harus kembali sebelum : "
					+ Common.dateFormat4.get().format(peminjamanSuratItemDetail.getBatasWaktupengembalian()) + "</font></li>";
		}
		jumlahHariTerlambat += "</ol>";

		HibernateUtil.closeSession();

		objects = null;

		String denda = "<font style='font-size:6pt;'>" + "<ul>";
		denda += "<li><font style=\"font-size:6pt;\">Tgl. Pinjam:"
				+ Common.dateFormat4.get().format(peminjamanSuratItem.getTanggalPembuatan()) + "</font></li>";
		denda += "<li><font style=\"font-size:6pt;\">Batas Kembali: "
				+ Common.numberFormat.get().format(peminjamanSuratItem.getJumlahHariBatas()) + " hari</font></li>";

		denda += "<li><font style=\"font-size:6pt;\">Rinci:" + jumlahHariTerlambat + "</font></li>";

		denda += "</ul>" + "</font>";
		return denda;
	}

	public static String tampilanSummaryPeminjamanFormatDesktop(KembaliPengadaanItem kembaliPengadaanItem,
			PeminjamanPengadaanItem peminjamanPengadaanItem) {
		return tampilanSummaryPeminjaman(kembaliPengadaanItem, peminjamanPengadaanItem);
	}

	@SuppressWarnings("deprecation")
	public static void checkSkripsiForItem(Skripsi skripsi, Long tipeItem, String lampiran, int indexke)
			throws Exception {
		if (skripsi == null || skripsi.getMahasiswa() == null || tipeItem == null || lampiran.trim().isEmpty()) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		try {
			ItemAction.pastikanKolomTeksItemTidakTerpotong(session);
			TipeItem myTipeItem = (TipeItem) session.createCriteria(TipeItem.class).add(Restrictions.idEq(tipeItem))
					.uniqueResult();
			if (myTipeItem == null) {
				return;
			}
			int count = ((Number) session.createCriteria(Item.class).setProjection(Projections.rowCount())
					.add(Restrictions.eq("isbn", "NIM:" + skripsi.getMahasiswa().getNim() + ":" + tipeItem))
					.setMaxResults(1).uniqueResult()).intValue();
			Item item = null;
			if (count == 0) {
				item = (Item) session.createCriteria(Item.class)
						.add(Restrictions.eq("isbn", "NIM:" + skripsi.getMahasiswa().getNim() + ":" + tipeItem))
						.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
				if (item == null) {
					item = new Item();
				}
				item.setAbstrak(skripsi.getAbstrack());
				item.setAbstrakEn(skripsi.getAbstrack());
				item.setNama(lampiran + " - " + skripsi.getJudul());
				item.setIsbn("NIM:" + skripsi.getMahasiswa().getNim() + ":" + tipeItem);
				item.setTipeItem(myTipeItem);
				item.setJenisItem(TEXT);
				item.setGoogleBookChecked(true);
				item.setOpenLibraryBookChecked(true);

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, item);
				session.getTransaction().commit();

			}

			if (item != null && skripsi != null) {
				Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

				int qtySkripsi = ((Number) streamingSession.createCriteria(LampiranLain.class)
						.setProjection(Projections.rowCount()).add(Restrictions.eq("ref", skripsi.getId()))
						.add(Restrictions.eq("jenis", "rowUploadLampiran" + indexke)).setMaxResults(1).uniqueResult())
						.intValue();
				if (qtySkripsi != 0) {
					LampiranLain lainMahasiswa = (LampiranLain) streamingSession.createCriteria(LampiranLain.class)
							.add(Restrictions.eq("ref", skripsi.getId()))
							.add(Restrictions.eq("jenis", "rowUploadLampiran" + indexke)).addOrder(Order.desc("id"))
							.setMaxResults(1).uniqueResult();
					if (lainMahasiswa != null) {

						int qtyItem = ((Number) streamingSession.createCriteria(LampiranLain.class)
								.setProjection(Projections.rowCount()).add(Restrictions.eq("ref", item.getId()))
								.add(Restrictions.eq("jenis", LampiranLain.ITEM)).setMaxResults(1).uniqueResult())
								.intValue();
						if (qtyItem == 0) {
							LampiranLain lainMahasiswaItem = (LampiranLain) streamingSession
									.createCriteria(LampiranLain.class).add(Restrictions.eq("ref", item.getId()))
									.add(Restrictions.eq("jenis", LampiranLain.ITEM)).setMaxResults(1).uniqueResult();
							if (lainMahasiswaItem != null) {
								streamingSession.getTransaction().begin();
								streamingSession
										.createSQLQuery(
												"delete from lampiran_lain where ref = " + lainMahasiswaItem.getRef()
														+ " and jenis = '" + lainMahasiswaItem.getJenis() + "'")
										.executeUpdate();
								streamingSession.getTransaction().commit();
							}

							File file = lainMahasiswa.ambilFile();
							FileInputStream fileInputStream = file == null || !file.exists() ? null
									: new FileInputStream(file);

							Blob blob = file == null || !file.exists() ? null
									: new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(fileInputStream));

							lainMahasiswaItem = new LampiranLain();
							lainMahasiswaItem.setFoto(blob);
							lainMahasiswaItem.setJenis(LampiranLain.ITEM);
							lainMahasiswaItem.setRef(item.getId());
							lainMahasiswaItem.setKeterangan(lainMahasiswa.getKeterangan());
							lainMahasiswaItem.setNama(lainMahasiswa.getNama());

							lainMahasiswaItem.setCopyDari(lainMahasiswa);
							lainMahasiswaItem.setGdrive(lainMahasiswa.getGdrive());

							streamingSession.getTransaction().begin();
							streamingSession.save(lainMahasiswaItem);
							streamingSession.getTransaction().commit();

							item.setLampiranPath(lainMahasiswaItem.ambilFile().getAbsolutePath());
							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, item);
							session.getTransaction().commit();

						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/library/util/LibraryUtil.java:1744");
		} finally {
			StreamingHibernateUtil.getInstance().closeSession();
			if (session != null && session.isOpen()) {
				try { session.clear(); } catch (Exception e) { }
				try { session.disconnect(); } catch (Exception e) { }
				try { session.close(); } catch (Exception e) { }
			}
			HibernateUtil.closeSession();
		}
	}

	@SuppressWarnings("deprecation")
	public static void checkSkripsiForItem(Skripsi skripsi, boolean ubahSekarang, Tbmuser tbmuser) throws Exception {

		if (Common.bolehKonfigurasi("input_data_skripsi_otomatis_masuk_perpustakaan")) {

			if (skripsi == null || skripsi.getMahasiswa() == null || !skripsi.getSetujuiSidang()) {
				return;
			}

			Session session = HibernateUtil.currentNativeSession();
			ItemAction.pastikanKolomTeksItemTidakTerpotong(session);

			try {

				TipeItem tipeItem = KARYA_ILMIAH;
				if (skripsi != null && skripsi.getMahasiswa() != null && skripsi.getMahasiswa().getJenjang() != null
						&& ConstantValues.d3 != null
						&& skripsi.getMahasiswa().getJenjang().getId().equals(ConstantValues.d3.getId())) {
					tipeItem = TUGAS_AKHIR;
				} else if (skripsi != null && skripsi.getMahasiswa() != null
						&& skripsi.getMahasiswa().getJenjang() != null && ConstantValues.s1 != null
						&& skripsi.getMahasiswa().getJenjang().getId().equals(ConstantValues.s1.getId())) {
					tipeItem = SKRIPSI;
				} else if (skripsi != null && skripsi.getMahasiswa() != null
						&& skripsi.getMahasiswa().getJenjang() != null && ConstantValues.s2 != null
						&& skripsi.getMahasiswa().getJenjang().getId().equals(ConstantValues.s2.getId())) {
					tipeItem = THESIS;
				} else if (skripsi != null && skripsi.getMahasiswa() != null
						&& skripsi.getMahasiswa().getJenjang() != null && ConstantValues.s3 != null
						&& skripsi.getMahasiswa().getJenjang().getId().equals(ConstantValues.s3.getId())) {
					tipeItem = DISERTASI;
				}

				Item item = (Item) session.createCriteria(Item.class)
						.add(Restrictions.eq("isbn", "NIM:" + skripsi.getMahasiswa().getNim())).setMaxResults(1)
						.addOrder(Order.desc("id")).uniqueResult();
				if (item == null || ubahSekarang) {

					if (item == null) {
						item = new Item();
						item.setJurusan(skripsi.getMahasiswa().getJurusan());
						String by_statement = "," + skripsi.getMahasiswa().getJurusan().getId() + ",";
						item.setBy_statement(by_statement);
					}
					item.setAbstrak(skripsi.getAbstrack());
					item.setAbstrakEn(skripsi.getAbstrack());
					item.setNama(skripsi.getJudul());
					item.setIsbn("NIM:" + skripsi.getMahasiswa().getNim());
					item.setTipeItem(tipeItem);
					item.setJenisItem(TEXT);
					item.setGoogleBookChecked(true);
					item.setOpenLibraryBookChecked(true);

					if (item.getBy_statement().isEmpty()) {
						String by_statement = "," + skripsi.getMahasiswa().getJurusan().getId() + ",";
						item.setBy_statement(by_statement);
					}

					item.setPengarangs(skripsi.getMahasiswa().getNama());
					if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
							&& tbmuser.getBiodataCalonMahasiswa() == null) {
						item.setDibuatOleh(tbmuser);
					}

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, item);
					session.getTransaction().commit();

					skripsi.setItemRef(item.getId());
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, skripsi);
					session.getTransaction().commit();

					Pengarang pengarang = (Pengarang) session.createCriteria(Pengarang.class)
							.add(Restrictions.eq("mahasiswa", skripsi.getMahasiswa())).setMaxResults(1).uniqueResult();
					if (pengarang == null) {
						pengarang = new Pengarang();
						pengarang.setMahasiswa(skripsi.getMahasiswa());
						session.getTransaction().begin();
						session.save(pengarang);
						session.getTransaction().commit();
					}

					int jml = ((Number) session.createCriteria(ItemPunyaPengarang.class)
							.add(Restrictions.eq("pengarang", pengarang)).add(Restrictions.eq("item", item))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();
					if (jml == 0) {
						ItemPunyaPengarang itemPunyaPengarang = new ItemPunyaPengarang();
						itemPunyaPengarang.setPengarang(pengarang);
						itemPunyaPengarang.setItem(item);
						session.getTransaction().begin();
						session.save(itemPunyaPengarang);
						session.getTransaction().commit();
					}
				}

				if (item != null && skripsi != null) {

					LampiranLain lainMahasiswa = LampiranLain.ambil(skripsi.getId(), LampiranLain.SKRIPSI);

					if (lainMahasiswa != null) {

						LampiranLain lampiranLainItem = LampiranLain.ambil(item.getId(), LampiranLain.ITEM);

						if (lampiranLainItem == null || ubahSekarang) {

							if (lampiranLainItem != null) {
								Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
								streamingSession.getTransaction().begin();
								lampiranLainItem.setRef(-111111111111111L);
								Common.refreshUpdate(streamingSession, lampiranLainItem);
								streamingSession.getTransaction().commit();
								StreamingHibernateUtil.getInstance().closeSession();
							}

							File file = lainMahasiswa.ambilFile();
							FileInputStream fileInputStream = file == null || !file.exists() ? null
									: new FileInputStream(file);

							Blob blob = file == null || !file.exists() ? null
									: new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(fileInputStream));

							LampiranLain lainMahasiswaItem = new LampiranLain();
							lainMahasiswaItem.setFoto(blob);
							lainMahasiswaItem.setJenis(LampiranLain.ITEM);
							lainMahasiswaItem.setRef(item.getId());
							lainMahasiswaItem.setKeterangan(lainMahasiswa.getKeterangan());
							lainMahasiswaItem.setNama(lainMahasiswa.getNama());
							lainMahasiswaItem.setCopyDari(lainMahasiswa);
							lainMahasiswaItem.setGdrive(lainMahasiswa.getGdrive());

							Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
							streamingSession.getTransaction().begin();
							streamingSession.save(lainMahasiswaItem);
							streamingSession.getTransaction().commit();

							item.setLampiranPath(lainMahasiswaItem.ambilFile().getAbsolutePath());
							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, item);
							session.getTransaction().commit();
							StreamingHibernateUtil.getInstance().closeSession();

						}
					}

				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null && session.isOpen()) {
					try { session.clear(); } catch (Exception e) { }
					try { session.disconnect(); } catch (Exception e) { }
					try { session.close(); } catch (Exception e) { }
				}
				HibernateUtil.closeSession();
			}

			if (skripsi != null && skripsi.getFormatNilaiSkripsi() != null) {
				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem1(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran1(), 1);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem2(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran2(), 2);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem3(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran3(), 3);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem4(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran4(), 4);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem5(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran5(), 5);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem6(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran6(), 6);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem7(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran7(), 7);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem8(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran8(), 8);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem9(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran9(), 9);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem10(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran10(), 10);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem11(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran11(), 11);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem12(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran12(), 12);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem13(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran13(), 13);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem14(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran14(), 14);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem15(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran15(), 15);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem16(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran16(), 16);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem17(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran17(), 17);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem18(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran18(), 18);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem19(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran19(), 19);

				checkSkripsiForItem(skripsi, skripsi.getFormatNilaiSkripsi().getTipeItem20(),
						skripsi.getFormatNilaiSkripsi().getUploadLampiran20(), 20);
			}
		}
	}

	public static int getJumlahMaksimalPeminjaman(PeminjamanPengadaanItem peminjaman) {
		if (peminjaman == null || peminjaman.getAnggota() == null) {
			return 0;
		}
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Mahasiswa mahasiswa = peminjaman.getAnggota().getMahasiswa();

			Fakultas fakultas = peminjaman.getAnggota().getMahasiswa() != null
					? peminjaman.getAnggota().getMahasiswa().getJurusan().getFakultas()
					: (peminjaman.getAnggota().getDosen() != null ? peminjaman.getAnggota().getDosen().getFakultas()
							: null);
			Jurusan jurusan = peminjaman.getAnggota().getMahasiswa() != null
					? peminjaman.getAnggota().getMahasiswa().getJurusan()
					: (peminjaman.getAnggota().getDosen() != null ? peminjaman.getAnggota().getDosen().getJurusan() : null);

			Number jumlahMaksimalPeminjaman = (Number) session.createCriteria(BatasWaktuPeminjamanItem.class)

					.add(mahasiswa != null ? Restrictions.le("berlakuUntukSemester", mahasiswa.currentSemester())
							: Restrictions.isNull("berlakuUntukSemester"))

					.add(Restrictions.or(Restrictions.eq("jenisAnggota", peminjaman.getAnggota().getJenisAnggota()),
							Restrictions.isNull("jenisAnggota")))

					.add(Restrictions.or(Restrictions.eq("tipeAnggota", peminjaman.getAnggota().getTipeAnggota()),
							Restrictions.isNull("tipeAnggota")))

					.add(Restrictions.or(Restrictions.eq("fakultas", fakultas), Restrictions.isNull("fakultas")))

					.add(Restrictions.or(Restrictions.eq("jurusan", jurusan), Restrictions.isNull("jurusan")))

					.add(Restrictions.eq("perpustakaan", peminjaman.getPerpustakaan()))
					.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
					.setProjection(Projections.property("jumlahMaksimalItemYangDipinjam"))
					.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();

			if (jumlahMaksimalPeminjaman == null) {
				jumlahMaksimalPeminjaman = (Number) session.createCriteria(BatasWaktuPeminjamanItem.class)

						.add(Restrictions.isNull("berlakuUntukSemester"))

						.add(Restrictions.or(Restrictions.eq("jenisAnggota", peminjaman.getAnggota().getJenisAnggota()),
								Restrictions.isNull("jenisAnggota")))
						.add(Restrictions.or(Restrictions.eq("tipeAnggota", peminjaman.getAnggota().getTipeAnggota()),
								Restrictions.isNull("tipeAnggota")))
						.add(Restrictions.or(Restrictions.eq("fakultas", fakultas), Restrictions.sqlRestriction("true")))

						.add(Restrictions.or(Restrictions.eq("jurusan", jurusan), Restrictions.sqlRestriction("true")))
						.add(Restrictions.eq("perpustakaan", peminjaman.getPerpustakaan()))
						.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
						.setProjection(Projections.property("jumlahMaksimalItemYangDipinjam"))
						.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();
			}
			System.out.println("jumlahMaksimalPeminjaman => " + jumlahMaksimalPeminjaman + ", "
					+ peminjaman.getPerpustakaan() + " jurusan " + jurusan + " fakultas " + fakultas);
			return jumlahMaksimalPeminjaman == null ? 0 : jumlahMaksimalPeminjaman.intValue();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 0;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static int getJumlahMaksimalPerpanjanganPeminjaman(PeminjamanPengadaanItem peminjaman) {
		if (peminjaman == null || peminjaman.getAnggota() == null) {
			return 0;
		}
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Mahasiswa mahasiswa = peminjaman.getAnggota().getMahasiswa();
			Fakultas fakultas = peminjaman.getAnggota().getMahasiswa() != null
					? peminjaman.getAnggota().getMahasiswa().getJurusan().getFakultas()
					: (peminjaman.getAnggota().getDosen() != null ? peminjaman.getAnggota().getDosen().getFakultas()
							: null);
			Jurusan jurusan = peminjaman.getAnggota().getMahasiswa() != null
					? peminjaman.getAnggota().getMahasiswa().getJurusan()
					: (peminjaman.getAnggota().getDosen() != null ? peminjaman.getAnggota().getDosen().getJurusan()
							: null);

			Number jumlahMaksimalPerpanjanganPeminjaman = (Number) session
					.createCriteria(BatasWaktuPeminjamanItem.class)

					.add(Restrictions.or(Restrictions.eq("jenisAnggota", peminjaman.getAnggota().getJenisAnggota()),
							Restrictions.isNull("jenisAnggota")))
					.add(Restrictions.or(Restrictions.eq("tipeAnggota", peminjaman.getAnggota().getTipeAnggota()),
							Restrictions.isNull("tipeAnggota")))
					.add(Restrictions.or(Restrictions.eq("fakultas", fakultas), Restrictions.isNull("fakultas")))

					.add(Restrictions.or(Restrictions.eq("jurusan", jurusan), Restrictions.isNull("jurusan")))

					.add(mahasiswa != null ? Restrictions.le("berlakuUntukSemester", mahasiswa.currentSemester())
							: Restrictions.isNull("berlakuUntukSemester"))

					.add(Restrictions.eq("perpustakaan", peminjaman.getPerpustakaan()))
					.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
					.setProjection(Projections.property("jumlahMaksimalPerpanjanganPeminjaman"))
					.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();

			if (jumlahMaksimalPerpanjanganPeminjaman == null) {
				jumlahMaksimalPerpanjanganPeminjaman = (Number) session
						.createCriteria(BatasWaktuPeminjamanItem.class)

						.add(Restrictions.or(Restrictions.eq("jenisAnggota", peminjaman.getAnggota().getJenisAnggota()),
								Restrictions.isNull("jenisAnggota")))
						.add(Restrictions.or(Restrictions.eq("tipeAnggota", peminjaman.getAnggota().getTipeAnggota()),
								Restrictions.isNull("tipeAnggota")))
						.add(Restrictions.or(Restrictions.eq("fakultas", fakultas), Restrictions.isNull("fakultas")))

						.add(Restrictions.or(Restrictions.eq("jurusan", jurusan), Restrictions.isNull("jurusan")))

						.add(Restrictions.isNull("berlakuUntukSemester"))

						.add(Restrictions.eq("perpustakaan", peminjaman.getPerpustakaan()))
						.add(Restrictions.sqlRestriction("mulaiberlaku <= CURRENT_DATE"))
						.setProjection(Projections.property("jumlahMaksimalPerpanjanganPeminjaman"))
						.addOrder(Order.desc("mulaiBerlaku")).setMaxResults(1).uniqueResult();
			}
			return jumlahMaksimalPerpanjanganPeminjaman == null ? 0
					: jumlahMaksimalPerpanjanganPeminjaman.intValue();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 0;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	@SuppressWarnings({})
	public static boolean onPerpanjang(PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail) throws Exception {
		if (peminjamanPengadaanItemDetail == null || peminjamanPengadaanItemDetail.getId() == null) {
			MyMessageboxConfig.show("Data peminjaman tidak valid atau belum tersimpan.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.openSession();
			PeminjamanPengadaanItemDetail detail = (PeminjamanPengadaanItemDetail) session
					.get(PeminjamanPengadaanItemDetail.class, peminjamanPengadaanItemDetail.getId());
			if (detail == null) {
				MyMessageboxConfig.show("Data peminjaman tidak ditemukan atau sudah dihapus.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}

			PeminjamanPengadaanItem peminjaman = detail.getPeminjamanPengadaanItem();
			Integer jumlahMaksimalPerpanjanganPeminjaman = LibraryUtil
					.getJumlahMaksimalPerpanjanganPeminjaman(peminjaman);
			int maksimal = jumlahMaksimalPerpanjanganPeminjaman == null ? 0
					: jumlahMaksimalPerpanjanganPeminjaman.intValue();
			int jumlahPerpanjangan = detail.getJumlahPerpanjangan() == null ? 0
					: detail.getJumlahPerpanjangan().intValue();
			if (maksimal <= jumlahPerpanjangan) {
				MyMessageboxConfig.show(
						"Peminjaman ini tidak bisa diperpanjang lagi. Maksimal perpanjangan adalah " + maksimal
								+ " kali",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}

			detail.setJumlahMaxPerpanjangan(maksimal);
			detail.setJumlahPerpanjangan(jumlahPerpanjangan + 1);

			tx = session.beginTransaction();
			Common.refreshUpdate(session, detail);
			tx.commit();
			peminjamanPengadaanItemDetail.setJumlahMaxPerpanjangan(detail.getJumlahMaxPerpanjangan());
			peminjamanPengadaanItemDetail.setJumlahPerpanjangan(detail.getJumlahPerpanjangan());
			return true;
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/library/util/LibraryUtil.java:2144");}
			}
			throw e;
		} finally {
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/util/LibraryUtil.java:2149");}
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/util/LibraryUtil.java:2150");}
			}
		}
	}

	public static boolean onPerpanjang(PeminjamanSuratItemDetail peminjamanSuratItemDetail) throws Exception {
		if (peminjamanSuratItemDetail == null || peminjamanSuratItemDetail.getId() == null) {
			MyMessageboxConfig.show("Data peminjaman surat tidak valid atau belum tersimpan.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.openSession();
			PeminjamanSuratItemDetail detail = (PeminjamanSuratItemDetail) session
					.get(PeminjamanSuratItemDetail.class, peminjamanSuratItemDetail.getId());
			if (detail == null) {
				MyMessageboxConfig.show("Data peminjaman surat tidak ditemukan atau sudah dihapus.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			Integer jumlahMaksimalPerpanjanganPeminjaman = 2;
			int maksimal = jumlahMaksimalPerpanjanganPeminjaman == null ? 0
					: jumlahMaksimalPerpanjanganPeminjaman.intValue();
			int jumlahPerpanjangan = detail.getJumlahPerpanjangan() == null ? 0
					: detail.getJumlahPerpanjangan().intValue();
			if (maksimal <= jumlahPerpanjangan) {
				MyMessageboxConfig.show(
						"Peminjaman ini tidak bisa diperpanjang lagi. Maksimal perpanjangan adalah " + maksimal
								+ " kali",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			detail.setJumlahMaxPerpanjangan(maksimal);
			detail.setJumlahPerpanjangan(jumlahPerpanjangan + 1);
			tx = session.beginTransaction();
			Common.refreshUpdate(session, detail);
			tx.commit();
			peminjamanSuratItemDetail.setJumlahMaxPerpanjangan(detail.getJumlahMaxPerpanjangan());
			peminjamanSuratItemDetail.setJumlahPerpanjangan(detail.getJumlahPerpanjangan());
			return true;
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/library/util/LibraryUtil.java:2194");}
			}
			throw e;
		} finally {
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/util/LibraryUtil.java:2199");}
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/util/LibraryUtil.java:2200");}
			}
		}
	}

	public static boolean onBatalPerpanjang(PeminjamanSuratItemDetail peminjamanSuratItemDetail) throws Exception {
		if (peminjamanSuratItemDetail == null || peminjamanSuratItemDetail.getId() == null) {
			return false;
		}
		Session session = null;
		Transaction tx = null; 
		try {
			session = HibernateUtil.openSession();
			PeminjamanSuratItemDetail detail = (PeminjamanSuratItemDetail) session
					.get(PeminjamanSuratItemDetail.class, peminjamanSuratItemDetail.getId());
			if (detail == null) {
				return false;
			}
			int jumlahPerpanjangan = detail.getJumlahPerpanjangan() == null ? 0
					: detail.getJumlahPerpanjangan().intValue();
			detail.setJumlahPerpanjangan(jumlahPerpanjangan <= 0 ? 0 : jumlahPerpanjangan - 1);
			tx = session.beginTransaction();
			Common.refreshUpdate(session, detail);
			tx.commit();
			peminjamanSuratItemDetail.setJumlahPerpanjangan(detail.getJumlahPerpanjangan());
			return true;
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/library/util/LibraryUtil.java:2228");}
			}
			throw e;
		} finally {
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/util/LibraryUtil.java:2233");}
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/util/LibraryUtil.java:2234");}
			}
		}
	}
	
	public static boolean onBatalPerpanjang(PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail)
			throws Exception {

		Session session = HibernateUtil.currentNativeSession();
		session.refresh(peminjamanPengadaanItemDetail);
		peminjamanPengadaanItemDetail.setJumlahPerpanjangan(peminjamanPengadaanItemDetail.getJumlahPerpanjangan() - 1);
		session.getTransaction().begin();
		Common.refreshUpdate(session, peminjamanPengadaanItemDetail);
		session.getTransaction().commit();
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
		return true;
	}

	public static void cariDiGoogleBook(String isbn, String judul, String keyword, String catatan, String pengarang,
			String penerbit, String kategori, String tahun, DataCriteria dataCriteria, Paging paging, List<Item> item,
			EventListener eventListener) throws Exception {

		if (Common.bolehKonfigurasi("terintegrasi_dengan_google_book_baru", Konfigurasi.TIDAK_AKTIF)) {

			if (item.size() != Common.ROWS_COUNT_ON_PAGE
					|| (paging == null ? 0 : paging.getActivePage()) == (paging.getPageCount() - 1)) {

				String perpus = "_";

				isbn = isbn.trim().isEmpty() ? "_" : URLEncoder.encode(isbn, "UTF-8");
				judul = judul.trim().isEmpty() ? "_" : URLEncoder.encode(judul, "UTF-8");
				keyword = keyword.trim().isEmpty() ? "_" : URLEncoder.encode(keyword, "UTF-8");
				catatan = catatan.trim().isEmpty() ? "_" : URLEncoder.encode(catatan, "UTF-8");
				pengarang = pengarang.trim().isEmpty() ? "_" : URLEncoder.encode(pengarang, "UTF-8");
				penerbit = penerbit.trim().isEmpty() ? "_" : URLEncoder.encode(penerbit, "UTF-8");
				kategori = kategori.trim().isEmpty() ? "_" : URLEncoder.encode(kategori, "UTF-8");
				tahun = tahun.trim().isEmpty() ? "_" : URLEncoder.encode(tahun, "UTF-8");

				int size = Common.ROWS_COUNT_ON_PAGE;

				try {
					// AKAR: initCriteria(true) menyertakan ORDER BY. Dengan projeksi COUNT (rowCount),
					// PostgreSQL menolak: "column this_.id must appear in the GROUP BY clause...".
					// Untuk hitungan jumlah, urutan tidak diperlukan -> pakai initCriteria(false).
					Object d = dataCriteria == null ? null : dataCriteria.initCriteria(false);
					size = ((Number) ((Criteria) d).add(Restrictions.ilike("link", "google", MatchMode.ANYWHERE))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
				String url = "http" + (Common.isSecure(request) ? "s" : "") + "://" + request.getServerName() + ":"
						+ request.getServerPort() + request.getContextPath() + "/resources/perpustakaan/items_manual/"
						+ perpus + "/_/" + judul + "/" + isbn + "/" + pengarang + "/" + keyword + "/" + catatan + "/"
						+ penerbit + "/" + kategori + "/" + tahun + "/_/_/" + size + "/35/false/";
				System.out.println("url = " + url);

				try {
					JSONObject items = Common.getJsonObject(url);
					JSONArray stokItem = items.getJSONArray("item");

					if (eventListener != null && stokItem.length() > 0) {
						Common.createDefaultTimer(eventListener, "Loading..", false, 5000);
					}

				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/util/LibraryUtil.java:2302");

				}

			}
		}
	}

}
