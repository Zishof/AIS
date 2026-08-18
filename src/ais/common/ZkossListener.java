package ais.common;

import java.util.List;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.sys.SessionsCtrl;
import org.zkoss.zkplus.util.ThreadLocalListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.DetailLogLogin;
import ais.database.model.LogUserActifity;
import ais.ui.util.MyButtonConfig;

public class ZkossListener extends ThreadLocalListener {

	@Override
	public void abortResume(Component comp, Event evt) {
		// TODO Auto-generated method stub
		super.abortResume(comp, evt);

		// // System.out.println("abortResume => " + comp.getClass().getName() +
		// ", "
		// + evt.getName());
	}

	@Override
	public void afterResume(Component comp, Event evt) {
		// TODO Auto-generated method stub
		super.afterResume(comp, evt);
		// // System.out.println("afterResume => " + comp.getClass().getName() +
		// ", "
		// + evt.getName());
	}

	@Override
	public void beforeResume(Component comp, Event evt) {
		// TODO Auto-generated method stub
		super.beforeResume(comp, evt);
		// // System.out.println("beforeResume => " + comp.getClass().getName()
		// + ", " + evt.getName());
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void cleanup(Component comp, Event evt, List errs) {
		// TODO Auto-generated method stub
		super.cleanup(comp, evt, errs);
	}

	@Override
	public void complete(Component comp, Event evt) {
		// TODO Auto-generated method stub
		super.complete(comp, evt);
		// // System.out.println("complete => " + comp.getClass().getName() +
		// ", "
		// + evt.getName());

		boolean savingActivity = false;

		// if (System.getProperties().get("savingActivity") != null) {
		// try {
		// savingActivity = Boolean.parseBoolean((String) System
		// .getProperties().get("savingActivity"));
		// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ZkossListener.java:76");
		// Common.tampilErrorJikaAdmin(e);
		// }
		// }

		if (savingActivity) {
			String keterangan1 = "";
			String keterangan12 = "";
			String keterangan2 = "";
			String img = "";
			if (comp instanceof Button) {
				Button button = (Button) comp;
				keterangan1 = button.getTooltip() != null ? button.getTooltip().trim() : "";
				keterangan2 = button.getLabel() != null ? button.getLabel().trim() : "";
				img = button.getImage() != null ? button.getImage().trim() : "";

				try {
					DetailLogLogin logLogin = (DetailLogLogin) SessionsCtrl.getCurrent().getAttribute("detailLogLogin");
					// System.out.println("logLogin = " + logLogin);
					if (logLogin != null) {
						LogUserActifity actifity = new LogUserActifity();
						actifity.setDetailLogLogin(logLogin);
						actifity.setImg(img);
						actifity.setKeterangan(keterangan1);
						actifity.setKeterangan12(keterangan12);
						actifity.setKeterangan1(keterangan2);
						actifity.setEvent(evt.getName());

						Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.save(actifity);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ZkossListener.java:112");
				}
				ais.database.hibernate.HibernateUtil.closeSession();
			} else if (comp instanceof Bandbox) {
				Bandbox textbox = (Bandbox) comp;
				keterangan1 = "Pengguna membuka banbox";

				keterangan12 = getLabelSebelah(comp);
				keterangan2 = textbox.getValue() == null ? "" : textbox.getValue().toString();
				try {
					DetailLogLogin logLogin = (DetailLogLogin) SessionsCtrl.getCurrent().getAttribute("detailLogLogin");
					// System.out.println("logLogin = " + logLogin);
					if (logLogin != null) {
						LogUserActifity actifity = new LogUserActifity();
						actifity.setDetailLogLogin(logLogin);
						actifity.setImg(img);
						actifity.setKeterangan(keterangan1);
						actifity.setKeterangan12(keterangan12);
						actifity.setKeterangan1(keterangan2);
						actifity.setEvent(evt.getName());
						Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.save(actifity);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ZkossListener.java:140");
				}
				ais.database.hibernate.HibernateUtil.closeSession();
			} else if (comp instanceof Combobox) {
				Combobox combobox = (Combobox) comp;
				keterangan1 = "Ubah combo";

				keterangan12 = getLabelSebelah(comp);
				keterangan2 = combobox.getSelectedItem() == null ? "" : combobox.getSelectedItem().getLabel();
				try {
					DetailLogLogin logLogin = (DetailLogLogin) SessionsCtrl.getCurrent().getAttribute("detailLogLogin");
					// System.out.println("logLogin = " + logLogin);
					if (logLogin != null) {
						LogUserActifity actifity = new LogUserActifity();
						actifity.setDetailLogLogin(logLogin);
						actifity.setImg(img);
						actifity.setKeterangan(keterangan1);
						actifity.setKeterangan12(keterangan12);
						actifity.setKeterangan1(keterangan2);
						actifity.setEvent(evt.getName());
						Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.save(actifity);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ZkossListener.java:168");
				}
				ais.database.hibernate.HibernateUtil.closeSession();
			} else if (comp instanceof Textbox) {
				Textbox textbox = (Textbox) comp;
				keterangan1 = "Memasukkan nilai ke textbox";

				keterangan12 = getLabelSebelah(comp);
				keterangan2 = textbox.getValue();
				try {
					DetailLogLogin logLogin = (DetailLogLogin) SessionsCtrl.getCurrent().getAttribute("detailLogLogin");
					// System.out.println("logLogin = " + logLogin);
					if (logLogin != null) {
						LogUserActifity actifity = new LogUserActifity();
						actifity.setDetailLogLogin(logLogin);
						actifity.setImg(img);
						actifity.setKeterangan(keterangan1);
						actifity.setKeterangan12(keterangan12);
						actifity.setKeterangan1(keterangan2);
						actifity.setEvent(evt.getName());
						Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.save(actifity);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ZkossListener.java:196");
				}
				ais.database.hibernate.HibernateUtil.closeSession();
			} else if (comp instanceof Doublebox) {
				Doublebox textbox = (Doublebox) comp;
				keterangan1 = "Memasukkan nilai ke doublebox";

				keterangan12 = getLabelSebelah(comp);
				keterangan2 = textbox.getValue() == null ? "" : textbox.getValue().toString();
				try {
					DetailLogLogin logLogin = (DetailLogLogin) SessionsCtrl.getCurrent().getAttribute("detailLogLogin");
					// System.out.println("logLogin = " + logLogin);
					if (logLogin != null) {
						LogUserActifity actifity = new LogUserActifity();
						actifity.setDetailLogLogin(logLogin);
						actifity.setImg(img);
						actifity.setKeterangan(keterangan1);
						actifity.setKeterangan12(keterangan12);
						actifity.setKeterangan1(keterangan2);
						actifity.setEvent(evt.getName());
						Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.save(actifity);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ZkossListener.java:224");
				}
				ais.database.hibernate.HibernateUtil.closeSession();
			} else if (comp instanceof Intbox) {
				Intbox textbox = (Intbox) comp;
				keterangan1 = "Memasukkan nilai ke intbox";

				keterangan12 = getLabelSebelah(comp);
				keterangan2 = textbox.getValue() == null ? "" : textbox.getValue().toString();
				try {
					DetailLogLogin logLogin = (DetailLogLogin) SessionsCtrl.getCurrent().getAttribute("detailLogLogin");
					// System.out.println("logLogin = " + logLogin);
					if (logLogin != null) {
						LogUserActifity actifity = new LogUserActifity();
						actifity.setDetailLogLogin(logLogin);
						actifity.setImg(img);
						actifity.setKeterangan(keterangan1);
						actifity.setKeterangan12(keterangan12);
						actifity.setKeterangan1(keterangan2);
						actifity.setEvent(evt.getName());
						Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.save(actifity);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ZkossListener.java:252");
				}
				ais.database.hibernate.HibernateUtil.closeSession();
			} else if (comp instanceof Decimalbox) {
				Decimalbox textbox = (Decimalbox) comp;
				keterangan1 = "Memasukkan nilai ke decimalbox";

				keterangan12 = getLabelSebelah(comp);
				keterangan2 = textbox.getValue() == null ? "" : textbox.getValue().toString();
				try {
					DetailLogLogin logLogin = (DetailLogLogin) SessionsCtrl.getCurrent().getAttribute("detailLogLogin");
					// System.out.println("logLogin = " + logLogin);
					if (logLogin != null) {
						LogUserActifity actifity = new LogUserActifity();
						actifity.setDetailLogLogin(logLogin);
						actifity.setImg(img);
						actifity.setKeterangan(keterangan1);
						actifity.setKeterangan12(keterangan12);
						actifity.setKeterangan1(keterangan2);
						actifity.setEvent(evt.getName());
						Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.save(actifity);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ZkossListener.java:280");
				}
				ais.database.hibernate.HibernateUtil.closeSession();
			} else if (comp instanceof Window) {
				Window textbox = (Window) comp;
				keterangan1 = "Pengguna membuka window";
				keterangan2 = textbox.getTitle() == null ? "" : textbox.getTitle().toString();
				keterangan12 = cariLabel(textbox, "");
				if (keterangan12.length() >= 250) {
					keterangan12 = keterangan12.substring(0, 250);
				}
				try {
					DetailLogLogin logLogin = (DetailLogLogin) SessionsCtrl.getCurrent().getAttribute("detailLogLogin");
					// System.out.println("logLogin = " + logLogin);
					if (logLogin != null) {
						LogUserActifity actifity = new LogUserActifity();
						actifity.setDetailLogLogin(logLogin);
						actifity.setImg(img);
						actifity.setKeterangan(keterangan1);
						actifity.setKeterangan12(keterangan12);
						actifity.setKeterangan1(keterangan2);
						actifity.setEvent(evt.getName());
						Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.save(actifity);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
						ais.database.hibernate.HibernateUtil.closeSession();
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}

		StreamingHibernateUtil.getInstance().closeSession();
	}

	@SuppressWarnings("unchecked")
	private String cariLabel(Component component, String label) {
		List<Component> components = component.getChildren();
		for (Component c : components) {
			// // System.out.println("c = " + c.getClass());
			if (c instanceof org.zkoss.zul.Label) {
				label += label.trim().equals("") ? "[" + ((Label) c).getValue() + "]"
						: ", [" + ((Label) c).getValue() + "]";
				// // System.out.println("label = " + label);
			} else {
				label = cariLabel(c, label);
			}
		}

		return label;
	}

	private String getLabelSebelah(Component component) {
		String label = "";
		try {
			if (component.getParent() instanceof Row) {
				Row row = (Row) component.getParent();

				int i = 0;
				for (Object o : row.getChildren()) {
					if (component != o) {
						if (o instanceof Label) {
							Label myLabel = (Label) o;
							if (i == 0) {
								label += myLabel.getValue();
							} else {
								label += ", " + myLabel.getValue();
							}
						} else if (o instanceof Textbox) {
							Textbox myLabel = (Textbox) o;
							if (i == 0) {
								label += myLabel.getValue();
							} else {
								label += ", " + myLabel.getValue();
							}
						} else if (o instanceof Doublebox) {
							Doublebox myLabel = (Doublebox) o;
							if (i == 0) {
								label += myLabel.getValue();
							} else {
								label += ", " + myLabel.getValue();
							}
						} else if (o instanceof Decimalbox) {
							Decimalbox myLabel = (Decimalbox) o;
							if (i == 0) {
								label += myLabel.getValue();
							} else {
								label += ", " + myLabel.getValue();
							}
						} else if (o instanceof Intbox) {
							Intbox myLabel = (Intbox) o;
							if (i == 0) {
								label += myLabel.getValue();
							} else {
								label += ", " + myLabel.getValue();
							}
						} else if (o instanceof MyButtonConfig) {
							MyButtonConfig myLabel = (MyButtonConfig) o;
							if (i == 0) {
								label += myLabel.getImage();
							} else {
								label += ", " + myLabel.getImage();
							}
						}
						if (!label.trim().equals("")) {
							i++;
						}
					}

				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return label;
	}

	@Override
	public boolean init(Component comp, Event evt) {
		// TODO Auto-generated method stub

		// // System.out.println("init => " + comp.getClass().getName() + ", "
		// + evt.getName());
		return super.init(comp, evt);
	}

	@Override
	public void prepare(Component comp, Event evt) {
		// TODO Auto-generated method stub
		super.prepare(comp, evt);
		// // System.out.println("prepare => " + comp.getClass().getName() +
		// ", "
		// + evt.getName());
	}

}
