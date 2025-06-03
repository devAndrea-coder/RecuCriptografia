package dao;

import dao.exceptions.NonexistentEntityException;
import dto.Medico;
import java.io.Serializable;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 *
 * @author ANDREA
 */
public class MedicoJpaController implements Serializable {

    public MedicoJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Medico medico) {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            em.persist(medico);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Medico medico) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            medico = em.merge(medico);
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = medico.getCodiMedi();
                if (findMedico(id) == null) {
                    throw new NonexistentEntityException("The medico with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Integer id) throws NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Medico medico;
            try {
                medico = em.getReference(Medico.class, id);
                medico.getCodiMedi();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The medico with id " + id + " no longer exists.", enfe);
            }
            em.remove(medico);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Medico> findMedicoEntities() {
        return findMedicoEntities(true, -1, -1);
    }

    public List<Medico> findMedicoEntities(int maxResults, int firstResult) {
        return findMedicoEntities(false, maxResults, firstResult);
    }

    private List<Medico> findMedicoEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Medico.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public Medico findMedico(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Medico.class, id);
        } finally {
            em.close();
        }
    }

    public int getMedicoCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Medico> rt = cq.from(Medico.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
    // METODO VALIDAR POR DNI
    public Medico validarDni(String dni, String pass) {
        EntityManager em = getEntityManager();
        try {
            Query q = em.createNamedQuery("Medico.validar");
            q.setParameter("ndniMedi", dni);
            q.setParameter("passMed", pass);

            List<Medico> lista = q.getResultList();

            if (lista == null || lista.isEmpty()) {
                return null;
            } else {
                return lista.get(0);
            }
        } finally {
            em.close();
        }
    }
    
    //BUSQUEDA POR DNI
    public Medico buscarPorDniSolo(String dni) {
        EntityManager em = getEntityManager();
        try {
            Query query = em.createQuery("Medico.findByNdniMedi");
            query.setParameter("dni", dni);

            List<Medico> resultados = query.getResultList();

            if (resultados.isEmpty()) {
                return null;
            }

            return resultados.get(0);

        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            System.err.println("Error al buscar Medico por DNI: " + e.getMessage());
            return null;
        } finally {
            em.close();
        }
    }
    
    //REGISTRAR NUEVO ESTUDIANTE
    public boolean registrarEstudianteConHash(Medico estudiante, String plainPassword) {
        try {
            // Hashear la contraseña antes de guardar
            String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(plainPassword, org.mindrot.jbcrypt.BCrypt.gensalt(12));
            estudiante.setPassMedi(hashedPassword);

            // Guardar en la base de datos
            create(estudiante);
            return true;

        } catch (Exception e) {
            System.err.println("Error al registrar estudiante: " + e.getMessage());
            return false;
        }
    }
}
