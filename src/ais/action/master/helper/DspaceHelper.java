package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Window;

import ais.action.master.MahasiswaRequestTugasAkhirAction;
import ais.action.master.PerkuliahanAction;
import ais.action.master.PertemuanAction;
import ais.action.master.SkripsiAction;
import ais.action.master.kkn.KelompokKknAction;
import ais.action.master.pkl.KelompokPklAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DspaceInformation;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.NamaTugasKelompok;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.TugasKelompok;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.file.TugasFileContent;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class DspaceHelper {

	public static DspaceInformation getDspaceInformation(final Perkuliahan perkuliahan, final KelompokKkn kelompokKkn,
			final KelompokPkl kelompokPkl, final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir,
			final Skripsi skripsi) {
		boolean dspace = Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF);
		DspaceInformation dspaceInformation;
		if (!dspace) {
			dspaceInformation = null;
		} else if (perkuliahan != null) {
			dspaceInformation = DspaceInformation.getDspaceInformation(Perkuliahan.class.getName(),
					perkuliahan.getId());
		} else if (kelompokKkn != null) {
			dspaceInformation = DspaceInformation.getDspaceInformation(KelompokKkn.class.getName(),
					kelompokKkn.getId());
		} else if (kelompokPkl != null) {
			dspaceInformation = DspaceInformation.getDspaceInformation(KelompokPkl.class.getName(),
					kelompokPkl.getId());
		} else if (mahasiswaRequestTugasAkhir != null) {
			dspaceInformation = DspaceInformation.getDspaceInformation(MahasiswaRequestTugasAkhir.class.getName(),
					mahasiswaRequestTugasAkhir.getId());
		} else if (skripsi != null) {
			dspaceInformation = DspaceInformation.getDspaceInformation(Skripsi.class.getName(), skripsi.getId());
		} else {
			dspaceInformation = null;
		}
		return dspaceInformation;
	}

	public static void tampilkanButtonExportDiPertemuan(Component hbox, final Perkuliahan perkuliahan,
			final KelompokKkn kelompokKkn, final KelompokPkl kelompokPkl,
			final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir, final Skripsi skripsi,
			final EventListener eventListener) {
		boolean dspace = Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("pertemuan_elearning_terhubung_ke_dspace");

		Tbmuser tbmuser = Common.getCurrentUser();

		MyToolbarbuttonConfig prosesExport = new MyToolbarbuttonConfig("Ekspor", "/img/corner.gif");
		hbox.appendChild(prosesExport);
		prosesExport
				.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && dspace);
		prosesExport.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// FIX: DspaceCommon.login() melempar IOException mentah (mis. endpoint DSpace
				// REST /rest/login membalas HTTP 404 -- URL/konfigurasi tenant salah/usang) yang
				// sebelumnya lolos sampai ke ZK's default exception handler & menampilkan stack
				// trace teknis ke pengguna. Tangkap & tampilkan pesan ramah, konsisten dgn pola
				// messagebox utk kegagalan layanan eksternal lain di codebase ini.
				try {
					String cookie = DspaceCommon.login();
					DspaceHelper.exportDisplayPilihan(cookie, null, perkuliahan, kelompokKkn, kelompokPkl,
							mahasiswaRequestTugasAkhir, skripsi, eventListener);
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit src/ais/action/master/helper/DspaceHelper.java:tampilkanButtonExportDiPertemuan-login");
					MyMessageboxConfig.show(
							"Gagal terhubung ke Repository DSpace, periksa konfigurasi URL/endpoint. Rincian: "
									+ e.getMessage(),
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				}
			}
		});

		final DspaceInformation dspaceInformation = tbmuser != null && tbmuser.getMahasiswa() != null ? null
				: DspaceHelper.getDspaceInformation(perkuliahan, kelompokKkn, kelompokPkl, mahasiswaRequestTugasAkhir,
						skripsi);

		final MyToolbarbuttonConfig batalExport = new MyToolbarbuttonConfig("Batalkan Ekspor", "/img/svg/trash.svg");
		hbox.appendChild(batalExport);
		batalExport.setVisible(dspaceInformation != null);
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
									if (dspaceInformation != null) {
										// FIX: sama seperti tombol "Ekspor" di atas -- DspaceCommon.login() bisa
										// melempar IOException mentah (mis. endpoint DSpace REST 404) yang
										// sebelumnya lolos sampai ke ZK's default exception handler.
										try {
											String cookie = DspaceCommon.login();
											DspaceInformation.delete(cookie, "collections/" + dspaceInformation.getUuid(),
													dspaceInformation.getPostInfo());
											Session session = HibernateUtil.currentNativeSession();
											session.getTransaction().begin();
											session.delete(dspaceInformation);
											session.getTransaction().commit();
											HibernateUtil.closeSession();
											batalExport.setVisible(false);
										} catch (Exception e) {
											ais.common.ErrorAuditUtil.record(e,
													"auto-audit src/ais/action/master/helper/DspaceHelper.java:tampilkanButtonExportDiPertemuan-batalExport");
											MyMessageboxConfig.show(
													"Gagal terhubung ke Repository DSpace, periksa konfigurasi URL/endpoint. Rincian: "
															+ e.getMessage(),
													"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
										}
									}
								}

							}
						});
			}
		});

	}

	public static void exportDisplayPilihan(final String cookie, final List<Perkuliahan> perkuliahans,
			final Perkuliahan perkuliahan, final KelompokKkn kelompokKkn, final KelompokPkl kelompokPkl,
			final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir, final Skripsi skripsi,
			final EventListener eventListener) throws Exception {

		final Window window = new Window();
		window.setHeight("400px");
		window.setWidth("500px");
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pilihlah data apa saja yang akan anda ekspor ke dspace :")));
		row.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		final MyCheckboxConfig pertemuan;
		row.appendChild(pertemuan = new MyCheckboxConfig("Ekspor pertemuan"));

		row = new MyFormRow();
		row.setParent(rows);
		final MyCheckboxConfig filePerkuliahan;
		row.appendChild(filePerkuliahan = new MyCheckboxConfig("Ekspor file perkuliahan"));

		row = new MyFormRow();
		row.setParent(rows);
		final MyCheckboxConfig tugasMandiri;
		row.appendChild(tugasMandiri = new MyCheckboxConfig("Ekspor tugas mandiri"));

		row = new MyFormRow();
		row.setParent(rows);
		final MyCheckboxConfig tugasHasilMandiri;
		row.appendChild(tugasHasilMandiri = new MyCheckboxConfig("Ekspor hasil tugas mandiri"));

		row = new MyFormRow();
		row.setVisible(perkuliahan != null || kelompokKkn != null || kelompokPkl != null);
		row.setParent(rows);
		final MyCheckboxConfig tugasKelompok;
		row.appendChild(tugasKelompok = new MyCheckboxConfig("Ekspor tugas kelompok"));

		row = new MyFormRow();
		row.setVisible(perkuliahan != null || kelompokKkn != null || kelompokPkl != null);
		row.setParent(rows);
		final MyCheckboxConfig tugasHasilKelompok;
		row.appendChild(tugasHasilKelompok = new MyCheckboxConfig("Ekspor hasil tugas kelompok"));

		row = new MyFormRow();
		row.setParent(rows);
		final MyCheckboxConfig audioPerkuliahan;
		row.appendChild(audioPerkuliahan = new MyCheckboxConfig("Ekspor audio"));

		row = new MyFormRow();
		row.setParent(rows);
		final MyCheckboxConfig videoPerkuliahan;
		row.appendChild(videoPerkuliahan = new MyCheckboxConfig("Ekspor video"));

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
				window.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				final Label label = Common.displayLoadBar(eventListener);

				new Thread(new Runnable() {

					@Override
					public void run() {

						if (perkuliahans != null) {

							for (Perkuliahan perkuliahan : perkuliahans) {
								try {
									DspaceHelper.exportDataPertemuan(cookie, label, pertemuan, filePerkuliahan,
											tugasMandiri, tugasHasilMandiri, tugasKelompok, tugasHasilKelompok,
											audioPerkuliahan, videoPerkuliahan, perkuliahan, kelompokKkn, kelompokPkl,
											mahasiswaRequestTugasAkhir, skripsi);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									Common.tampilErrorJikaAdmin(e);
								}
							}

						} else {

							try {
								DspaceHelper.exportDataPertemuan(cookie, label, pertemuan, filePerkuliahan,
										tugasMandiri, tugasHasilMandiri, tugasKelompok, tugasHasilKelompok,
										audioPerkuliahan, videoPerkuliahan, perkuliahan, kelompokKkn, kelompokPkl,
										mahasiswaRequestTugasAkhir, skripsi);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								Common.tampilErrorJikaAdmin(e);
							}
						}

						label.setValue("");
					}
				}).start();

				window.detach();
			}
		});
		save.setParent(toolbar);

		window.onModal();

	}

	@SuppressWarnings("unchecked")
	public static void exportDataPertemuan(final String cookie, final Label label,

			final MyCheckboxConfig pertemuanC, final MyCheckboxConfig filePerkuliahan,
			final MyCheckboxConfig tugasMandiri, final MyCheckboxConfig tugasHasilMandiri,
			final MyCheckboxConfig ctugasKelompok, final MyCheckboxConfig tugasHasilKelompok,
			final MyCheckboxConfig audioPerkuliahan, final MyCheckboxConfig videoPerkuliahan,

			final Perkuliahan perkuliahan, final KelompokKkn kelompokKkn, final KelompokPkl kelompokPkl,
			final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir, final Skripsi skripsi) throws Exception {

		Session session1 = null;
		try {

			session1 = HibernateUtil.openSession();
			DspaceInformation uuidParent = null;

			if (perkuliahan != null) {
				uuidParent = PerkuliahanAction.getDspace(cookie, perkuliahan);

			} else if (kelompokKkn != null) {
				Jurusan jurusan = kelompokKkn.getKkn() == null ? null : kelompokKkn.getKkn().getJurusan();
				if (jurusan == null) {
					jurusan = (Jurusan) session1.createCriteria(MahasiswaDapatKelompokKkn.class)
							.add(Restrictions.eq("diterima", true)).createAlias("mahasiswa", "mahasiswa")
							.setProjection(Projections.groupProperty("mahasiswa.jurusan")).setMaxResults(1)
							.add(Restrictions.eq("kelompokKkn", kelompokKkn)).uniqueResult();
				}
				if (jurusan == null) {
					label.setValue("");
					return;
				}
				uuidParent = KelompokKknAction.getDspace(cookie, kelompokKkn, jurusan);
			} else if (kelompokPkl != null) {
				Jurusan jurusan = kelompokPkl.getPkl() == null ? null : kelompokPkl.getPkl().getJurusan();
				if (jurusan == null) {
					jurusan = (Jurusan) session1.createCriteria(MahasiswaDapatKelompokPkl.class)
							.add(Restrictions.eq("diterima", true)).createAlias("mahasiswa", "mahasiswa")
							.setProjection(Projections.groupProperty("mahasiswa.jurusan")).setMaxResults(1)
							.add(Restrictions.eq("kelompokPkl", kelompokPkl)).uniqueResult();
				}
				if (jurusan == null) {
					label.setValue("");
					return;
				}
				uuidParent = KelompokPklAction.getDspace(cookie, kelompokPkl, jurusan);
			} else if (mahasiswaRequestTugasAkhir != null) {
				uuidParent = MahasiswaRequestTugasAkhirAction.getDspace(cookie, mahasiswaRequestTugasAkhir);
			} else if (skripsi != null) {
				uuidParent = SkripsiAction.getDspaceArtefakSkripsi(cookie, skripsi);
			}

			if (pertemuanC.isChecked()) {
				List<Pertemuan> pertemuans = session1.createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("tanggal")).addOrder(Order.asc("id"))
						.add(perkuliahan == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("perkuliahan", perkuliahan))
						.add(kelompokKkn == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("kelompokKkn", kelompokKkn))
						.add(kelompokPkl == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("kelompokPkl", kelompokPkl))
						.add(mahasiswaRequestTugasAkhir == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("mahasiswaRequestTugasAkhir", mahasiswaRequestTugasAkhir))
						.add(skripsi == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("skripsi", skripsi))
						.list();

				int rowIndex = 1;
				for (Pertemuan pertemuan : pertemuans) {
					if (pertemuan.getAktif()) {
						label.setValue("Sedang memproses data " + pertemuan.toString() + " ("
								+ Common.numberFormat.get().format((rowIndex++) * 100.0 / pertemuans.size()) + " %)");

						DspaceInformation dspaceInformation = PertemuanAction.getDspace(cookie, pertemuan,
								uuidParent.getUuid(), true);

						if (filePerkuliahan.isChecked()) {

							for (PertemuanFileContent pertemuanFileContent : pertemuan.ambilPertemuanFileContentTotal()
									.values()) {
								DspaceInformation.upload(cookie, dspaceInformation.getUuid(), pertemuanFileContent,
										pertemuanFileContent.getKeterangan());
							}

						}

						if (tugasMandiri.isChecked()) {

							LampiranLain lampiranLain = LampiranLain.ambil(pertemuan.getId(),
									LampiranLain.TUGAS_MANDIRI_PERKULIAHAN);

							if (lampiranLain != null) {
								DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
										pertemuan.getJudultugas());
							}

						}

						if (tugasHasilMandiri.isChecked()) {

							for (TugasFileContent tugasFileContent : pertemuan.ambilTugasFileContentTotal().values()) {
								DspaceInformation.upload(cookie, dspaceInformation.getUuid(), tugasFileContent,
										tugasFileContent.getKeterangan());
							}

						}

						if (audioPerkuliahan.isChecked()) {

							for (AudioPertemuan audioPertemuan : pertemuan.ambilAudioPertemuanTotal().values()) {
								DspaceInformation.upload(cookie, dspaceInformation.getUuid(), audioPertemuan,
										audioPertemuan.getKeteranganTambahan());
							}

						}

						if (videoPerkuliahan.isChecked()) {

							for (VideoPertemuan videoPertemuan : pertemuan.ambilVideoPertemuanTotal().values()) {
								DspaceInformation.upload(cookie, dspaceInformation.getUuid(), videoPertemuan,
										videoPertemuan.getKeteranganTambahan());
							}

						}
					}
				}
			}

			if (ctugasKelompok.isChecked()) {

				List<TugasKelompok> tugasKelompoks = session1.createCriteria(TugasKelompok.class)
						.add(Restrictions.eq("perkuliahan", perkuliahan))
						.add(kelompokKkn == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("kelompokKkn", kelompokKkn))
						.add(kelompokPkl == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("kelompokPkl", kelompokPkl))
						.list();

				int rowIndex = 1;
				for (TugasKelompok tugasKelompok : tugasKelompoks) {

					label.setValue("Sedang memproses data " + tugasKelompok.toString() + " ("
							+ Common.numberFormat.get().format((rowIndex++) * 100.0 / tugasKelompoks.size()) + " %)");

					DspaceInformation dspaceInformation = TugasKelompokHelper.getDspace(cookie, tugasKelompok, true);

					if (tugasHasilKelompok.isChecked()) {
						List<NamaTugasKelompok> namaTugasKelompoks = session1.createCriteria(NamaTugasKelompok.class)
								.add(Restrictions.eq("tugasKelompok", tugasKelompok)).list();

						for (NamaTugasKelompok namaTugasKelompok : namaTugasKelompoks) {

							LampiranLain lampiranLain = LampiranLain.ambil(namaTugasKelompok.getId(),
									NamaTugasKelompok.class.getName());
							if (lampiranLain != null) {
								DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
										"Kelompok " + namaTugasKelompok.getNama());
							}
						}
					}

				}

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		} finally {
			HibernateUtil.closeSessionQuietly(session1);
		}

	}

}
