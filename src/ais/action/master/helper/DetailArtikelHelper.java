package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.JurusanAction;
import ais.action.master.LogLoginAction;
import ais.action.master.bkd.helper.PenilaianAsesorHelper;
import ais.action.master.helper.generic.AmbilDataMahasiswaBanyak;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.action.master.penelitiandanpengabdian.JurnalPenelitianAction;
import ais.action.master.penelitiandanpengabdian.TahapanPenyusunanArtikelAction;
import ais.action.report.format1.penelitiandanpengabdian.LaporanArtikel;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.Html2Text;
import ais.common.listener.DataLoader;
import ais.common.scholar.GoogleScholarCrawlerByUser;
import ais.common.sinta.SintaPtCrawler;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PenilaianAsesor;
import ais.database.model.Perkuliahan;
import ais.database.model.ScholarArticle;
import ais.database.model.ScholarAuthor;
import ais.database.model.SintaArticle;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.penelitiandanpengabdian.AnggotaArtikel;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.database.model.penelitiandanpengabdian.ArtikelTerindeks;
import ais.database.model.penelitiandanpengabdian.FileArtikel;
import ais.database.model.penelitiandanpengabdian.JenisJabatanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.JenisPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.SumberDanaPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.TahapanPenyusunanArtikel;
import ais.database.model.penelitiandanpengabdian.TingkatArtikel;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.DataCriteria;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DetailArtikelHelper implements DataLoader, DataCriteria, FormSop {

	private MyGrid gridPengajuan;
	private Combobox searchJurnalPenelitian;
	private Boolean readonly = false;

	private Paging paging;
	protected String usernamePengajuan;
	public String diperuntukkanPengajuan;
	private Textbox cariPengaju;
	private Tbmuser tbmuser = null;
	private Dosen dosen = null;

	public DetailArtikelHelper() {
		tbmuser = Common.getCurrentUser();
		if (dosen == null) {
			dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		}
	}

	public DetailArtikelHelper(Dosen dosen, boolean persetujuan) {
		this.persetujuan = persetujuan;
		tbmuser = Common.getCurrentUser();
		if (dosen == null) {
			dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		}
		this.dosen = dosen;
	}

	public DetailArtikelHelper(Dosen dosen) {
		tbmuser = Common.getCurrentUser();
		if (dosen == null) {
			dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		}
		this.dosen = dosen;
	}

	private File f = null;
	protected LampiranLain plagiatCheckerApp;
	protected LampiranLain peerReviewApp;
	protected LampiranLain sKeterangan;
	protected LampiranLain sTugas;
	private boolean surat_tugas_wajib_diupload_saat_mengajukan_artikel;
	private boolean surat_keterangan_wajib_diupload_saat_mengajukan_artikel;
	private DisposisiSop disposisiSop;
	private boolean persetujuan = false;
	private MyWindow window;
	private Artikel artikelData;
	private AmbilDataTbmuserBanbox tbmuserD;
	private Label labelMahasiswa;
	private AmbilDataMahasiswaBanbox mahasiswa;
	private AmbilDataJurnalPenelitianBanbox jurnalPenelitian;
	private Textbox judul;
	private Intbox tahun;
	private Textbox keterangan;
	private Textbox keyword;
	private Textbox referensi;
	private Radiogroup tingkat;
	private Hbox terindeks;
	private Combobox tahapanPenyusunanArtikel;
	private Textbox path;
	private Textbox issn;
	private Textbox eIssn;
	private Intbox vol;
	private Textbox nomor;
	private Textbox bahasa;
	private Textbox masaPenugasan;
	private MyDatebox tanggalPublikasi;
	private Textbox editorDanKontributor;
	private Textbox previewJurnal;
	private Textbox plagiatChecker;
	private Textbox peerReview;
	private MyCheckboxConfig telahTerindeksSitasi;
	private Textbox anggotaEksternal;
	private Textbox anggota;
	private Combobox tahunAkademik;
	private Combobox semester;
	private Textbox sitasi;

	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop, MyToolbarbuttonConfig save,
			EventListener setujui) throws Exception {
		this.disposisiSop = disposisiSop;
		artikelData = (Artikel) generalValueObject;
		if (artikelData != null) {
			if (artikelData.getTbmuser() == null && tbmuser != null && tbmuser.ambilDosen() != null) {
				artikelData.setTbmuser(tbmuser);
				artikelData.setDiajukanOleh(tbmuser);
			} else if (artikelData.getTbmuser() == null && tbmuser != null && tbmuser.getMahasiswa() != null) {
				artikelData.setMahasiswa(tbmuser.getMahasiswa());
			}
		}

		Component parent = (Component) save.getAttribute("parent");
		MyGrid grid = displayWindowPengajuan(parent, artikelData.getJurnalPenelitian(), artikelData);
		if (disposisiSop == null) {

			South south = new South();
			south.setVisible(parent != null && parent instanceof Window);
			ais.ui.util.ZkCompat.setFlex(south, true);
			if (south.isVisible()) {
				window = new MyWindow();

				window.setHeight("95%");
				window.setWidth("90%");
				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				south.setParent(borderlayout);
				window.setParent(parent);
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);
				grid.setParent(center);

			} else {
				window = null;
				grid.setParent(parent);
			}

			if (!south.isVisible()) {
				Common.freeze(parent, true);
			}

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(south);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					window.detach();
				}
			});
			cancel.setParent(toolbar);

			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (onSave(event)) {

						loadDataPengajuan();
						window.detach();
					}
				}
			});
			save.setParent(toolbar);

			if (artikelData != null && artikelData.getStatus().equals(PengajuanPenelitianDanPengabdian.DISETUJUI)) {
				Common.freeze(window, true);
				save.setVisible(false);
				cancel.setDisabled(false);
			}

			if (south.isVisible()) {
				window.onModal();
			}
		}
		return grid;
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Pengajuan Artikel";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return artikelData;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return Artikel.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;

	}

	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public static void initdataAwal() {

		TahapanPenyusunanArtikelAction.isiDataDefault();

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(TingkatArtikel.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			TingkatArtikel angket = new TingkatArtikel();
			angket.setNama("Lokal");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new TingkatArtikel();
			angket.setNama("Nasional non terakreditasi");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new TingkatArtikel();
			angket.setNama("Nasional terakreditasi");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new TingkatArtikel();
			angket.setNama("Internasional");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new TingkatArtikel();
			angket.setNama("Internasional Bereputasi");
			Common.refreshSaveOrUpdate(session, angket);
		}

		count = ((Number) session.createCriteria(ArtikelTerindeks.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			ArtikelTerindeks angket = new ArtikelTerindeks();
			angket.setNama("Scopus");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new ArtikelTerindeks();
			angket.setNama("DOAJ");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new ArtikelTerindeks();
			angket.setNama("Thomson");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new ArtikelTerindeks();
			angket.setNama("Elsevier");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new ArtikelTerindeks();
			angket.setNama("SAGE");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new ArtikelTerindeks();
			angket.setNama("OXFORD");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new ArtikelTerindeks();
			angket.setNama("Zetoc");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new ArtikelTerindeks();
			angket.setNama("Google Scholar");
			Common.refreshSaveOrUpdate(session, angket);
		}

		count = ((Number) session.createCriteria(JenisPenelitianDanPengabdian.class)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {
			JenisPenelitianDanPengabdian angket = new JenisPenelitianDanPengabdian();
			angket.setKode("001.000");
			angket.setIsi("Penelitian Ilmiah");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new JenisPenelitianDanPengabdian();
			angket.setKode("002.000");
			angket.setIsi("Pengabdian Masyarakat");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new JenisPenelitianDanPengabdian();
			angket.setKode("003.000");
			angket.setIsi("Kreatifitas Mahasiswa");
			Common.refreshSaveOrUpdate(session, angket);
		}

		count = ((Number) session.createCriteria(JurnalPenelitian.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			JurnalPenelitian angket = new JurnalPenelitian();
			angket.setNama("Semua Publikasi");
			angket.setJudul("Semua Publikasi");
			angket.setPath("semua_publikasi");
			angket.setAktif(true);
			Common.refreshSaveOrUpdate(session, angket);

		}

		count = ((Number) session.createCriteria(SumberDanaPenelitianDanPengabdian.class)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {
			SumberDanaPenelitianDanPengabdian angket = new SumberDanaPenelitianDanPengabdian();
			angket.setNama("Pembiayaan sendiri oleh peneliti");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new SumberDanaPenelitianDanPengabdian();
			angket.setNama("PT/yayasan yang bersangkutan");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new SumberDanaPenelitianDanPengabdian();
			angket.setNama("Kemdiknas/Kementerian lain terkait");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new SumberDanaPenelitianDanPengabdian();
			angket.setNama("Institusi dalam negeri di luar Kemdiknas/Kementerian lain terkait");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new SumberDanaPenelitianDanPengabdian();
			angket.setNama("Institusi luar negeri");
			Common.refreshSaveOrUpdate(session, angket);
		}

		count = ((Number) session.createCriteria(JenisJabatanPenelitianDanPengabdian.class)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {
			JenisJabatanPenelitianDanPengabdian angket = new JenisJabatanPenelitianDanPengabdian();
			angket.setNama("Ketua");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new JenisJabatanPenelitianDanPengabdian();
			angket.setNama("Anggota");
			Common.refreshSaveOrUpdate(session, angket);
		}
	}

//	private MyGrid displayWindowPengajuan(JurnalPenelitian jurnalPenelitianData, Artikel artikelData) throws Exception {
//		return displayWindowPengajuan(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot(),
//				jurnalPenelitianData, artikelData);
//	}

	@SuppressWarnings("unchecked")
	private MyGrid displayWindowPengajuan(Component parent, final JurnalPenelitian jurnalPenelitianData,
			final Artikel artikelData) throws Exception {
		this.artikelData = artikelData;
		DetailArtikelHelper.initdataAwal();

		MyGrid grid = new MyGrid();

		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File / link Publikasi Ilmiah"));

		f = null;

		Hbox hbox1 = new Hbox();

		hbox1.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox1, artikelData == null ? null : artikelData.getId(),
				"File Publikasi Ilmiah", "File / link Publikasi Ilmiah", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain ttd = (LampiranLain) arg0.getData();
						f = ttd.ambilFile();
						if (artikelData != null && artikelData.getId() != null) {
							try {
								Session session = StreamingHibernateUtil.getInstance().currentSession();

								session.refresh(ttd);
								ttd.setRef(artikelData.getId());

								session.getTransaction().begin();
								session.update(ttd);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}
						}
					}
				}, null, false, false, false, !persetujuan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan oleh"));

		Hbox hboxDiajukan = new Hbox();
		hboxDiajukan.setParent(row);
		if (usernamePengajuan != null) {
			tbmuser = (Tbmuser) ConstantValues.simpleObject(
					HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("userId", usernamePengajuan)).setMaxResults(1),
					Tbmuser.class);
			artikelData.setTbmuser(tbmuser);
		}

		final Label labelPengguna;
		hboxDiajukan.appendChild(labelPengguna = new Label(ais.common.Common.getBahasaConfig("Pengaju:")));
		tbmuserD = new AmbilDataTbmuserBanbox();

		if (persetujuan) {
			hboxDiajukan.appendChild(new Label(artikelData == null || artikelData.getTbmuser() == null ? ""
					: artikelData.getTbmuser().getUserNama()));
		} else {
			hboxDiajukan.appendChild(tbmuserD);
		}
		tbmuserD.setDiperuntukkan(diperuntukkanPengajuan);
		tbmuserD.setValue(
				artikelData == null || artikelData.getTbmuser() == null ? "" : artikelData.getTbmuser().getUserNama());
		tbmuserD.setAttribute("tbmuser", artikelData == null ? null : artikelData.getTbmuser());
		tbmuserD.setWidth("200px");

		if (artikelData != null && artikelData.getTbmuser() != null) {
			tbmuserD.setDisabled(true);
		}

		labelPengguna.setVisible(
				diperuntukkanPengajuan == null || diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_UMUM));
		tbmuserD.setVisible(
				diperuntukkanPengajuan == null || diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_UMUM)
						|| diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_DOSEN)
						|| diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_PEGAWAI));

		if (artikelData.getTbmuser() == null && usernamePengajuan != null) {
			Mahasiswa mahasiswa = (Mahasiswa) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", usernamePengajuan)).setMaxResults(1)
					.uniqueResult();
			artikelData.setMahasiswa(mahasiswa);
		}

		hboxDiajukan.appendChild(labelMahasiswa = new Label(ais.common.Common.getBahasaConfig("Mahasiswa:")));
		mahasiswa = new AmbilDataMahasiswaBanbox();
		if (persetujuan) {
			hboxDiajukan.appendChild(new Label(artikelData == null || artikelData.getMahasiswa() == null ? ""
					: artikelData.getMahasiswa().getNama()));
		} else {
			hboxDiajukan.appendChild(mahasiswa);
		}

		mahasiswa.setValue(
				artikelData == null || artikelData.getMahasiswa() == null ? "" : artikelData.getMahasiswa().getNama());
		mahasiswa.setAttribute("mahasiswa", artikelData == null ? null : artikelData.getMahasiswa());
		mahasiswa.setAttribute("myValue", artikelData == null ? null : artikelData.getMahasiswa());
		mahasiswa.setWidth("200px");

		if (artikelData != null && artikelData.getMahasiswa() != null) {
			mahasiswa.setDisabled(true);
		}

		labelMahasiswa.setVisible(
				diperuntukkanPengajuan == null || diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_UMUM));
		mahasiswa.setVisible(
				diperuntukkanPengajuan == null || diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_UMUM)
						|| diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_MAHASISWA));

		if (diperuntukkanPengajuan == null || diperuntukkanPengajuan.equals(PengumumanAkademis.UNTUK_UMUM)) {
			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					mahasiswa.setVisible(true);
					mahasiswa.setVisible(true);
					labelPengguna.setVisible(true);
					labelMahasiswa.setVisible(true);
					if (mahasiswa.getAttribute("mahasiswa") == null && tbmuserD.getAttribute("tbmuser") == null) {
						mahasiswa.setVisible(true);
						mahasiswa.setVisible(true);
						labelPengguna.setVisible(true);
						labelMahasiswa.setVisible(true);
					} else if (mahasiswa.getAttribute("mahasiswa") == null) {
						mahasiswa.setVisible(false);
						labelMahasiswa.setVisible(false);
					} else if (tbmuserD.getAttribute("tbmuser") == null) {
						tbmuserD.setVisible(false);
						labelPengguna.setVisible(false);
					}

				}
			};

			eventListener.onEvent(null);
			mahasiswa.setEventListener(eventListener);
			tbmuserD.setEventListener(eventListener);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Jurnal / Publikasi"));
		jurnalPenelitian = new AmbilDataJurnalPenelitianBanbox();
		if (persetujuan) {
			row.appendChild(new Label(jurnalPenelitianData == null ? "" : jurnalPenelitianData.getJudul()));
		} else {
			row.appendChild(jurnalPenelitian);
		}
		jurnalPenelitian.setWidth("90%");
		jurnalPenelitian.setAttribute("jurnalPenelitian", jurnalPenelitianData);
		jurnalPenelitian.setReadonly(true);
		jurnalPenelitian.setValue(jurnalPenelitianData == null ? "" : jurnalPenelitianData.getJudul());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Jurnal / Publikasi"));
		judul = new Textbox(artikelData.getJudul());
		if (persetujuan) {
			row.appendChild(new Label(artikelData.getJudul()));
		} else {
			row.appendChild(judul);
		}

		judul.setWidth("90%");
		judul.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		tahun = new Intbox(artikelData.getTahun());
		if (persetujuan) {
			row.appendChild(new Label(artikelData.getTahun() + ""));
		} else {
			row.appendChild(tahun);
		}
		Html2Text parser = new Html2Text();
		parser.parse(new StringReader(artikelData == null ? "" : artikelData.getAbstrak()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Abstrak"));
		keterangan = new Textbox();
		if (persetujuan) {
			row.appendChild(new Label(parser.getText()));
		} else {
			row.appendChild(keterangan);
		}
		keterangan.setValue(parser.getText());
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kata Kunci"));
		keyword = new Textbox();
		if (persetujuan) {
			row.appendChild(new Label(artikelData == null ? "" : artikelData.getKeyword()));
		} else {
			row.appendChild(keyword);
		}
		keyword.setValue(artikelData == null ? "" : artikelData.getKeyword());
		keyword.setWidth("90%");
		keyword.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Daftar Pustaka"));
		referensi = new Textbox(artikelData == null ? "" : artikelData.getReferensi());
		if (persetujuan) {
			row.appendChild(new Label(artikelData == null ? "" : artikelData.getReferensi()));
		} else {
			row.appendChild(referensi);
		}
		referensi.setRows(2);
		referensi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sitasi"));
		sitasi = new Textbox(artikelData == null ? "" : artikelData.getSitasi());
		if (persetujuan) {
			row.appendChild(new Label(artikelData == null ? "" : artikelData.getSitasi()));
		} else {
			row.appendChild(sitasi);
		}
		sitasi.setRows(2);
		sitasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Tingkat Publikasi /	Jurnal"));

		tingkat = new Radiogroup();

		Session session = HibernateUtil.currentSession();
		if (artikelData.getId() != null) {
			session.refresh(artikelData);
		}
		String ss = "";
		List<TingkatArtikel> tingkatArtikels = session.createCriteria(TingkatArtikel.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		for (TingkatArtikel s : tingkatArtikels) {
			Radio checkbox = new Radio(s.getNama());
			checkbox.setAttribute("nilai", s);
			tingkat.appendChild(checkbox);

			if (artikelData.getId() != null && artikelData.getTingkatArtikeles().contains(s)) {
				checkbox.setChecked(true);
				ss += ss.isEmpty() ? s.getNama() : ", " + s.getNama();
			}
		}

		if (persetujuan) {
			row.appendChild(new Label(ss));
		} else {
			row.appendChild(tingkat);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Terindeks di"));

		terindeks = new Hbox();
		ss = "";
		List<ArtikelTerindeks> artikelTerindekss = session.createCriteria(ArtikelTerindeks.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		for (ArtikelTerindeks s : artikelTerindekss) {
			MyCheckboxConfig checkbox = new MyCheckboxConfig(s.getNama());
			checkbox.setAttribute("nilai", s);
			terindeks.appendChild(checkbox);

			if (artikelData.getId() != null && artikelData.getArtikelTerindekses().contains(s)) {
				checkbox.setChecked(true);
				ss += ss.isEmpty() ? s.getNama() : ", " + s.getNama();
			}
		}
		if (persetujuan) {
			row.appendChild(new Label(ss));
		} else {
			row.appendChild(terindeks);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahapan Penyusunan *"));
		tahapanPenyusunanArtikel = new Combobox();

		if (persetujuan) {
			row.appendChild(new Label(artikelData.getTahapanPenyusunanArtikel() == null ? ""
					: artikelData.getTahapanPenyusunanArtikel().getNama()));
		} else {
			row.appendChild(tahapanPenyusunanArtikel);
		}

		tahapanPenyusunanArtikel.setWidth("90%");

		Common.insertCombo(tahapanPenyusunanArtikel, new String[] { "nama", "prosentase" }, "keterangan",
				TahapanPenyusunanArtikel.class);
		Common.selectComboItem(tahapanPenyusunanArtikel, artikelData.getTahapanPenyusunanArtikel());
		tahapanPenyusunanArtikel.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Link / URL Publikasi"));
		path = new Textbox(artikelData.getPath());
		if (persetujuan) {
			row.appendChild(new Label(artikelData.getPath()));
		} else {
			row.appendChild(path);
		}

		path.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("ISSN"));
		issn = new Textbox(artikelData.getIssn());
		if (persetujuan) {
			row.appendChild(new Label(artikelData.getIssn()));
		} else {
			row.appendChild(issn);
		}
		issn.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("E-ISSN"));
		eIssn = new Textbox(artikelData.geteIssn());
		if (persetujuan) {
			row.appendChild(new Label(artikelData.geteIssn()));
		} else {
			row.appendChild(eIssn);
		}
		eIssn.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Volume"));
		vol = new Intbox(artikelData.getVol());
		if (persetujuan) {
			row.appendChild(new Label(artikelData.getVol() + ""));
		} else {
			row.appendChild(vol);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor"));
		nomor = new Textbox(artikelData.getNomor());
		if (persetujuan) {
			row.appendChild(new Label(artikelData.getVol() + ""));
		} else {
			row.appendChild(nomor);
		}
		nomor.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bahasa"));
		bahasa = new Textbox(artikelData.getBahasa());
		if (persetujuan) {
			row.appendChild(new Label(artikelData.getBahasa() + ""));
		} else {
			row.appendChild(bahasa);
		}
		bahasa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lama Pengerjaan (*)"));
		masaPenugasan = new Textbox(artikelData.getMasaPenugasan());
		if (persetujuan) {
			row.appendChild(new Label(artikelData.getMasaPenugasan() + ""));
		} else {
			row.appendChild(masaPenugasan);
		}
		masaPenugasan.setWidth("90%");

		Common.initKeterangan(rows, "Misal: 1 tahun, 6 bulan, 2 minggu, 5 hari, 8 jam, 1 semester");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sumbit/Publikasi (*)"));
		tanggalPublikasi = new MyDatebox(artikelData.getTanggalPublikasi());
		if (persetujuan) {
			row.appendChild(new Label(Common.dateFormat6.get().format(artikelData.getTanggalPublikasi())));
		} else {
			row.appendChild(tanggalPublikasi);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama editor dan kontributor"));
		editorDanKontributor = new Textbox(artikelData.getEditorDanKontributor());
		if (persetujuan) {
			row.appendChild(new Label(artikelData.getEditorDanKontributor()));
		} else {
			row.appendChild(editorDanKontributor);
		}
		editorDanKontributor.setWidth("90%");
		editorDanKontributor.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Publikasi ini di review oleh *"));
		previewJurnal = new Textbox(artikelData.getPreviewJurnal());
		if (persetujuan) {
			row.appendChild(new Label(artikelData.getPreviewJurnal()));
		} else {
			row.appendChild(previewJurnal);
		}
		previewJurnal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Publikasi telah di cek plagiat oleh"));
		plagiatChecker = new Textbox(artikelData.getPlagiatChecker());
		if (persetujuan) {
			row.appendChild(new Label(artikelData.getPlagiatChecker()));
		} else {
			row.appendChild(plagiatChecker);
		}

		plagiatChecker.setWidth("90%");

		plagiatCheckerApp = null;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Upload aplikasi plagiat checker"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, artikelData.getId(), "Plagiat_Checker", "Plagiat Checker",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						plagiatCheckerApp = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, !persetujuan);
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peer Review / Penelaahan sejawat"));

		row.appendChild(peerReview = new Textbox(artikelData.getPeerReview()));
		peerReview.setWidth("90%");

		peerReviewApp = null;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Upload Peer Review / Penelaahan sejawat"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, artikelData.getId(), "Peer Review", "Peer Review", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						peerReviewApp = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, !persetujuan);
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		telahTerindeksSitasi = new MyCheckboxConfig("Telah tercatat dalam indeks sitasi internasional");
		if (persetujuan) {
			row.appendChild(new Label("Telah tercatat dalam indeks sitasi internasional ? "
					+ (artikelData.getTelahTerindeksSitasi() ? "Ya" : "Tidak")));
		} else {
			row.appendChild(telahTerindeksSitasi);
		}

		telahTerindeksSitasi.setChecked(artikelData.getTelahTerindeksSitasi());

		surat_tugas_wajib_diupload_saat_mengajukan_artikel = Common.bolehKonfigurasi("surat_tugas_wajib_diupload_saat_mengajukan_artikel", Konfigurasi.TIDAK_AKTIF);
		surat_keterangan_wajib_diupload_saat_mengajukan_artikel = Common.bolehKonfigurasi("surat_keterangan_wajib_diupload_saat_mengajukan_artikel", Konfigurasi.TIDAK_AKTIF);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Surat Tugas " + (surat_tugas_wajib_diupload_saat_mengajukan_artikel ? " *" : "")));

		sTugas = null;

		hbox1 = new Hbox();

		hbox1.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox1, artikelData == null ? null : artikelData.getId(),
				"Surat Tugas Publikasi", "Surat Tugas", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						sTugas = (LampiranLain) arg0.getData();
						if (artikelData != null && artikelData.getId() != null) {
							try {
								Session session = StreamingHibernateUtil.getInstance().currentSession();

								session.refresh(sTugas);
								sTugas.setRef(artikelData.getId());

								session.getTransaction().begin();
								session.update(sTugas);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}
						}
					}
				}, null, false, false, false, !persetujuan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Surat Keterangan" + (surat_keterangan_wajib_diupload_saat_mengajukan_artikel ? " *" : "")));

		sKeterangan = null;

		hbox1 = new Hbox();

		hbox1.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox1, artikelData == null ? null : artikelData.getId(),
				"Surat Keterangan Publikasi", "Surat Keterangan", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						sKeterangan = (LampiranLain) arg0.getData();
						if (artikelData != null && artikelData.getId() != null) {
							try {
								Session session = StreamingHibernateUtil.getInstance().currentSession();

								session.refresh(sKeterangan);
								sKeterangan.setRef(artikelData.getId());

								session.getTransaction().begin();
								session.update(sKeterangan);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}
						}
					}
				}, null, false, false, false, !persetujuan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Author Eksternal"));
		anggotaEksternal = new Textbox(artikelData == null ? "" : artikelData.getAnggotaEksternal());
		if (persetujuan) {
			row.appendChild(new Label(artikelData == null ? "" : artikelData.getAnggotaEksternal()));
		} else {
			row.appendChild(anggotaEksternal);
		}
		anggotaEksternal.setWidth("90%");
		anggotaEksternal.setRows(3);

		Common.initKeterangan(rows,
				"Untuk memasukkan banyak Author Eksternal, masukkan nama lengkap Author Eksternal dengan pemisah tanda koma (,)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Author Internal Jurnal / Publikasi"));
		anggota = new Textbox(artikelData == null ? "" : artikelData.getAnggota());
		if (persetujuan) {
			row.appendChild(new Label(artikelData == null ? "" : artikelData.getAnggota()));
		} else {
			row.appendChild(anggota);
		}
		anggota.setWidth("90%");
		anggota.setRows(3);

		Common.initKeterangan(rows,
				"Untuk memasukkan banyak Author, masukkan username masing-masing pengguna dengan pemisah tanda koma (,)");

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Author Baru", "/img/user_male_add.png");

		row = new MyFormRow();
		row.setVisible(!persetujuan);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Hbox hboxAmbilAnggotaBaru = new Hbox();
		row.appendChild(hboxAmbilAnggotaBaru);
		hboxAmbilAnggotaBaru.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						if (tbmusers != null && tbmusers.size() != 0) {
							for (Tbmuser tbmuser : tbmusers) {
								anggota.setValue(
										anggota.getValue() + (anggota.getValue().isEmpty() ? tbmuser.getUserId()
												: "," + tbmuser.getUserId()));
							}
						}
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		toolbarbutton = new MyToolbarbuttonConfig("Ambil Author Mahasiswa", "/img/user_male_add.png");

		hboxAmbilAnggotaBaru.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataMahasiswaBanyak ambil = new AmbilDataMahasiswaBanyak(new ArrayList<Mahasiswa>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Mahasiswa> mahasiswas = (List<Mahasiswa>) arg0.getData();
						if (mahasiswas != null && mahasiswas.size() != 0) {
							for (Mahasiswa mahasiswa : mahasiswas) {
								anggota.setValue(anggota.getValue() + (anggota.getValue().isEmpty() ? mahasiswa.getNim()
										: "," + mahasiswa.getNim()));
							}
						}
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik (*)"));
		tahunAkademik = new Combobox();
		if (persetujuan) {
			row.appendChild(new Label(artikelData == null ? "" : artikelData.getTahunAkademik()));
		} else {
			row.appendChild(tahunAkademik);
		}
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		if (artikelData.getTahunAkademik() != null) {
			Common.selectComboItem(tahunAkademik, artikelData.getTahunAkademik());
		}
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		semester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semester.appendChild(comboitem);

		Common.selectComboItem(semester, artikelData.getSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester (*)"));

		if (persetujuan) {
			row.appendChild(new Label(artikelData == null ? "" : artikelData.getSemester()));
		} else {
			row.appendChild(semester);
		}

		semester.setReadonly(true);

		tanggalPublikasi.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tanggalPublikasi.getValue() != null) {
					Common.selectComboItem(tahunAkademik, Common.getCurrentTahunAkademik(tanggalPublikasi.getValue()));
					Common.selectComboItem(semester,
							Common.isNowSemensterGanjil(tanggalPublikasi.getValue()) ? Perkuliahan.GANJIL
									: Perkuliahan.GENAP);
				}
			}
		});

		return grid;

//		if (!south.isVisible()) {
//			Common.freeze(parent, true);
//		}
//
//		Toolbar toolbar = new Toolbar();
//		// toolbar.setHeight("25px");
//		toolbar.setParent(south);
//		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
//		cancel.setTooltiptext("Tutup");
//		cancel.addEventListener("onClick", new EventListener() {
//			@Override
//			public void onEvent(Event event) throws Exception {
//				window.detach();
//			}
//		});
//		cancel.setParent(toolbar);
//		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
//		save.setTooltiptext("Simpan");
//		save.addEventListener("onClick", new EventListener() {
//			@Override
//			public void onEvent(Event event) throws Exception {
//				
//				loadDataPengajuan();
//				window.detach();
//			}
//		});
//		save.setParent(toolbar);
//
//		if (south.isVisible()) {
//			window.onModal();
//		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean onSave(Event event) throws Exception {
		Session session = Common.getManualSession();

		Tbmuser selectedTbmuser = (Tbmuser) tbmuserD.getAttribute("tbmuser");
		Mahasiswa selectedMahasiswa = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");

		if (selectedTbmuser == null && selectedMahasiswa == null) {
			MyMessageboxConfig.show("Mohon maaf, mahasiswa atau user belum dipilih. Langkah yang dapat dilakukan: (1) pilih mahasiswa atau user dari daftar yang tersedia; (2) pastikan data sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		// if (keterangan.getValue().trim().isEmpty()) {
		// MyMessageboxConfig.show("Catatan harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.EXCLAMATION);
		// return;
		// }
		if (tahapanPenyusunanArtikel.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, tahapan penyusunan belum dipilih. Langkah yang dapat dilakukan: (1) pilih tahapan penyusunan dari daftar yang tersedia; (2) pastikan data tahapan sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (jurnalPenelitian.getAttribute("jurnalPenelitian") == null) {
			MyMessageboxConfig.show("Mohon maaf, jurnal belum dipilih. Langkah yang dapat dilakukan: (1) pilih jurnal dari daftar yang tersedia; (2) pastikan data jurnal sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (tanggalPublikasi.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, tanggal publikasi belum diisi. Langkah yang dapat dilakukan: (1) pilih tanggal publikasi dari kalender; (2) pastikan tanggal tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (masaPenugasan.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, lama pengerjaan belum diisi. Langkah yang dapat dilakukan: (1) isi kolom lama pengerjaan dengan durasi yang sesuai; (2) pastikan kolom tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (previewJurnal.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, preview jurnal belum diisi. Langkah yang dapat dilakukan: (1) isi kolom preview jurnal dengan abstrak atau ringkasan; (2) pastikan kolom tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							previewJurnal.focus();
						}
					});
			return false;
		}

		HashSet<TingkatArtikel> tingkatArtikes = new HashSet<TingkatArtikel>();
		List<Component> checkboxs = tingkat.getChildren();
		for (Component c : checkboxs) {
			if (c instanceof Radio) {
				Radio checkbox = (Radio) c;
				if (checkbox.isChecked()) {
					tingkatArtikes.add((TingkatArtikel) checkbox.getAttribute("nilai"));
				}
			}
		}
		if (tingkatArtikes.isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, tingkat publikasi belum dipilih. Langkah yang dapat dilakukan: (1) pilih tingkat publikasi dari pilihan yang tersedia; (2) pastikan salah satu tingkat sudah dipilih; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (surat_tugas_wajib_diupload_saat_mengajukan_artikel) {
			if (artikelData.getId() == null && sTugas == null) {
				MyMessageboxConfig.show("Mohon maaf, Surat Tugas belum diunggah. Langkah yang dapat dilakukan: (1) klik tombol upload dan pilih file Surat Tugas; (2) pastikan file sudah terunggah dengan benar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			} else if (artikelData.getId() != null) {
				FileFotoLain d = FileFotoLain.ambil(false, artikelData.getId(), "Surat Tugas Publikasi",
						LampiranLain.class);
				if (d == null) {
					MyMessageboxConfig.show("Surat Tugas harus diuplaod", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (surat_keterangan_wajib_diupload_saat_mengajukan_artikel) {
			if (artikelData.getId() == null && sKeterangan == null) {
				MyMessageboxConfig.show("Mohon maaf, Surat Keterangan belum diunggah. Langkah yang dapat dilakukan: (1) klik tombol upload dan pilih file Surat Keterangan; (2) pastikan file sudah terunggah dengan benar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			} else if (artikelData.getId() != null) {
				FileFotoLain d = FileFotoLain.ambil(false, artikelData.getId(), "Surat Keterangan Publikasi",
						LampiranLain.class);
				if (d == null) {
					MyMessageboxConfig.show("Surat Keterangan harus diuplaod", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (artikelData == null || artikelData.getId() == null) {
			artikelData = new Artikel();
		} else {
			session.refresh(artikelData);
		}
		artikelData.setEditorDanKontributor(editorDanKontributor.getValue());
		artikelData.setMasaPenugasan(masaPenugasan.getValue());
		artikelData.setTbmuser(selectedTbmuser);
		artikelData.setMahasiswa(selectedMahasiswa);
		artikelData.setJurnalPenelitian((JurnalPenelitian) jurnalPenelitian.getAttribute("jurnalPenelitian"));
		artikelData.setJudul(judul.getValue());
		artikelData.setAnggota(anggota.getValue());
		artikelData.setKeterangan(keterangan.getValue());
		artikelData.setAbstrak(keterangan.getValue());
		artikelData.seteIssn(eIssn.getValue());
		artikelData.setIssn(issn.getValue());
		artikelData.setNomor(nomor.getValue());
		artikelData.setPath(path.getValue());
		artikelData.setReferensi(referensi.getValue());
		artikelData.setSitasi(sitasi.getValue());
		artikelData.setTahun(tahun.getValue());
		artikelData.setVol(vol.getValue());
		artikelData.setTanggalPublikasi(tanggalPublikasi.getValue());
		artikelData.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		artikelData.setSemester((String) semester.getSelectedItem().getValue());
		artikelData.setBahasa(bahasa.getValue());
		artikelData.setKeyword(keyword.getValue());
		artikelData.setPreviewJurnal(previewJurnal.getValue());
		artikelData.setPlagiatChecker(plagiatChecker.getValue());
		artikelData.setPeerReview(peerReview.getValue());
		artikelData.setTahapanPenyusunanArtikel(
				(TahapanPenyusunanArtikel) tahapanPenyusunanArtikel.getSelectedItem().getValue());
		artikelData.setTelahTerindeksSitasi(telahTerindeksSitasi.isChecked());

		artikelData.setTingkatArtikeles(tingkatArtikes);
		artikelData.setAnggotaEksternal(anggotaEksternal.getValue().trim());

		if (disposisiSop != null && disposisiSop.getId() != null) {
			artikelData.setDisposisiSop(disposisiSop);
		}

		artikelData.setArtikelTerindekses(new HashSet<ArtikelTerindeks>());
		checkboxs = terindeks.getChildren();
		for (Component c : checkboxs) {
			if (c instanceof Checkbox) {
				Checkbox checkbox = (Checkbox) c;
				if (checkbox.isChecked()) {
					artikelData.getArtikelTerindekses().add((ArtikelTerindeks) checkbox.getAttribute("nilai"));
				}
			}
		}

		if (artikelData != null) {
			if (artikelData.getTbmuser() == null && tbmuser != null && tbmuser.ambilDosen() != null) {
				artikelData.setTbmuser(tbmuser);
				artikelData.setDiajukanOleh(tbmuser);
			} else if (artikelData.getTbmuser() == null && tbmuser != null && tbmuser.getMahasiswa() != null) {
				artikelData.setMahasiswa(tbmuser.getMahasiswa());
			}
		}

		session.saveOrUpdate(artikelData);

		session.flush();

		List<AnggotaArtikel> anggotaArtikels = new ArrayList<AnggotaArtikel>();
		String copyrightholder = "";
		for (String s : artikelData.getAnggota().split(",")) {
			if (!s.trim().isEmpty()) {
				Tbmuser tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.idEq(s.trim())).uniqueResult();
				if (tbmuser != null) {
					copyrightholder += copyrightholder.isEmpty() ? tbmuser.getUserNama() : "," + tbmuser.getUserNama();
					AnggotaArtikel anggotaArtikel = new AnggotaArtikel();
					anggotaArtikel.setTbmuser(tbmuser);
					anggotaArtikel.setArtikel(artikelData);
					anggotaArtikels.add(anggotaArtikel);
				} else {
					Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", s.trim())).setMaxResults(1)
							.uniqueResult();
					if (mahasiswa != null) {
						copyrightholder += copyrightholder.isEmpty() ? mahasiswa.getNama() : "," + mahasiswa.getNama();
						AnggotaArtikel anggotaArtikel = new AnggotaArtikel();
						anggotaArtikel.setMahasiswa(mahasiswa);
						anggotaArtikel.setArtikel(artikelData);
						anggotaArtikels.add(anggotaArtikel);
					} 
					
//					else {
//						MyMessageboxConfig.show("Username anggota \"" + s + "\" tidak ditemukan, coba periksa kembali",
//								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
//						return false;
//					}
				}
			}
		}

		if (!copyrightholder.trim().isEmpty()) {
			artikelData.setCopyrightHolder(copyrightholder);
			session.saveOrUpdate(artikelData);
		}

		session.createSQLQuery(
				"delete from penelitiandanpengabdian.anggota_artikel where artikel=" + artikelData.getId())
				.executeUpdate();

		for (AnggotaArtikel anggotaArtikel : anggotaArtikels) {
			anggotaArtikel.setArtikel(artikelData);
			Common.refreshSaveOrUpdate(session, anggotaArtikel);
		}

		if (plagiatCheckerApp != null) {
			try {
				Session sessionStreamin = StreamingHibernateUtil.getInstance().currentSession();

				sessionStreamin.refresh(plagiatCheckerApp);
				plagiatCheckerApp.setRef(artikelData.getId());

				sessionStreamin.getTransaction().begin();
				sessionStreamin.update(plagiatCheckerApp);
				sessionStreamin.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (peerReviewApp != null) {
			try {
				Session sessionStreamin = StreamingHibernateUtil.getInstance().currentSession();

				sessionStreamin.refresh(peerReviewApp);
				peerReviewApp.setRef(artikelData.getId());

				sessionStreamin.getTransaction().begin();
				sessionStreamin.update(peerReviewApp);
				sessionStreamin.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (sTugas != null) {
			try {
				Session sessionStreamin = StreamingHibernateUtil.getInstance().currentSession();

				sessionStreamin.refresh(sTugas);
				sTugas.setRef(artikelData.getId());

				sessionStreamin.getTransaction().begin();
				sessionStreamin.update(sTugas);
				sessionStreamin.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (sKeterangan != null) {
			try {
				Session sessionStreamin = StreamingHibernateUtil.getInstance().currentSession();

				sessionStreamin.refresh(sKeterangan);
				sKeterangan.setRef(artikelData.getId());

				sessionStreamin.getTransaction().begin();
				sessionStreamin.update(sKeterangan);
				sessionStreamin.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (f != null) {
			String mimeType = Files.probeContentType(f.toPath());
			FileArtikel filePengajuanArtikel = new FileArtikel();
			filePengajuanArtikel.setMimeType(mimeType);
			filePengajuanArtikel.setNama(f.getName());
			filePengajuanArtikel.setPath(f.getAbsolutePath());
			filePengajuanArtikel.setArtikel(artikelData);
			filePengajuanArtikel.setUploadDate(ais.ui.util.WaktuUtil.getDate());
			session.save(filePengajuanArtikel);

			HttpServletRequest request = (HttpServletRequest) (ExecutionsCtrl.getCurrent() == null ? null
					: ExecutionsCtrl.getCurrent().getNativeRequest());
			String url = "http" + (Common.isSecure(request) ? "s" : "") + "://" + request.getServerName() + ":"
					+ request.getServerPort() + request.getContextPath() + "/FilePengajuanArtikel?id="
					+ filePengajuanArtikel.getId();
			artikelData.setPathUrl(url);
			Common.refreshSaveOrUpdate(session, artikelData);
		}

		return true;
	}

	private Boolean ases = false;
	private Combobox status;
	private Textbox searchJudul;
	private List<Artikel> artikels = null;

	@SuppressWarnings("unused")
	public void displayPengajuan(Boolean ases, final String usernamePengajuan, final String diperuntukkanPengajuan,
			final JurnalPenelitian jurnalPenelitianData, final Component component, final MyWindow window,
			final String tinggi) {

		this.usernamePengajuan = usernamePengajuan;
		this.diperuntukkanPengajuan = diperuntukkanPengajuan;
		boolean terhubungKeOjs = Common.bolehKonfigurasi("terhubung_ke_ojs", Konfigurasi.TIDAK_AKTIF);
		System.out.println("usernamePengajuan => " + usernamePengajuan);
		this.ases = ases;

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataPengajuan();
			}
		});

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setVisible(!readonly);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(component instanceof Tabpanel ? Common.tampilanScroll(component) : component);
		borderlayout.setHeight(tinggi);

		North north = new North();
		north.setParent(borderlayout);
		toolbar.setParent(north);
		north.setBorder("none");

		Center center1 = new Center();
		center1.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center1, true);
		center1.setBorder("none");

		Row rowUtama = Common.tampilanScroll1(center1);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah / Ajukan Publikasi Ilmiah", "/img/new.gif");
		button.setVisible(!ases);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Artikel artikelData = new Artikel();
				artikelData.setJurnalPenelitian(jurnalPenelitianData);
//				displayWindowPengajuan(jurnalPenelitianData, artikelData);

				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setAttribute("parent", ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				form(artikelData, null, save, null);

			}

		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig singkron = new MyToolbarbuttonConfig("Singkronkan dg SINTA", "/img/favicon_sinta.png");
//		toolbar.appendChild(singkron);
		singkron.setVisible(
				!ases && dosen != null && !dosen.getKodeSinta().isEmpty() && dosen.getKodeSinta().length() > 3);

		singkron.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						MyMessageboxConfig.show("Singkron dengan data SINTA selesai dilakukan", "Informasi",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						loadData(null);
					}
				});

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
						try {
							Session session = HibernateUtil.currentNativeSession();
							TahapanPenyusunanArtikel tahapanPenyusunanArtikel = (TahapanPenyusunanArtikel) session
									.createCriteria(TahapanPenyusunanArtikel.class)
									.add(Restrictions.eq("nama", "Dicetak (terbit)")).setMaxResults(1).uniqueResult();
							SintaPtCrawler.singkronkanArtikel(dosen, label, session, tahapanPenyusunanArtikel);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailArtikelHelper.java:1615");
						}
						HibernateUtil.closeSession();
						label.setValue("");
											} finally {
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();
			}
		});

		if (false && dosen != null && dosen.getGoogleScholar() != null && dosen.getGoogleScholar().length() > 5) {
			button = new MyToolbarbuttonConfig("Singkronkan dengan Google Scholar",
					"/img/education-university-icon.png");
			button.setVisible(!ases);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					final List<ScholarArticle> articleList = new ArrayList<ScholarArticle>();
					final Label label = Common.displayLoadBar(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Session session = HibernateUtil.currentSession();
							if (usernamePengajuan != null) {
								tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("userId", usernamePengajuan)).setMaxResults(1)
										.uniqueResult();
							}

							TahapanPenyusunanArtikel tahapanPenyusunanArtikel = (TahapanPenyusunanArtikel) session
									.createCriteria(TahapanPenyusunanArtikel.class)
									.add(Restrictions.eq("nama", "Dicetak (terbit)")).setMaxResults(1).uniqueResult();

							for (ScholarArticle scholarArticle : articleList) {

								try {

									Artikel artikel = (Artikel) session.createCriteria(Artikel.class)
											.add(Restrictions.eq("scholarArticle", scholarArticle)).uniqueResult();
									if (artikel == null) {

										JSONObject jsonObject = new JSONObject(scholarArticle.getKeterangan());
										String namaJurnal = jsonObject.isNull("Jurnal") ? "Jurnal Default"
												: jsonObject.getString("Jurnal");

										String path = namaJurnal.toLowerCase().trim().replaceAll(" ", "_");

										JurnalPenelitian jurnalPenelitian = jurnalPenelitianData != null
												? jurnalPenelitianData
												: (JurnalPenelitian) session.createCriteria(JurnalPenelitian.class)
														.add(Restrictions.eq("path", path)).setMaxResults(1)
														.uniqueResult();
										if (jurnalPenelitian == null) {
											jurnalPenelitian = new JurnalPenelitian();
											jurnalPenelitian.setJudul(namaJurnal);
											jurnalPenelitian.setPath(path);
											session.save(jurnalPenelitian);
										}

										artikel = new Artikel();
										artikel.setJurnalPenelitian(jurnalPenelitian);

									}

									artikel.setTahapanPenyusunanArtikel(tahapanPenyusunanArtikel);
									artikel.setTbmuser(tbmuser);
									artikel.setScholarArticle(scholarArticle);

									session.saveOrUpdate(artikel);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);

								}

							}

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(null);
								}
							});

						}
					});

					final GoogleScholarCrawlerByUser googleScholarCrawlerByUser = new GoogleScholarCrawlerByUser(label);

					new Thread(new Runnable() {

						@Override
						public void run() {

							try {
								articleList.addAll(googleScholarCrawlerByUser.byUser(dosen.getGoogleScholar()));
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailArtikelHelper.java:1718");
							}
						}
					}).start();
				}

			});
			button.setParent(toolbar);
		}

		String[] contents = new String[] { "id", "jurnalPenelitian", "tbmuser", "mahasiswa", "judul", "tahun",
				"abstrak", "referensi", "licenseURL", "copyrightYear", "copyrightHolder", "sponsor", "anggota", "path",
				"pathUrl", "aktif", "issn", "eIssn", "vol", "nomor" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Artikel.class, this, "Download",
				"/img/print.png", contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig exportArtikelKeOjs = new MyToolbarbuttonConfig("Import dari OJS", "/img/corner.gif");
		toolbar.appendChild(exportArtikelKeOjs);
		exportArtikelKeOjs.setVisible(terhubungKeOjs && !ases);
		exportArtikelKeOjs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JurnalPenelitianAction.singkronkanArtikel(jurnalPenelitianData, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadDataPengajuan();
					}
				});
			}
		});

		cetakToolbarbutton = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
		toolbar.appendChild(cetakToolbarbutton);
		cetakToolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				LaporanArtikel laporanArtikel = new LaporanArtikel(DetailArtikelHelper.this.usernamePengajuan);
				laporanArtikel.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				laporanArtikel.setTitle("Cetak Artikel");
				laporanArtikel.setClosable(true);
				laporanArtikel.setHeight("99%");
				laporanArtikel.setWidth("90%");
				laporanArtikel.onModal();
			}
		});

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Jurnal : ")));
		searchJurnalPenelitian = new Combobox();
		searchJurnalPenelitian.setCols(10);
		Common.insertComboDanSemua(searchJurnalPenelitian, "judul", "path", JurnalPenelitian.class,
				Restrictions.eq("aktif", true));

		Common.selectComboItem(searchJurnalPenelitian, jurnalPenelitianData);
		if (jurnalPenelitianData != null) {
			searchJurnalPenelitian.setDisabled(true);
		}
		searchJurnalPenelitian.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataPengajuan();
			}
		});

		toolbar.appendChild(searchJurnalPenelitian);
		Label diajukanOleh;
		toolbar.appendChild(diajukanOleh = new Label(ais.common.Common.getBahasaConfig("Diajukan oleh : ")));
		cariPengaju = new Textbox();
		cariPengaju.setCols(10);
		cariPengaju.setParent(toolbar);
		cariPengaju.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataPengajuan();
			}
		});

		status = new Combobox();
		status.setParent(toolbar);
		status.setCols(10);
		MyComboitemConfig comboitem = new MyComboitemConfig(Artikel.BELUM_DIPROSES);
		comboitem.setValue(Artikel.BELUM_DIPROSES);
		status.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Artikel.SEDANG_DIPROSES);
		comboitem.setValue(Artikel.SEDANG_DIPROSES);
		status.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Artikel.DISETUJUI);
		comboitem.setValue(Artikel.DISETUJUI);
		status.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Artikel.DITOLAK);
		comboitem.setValue(Artikel.DITOLAK);
		status.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Semua Status");
		comboitem.setValue(null);
		status.appendChild(comboitem);
		status.setReadonly(true);

		status.setSelectedItem(comboitem);

		diajukanOleh.setVisible(usernamePengajuan == null);
		cariPengaju.setVisible(usernamePengajuan == null);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Judul : ")));
		searchJudul = new Textbox();
		searchJudul.setCols(10);
		toolbar.appendChild(searchJudul);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setParent(toolbar);
		cari.setTooltiptext("Cari");
		cari.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadDataPengajuan();
			}
		});

		MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor", "/img/corner.gif");
		toolbar.appendChild(exportKeOjs);
		exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("artikel_terhubung_ke_dspace"));
		exportKeOjs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Intbox intbox = new Intbox(0);
				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (intbox.getValue() == 0) {
							MyMessageboxConfig.show(
									"Data tidak ditemukan, apakah artikel-nya telah disetujui ?, dan khusus untuk publikasi dosen, dosen harus mempunya HOMEBASE PRODI terlebih dahulu sebelum bisa mempublikasikan ke dalam repository",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
						loadDataPengajuan();
						LogLoginAction.tampilDpsaceLog();
					}
				});

				new Thread(new Runnable() {

					@SuppressWarnings("unchecked")
					@Override
					public void run() {
						try {
							String cookie = DspaceCommon.login();
							List<Artikel> artikels = initCriteria(true)
									.createAlias("tbmuser.dosen", "dosen", Criteria.LEFT_JOIN)
									.add(Restrictions.or(Restrictions.isNotNull("mahasiswa.jurusan"),
											Restrictions.isNotNull("dosen.jurusan")))
									.add(Restrictions.eq("status", Artikel.DISETUJUI)).list();
							intbox.setValue(artikels.size());

							int rowIndex = 1;
							for (Artikel artikel : artikels) {

								Jurusan jurusan = null;

								if (artikel.getMahasiswa() != null) {
									jurusan = artikel.getMahasiswa().getJurusan();
								} else if (artikel.getTbmuser() != null && artikel.getTbmuser().getDosen() != null
										&& artikel.getTbmuser().getDosen().getJurusan() != null) {
									jurusan = artikel.getTbmuser().getDosen().getJurusan();
								} else if (artikel.getTbmuser() != null
										&& artikel.getTbmuser().ambilJurusan() != null) {
									jurusan = artikel.getTbmuser().ambilJurusan();
								}

								if (jurusan != null) {
									label.setValue("Sedang memproses data " + artikel.toString() + " ("
											+ Common.numberFormat.get().format((rowIndex++) * 100.0 / artikels.size())
											+ " %)");
									DetailArtikelHelper.getDspace(cookie, artikel, true);
								}
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							Common.tampilErrorJikaAdmin(e);
						}
						label.setValue("");
					}
				}).start();
			}
		});

		MyToolbarbuttonConfig batalExport = new MyToolbarbuttonConfig("Batalkan Ekspor", "/img/svg/trash.svg");
		toolbar.appendChild(batalExport);
		batalExport.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("artikel_terhubung_ke_dspace"));
		batalExport.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin membatalkan ekspor data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											loadDataPengajuan();
											LogLoginAction.tampilDpsaceLog();
										}
									});

									new Thread(new Runnable() {

										@SuppressWarnings("unchecked")
										@Override
										public void run() {
											try {
											try {
												String cookie = DspaceCommon.login();
												List<Artikel> artikels = initCriteria(true)
														.createAlias("tbmuser.dosen", "dosen", Criteria.LEFT_JOIN)
														.add(Restrictions.or(
																Restrictions.isNotNull("mahasiswa.jurusan"),
																Restrictions.isNotNull("dosen.jurusan")))
														.add(Restrictions.eq("status", Artikel.DISETUJUI)).list();

												int rowIndex = 1;
												for (Artikel artikel : artikels) {
													label.setValue(
															"Sedang memproses data " + artikel.toString() + " ("
																	+ Common.numberFormat.get().format(
																			(rowIndex++) * 100.0 / artikels.size())
																	+ " %)");
													DspaceInformation dspaceInformation = DspaceInformation
															.getDspaceInformation(Artikel.class.getName(),
																	artikel.getId());
													if (dspaceInformation != null) {
														int i = DspaceInformation.delete(cookie,
																"items/" + dspaceInformation.getUuid(),
																dspaceInformation.getPostInfo());
														if (i == 200) {

															Session session = HibernateUtil.currentNativeSession();
															session.getTransaction().begin();
															session.delete(dspaceInformation);
															session.getTransaction().commit();
															HibernateUtil.closeSession();
														}
													}
												}
											} catch (Exception e) {
												// TODO Auto-generated catch
												// block
												Common.tampilErrorJikaAdmin(e);
											}
											label.setValue("");
																					} finally {
												ais.database.hibernate.HibernateUtil.closeSession();
											}
										}
									}).start();

								}

							}
						});
			}
		});

		batalExport = new MyToolbarbuttonConfig("Setujui Semua", "/img/svg/check2.svg");
		toolbar.appendChild(batalExport);
		batalExport.setVisible(Common.getApakahAdmin());
		batalExport.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin mensetujui semua ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											loadDataPengajuan();
										}
									});

									new Thread(new Runnable() {

										@SuppressWarnings("unchecked")
										@Override
										public void run() {
											try {
											try {
												List<Artikel> artikels = initCriteria(true).list();

												int rowIndex = 1;
												for (Artikel artikel : artikels) {

													Session session = HibernateUtil.currentNativeSession();
													session.refresh(artikel);

													label.setValue(
															"Sedang memproses data " + artikel.toString() + " ("
																	+ Common.numberFormat.get().format(
																			(rowIndex++) * 100.0 / artikels.size())
																	+ " %)");
													artikel.setStatus(Artikel.DISETUJUI);

													try {
														session.getTransaction().begin();
														Common.refreshUpdate(session, artikel);
														session.getTransaction().commit();
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailArtikelHelper.java:2054");
														// TODO: handle exception
													}
													HibernateUtil.closeSession();

												}
											} catch (Exception e) {
												// TODO Auto-generated catch
												// block
												Common.tampilErrorJikaAdmin(e);
											}
											label.setValue("");
																					} finally {
												ais.database.hibernate.HibernateUtil.closeSession();
											}
										}
									}).start();

								}

							}
						});
			}
		});

		gridPengajuan = new MyGrid();
		gridPengajuan.setMold("paging");
		gridPengajuan.setPageSize(1000);
		gridPengajuan.setParent(rowUtama);
		gridPengajuan.setSclass("dgrid");

		Columns columns = new Columns();
		columns.setParent(gridPengajuan);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth(ases ? "40px" : "0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Rincian Artikel");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Informasi");
		column.setWidth(ases ? "35%" : "25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth(ases ? "0%" : "10%");

		loadDataPengajuan();

		MyFormRow r = new MyFormRow();
		r.setParent(rowUtama.getParent());
		paging.setParent(r);

	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Artikel.class)

				.add(searchJudul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("judul", searchJudul.getValue().trim(), MatchMode.ANYWHERE))

				.add(status.getSelectedItem() == null || status.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("status", status.getSelectedItem().getValue()))

				.add(Restrictions.isNotNull("jurnalPenelitian"))

				.createAlias("tbmuser", "tbmuser", Criteria.LEFT_JOIN)
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("jurnalPenelitian", "jurnalPenelitian", Criteria.LEFT_JOIN)

				.add(usernamePengajuan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("tbmuser.userId", usernamePengajuan),
								Restrictions.eq("mahasiswa.nim", usernamePengajuan)))

				.add(cariPengaju.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.or(
								Restrictions.ilike("tbmuser.userId", cariPengaju.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("tbmuser.userNama", cariPengaju.getValue().trim(),
										MatchMode.ANYWHERE)),
								Restrictions.or(
										Restrictions.ilike("mahasiswa.nim", cariPengaju.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("mahasiswa.nama", cariPengaju.getValue().trim(),
												MatchMode.ANYWHERE))))

				.add(searchJurnalPenelitian.getSelectedItem() == null
						|| searchJurnalPenelitian.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jurnalPenelitian",
										searchJurnalPenelitian.getSelectedItem().getValue()));

		if (order) {
			criteria.addOrder(Order.desc("tahun")).addOrder(Order.asc("id"));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadDataPengajuan() {
		Common.initPaging(initCriteria(false), paging);
		artikels = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(artikels);

		gridPengajuan.setRowRenderer(new DetailArtikelRenderer());
		gridPengajuan.setModelCheckMobile(strset);
		gridPengajuan.renderAll();

	}

	public static FileArtikel displayRow(Row arg0, final Artikel artikel, final Pegawai pegawai, final Boolean ases)
			throws Exception {

		Tbmuser tbmuser = Common.getCurrentUser();
		arg0.setValign("top");
		// TODO Auto-generated method stub

		final Vbox vboxKeterangan = new Vbox();
		final EventListener keteranganEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(vboxKeterangan);

				Session session = HibernateUtil.currentSession();
				@SuppressWarnings("unchecked")
				List<PenilaianAsesor> asesorMemberikanPenilaians = session.createCriteria(PenilaianAsesor.class)
						.add(Restrictions.isNotNull("asesor")).createAlias("asesemenPenilaian", "asesemenPenilaian")
						.add(Restrictions.eq("asesemenPenilaian.artikel", artikel)).list();
				for (PenilaianAsesor penilaianAsesor : asesorMemberikanPenilaians) {
					new Label(penilaianAsesor.getAsesor().getAsesorPenunjangKinerjaDosen().getNama() + " : "
							+ Common.numberFormat.get().format(penilaianAsesor.getSks()) + " sks, "
							+ (penilaianAsesor.getKeterangan())
							+ (penilaianAsesor.getAsesemenPenilaian().getPegawai() == null ? ""
									: " (" + penilaianAsesor.getAsesemenPenilaian().getPegawai().getNama() + ")"))
							.setParent(vboxKeterangan);
				}
			}
		};

		if (ases && pegawai != null && artikel.getStatus().equals(Artikel.DISETUJUI)) {
			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			EventListener eventListener = new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						PenilaianAsesorHelper.formNilai(pegawai, "artikel", artikel, null, artikel.getTahunAkademik(),
								artikel.getSemester(), "Karya Ilmiah ber-judul \"" + artikel.getJudul() + "\"",
								PenilaianAsesor.ARTIKEL, keteranganEventListener).setParent(detail);

					}
				}
			};

			detail.addEventListener("onOpen", eventListener);
			if (ases) {
				detail.setOpen(true);
				eventListener.onEvent(null);
			}
		} else {
			new Label().setParent(arg0);
		}
		Vbox vbox = new Vbox();
		if (artikel != null && artikel.getSintaArticle() != null) {
			final SintaArticle sintaArticle = artikel.getSintaArticle();

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);

			vbox = new Vbox();
			vbox.setParent(hbox);
			CommonMedia.tampilkanGambarKecil(sintaArticle.getDosen()).setParent(vbox);

			MyToolbarbuttonConfig myButtonConfig = new MyToolbarbuttonConfig("Lihat Isi Artikel",
					"/img/education-university-icon.png");
			myButtonConfig.setParent(vbox);
			myButtonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					LampiranLain ttd = LampiranLain.ambil(artikel.getId(), "File Publikasi Ilmiah");

					if (ttd != null) {

						String link = ttd == null ? null
								: (ttd.getLink() == null || ttd.getLink().isEmpty() ? null : ttd.getLink());

						if (ttd != null && (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
							link = ttd.createLinkUri();
							if (link != null) {
								link = link.replaceAll("download=true", "download=false");
							}
						}

						Common.displayWindow(ttd.merupakanGambar(), link, true, "95%", "95%", true, ttd);
					} else {
						if (Common.isMobile()) {
							ExecutionsCtrl.getCurrent().sendRedirect(sintaArticle.getLink(), "_blank");
						} else {
							Clients.evalJavaScript("popupCenter({url: '" + sintaArticle.getLink()
									+ "', title: 'Artikel', w: 1200, h: 600});");
						}
					}
				}
			});

			vbox = new Vbox();
			vbox.setParent(hbox);

			RevisiHelper.createNewRevisi(SintaArticle.class, sintaArticle, sintaArticle.getNama()).setParent(vbox);

			new MyLabelAgakKecilBold(sintaArticle.getJurnal() + ", " + sintaArticle.getPage() + " | issue:"
					+ sintaArticle.getIssue() + " | vol:" + sintaArticle.getVol() + " | "
					+ (sintaArticle.getTahun() == null ? "" : sintaArticle.getTahun())).setParent(vbox);
			new MyLabelKecil(sintaArticle.getDosen().getNama()).setParent(vbox);
			new MyLabelKecil(sintaArticle.getDosen().getNidn()).setParent(vbox);

			new MyLabelAgakKecilBold(sintaArticle.getAuthor()).setParent(vbox);

		} else if (artikel != null && artikel.getScholarArticle() != null) {
			final ScholarArticle scholarArticle = artikel.getScholarArticle();

			vbox.setParent(arg0);

			RevisiHelper.createNewRevisi(ScholarArticle.class, scholarArticle, scholarArticle.getNama())
					.setParent(vbox);

			new MyLabelAgakKecilBold(artikel.getJurnalPenelitian().getJudul() + " | issn:" + artikel.getIssn()
					+ " | vol:" + artikel.getVol() + " | " + (artikel.getTahun() == null ? "" : artikel.getTahun()))
					.setParent(vbox);

			MyToolbarbuttonConfig myButtonConfig = new MyToolbarbuttonConfig("Lihat Isi Artikel",
					"/img/education-university-icon.png");
			myButtonConfig.setParent(vbox);
			myButtonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					LampiranLain ttd = LampiranLain.ambil(artikel.getId(), "File Publikasi Ilmiah");

					if (ttd != null) {

						String link = ttd == null ? null
								: (ttd.getLink() == null || ttd.getLink().isEmpty() ? null : ttd.getLink());

						if (ttd != null && (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
							link = ttd.createLinkUri();
							if (link != null) {
								link = link.replaceAll("download=true", "download=false");
							}
						}

						Common.displayWindow(ttd.merupakanGambar(), link, true, "95%", "95%", true, ttd);
					} else {
						if (Common.isMobile()) {
							ExecutionsCtrl.getCurrent().sendRedirect(scholarArticle.getLink(), "_blank");
						} else {
							Clients.evalJavaScript("popupCenter({url: '" + scholarArticle.getLink()
									+ "', title: 'Artikel', w: 1200, h: 600});");
						}
					}
				}
			});

			Hbox hbox = new Hbox();
			hbox.setParent(vbox);
			HibernateUtil.currentSession().refresh(scholarArticle);
			for (final ScholarAuthor scholarAuthor : scholarArticle.getScholarAuthors()) {

				Vbox vbox2 = new Vbox();
				vbox2.setParent(hbox);
				if (scholarAuthor.getImageLink() != null && !scholarAuthor.getImageLink().trim().isEmpty()) {
					Image image = new Image(scholarAuthor.getImageLink());
					image.setHeight("72px");
					image.setParent(vbox2);
				}

				if (scholarAuthor.getKeterangan() == null || scholarAuthor.getKeterangan().equalsIgnoreCase("empty")) {
					new Label(scholarAuthor.getNama()).setParent(vbox2);
				} else {
					Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig(scholarAuthor.getNama(),
							"/img/education-university-icon.png");
					toolbarbutton.setParent(vbox2);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							if (Common.isMobile()) {
								ExecutionsCtrl.getCurrent().sendRedirect(scholarAuthor.getKeterangan(), "_blank");
							} else {
								Clients.evalJavaScript("popupCenter({url: '" + scholarAuthor.getKeterangan()
										+ "', title: 'Author', w: 1200, h: 600});");
							}
						}
					});
				}
			}

			if (scholarArticle.getLinkFile() != null && !scholarArticle.getLinkFile().trim().isEmpty()) {
				A a = new A(scholarArticle.getLinkFile());
				a.setParent(vbox);
				a.setTarget("_blank");
				a.setHref(scholarArticle.getLinkFile());
			}

		} else {

			A foto = null;
			String oleh = "Tanpa Author";
			if (artikel.getMahasiswa() != null) {
				foto = CommonMedia.tampilkanGambarKecil(artikel.getMahasiswa());
				oleh = (artikel.getMahasiswa().getNim() + " " + artikel.getMahasiswa().getNama());
			} else if (artikel.getTbmuser() != null) {
				if (artikel.getTbmuser().getDosen() != null) {
					foto = CommonMedia.tampilkanGambarKecil(artikel.getTbmuser().getDosen());
				} else if (artikel.getTbmuser().getPegawai() != null) {
					foto = CommonMedia.tampilkanGambarKecil(artikel.getTbmuser().getPegawai());
				} else {
					foto = CommonMedia.tampilkanGambarKecil(artikel.getTbmuser());
				}
				oleh = (artikel.getTbmuser().getUserNama() + " (" + artikel.getTbmuser().getUserId() + ")");
			}

			Hbox myhbox = new Hbox();
			myhbox.setParent(arg0);

			if (foto != null) {
				foto.setParent(myhbox);
			}

			vbox = new Vbox();
			vbox.setParent(myhbox);

			RevisiHelper.createNewRevisi(Artikel.class, artikel, artikel.getJudul()).setParent(vbox);

			new Label(oleh).setParent(vbox);

			String judulJurnal = artikel.getJurnalPenelitian() == null || artikel.getJurnalPenelitian().getJudul() == null
					? "Jurnal belum ditentukan" : artikel.getJurnalPenelitian().getJudul();
			new MyLabelAgakKecilBold(judulJurnal + ", " + artikel.getNomor() + " | issn:"
					+ artikel.getIssn() + " | vol:" + artikel.getVol() + " | " + artikel.getTahun()).setParent(vbox);

			MyToolbarbuttonConfig myButtonConfig = new MyToolbarbuttonConfig("Lihat Isi Artikel",
					"/img/education-university-icon.png");
			myButtonConfig.setParent(vbox);
			myButtonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					LampiranLain ttd = LampiranLain.ambil(artikel.getId(), "File Publikasi Ilmiah");

					if (ttd != null) {

						String link = ttd == null ? null
								: (ttd.getLink() == null || ttd.getLink().isEmpty() ? null : ttd.getLink());

						if (ttd != null && (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
							link = ttd.createLinkUri();
							if (link != null) {
								link = link.replaceAll("download=true", "download=false");
							}
						}

						Common.displayWindow(ttd.merupakanGambar(), link, true, "95%", "95%", true, ttd);
					} else {
						MyMessageboxConfig.show("Artikel belum di-uplaod", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
					}
				}
			});
		}

		final FileArtikel content = (FileArtikel) HibernateUtil.currentSession().createCriteria(FileArtikel.class)
				.add(Restrictions.eq("artikel", artikel)).addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

		List<String> koresponden = new ArrayList<String>();
		if (artikel.getJurnalPenelitian() != null && artikel.getJurnalPenelitian().getKorespondensi() != null) {

			for (String s : artikel.getJurnalPenelitian().getKorespondensi().split(",")) {
				if (!s.trim().isEmpty()) {
					koresponden.add(s.trim());
				}
			}
		}

		List<String> korespondenGrup = new ArrayList<String>();
		if (artikel.getJurnalPenelitian() != null && artikel.getJurnalPenelitian().getKorespondensiGrupPengguna() != null) {

			for (String s : artikel.getJurnalPenelitian().getKorespondensiGrupPengguna().split(",")) {
				if (!s.trim().isEmpty()) {
					korespondenGrup.add(s.trim());
				}
			}
		}

		Hbox hbox1 = new Hbox();

		hbox1.setParent(vbox);
		LampiranLain.createDownloadUploadFileLain(hbox1, artikel == null ? null : artikel.getId(),
				"Surat Tugas Publikasi", "Surat Tugas", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}, null, false, false, false, false);

		hbox1 = new Hbox();

		hbox1.setParent(vbox);
		LampiranLain.createDownloadUploadFileLain(hbox1, artikel == null ? null : artikel.getId(),
				"Surat Keterangan Publikasi", "Surat Keterangan", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}, null, false, false, false, false);

		vbox = new Vbox();
		vbox.setParent(arg0);

		new Label("Tgl : " + Common.dateFormat.get().format(artikel.getTanggal_dirubah())).setParent(vbox);

		Dosen dsn = artikel.getTbmuser() == null ? null : artikel.getTbmuser().getDosen();
		if ((dsn != null && dsn.yangLoginMerupakanAtasan())
				|| (!ases && artikel.getArticleId() < 0 && tbmuser != null && (koresponden.contains(tbmuser.getUserId())
						|| korespondenGrup.contains(tbmuser.hakAkses().getRoleId())))) {
			final Combobox status = new Combobox();
			status.setParent(vbox);
			status.setWidth("90%");
			MyComboitemConfig comboitem = new MyComboitemConfig(Artikel.BELUM_DIPROSES);
			comboitem.setValue(Artikel.BELUM_DIPROSES);
			status.appendChild(comboitem);

			comboitem = new MyComboitemConfig(Artikel.SEDANG_DIPROSES);
			comboitem.setValue(Artikel.SEDANG_DIPROSES);
			status.appendChild(comboitem);

			comboitem = new MyComboitemConfig(Artikel.DISETUJUI);
			comboitem.setValue(Artikel.DISETUJUI);
			status.appendChild(comboitem);

			comboitem = new MyComboitemConfig(Artikel.DITOLAK);
			comboitem.setValue(Artikel.DITOLAK);
			status.appendChild(comboitem);

			Common.selectComboItem(status, artikel.getStatus());
			status.setReadonly(true);

			status.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					artikel.setStatus(
							(String) (status.getSelectedItem() == null || status.getSelectedItem().getValue() == null
									? null
									: status.getSelectedItem().getValue()));
					Common.refreshUpdate(artikel);
				}
			});
		} else {
			new Label("Status : " + artikel.getStatus()).setParent(vbox);
		}

		new Label(ais.common.Common.getBahasaConfig("Anggota : ")).setParent(vbox);
		int i = 1;
		Session session = HibernateUtil.currentSession();
		for (String username : StringUtils.split(artikel.getAnggota(), ",")) {
			System.out.println("username=>" + username);
			tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("userId", username)).uniqueResult();
			String oleh = username;
			if (tbmuser != null) {
				oleh = (tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")");
			} else {
				Mahasiswa anggota = (Mahasiswa) session.createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", username)).setMaxResults(1)
						.uniqueResult();
				if (anggota != null) {
					oleh = (anggota.getNim() + " " + anggota.getNama());
				}
			}

			new Label(i + ". " + oleh).setParent(vbox);

			i++;
		}

		vboxKeterangan.setParent(vbox);
		keteranganEventListener.onEvent(null);

		return content;

	}

	class DetailArtikelRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Artikel artikel = (Artikel) arg1;
			DetailArtikelHelper.displayRow(arg0, artikel, tbmuser == null ? null : tbmuser.ambilPegawai(), ases);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			// MyToolbarbuttonConfig toolbarbutton = new
			// MyToolbarbuttonConfig("Download", FileFoto.icon(null));
			// toolbarbutton.setVisible(content != null);
			// toolbarbutton.setParent(hbox);
			// toolbarbutton.addEventListener("onClick", new EventListener() {
			// @Override
			// public void onEvent(Event event) throws Exception {
			// final File file = new File(content.getPath());
			// Filedownload.save(file, content.getMimeType());
			// }
			//
			// });

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			toolbarbutton.setVisible(!ases);
			toolbarbutton.setParent(hbox);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
//					displayWindowPengajuan(artikel.getJurnalPenelitian(), artikel);

					MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
					save.setAttribute("parent", ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					form(artikel, null, save, null);
				}

			});

			toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			toolbarbutton.setVisible(!ases);
			toolbarbutton.setTooltiptext("Hapus Data");
			toolbarbutton.setParent(hbox);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(artikel);

											loadDataPengajuan();
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
		}

	}

	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public Boolean getReadonly() {
		return readonly;
	}

	@Override
	public void loadData(Object value) {
		loadDataPengajuan();
	}

	public static DspaceInformation getDspaceArtikel(String cookie, Artikel artikel) throws Exception {
		Jurusan jurusan = null;

		if (artikel.getMahasiswa() != null) {
			jurusan = artikel.getMahasiswa().getJurusan();
		} else if (artikel.getTbmuser() != null && artikel.getTbmuser().getDosen() != null) {
			jurusan = artikel.getTbmuser().getDosen().getJurusan();
		}

		String label_artikel = "Artikel " + jurusan.getNama();

		String description = label_artikel + " untuk " + Common.getBahasaConfig("Jurusan") + " " + jurusan.getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", label_artikel);
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", label_artikel + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common.getKonfigurasi("dspace_label_collection_artikel_" + jurusan.getId(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "communities",
				"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/communities");

	}

	public static DspaceInformation getDspaceTahunArtikel(String cookie, Artikel artikel) throws Exception {
		Jurusan jurusan = null;

		if (artikel.getMahasiswa() != null) {
			jurusan = artikel.getMahasiswa().getJurusan();
		} else if (artikel.getTbmuser() != null && artikel.getTbmuser().getDosen() != null) {
			jurusan = artikel.getTbmuser().getDosen().getJurusan();
		}
		JurnalPenelitian jurnalPenelitian = artikel.getJurnalPenelitian();

		String label_artikel = jurnalPenelitian.getJudul();

		String description = label_artikel + " untuk " + Common.getBahasaConfig("Jurusan") + " " + jurusan.getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", "Tahun " + artikel.getTahun().toString());
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", label_artikel + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common.getKonfigurasi("dspace_label_collection_jurnalPenelitian_tahun_"
				+ jurusan.getId() + "_" + jurnalPenelitian.getId() + "_" + artikel.getTahun(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
				"communities/" + getDspaceTipeArtikel(cookie, artikel) + "/collections");
	}

	public static DspaceInformation getDspaceTipeArtikel(String cookie, Artikel artikel) throws Exception {
		Jurusan jurusan = null;

		if (artikel.getMahasiswa() != null) {
			jurusan = artikel.getMahasiswa().getJurusan();
		} else if (artikel.getTbmuser() != null && artikel.getTbmuser().getDosen() != null) {
			jurusan = artikel.getTbmuser().getDosen().getJurusan();
		}
		JurnalPenelitian jurnalPenelitian = artikel.getJurnalPenelitian();

		String label_artikel = jurnalPenelitian.getJudul();

		String description = label_artikel + " untuk " + Common.getBahasaConfig("Jurusan") + " " + jurusan.getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", label_artikel);
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", label_artikel + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common.getKonfigurasi(
				"dspace_label_collection_jurnalPenelitian_" + jurusan.getId() + "_" + jurnalPenelitian.getId(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
				"communities/" + getDspaceArtikel(cookie, artikel) + "/collections");

	}

	@SuppressWarnings("unchecked")
	public static DspaceInformation getDspace(String cookie, Artikel artikel, boolean update) throws Exception {

		JSONArray jsonArray = new JSONArray();

		String nama = "";
		if (artikel.getMahasiswa() != null) {
			nama = artikel.getMahasiswa().getNama();
		} else if (artikel.getTbmuser() != null) {
			nama = artikel.getTbmuser().getUserNama();
		}

		JSONObject jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.contributor.author");
		jsonMetadata.put("value", nama);
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.contributor.editor");
		jsonMetadata.put("value", nama);
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.date.copyright");
		jsonMetadata.put("value",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonArray.put(jsonMetadata);

		List<AnggotaArtikel> anggotaArtikels = HibernateUtil.currentSession().createCriteria(AnggotaArtikel.class)
				.add(Restrictions.eq("artikel", artikel)).list();

		for (AnggotaArtikel anggota : anggotaArtikels) {
			String oleh = "";
			if (anggota.getMahasiswa() != null) {
				oleh = anggota.getMahasiswa().getNama();
			} else if (anggota.getTbmuser() != null) {
				oleh = anggota.getTbmuser().getUserNama();
			}
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.contributor.advisor");
			jsonMetadata.put("value", oleh);
			jsonArray.put(jsonMetadata);
		}

		for (String anggota : artikel.getAnggotaEksternal().split(",")) {
			if (anggota.trim().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.contributor.advisor");
				jsonMetadata.put("value", anggota);
				jsonArray.put(jsonMetadata);
			}
		}

		Html2Text parser = new Html2Text();
		parser.parse(new StringReader(artikel.getAbstrak()));

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description.abstract");
		jsonMetadata.put("value", parser.getText());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.type");
		jsonMetadata.put("value", artikel.getJurnalPenelitian().getJudul());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.title");
		jsonMetadata.put("value", artikel.getJudul());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.subject");
		jsonMetadata.put("value", artikel.getKeyword());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.publisher");
		jsonMetadata.put("value", artikel.getCopyrightHolder());
		jsonArray.put(jsonMetadata);

//		jsonMetadata = new JSONObject();
//		jsonMetadata.put("key", "dc.identifier.uri");
//		jsonMetadata.put("value", artikel.getPath().isEmpty() ? artikel.getPathUrl() : artikel.getPath());
//		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.issn");
		jsonMetadata.put("value", artikel.getIssn());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.issn");
		jsonMetadata.put("value", artikel.geteIssn());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.language");
		jsonMetadata.put("value", artikel.getBahasa());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.issn");
		jsonMetadata.put("value", artikel.geteIssn());
		jsonArray.put(jsonMetadata);

		if (artikel.getTanggalPublikasi() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.date.issued");
			jsonMetadata.put("value", Common.databaseDateFormat.get().format(artikel.getTanggalPublikasi()));
			jsonArray.put(jsonMetadata);
		}

		LampiranLain ttd = LampiranLain.ambil(artikel.getId(), "File Publikasi Ilmiah");
		if (ttd != null) {
			if (ttd.getGdrive() != null && !ttd.getGdrive().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", ttd.exportGDriveUrl());
				jsonArray.put(jsonMetadata);
			}
		}

		LampiranLain peerReview = LampiranLain.ambil(artikel.getId(), "Peer Review");

		if (peerReview != null) {
			if (peerReview.getGdrive() != null && !peerReview.getGdrive().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", peerReview.exportGDriveUrl());
				jsonArray.put(jsonMetadata);
			}
		}

		LampiranLain lampiranLain = LampiranLain.ambil(artikel.getId(), "Plagiat_Checker");
		if (lampiranLain != null) {
			if (lampiranLain.getGdrive() != null && !lampiranLain.getGdrive().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", lampiranLain.exportGDriveUrl());
				jsonArray.put(jsonMetadata);
			} else {

			}
		}

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("metadata", jsonArray);

		boolean berdasarkanTahun = Common.bolehKonfigurasi("export_artikel_dspace_berdasarkan_tahun", Konfigurasi.TIDAK_AKTIF);

		DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie, artikel, jsonPost.toString(),
				jsonArray.toString(), update, "items",
				"collections/" + (berdasarkanTahun ? getDspaceTahunArtikel(cookie, artikel)
						: getDspaceTipeArtikel(cookie, artikel)) + "/items",
				"items/{uuid}/metadata");

		if (ttd != null) {
			if (ttd.getGdrive() != null && !ttd.getGdrive().isEmpty()) {

			} else {
				DspaceInformation.upload(cookie, dspaceInformation.getUuid(), ttd,
						"File Publikasi Ilmiah " + artikel.getJudul());
			}
		}

		if (peerReview != null) {
			if (peerReview.getGdrive() != null && !peerReview.getGdrive().isEmpty()) {

			} else {
				DspaceInformation.upload(cookie, dspaceInformation.getUuid(), peerReview,
						"Peer Review " + artikel.getJudul());
			}
		}

		if (lampiranLain != null) {
			if (lampiranLain.getGdrive() != null && !lampiranLain.getGdrive().isEmpty()) {

			} else {
				DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
						"Plagiat Checker " + artikel.getJudul());
			}
		}

		StreamingHibernateUtil.getInstance().closeSession();

		return dspaceInformation;
	}

}
