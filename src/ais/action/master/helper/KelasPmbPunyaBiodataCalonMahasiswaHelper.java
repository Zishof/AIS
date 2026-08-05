package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.generic.AmbilDataBiodataCalonMahasiswaBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.KelasPmb;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KelasPmbPunyaBiodataCalonMahasiswaHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	private KelasPmb kelasPmb;
	private Textbox nama;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private Paging paging;

	public KelasPmbPunyaBiodataCalonMahasiswaHelper() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	class DetailKelasRenderer extends ais.ui.util.MyRowRenderer {

		private boolean delete = false;

		public DetailKelasRenderer() {
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			arg0.setValign("top");
			final BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) data;

			CommonMedia.tampilkanGambarKecil(calonMahasiswa).setParent(arg0);

			new Label(calonMahasiswa.getNama()).setParent(arg0);

			RevisiHelper.createNewRevisi(BiodataCalonMahasiswa.class, calonMahasiswa,
					calonMahasiswa.getTanggalLahir() == null
							? Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate())
							: Common.dateFormat2.get().format(calonMahasiswa.getTanggalLahir()))
					.setParent(arg0);

			new Label(calonMahasiswa.getAsalSma() == null ? "" : calonMahasiswa.getAsalSma()).setParent(arg0);

			Vbox vbox = new Vbox();
			if (calonMahasiswa.getJenisSeleksi() != null)
				vbox.appendChild(new Label("No. Reg.:" + (calonMahasiswa.getJenisSeleksi().toString())));
			if (calonMahasiswa.getNoRegistrasi() != null && !calonMahasiswa.getNoRegistrasi().trim().isEmpty())
				vbox.appendChild(new Label("No. Reg.:" + (calonMahasiswa.getNoRegistrasi())));
			if (calonMahasiswa.getNoUjian() != null && !calonMahasiswa.getNoUjian().trim().isEmpty())
				vbox.appendChild(new Label("No. Ujian:" + (calonMahasiswa.getNoUjian())));
			if (calonMahasiswa.getTotalSkor() > 0)
				vbox.appendChild(new Label("Skor :" + Common.numberFormat.get().format((calonMahasiswa.getTotalSkor()))));
			vbox.appendChild(new Label("Login :" + (calonMahasiswa.getTelahLogin() ? "Ya" : "Tidak")));
			if (calonMahasiswa.getWaktuLogin() != null)
				vbox.appendChild(
						new Label("Terakhir Login :" + Common.dateFormat.get().format(calonMahasiswa.getWaktuLogin())));
			if (calonMahasiswa.getNim() != null && !calonMahasiswa.getNim().trim().isEmpty())
				vbox.appendChild(new Label("NIM :" + (calonMahasiswa.getNim())));
			if (calonMahasiswa.getMerupakanPindahan()) {
				vbox.appendChild(new Label("Pindahan dari :" + (calonMahasiswa.getPindahanDariKampus())));
				vbox.appendChild(new Label("Prodi :" + (calonMahasiswa.getPindahanDariProdi())));
				vbox.appendChild(
						new Label("Pindah di semester :" + (calonMahasiswa.getPindahDariKampusLamaDiSemester())));
				vbox.appendChild(new Label("NIM lama :" + (calonMahasiswa.getNimLamaSebelumPindah())));
				vbox.appendChild(new Label("Alasan pindah:" + (calonMahasiswa.getKeteranganPindah())));
			}

			vbox.setParent(arg0);

			new Label(calonMahasiswa.getPaket() == null ? "" : calonMahasiswa.getPaket().getNama()).setParent(arg0);

			vbox = new Vbox();
			if (calonMahasiswa.getProdi1() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi1())));
			}
			if (calonMahasiswa.getProdi2() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi2())));
			}
			if (calonMahasiswa.getProdi3() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi3())));
			}
			if (calonMahasiswa.getProdi4() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi4())));
			}
			if (calonMahasiswa.getProdi5() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi5())));
			}
			if (calonMahasiswa.getProdiLulus() != null) {
				vbox.appendChild(new Label("Lulus di prodi : " + (calonMahasiswa.getProdiLulus())));
			} else {
				vbox.appendChild(new Label("Belum / tidak lulus"));
			}
			vbox.setParent(arg0);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setVisible(delete);
			button.setTooltiptext("Hapus Data");
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
										Common.createDefaultTimer(new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {

												// MEMAKAI OPEN SESSION & TRANSAKSI AMAN
												Session updateSession = null;
												try {
													updateSession = HibernateUtil.openSession();
													updateSession.getTransaction().begin();

													calonMahasiswa.setKelasPmb(null);
													updateSession.update(calonMahasiswa);

													updateSession.getTransaction().commit();
													loadData(null);

												} catch (Exception e) {
													if (updateSession != null
															&& updateSession.getTransaction().isActive()) {
														updateSession.getTransaction().rollback();
													}
													Common.tampilErrorJikaAdmin(e);
													PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
												} finally {
													if (updateSession != null && updateSession.isOpen()) {
														try {
															updateSession.clear();
														} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:194");
														}
														try {
															updateSession.disconnect();
														} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:198");
														}
														try {
															updateSession.close();
														} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:202");
														}
													}
												}
											}
										});
									}
								}
							});
				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}
	}

	// Method wajib dari interface DataCriteria. Dialihkan ke helper internal
	// (buildCriteria).
	@Override
	public Criteria initCriteria(boolean order) {
		return buildCriteria(HibernateUtil.currentSession(), order);
	}

	// HELPER INTERNAL: Membuat kriteria dengan injeksi Session dinamis untuk
	// openSession()
	private Criteria buildCriteria(Session session, boolean order) {
		Criteria criteria = session.createCriteria(BiodataCalonMahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		// Alias Prodi (Sangat aman dari Error Resolving Property)
		criteria.createAlias("prodiLulus", "prodiLulus", Criteria.LEFT_JOIN)
				.createAlias("prodi1", "prodi1", Criteria.LEFT_JOIN).createAlias("prodi2", "prodi2", Criteria.LEFT_JOIN)
				.createAlias("prodi3", "prodi3", Criteria.LEFT_JOIN).createAlias("prodi4", "prodi4", Criteria.LEFT_JOIN)
				.createAlias("prodi5", "prodi5", Criteria.LEFT_JOIN);

		criteria.createAlias("prodiLulus.fakultas", "fakultasLulus", Criteria.LEFT_JOIN)
				.createAlias("prodi1.fakultas", "fakultas1", Criteria.LEFT_JOIN)
				.createAlias("prodi2.fakultas", "fakultas2", Criteria.LEFT_JOIN)
				.createAlias("prodi3.fakultas", "fakultas3", Criteria.LEFT_JOIN)
				.createAlias("prodi4.fakultas", "fakultas4", Criteria.LEFT_JOIN)
				.createAlias("prodi5.fakultas", "fakultas5", Criteria.LEFT_JOIN);

		Object valJurusan = searchjurusan.getSelectedItem() == null ? null : searchjurusan.getSelectedItem().getValue();
		Object valFakultas = searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue();
		String valNama = nama.getValue() == null ? "" : nama.getValue().trim();

		// FILTER JURUSAN DINAMIS (Mencari di semua opsi pilihan Mhs)
		if (valJurusan != null) {
			Disjunction orJurusan = Restrictions.disjunction();
			orJurusan.add(Restrictions.eq("prodiLulus", valJurusan));
			orJurusan.add(Restrictions.eq("prodi1", valJurusan));
			orJurusan.add(Restrictions.eq("prodi2", valJurusan));
			orJurusan.add(Restrictions.eq("prodi3", valJurusan));
			orJurusan.add(Restrictions.eq("prodi4", valJurusan));
			orJurusan.add(Restrictions.eq("prodi5", valJurusan));
			criteria.add(orJurusan);
		}

		// FILTER FAKULTAS DINAMIS
		if (valFakultas != null) {
			Disjunction orFakultas = Restrictions.disjunction();
			orFakultas.add(Restrictions.eq("fakultasLulus", valFakultas));
			orFakultas.add(Restrictions.eq("fakultas1", valFakultas));
			orFakultas.add(Restrictions.eq("fakultas2", valFakultas));
			orFakultas.add(Restrictions.eq("fakultas3", valFakultas));
			orFakultas.add(Restrictions.eq("fakultas4", valFakultas));
			orFakultas.add(Restrictions.eq("fakultas5", valFakultas));
			criteria.add(orFakultas);
		}

		if (!valNama.isEmpty()) {
			criteria.add(Restrictions.or(Restrictions.ilike("noRegistrasi", valNama, MatchMode.ANYWHERE),
					Restrictions.ilike("nama", valNama, MatchMode.ANYWHERE)));
		}

		if (kelasPmb != null) {
			criteria.add(Restrictions.eq("kelasPmb", kelasPmb));
		}

		if (order) {
			criteria.addOrder(Order.asc("noRegistrasi"));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = null;
				try {
					session = HibernateUtil.openSession();

					Criteria criteriaCount = buildCriteria(session, false);
					Common.initPaging(criteriaCount, paging);

					Criteria criteriaData = buildCriteria(session, true);
					criteriaData.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
							.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()));

					List<BiodataCalonMahasiswa> myBiodataCalonMahasiswas = ConstantValues.simpleList(criteriaData,
							BiodataCalonMahasiswa.class);

					ListModel strset = new SimpleListModel(myBiodataCalonMahasiswas);
					grid.setRowRenderer(new DetailKelasRenderer());
					grid.setModelCheckMobile(strset);

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:314");
				} finally {
					// PEMBERSIHAN MEMORI MUTLAK PADA LOAD DATA GRID
					if (session != null && session.isOpen()) {
						try {
							session.clear();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:320");
						}
						try {
							session.disconnect();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:324");
						}
						try {
							session.close();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:328");
						}
					}
				}
			}
		});
	}

	public void display(final KelasPmb kelasPmb, final Component component, final MyWindow window) {
		this.kelasPmb = kelasPmb;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar Calon Mahasiswa yang mengikuti kelas " + kelasPmb.getNama()));

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(groupbox);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Mhs : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
		nama.addEventListener(Events.ON_OK, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(Common.getBahasaConfig("Fakultas") + " : "));
		toolbar.appendChild(searchfakultas);
		searchfakultas.setCols(10);
		searchfakultas.addEventListener(Events.ON_CHANGE, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(Common.getBahasaConfig("Jurusan") + " : "));
		toolbar.appendChild(searchjurusan);
		searchjurusan.setCols(10);
		searchjurusan.addEventListener(Events.ON_CHANGE, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		if (kelasPmb.getJurusan() != null) {
			Common.selectComboItem(searchfakultas, kelasPmb.getJurusan().getFakultas());
			searchfakultas.setDisabled(true);
			Common.insertCombo(searchjurusan, "nama", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", kelasPmb.getJurusan().getFakultas()));
			Common.selectComboItem(searchjurusan, kelasPmb.getJurusan());
			searchjurusan.setDisabled(true);
		}

		if (kelasPmb.getFakultas() != null) {
			Common.selectComboItem(searchfakultas, kelasPmb.getFakultas());
			searchfakultas.setDisabled(true);
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Calon Mahasiswa", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session listSession = null;
				List<BiodataCalonMahasiswa> biodataCalonMahasiswas = new ArrayList<BiodataCalonMahasiswa>();

				try {
					listSession = HibernateUtil.openSession();
					biodataCalonMahasiswas = ConstantValues.simpleList(listSession
							.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.eq("kelasPmb", kelasPmb)),
							BiodataCalonMahasiswa.class, false);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:417");
				} finally {
					if (listSession != null && listSession.isOpen()) {
						try {
							listSession.clear();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:422");
						}
						try {
							listSession.disconnect();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:426");
						}
						try {
							listSession.close();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:430");
						}
					}
				}

				AmbilDataBiodataCalonMahasiswaBanyak ambil = new AmbilDataBiodataCalonMahasiswaBanyak(
						biodataCalonMahasiswas);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<BiodataCalonMahasiswa> selectedMhs = (List<BiodataCalonMahasiswa>) arg0.getData();
						if (selectedMhs != null && !selectedMhs.isEmpty()) {

							Session saveSession = null;
							try {
								saveSession = HibernateUtil.openSession();
								saveSession.getTransaction().begin();

								// Batch Update (Efisien dalam satu transaksi)
								for (BiodataCalonMahasiswa bcm : selectedMhs) {
									bcm.setKelasPmb(kelasPmb);
									saveSession.update(bcm);
								}

								saveSession.getTransaction().commit();
							} catch (Exception e) {
								if (saveSession != null && saveSession.getTransaction().isActive()) {
									saveSession.getTransaction().rollback();
								}
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:461");
							} finally {
								if (saveSession != null && saveSession.isOpen()) {
									try {
										saveSession.clear();
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:466");
									}
									try {
										saveSession.disconnect();
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:470");
									}
									try {
										saveSession.close();
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:474");
									}
								}
							}
						}
						loadData(null);
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Bersihkan", "/img/svg/trash.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus semua data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									Session clearSession = null;
									try {
										clearSession = HibernateUtil.openSession();
										clearSession.getTransaction().begin();

										// PENGAMANAN SQL QUERY MENGGUNAKAN SET PARAMETER
										clearSession.createSQLQuery(
												"update biodata_calon_mahasiswa set kelas_pmb = null where kelas_pmb = :namaKelas")
												.setParameter("namaKelas", kelasPmb.getId()).executeUpdate();

										clearSession.getTransaction().commit();
										loadData(null);

									} catch (Exception e) {
										if (clearSession != null && clearSession.getTransaction().isActive()) {
											clearSession.getTransaction().rollback();
										}
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
									} finally {
										if (clearSession != null && clearSession.isOpen()) {
											try {
												clearSession.clear();
											} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:528");
											}
											try {
												clearSession.disconnect();
											} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:532");
											}
											try {
												clearSession.close();
											} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KelasPmbPunyaBiodataCalonMahasiswaHelper.java:536");
											}
										}
									}
								}
							}
						});
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal Lahir");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Asal Sekolah/Kampus");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No. Registrasi, Ujian, NIM");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Paket");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pilihan Prodi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}
}