package ais.action.master;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.bkd.helper.PenilaianAsesorHelper;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.FileBukuAjarHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.dao.BukuBahanAjarDao;
import ais.database.dao.DaoFactory;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BukuBahanAjar;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.JenisPeredaranBuku;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.Perkuliahan;
import ais.database.model.TahapanPenyusunanBuku;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileBukuBahanAjar;
import ais.database.model.file.LampiranLain;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLType;
import ais.action.master.helper.FilterLanjutHelper;

public class BukuBahanAjarAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchpengarang;
	private Textbox searchpenerbit;
	private Textbox searchisbn;
	private Doublebox searchtahun;

	private Textbox nama;

	private AmbilDataDosenBanbox searchdosen1;

	private AmbilDataDosenBanbox dosenPengarang1;
	private AmbilDataDosenBanbox dosenPengarang2;
	private AmbilDataDosenBanbox dosenPengarang3;
	private AmbilDataDosenBanbox dosenPengarang4;
	private AmbilDataDosenBanbox dosenPengarang5;

	private Textbox pengarang1;
	private Textbox pengarang2;
	private Textbox pengarang3;

	private Textbox penerbit;
	private Textbox isbn;
	private Textbox link;
	private Textbox keterangan;
	private Textbox abstrak;
	private MyDatebox tanggal;
	private Intbox tahun;

	private boolean edit = false;
	private boolean delete = false;

	private BukuBahanAjar bukuBahanAjar;
	private MyToolbarbuttonConfig add;
	private EventListener eventListener;
	private Dosen dosen;
	private Combobox semester;
	private Combobox tahunAkademik;

	private boolean ases = false;
	protected LampiranLain lainMahasiswa;
	protected LampiranLain lainMahasiswaCover;
	private Textbox masaPenugasan;
	private MyCheckboxConfig pengarangAdalahDosen;
	private Combobox tahapanPenyusunanBuku;
	private Row rowPenerbit;
	private Row rowTahunTerbit;
	private Row rowTanggalTerbit;
	private Row rowISBN;
	private Textbox editorDanKontributor;
	private Combobox jenisPeredaranBuku;

	public static void tampilkanKutipan(final BukuBahanAjar itemData) throws Exception {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final MyWindow window = new MyWindow("Kutipan", "true", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("300px");
				window.setWidth("90%");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);
				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("15%");

				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				CSLItemData item = BukuBahanAjarAction.generateCSLItemData(itemData);

				String bibl = CSL.makeAdhocBibliography("ieee", item).makeString();

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("IEEE"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				bibl = CSL.makeAdhocBibliography("acm-siggraph", item).makeString();

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("ACM"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				bibl = CSL.makeAdhocBibliography("apa", item).makeString();

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("APA"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				bibl = CSL.makeAdhocBibliography("chicago-author-date", item).makeString();

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Chicago"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				bibl = CSL.makeAdhocBibliography("council-of-science-editors", item).makeString();

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("CSE"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				bibl = CSL.makeAdhocBibliography("modern-language-association", item).makeString();

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("MLA"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
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

				borderlayout.setParent(window);

				window.onModal();
			}
		});

	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private MyTabConfig tabTahapanPenyusunan;
	private MyTabConfig tabPenulisan;
	private MyTabConfig tabPenilaianSks;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tabTahapanPenyusunan != null) { tabTahapanPenyusunan.setVisible(tbmuser != null && tbmuser.ambilDosen() == null); }
		if (tabPenulisan != null) { tabPenulisan.setVisible(tbmuser != null && tbmuser.ambilDosen() == null); }
		if (tabPenilaianSks != null) { tabPenilaianSks.setVisible(tbmuser != null && tbmuser.ambilDosen() == null); }

		Session session = HibernateUtil.currentSession();

		int count = ((Number) session.createCriteria(TahapanPenyusunanBuku.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {

			TahapanPenyusunanBuku tahapanPenyusunanBuku = new TahapanPenyusunanBuku();
			tahapanPenyusunanBuku.setNama("Pendahuluan");
			tahapanPenyusunanBuku.setProsentase(25.0);
			tahapanPenyusunanBuku.setKeterangan("Tahapan Pendahuluan");
			session.save(tahapanPenyusunanBuku);

			tahapanPenyusunanBuku = new TahapanPenyusunanBuku();
			tahapanPenyusunanBuku.setNama("50% dari Buku");
			tahapanPenyusunanBuku.setProsentase(50.0);
			tahapanPenyusunanBuku.setKeterangan("Tahapan 50% dari Buku");
			session.save(tahapanPenyusunanBuku);

			tahapanPenyusunanBuku = new TahapanPenyusunanBuku();
			tahapanPenyusunanBuku.setNama("Buku Jadi");
			tahapanPenyusunanBuku.setProsentase(75.0);
			tahapanPenyusunanBuku.setKeterangan("Buku telah jadi");
			session.save(tahapanPenyusunanBuku);

			tahapanPenyusunanBuku = new TahapanPenyusunanBuku();
			tahapanPenyusunanBuku.setNama("Persetujuan Penerbit");
			tahapanPenyusunanBuku.setProsentase(85.0);
			tahapanPenyusunanBuku.setKeterangan("Buku telah jadi dan telah mendapatkan persetujuan penerbit");
			session.save(tahapanPenyusunanBuku);

			tahapanPenyusunanBuku = new TahapanPenyusunanBuku();
			tahapanPenyusunanBuku.setNama("Dicetak (terbit)");
			tahapanPenyusunanBuku.setProsentase(100.0);
			tahapanPenyusunanBuku.setKeterangan("Buku telah jadi dan telah mendapatkan dicetak oleh penerbit");
			session.save(tahapanPenyusunanBuku);

		}

		count = ((Number) session.createCriteria(JenisPeredaranBuku.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {

			JenisPeredaranBuku jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku.setNama("Hanya berupa modul pengajaran / bukan buku");
			jenisPeredaranBuku.setKeterangan("Hanya berupa modul pengajaran / bukan buku");
			session.save(jenisPeredaranBuku);

			jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku.setNama("Merupakan satu Naskah Buku Ajar ada editor dan contributor");
			jenisPeredaranBuku.setKeterangan("Merupakan satu Naskah Buku Ajar ada editor dan contributor");
			session.save(jenisPeredaranBuku);

			jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku.setNama("Merupakan satu Naskah Buku Ajar tidak ada editor dan contributor");
			jenisPeredaranBuku.setKeterangan("Merupakan satu Naskah Buku Ajar tidak ada editor dan contributor");
			session.save(jenisPeredaranBuku);

			jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku.setNama("Merupakan satu Naskah Buku ajar ber ISBN");
			jenisPeredaranBuku.setKeterangan("Merupakan satu Naskah Buku ajar ber ISBN");
			session.save(jenisPeredaranBuku);

			jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku.setNama("Merupakan satu naskah buku referensi utuh tidak ada editor");
			jenisPeredaranBuku.setKeterangan("Merupakan satu naskah buku referensi utuh tidak ada editor");
			session.save(jenisPeredaranBuku);

			jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku.setNama(
					"Merupakan buku internasional (berbahasa Internasional yang diakui oleh PBB dan diedarkan secara internasional)");
			jenisPeredaranBuku.setKeterangan(
					"Merupakan buku internasional (berbahasa Internasional yang diakui oleh PBB dan diedarkan secara internasional)");
			session.save(jenisPeredaranBuku);

			jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku.setNama(
					"Merupakan terjemahan / saduran buku ilmiah yang diterbitkan dan diedarkan secara nasional");
			jenisPeredaranBuku.setKeterangan(
					"Merupakan terjemahan / saduran buku ilmiah yang diterbitkan dan diedarkan secara nasional");
			session.save(jenisPeredaranBuku);

			jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku.setNama(
					"Merupakan editan / suntingan buku ilmiah yang diterbitkan dan diedarkan secara nasional.");
			jenisPeredaranBuku.setKeterangan(
					"Merupakan editan / suntingan buku ilmiah yang diterbitkan dan diedarkan secara nasional.");
			session.save(jenisPeredaranBuku);

			jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku.setNama("Merupakan sebuah karya pengabdian pada masyarakat");
			jenisPeredaranBuku.setKeterangan(
					"Membuat/menulis karya pengabdian pada masyarakat yang tidak dipublikasikan Menulis 1 judul karya pengabdian, direncanakan terbit ber ISBN, ada kontrak penerbitan dan/atau sudah diterbitkan dan ber-ISBN");
			session.save(jenisPeredaranBuku);

			jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku
					.setNama("Merupakan sebuah judul karya pengabdian, ada editor, tiap chapter ada contributor");
			jenisPeredaranBuku.setKeterangan(
					"Merupakan sebuah judul karya pengabdian, ada editor, tiap chapter ada contributor yang tidak dipublikasikan Menulis 1 judul karya pengabdian, direncanakan terbit ber ISBN, ada kontrak penerbitan dan/atau sudah diterbitkan dan ber-ISBN");
			session.save(jenisPeredaranBuku);

			jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku.setNama(
					"Merupakan sebuah karya pengabdian yang dipakai sebagai Modul/Bahan Ajar oleh seorang Dosen (Tidak diterbitkan, tetapi digunakan oleh mahasiswa)");
			jenisPeredaranBuku.setKeterangan(
					"Merupakan sebuah karya pengabdian yang dipakai sebagai Modul/Bahan Ajar oleh seorang Dosen (Tidak diterbitkan, tetapi digunakan oleh mahasiswa)");
			session.save(jenisPeredaranBuku);

			jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku.setNama(
					"Merupakan sebuah artikel karya pengabdian kepada masyarakat pada buletin, majalah atau koran nasional");
			jenisPeredaranBuku.setKeterangan(
					"Merupakan sebuah artikel karya pengabdian kepada masyarakat pada buletin, majalah atau koran nasional");
			session.save(jenisPeredaranBuku);

			jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku.setNama(
					"Merupakan sebuah artikel karya pengabdian kepada masyarakat pada buletin, majalah atau koran lokal");
			jenisPeredaranBuku.setKeterangan(
					"Merupakan sebuah artikel karya pengabdian kepada masyarakat pada buletin, majalah atau koran lokal");
			session.save(jenisPeredaranBuku);
		}

		if (execution.getParameter("dosen") != null) {
			dosen = (Dosen) HibernateUtil.currentSession().createCriteria(Dosen.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("dosen")))).uniqueResult();
			searchdosen1.setValue(dosen.getNama());
			searchdosen1.setAttribute("myValue", dosen);
			searchdosen1.setAttribute("dosen", dosen);
			searchdosen1.setDisabled(true);
		}

		if (execution.getParameter("ases") != null) {
			ases = Boolean.parseBoolean(execution.getParameter("ases"));
		}

		System.out.println("dosen => " + execution.getParameter("dosen") + ", " + dosen);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor", "/img/corner.gif");
		Common.appendKeToolbar(exportKeOjs, add, comp);
		exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("bahan_ajar_terhubung_ke_dspace"));
		exportKeOjs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Intbox intbox = new Intbox(0);
				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (intbox.getValue() == 0) {
							MyMessageboxConfig.show(
									"Data tidak ditemukan, khusus untuk buku dengan pengarang dosen, dosen harus mempunya HOMEBASE PRODI terlebih dahulu sebelum bisa mempublikasikan ke dalam repository",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						onSearchDefault(arg0);
						LogLoginAction.tampilDpsaceLog();
					}
				});

				new Thread(new Runnable() {

					@SuppressWarnings("unchecked")
					@Override
					public void run() {
						try {
							String cookie = DspaceCommon.login();
							List<BukuBahanAjar> bukuBahanAjars = initCriteria(true).list();
							intbox.setValue(bukuBahanAjars.size());

							int rowIndex = 1;
							for (BukuBahanAjar bukuBahanAjar : bukuBahanAjars) {
								label.setValue("Sedang memproses data " + bukuBahanAjar.toString() + " ("
										+ Common.numberFormat.get().format((rowIndex++) * 100.0 / bukuBahanAjars.size())
										+ " %)");
								BukuBahanAjarAction.getDspace(cookie, bukuBahanAjar, true);
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
		Common.appendKeToolbar(batalExport, add, comp);
		batalExport.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("bahan_ajar_terhubung_ke_dspace"));
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
											onSearchDefault(arg0);
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
												List<BukuBahanAjar> bukuBahanAjars = initCriteria(true)
														.createAlias("dosenPengarang1", "dosenPengarang1")
														.add(Restrictions.isNotNull("dosenPengarang1.jurusan")).list();

												int rowIndex = 1;
												for (BukuBahanAjar bukuBahanAjar : bukuBahanAjars) {
													label.setValue("Sedang memproses data " + bukuBahanAjar.toString()
															+ " ("
															+ Common.numberFormat.get().format(
																	(rowIndex++) * 100.0 / bukuBahanAjars.size())
															+ " %)");
													DspaceInformation dspaceInformation = DspaceInformation
															.getDspaceInformation(BukuBahanAjar.class.getName(),
																	bukuBahanAjar.getId());
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

	        FilterLanjutHelper.setup(comp);
}

	public static void displayRow(Row arg0, final BukuBahanAjar bukuBahanAjar, final Pegawai pegawai,
			final Boolean ases) throws Exception {

		final MyDetail detail = new MyDetail();
		detail.setParent(arg0);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detail);
				if (detail.isOpen()) {

					Set<Long> treeMap = new HashSet<Long>();
					for (Dosen dosen : bukuBahanAjar.populateDosen().values()) {
						if (dosen.getPegawaiId() != null) {
							treeMap.add(dosen.getPegawaiId());
						}
					}

					if (ases && pegawai != null && treeMap.contains(pegawai.getId())) {

						ais.ui.util.MyButtonTabbox btnTabAsesor = ais.ui.util.MyButtonTabbox.buat(detail, "100%", new int[] { 0 });

						{
							org.zkoss.zul.Div panelPenilaian = btnTabAsesor.tambahTab(0, "Penilaian Asesor", "/img/svg/award.svg");
							PenilaianAsesorHelper.formNilai(pegawai, "bukuBahanAjar", bukuBahanAjar, null,
									bukuBahanAjar.getTahunAkademik(), bukuBahanAjar.getSemester(),
									"Buku ber-judul \"" + bukuBahanAjar.getNama() + "\"", PenilaianAsesor.PENULIS_BUKU,
									new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {}
									}).setParent(panelPenilaian);
						}
						btnTabAsesor.tambahTabLazy(1, "Lampiran Lain Buku", "/img/svg/folder-open-thin.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
							@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
								FileBukuAjarHelper fileBukuAjarHelper = new FileBukuAjarHelper(true);
								fileBukuAjarHelper.display(bukuBahanAjar, panel);
							}
						});

					} else {
						FileBukuAjarHelper fileBukuAjarHelper = new FileBukuAjarHelper(true);

						fileBukuAjarHelper.display(bukuBahanAjar, detail);
					}

				}

			}
		};

		detail.addEventListener("onOpen", eventListener);

		if (ases) {
			detail.setOpen(true);
			eventListener.onEvent(null);
		}

		Vbox vbox = new Vbox();
		vbox.setParent(arg0);

		RevisiHelper.createNewRevisi(BukuBahanAjar.class, bukuBahanAjar, bukuBahanAjar.getNama()).setParent(vbox);

		Vbox myvbox = new Vbox();
		myvbox.setParent(vbox);

		Hbox hbox = new Hbox();
		hbox.setParent(myvbox);
		LampiranLain.createDownloadUploadFileLain(hbox, bukuBahanAjar.getId(), LampiranLain.BUKU, LampiranLain.BUKU,
				true, null, null, false, false, false, false);

		myvbox = new Vbox();
		myvbox.setParent(vbox);

		hbox = new Hbox();
		hbox.setParent(myvbox);
		LampiranLain.createDownloadUploadFileLain(hbox, bukuBahanAjar.getId(), LampiranLain.COVER_BUKU, "Cover", true,
				null, null, false, false, false, false);

		if (bukuBahanAjar.getPengarangAdalahDosen()) {
			BukuBahanAjarAction.tampilkanInfoDosen(bukuBahanAjar, false).setParent(arg0);
		} else {
			new Label((bukuBahanAjar.getPengarang1() != null && !bukuBahanAjar.getPengarang1().trim().equals("")
					? bukuBahanAjar.getPengarang1().trim() + ", "
					: "")
					+ (bukuBahanAjar.getPengarang2() != null && !bukuBahanAjar.getPengarang2().trim().equals("")
							? bukuBahanAjar.getPengarang2().trim() + ", "
							: "")
					+ (bukuBahanAjar.getPengarang3() != null && !bukuBahanAjar.getPengarang3().trim().equals("")
							? bukuBahanAjar.getPengarang3().trim() + ", "
							: "")).setParent(arg0);
		}

		new Label(bukuBahanAjar.getIsbn()).setParent(arg0);
		new Label(bukuBahanAjar.getPenerbit()).setParent(arg0);
		new Label(bukuBahanAjar.getLink()).setParent(arg0);
		new Label(bukuBahanAjar.getTahun() + " / " + bukuBahanAjar.getTahunAkademik() + " / "
				+ bukuBahanAjar.getSemester()).setParent(arg0);
		new Label(bukuBahanAjar.getTahapanPenyusunanBuku() == null ? ""
				: bukuBahanAjar.getTahapanPenyusunanBuku().getNama()).setParent(arg0);
	}

	class BukuBahanAjarRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BukuBahanAjar bukuBahanAjar = (BukuBahanAjar) arg1;

			Dosen dosenPemimbing = (Dosen) searchdosen1.getAttribute("myValue");
			BukuBahanAjarAction.displayRow(arg0, bukuBahanAjar,
					dosenPemimbing == null || dosenPemimbing.getPegawaiId() == null ? null
							: new Pegawai(dosenPemimbing),
					ases);

			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/eye-icon.png");
			button.setOrient("vertical");
			button.setTooltiptext("Kutipan Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					BukuBahanAjarAction.tampilkanKutipan(bukuBahanAjar);
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(bukuBahanAjar);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
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

											Common.refreshDelete(bukuBahanAjar);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new BukuBahanAjar());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public static void onAddExternal(Event event, EventListener eventListener, BukuBahanAjar bukuBahanAjar)
			throws Exception {
		BukuBahanAjarAction bukuBahanAjarAction = new BukuBahanAjarAction();
		bukuBahanAjarAction.eventListener = eventListener;
		bukuBahanAjarAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(bukuBahanAjarAction.addWindow);
		bukuBahanAjarAction.addWindow.setHeight("98%");
		bukuBahanAjarAction.addWindow.setWidth("750px");

		bukuBahanAjarAction.init(bukuBahanAjar);

		bukuBahanAjarAction.addWindow.setVisible(true);
		bukuBahanAjarAction.addWindow.onModal();
	}

	public static DspaceInformation getDspaceBukuBahanAjar(String cookie, BukuBahanAjar bukuBahanAjar)
			throws Exception {
		Jurusan jurusan = bukuBahanAjar.getDosenPengarang1().getJurusan();

		String description = "Buku pada " + Common.getBahasaConfig("Jurusan") + " " + jurusan.getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", "Buku");
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", "Buku Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common.getKonfigurasi("dspace_label_collection_bukuBahanAjar_" + jurusan.getId(),
				"");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
				"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/collections");

	}

	public static DspaceInformation getDspace(String cookie, BukuBahanAjar bukuBahanAjar, boolean update)
			throws Exception {

		JSONArray jsonArray = new JSONArray();

		JSONObject jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.date.copyright");
		jsonMetadata.put("value",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonArray.put(jsonMetadata);

		Map<String, Dosen> map = bukuBahanAjar.populateDosen();
		for (Dosen dosen : map.values()) {
			String nama = dosen.getNama();

			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.contributor.author");
			jsonMetadata.put("value", nama);
			jsonArray.put(jsonMetadata);
		}

		if (!bukuBahanAjar.getPengarang1().trim().isEmpty()) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.contributor.author");
			jsonMetadata.put("value", bukuBahanAjar.getPengarang1());
			jsonArray.put(jsonMetadata);
		}

		if (!bukuBahanAjar.getPengarang2().trim().isEmpty()) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.contributor.author");
			jsonMetadata.put("value", bukuBahanAjar.getPengarang2());
			jsonArray.put(jsonMetadata);
		}

		if (!bukuBahanAjar.getPengarang3().trim().isEmpty()) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.contributor.author");
			jsonMetadata.put("value", bukuBahanAjar.getPengarang3());
			jsonArray.put(jsonMetadata);
		}

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description.abstract");
		jsonMetadata.put("value", bukuBahanAjar.getAbstrak());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.uri");
		jsonMetadata.put("value", bukuBahanAjar.getLink());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.title");
		jsonMetadata.put("value", bukuBahanAjar.getNama());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.publisher");
		jsonMetadata.put("value", bukuBahanAjar.getPenerbit());
		jsonArray.put(jsonMetadata);

		if (bukuBahanAjar.getTanggal() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.date.issued");
			jsonMetadata.put("value", Common.databaseDateFormat.get().format(bukuBahanAjar.getTanggal()));
			jsonArray.put(jsonMetadata);
		}

		LampiranLain lampiranLain = LampiranLain.ambil(bukuBahanAjar.getId(), LampiranLain.BUKU);
		if (lampiranLain != null) {
			String uri = lampiranLain.createLinkUri(false);
			if (uri != null && !uri.trim().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", uri);
				jsonArray.put(jsonMetadata);
			}
		}

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("metadata", jsonArray);

		DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie, bukuBahanAjar,
				jsonPost.toString(), jsonArray.toString(), update, "items",
				"collections/" + getDspaceBukuBahanAjar(cookie, bukuBahanAjar) + "/items", "items/{uuid}/metadata");

		Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

		@SuppressWarnings("unchecked")
		List<FileBukuBahanAjar> fileBukuBahanAjars = streamingSession.createCriteria(FileBukuBahanAjar.class)
				.addOrder(Order.asc("id")).add(Restrictions.eq("bukuBahanAjar", bukuBahanAjar.getId())).list();
		for (FileBukuBahanAjar fileBukuBahanAjar : fileBukuBahanAjars) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), fileBukuBahanAjar,
					"File \"" + bukuBahanAjar.getNama() + "\"");
		}
		StreamingHibernateUtil.getInstance().closeSession();

		if (lampiranLain != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
					"File " + bukuBahanAjar.getNama());
		}
		lampiranLain = LampiranLain.ambil(bukuBahanAjar.getId(), LampiranLain.COVER_BUKU);

		if (lampiranLain != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
					"Cover " + bukuBahanAjar.getNama());
		}

		return dspaceInformation;
	}

	private void init(BukuBahanAjar bukuBahanAjar) throws Exception {
		this.bukuBahanAjar = bukuBahanAjar;
		addWindow.setTitle(bukuBahanAjar.getId() == null ? "Tambah Buku" : "Ubah Buku");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		final ais.ui.util.MyButtonTabbox btnTabBuku = ais.ui.util.MyButtonTabbox.buat(center, "100%", new int[] { 0 });

		org.zkoss.zul.Div panelData = btnTabBuku.tambahTab(0, "Data Buku", "/img/svg/book.svg");
		final org.zkoss.zul.Div panelLampiran = btnTabBuku.tambahTab(1, "Lampiran Lain", "/img/svg/folder-open-thin.svg");

		btnTabBuku.onSetiapPilih(1, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(panelLampiran);
				if (!onSave(arg0)) {
					btnTabBuku.pilih(0);
					Common.clear(panelLampiran);
					return;
				}
				FileBukuAjarHelper fileBukuAjarHelper = new FileBukuAjarHelper(delete);
				fileBukuAjarHelper.display(BukuBahanAjarAction.this.bukuBahanAjar, panelLampiran);
			}
		});

		Borderlayout myborderlayout = new ais.ui.util.MyBorderlayout();
		myborderlayout.setParent(panelData);
		Center mycenter = new Center();
		mycenter.setParent(myborderlayout);
		ais.ui.util.ZkCompat.setFlex(mycenter, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(mycenter);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul"));
		row.appendChild(nama = new Textbox(bukuBahanAjar.getNama() == null ? "" : bukuBahanAjar.getNama()));
		nama.setWidth("90%");

		rowPenerbit = new MyFormRow();
		rowPenerbit.setStyle("border:0px;background: transparent;");
		rowPenerbit.setParent(rows);
		rowPenerbit.appendChild(new ais.ui.util.MyLabelConfig("Penerbit"));
		rowPenerbit.appendChild(penerbit = new Textbox(bukuBahanAjar.getPenerbit()));
		penerbit.setWidth("90%");

		rowTahunTerbit = new MyFormRow();
		rowTahunTerbit.setStyle("border:0px;background: transparent;");
		rowTahunTerbit.setParent(rows);
		rowTahunTerbit.appendChild(new ais.ui.util.MyLabelConfig("Tahun Terbit"));
		rowTahunTerbit.appendChild(tahun = new Intbox(bukuBahanAjar.getTahun()));
		tahun.setWidth("90%");

		rowTanggalTerbit = new MyFormRow();
		rowTanggalTerbit.setStyle("border:0px;background: transparent;");
		rowTanggalTerbit.setParent(rows);
		rowTanggalTerbit.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Terbit"));
		rowTanggalTerbit.appendChild(tanggal = new MyDatebox(bukuBahanAjar.getTanggal()));

		rowISBN = new MyFormRow();
		rowISBN.setStyle("border:0px;background: transparent;");
		rowISBN.setParent(rows);
		rowISBN.appendChild(new ais.ui.util.MyLabelConfig("ISBN"));
		rowISBN.appendChild(isbn = new Textbox(bukuBahanAjar.getIsbn()));
		isbn.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Buku Ajar (PDF) jika ada"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, bukuBahanAjar.getId(), LampiranLain.BUKU, LampiranLain.BUKU,
				true, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cover Buku Ajar (JPG) jika ada"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, bukuBahanAjar.getId(), LampiranLain.COVER_BUKU, "Cover Buku",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswaCover = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(pengarangAdalahDosen = new MyCheckboxConfig(
				"Pengarang buku ini merupakan dosen " + Common.getKonfigurasi("label_universitas", "").getNilai()));
		pengarangAdalahDosen.setChecked(bukuBahanAjar.getPengarangAdalahDosen());

		if (dosen != null) {
			pengarangAdalahDosen.setChecked(true);
			pengarangAdalahDosen.setDisabled(true);
		}

		final MyFormRow rowPengarang1 = new MyFormRow();
		rowPengarang1.setStyle("border:0px;background: transparent;");
		rowPengarang1.setParent(rows);
		rowPengarang1.appendChild(new ais.ui.util.MyLabelConfig("Pengarang 1"));
		rowPengarang1.appendChild(pengarang1 = new Textbox(bukuBahanAjar.getPengarang1()));
		pengarang1.setWidth("90%");

		final MyFormRow rowPengarang2 = new MyFormRow();
		rowPengarang2.setStyle("border:0px;background: transparent;");
		rowPengarang2.setParent(rows);
		rowPengarang2.appendChild(new ais.ui.util.MyLabelConfig("Pengarang 2"));
		rowPengarang2.appendChild(pengarang2 = new Textbox(bukuBahanAjar.getPengarang2()));
		pengarang2.setWidth("90%");

		final MyFormRow rowPengarang3 = new MyFormRow();
		rowPengarang3.setStyle("border:0px;background: transparent;");
		rowPengarang3.setParent(rows);
		rowPengarang3.appendChild(new ais.ui.util.MyLabelConfig("Pengarang 3"));
		rowPengarang3.appendChild(pengarang3 = new Textbox(bukuBahanAjar.getPengarang1()));
		pengarang3.setWidth("90%");

		final MyFormRow rowPengarangDosen1 = new MyFormRow();
		rowPengarangDosen1.setStyle("border:0px;background: transparent;");
		rowPengarangDosen1.setParent(rows);
		rowPengarangDosen1.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pengarang I"));
		rowPengarangDosen1.appendChild(dosenPengarang1 = new AmbilDataDosenBanbox(false));
		dosenPengarang1.setValue(
				bukuBahanAjar.getDosenPengarang1() == null ? "" : (bukuBahanAjar.getDosenPengarang1().getNama()));

		if (bukuBahanAjar.getDosenPengarang1() != null) {
			dosenPengarang1.setAttribute("myValue", bukuBahanAjar.getDosenPengarang1());
		} else if (dosen != null) {
			dosenPengarang1.setValue(dosen.getNama());
			dosenPengarang1.setAttribute("myValue", dosen);
			dosenPengarang1.setDisabled(true);
		}

		dosenPengarang1.setWidth("90%");

		final MyFormRow rowPengarangDosen2 = new MyFormRow();
		rowPengarangDosen2.setStyle("border:0px;background: transparent;");
		rowPengarangDosen2.setParent(rows);
		rowPengarangDosen2.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pengarang II"));
		rowPengarangDosen2.appendChild(dosenPengarang2 = new AmbilDataDosenBanbox(false));
		dosenPengarang2.setValue(
				bukuBahanAjar.getDosenPengarang2() == null ? "" : (bukuBahanAjar.getDosenPengarang2().getNama()));

		dosenPengarang2.setAttribute("myValue", bukuBahanAjar.getDosenPengarang2());
		dosenPengarang2.setWidth("90%");

		final MyFormRow rowPengarangDosen3 = new MyFormRow();
		rowPengarangDosen3.setStyle("border:0px;background: transparent;");
		rowPengarangDosen3.setParent(rows);
		rowPengarangDosen3.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pengarang III"));
		rowPengarangDosen3.appendChild(dosenPengarang3 = new AmbilDataDosenBanbox(false));
		dosenPengarang3.setValue(
				bukuBahanAjar.getDosenPengarang3() == null ? "" : (bukuBahanAjar.getDosenPengarang3().getNama()));

		dosenPengarang3.setAttribute("myValue", bukuBahanAjar.getDosenPengarang3());
		dosenPengarang3.setWidth("90%");

		final MyFormRow rowPengarangDosen4 = new MyFormRow();
		rowPengarangDosen4.setStyle("border:0px;background: transparent;");
		rowPengarangDosen4.setParent(rows);
		rowPengarangDosen4.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pengarang IV"));
		rowPengarangDosen4.appendChild(dosenPengarang4 = new AmbilDataDosenBanbox(false));
		dosenPengarang4.setValue(
				bukuBahanAjar.getDosenPengarang4() == null ? "" : (bukuBahanAjar.getDosenPengarang4().getNama()));

		dosenPengarang4.setAttribute("myValue", bukuBahanAjar.getDosenPengarang4());
		dosenPengarang4.setWidth("90%");

		final MyFormRow rowPengarangDosen5 = new MyFormRow();
		rowPengarangDosen5.setStyle("border:0px;background: transparent;");
		rowPengarangDosen5.setParent(rows);
		rowPengarangDosen5.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pengarang V"));
		rowPengarangDosen5.appendChild(dosenPengarang5 = new AmbilDataDosenBanbox(false));
		dosenPengarang5.setValue(
				bukuBahanAjar.getDosenPengarang5() == null ? "" : (bukuBahanAjar.getDosenPengarang5().getNama()));

		dosenPengarang5.setAttribute("myValue", bukuBahanAjar.getDosenPengarang5());
		dosenPengarang5.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahapan Penyusunan Buku"));
		row.appendChild(tahapanPenyusunanBuku = new Combobox());
		Common.insertComboDanSemua(tahapanPenyusunanBuku, new String[] { "nama", "prosentase" }, "keterangan",
				TahapanPenyusunanBuku.class, "== Tidak ada tahapan penyusunan buku ==",
				Restrictions.sqlRestriction("true"));
		Common.selectComboItem(tahapanPenyusunanBuku, bukuBahanAjar.getTahapanPenyusunanBuku());
		tahapanPenyusunanBuku.setWidth("90%");
		tahapanPenyusunanBuku.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penulisan dan Peredaran"));
		row.appendChild(jenisPeredaranBuku = new Combobox());
		jenisPeredaranBuku.setWidth("90%");
		Common.insertComboDanSemua(jenisPeredaranBuku, new String[] { "nama" }, "keterangan", JenisPeredaranBuku.class,
				"== Tidak ada proses penulisan dan peredaran buku ==", Restrictions.sqlRestriction("true"));
		Common.selectComboItem(jenisPeredaranBuku, bukuBahanAjar.getJenisPeredaranBuku());
		jenisPeredaranBuku.setReadonly(true);

		final MyFormRow rowLama = new MyFormRow();
		rowLama.setVisible(dosenPengarang1.getAttribute("myValue") != null);
		rowLama.setStyle("border:0px;background: transparent;");
		rowLama.setParent(rows);
		rowLama.appendChild(new ais.ui.util.MyLabelConfig("Lama Penulisan"));

		Vbox vbox = new Vbox();
		rowLama.appendChild(vbox);
		vbox.appendChild(masaPenugasan = new Textbox(bukuBahanAjar.getMasaPenugasan()));
		vbox.appendChild(new MyLabelConfig(
				"*) Jika penulis-nya adalah dosen, lama penulisan harus diisi. Misal: 1 tahun, 6 bulan, 2 minggu, 5 hari, 8 jam, 1 semester"));
		masaPenugasan.setWidth("90%");

		dosenPengarang1.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowLama.setVisible(dosenPengarang1.getAttribute("myValue") != null);
			}
		});

		EventListener eventListenerDosen = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowPengarang1.setVisible(!pengarangAdalahDosen.isChecked());
				rowPengarang2.setVisible(!pengarangAdalahDosen.isChecked());
				rowPengarang3.setVisible(!pengarangAdalahDosen.isChecked());

				rowPengarangDosen1.setVisible(pengarangAdalahDosen.isChecked());
				rowPengarangDosen2.setVisible(pengarangAdalahDosen.isChecked());
				rowPengarangDosen3.setVisible(pengarangAdalahDosen.isChecked());
				rowPengarangDosen4.setVisible(pengarangAdalahDosen.isChecked());
				rowPengarangDosen5.setVisible(pengarangAdalahDosen.isChecked());
			}
		};

		eventListenerDosen.onEvent(null);
		pengarangAdalahDosen.addEventListener("onClick", eventListenerDosen);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Link"));
		row.appendChild(link = new Textbox(bukuBahanAjar.getLink()));
		link.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Abstrak"));
		row.appendChild(abstrak = new Textbox(bukuBahanAjar.getAbstrak()));
		abstrak.setWidth("90%");
		abstrak.setRows(3);
		abstrak.setRows(4);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama editor dan kontributor"));
		row.appendChild(editorDanKontributor = new Textbox(bukuBahanAjar.getEditorDanKontributor()));
		editorDanKontributor.setWidth("90%");
		editorDanKontributor.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Publikasi di Tahun Akademik (*)"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		if (bukuBahanAjar.getTahunAkademik() != null) {
			Common.selectComboItem(tahunAkademik, bukuBahanAjar.getTahunAkademik());
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

		Common.selectComboItem(semester, bukuBahanAjar.getSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Publikasi di Semester (*)"));
		row.appendChild(semester);
		semester.setReadonly(true);

		tanggal.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tanggal.getValue() != null) {
					Common.selectComboItem(tahunAkademik, Common.getCurrentTahunAkademik(tanggal.getValue()));
					Common.selectComboItem(semester,
							Common.isNowSemensterGanjil(tanggal.getValue()) ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(bukuBahanAjar.getKeterangan() == null ? "" : bukuBahanAjar.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(4);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);

					if (eventListener != null) {
						eventListener.onEvent(new Event("", addWindow, BukuBahanAjarAction.this.bukuBahanAjar));
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Judul Buku",
					"Kolom Judul Buku belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Judul Buku.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (dosenPengarang1.getAttribute("myValue") != null && masaPenugasan.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Jika penulis-nya adalah dosen, lama penulisan harus diisi. Misal: 1 tahun, 6 bulan, 2 minggu, 5 hari, 8 jam, 1 semester",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		BukuBahanAjarDao bukuBahanAjarDao = DaoFactory.getInstance().getBukuBahanAjarDao();
		if (bukuBahanAjar.getId() != null) {
			bukuBahanAjar = bukuBahanAjarDao.load(bukuBahanAjar.getId());

		}

		bukuBahanAjar.setJenisPeredaranBuku((JenisPeredaranBuku) (jenisPeredaranBuku.getSelectedItem() == null ? null
				: jenisPeredaranBuku.getSelectedItem().getValue()));
		bukuBahanAjar.setMasaPenugasan(masaPenugasan.getValue());
		bukuBahanAjar.setTanggal(tanggal.getValue());
		bukuBahanAjar.setAbstrak(abstrak.getValue());
		bukuBahanAjar.setTahun(tahun.getValue());
		bukuBahanAjar.setNama(nama.getValue());
		bukuBahanAjar.setKeterangan(keterangan.getValue());
		bukuBahanAjar.setIsbn(isbn.getValue());
		bukuBahanAjar.setLink(link.getValue());
		bukuBahanAjar.setPenerbit(penerbit.getValue());
		bukuBahanAjar.setPengarang1(pengarang1.getValue());
		bukuBahanAjar.setPengarang2(pengarang2.getValue());
		bukuBahanAjar.setPengarang3(pengarang3.getValue());
		bukuBahanAjar.setEditorDanKontributor(editorDanKontributor.getValue());

		bukuBahanAjar.setDosenPengarang1((Dosen) dosenPengarang1.getAttribute("myValue"));
		bukuBahanAjar.setDosenPengarang2((Dosen) dosenPengarang2.getAttribute("myValue"));

		bukuBahanAjar.setDosenPengarang3((Dosen) dosenPengarang3.getAttribute("myValue"));
		bukuBahanAjar.setDosenPengarang4((Dosen) dosenPengarang4.getAttribute("myValue"));
		bukuBahanAjar.setDosenPengarang5((Dosen) dosenPengarang5.getAttribute("myValue"));

		bukuBahanAjar.setTahapanPenyusunanBuku(
				(TahapanPenyusunanBuku) (tahapanPenyusunanBuku.getSelectedItem() == null ? null
						: tahapanPenyusunanBuku.getSelectedItem().getValue()));

		bukuBahanAjar.setSemester((String) semester.getSelectedItem().getValue());
		bukuBahanAjar.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());

		if (bukuBahanAjar.getId() != null) {
			bukuBahanAjarDao.update(bukuBahanAjar);
		} else {
			bukuBahanAjarDao.save(bukuBahanAjar);
		}

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				Session session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(bukuBahanAjar.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (lainMahasiswaCover != null && lainMahasiswaCover.getId() != null) {
			try {
				Session session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswaCover);
				lainMahasiswaCover.setRef(bukuBahanAjar.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswaCover);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		return true;
	}

	public static Hbox tampilkanInfoDosen(BukuBahanAjar bukuBahanAjar, boolean tampilkanAsesor) throws Exception {
		Hbox hbox = new Hbox();

		for (Dosen dosen : bukuBahanAjar.populateDosen().values()) {
			Vbox vbox = new Vbox();
			vbox.setParent(hbox);
			CommonMedia.tampilkanGambarKecil(dosen).setParent(vbox);
			new Label(dosen.getNama()).setParent(vbox);
		}

		return hbox;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Dosen dosenPemimbing = (Dosen) searchdosen1.getAttribute("myValue");

		Criterion criterion = Restrictions.eq("dosenPengarang1", dosenPemimbing);
		criterion = Restrictions.or(criterion, Restrictions.eq("dosenPengarang2", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosenPengarang3", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosenPengarang4", dosenPemimbing));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosenPengarang5", dosenPemimbing));

		Criteria criteria = session.createCriteria(BukuBahanAjar.class)
				.add(dosenPemimbing == null ? Restrictions.sqlRestriction("1=1") : criterion);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria

				.add(searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
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

		if (searchnama == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);

		List<BukuBahanAjar> bukuBahanAjar = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(bukuBahanAjar);
		grid.setRowRenderer(new BukuBahanAjarRenderer());
		grid.setModelCheckMobile(strset);

	}

	public static CSLItemData generateCSLItemData(BukuBahanAjar itemData) {
		CSLItemDataBuilder builder = new CSLItemDataBuilder().type(CSLType.BOOK).title(itemData.getNama());
		if (itemData.getPengarang1() != null && !itemData.getPengarang1().trim().isEmpty()) {
			String[] pp = itemData.getPengarang1().split(" ", 1);
			String given = pp[0];
			String family = pp.length > 1 ? pp[1] : "";
			builder.author(given, family);
		}
		if (itemData.getPengarang2() != null && !itemData.getPengarang2().trim().isEmpty()) {
			String[] pp = itemData.getPengarang2().split(" ", 1);
			String given = pp[0];
			String family = pp.length > 1 ? pp[1] : "";
			builder.author(given, family);
		}
		if (itemData.getPengarang3() != null && !itemData.getPengarang3().trim().isEmpty()) {
			String[] pp = itemData.getPengarang3().split(" ", 1);
			String given = pp[0];
			String family = pp.length > 1 ? pp[1] : "";
			builder.author(given, family);
		}

		for (Dosen dosen : itemData.populateDosen().values()) {
			String[] pp = dosen.getNama().split(" ", 1);
			String given = pp[0];
			String family = pp.length > 1 ? pp[1] : "";
			builder.author(given, family);
		}

		if (itemData.getTanggal() != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(itemData.getTanggal());
			builder.issued(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DATE));
		}

		if (itemData.getIsbn() != null && !itemData.getIsbn().trim().isEmpty()) {
			builder.ISBN(itemData.getIsbn());
		}
		if (itemData.getIssn() != null && !itemData.getIssn().trim().isEmpty()) {
			builder.ISSN(itemData.getIssn());
		}
		if (itemData.getAbstrak() != null && !itemData.getAbstrak().trim().isEmpty()) {
			builder.abstrct(itemData.getAbstrak());
		}
		if (itemData.getPenerbit() != null && !itemData.getPenerbit().trim().isEmpty()) {
			builder.publisher(itemData.getPenerbit());
		}

		CSLItemData item = builder.build();
		return item;
	}

}
