package ais.database.dao;


import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;



public class TbmuserDaoImpl extends GenericHibernateDao<Tbmuser, Long, TbmuserDao> implements TbmuserDao {
    public Boolean login(Tbmuser users) {
        Criteria criteria = createCriteria();
        criteria.setProjection(Projections.rowCount());
        criteria.add(Restrictions.eq("userId", users.getUserId()));
        criteria.add(Restrictions.eq("userPassword", users.getUserPassword()));
        return ((Number) criteria.uniqueResult()).intValue() != 0;
    }

    public Tbmuser loadByUsernameAndPass(Tbmuser users) {
        Criteria criteria = createCriteria();
        criteria.add(Restrictions.eq("userId", users.getUserId()));
        criteria.add(Restrictions.eq("userPassword", users.getUserPassword()));
        return ((Tbmuser) criteria.uniqueResult());
    }

    public Boolean isExist(Tbmuser users) {
        Criteria criteria = createCriteria();
        criteria.add(Restrictions.eq("userId", users.getUserId()));
        return criteria.setMaxResults(ais.common.Common.MAX_RESULT).list().size() != 0;
    }
    
    public Tbmuser loadByUsernameAndPassWithNewSession(Tbmuser users) {
    	Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        criteria.add(Restrictions.eq("userId", users.getUserId()));
        criteria.add(Restrictions.eq("userPassword", users.getUserPassword()));
        Tbmuser users2 = ((Tbmuser) criteria.uniqueResult());
        return users2;
    }

	@Override
	public Boolean loginWithNewSession(Tbmuser users) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        criteria.setProjection(Projections.rowCount());
        criteria.add(Restrictions.eq("userId", users.getUserId()));
        criteria.add(Restrictions.eq("userPassword", users.getUserPassword()));
        Boolean result =  ((Number) criteria.uniqueResult()).intValue() != 0;
        return result;
	}


}
