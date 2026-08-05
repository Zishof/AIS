package ais.action.master.pmb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import org.zkoss.zul.Messagebox;

import ais.common.Common;
import ais.common.CommonPMB;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiBerkas;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.JenisSeleksi;
import ais.database.model.Tbmuser;
import ais.database.model.VerifikasiKelengkapanCalonMahasiswa;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowStyled;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

public class VerifikasiPMBHelper {

	public static EventListener tampilkanGrid(final Component rowVerifikasi,
			final Rows subRowsVerifikasiKelengkapanCalonMahasiswa, final GelombangPendaftaran gel,
			final Long biodataCalonMahasiswaId) {
		return tampilkanGrid(rowVerifikasi, subRowsVerifikasiKelengkapanCalonMahasiswa, gel, biodataCalonMahasiswaId,
				null, false);
	}

	public static EventListener tampilkanGrid(final Component rowVerifikasi,
			final Rows subRowsVerifikasiKelengkapanCalonMahasiswa, final GelombangPendaftaran gel,
			final Long biodataCalonMahasiswaId, final List<VerifikasiKelengkapanCalonMahasiswa> verif,
			final boolean readonly) {

		buatGridVerifikasi(rowVerifikasi, subRowsVerifikasiKelengkapanCalonMahasiswa);

		EventListener eventListenerBerkas = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(subRowsVerifikasiKelengkapanCalonMahasiswa);

				if (gel == null) {
					setVisible(rowVerifikasi, false);
					return;
				}

				Session session = null;
				try {
					session = HibernateUtil.getSessionFactory().openSession();
					Tbmuser tbmuser = Common.getCurrentUser();

					GelombangPendaftaran gelAktif = reloadGelombang(session, gel);
					if (gelAktif == null) {
						setVisible(rowVerifikasi, false);
						return;
					}

					final boolean belumExpired = belumLewatTanggalDaftarUlang(gelAktif);
					List<VerifikasiKelengkapanCalonMahasiswa> verifikasiKelengkapanCalonMahasiswas = ambilVerifikasiEfektif(
							session, gelAktif, biodataCalonMahasiswaId, verif);

					setVisible(rowVerifikasi, !verifikasiKelengkapanCalonMahasiswas.isEmpty());
					if (verifikasiKelengkapanCalonMahasiswas.isEmpty()) {
						return;
					}

					Map<Long, BiodataCalonMahasiswaPunyaVerifikasiBerkas> mapBerkas = ambilMapBerkas(session,
							biodataCalonMahasiswaId, verifikasiKelengkapanCalonMahasiswas);

					for (final VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa : verifikasiKelengkapanCalonMahasiswas) {
						if (!isAktif(verifikasiKelengkapanCalonMahasiswa)) {
							continue;
						}

						BiodataCalonMahasiswaPunyaVerifikasiBerkas biodataCalonMahasiswaPunyaVerifikasiBerkas = mapBerkas
								.get(verifikasiKelengkapanCalonMahasiswa.getId());

						if (biodataCalonMahasiswaPunyaVerifikasiBerkas == null) {
							biodataCalonMahasiswaPunyaVerifikasiBerkas = buatBerkasJikaPerlu(session,
									biodataCalonMahasiswaId, verifikasiKelengkapanCalonMahasiswa);
						}

						renderBarisVerifikasi(subRowsVerifikasiKelengkapanCalonMahasiswa,
								verifikasiKelengkapanCalonMahasiswa, biodataCalonMahasiswaPunyaVerifikasiBerkas, tbmuser,
								readonly, belumExpired);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/VerifikasiPMBHelper.java:117");
				} finally {
					if (session != null && session.isOpen()) {
						try {
							session.close();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiPMBHelper.java:122");
						}
					}
				}
			}
		};

		return eventListenerBerkas;
	}

	private static void buatGridVerifikasi(Component rowVerifikasi, Rows subRowsVerifikasiKelengkapanCalonMahasiswa) {
		if (rowVerifikasi == null || subRowsVerifikasiKelengkapanCalonMahasiswa == null) {
			return;
		}

		MyGrid subGrid = new MyGrid();
		subGrid.setSclass("fgrid");
		rowVerifikasi.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);

		Column c = new Column("");
		c.setWidth("0%");
		subColumns.appendChild(c);

		c = new Column("Formulir Verifikasi Kelengkapan Berkas");
		c.setWidth("60%");
		subColumns.appendChild(c);

		c = new Column("Keterangan Kelengkapan Berkas");
		subColumns.appendChild(c);

		subRowsVerifikasiKelengkapanCalonMahasiswa.setParent(subGrid);
	}

	private static void setVisible(Component component, boolean visible) {
		if (component != null) {
			component.setVisible(visible);
		}
	}

	private static boolean belumLewatTanggalDaftarUlang(GelombangPendaftaran gel) {
		boolean belumExpired = true;
		try {
			if (gel != null && gel.getTanggalDaftarUlangBerakhir() != null) {
				Date sekarang = WaktuUtil.getDate();
				belumExpired = gel.getTanggalDaftarUlangBerakhir().after(sekarang)
						|| Common.dateFormat8.get().format(gel.getTanggalDaftarUlangBerakhir())
								.equals(Common.dateFormat8.get().format(sekarang));
			}
		} catch (Exception e) {
			belumExpired = true;
		}
		return belumExpired;
	}

	private static GelombangPendaftaran reloadGelombang(Session session, GelombangPendaftaran gel) {
		if (gel == null) {
			return null;
		}
		try {
			if (session != null && gel.getId() != null) {
				GelombangPendaftaran loaded = (GelombangPendaftaran) session.get(GelombangPendaftaran.class, gel.getId());
				return loaded == null ? gel : loaded;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiPMBHelper.java:188");
		}
		return gel;
	}

	private static JenisSeleksi reloadJenisSeleksi(Session session, JenisSeleksi jenisSeleksi) {
		if (jenisSeleksi == null) {
			return null;
		}
		try {
			if (session != null && jenisSeleksi.getId() != null) {
				JenisSeleksi loaded = (JenisSeleksi) session.get(JenisSeleksi.class, jenisSeleksi.getId());
				return loaded == null ? jenisSeleksi : loaded;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiPMBHelper.java:202");
		}
		return jenisSeleksi;
	}

	private static boolean isAktif(VerifikasiKelengkapanCalonMahasiswa data) {
		try {
			return data != null && data.getId() != null && data.getAktif();
		} catch (Exception e) {
			return false;
		}
	}

	private static List<VerifikasiKelengkapanCalonMahasiswa> ambilVerifikasiEfektif(Session session,
			GelombangPendaftaran gel, Long biodataCalonMahasiswaId, List<VerifikasiKelengkapanCalonMahasiswa> verif) {
		Map<Long, VerifikasiKelengkapanCalonMahasiswa> map = new LinkedHashMap<Long, VerifikasiKelengkapanCalonMahasiswa>();

		if (verif != null) {
			for (VerifikasiKelengkapanCalonMahasiswa v : verif) {
				tambahJikaAktif(map, v);
			}
			return sortVerifikasi(map);
		}

		GelombangPendaftaran gelAktif = reloadGelombang(session, gel);
		if (gelAktif == null) {
			return new ArrayList<VerifikasiKelengkapanCalonMahasiswa>();
		}

		Set<VerifikasiKelengkapanCalonMahasiswa> pilihanGelombang = null;
		try {
			pilihanGelombang = gelAktif.getVerifikasiKelengkapanCalonMahasiswas();
		} catch (Exception e) {
			pilihanGelombang = null;
		}

		if (pilihanGelombang != null) {
			for (VerifikasiKelengkapanCalonMahasiswa v : pilihanGelombang) {
				tambahJikaAktif(map, v);
			}
		}

		if (map.isEmpty()) {
			return new ArrayList<VerifikasiKelengkapanCalonMahasiswa>();
		}

		JenisSeleksi jenisSeleksi = ambilJenisSeleksiCalon(session, biodataCalonMahasiswaId);
		if (jenisSeleksi != null) {
			filterDenganJenisSeleksiJikaAda(session, map, jenisSeleksi);
		}

		return sortVerifikasi(map);
	}

	private static JenisSeleksi ambilJenisSeleksiCalon(Session session, Long biodataCalonMahasiswaId) {
		if (session == null || biodataCalonMahasiswaId == null) {
			return null;
		}
		try {
			BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) session.get(
					BiodataCalonMahasiswa.class, biodataCalonMahasiswaId);
			if (biodataCalonMahasiswa != null) {
				return reloadJenisSeleksi(session, biodataCalonMahasiswa.getJenisSeleksi());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiPMBHelper.java:266");
		}
		return null;
	}

	private static void filterDenganJenisSeleksiJikaAda(Session session, Map<Long, VerifikasiKelengkapanCalonMahasiswa> map,
			JenisSeleksi jenisSeleksi) {
		if (map == null || map.isEmpty() || jenisSeleksi == null) {
			return;
		}

		Map<Long, VerifikasiKelengkapanCalonMahasiswa> mapJenisSeleksi = new HashMap<Long, VerifikasiKelengkapanCalonMahasiswa>();
		try {
			JenisSeleksi loaded = reloadJenisSeleksi(session, jenisSeleksi);
			Set<VerifikasiKelengkapanCalonMahasiswa> pilihanJenisSeleksi = loaded == null ? null
					: loaded.getVerifikasiKelengkapanCalonMahasiswas();
			if (pilihanJenisSeleksi == null || pilihanJenisSeleksi.isEmpty()) {
				return;
			}
			for (VerifikasiKelengkapanCalonMahasiswa v : pilihanJenisSeleksi) {
				if (isAktif(v)) {
					mapJenisSeleksi.put(v.getId(), v);
				}
			}
			if (mapJenisSeleksi.isEmpty()) {
				return;
			}
			List<Long> hapus = new ArrayList<Long>();
			for (Long id : map.keySet()) {
				if (!mapJenisSeleksi.containsKey(id)) {
					hapus.add(id);
				}
			}
			for (Long id : hapus) {
				map.remove(id);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiPMBHelper.java:302");
		}
	}

	private static void tambahJikaAktif(Map<Long, VerifikasiKelengkapanCalonMahasiswa> map,
			VerifikasiKelengkapanCalonMahasiswa data) {
		if (map == null || !isAktif(data)) {
			return;
		}
		if (!map.containsKey(data.getId())) {
			map.put(data.getId(), data);
		}
	}

	private static List<VerifikasiKelengkapanCalonMahasiswa> sortVerifikasi(
			Map<Long, VerifikasiKelengkapanCalonMahasiswa> map) {
		List<VerifikasiKelengkapanCalonMahasiswa> list = new ArrayList<VerifikasiKelengkapanCalonMahasiswa>();
		if (map != null) {
			list.addAll(map.values());
		}
		try {
			Collections.sort(list);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiPMBHelper.java:324");
		}
		return list;
	}

	private static Map<Long, BiodataCalonMahasiswaPunyaVerifikasiBerkas> ambilMapBerkas(Session session,
			Long biodataCalonMahasiswaId, List<VerifikasiKelengkapanCalonMahasiswa> verifikasiKelengkapanCalonMahasiswas) {
		Map<Long, BiodataCalonMahasiswaPunyaVerifikasiBerkas> mapBerkas = new HashMap<Long, BiodataCalonMahasiswaPunyaVerifikasiBerkas>();
		if (session == null || biodataCalonMahasiswaId == null || verifikasiKelengkapanCalonMahasiswas == null
				|| verifikasiKelengkapanCalonMahasiswas.isEmpty()) {
			return mapBerkas;
		}

		List<Long> verifIds = new ArrayList<Long>();
		for (VerifikasiKelengkapanCalonMahasiswa v : verifikasiKelengkapanCalonMahasiswas) {
			if (isAktif(v)) {
				verifIds.add(v.getId());
			}
		}

		if (verifIds.isEmpty()) {
			return mapBerkas;
		}

		try {
			@SuppressWarnings("unchecked")
			List<BiodataCalonMahasiswaPunyaVerifikasiBerkas> existingBerkasList = session
					.createCriteria(BiodataCalonMahasiswaPunyaVerifikasiBerkas.class)
					.add(Restrictions.eq("biodataCalonMahasiswa.id", biodataCalonMahasiswaId))
					.add(Restrictions.in("verifikasiKelengkapanCalonMahasiswa.id", verifIds)).list();
			for (BiodataCalonMahasiswaPunyaVerifikasiBerkas b : existingBerkasList) {
				if (b != null && b.getVerifikasiKelengkapanCalonMahasiswa() != null
						&& b.getVerifikasiKelengkapanCalonMahasiswa().getId() != null) {
					mapBerkas.put(b.getVerifikasiKelengkapanCalonMahasiswa().getId(), b);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiPMBHelper.java:360");
		}
		return mapBerkas;
	}

	private static BiodataCalonMahasiswaPunyaVerifikasiBerkas buatBerkasJikaPerlu(Session session,
			Long biodataCalonMahasiswaId, VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa) {
		BiodataCalonMahasiswaPunyaVerifikasiBerkas data = new BiodataCalonMahasiswaPunyaVerifikasiBerkas();
		if (verifikasiKelengkapanCalonMahasiswa != null) {
			data.setVerifikasiKelengkapanCalonMahasiswa(verifikasiKelengkapanCalonMahasiswa);
		}
		if (biodataCalonMahasiswaId == null) {
			return data;
		}

		BiodataCalonMahasiswa mhsRef = new BiodataCalonMahasiswa(biodataCalonMahasiswaId);
		data.setBiodataCalonMahasiswa(mhsRef);

		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.save(data);
			tx.commit();
		} catch (Exception ex) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiPMBHelper.java:387");
				}
			}
		}
		return data;
	}

	private static void renderBarisVerifikasi(final Rows subRowsVerifikasiKelengkapanCalonMahasiswa,
			final VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa,
			final BiodataCalonMahasiswaPunyaVerifikasiBerkas biodataCalonMahasiswaPunyaVerifikasiBerkas,
			Tbmuser tbmuser, final boolean readonly, final boolean belumExpired) {

		final BiodataCalonMahasiswaPunyaVerifikasiBerkas fix = biodataCalonMahasiswaPunyaVerifikasiBerkas;

		final Row subRow = new MyRowStyled();
		subRow.setAttribute("verifikasiKelengkapanCalonMahasiswa", verifikasiKelengkapanCalonMahasiswa);
		subRow.setAttribute("biodataCalonMahasiswaPunyaVerifikasiBerkas", biodataCalonMahasiswaPunyaVerifikasiBerkas);
		subRow.setParent(subRowsVerifikasiKelengkapanCalonMahasiswa);
		subRow.setValign("top");

		final MyDetail myvbox1 = new MyDetail();
		myvbox1.setParent(subRow);

		final boolean isAdmin = tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null && !readonly;
		if (isAdmin) {
			renderBarisAdmin(subRow, myvbox1, verifikasiKelengkapanCalonMahasiswa,
					biodataCalonMahasiswaPunyaVerifikasiBerkas, belumExpired);
		} else {
			renderBarisCalon(subRow, verifikasiKelengkapanCalonMahasiswa, biodataCalonMahasiswaPunyaVerifikasiBerkas,
					readonly);
		}

		renderUploadBerkasUtama(myvbox1, subRow, fix, readonly, belumExpired, isAdmin);
	}

	private static void renderBarisAdmin(final Row subRow, final MyDetail myvbox1,
			final VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa,
			final BiodataCalonMahasiswaPunyaVerifikasiBerkas biodataCalonMahasiswaPunyaVerifikasiBerkas,
			final boolean belumExpired) {
		Vbox vboxBerkas = new Vbox();
		vboxBerkas.setParent(subRow);

		final Checkbox checkbox = new Checkbox("Sesuai", "/img/Check-icon.png");
		checkbox.setStyle("font-size:11px;font-weight: bolder;");
		checkbox.setParent(vboxBerkas);
		checkbox.setChecked(biodataCalonMahasiswaPunyaVerifikasiBerkas != null
				&& biodataCalonMahasiswaPunyaVerifikasiBerkas.getVerified());

		new ais.ui.util.MyHtml("<div style=\"font-size:12px;\">" + escapeHtml(namaVerifikasi(verifikasiKelengkapanCalonMahasiswa))
				+ (wajib(verifikasiKelengkapanCalonMahasiswa) ? " (wajib) 👉" : " (tidak wajib)") + "</div>")
				.setParent(vboxBerkas);

		Hbox hbox = new Hbox();
		hbox.setParent(vboxBerkas);
		LampiranLain.createDownloadUploadFileLain(hbox, verifikasiKelengkapanCalonMahasiswa.getId(),
				VerifikasiKelengkapanCalonMahasiswa.class.getName(), "Lampiran", false, null, null, false, false, false,
				false);

		subRow.setAttribute("checkbox", checkbox);

		final Textbox keterangan = new Textbox(biodataCalonMahasiswaPunyaVerifikasiBerkas == null ? ""
				: biodataCalonMahasiswaPunyaVerifikasiBerkas.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setParent(subRow);
		keterangan.setDisabled(checkbox.isChecked());

		subRow.setAttribute("keterangan", keterangan);

		final BiodataCalonMahasiswaPunyaVerifikasiBerkas bio = biodataCalonMahasiswaPunyaVerifikasiBerkas;
		checkbox.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				keterangan.setDisabled(checkbox.isChecked());

				Common.clear(myvbox1);
				Hbox hbox = new Hbox();
				hbox.setStyle(subRow.getStyle());
				hbox.setParent(myvbox1);
				hbox.setWidth("100%");

				if (bio == null || bio.getId() == null) {
					new ais.ui.util.MyHtml("<span style='color:#888;font-size:11px;'>Simpan data pendaftaran terlebih dahulu untuk mengunggah berkas.</span>")
							.setParent(hbox);
				} else {
					LampiranLain.createDownloadUploadFileLain(hbox, bio.getId(),
							BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName(), "Berkas", false,
							new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									LampiranLain lampiranLain = (LampiranLain) arg0.getData();
									if (lampiranLain != null) {
										bio.setUploaded(true);
										bio.setNamaFile(lampiranLain.getNama());
										Common.refreshUpdate(bio);
									}
								}
							}, null, false, false, false, !checkbox.isChecked(), null, false, false);
					updateStatusUploadDariAtribut(hbox, bio);
					// Tombol hapus berkas untuk admin (selalu tampil jika file ada)
					tambahTombolHapusBerkasAdmin(hbox, myvbox1, subRow, bio, belumExpired);
				}
				new ais.ui.util.MyHtml("<hr style='border: 1px solid #bdbbbb;'>").setParent(myvbox1);
			}
		});

		final BiodataCalonMahasiswaPunyaVerifikasiBerkas data = biodataCalonMahasiswaPunyaVerifikasiBerkas;
		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (data == null || data.getId() == null) {
					return;
				}
				data.setVerified(checkbox.isChecked());
				data.setKeterangan(keterangan.getValue());
				Common.refreshSaveOrUpdate(data);
				if (checkbox.isChecked()) {
					BiodataCalonMahasiswa biodata = data.getBiodataCalonMahasiswa();
					if (biodata != null && biodata.getId() != null
							&& semuaBerkasWajibSudahTerverifikasi(biodata.getId())) {
						try {
							CommonPMB.generateNoUjian(Common.getCurrentUser(), biodata, null);
						} catch (Exception e) {
							ais.common.ErrorAuditUtil.record(e,
									"auto-audit src/ais/action/master/pmb/VerifikasiPMBHelper.java");
						}
					}
				}
			}
		};

		checkbox.addEventListener("onClick", eventListener);
		keterangan.addEventListener("onChange", eventListener);
	}

	private static void renderBarisCalon(final Row subRow,
			final VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa,
			final BiodataCalonMahasiswaPunyaVerifikasiBerkas biodataCalonMahasiswaPunyaVerifikasiBerkas,
			boolean readonly) {
		Vbox vboxBerkas = new Vbox();
		vboxBerkas.setParent(subRow);

		boolean verified = biodataCalonMahasiswaPunyaVerifikasiBerkas != null
				&& biodataCalonMahasiswaPunyaVerifikasiBerkas.getVerified();
		new ais.ui.util.MyHtml(verified ? "<font style='font-weight:bold;color:blue;font-size:xx-small'>"
				+ escapeHtml(namaVerifikasi(verifikasiKelengkapanCalonMahasiswa)) + " ==> Telah Sesuai</font>"
				: "<font style='font-weight:bold;color:red;font-size:xx-small'>"
						+ escapeHtml(namaVerifikasi(verifikasiKelengkapanCalonMahasiswa))
						+ " ==> Belum Diverifikasi</font>"
						+ (wajib(verifikasiKelengkapanCalonMahasiswa) ? " (wajib) 👉" : " (tidak wajib)"))
				.setParent(vboxBerkas);

		Hbox hbox = new Hbox();
		hbox.setParent(vboxBerkas);

		FileFotoLain fileFotoLain = LampiranLain.ambil(verifikasiKelengkapanCalonMahasiswa.getId(),
				VerifikasiKelengkapanCalonMahasiswa.class.getName());
		if (!readonly || fileFotoLain != null) {
			LampiranLain.createDownloadUploadFileLain(hbox, verifikasiKelengkapanCalonMahasiswa.getId(),
					VerifikasiKelengkapanCalonMahasiswa.class.getName(), "Lampiran", false, null, null, false, false,
					false, false);
		}

		String keterangan = biodataCalonMahasiswaPunyaVerifikasiBerkas == null ? ""
				: biodataCalonMahasiswaPunyaVerifikasiBerkas.getKeterangan();
		if (keterangan != null && keterangan.length() > 0) {
			new ais.ui.util.MyHtml("Keterangan:<br>" + escapeHtml(keterangan)).setParent(subRow);
		} else {
			new ais.ui.util.MyHtml("").setParent(subRow);
		}
	}

	private static void renderUploadBerkasUtama(final MyDetail myvbox1, final Row subRow,
			final BiodataCalonMahasiswaPunyaVerifikasiBerkas fix, final boolean readonly, final boolean belumExpired,
			final boolean isAdmin) {
		final Hbox hbox = new Hbox();
		hbox.setParent(myvbox1);
		hbox.setStyle(subRow.getStyle());
		hbox.setWidth("100%");

		if (fix == null || fix.getId() == null) {
			new ais.ui.util.MyHtml("<span style='color:#888;font-size:11px;'>Simpan data pendaftaran terlebih dahulu untuk mengunggah berkas.</span>")
					.setParent(hbox);
			new ais.ui.util.MyHtml("<hr style='border: 1px solid #bdbbbb;'>").setParent(myvbox1);
			myvbox1.setOpen(true);
			return;
		}

		final FileFotoLain fileFotoLain = LampiranLain.ambil(fix.getId(),
				BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName());
		if (readonly && fileFotoLain == null) {
			hbox.appendChild(new MyLabelAgakKecilBoldMerah("Tidak/belum diupload"));
		} else {
			LampiranLain.createDownloadUploadFileLain(hbox, fix.getId(),
					BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName(), "Berkas", false, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getData();
							if (lampiranLain != null) {
								fix.setUploaded(true);
								fix.setNamaFile(lampiranLain.getNama());
								Common.refreshUpdate(fix);
							}
						}
					}, null, false, false, false, isAdmin ? !fix.getVerified() : (!fix.getVerified() && belumExpired && !readonly), null, false, false);
			updateStatusUploadDariAtribut(hbox, fix);
			// Tombol hapus berkas untuk admin (selalu tampil jika file ada)
			if (isAdmin) {
				tambahTombolHapusBerkasAdmin(hbox, myvbox1, subRow, fix, belumExpired);
			}
		}
		new ais.ui.util.MyHtml("<hr style='border: 1px solid #bdbbbb;'>").setParent(myvbox1);
		myvbox1.setOpen(true);
	}

	private static void updateStatusUploadDariAtribut(Hbox hbox, BiodataCalonMahasiswaPunyaVerifikasiBerkas data) {
		try {
			if (hbox != null && data != null && hbox.getAttribute("jumlah_upload") != null) {
				Integer jumlahUpload = (Integer) hbox.getAttribute("jumlah_upload");
				if (jumlahUpload != null && jumlahUpload.intValue() > 0 && !data.getUploaded()) {
					data.setUploaded(true);
					Common.refreshUpdate(data);
				} else if (jumlahUpload != null && jumlahUpload.intValue() == 0 && data.getUploaded()) {
					data.setUploaded(false);
					Common.refreshUpdate(data);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiPMBHelper.java:602");
		}
	}

	private static String namaVerifikasi(VerifikasiKelengkapanCalonMahasiswa data) {
		try {
			return data == null || data.getNama() == null ? "" : data.getNama();
		} catch (Exception e) {
			return "";
		}
	}

	private static boolean wajib(VerifikasiKelengkapanCalonMahasiswa data) {
		try {
			return data != null && data.getWajib();
		} catch (Exception e) {
			return true;
		}
	}

	private static String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		String s = value;
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		s = s.replace("\"", "&quot;");
		s = s.replace("'", "&#39;");
		return s;
	}

	public static boolean checkVerifikasi(BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		if (biodataCalonMahasiswa == null) {
			return true;
		}
		// Form upload berkas hanya ditampilkan untuk catatan yang sudah tersimpan (id != null).
		// Untuk catatan baru, tidak ada form upload yang bisa diisi — lewati cek agar simpan tidak diblokir.
		if (biodataCalonMahasiswa.getId() == null) {
			return true;
		}

		GelombangPendaftaran gel = biodataCalonMahasiswa.getGelombangPendaftaran();
		if (gel == null) {
			return true;
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			GelombangPendaftaran gelAktif = reloadGelombang(session, gel);
			List<VerifikasiKelengkapanCalonMahasiswa> verifikasiKelengkapanCalonMahasiswas = ambilVerifikasiEfektif(
					session, gelAktif, biodataCalonMahasiswa.getId(), null);

			if (verifikasiKelengkapanCalonMahasiswas.isEmpty()) {
				return true;
			}

			Map<Long, BiodataCalonMahasiswaPunyaVerifikasiBerkas> mapBerkas = ambilMapBerkas(session,
					biodataCalonMahasiswa.getId(), verifikasiKelengkapanCalonMahasiswas);

			for (final VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa : verifikasiKelengkapanCalonMahasiswas) {
				if (!isAktif(verifikasiKelengkapanCalonMahasiswa)) {
					continue;
				}

				BiodataCalonMahasiswaPunyaVerifikasiBerkas berkas = mapBerkas
						.get(verifikasiKelengkapanCalonMahasiswa.getId());
				if (berkas == null) {
					berkas = buatBerkasJikaPerlu(session, biodataCalonMahasiswa.getId(), verifikasiKelengkapanCalonMahasiswa);
				}

				if (wajib(verifikasiKelengkapanCalonMahasiswa)) {
					FileFotoLain fileFotoLain = berkas == null || berkas.getId() == null ? null
							: FileFotoLain.ambil(false, berkas.getId(),
									BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName(), LampiranLain.class);
					if (fileFotoLain == null) {
						String nama = verifikasiKelengkapanCalonMahasiswa.getNama();
						MyMessageboxConfig.show(
								"Maaf, Anda belum dapat melanjutkan karena berkas \"" + nama + "\" belum diunggah.\n\n"
								+ "Langkah yang perlu dilakukan:\n"
								+ "1. Buka menu \"Kelengkapan Berkas\" atau \"Unggah Berkas\" di halaman utama PMB.\n"
								+ "2. Cari berkas \"" + nama + "\", lalu klik tombol Unggah.\n"
								+ "3. Pilih file dari perangkat Anda (format: PDF, JPG, atau PNG).\n"
								+ "4. Setelah berhasil diunggah, coba langkah ini kembali.\n\n"
								+ "Jika mengalami kendala, hubungi panitia penerimaan mahasiswa baru.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return false;
					}
				}

				if (wajib(verifikasiKelengkapanCalonMahasiswa) && verifikasiKelengkapanCalonMahasiswa.getVerifikasi()
						&& (berkas == null || !berkas.getVerified())) {
					String nama = verifikasiKelengkapanCalonMahasiswa.getNama();
					MyMessageboxConfig.show(
							"Maaf, Anda belum dapat melanjutkan karena berkas \"" + nama + "\" belum diverifikasi oleh panitia.\n\n"
							+ "Hal ini berarti berkas Anda masih dalam proses pemeriksaan.\n\n"
							+ "Yang perlu Anda lakukan:\n"
							+ "1. Pastikan Anda telah mengunggah berkas \"" + nama + "\" dengan benar dan jelas terbaca.\n"
							+ "2. Tunggu konfirmasi verifikasi dari panitia (biasanya 1-2 hari kerja).\n"
							+ "3. Jika sudah lebih dari 2 hari kerja belum ada konfirmasi, hubungi panitia penerimaan mahasiswa baru untuk memastikan status berkas Anda.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/VerifikasiPMBHelper.java:709");
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiPMBHelper.java:714");
				}
			}
		}
		return true;
	}

	/** Diam (tanpa Messagebox): true hanya jika seluruh berkas wajib sudah diunggah DAN diverifikasi admin. */
	static boolean semuaBerkasWajibSudahTerverifikasi(Long biodataCalonMahasiswaId) {
		if (biodataCalonMahasiswaId == null) {
			return false;
		}
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			BiodataCalonMahasiswa biodata = (BiodataCalonMahasiswa) session.get(
					BiodataCalonMahasiswa.class, biodataCalonMahasiswaId);
			if (biodata == null || biodata.getGelombangPendaftaran() == null) {
				return false;
			}
			GelombangPendaftaran gelAktif = reloadGelombang(session, biodata.getGelombangPendaftaran());
			List<VerifikasiKelengkapanCalonMahasiswa> list = ambilVerifikasiEfektif(
					session, gelAktif, biodataCalonMahasiswaId, null);
			if (list.isEmpty()) {
				return true;
			}
			Map<Long, BiodataCalonMahasiswaPunyaVerifikasiBerkas> mapBerkas = ambilMapBerkas(
					session, biodataCalonMahasiswaId, list);
			for (VerifikasiKelengkapanCalonMahasiswa v : list) {
				if (!isAktif(v) || !wajib(v)) {
					continue;
				}
				BiodataCalonMahasiswaPunyaVerifikasiBerkas berkas = mapBerkas.get(v.getId());
				if (berkas == null || berkas.getId() == null) {
					return false;
				}
				FileFotoLain file = FileFotoLain.ambil(false, berkas.getId(),
						BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName(), LampiranLain.class);
				if (file == null) {
					return false;
				}
				if (v.getVerifikasi() && !Boolean.TRUE.equals(berkas.getVerified())) {
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/action/master/pmb/VerifikasiPMBHelper.java");
			return false;
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiPMBHelper.java"); }
			}
		}
	}

	private static String cekBerkasWajibUpload(BiodataCalonMahasiswa biodataCalonMahasiswa, boolean untukUjian) {
		if (biodataCalonMahasiswa == null || biodataCalonMahasiswa.getId() == null
				|| biodataCalonMahasiswa.getGelombangPendaftaran() == null) {
			return null;
		}
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			GelombangPendaftaran gelAktif = reloadGelombang(session, biodataCalonMahasiswa.getGelombangPendaftaran());
			List<VerifikasiKelengkapanCalonMahasiswa> list = ambilVerifikasiEfektif(
					session, gelAktif, biodataCalonMahasiswa.getId(), null);
			if (list.isEmpty()) {
				return null;
			}
			Map<Long, BiodataCalonMahasiswaPunyaVerifikasiBerkas> mapBerkas = ambilMapBerkas(
					session, biodataCalonMahasiswa.getId(), list);
			for (VerifikasiKelengkapanCalonMahasiswa v : list) {
				if (!isAktif(v)) {
					continue;
				}
				boolean flagSesuai = untukUjian ? v.getWajibUploadSebelumUjian() : v.getWajibUploadSebelumInterview();
				if (!flagSesuai) {
					continue;
				}
				BiodataCalonMahasiswaPunyaVerifikasiBerkas berkas = mapBerkas.get(v.getId());
				if (berkas == null) {
					berkas = buatBerkasJikaPerlu(session, biodataCalonMahasiswa.getId(), v);
				}
				FileFotoLain fileFotoLain = berkas == null || berkas.getId() == null ? null
						: FileFotoLain.ambil(false, berkas.getId(),
								BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName(), LampiranLain.class);
				if (fileFotoLain == null) {
					String jenis = untukUjian ? "Ujian" : "Wawancara/Interview";
					String tombol = untukUjian ? "\"Ikut Ujian\"" : "\"Wawancara/Interview\"";
					return "Maaf, Anda belum dapat mengikuti " + jenis + " karena berkas \""
							+ v.getNama() + "\" belum diunggah.\n\n"
							+ "Langkah yang perlu dilakukan:\n"
							+ "1. Tutup jendela ini.\n"
							+ "2. Buka menu \"Kelengkapan Berkas\" atau \"Unggah Berkas\" di halaman utama PMB.\n"
							+ "3. Cari berkas \"" + v.getNama() + "\", lalu klik tombol Unggah.\n"
							+ "4. Pilih file dari perangkat Anda (format: PDF, JPG, atau PNG).\n"
							+ "5. Setelah berhasil diunggah, kembali ke halaman ini dan klik " + tombol + " kembali.\n\n"
							+ "Jika mengalami kendala:\n"
							+ "- Pastikan format dan ukuran file sesuai ketentuan panitia.\n"
							+ "- Coba gunakan browser lain (Chrome / Firefox) jika proses unggah gagal.\n"
							+ "- Hubungi panitia penerimaan mahasiswa baru untuk mendapatkan bantuan lebih lanjut.";
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/VerifikasiPMBHelper.java:770");
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiPMBHelper.java:775");
				}
			}
		}
		return null;
	}

	/** Kembalikan pesan kendala jika ada berkas wajib-ujian yang belum diunggah; null jika aman. Aman dipanggil dari JSP. */
	public static String ambilPesanGagalSebelumUjian(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return cekBerkasWajibUpload(biodataCalonMahasiswa, true);
	}

	/** Kembalikan pesan kendala jika ada berkas wajib-interview yang belum diunggah; null jika aman. Aman dipanggil dari JSP. */
	public static String ambilPesanGagalSebelumInterview(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return cekBerkasWajibUpload(biodataCalonMahasiswa, false);
	}

	public static boolean checkVerifikasiSebelumUjian(BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		String pesan = ambilPesanGagalSebelumUjian(biodataCalonMahasiswa);
		if (pesan != null) {
			MyMessageboxConfig.show(pesan, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		return true;
	}

	public static boolean checkVerifikasiSebelumInterview(BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		String pesan = ambilPesanGagalSebelumInterview(biodataCalonMahasiswa);
		if (pesan != null) {
			MyMessageboxConfig.show(pesan, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		return true;
	}

	public static Rows tampilkanVerifikasi(BiodataCalonMahasiswa biodataCalonMahasiswa, Rows rows,
			Combobox gelombangPendaftaran, Combobox jenisSeleksi, GelombangPendaftaran gelombang) throws Exception {
		return tampilkanVerifikasi(biodataCalonMahasiswa, rows, gelombangPendaftaran, jenisSeleksi, gelombang, null);
	}

	@SuppressWarnings("deprecation")
	public static Rows tampilkanVerifikasi(final BiodataCalonMahasiswa biodataCalonMahasiswa, Rows rows,
			final Combobox gelombangPendaftaran, final Combobox jenisSeleksi, final GelombangPendaftaran gelombang,
			final List<VerifikasiKelengkapanCalonMahasiswa> verif) throws Exception {

		final Row rowBerkas = new MyRowStyled();
		rowBerkas.setVisible(false);
		rowBerkas.setParent(rows);
		rowBerkas.appendChild(new ais.ui.util.MyLabelBold("- Verifikasi Kelengkapan Berkas"));

		final Row rowVerifikasi = new MyRowStyled();
		rowVerifikasi.setVisible(false);
		ais.ui.util.ZkCompat.setSpans(rowVerifikasi, "2");
		rowVerifikasi.setParent(rows);

		final Rows subRowsVerifikasiKelengkapanCalonMahasiswa = new Rows();

		final EventListener eventListenerBerkas = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowVerifikasi);
				try {
					if (subRowsVerifikasiKelengkapanCalonMahasiswa.getParent() != null) {
						subRowsVerifikasiKelengkapanCalonMahasiswa.detach();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiPMBHelper.java:840");
				}

				GelombangPendaftaran gelAktif = ambilGelombangAktif(biodataCalonMahasiswa, gelombangPendaftaran,
						gelombang);
				EventListener render = VerifikasiPMBHelper.tampilkanGrid(rowVerifikasi,
						subRowsVerifikasiKelengkapanCalonMahasiswa, gelAktif,
						biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getId(), verif, false);
				render.onEvent(arg0);
				rowBerkas.setVisible(rowVerifikasi.isVisible());
			}
		};

		eventListenerBerkas.onEvent(null);

		if (gelombangPendaftaran != null) {
			gelombangPendaftaran.addEventListener("onChange", eventListenerBerkas);
		}
		if (jenisSeleksi != null) {
			jenisSeleksi.addEventListener("onChange", eventListenerBerkas);
		}

		return subRowsVerifikasiKelengkapanCalonMahasiswa;
	}

	private static GelombangPendaftaran ambilGelombangAktif(BiodataCalonMahasiswa biodataCalonMahasiswa,
			Combobox gelombangPendaftaran, GelombangPendaftaran gelombang) {
		if (gelombang != null) {
			return gelombang;
		}
		try {
			if (gelombangPendaftaran != null && gelombangPendaftaran.getSelectedItem() != null
					&& gelombangPendaftaran.getSelectedItem().getValue() instanceof GelombangPendaftaran) {
				return (GelombangPendaftaran) gelombangPendaftaran.getSelectedItem().getValue();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiPMBHelper.java:875");
		}
		try {
			return biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getGelombangPendaftaran();
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static void simpanVerifikasi(BiodataCalonMahasiswa biodataCalonMahasiswa,
			Rows subRowsVerifikasiKelengkapanCalonMahasiswa) {
		if (subRowsVerifikasiKelengkapanCalonMahasiswa != null) {
			List<Row> rowsVerifikasi = subRowsVerifikasiKelengkapanCalonMahasiswa.getChildren();
			for (Row row : rowsVerifikasi) {
				try {
					VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa = (VerifikasiKelengkapanCalonMahasiswa) row
							.getAttribute("verifikasiKelengkapanCalonMahasiswa");
					BiodataCalonMahasiswaPunyaVerifikasiBerkas data = (BiodataCalonMahasiswaPunyaVerifikasiBerkas) row
							.getAttribute("biodataCalonMahasiswaPunyaVerifikasiBerkas");

					if (!isAktif(verifikasiKelengkapanCalonMahasiswa)) {
						continue;
					}

					Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
					Textbox keterangan = null;
					if (row.getAttribute("keterangan") != null && row.getAttribute("keterangan") instanceof Textbox) {
						keterangan = (Textbox) row.getAttribute("keterangan");
					}

					if (data == null) {
						data = new BiodataCalonMahasiswaPunyaVerifikasiBerkas();
						data.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
						data.setVerifikasiKelengkapanCalonMahasiswa(verifikasiKelengkapanCalonMahasiswa);
					}
					if (checkbox != null) {
						data.setVerified(checkbox.isChecked());
					}
					if (keterangan != null) {
						data.setKeterangan(keterangan.getValue());
					}

					Common.refreshSaveOrUpdate(data);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiPMBHelper.java:919");
					// Lanjutkan proses verifikasi baris selanjutnya meskipun baris saat ini gagal.
				}
			}
		}
	}

	/**
	 * Tambahkan tombol "Hapus Berkas" untuk admin di samping tombol upload/download.
	 * Hanya muncul jika ada file yang sudah di-upload oleh calon.
	 * Setelah hapus, myvbox1 di-refresh agar tampilan sinkron.
	 */
	private static void tambahTombolHapusBerkasAdmin(final Hbox hbox, final MyDetail myvbox1,
			final Row subRow, final BiodataCalonMahasiswaPunyaVerifikasiBerkas bio,
			final boolean belumExpired) {
		if (bio == null || bio.getId() == null) {
			return;
		}
		final FileFotoLain cekFile = LampiranLain.ambil(bio.getId(),
				BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName());
		if (cekFile == null) {
			return;
		}

		// Tambahkan ke containerTombol (jika createDownloadUploadFileLain sudah membuatnya)
		// agar posisi tombol sejajar. Jika tidak ada, langsung ke hbox.
		Component target = hbox;
		try {
			Object tombol = hbox.getAttribute("tombol");
			if (tombol instanceof Component) {
				target = (Component) tombol;
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiPMBHelper.java:951");
		}

		final MyToolbarbuttonConfig hapusBerkasBtn = new MyToolbarbuttonConfig("Hapus Berkas",
				"/img/svg/trash.svg");
		hapusBerkasBtn.setStyle("font-size:9px;color:red;");
		hapusBerkasBtn.setParent(target);

		hapusBerkasBtn.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show(
						"Apakah Bapak/Ibu yakin ingin menghapus berkas yang telah diunggah? Berkas yang telah dihapus tidak dapat dikembalikan lagi. Silakan tekan OK untuk melanjutkan penghapusan, atau Batal untuk mengurungkan.",
						"Konfirmasi Hapus",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {
							@Override
							public void onEvent(Event e) throws Exception {
								if (Integer.parseInt(e.getData().toString()) != MyMessageboxConfig.OK) {
									return;
								}
								// Hapus file dari storage
								Session delSession = null;
								try {
									delSession = StreamingHibernateUtil.getInstance().openSession();
									LampiranLain hapusTemp = new LampiranLain();
									FileFotoLain.hapusAtauUpdate(hapusTemp, delSession, false, bio.getId(),
											BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName());
								} catch (Exception ex) {
									ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/pmb/VerifikasiPMBHelper.java:980");
									Common.tampilErrorJikaAdmin(ex);
									return;
								} finally {
									if (delSession != null && delSession.isOpen()) {
										try {
											delSession.close();
										} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiPMBHelper.java:987");
										}
									}
								}
								// Perbarui status upload pada entitas
								try {
									bio.setUploaded(false);
									bio.setNamaFile(null);
									Common.refreshUpdate(bio);
								} catch (Exception ex) {
									ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/pmb/VerifikasiPMBHelper.java:997");
								}
								// Refresh tampilan myvbox1 (admin, readonly=false, isAdmin=true)
								Common.clear(myvbox1);
								renderUploadBerkasUtama(myvbox1, subRow, bio, false, belumExpired, true);
							}
						});
			}
		});
	}
}
