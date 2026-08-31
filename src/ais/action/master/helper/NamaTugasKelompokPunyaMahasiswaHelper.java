package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.generic.AmbilDataMahasiswaDariMahasiswaBanyak;
import ais.action.master.helper.generic.AmbilDataSiswaDariSiswaBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.FormatNilai;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.NamaTugasKelompok;
import ais.database.model.NamaTugasKelompokPunyaMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.TugasKelompok;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Composer ZK untuk mengelola anggota satu kelompok tugas ({@link NamaTugasKelompok}): menampilkan
 * grid mahasiswa/siswa anggota beserta keterangan dan nilai (per format nilai OBE bila kurikulum
 * matakuliah terkait memakai OBE, atau nilai tunggal biasa bila tidak), menyediakan tombol "Ambil
 * Anggota Kelompok" untuk menambah anggota dari peserta jadwal pelajaran/perkuliahan/kelompok KKN/
 * kelompok PKL yang belum dikecualikan ({@code mhsYgTidakIkut}), dan tombol hapus per anggota.
 *
 * <p>
 * Perilaku tampilan/edit berbeda tergantung siapa yang login: operator (baik {@code mahasiswa} maupun
 * {@code biodataCalonMahasiswa} milik pemanggil bernilai {@code null}) dapat mengedit keterangan dan
 * nilai langsung di grid (disimpan per-perubahan via listener {@code onChange}, dengan indikator
 * "tersimpan" sekejap); mahasiswa yang login hanya melihat baris miliknya sendiri (baris lain
 * disamarkan sebagai "-") dalam mode baca saja. Nilai OBE disimpan sebagai JSON per-kunci
 * ({@code <id>_mhs|siswa_nilai_<idFormatNilai>}) pada kolom {@code keteranganNilai} milik
 * {@link Tugas}, bukan pada kolom nilai tunggal {@link NamaTugasKelompokPunyaMahasiswa#getNilai()}.
 * </p>
 */
public class NamaTugasKelompokPunyaMahasiswaHelper implements DataLoader {

	private MyGrid grid;
	private NamaTugasKelompok namaTugasKelompok;
	private Mahasiswa mahasiswa;
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private JSONObject jsonObjectTugas;

	/**
	 * @param mahasiswa               bila diisi, membatasi tampilan grid hanya ke baris milik
	 *                                mahasiswa ini (mode baca saja untuk mahasiswa yang login)
	 * @param biodataCalonMahasiswa   dipakai bersama {@code mahasiswa} untuk menentukan mode
	 *                                edit-operator vs baca-saja-mahasiswa
	 */
	public NamaTugasKelompokPunyaMahasiswaHelper(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.mahasiswa = mahasiswa;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/** Row renderer grid anggota kelompok tugas: foto+identitas anggota, keterangan (editable untuk operator), nilai per format-nilai OBE atau nilai tunggal, dan tombol hapus. */
	class DetailNamaTugasKelompokRenderer extends ais.ui.util.MyRowRenderer {

		private List<FormatNilai> obeFormatNilais;

		public DetailNamaTugasKelompokRenderer(List<FormatNilai> obeFormatNilais) {
			this.obeFormatNilais = obeFormatNilais;
		}

		private void tampilkanIndikatorTersimpan(final Label indikator) {
			indikator.setValue("✓ tersimpan");
			indikator.setStyle("color:#16a34a;font-size:11px;font-weight:bold;margin-left:6px;");
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					indikator.setValue("");
					indikator.setStyle("font-size:11px;margin-left:6px;");
				}
			});
		}

		private NamaTugasKelompokPunyaMahasiswa ambilAnggotaTerkelola(Session session,
				NamaTugasKelompokPunyaMahasiswa sumber) {
			if (sumber != null && sumber.getId() != null) {
				return (NamaTugasKelompokPunyaMahasiswa) session.load(NamaTugasKelompokPunyaMahasiswa.class,
						sumber.getId());
			}
			return sumber;
		}

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			final NamaTugasKelompokPunyaMahasiswa namaTugasKelompokPunyaMahasiswa = (NamaTugasKelompokPunyaMahasiswa) data;

			final Mahasiswa mahasiswa = namaTugasKelompokPunyaMahasiswa.getMahasiswa();

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);

			Vbox a = new Vbox();
			a.setParent(hbox);
			new Label(mahasiswa == null ? "" : mahasiswa.getNim()).setParent(a);
			new Label(mahasiswa == null ? "" : mahasiswa.getNama()).setParent(a);

			mahasiswa.tampilkanHp(a);
			mahasiswa.tampilkanEmail(a);

			if (NamaTugasKelompokPunyaMahasiswaHelper.this.mahasiswa == null
					&& NamaTugasKelompokPunyaMahasiswaHelper.this.biodataCalonMahasiswa == null) {

				Hbox ketBox = new Hbox();
				ketBox.setWidth("100%");
				ketBox.setStyle("align-items:center;");
				ketBox.setParent(arg0);
				final Textbox ketarangan = new Textbox(namaTugasKelompokPunyaMahasiswa.getKeterangan());
				ketarangan.setWidth("90%");
				ketarangan.setRows(2);
				ketarangan.setParent(ketBox);
				ketarangan.setMaxlength(255);
				final Label indikatorKet = new Label("");
				indikatorKet.setStyle("font-size:11px;margin-left:6px;");
				indikatorKet.setParent(ketBox);
				ketarangan.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						try {
							Session session = HibernateUtil.currentSession();
							NamaTugasKelompokPunyaMahasiswa anggota = ambilAnggotaTerkelola(session,
									namaTugasKelompokPunyaMahasiswa);
							anggota.setKeterangan(ketarangan.getValue());
							Common.refreshUpdate(session, anggota);
							tampilkanIndikatorTersimpan(indikatorKet);
						} catch (Exception e) {
							PesanFormalHelper.tampilkanGagalException(
									"menyimpan keterangan anggota kelompok", e,
									new String[] { "Muat ulang lalu masukkan kembali keterangan." });
						}
					}
				});

				if (!obeFormatNilais.isEmpty()) {

					for (final FormatNilai formatNilai : obeFormatNilais) {

						String key = "";
						if (namaTugasKelompokPunyaMahasiswa.getMahasiswa() != null) {
							key = namaTugasKelompokPunyaMahasiswa.getMahasiswa().getId() + "_mhs";
						} else if (namaTugasKelompokPunyaMahasiswa.getSiswa() != null) {
							key = namaTugasKelompokPunyaMahasiswa.getSiswa().getId() + "_siswa";
						}
						final String fullKey = key + "_nilai_" + formatNilai.getId();

						Hbox nilaiBox = new Hbox();
						nilaiBox.setWidth("100%");
						nilaiBox.setStyle("align-items:center;");
						nilaiBox.setParent(arg0);
						final MyDoublebox nilai = new MyDoublebox(
								jsonObjectTugas.isNull(fullKey) ? 0.0
										: jsonObjectTugas.getDouble(fullKey));
						ais.ui.util.UIUtil.gayaInputNilai(nilai);
						nilai.setParent(nilaiBox);
						final Label indikatorNilai = new Label("");
						indikatorNilai.setStyle("font-size:11px;margin-left:6px;");
						indikatorNilai.setParent(nilaiBox);
						nilai.addEventListener("onChange", new EventListener() {
							@Override
							public void onEvent(Event ev) throws Exception {
								try {
									Session session = HibernateUtil.currentSession();
									jsonObjectTugas.put(fullKey, nilai.getValue());
									Tugas tugasSave = namaTugasKelompok.getTugasKelompok();
									if (tugasSave != null) {
										session.refresh(tugasSave);
										tugasSave.belum("tugas_file_content_" + tugasSave.getClass().getName());
										tugasSave.setKeteranganNilai(jsonObjectTugas.toString());
										Common.refreshUpdate(session, tugasSave);
									}
									tampilkanIndikatorTersimpan(indikatorNilai);
								} catch (Exception e) {
									PesanFormalHelper.tampilkanGagalException(
											"menyimpan nilai OBE anggota kelompok", e,
											new String[] { "Muat ulang lalu masukkan kembali nilai." });
								}
							}
						});
					}

				} else {

					Hbox nilaiBox = new Hbox();
					nilaiBox.setWidth("100%");
					nilaiBox.setStyle("align-items:center;");
					nilaiBox.setParent(arg0);
					final MyDoublebox nilai = new MyDoublebox(namaTugasKelompokPunyaMahasiswa.getNilai());
					ais.ui.util.UIUtil.gayaInputNilai(nilai);
					nilai.setParent(nilaiBox);
					final Label indikatorNilai = new Label("");
					indikatorNilai.setStyle("font-size:11px;margin-left:6px;");
					indikatorNilai.setParent(nilaiBox);
					nilai.addEventListener("onChange", new EventListener() {
						@Override
						public void onEvent(Event ev) throws Exception {
							try {
								Session session = HibernateUtil.currentSession();
								NamaTugasKelompokPunyaMahasiswa anggota = ambilAnggotaTerkelola(session,
										namaTugasKelompokPunyaMahasiswa);
								anggota.setNilai(nilai.getValue());
								Common.refreshUpdate(session, anggota);
								tampilkanIndikatorTersimpan(indikatorNilai);
							} catch (Exception e) {
								PesanFormalHelper.tampilkanGagalException(
										"menyimpan nilai anggota kelompok", e,
										new String[] { "Muat ulang lalu masukkan kembali nilai." });
							}
						}
					});
				}
			} else {

				if (NamaTugasKelompokPunyaMahasiswaHelper.this.mahasiswa != null
						&& NamaTugasKelompokPunyaMahasiswaHelper.this.mahasiswa.getId() != null
						&& namaTugasKelompokPunyaMahasiswa.getMahasiswa() != null
						&& namaTugasKelompokPunyaMahasiswa.getMahasiswa().getId()
								.equals(NamaTugasKelompokPunyaMahasiswaHelper.this.mahasiswa.getId())) {
					new Label(namaTugasKelompokPunyaMahasiswa.getKeterangan()).setParent(arg0);

					if (!obeFormatNilais.isEmpty()) {

						for (FormatNilai formatNilai : obeFormatNilais) {

							String key = "";
							if (namaTugasKelompokPunyaMahasiswa.getMahasiswa() != null) {
								key = namaTugasKelompokPunyaMahasiswa.getMahasiswa().getId() + "_mhs";
							} else if (namaTugasKelompokPunyaMahasiswa.getSiswa() != null) {
								key = namaTugasKelompokPunyaMahasiswa.getSiswa().getId() + "_siswa";
							}

							Double n = jsonObjectTugas.isNull(key + "_nilai_" + formatNilai.getId()) ? 0.0
									: jsonObjectTugas.getDouble(key + "_nilai_" + formatNilai.getId());

							new MyLabelAgakKecilBold(
									n > 0.1 ? formatNilai.getNama() + " : " + Common.numberFormat.get().format(n) + "; "
											: formatNilai.getNama() + " belum di-input; ")
									.setParent(arg0);
						}
					} else {

						new Label(Common.numberFormat.get().format(namaTugasKelompokPunyaMahasiswa.getNilai()))
								.setParent(arg0);
					}
				}

				else {
					new Label("-").setParent(arg0);
					new Label("-").setParent(arg0);
				}
			}

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(Common.getCurrentUser().getMahasiswa() == null);

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
										try {
											Common.refreshDelete(namaTugasKelompokPunyaMahasiswa);

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													loadData(null);
												}
											});

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException(
													"menghapus data anggota kelompok tugas",
													e, new String[] {
															"Pastikan tidak ada nilai atau data lain yang masih berelasi dengan data ini.",
															"Muat ulang (refresh) halaman ini lalu coba hapus kembali.",
															"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
													});
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);

		}
	}

	/**
	 * Memuat ulang daftar anggota {@link NamaTugasKelompok} saat ini (mengecualikan mahasiswa yang
	 * ada di {@code mhsYgTidakIkut}), menyiapkan daftar format nilai OBE bila kurikulum matakuliah
	 * terkait memakai OBE, dan mem-parsing JSON {@code keteranganNilai} milik {@link Tugas} untuk
	 * dipakai render nilai per anggota. Parameter {@code value} tidak dipakai.
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = null;
		try {
			if (namaTugasKelompok == null || namaTugasKelompok.getId() == null) {
				return;
			}
			session = HibernateUtil.openSession();
			session.setFlushMode(FlushMode.MANUAL);

			List<NamaTugasKelompokPunyaMahasiswa> namaTugasKelompokPunyaMahasiswatempo = session
					.createCriteria(NamaTugasKelompokPunyaMahasiswa.class).addOrder(Order.asc("id"))
					.createAlias("namaTugasKelompok", "ntk")
					.add(Restrictions.eq("ntk.id", namaTugasKelompok.getId())).list();

			List<NamaTugasKelompokPunyaMahasiswa> namaTugasKelompokPunyaMahasiswas = new ArrayList<NamaTugasKelompokPunyaMahasiswa>();
			for (NamaTugasKelompokPunyaMahasiswa namaTugasKelompokPunyaMahasiswa : namaTugasKelompokPunyaMahasiswatempo) {
				try {
					String tidakIkut = namaTugasKelompok.getTugasKelompok() == null ? ""
							: namaTugasKelompok.getTugasKelompok().getMhsYgTidakIkut();
					Long mahasiswaId = namaTugasKelompokPunyaMahasiswa.getMahasiswa() == null ? null
							: namaTugasKelompokPunyaMahasiswa.getMahasiswa().getId();
					if (mahasiswaId == null || tidakIkut == null || !tidakIkut.contains("," + mahasiswaId + ",")) {
						namaTugasKelompokPunyaMahasiswas.add(namaTugasKelompokPunyaMahasiswa);
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			TugasKelompok tugas = namaTugasKelompok.getTugasKelompok();
			Perkuliahan perkuliahan = tugas == null ? null : tugas.getPerkuliahan();

			List<FormatNilai> obeFormatNilais = new ArrayList<FormatNilai>();
			if (tugas != null && perkuliahan != null && perkuliahan.getKurikulum() != null
					&& perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())
					&& tugas.getFormatNilais() != null && !tugas.getFormatNilais().equalsIgnoreCase(Tugas.JSON)) {
				String formatNilaiJson = tugas.getFormatNilais().replace('\0', ' ');
				List<FormatNilai> formatNilais = Common.getFormatNilais(session, perkuliahan);
				JSONObject jsonObject = new JSONObject(formatNilaiJson);
				for (FormatNilai nilai : formatNilais) {
					if (nilai.getStatusPertemuan() != null && !jsonObject.isNull(nilai.getId().toString())) {
						obeFormatNilais.add(nilai);
					}
				}
			}
			String keteranganNilai = tugas == null ? "{}" : tugas.getKeteranganNilai();
			jsonObjectTugas = new JSONObject(keteranganNilai == null || keteranganNilai.trim().length() == 0 ? "{}"
					: keteranganNilai.replace('\0', ' '));

			ListModel strset = new SimpleListModel(namaTugasKelompokPunyaMahasiswas);
			grid.setRowRenderer(new DetailNamaTugasKelompokRenderer(obeFormatNilais));
			grid.setModelCheckMobile(strset);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"memuat daftar anggota kelompok tugas",
					e, new String[] {
							"Muat ulang (refresh) halaman ini lalu coba kembali.",
							"Periksa apakah data kelompok tugas terkait masih tersedia dan belum dihapus.",
							"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
		} finally {
			Common.closeNativeSessionQuietly(session);
		}
	}

	/**
	 * Membangun UI pengelolaan anggota kelompok tugas (judul, tombol "Ambil Anggota Kelompok", grid
	 * dengan kolom nilai dinamis mengikuti format nilai OBE bila berlaku) di dalam {@code component}
	 * dan memuat data awal. Sumber calon anggota untuk tombol "Ambil Anggota Kelompok" tergantung jenis
	 * tugas: peserta jadwal pelajaran (siswa), peserta perkuliahan, anggota kelompok KKN diterima, atau
	 * anggota kelompok PKL diterima — sesuai relasi yang dimiliki {@link TugasKelompok} terkait.
	 *
	 * @param namaTugasKelompok  kelompok tugas yang anggotanya dikelola
	 * @param component          container ZK yang akan diisi
	 * @throws Exception diteruskan dari kegagalan Hibernate saat memuat format nilai OBE
	 */
	public void display(final NamaTugasKelompok namaTugasKelompok, final Component component) throws Exception {
		this.namaTugasKelompok = namaTugasKelompok;
		Common.clear(component);

		Groupbox myDiv = new ais.ui.util.MyGroupboxStyled();
		myDiv.setWidth("90%");
		myDiv.appendChild(new MyCaptionStyled("Anggota Kelompok \"" + namaTugasKelompok.getNama() + "\""));
		myDiv.setParent(component);

		Toolbar toolbar = new Toolbar();
		toolbar.setVisible(
				Common.getCurrentUser().getMahasiswa() == null && Common.getCurrentUser().getSiswa() == null);
		toolbar.setParent(myDiv);
		if (namaTugasKelompok.getTugasKelompok().getJadwalPelajaran() != null) {

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Anggota Kelompok", "/img/new.gif");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					Session session = HibernateUtil.currentSession();
					List<Siswa> siswas = ConstantValues
							.simpleList(session.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
									.createAlias("namaTugasKelompok", "namaTugasKelompok")
									.add(Restrictions.eq("namaTugasKelompok.tugasKelompok",
											namaTugasKelompok.getTugasKelompok()))
									.setProjection(Projections.property("siswa.id")), Siswa.class, false);

					List<Siswa> hanyaSiswastempo = new ArrayList<Siswa>();

					if (namaTugasKelompok.getTugasKelompok().getJadwalPelajaran() != null) {
						hanyaSiswastempo = namaTugasKelompok.getTugasKelompok().getJadwalPelajaran().ambilSiswa();
					}
					List<Siswa> hanyaSiswas = new ArrayList<Siswa>();
					for (Siswa siswa : hanyaSiswastempo) {
						if (!namaTugasKelompok.getTugasKelompok().getMhsYgTidakIkut()
								.contains("," + siswa.getId() + ",")) {
							hanyaSiswas.add(siswa);
						}
					}

					AmbilDataSiswaDariSiswaBanyak window = new AmbilDataSiswaDariSiswaBanyak(siswas, hanyaSiswas);

					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
					window.setWidth("950px");
					window.setHeight("90%");

					window.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							List<Siswa> siswas = (List<Siswa>) arg0.getData();

							if (siswas != null) {
								Session session = HibernateUtil.currentSession();

								for (Siswa siswa : siswas) {

									NamaTugasKelompokPunyaMahasiswa namaTugasKelompokPunyaMahasiswa = new NamaTugasKelompokPunyaMahasiswa();
									namaTugasKelompokPunyaMahasiswa.setSiswa(siswa);
									namaTugasKelompokPunyaMahasiswa.setNamaTugasKelompok(namaTugasKelompok);

									session.save(namaTugasKelompokPunyaMahasiswa);
								}
								session.flush();

								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										loadData(null);
									}
								});

							}

						}
					});

					window.onModal();
					hanyaSiswastempo = null;
				}

			});
			button.setParent(toolbar);

		} else {
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Anggota Kelompok", "/img/new.gif");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					Session session = HibernateUtil.currentSession();
					List<Mahasiswa> mahasiswas = ConstantValues
							.simpleList(
									session.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
											.createAlias("namaTugasKelompok", "namaTugasKelompok")
											.add(Restrictions.eq("namaTugasKelompok.tugasKelompok",
													namaTugasKelompok.getTugasKelompok()))
											.setProjection(Projections.property("mahasiswa.id")),
									Mahasiswa.class, false);

					List<Mahasiswa> hanyaMahasiswastempo = new ArrayList<Mahasiswa>();

					if (namaTugasKelompok.getTugasKelompok().getPerkuliahan() != null) {
						hanyaMahasiswastempo = namaTugasKelompok.getTugasKelompok().getPerkuliahan().ambilMahasiswa();
					} else if (namaTugasKelompok.getTugasKelompok().getKelompokKkn() != null) {
						hanyaMahasiswastempo = ConstantValues
								.simpleList(
										session.createCriteria(MahasiswaDapatKelompokKkn.class)
												.add(Restrictions.eq("diterima", true))
												.add(Restrictions.eq("kelompokKkn",
														namaTugasKelompok.getTugasKelompok().getKelompokKkn()))
												.setProjection(Projections.property("mahasiswa.id"))
												.createCriteria("mahasiswa").addOrder(Order.desc("tahunangkatan"))
												.addOrder(Order.asc("nim")).setMaxResults(Common.MAX_RESULT_500),
										Mahasiswa.class, false);
					} else if (namaTugasKelompok.getTugasKelompok().getKelompokPkl() != null) {
						hanyaMahasiswastempo = ConstantValues
								.simpleList(
										session.createCriteria(MahasiswaDapatKelompokPkl.class)
												.add(Restrictions.eq("diterima", true))
												.add(Restrictions.eq("kelompokPkl",
														namaTugasKelompok.getTugasKelompok().getKelompokPkl()))
												.setProjection(Projections.property("mahasiswa.id"))
												.createCriteria("mahasiswa").addOrder(Order.desc("tahunangkatan"))
												.addOrder(Order.asc("nim")).setMaxResults(Common.MAX_RESULT_500),
										Mahasiswa.class, false);
					}
					List<Mahasiswa> hanyaMahasiswas = new ArrayList<Mahasiswa>();
					for (Mahasiswa mahasiswa : hanyaMahasiswastempo) {
						if (!namaTugasKelompok.getTugasKelompok().getMhsYgTidakIkut()
								.contains("," + mahasiswa.getId() + ",")) {
							hanyaMahasiswas.add(mahasiswa);
						}
					}

					AmbilDataMahasiswaDariMahasiswaBanyak window = new AmbilDataMahasiswaDariMahasiswaBanyak(mahasiswas,
							hanyaMahasiswas);

					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
					window.setWidth("950px");
					window.setHeight("90%");

					window.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							List<Mahasiswa> mahasiswas = (List<Mahasiswa>) arg0.getData();

							if (mahasiswas != null) {
								Session session = HibernateUtil.currentSession();

								for (Mahasiswa mahasiswa : mahasiswas) {

									NamaTugasKelompokPunyaMahasiswa namaTugasKelompokPunyaMahasiswa = new NamaTugasKelompokPunyaMahasiswa();
									namaTugasKelompokPunyaMahasiswa.setMahasiswa(mahasiswa);
									namaTugasKelompokPunyaMahasiswa.setNamaTugasKelompok(namaTugasKelompok);

									session.save(namaTugasKelompokPunyaMahasiswa);
								}
								session.flush();

								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										loadData(null);
									}
								});

							}

						}
					});

					window.onModal();
					hanyaMahasiswastempo = null;
				}

			});
			button.setParent(toolbar);
		}

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(myDiv);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mahasiswa");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("20%");

		Tugas tugas = namaTugasKelompok.getTugasKelompok();
		Perkuliahan perkuliahan = namaTugasKelompok.getTugasKelompok() == null ? null
				: namaTugasKelompok.getTugasKelompok().getPerkuliahan();

		if (perkuliahan != null && perkuliahan.getKurikulum() != null
				&& perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {

			Session session = HibernateUtil.currentSession();
			List<FormatNilai> formatNilais = Common.getFormatNilais(session, perkuliahan);
			JSONObject jsonObject = new JSONObject(tugas.getFormatNilais());
			for (FormatNilai nilai : formatNilais) {
				if (nilai.getStatusPertemuan() != null) {
					if (!jsonObject.isNull(nilai.getId().toString())) {
						column = new MyColumnConfig();
						column.setStyle("font-size:9px;");
						column.setParent(columns);
						column.setLabel(nilai.getNama());
						column.setTooltiptext(nilai.getNama());
						column.setWidth("7%");
					}
				}
			}
		} else {

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nilai");
			/* Dilebarkan agar kotak nilai terbaca jelas (keluhan: angka
			 * tersembunyi, rawan salah input). */
			column.setWidth("18%");
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth(tbmuser != null && tbmuser.getMahasiswa() != null ? "0%" : "10%");

		loadData(null);

	}

}
