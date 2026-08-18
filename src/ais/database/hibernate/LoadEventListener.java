package ais.database.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.event.LoadEvent;
import org.hibernate.event.def.DefaultLoadEventListener;

public class LoadEventListener extends DefaultLoadEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7659412376297074556L;

	@Override
	public void onLoad(LoadEvent arg0, LoadType arg1) throws HibernateException {

//		Serializable entityId = arg0.getEntityId();
//		String entityClassName = arg0.getEntityClassName();
//		String type = arg1.getName();
//		boolean ada = false;
//		if (type != null && type.equalsIgnoreCase("INTERNAL_LOAD_LAZY")) {
//			GeneralValueObject generalValueObject = null;
//			try {
//				generalValueObject = (GeneralValueObject) Class.forName(entityClassName).newInstance();
//
//				if (entityId instanceof Long) {
//					generalValueObject.setId((Long) entityId);
//				} else if (entityClassName.equals(Tbmuser.class.getName())) {
//					Tbmuser tbmuser = (Tbmuser) generalValueObject;
//					tbmuser.setUserId((String) entityId);
//					generalValueObject = tbmuser;
//				} else if (entityClassName.equals(Tbmrole.class.getName())) {
//					Tbmrole tbmrole = (Tbmrole) generalValueObject;
//					tbmrole.setRoleId((String) entityId);
//					generalValueObject = tbmrole;
//				}
//				ada = ConstantValues.classExist(entityClassName);
//				if (ada) {
//					generalValueObject = GeneralValueObject.check(generalValueObject);
//					if (generalValueObject != null) {
//						arg0.setResult(generalValueObject);
//					}
//
//				} else if (entityId instanceof Long && (generalValueObject instanceof PunyaFileLocation)) {
////					PunyaFileLocation punyaFileLocation = (PunyaFileLocation) generalValueObject;
////					System.out.println("loding dari -> " + punyaFileLocation.getFileLocation()
////							+ ", punyaFileLocation.id -> " + punyaFileLocation.getId());
//				}
//			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/LoadEventListener.java:49");
//				e.printStackTrace();
//			}
//
////			System.out.println("entityId -> " + entityId + ", entityClassName ->" + entityClassName + ", type -> "
////					+ type + ", generalValueObject -> " + generalValueObject + ", ada => " + ada);
//		}

		super.onLoad(arg0, arg1);
	}

}
