package serlet;

import dto.Medico;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

/**
 *
 * @author Quichiz
 */
@WebServlet(name = "estudiante", urlPatterns = {"/estudiante"})
public class medico extends HttpServlet {
    // FORMATO DE FECHA PARA CONVERSIÓN
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // CONFIGURACIÓN DE PERSISTENCIA JPA
    private EntityManagerFactory emf = Persistence.createEntityManagerFactory("com.mycompany_ExamenRecuperacion_war_1.0-SNAPSHOTPU");

    // OBTENER ENTITY MANAGER
    private EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    // MÉTODO PARA HASHEAR CONTRASEÑAS CON BCRYPT
    private String hashPassword(String password) {
        try {
            // Generar salt y hashear contraseña con BCrypt
            return BCrypt.hashpw(password, BCrypt.gensalt(12));
        } catch (Exception e) {
            throw new RuntimeException("Error al hashear contraseña con BCrypt", e);
        }
    }
    
    // MÉTODO PARA VERIFICAR CONTRASEÑAS CON BCRYPT
    private boolean verifyPassword(String password, String hashedPassword) {
        try {
            return BCrypt.checkpw(password, hashedPassword);
        } catch (Exception e) {
            System.err.println("Error al verificar contraseña: " + e.getMessage());
            return false;
        }
    }

    // MÉTODO GET - LISTAR TODOS LOS MÉDICOS
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        EntityManager em = getEntityManager();
        JSONObject jsonResponse = new JSONObject();

        try {
            List<Medico> medicos = em.createQuery("SELECT m FROM Medico m ORDER BY m.codiMedi ASC", Medico.class).getResultList();
            JSONArray jsonArray = new JSONArray();

            for (Medico m : medicos) {
                JSONObject obj = new JSONObject();
                obj.put("codiMedi", m.getCodiMedi());
                obj.put("ndniMedi", m.getNdniMedi());
                obj.put("appaMedi", m.getAppaMedi());
                obj.put("apmaMedi", m.getApmaMedi());
                obj.put("nombMedi", m.getNombMedi());

                // FORMATEAR FECHA
                if (m.getFechNaciMedi() != null) {
                    obj.put("fechNaciMedi", dateFormat.format(m.getFechNaciMedi()));
                } else {
                    obj.put("fechNaciMedi", JSONObject.NULL);
                }

                // VALIDACIÓN SIMPLE PARA CAMPOS STRING
                obj.put("logiMedi", m.getLogiMedi() != null ? m.getLogiMedi() : "");
                // NO ENVIAR LA CONTRASEÑA AL FRONTEND POR SEGURIDAD
                obj.put("passMedi", ""); // SIEMPRE VACÍO EN GET

                jsonArray.put(obj);
            }

            jsonResponse.put("success", true);
            jsonResponse.put("data", jsonArray);
            jsonResponse.put("message", "Médicos cargados correctamente");

        } catch (Exception e) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Error al cargar médicos: " + e.getMessage());
        } finally {
            em.close();
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.print(jsonResponse.toString());
        out.flush();
    }

    // MÉTODO POST - CREAR NUEVO MÉDICO
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        EntityManager em = getEntityManager();
        JSONObject jsonResponse = new JSONObject();

        try {
            // LEER DATOS JSON DEL REQUEST
            BufferedReader reader = request.getReader();
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            JSONObject json = new JSONObject(jsonBuilder.toString());

            // VALIDAR CAMPOS REQUERIDOS
            String[] camposRequeridos = {"ndniMedi", "appaMedi", "apmaMedi", "nombMedi", "fechNaciMedi", "logiMedi", "passMedi"};
            for (String campo : camposRequeridos) {
                if (!json.has(campo) || json.getString(campo).trim().isEmpty()) {
                    jsonResponse.put("success", false);
                    jsonResponse.put("message", "El campo " + campo + " es requerido");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    sendResponse(response, jsonResponse);
                    return;
                }
            }

            // VALIDAR QUE EL LOGIN NO EXISTA
            List<Medico> existeLogin = em.createQuery("SELECT m FROM Medico m WHERE m.logiMedi = :login", Medico.class)
                    .setParameter("login", json.getString("logiMedi"))
                    .getResultList();

            if (!existeLogin.isEmpty()) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "El login ya existe, por favor elija otro");
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                sendResponse(response, jsonResponse);
                return;
            }

            // CREAR NUEVO MÉDICO
            Medico medico = new Medico();
            medico.setNdniMedi(json.getString("ndniMedi"));
            medico.setAppaMedi(json.getString("appaMedi").toUpperCase());
            medico.setApmaMedi(json.getString("apmaMedi").toUpperCase());
            medico.setNombMedi(json.getString("nombMedi").toUpperCase());

            // CONVERTIR FECHA DE STRING A DATE
            String fechaStr = json.getString("fechNaciMedi");
            Date fecha = dateFormat.parse(fechaStr);
            medico.setFechNaciMedi(fecha);

            medico.setLogiMedi(json.getString("logiMedi"));
            
            // HASHEAR LA CONTRASEÑA CON BCRYPT ANTES DE GUARDAR
            String passwordPlano = json.getString("passMedi");
            String passwordHasheado = hashPassword(passwordPlano);
            medico.setPassMedi(passwordHasheado);

            System.out.println("DEBUG - Creando médico:");
            System.out.println("Login: " + json.getString("logiMedi"));
            System.out.println("Password original: " + passwordPlano);
            System.out.println("Password hasheado: " + passwordHasheado);

            // PERSISTIR EN BASE DE DATOS
            em.getTransaction().begin();
            em.persist(medico);
            em.getTransaction().commit();

            jsonResponse.put("success", true);
            jsonResponse.put("message", "Médico creado correctamente");
            response.setStatus(HttpServletResponse.SC_CREATED);

        } catch (Exception e) {
            // ROLLBACK EN CASO DE ERROR
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Error al crear médico: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        } finally {
            em.close();
        }

        sendResponse(response, jsonResponse);
    }

    // MÉTODO PUT - ACTUALIZAR MÉDICO EXISTENTE
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        EntityManager em = getEntityManager();
        JSONObject jsonResponse = new JSONObject();

        try {
            // LEER DATOS JSON DEL REQUEST
            BufferedReader reader = request.getReader();
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            JSONObject json = new JSONObject(jsonBuilder.toString());
            
            if (!json.has("codiMedi")) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "ID del médico es requerido para actualizar");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                sendResponse(response, jsonResponse);
                return;
            }

            int codiMedi = json.getInt("codiMedi");

            em.getTransaction().begin();

            // BUSCAR MÉDICO POR ID
            Medico medico = em.find(Medico.class, codiMedi);

            if (medico != null) {
                // VALIDAR QUE EL LOGIN NO EXISTA EN OTRO MÉDICO
                List<Medico> existeLogin = em.createQuery("SELECT m FROM Medico m WHERE m.logiMedi = :login AND m.codiMedi != :id", Medico.class)
                        .setParameter("login", json.getString("logiMedi"))
                        .setParameter("id", codiMedi)
                        .getResultList();

                if (!existeLogin.isEmpty()) {
                    em.getTransaction().rollback();
                    jsonResponse.put("success", false);
                    jsonResponse.put("message", "El login ya existe en otro médico");
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    sendResponse(response, jsonResponse);
                    return;
                }

                // ACTUALIZAR DATOS
                medico.setNdniMedi(json.getString("ndniMedi"));
                medico.setAppaMedi(json.getString("appaMedi").toUpperCase());
                medico.setApmaMedi(json.getString("apmaMedi").toUpperCase());
                medico.setNombMedi(json.getString("nombMedi").toUpperCase());

                String fechaStr = json.getString("fechNaciMedi");
                Date fecha = dateFormat.parse(fechaStr);
                medico.setFechNaciMedi(fecha);

                medico.setLogiMedi(json.getString("logiMedi"));

                // SOLO ACTUALIZAR CONTRASEÑA SI SE PROPORCIONA UNA NUEVA
                String nuevaPassword = json.optString("passMedi", "").trim();
                if (!nuevaPassword.isEmpty()) {
                    String passwordHasheado = hashPassword(nuevaPassword);
                    medico.setPassMedi(passwordHasheado);
                    
                    System.out.println("DEBUG - Actualizando password:");
                    System.out.println("Login: " + json.getString("logiMedi"));
                    System.out.println("Nueva password: " + nuevaPassword);
                    System.out.println("Nueva password hasheada: " + passwordHasheado);
                }

                // GUARDAR CAMBIOS
                em.merge(medico);
                em.getTransaction().commit();

                jsonResponse.put("success", true);
                jsonResponse.put("message", "Médico actualizado correctamente");
            } else {
                em.getTransaction().rollback();
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Médico no encontrado");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (Exception e) {
            // ROLLBACK EN CASO DE ERROR
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Error al actualizar médico: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        } finally {
            em.close();
        }

        sendResponse(response, jsonResponse);
    }

    // MÉTODO DELETE - ELIMINAR MÉDICO
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        EntityManager em = getEntityManager();
        JSONObject jsonResponse = new JSONObject();

        try {
            // OBTENER ID DEL MÉDICO A ELIMINAR
            String codiParam = request.getParameter("codiMedi");
            if (codiParam == null || codiParam.trim().isEmpty()) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "ID del médico es requerido");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                sendResponse(response, jsonResponse);
                return;
            }

            int codiMedi = Integer.parseInt(codiParam);

            em.getTransaction().begin();

            // BUSCAR MÉDICO POR ID
            Medico medico = em.find(Medico.class, codiMedi);

            if (medico != null) {
                // ELIMINAR MÉDICO
                em.remove(medico);
                em.getTransaction().commit();

                jsonResponse.put("success", true);
                jsonResponse.put("message", "Médico eliminado correctamente");
            } else {
                em.getTransaction().rollback();
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Médico no encontrado");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (NumberFormatException e) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "ID del médico debe ser un número válido");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            // ROLLBACK EN CASO DE ERROR
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Error al eliminar médico: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            em.close();
        }

        sendResponse(response, jsonResponse);
    }

    // MÉTODO PÚBLICO PARA VERIFICAR LOGIN (USADO POR SERVLET DE LOGIN)
    public boolean verificarLogin(String login, String password) {
        EntityManager em = getEntityManager();
        try {
            List<Medico> medicos = em.createQuery("SELECT m FROM Medico m WHERE m.logiMedi = :login", Medico.class)
                    .setParameter("login", login)
                    .getResultList();
            
            if (!medicos.isEmpty()) {
                Medico medico = medicos.get(0);
                String hashedPassword = medico.getPassMedi();
                
                System.out.println("DEBUG - Verificando login:");
                System.out.println("Login: " + login);
                System.out.println("Password ingresada: " + password);
                System.out.println("Hash almacenado: " + hashedPassword);
                
                boolean resultado = verifyPassword(password, hashedPassword);
                System.out.println("Resultado verificación: " + resultado);
                
                return resultado;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error en verificarLogin: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            em.close();
        }
    }

    // MÉTODO AUXILIAR PARA ENVIAR RESPUESTA
    private void sendResponse(HttpServletResponse response, JSONObject jsonResponse) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.print(jsonResponse.toString());
        out.flush();
    }

    // MANEJAR PETICIONES OPTIONS PARA CORS
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    public String getServletInfo() {
        return "Servlet para operaciones CRUD de médicos con contraseñas BCrypt - Versión segura";
    }

    @Override
    public void destroy() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
        super.destroy();
    }
}
