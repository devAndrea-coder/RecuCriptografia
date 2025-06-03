package serlet;

import dao.MedicoJpaController;
import dto.Medico;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

/**
 *
 * @author ANDREA
 */
@WebServlet(name = "login", urlPatterns = {"/login"})
public class login extends HttpServlet {
     private MedicoJpaController loginService;
    private EntityManagerFactory emf;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            emf = Persistence.createEntityManagerFactory("com.mycompany_ExamenRecuperacion_war_1.0-SNAPSHOTPU");
            loginService = new MedicoJpaController(emf);
            System.out.println("INFO - LoginServlet inicializado correctamente");
        } catch (Exception e) {
            System.err.println("ERROR - Error al inicializar LoginServlet: " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Error al inicializar servlet de login", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Configurar respuesta
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // CORS Headers
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");

        PrintWriter out = response.getWriter();

        try {
            // Leer el cuerpo de la petición
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            String requestBody = sb.toString();
            System.out.println("DEBUG - Request body recibido: " + requestBody);

            // Validar que el body no esté vacío
            if (requestBody == null || requestBody.trim().isEmpty()) {
                enviarError(response, out, "Cuerpo de la petición vacío");
                return;
            }

            // Parsear JSON
            JSONObject body;
            try {
                body = new JSONObject(requestBody);
            } catch (Exception e) {
                System.err.println("ERROR - Error al parsear JSON: " + e.getMessage());
                enviarError(response, out, "Formato de datos inválido");
                return;
            }

            // Validar campos requeridos
            if (!body.has("dni") || !body.has("clave")) {
                enviarError(response, out, "DNI y clave son requeridos");
                return;
            }

            String dni = body.getString("dni").trim();
            String claveIngresada = body.getString("clave").trim();
            
            System.out.println("DEBUG - DNI recibido: " + dni);
            System.out.println("DEBUG - Clave recibida (longitud): " + claveIngresada.length());

            // Validar campos no vacíos
            if (dni.isEmpty() || claveIngresada.isEmpty()) {
                enviarError(response, out, "DNI y clave no pueden estar vacíos");
                return;
            }

            // Validar formato de DNI (8 dígitos)
            if (!dni.matches("\\d{8}")) {
                enviarError(response, out, "DNI debe tener exactamente 8 dígitos");
                return;
            }

            // CORRECCIÓN: Buscar estudiante por ndniEstdWeb
            Medico estudiante = buscarEstudiantePorNdniWeb(dni);
            System.out.println("DEBUG - Estudiante encontrado: " + (estudiante != null ? "Sí" : "No"));

            JSONObject json = new JSONObject();
            
            if (estudiante != null) {
                // IMPORTANTE: Verificar que el método getPassEstd() existe en tu entidad
                String hashAlmacenado = estudiante.getPassMedi();
                System.out.println("DEBUG - Hash almacenado existe: " + (hashAlmacenado != null && !hashAlmacenado.isEmpty()));
                System.out.println("DEBUG - Longitud del hash: " + (hashAlmacenado != null ? hashAlmacenado.length() : 0));
                
                if (hashAlmacenado == null || hashAlmacenado.trim().isEmpty()) {
                    System.err.println("ERROR - Hash almacenado está vacío para DNI: " + dni);
                    enviarError(response, out, "Error de configuración de usuario - password no configurado");
                    return;
                }
                
                // Verificar contraseña
                boolean passwordMatch = verificarPassword(claveIngresada, hashAlmacenado);
                System.out.println("DEBUG - Password match result: " + passwordMatch);
                
                if (passwordMatch) {
                    // Login exitoso - CREAR SESIÓN COMPLETA
                    request.getSession().setAttribute("estudiante", estudiante);
                    request.getSession().setAttribute("isLoggedIn", true);
                    request.getSession().setAttribute("loginMethod", "normal");
                    request.getSession().setAttribute("dni", dni);
                    request.getSession().setAttribute("usuario", estudiante.getLogiMedi());
                    
                    json.put("status", "ok");
                    json.put("redirect", "tabla.html");
                    json.put("usuario", estudiante.getLogiMedi());
                    json.put("dni", dni);
                    json.put("loginMethod", "normal");
                    
                    System.out.println("INFO - Login exitoso para DNI: " + dni + " Usuario: " + estudiante.getLogiMedi());
                } else {
                    // Contraseña incorrecta
                    json.put("status", "fail");
                    json.put("message", "Usuario o contraseña incorrectos");
                    
                    System.out.println("WARN - Contraseña incorrecta para DNI: " + dni);
                }
                
            } else {
                // Usuario no encontrado
                json.put("status", "fail");
                json.put("message", "Usuario o contraseña incorrectos");
                
                System.out.println("WARN - DNI no encontrado: " + dni);
            }

            out.print(json.toString());
            out.flush();

        } catch (Exception e) {
            System.err.println("ERROR - Error en login servlet: " + e.getMessage());
            e.printStackTrace();
            enviarError(response, out, "Error interno del servidor");
        }
    }
    
    /**
     * FUNCIÓN MEJORADA: Verifica contraseña con múltiples opciones
     */
    private boolean verificarPassword(String claveIngresada, String hashAlmacenado) {
        try {
            System.out.println("DEBUG - Verificando contraseña");
            System.out.println("DEBUG - Clave ingresada: '" + claveIngresada + "'");
            System.out.println("DEBUG - Hash almacenado completo: " + hashAlmacenado);
            System.out.println("DEBUG - Longitud hash: " + hashAlmacenado.length());
            
            // OPCIÓN 1: Verificar si es un hash BCrypt válido
            if (esBCryptHash(hashAlmacenado)) {
                System.out.println("DEBUG - Es hash BCrypt, verificando...");
                boolean resultado = BCrypt.checkpw(claveIngresada, hashAlmacenado);
                System.out.println("DEBUG - Resultado BCrypt: " + resultado);
                return resultado;
            }
            
            // OPCIÓN 2: Si no es BCrypt, verificar si es texto plano (TEMPORAL - SOLO PARA DEBUG)
            System.out.println("DEBUG - No es hash BCrypt, comparando texto plano...");
            boolean textoPlano = claveIngresada.equals(hashAlmacenado);
            System.out.println("DEBUG - Comparación texto plano: " + textoPlano);
            
            if (textoPlano) {
                System.out.println("WARN - ¡CONTRASEÑA EN TEXTO PLANO DETECTADA! Actualizar a BCrypt urgentemente");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("ERROR - Error al verificar contraseña: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Verifica si el hash es de BCrypt
     */
    private boolean esBCryptHash(String hash) {
        return hash != null && hash.length() >= 59 && 
               (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }
    
    /**
     * CORRECCIÓN CRÍTICA: Busca estudiante por ndniEstdWeb usando el método correcto
     */
    private Medico buscarEstudiantePorNdniWeb(String ndniWeb) {
        try {
            System.out.println("DEBUG - Buscando estudiante con ndniEstdWeb: " + ndniWeb);
            
            // OPCIONES DE BÚSQUEDA (usar la que funcione en tu DAO):
            
            // OPCIÓN 1: Si tienes un método específico para ndniEstdWeb
            Medico estudiante = null;
            try {
                // Reemplaza esto con tu método real del DAO
                estudiante = loginService.buscarPorDniSolo(ndniWeb);
            } catch (Exception e) {
                System.out.println("DEBUG - Método buscarPorNdniEstdWeb no existe, intentando alternativa...");
            }
            
            // OPCIÓN 2: Si no tienes el método específico, usar el genérico
            if (estudiante == null) {
                try {
                    estudiante = loginService.buscarPorDniSolo(ndniWeb);
                } catch (Exception e) {
                    System.out.println("DEBUG - Método buscarPorDniSolo tampoco funciona");
                }
            }
            
            // OPCIÓN 3: Si tienes el método findAll, buscar manualmente
            if (estudiante == null) {
                try {
                    List<Medico>  todos = loginService.findMedicoEntities();
                    for (Medico est : todos) {
                        if (est.getNdniMedi() != null && est.getNdniMedi().equals(ndniWeb)) {
                            estudiante = est;
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("ERROR - Error al buscar manualmente: " + e.getMessage());
                }
            }
            
            if (estudiante != null) {
                System.out.println("DEBUG - Estudiante encontrado:");
                System.out.println("  - Código: " + estudiante.getCodiMedi());
                System.out.println("  - DNI Web: " + estudiante.getNdniMedi());
                System.out.println("  - Login: " + estudiante.getLogiMedi());
                System.out.println("  - Nombre: " + estudiante.getAppaMedi() + ", " + estudiante.getApmaMedi());
                System.out.println("  - Tiene password: " + (estudiante.getPassMedi() != null ? "Sí" : "No"));
            } else {
                System.out.println("DEBUG - No se encontró estudiante con ndniEstdWeb: " + ndniWeb);
            }
            
            return estudiante;
        } catch (Exception e) {
            System.err.println("ERROR - Error al buscar estudiante: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Envía una respuesta de error en formato JSON
     */
    private void enviarError(HttpServletResponse response, PrintWriter out, String mensaje) {
        try {
            JSONObject errorJson = new JSONObject();
            errorJson.put("status", "fail");
            errorJson.put("message", mensaje);
            out.print(errorJson.toString());
            out.flush();
        } catch (Exception e) {
            System.err.println("ERROR - Error al enviar respuesta de error: " + e.getMessage());
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setStatus(HttpServletResponse.SC_OK);
    }
    
    @Override
    public void destroy() {
        super.destroy();
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
    
    // MÉTODOS AUXILIARES PARA MANEJO DE PASSWORDS
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }
    
    public static boolean testPassword(String plainPassword, String hash) {
        try {
            return BCrypt.checkpw(plainPassword, hash);
        } catch (Exception e) {
            System.err.println("Error testing password: " + e.getMessage());
            return false;
        }
    }
}
