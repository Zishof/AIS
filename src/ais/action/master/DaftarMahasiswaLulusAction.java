package ais.action.master;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
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
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.pmb.VerifikasiPMBHelper;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.PmbArkatama;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiBerkas;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.JenisSekolahMahasiswaBaru;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jurusan;
import ais.database.model.JurusanSekolahMahasiswaBaru;
import ais.database.model.KelompokJenisSeleksi;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Paket;
import ais.database.model.PerguruanTinggi;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.VerifikasiKelengkapanCalonMahasiswa;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelConfigTitikDua;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class DaftarMahasiswaLulusAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchnoreg;

	private Textbox searchujian;
	private Combobox searchTahunAjaran;
	private Combobox searchGelombang;
	private Combobox searchJenisSeleksi;
	private Combobox searchJenisSekolahMahasiswaBaru;
	private Combobox searchJurusanSekolahMahasiswaBaru;
	private Combobox searchPaket;
	private Combobox searchProdiPilihan;
	private Combobox searchProdiLulus;
	private MyCheckboxConfig tampilkanYgSudahBayar;
	private MyCheckboxConfig tampilkanYgSudahBayarDaftarUlang;
	private MyCheckboxConfig tampilkanYgSudahLunasDaftarUlang;
	private MyCheckboxConfig tampilkanYgBelumLunasDaftarUlang;
	private MyCheckboxConfig tampilkanYgSudahdapatNIM;
	private MyCheckboxConfig blmDiterima;
	private MyCheckboxConfig tampilkanYgBelumBayar;
	private MyCheckboxConfig tampilkanYgBelumBayarDaftarUlang;
	private MyCheckboxConfig tampilkanYgBelumdapatNIM;
	private MyCheckboxConfig mengisiFormTambahan;

	private MyCheckboxConfig belumUploadBerkas;
	private MyCheckboxConfig telahUploadBerkas;
	private MyCheckboxConfig belumLolosBerkas;
	private MyCheckboxConfig telahLolosBerkas;

	private MyCheckboxConfig telahLogin;

	private Radiogroup pilihan;

	private boolean edit = false;

	private BiodataCalonMahasiswa biodataCalonMahasiswa;

//	private MyCheckboxConfig luluskanPilihan1;

	private MyToolbarbuttonConfig find;

	public static String[] contents = new String[] { "id", "noRegistrasi", "noUjian", "nama", "prodi1", "prodi2",
			"prodi3", "prodi4", "prodi5", "prodiLulus", "program", "ditolak", "keterangan" };
	private PerguruanTinggi selectedPerguruanTinggi;
	private Combobox program;
	private Textbox keterangan;
	private MyDatebox tanggalDiterima;
	private Tabpanel chekUpload;

	public void onCheck(Event event) {

		if (chekUpload.getChildren().size() == 0) {
			MyInclude halaman = new MyInclude("/pages/master/biodata_calon_mahasiswa_punya_verifikasi_berkas.zul");
			halaman.setHeight("100%");
			halaman.setWidth("100%");
			chekUpload.appendChild(halaman);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private boolean integrasi_pmb_arkatama = false;
	private Combobox jenisSeleksi;
	private Row rowJalurPenerimaan;
	private Combobox kelompokJenisSeleksi;
	private Combobox gelombangPendaftaran;
	private Combobox statusAwalDiterima;
	private AmbilDataMahasiswaBanbox mahasiswa;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
		selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		Common.insertCombo(searchJenisSeleksi, "nama", "deskripsi", JenisSeleksi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchJenisSekolahMahasiswaBaru, "nama", "keterangan", JenisSekolahMahasiswaBaru.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchJurusanSekolahMahasiswaBaru, "nama", "keterangan", JurusanSekolahMahasiswaBaru.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchPaket, "nama", "keterangan", Paket.class,
				selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi),
								Restrictions.isNull("perguruanTinggi")),
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchProdiPilihan, "nama", "fakultas", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchProdiLulus, "nama", "fakultas", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

		Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);

		EventListener gelombangEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.insertComboDanSemua(searchGelombang, "nama", GelombangPendaftaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								searchTahunAjaran.getSelectedItem() == null
										|| searchTahunAjaran.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("tahunAkademik",
														searchTahunAjaran.getSelectedItem().getValue())));
				searchGelombang.setReadonly(true);
				searchGelombang.setSelectedIndex(searchGelombang.getChildren().size() - 1);
			}
		};

		gelombangEventListener.onEvent(null);
		searchTahunAjaran.addEventListener("onChange", gelombangEventListener);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(BiodataCalonMahasiswa.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, BiodataCalonMahasiswa.class, contents);
		if (upload != null) { upload.setVisible(edit); }
		Common.appendKeToolbar(upload, find, comp);

		if (integrasi_pmb_arkatama = Common.bolehKonfigurasi("integrasi_pmb_arkatama", Konfigurasi.TIDAK_AKTIF)) {
			MyToolbarbuttonConfig singkronDenganMhs = new MyToolbarbuttonConfig("Kirimkan Lolos Berkas ke Feeder PMB",
					"/img/svg/check2.svg");
			singkronDenganMhs.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					final List<String> hasils = new ArrayList<String>();
					final Label label = Common.displayLoadBar(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							String h = "";
							for (String s : hasils) {
								h += h.isEmpty() ? s : "\n" + s;
							}
							MyMessageboxConfig.show("Hasil :\n" + h, "Hasil", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
						}
					});

					new Thread(new Runnable() {

						@SuppressWarnings("unchecked")
						@Override
						public void run() {
							try {

							List<Long> longs = initCriteria(true).add(Restrictions.isNotNull("prodiLulus"))
									.add(Restrictions.eq("ditolak", false)).add(Restrictions.eq("mundur", false))
									.add(Restrictions.ne("pinPassword", "")).add(Restrictions.ne("pinPassword", ""))
									.list();

							int size = longs.size();
							int index = 0;
							for (Long biodataCalonMahasiswaId : longs) {
								index++;
								try {

									BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
											.ambil(BiodataCalonMahasiswa.class.getName(), biodataCalonMahasiswaId);

									if (biodataCalonMahasiswa != null) {
										Session session = HibernateUtil.currentNativeSession();
										session.refresh(biodataCalonMahasiswa);
										GelombangPendaftaran gelombangPendaftaran = biodataCalonMahasiswa
												.getGelombangPendaftaran();
										session.refresh(gelombangPendaftaran);
										boolean verified = true;
										List<String> belums = new ArrayList<String>();

										Set<VerifikasiKelengkapanCalonMahasiswa> verifikasiKelengkapanCalonMahasiswasTemp = gelombangPendaftaran
												.getVerifikasiKelengkapanCalonMahasiswas();

										JenisSeleksi jenisSeleksi = biodataCalonMahasiswa
												.getJenisSeleksiDipilih() != null
														? biodataCalonMahasiswa.getJenisSeleksiDipilih()
														: biodataCalonMahasiswa.getJenisSeleksi();
										if (jenisSeleksi != null) {
											session.refresh(jenisSeleksi);
											if (!jenisSeleksi.getVerifikasiKelengkapanCalonMahasiswas().isEmpty()) {
												verifikasiKelengkapanCalonMahasiswasTemp = jenisSeleksi
														.getVerifikasiKelengkapanCalonMahasiswas();
											}
										}

										List<VerifikasiKelengkapanCalonMahasiswa> verifikasiKelengkapanCalonMahasiswas = new ArrayList<VerifikasiKelengkapanCalonMahasiswa>(
												verifikasiKelengkapanCalonMahasiswasTemp);

										try {
											Collections.sort(verifikasiKelengkapanCalonMahasiswas);
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/DaftarMahasiswaLulusAction.java:313");
											// TODO: handle exception
										}

										for (final VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa : verifikasiKelengkapanCalonMahasiswas) {
											if (verifikasiKelengkapanCalonMahasiswa.getAktif()) {
												BiodataCalonMahasiswaPunyaVerifikasiBerkas biodataCalonMahasiswaPunyaVerifikasiBerkas = (BiodataCalonMahasiswaPunyaVerifikasiBerkas) session
														.createCriteria(
																BiodataCalonMahasiswaPunyaVerifikasiBerkas.class)
														.add(Restrictions.eq("verifikasiKelengkapanCalonMahasiswa",
																verifikasiKelengkapanCalonMahasiswa))
														.add(Restrictions.eq("biodataCalonMahasiswa",
																biodataCalonMahasiswa))
														.setMaxResults(1).uniqueResult();

												boolean apakahSudah = (biodataCalonMahasiswaPunyaVerifikasiBerkas != null
														&& biodataCalonMahasiswaPunyaVerifikasiBerkas.getVerified());
												if (!apakahSudah) {
													belums.add("untuk calon mahasiswa "
															+ biodataCalonMahasiswa.getNama() + " "
															+ biodataCalonMahasiswa.getNoRegistrasi() + ", "
															+ verifikasiKelengkapanCalonMahasiswa.getNama()
															+ " belum diverifikasi");
												}
												verified &= apakahSudah;
											}
										}

										if (verified) {
											label.setValue("Kirimkan Lolos Berkas ke Feeder PMB "
													+ biodataCalonMahasiswa.getNoRegistrasi() + "-"
													+ biodataCalonMahasiswa.getNama() + " ("
													+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

											PmbArkatama.doPostLolos(biodataCalonMahasiswa, hasils);

											session.getTransaction().begin();
											session.update(biodataCalonMahasiswa);
											session.getTransaction().commit();
										} else {
											hasils.addAll(belums);
										}

										// session.disconnect();
										if (session.isOpen()) {session.disconnect();session.close();}
									}

								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
								}

								HibernateUtil.closeSession();
							}
							longs.clear();
							longs = null;
							label.setValue("");
													} finally {
								ais.database.hibernate.HibernateUtil.closeSession();
							}
						}
					}).start();

				}
			});
			Common.appendKeToolbar(singkronDenganMhs, find, comp);

		}
	        FilterLanjutHelper.setup(comp);
}

	class DaftarMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object arg1) throws Exception {
			row.setValign("top");
			// TODO Auto-generated method stub
			final BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
					.ambil(BiodataCalonMahasiswa.class.getName(), (Serializable) arg1);
			if (biodataCalonMahasiswa == null) {
				row.setVisible(false);
				return;
			}
			final MyToolbarbuttonConfig checkbox = new MyToolbarbuttonConfig("Diterima/Ditolak/Mundur",
					"/img/svg/check2.svg");

			MyDetail detail = new MyDetail();
			detail.setOpen(true);
			detail.setParent(row);
			final Hbox hbox = new Hbox();
			hbox.setParent(detail);

			CommonMedia.tampilkanGambarKecil(biodataCalonMahasiswa).setParent(row);

			Vbox aa;
			(aa = RevisiHelper.createNewRevisi(BiodataCalonMahasiswa.class, biodataCalonMahasiswa,
					biodataCalonMahasiswa.getNoRegistrasi())).setParent(row);

			if (integrasi_pmb_arkatama) {
				aa.appendChild(new MyLabelAgakKecilBold(biodataCalonMahasiswa.getPinPassword()));
			}

			biodataCalonMahasiswa.tampilkanHp(aa);
			biodataCalonMahasiswa.tampilkanEmail(aa);

			new Label(biodataCalonMahasiswa.getNoUjian()).setParent(row);
			new Label(biodataCalonMahasiswa.getNama()).setParent(row);
			String str = "<ol style='font-size:9px;'>";
			if (biodataCalonMahasiswa.getProdi1() != null) {
				str += "<li>" + biodataCalonMahasiswa.getProdi1().getNama() + "</li>";
			}
			if (biodataCalonMahasiswa.getProdi2() != null) {
				str += "<li>" + biodataCalonMahasiswa.getProdi2().getNama() + "</li>";
			}
			if (biodataCalonMahasiswa.getProdi3() != null) {
				str += "<li>" + biodataCalonMahasiswa.getProdi3().getNama() + "</li>";
			}
			if (biodataCalonMahasiswa.getProdi4() != null) {
				str += "<li>" + biodataCalonMahasiswa.getProdi4().getNama() + "</li>";
			}
			if (biodataCalonMahasiswa.getProdi5() != null) {
				str += "<li>" + biodataCalonMahasiswa.getProdi5().getNama() + "</li>";
			}
			str += "</ol>";
			new ais.ui.util.MyHtml(str).setParent(row);

			Vbox vbox = new Vbox();

			vbox.setParent(row);

			new Label(biodataCalonMahasiswa.getMundur() ? "Mengundurkan diri"
					: biodataCalonMahasiswa.getDitolak() ? "Tidak diterima (ditolak)"
							: biodataCalonMahasiswa.getProdiLulus() == null ? "Belum Diterima"
									: biodataCalonMahasiswa.getProdiLulus().getNama())
					.setParent(vbox);

			new Label(biodataCalonMahasiswa.getTanggalDiterima() == null ? ""
					: Common.dateFormat.get().format(biodataCalonMahasiswa.getTanggalDiterima())).setParent(vbox);

			new Label(biodataCalonMahasiswa.getProgram()).setParent(row);

			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					init(biodataCalonMahasiswa);
					addWindow.setVisible(true);
					addWindow.onModal();

				}
			});

			checkbox.setParent(row);
			row.setValign("top");
			row.setAttribute("checkbox", checkbox);

			// Ambil ID sebelum masuk anonymous class agar tetap bisa diakses
			// saat session asal sudah ditutup (cegah LazyInitializationException)
			final Long _bioId = biodataCalonMahasiswa.getId();

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// Session DEDIKASI utk timer ini (JANGAN currentSession bersama): criteria di
					// bawah meng-autoflush; bila entity/koleksi (freshBio dll) juga ter-asosiasi ke
					// session LAIN yang masih terbuka -> "Illegal attempt to associate a collection
					// with two open sessions". Membuka & menutup session sendiri di sini
					// meng-isolasi seluruh entity ke satu session saja.
					Session session = null;
					org.hibernate.Transaction txLulus = null;
					try {
						session = HibernateUtil.openSession();
						txLulus = session.beginTransaction();
						// Muat ulang biodataCalonMahasiswa di session ini agar koleksi lazy dapat
						// diinisialisasi (cegah LazyInitializationException saat timer berjalan).
						final BiodataCalonMahasiswa freshBio = (BiodataCalonMahasiswa) session.get(
								BiodataCalonMahasiswa.class, _bioId);
					if (freshBio == null) return;
					GelombangPendaftaran gel = freshBio.getGelombangPendaftaran();
					if (gel == null) return;

					// FIX HibernateException "Illegal attempt to associate a collection with two
					// open sessions": gel/jenisSeleksi bisa berupa instance kanonik/identity-map
					// yang dibagi lintas request -- koleksi ManyToMany lazy-nya (via tabel join)
					// bisa masih terikat ke sesi LAIN yang masih terbuka. session.refresh(gel) lalu
					// mengakses koleksinya tetap bisa memicu Hibernate mencoba meng-asosiasikan
					// ulang koleksi itu ke sesi ini. Query LANGSUNG ke tabel join lewat SQL native
					// yang terikat penuh ke `session` lokal ini, tanpa pernah menyentuh koleksi
					// lazy milik objek gel/jenisSeleksi yang mungkin dibagi antar sesi.
					@SuppressWarnings("unchecked")
					List<VerifikasiKelengkapanCalonMahasiswa> verifikasiKelengkapanCalonMahasiswasTemp = session
							.createSQLQuery("select v.* from verifikasi_kelengkapan_calon_mahasiswa v "
									+ "join gelombang_punya_verifikasi g on g.verifikasi = v.id "
									+ "where g.gelombang = :gelId")
							.addEntity(VerifikasiKelengkapanCalonMahasiswa.class)
							.setLong("gelId", gel.getId())
							.list();

					JenisSeleksi jenisSeleksi = freshBio.getJenisSeleksiDipilih() != null
							? freshBio.getJenisSeleksiDipilih()
							: freshBio.getJenisSeleksi();
					if (jenisSeleksi != null) {
						@SuppressWarnings("unchecked")
						List<VerifikasiKelengkapanCalonMahasiswa> verifikasiDariJenisSeleksi = session
								.createSQLQuery("select v.* from verifikasi_kelengkapan_calon_mahasiswa v "
										+ "join jenis_seleksi_punya_verifikasi j on j.verifikasi = v.id "
										+ "where j.jenis_seleksi = :jsId")
								.addEntity(VerifikasiKelengkapanCalonMahasiswa.class)
								.setLong("jsId", jenisSeleksi.getId())
								.list();
						if (!verifikasiDariJenisSeleksi.isEmpty()) {
							verifikasiKelengkapanCalonMahasiswasTemp = verifikasiDariJenisSeleksi;
						}
					}

					List<VerifikasiKelengkapanCalonMahasiswa> verifikasiKelengkapanCalonMahasiswas = new ArrayList<VerifikasiKelengkapanCalonMahasiswa>(
							verifikasiKelengkapanCalonMahasiswasTemp);

					try {
						Collections.sort(verifikasiKelengkapanCalonMahasiswas);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/DaftarMahasiswaLulusAction.java:514");
						// TODO: handle exception
					}

					final List<VerifikasiKelengkapanCalonMahasiswa> upload = new ArrayList<VerifikasiKelengkapanCalonMahasiswa>();
					final List<VerifikasiKelengkapanCalonMahasiswa> belumupload = new ArrayList<VerifikasiKelengkapanCalonMahasiswa>();
					final List<VerifikasiKelengkapanCalonMahasiswa> lolos = new ArrayList<VerifikasiKelengkapanCalonMahasiswa>();
					final List<VerifikasiKelengkapanCalonMahasiswa> belum = new ArrayList<VerifikasiKelengkapanCalonMahasiswa>();

					for (VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa : verifikasiKelengkapanCalonMahasiswas) {
						if (verifikasiKelengkapanCalonMahasiswa.getAktif()) {
							BiodataCalonMahasiswaPunyaVerifikasiBerkas biodataCalonMahasiswaPunyaVerifikasiBerkas = (BiodataCalonMahasiswaPunyaVerifikasiBerkas) session
									.createCriteria(BiodataCalonMahasiswaPunyaVerifikasiBerkas.class)
									.add(Restrictions.eq("verifikasiKelengkapanCalonMahasiswa",
											verifikasiKelengkapanCalonMahasiswa))
									.add(Restrictions.eq("biodataCalonMahasiswa", freshBio))
									.setMaxResults(1).uniqueResult();

							if (biodataCalonMahasiswaPunyaVerifikasiBerkas == null) {
								biodataCalonMahasiswaPunyaVerifikasiBerkas = new BiodataCalonMahasiswaPunyaVerifikasiBerkas();
								biodataCalonMahasiswaPunyaVerifikasiBerkas
										.setBiodataCalonMahasiswa(freshBio);
								biodataCalonMahasiswaPunyaVerifikasiBerkas
										.setVerifikasiKelengkapanCalonMahasiswa(verifikasiKelengkapanCalonMahasiswa);
								Common.refreshSaveOrUpdate(session, biodataCalonMahasiswaPunyaVerifikasiBerkas);
							}

							FileFotoLain lampiranLain = FileFotoLain.ambil(
									biodataCalonMahasiswaPunyaVerifikasiBerkas.getId(),
									BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName(), LampiranLain.class);
							if (lampiranLain != null) {
								upload.add(verifikasiKelengkapanCalonMahasiswa);
							} else {
								belumupload.add(verifikasiKelengkapanCalonMahasiswa);
							}

							if (biodataCalonMahasiswaPunyaVerifikasiBerkas.getVerified()) {
								lolos.add(verifikasiKelengkapanCalonMahasiswa);
							} else {
								belum.add(verifikasiKelengkapanCalonMahasiswa);
							}
						}
					}

					Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig(belumupload.size() + " belum upload",
							"/img/Record-Normal-icon.png");
					toolbarbutton.setTooltiptext("Belum upload : " + belumupload);
					toolbarbutton.setStyle("font-size:9px;");
					toolbarbutton.setDisabled(belumupload.isEmpty());
					toolbarbutton.setParent(hbox);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							displayVerifikasi(freshBio, belumupload, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
								}
							});
						}
					});

					toolbarbutton = new MyToolbarbuttonConfig(upload.size() + " telah upload",
							"/img/attachment-icon.png");
					toolbarbutton.setTooltiptext("Telah upload : " + upload);
					toolbarbutton.setDisabled(upload.isEmpty());
					toolbarbutton.setStyle("font-size:9px;");
					toolbarbutton.setParent(hbox);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							displayVerifikasi(freshBio, upload, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
								}
							});
						}
					});

					toolbarbutton = new MyToolbarbuttonConfig(belum.size() + " belum verifikasi",
							"/img/Check-icon.png");
					toolbarbutton.setTooltiptext("Belum verifikasi : " + belum);
					toolbarbutton.setDisabled(belum.isEmpty());
					toolbarbutton.setStyle("font-size:9px;");
					toolbarbutton.setParent(hbox);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							displayVerifikasi(freshBio, belum, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
								}
							});
						}
					});

					toolbarbutton = new MyToolbarbuttonConfig(lolos.size() + " telah verifikasi",
							"/img/Cute-Ball-Go-icon.png");
					toolbarbutton.setTooltiptext("Telah verifikasi : " + lolos);
					toolbarbutton.setStyle("font-size:9px;");
					toolbarbutton.setDisabled(lolos.isEmpty());
					toolbarbutton.setParent(hbox);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							displayVerifikasi(freshBio, lolos, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
								}
							});
						}
					});

						txLulus.commit();
					} catch (Exception eLulus) {
						if (txLulus != null) {
							try {
								if (txLulus.isActive()) {
									txLulus.rollback();
								}
							} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/DaftarMahasiswaLulusAction.java:645");
							}
						}
						ais.common.Common.tampilErrorJikaAdmin(eLulus);
					} finally {
						if (session != null) {
							try {
								if (session.isOpen()) {
									session.close();
								}
							} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/DaftarMahasiswaLulusAction.java:655");
							}
						}
					}
				}
			});

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ket. Ujian", "/img/Time-Today-icon.png");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportHelper.prosesSuratKeteranganHasilUjian(biodataCalonMahasiswa);

				}
			});
			button.setParent(row);
		}

	}

	private void displayVerifikasi(BiodataCalonMahasiswa biodataCalonMahasiswa,
			List<VerifikasiKelengkapanCalonMahasiswa> data, final EventListener eventListener) throws Exception {
		final MyWindow window = new MyWindow("Verifikasi Berkas", "none", false);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("90%");
		window.setWidth("900px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);

		Rows rows = new Rows();
		rows.setParent(grid);

		VerifikasiPMBHelper.tampilkanVerifikasi(biodataCalonMahasiswa, rows, null, null,
				biodataCalonMahasiswa.getGelombangPendaftaran(), data);

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
				Common.createDefaultTimer(eventListener);
			}
		});
		cancel.setParent(toolbar);

		window.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new BiodataCalonMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		addWindow.setTitle("Kelulusan");
		Common.clear(addWindow);
		addWindow.setHeight("90%");
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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		pilihan = new Radiogroup();
		Vbox vbox = new Vbox();
		vbox.setHeight("100%");
		vbox.setWidth("100%");
		vbox.setParent(pilihan);

		MyRadioConfig radio1 = new MyRadioConfig();
		if (biodataCalonMahasiswa.getProdi1() != null) {
			radio1.setLabel(biodataCalonMahasiswa.getProdi1().getNama());
			radio1.setValue(biodataCalonMahasiswa.getProdi1().getNama());
			radio1.setAttribute("pil", biodataCalonMahasiswa.getProdi1());
			vbox.appendChild(radio1);
		}
		MyRadioConfig radio2 = new MyRadioConfig();
		if (biodataCalonMahasiswa.getProdi2() != null) {
			radio2 = new MyRadioConfig();
			radio2.setLabel(biodataCalonMahasiswa.getProdi2().getNama());
			radio2.setValue(biodataCalonMahasiswa.getProdi2().getNama());
			radio2.setAttribute("pil", biodataCalonMahasiswa.getProdi2());
			vbox.appendChild(radio2);
		}

		MyRadioConfig radio3 = new MyRadioConfig();
		if (biodataCalonMahasiswa.getProdi3() != null) {
			radio3 = new MyRadioConfig();
			radio3.setLabel(biodataCalonMahasiswa.getProdi3().getNama());
			radio3.setValue(biodataCalonMahasiswa.getProdi3().getNama());
			radio3.setAttribute("pil", biodataCalonMahasiswa.getProdi3());
			vbox.appendChild(radio3);
		}

		MyRadioConfig radio4 = new MyRadioConfig();
		if (biodataCalonMahasiswa.getProdi4() != null) {
			radio4 = new MyRadioConfig();
			radio4.setLabel(biodataCalonMahasiswa.getProdi4().getNama());
			radio4.setValue(biodataCalonMahasiswa.getProdi4().getNama());
			radio4.setAttribute("pil", biodataCalonMahasiswa.getProdi4());
			vbox.appendChild(radio4);
		}

		MyRadioConfig radio5 = new MyRadioConfig();
		if (biodataCalonMahasiswa.getProdi5() != null) {
			radio5 = new MyRadioConfig();
			radio5.setLabel(biodataCalonMahasiswa.getProdi5().getNama());
			radio5.setValue(biodataCalonMahasiswa.getProdi5().getNama());
			radio5.setAttribute("pil", biodataCalonMahasiswa.getProdi5());
			vbox.appendChild(radio5);
		}

		vbox.appendChild(new ais.ui.util.MyHtml("<hr>"));

		MyRadioConfig radioBelum = new MyRadioConfig();
		radioBelum = new MyRadioConfig();
		radioBelum.setLabel("Mahasiswa ini belum diterima (belum dinyatakan lulus)");
		radioBelum.setAttribute("belum", true);
		vbox.appendChild(radioBelum);

		MyRadioConfig radioDitolak = new MyRadioConfig();
		radioDitolak = new MyRadioConfig();
		radioDitolak.setLabel("Mahasiswa ini tidak diterima (ditolak)");
		radioDitolak.setAttribute("ditolak", true);
		vbox.appendChild(radioDitolak);

		MyRadioConfig radioMundur = new MyRadioConfig();
		radioMundur = new MyRadioConfig();
		radioMundur.setLabel("Mahasiswa ini mengundurkan diri");
		radioMundur.setAttribute("mundur", true);
		vbox.appendChild(radioMundur);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilihan"));
		if (biodataCalonMahasiswa.getProdiLulus() == null) {
			radio1.setChecked(false);
			radio2.setChecked(false);
			radio3.setChecked(false);
			radio4.setChecked(false);
			radio5.setChecked(false);
		} else if (biodataCalonMahasiswa.getProdi1() != null
				&& biodataCalonMahasiswa.getProdiLulus().getId().equals(biodataCalonMahasiswa.getProdi1().getId())) {
			radio1.setChecked(true);
		} else if (biodataCalonMahasiswa.getProdi2() != null
				&& biodataCalonMahasiswa.getProdiLulus().getId().equals(biodataCalonMahasiswa.getProdi2().getId())) {
			radio2.setChecked(true);
		} else if (biodataCalonMahasiswa.getProdi3() != null
				&& biodataCalonMahasiswa.getProdiLulus().getId().equals(biodataCalonMahasiswa.getProdi3().getId())) {
			radio3.setChecked(true);
		} else if (biodataCalonMahasiswa.getProdi4() != null
				&& biodataCalonMahasiswa.getProdiLulus().getId().equals(biodataCalonMahasiswa.getProdi4().getId())) {
			radio4.setChecked(true);
		} else if (biodataCalonMahasiswa.getProdi5() != null
				&& biodataCalonMahasiswa.getProdiLulus().getId().equals(biodataCalonMahasiswa.getProdi5().getId())) {
			radio5.setChecked(true);
		}

		if (biodataCalonMahasiswa.getDitolak()) {
			radioDitolak.setChecked(biodataCalonMahasiswa.getDitolak());
		} else if (biodataCalonMahasiswa.getMundur()) {
			radioMundur.setChecked(biodataCalonMahasiswa.getMundur());
		} else if (biodataCalonMahasiswa.getProdiLulus() == null) {
			radioBelum.setChecked(true);
		}

		row.appendChild(pilihan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Diterima"));
		row.appendChild(tanggalDiterima = new MyDatebox(biodataCalonMahasiswa.getTanggalDiterima()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.initPrograms(program = new Combobox());
		row.appendChild(program);
		program.setWidth("90%");
		Common.selectComboItem(true, program, biodataCalonMahasiswa.getProgram());
		if (biodataCalonMahasiswa.getGelombangPendaftaran() != null
				&& biodataCalonMahasiswa.getGelombangPendaftaran().getTidakBolehMemilihProgramLain()) {
			Common.initKeterangan(rows,
					"Pada pilihan gelombang pendaftaran, calon mahasiswa tidak diizinkan untuk mengubah ke program pendaftaran yang lain");
			program.setDisabled(true);
		}

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new MyLabelConfigTitikDua("Diterima di gelombang *"));
		gelombangPendaftaran = new Combobox();
		gelombangPendaftaran.setReadonly(true);
		row.appendChild(gelombangPendaftaran);
		gelombangPendaftaran.setWidth("90%");
		Common.insertComboDanSemua(gelombangPendaftaran, new String[] { "nama" }, "tahunAkademik",
				GelombangPendaftaran.class, "== Klik disini untuk pilih ==",
				selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi),
								Restrictions.isNull("perguruanTinggi")),
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.selectComboItem(true, gelombangPendaftaran,
				biodataCalonMahasiswa.getGelombangPendaftaranDiterima() != null
						? biodataCalonMahasiswa.getGelombangPendaftaranDiterima()
						: biodataCalonMahasiswa.getGelombangPendaftaran());

		rowJalurPenerimaan = new MyFormRow();
		rowJalurPenerimaan.setVisible(false);
		kelompokJenisSeleksi = new Combobox();
		rowJalurPenerimaan.setParent(rows);
		rowJalurPenerimaan.appendChild(new MyLabelConfigTitikDua("Jalur penerimaan"));
		rowJalurPenerimaan.appendChild(kelompokJenisSeleksi);
		kelompokJenisSeleksi.setReadonly(true);
		kelompokJenisSeleksi.setWidth("90%");

		row = new MyFormRow();
		jenisSeleksi = new Combobox();
		row.setParent(rows);
		row.appendChild(new MyLabelConfigTitikDua("Diterima di jenis seleksi *"));
		row.appendChild(jenisSeleksi);
		jenisSeleksi.setReadonly(true);
		jenisSeleksi.setWidth("90%");

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				kelompokJenisSeleksi.getParent().setVisible(false);

				GelombangPendaftaran gelombang = (GelombangPendaftaran) (gelombangPendaftaran.getSelectedItem() == null
						? biodataCalonMahasiswa.getGelombangPendaftaran()
						: gelombangPendaftaran.getSelectedItem().getValue());
				if (gelombang == null) {
					gelombang = biodataCalonMahasiswa.getGelombangPendaftaran();
				}

				List<JenisSeleksi> temp = gelombang == null ? new ArrayList<JenisSeleksi>()
						: gelombang.ambilJenisSeleksi();
				List<JenisSeleksi> jenisSeleksis;
				if (arg0 != null && arg0.getTarget() == kelompokJenisSeleksi) {

					KelompokJenisSeleksi pilih = (KelompokJenisSeleksi) (kelompokJenisSeleksi.getSelectedItem() == null
							? null
							: kelompokJenisSeleksi.getSelectedItem().getValue());

					if (pilih == null) {
						jenisSeleksis = temp;
					} else {
						jenisSeleksis = new ArrayList<JenisSeleksi>();
						for (JenisSeleksi jenisSeleksi : temp) {
							if (jenisSeleksi.getKelompokJenisSeleksi() != null
									&& jenisSeleksi.getKelompokJenisSeleksi().getId().equals(pilih.getId())) {
								jenisSeleksis.add(jenisSeleksi);
							}
						}
					}
					Collections.sort(jenisSeleksis);
				} else {
					jenisSeleksis = temp;
					Collections.sort(jenisSeleksis);
					List<KelompokJenisSeleksi> kelompokJenisSeleksis = new ArrayList<KelompokJenisSeleksi>();
					for (JenisSeleksi jenisSeleksi : jenisSeleksis) {
						if (jenisSeleksi != null && jenisSeleksi.getKelompokJenisSeleksi() != null
								&& !kelompokJenisSeleksis.contains(jenisSeleksi.getKelompokJenisSeleksi())) {
							kelompokJenisSeleksis.add(jenisSeleksi.getKelompokJenisSeleksi());
						}
					}

					Collections.sort(kelompokJenisSeleksis);

					Common.clear(kelompokJenisSeleksi);
					kelompokJenisSeleksi.getParent().setVisible(!kelompokJenisSeleksis.isEmpty());
					kelompokJenisSeleksi.setVisible(!kelompokJenisSeleksis.isEmpty());

					Common.insertComboItems(kelompokJenisSeleksi, "nama", "keterangan", kelompokJenisSeleksis);

					MyComboitemConfig comboitem = new MyComboitemConfig();
					comboitem.setLabel("== Klik disini untuk pilih ==");
					comboitem.setValue(null);
					kelompokJenisSeleksi.appendChild(comboitem);

					Common.selectComboItem(true, kelompokJenisSeleksi, biodataCalonMahasiswa.getKelompokJenisSeleksi());

				}

				Common.insertComboItems(jenisSeleksi, "nama", "deskripsi", jenisSeleksis);

				MyComboitemConfig comboitem = new MyComboitemConfig();
				comboitem.setLabel("== Klik disini untuk pilih ==");
				comboitem.setValue(null);
				jenisSeleksi.appendChild(comboitem);

				Common.selectComboItem(true, jenisSeleksi,
						biodataCalonMahasiswa.getJenisSeleksiDipilih() != null
								? biodataCalonMahasiswa.getJenisSeleksiDipilih()
								: biodataCalonMahasiswa.getJenisSeleksi());

			}
		};

		kelompokJenisSeleksi.addEventListener("onChange", eventListener);
		gelombangPendaftaran.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		mahasiswa = new AmbilDataMahasiswaBanbox();
		mahasiswa.setAttribute("mahasiswa", biodataCalonMahasiswa.getMahasiswa());
		mahasiswa.setAttribute("myValue", biodataCalonMahasiswa.getMahasiswa());
		mahasiswa.setValue(
				biodataCalonMahasiswa.getMahasiswa() == null ? "" : biodataCalonMahasiswa.getMahasiswa().getNama());
		row.setParent(rows);
		row.appendChild(new MyLabelConfigTitikDua("Data mahasiswa "));
		row.appendChild(mahasiswa);
		mahasiswa.setReadonly(true);
		mahasiswa.setWidth("90%");

		Common.initKeterangan(rows, "(jika belum ada data mahasiswa akan terbuat otomatis ketika di generate NIM)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfigTitikDua("Diterima di status awal"));
		statusAwalDiterima = new Combobox();
		statusAwalDiterima.setReadonly(true);
		statusAwalDiterima.setWidth("90%");
		row.appendChild(statusAwalDiterima);
		Common.insertComboDanSemua(statusAwalDiterima, new String[] { "nama" }, "kode", StatusAwalMahasiswa.class,
				"== Ikuti Status Awal Calon Mahasiswa yang Ada ==", Restrictions.eq("aktif", true));
		Common.selectComboItem(true, statusAwalDiterima, biodataCalonMahasiswa.getStatusAwalDiterima());

		EventListener pilihanListrene = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				MyRadioConfig config = (MyRadioConfig) pilihan.getSelectedItem();
				Jurusan jurusan = (Jurusan) (config == null ? null : config.getAttribute("pil"));

				tanggalDiterima.getParent().setVisible(jurusan != null);
				kelompokJenisSeleksi.getParent().setVisible(jurusan != null);
				jenisSeleksi.getParent().setVisible(jurusan != null);
				gelombangPendaftaran.getParent().setVisible(jurusan != null);
				mahasiswa.getParent().setVisible(jurusan != null);
				statusAwalDiterima.getParent().setVisible(jurusan != null);

				try {
					eventListener.onEvent(null);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		pilihan.addEventListener("onClick", pilihanListrene);
		Common.createDefaultTimer(pilihanListrene);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(biodataCalonMahasiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		// row = new MyFormRow();
		//		// row.setParent(rows);
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
				onSearchDefault(null);
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
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {

		Session session = HibernateUtil.currentSession();
		if (biodataCalonMahasiswa.getId() != null) {
			biodataCalonMahasiswa = (BiodataCalonMahasiswa) session.load(BiodataCalonMahasiswa.class,
					biodataCalonMahasiswa.getId());

		}

		boolean belum = false;
		if (biodataCalonMahasiswa.getProdiLulus() == null) {
			belum = true;
		}

		MyRadioConfig config = (MyRadioConfig) pilihan.getSelectedItem();
		Jurusan jurusan = (Jurusan) (config == null ? null : config.getAttribute("pil"));

		if (jurusan != null && tanggalDiterima.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal diterima",
					"Kolom Tanggal diterima belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tanggal diterima.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		biodataCalonMahasiswa.setStatusLulus(BiodataCalonMahasiswa.LULUS);
		biodataCalonMahasiswa.setProdiLulus(jurusan);
		biodataCalonMahasiswa.setKeterangan(keterangan.getValue());

		if (gelombangPendaftaran.getSelectedItem() != null)
			biodataCalonMahasiswa.setGelombangPendaftaranDiterima(
					(GelombangPendaftaran) (gelombangPendaftaran.getSelectedItem() == null ? null
							: gelombangPendaftaran.getSelectedItem().getValue()));

		if (jenisSeleksi.getSelectedItem() != null)
			biodataCalonMahasiswa.setJenisSeleksiDipilih((JenisSeleksi) (jenisSeleksi.getSelectedItem() == null ? null
					: jenisSeleksi.getSelectedItem().getValue()));
		if (kelompokJenisSeleksi.getSelectedItem() != null)
			biodataCalonMahasiswa.setKelompokJenisSeleksi(
					(KelompokJenisSeleksi) (kelompokJenisSeleksi.getSelectedItem() == null ? null
							: kelompokJenisSeleksi.getSelectedItem().getValue()));
		biodataCalonMahasiswa.setTanggalDiterima(tanggalDiterima.getValue());

		biodataCalonMahasiswa.setMahasiswa((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));
		biodataCalonMahasiswa.setStatusAwalDiterima(
				(StatusAwalMahasiswa) (statusAwalDiterima.getSelectedItem() == null ? null
						: statusAwalDiterima.getSelectedItem().getValue()));

		if (program.getSelectedItem() != null) {
			biodataCalonMahasiswa.setProgram((String) program.getSelectedItem().getValue());
		}
		if (config != null && config.getAttribute("ditolak") != null) {
			biodataCalonMahasiswa.setDitolak(true);
		} else {
			biodataCalonMahasiswa.setDitolak(false);
		}
		if (config != null && config.getAttribute("mundur") != null) {
			biodataCalonMahasiswa.setMundur(true);
		} else {
			biodataCalonMahasiswa.setMundur(false);
		}
		if (config != null && config.getAttribute("belum") != null) {
			biodataCalonMahasiswa.setProdiLulus(null);
			biodataCalonMahasiswa.setMahasiswa(null);
			biodataCalonMahasiswa.setNim(null);
		}

		Common.refreshSaveOrUpdate(session, biodataCalonMahasiswa);

		if (belum && biodataCalonMahasiswa.getProdiLulus() != null) {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportHelper.onCetakSuratKeteranganLulus(biodataCalonMahasiswa, true);
				}
			});

		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Jurusan prodiPilihan = (Jurusan) (searchProdiPilihan.getSelectedItem() == null ? null
				: searchProdiPilihan.getSelectedItem().getValue());

		Jurusan prodiLulus = (Jurusan) (searchProdiLulus.getSelectedItem() == null ? null
				: searchProdiLulus.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();

		Criterion criterion = Restrictions.eq("prodi1", prodiPilihan);
		criterion = Restrictions.or(criterion, Restrictions.eq("prodi2", prodiPilihan));
		criterion = Restrictions.or(criterion, Restrictions.eq("prodi3", prodiPilihan));
		criterion = Restrictions.or(criterion, Restrictions.eq("prodi4", prodiPilihan));
		criterion = Restrictions.or(criterion, Restrictions.eq("prodi5", prodiPilihan));

		Criteria criteria = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (belumUploadBerkas.isChecked() || telahUploadBerkas.isChecked() || belumLolosBerkas.isChecked()
				|| telahLolosBerkas.isChecked()) {
			criteria = session.createCriteria(BiodataCalonMahasiswaPunyaVerifikasiBerkas.class)
					.setProjection(Projections.groupProperty("biodataCalonMahasiswa.id"));
			if (belumUploadBerkas.isChecked()) {
				criteria.add(Restrictions.eq("uploaded", false));
			}
			if (telahUploadBerkas.isChecked()) {
				criteria.add(Restrictions.eq("uploaded", true));
			}
			if (belumLolosBerkas.isChecked()) {
				criteria.add(Restrictions.eq("verified", false));
			}
			if (telahLolosBerkas.isChecked()) {
				criteria.add(Restrictions.eq("verified", true));
			}
			if (order)
				criteria.addOrder(Order.desc("biodataCalonMahasiswa.id"));
			else
				criteria.setProjection(Projections.countDistinct("biodataCalonMahasiswa.id"));

			criteria = criteria.createCriteria("biodataCalonMahasiswa");

		} else if (order) {
			criteria.addOrder(Order.desc("id")).setProjection(Projections.property("id"));
		} else {
			criteria.setProjection(Projections.property("id"));
		}
		criteria.createAlias("gelombangPendaftaran", "gelombangPendaftaran", Criteria.LEFT_JOIN)
				.add(selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.eq("gelombangPendaftaran.perguruanTinggi", selectedPerguruanTinggi),
								Restrictions.isNull("gelombangPendaftaran.perguruanTinggi")))

				.add(prodiLulus == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("prodiLulus", prodiLulus))
				.add(mengisiFormTambahan.isChecked() ? Restrictions.ne("parameterTambahanInds", "")
						: Restrictions.sqlRestriction("true"))

//				.add(tampilkanYgSudahBayar.isChecked() ? Restrictions.isNotNull("pembayaranRegistrasi")
//						: Restrictions.sqlRestriction("true"))

				.add(telahLogin.isChecked() ? Restrictions.eq("telahLogin", true) : Restrictions.sqlRestriction("true"))

//				.add(tampilkanYgSudahBayarDaftarUlang.isChecked() ? Restrictions.isNotNull("pembayaranDaftarUlang")
//						: Restrictions.sqlRestriction("true"))

				.add(tampilkanYgSudahdapatNIM.isChecked()
						? Restrictions.and(Restrictions.isNotNull("nim"), Restrictions.ne("nim", ""))
						: Restrictions.sqlRestriction("true"))

				.add(blmDiterima != null && blmDiterima.isChecked() ? Restrictions.isNull("prodiLulus")
						: Restrictions.sqlRestriction("true"))

//				.add(tampilkanYgBelumBayar.isChecked() ? Restrictions.isNull("pembayaranRegistrasi")
//						: Restrictions.sqlRestriction("true"))
//
//				.add(tampilkanYgBelumBayarDaftarUlang.isChecked() ? Restrictions.isNull("pembayaranDaftarUlang")
//						: Restrictions.sqlRestriction("true"))

				.add(tampilkanYgBelumdapatNIM.isChecked()
						? Restrictions.or(Restrictions.isNull("nim"), Restrictions.eq("nim", ""))
						: Restrictions.sqlRestriction("true"))

				.add(searchJenisSekolahMahasiswaBaru.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jenisSekolah", searchJenisSekolahMahasiswaBaru.getSelectedItem().getValue()))

				.add(searchJurusanSekolahMahasiswaBaru.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusanSekolah", searchJurusanSekolahMahasiswaBaru, false))

				.add(searchPaket.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("paket", searchPaket.getSelectedItem().getValue()))

				.add(searchProdiPilihan.getSelectedItem() == null ? Restrictions.sqlRestriction("true") : criterion)

				.add(searchJenisSeleksi.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jenisSeleksi", searchJenisSeleksi.getSelectedItem().getValue()))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
				.add(searchGelombang.getSelectedItem() == null || searchGelombang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("gelombangPendaftaran", searchGelombang.getSelectedItem().getValue()))
				.add(searchnoreg.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("noRegistrasi", searchnoreg.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchujian.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("noUjian", searchujian.getValue().trim(), MatchMode.ANYWHERE));

		if (tampilkanYgSudahLunasDaftarUlang.isChecked() || tampilkanYgBelumLunasDaftarUlang.isChecked()
				|| tampilkanYgSudahBayarDaftarUlang.isChecked() || tampilkanYgBelumBayarDaftarUlang.isChecked()) {
			criteria.createAlias("pembayaranDaftarUlang", "pembayaranDaftarUlang", Criteria.LEFT_JOIN)

					.add(tampilkanYgSudahLunasDaftarUlang.isChecked()
							? Restrictions.eq("pembayaranDaftarUlang.lunas", true)
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgBelumLunasDaftarUlang.isChecked()
							? Restrictions.eq("pembayaranDaftarUlang.lunas", false)
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgSudahBayarDaftarUlang.isChecked()
							? Restrictions.gt("pembayaranDaftarUlang.amount", 0.1)
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgBelumBayarDaftarUlang.isChecked()
							? Restrictions.or(Restrictions.isNull("pembayaranDaftarUlang"),
									Restrictions.lt("pembayaranDaftarUlang.amount", 0.1))
							: Restrictions.sqlRestriction("true"));
		}

		if (tampilkanYgSudahBayar.isChecked() || tampilkanYgBelumBayar.isChecked()) {
			criteria.createAlias("pembayaranRegistrasi", "pembayaranRegistrasi", Criteria.LEFT_JOIN)
					.add(tampilkanYgSudahBayar.isChecked() ? Restrictions.gt("pembayaranRegistrasi.amount", 0.1)
							: Restrictions.sqlRestriction("true"))
					.add(tampilkanYgBelumBayar.isChecked()
							? Restrictions.or(Restrictions.isNull("pembayaranRegistrasi"),
									Restrictions.lt("pembayaranRegistrasi.amount", 0.1))
							: Restrictions.sqlRestriction("true"));
		}

		return criteria;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onSearchDefault(Event event) {
		if (belumUploadBerkas.isChecked() || telahUploadBerkas.isChecked() || belumLolosBerkas.isChecked()
				|| telahLolosBerkas.isChecked()) {
			paging.setPageSize(Common.ROWS_COUNT_ON_PAGE);
			paging.setPageIncrement(Common.isMobile() ? 5 : 10);
			paging.setMold("os");
			int size = Common.ROWS_COUNT_ON_PAGE;

			try {
				size = ((Number) initCriteria(false).uniqueResult()).intValue();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			paging.setTotalSize(size);
			paging.setVisible(size > Common.ROWS_COUNT_ON_PAGE);
			try {
				if (paging.getParent() instanceof South) {
					((South) paging.getParent()).setHeight(size > Common.ROWS_COUNT_ON_PAGE ? "30px" : "0px");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/DaftarMahasiswaLulusAction.java:1322");
				// Common.tampilErrorJikaAdmin(e);
			}
		} else {
			Common.initPaging(initCriteria(false), paging);
		}

		List<Long> calonMahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		List newcalonMahasiswa = calonMahasiswa.stream().distinct().collect(Collectors.toList());
		ListModel strset = new SimpleListModel(newcalonMahasiswa);
		grid.setRowRenderer(new DaftarMahasiswaRenderer());
		grid.setModelCheckMobile(strset);
		calonMahasiswa = null;
		newcalonMahasiswa = null;
	}

}
