package ais.action.master.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.KelompokParameterTambahanMahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyMessageboxConfig;

public class ParameterTambahanMahasiswaListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private BiodataMahasiswa biodataMahasiswa;
	private Map<String, LampiranLain> lampiranLains;

	public ParameterTambahanMahasiswaListener(BiodataMahasiswa biodataMahasiswa, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.rows = rows;
		this.biodataMahasiswa = biodataMahasiswa;
		this.lampiranLains = lampiranLains;
	}

	@SuppressWarnings("unchecked")
	public static boolean validate(BiodataMahasiswa biodataMahasiswa, EventListener eventListener,
			final Boolean tampilMessage) throws Exception {
		if (biodataMahasiswa == null) {
			return true;
		}
		Integer gel = biodataMahasiswa.getMahasiswa() == null ? null
				: biodataMahasiswa.getMahasiswa().getTahunangkatan();
		List<ParameterTambahanMahasiswa> parameterTambahanMahasiswas = null;
		try {
			Session session = HibernateUtil.currentNativeSession();
			parameterTambahanMahasiswas = ConstantValues.simpleList(
					session.createCriteria(ParameterTambahanMahasiswa.class)
							.add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", true),
									gel == null ? Restrictions.sqlRestriction("false")
											: Restrictions.ilike("tahunAngkatans", ";" + gel + ";",
													MatchMode.ANYWHERE)))
							.createAlias("parameterTambahan", "parameterTambahan")
							.createAlias("kelompokParameterTambahanMahasiswa", "kelompokParameterTambahanMahasiswa")
							.add(Restrictions.eq("parameterTambahan.aktif", true))
							.add(Restrictions.eq("kelompokParameterTambahanMahasiswa.aktif", true)),
					ParameterTambahanMahasiswa.class);
			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ParameterTambahanMahasiswaListener.java:68");
		}
		HibernateUtil.closeSession();

		if (parameterTambahanMahasiswas == null) {
			return true;
		}

		for (ParameterTambahanMahasiswa row : parameterTambahanMahasiswas) {
			ParameterTambahan parameterTambahan = row.getParameterTambahan();
			KelompokParameterTambahanMahasiswa kelompokParameterTambahanMahasiswa = row
					.getKelompokParameterTambahanMahasiswa();
			if (parameterTambahan != null && kelompokParameterTambahanMahasiswa != null) {
				String jenis = kelompokParameterTambahanMahasiswa.getId() + "->" + parameterTambahan.getId();

				String val = "";
				String[] spl = biodataMahasiswa.getParameterTambahanInds().split("\n");
				for (String d : spl) {
					String[] value = d.split("<=>");
					if (value[0].trim().equalsIgnoreCase(jenis)) {
						val = value.length > 1 ? value[1].trim() : "";
					}
				}

				boolean wajib = parameterTambahan.getWajibDiisi()
						&& !parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.TIDAK_ADA)
						&& (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"));
				System.out.println(
						"parameterTambahan => " + parameterTambahan + ", val => " + val + ", wajib => " + wajib);

				if (wajib) {
					if (tampilMessage) {
						MyMessageboxConfig.show(
								"\"" + kelompokParameterTambahanMahasiswa.getNama()
										+ "\" harus Anda lengkapi !\n\nPilihan \"" + parameterTambahan.getLabelInputan()
										+ "\" harus dipilih",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, eventListener);
					} else {
						if (eventListener != null) {
							eventListener.onEvent(null);
						}
					}
					return false;
				}
				if (parameterTambahan.getLampiranWajibDiisi()) {
					if (parameterTambahan.getHarusMenyertakanLampiran()) {

						LampiranLain lam = LampiranLain.ambil(biodataMahasiswa.getId(), jenis);

						if (lam == null) {
							if (tampilMessage) {
								MyMessageboxConfig.show(
										"\"" + kelompokParameterTambahanMahasiswa.getNama()
												+ "\" harus Anda lengkapi !\n\nUntuk pilihan \""
												+ parameterTambahan.getLabelInputan() + "\", lampiran harus di-upload",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
										eventListener);
							} else {
								if (eventListener != null) {
									eventListener.onEvent(null);
								}
							}
							return false;
						}

					}
				}
			}
		}
		return true;
	}

	public void onSave(BiodataMahasiswa biodataMahasiswa) {
		biodataMahasiswa.populateParameterTambahan(parameterRows);
	}

	public boolean check() {
		Integer gel = biodataMahasiswa.getMahasiswa() == null ? null
				: biodataMahasiswa.getMahasiswa().getTahunangkatan();
		int c = ((Number) HibernateUtil.currentSession().createCriteria(ParameterTambahanMahasiswa.class)
				.add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", true),
						gel == null ? Restrictions.sqlRestriction("false")
								: Restrictions.ilike("tahunAngkatans", ";" + gel + ";", MatchMode.ANYWHERE)))
				.createAlias("parameterTambahan", "parameterTambahan")
				.createAlias("kelompokParameterTambahanMahasiswa", "kelompokParameterTambahanMahasiswa")
				.add(Restrictions.eq("parameterTambahan.aktif", true))
				.add(Restrictions.eq("kelompokParameterTambahanMahasiswa.aktif", true))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		return c != 0;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public void onEvent(Event event) throws Exception {

		for (Row row : parameterRows) {
			row.setVisible(false);
		}
		parameterRows.clear();

		Session session = HibernateUtil.currentSession();

		Integer gel = biodataMahasiswa.getMahasiswa() == null ? null
				: biodataMahasiswa.getMahasiswa().getTahunangkatan();
		List<KelompokParameterTambahanMahasiswa> kelompokParameterTambahanMahasiswas = session
				.createCriteria(ParameterTambahanMahasiswa.class)
				.add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", true),
						gel == null ? Restrictions.sqlRestriction("false")
								: Restrictions.ilike("tahunAngkatans", ";" + gel + ";", MatchMode.ANYWHERE)))
				.createAlias("parameterTambahan", "parameterTambahan")
				.createAlias("kelompokParameterTambahanMahasiswa", "kelompokParameterTambahanMahasiswa")
				.add(Restrictions.eq("parameterTambahan.aktif", true))
				.add(Restrictions.eq("kelompokParameterTambahanMahasiswa.aktif", true))
				.setProjection(Projections.groupProperty("kelompokParameterTambahanMahasiswa")).list();
		Collections.sort(kelompokParameterTambahanMahasiswas);

		EventListener isi = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				biodataMahasiswa.populateParameterTambahan(parameterRows);
			}
		};

		for (KelompokParameterTambahanMahasiswa kelompokParameterTambahanMahasiswa : kelompokParameterTambahanMahasiswas) {

			MyFormRow rowParameterTambahan = new MyFormRow();
			rowParameterTambahan.setVisible(false);
			rowParameterTambahan.setStyle("border:0px;background: transparent;");
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan.appendChild(new MyLabelBold(kelompokParameterTambahanMahasiswa.getNama() + ""));
			parameterRows.add(rowParameterTambahan);

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanMahasiswa.class)
									.add(Restrictions.eq("kelompokParameterTambahanMahasiswa",
											kelompokParameterTambahanMahasiswa))
									.add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", true),
											gel == null ? Restrictions.sqlRestriction("false")
													: Restrictions.ilike("tahunAngkatans", ";" + gel + ";",
															MatchMode.ANYWHERE)))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanMahasiswa",
											"kelompokParameterTambahanMahasiswa")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanMahasiswa.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);
			boolean tampil = false;
			rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
			if (!parameterTambahans.isEmpty()) {

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = kelompokParameterTambahanMahasiswa.getId() + "->" + parameterTambahan.getId();

					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setValign("top");row.setAttribute("parameterTambahan", parameterTambahan);
					row.setValign("top");row.setAttribute("kelompokParameterTambahanMahasiswa", kelompokParameterTambahanMahasiswa);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(
							parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
					if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
						parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
					}
					String val = "";
					String ket = "";
					String[] spl = biodataMahasiswa.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
							try {
								ket = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanMahasiswaListener.java:244");

							}
						}
					}

					tampil |= ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
							biodataMahasiswa.getId(), val, ket, parameterTambahan, isi);
				}
			}

			rowParameterTambahan.setVisible(tampil);
		}
	}
}
