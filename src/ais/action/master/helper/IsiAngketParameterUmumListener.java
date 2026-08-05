package ais.action.master.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GrupChecklistPenilaianDosen;
import ais.database.model.GrupChecklistPenilaianUmum;
import ais.database.model.IsiAngketParameterUmum;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanAngketUmum;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.GrupChecklistPenilaianGuru;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyMessageboxConfig;

/**
 * Renderer dan validator parameter tambahan angket.
 *
 * Listener lama hanya mendukung GrupChecklistPenilaianUmum. Versi ini tetap
 * kompatibel dengan constructor lama, dan menambahkan constructor untuk grup
 * angket dosen serta guru.
 */
public class IsiAngketParameterUmumListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private IsiAngketParameterUmum isiAngketParameterUmum;
	private Map<String, LampiranLain> lampiranLains;
	private GrupChecklistPenilaianUmum grupChecklistPenilaianUmum;
	private GrupChecklistPenilaianDosen grupChecklistPenilaianDosen;
	private GrupChecklistPenilaianGuru grupChecklistPenilaianGuru;

	public IsiAngketParameterUmumListener(IsiAngketParameterUmum isiAngketParameterUmum, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this(isiAngketParameterUmum, parameterRows, lampiranLains, rows, null, null, null);
	}

	public IsiAngketParameterUmumListener(IsiAngketParameterUmum isiAngketParameterUmum, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows, GrupChecklistPenilaianDosen grupChecklistPenilaianDosen) {
		this(isiAngketParameterUmum, parameterRows, lampiranLains, rows, null, grupChecklistPenilaianDosen, null);
	}

	public IsiAngketParameterUmumListener(IsiAngketParameterUmum isiAngketParameterUmum, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows, GrupChecklistPenilaianGuru grupChecklistPenilaianGuru) {
		this(isiAngketParameterUmum, parameterRows, lampiranLains, rows, null, null, grupChecklistPenilaianGuru);
	}

	private IsiAngketParameterUmumListener(IsiAngketParameterUmum isiAngketParameterUmum, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows, GrupChecklistPenilaianUmum grupChecklistPenilaianUmum,
			GrupChecklistPenilaianDosen grupChecklistPenilaianDosen,
			GrupChecklistPenilaianGuru grupChecklistPenilaianGuru) {
		this.parameterRows = parameterRows;
		this.rows = rows;
		this.isiAngketParameterUmum = isiAngketParameterUmum;
		this.lampiranLains = lampiranLains;
		this.grupChecklistPenilaianUmum = grupChecklistPenilaianUmum;
		this.grupChecklistPenilaianDosen = grupChecklistPenilaianDosen;
		this.grupChecklistPenilaianGuru = grupChecklistPenilaianGuru;
	}

	public boolean validate() throws Exception {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return true;
		}
		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			if (parameterTambahan == null) {
				continue;
			}
			String jenis = buildJenis(row, parameterTambahan);
			if (jenis == null || jenis.trim().isEmpty()) {
				continue;
			}

			String val = ParameterTambahan.ambilVal(row, parameterTambahan);

			if (parameterTambahan.getWajibDiisi()
					&& (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"))) {
				MyMessageboxConfig.show("Pilihan \"" + parameterTambahan.getLabelInputan() + "\" harus dipilih",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			if (parameterTambahan.getLampiranWajibDiisi()) {
				if (parameterTambahan.getHarusMenyertakanLampiran()
						&& (lampiranLains == null || !lampiranLains.keySet().contains(jenis))) {
					MyMessageboxConfig.show(
							"Untuk pilihan \"" + parameterTambahan.getLabelInputan() + "\", lampiran harus di-upload",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}
		return true;
	}

	public void onSave(IsiAngketParameterUmum isiAngketParameterUmum) {
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			IsiAngketParameterUmum managed = isiAngketParameterUmum;
			if (managed != null && managed.getId() != null) {
				managed = (IsiAngketParameterUmum) session.get(IsiAngketParameterUmum.class, managed.getId());
			}
			if (managed == null) {
				managed = isiAngketParameterUmum;
			}
			managed.populateParameterTambahan(parameterRows);
			pastikanTidakSetTbmuserUntukPeserta(managed);
			session.saveOrUpdate(managed);
			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/IsiAngketParameterUmumListener.java:130");
				}
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/IsiAngketParameterUmumListener.java:138");
				}
			}
		}
	}

	private void pastikanTidakSetTbmuserUntukPeserta(IsiAngketParameterUmum isi) {
		if (isi == null) {
			return;
		}
		try {
			if (isi.getMahasiswa() != null || isi.getSiswa() != null || isi.getDosen() != null || isi.getGuru() != null) {
				isi.setTbmuser(null);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/IsiAngketParameterUmumListener.java:152");
		}
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public void onEvent(Event event) throws Exception {
		clearParameterRows();

		Session session = HibernateUtil.currentSession();
		Object grup = resolveGrupTarget();
		if (grup == null || rows == null) {
			return;
		}

		MyFormRow rowParameterTambahan = new MyFormRow();
		rowParameterTambahan.setVisible(false);
		rowParameterTambahan.setStyle("border:0px;background: transparent;");
		rowParameterTambahan.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
		rowParameterTambahan.appendChild(new MyLabelBold(getGrupLabel(grup)));
		parameterRows.add(rowParameterTambahan);

		List<ParameterTambahan> parameterTambahans;
		if (grup instanceof GrupChecklistPenilaianDosen) {
			parameterTambahans = session.createCriteria(ParameterTambahanAngketUmum.class)
					.add(Restrictions.eq("grupChecklistPenilaianDosen", grup))
					.setProjection(Projections.groupProperty("parameterTambahan")).list();
		} else if (grup instanceof GrupChecklistPenilaianGuru) {
			parameterTambahans = session.createCriteria(ParameterTambahanAngketUmum.class)
					.add(Restrictions.eq("grupChecklistPenilaianGuru", grup))
					.setProjection(Projections.groupProperty("parameterTambahan")).list();
		} else {
			parameterTambahans = session.createCriteria(ParameterTambahanAngketUmum.class)
					.add(Restrictions.eq("grupChecklistPenilaianUmum", grup))
					.setProjection(Projections.groupProperty("parameterTambahan")).list();
		}

		if (parameterTambahans == null) {
			return;
		}
		Collections.sort(parameterTambahans);

		EventListener isi = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				isiAngketParameterUmum.populateParameterTambahan(parameterRows);
			}
		};

		boolean tampil = false;
		rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
		for (ParameterTambahan parameterTambahan : parameterTambahans) {
			String jenis = buildJenis(grup, parameterTambahan);
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setAttribute("parameterTambahan", parameterTambahan);
			row.setAttribute("grupChecklistPenilaianUmum", grup instanceof GrupChecklistPenilaianUmum ? grup : null);
			row.setAttribute("grupChecklistPenilaianDosen", grup instanceof GrupChecklistPenilaianDosen ? grup : null);
			row.setAttribute("grupChecklistPenilaianGuru", grup instanceof GrupChecklistPenilaianGuru ? grup : null);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(
					parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
			if (parameterTambahan.getKeterangan() != null && !parameterTambahan.getKeterangan().trim().isEmpty()) {
				parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
			}

			String val = "";
			String ket = "";
			String[] spl = isiAngketParameterUmum.getParameterTambahanInds().split("\\n");
			for (String d : spl) {
				String[] value = d.split("<=>");
				if (value.length > 0 && value[0].trim().equalsIgnoreCase(jenis)) {
					val = value.length > 1 ? value[1].trim() : "";
					try {
						ket = value.length > 0 ? value[value.length - 1] : "";
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/IsiAngketParameterUmumListener.java:228");
					}
				}
			}

			tampil |= ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
					isiAngketParameterUmum.getId(), val, ket, parameterTambahan, isi);
		}
		rowParameterTambahan.setVisible(tampil);
	}

	private void clearParameterRows() {
		if (parameterRows == null) {
			return;
		}
		for (Row row : parameterRows) {
			try {
				row.setVisible(false);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/IsiAngketParameterUmumListener.java:246");
			}
		}
		parameterRows.clear();
	}

	private Object resolveGrupTarget() {
		if (grupChecklistPenilaianDosen != null) {
			return grupChecklistPenilaianDosen;
		}
		if (grupChecklistPenilaianGuru != null) {
			return grupChecklistPenilaianGuru;
		}
		if (grupChecklistPenilaianUmum != null) {
			return grupChecklistPenilaianUmum;
		}
		try {
			if (isiAngketParameterUmum != null && isiAngketParameterUmum.getJadwalChecklistPenilaianUmum() != null) {
				return isiAngketParameterUmum.getJadwalChecklistPenilaianUmum().getGrupChecklistPenilaianUmum();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/IsiAngketParameterUmumListener.java:266");
		}
		return null;
	}

	private String getGrupLabel(Object grup) {
		if (grup instanceof GrupChecklistPenilaianDosen) {
			return "Parameter Tambahan Angket Dosen - " + ((GrupChecklistPenilaianDosen) grup).getIsi();
		}
		if (grup instanceof GrupChecklistPenilaianGuru) {
			return "Parameter Tambahan Angket Guru - " + ((GrupChecklistPenilaianGuru) grup).getIsi();
		}
		if (grup instanceof GrupChecklistPenilaianUmum) {
			return "Parameter Tambahan Angket Umum - " + ((GrupChecklistPenilaianUmum) grup).getIsi();
		}
		return "Parameter Tambahan Angket";
	}

	private String buildJenis(Row row, ParameterTambahan parameterTambahan) {
		Object grup = row.getAttribute("grupChecklistPenilaianDosen");
		if (grup instanceof GrupChecklistPenilaianDosen) {
			return buildJenis(grup, parameterTambahan);
		}
		grup = row.getAttribute("grupChecklistPenilaianGuru");
		if (grup instanceof GrupChecklistPenilaianGuru) {
			return buildJenis(grup, parameterTambahan);
		}
		grup = row.getAttribute("grupChecklistPenilaianUmum");
		if (grup instanceof GrupChecklistPenilaianUmum) {
			return buildJenis(grup, parameterTambahan);
		}
		return null;
	}

	private String buildJenis(Object grup, ParameterTambahan parameterTambahan) {
		if (grup == null || parameterTambahan == null || parameterTambahan.getId() == null) {
			return "";
		}
		if (grup instanceof GrupChecklistPenilaianDosen) {
			return "DOSEN:" + ((GrupChecklistPenilaianDosen) grup).getId() + "->" + parameterTambahan.getId();
		}
		if (grup instanceof GrupChecklistPenilaianGuru) {
			return "GURU:" + ((GrupChecklistPenilaianGuru) grup).getId() + "->" + parameterTambahan.getId();
		}
		if (grup instanceof GrupChecklistPenilaianUmum) {
			return ((GrupChecklistPenilaianUmum) grup).getId() + "->" + parameterTambahan.getId();
		}
		return "";
	}
}
