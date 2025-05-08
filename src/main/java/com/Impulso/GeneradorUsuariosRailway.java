package com.Impulso;

import com.Impulso.Alcambio.Modelo.Rol;
import com.Impulso.Alcambio.Modelo.Usuario;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.ArrayList;

public class GeneradorUsuariosRailway {
    // URL de conexión a Railway (reemplaza con tus credenciales)
    private static final String MONGODB_URI = "mongodb://mongo:pwlbDePolcblxDLpkfaSPfGPReYJPkxL@tramway.proxy.rlwy.net:43956/";

    private static final String[] NOMBRES = {
            "Thiago", "Maximiliano", "Benjamín", "Bruno", "Gael", "Iván", "Francisco", "Facundo",
            "Agustín", "Elías", "Matías", "Renato", "Ángel", "Aaron", "Ian", "Emilio", "Axel", "Dylan",
            "Aitana", "Renata", "Mía", "Zoe", "Jazmín", "Sara", "Milagros", "Luna", "Bianca", "Florencia",
            "Abril", "Ámbar", "Nicole", "Alexa", "Alma", "Noa", "Ariadna", "Violeta", "Julieta", "Josué", "Ezequiel",
            "Lautaro", "Nazareno", "Ulises", "Simón", "Lisandro", "Gael", "Dante", "Ismael",
            "Tamara", "Celeste", "Brenda", "Selena", "Miriam", "Noelia", "Agostina", "Kiara", "Melina", "Delfina",
            "Federico", "Giovanni", "Ricardo", "Fernando", "Belen", "Aitana", "Paola", "Diana", "Victoria", "Adela",
            "Raul", "Nora", "Olga", "Marcelo", "Esteban", "Josefina", "Laura", "Miguel", "Claudia", "Santiago",
            "Luciano", "Martina", "Sofía", "Bárbara", "César", "Franco", "Diego", "Alicia", "Héctor", "Antonio", "Alba",
            "Alexandra", "Amparo", "Ana", "Antonia", "Antonio", "Aurelio", "Aurora", "Benito", "Blanca",
            "Caridad", "Carlos", "Carmen", "Cesar", "Clara", "Cristina", "Dalia", "Daniel", "Diana", "Eduardo",
            "Esteban", "Eva", "Felicia", "Felipe", "Fernando", "Gabriela", "Gonzalo", "Guillermo", "Hector", "Hilda",
            "Ismael", "Iván", "Javier", "Jimena", "Jonás", "Josefa", "José", "Juan", "Julia", "Julieta",
            "Julio", "Karla", "Lara", "Laura", "Leticia", "Lidia", "Lourdes", "Luis", "Manuel", "Marcelino",
            "Mariana", "Marta", "Matías", "Miriam", "Montserrat", "Natalia", "Nicolás", "Noelia", "Olga", "Óscar",
            "Pablo", "Patricia", "Pedro", "Raquel", "Raul", "Rocío", "Rodrigo", "Rubén", "Salvador", "Samuel",
            "Susana", "Tania", "Teresa", "Tomás", "Verónica", "Victoria", "Virginia", "Víctor", "Yolanda", "Zoe",
            "Antonio", "Alfredo", "Ángel", "Alma", "Amparo", "Arturo", "Claudia", "Marina", "Ricardo", "Tomas",
            "Adrián", "Elena", "Luis", "Fabiola", "Ximena", "Natalia", "Lucía", "Luciano", "Patricio", "Esteban",
            "Thiago", "Maximiliano", "Benjamín", "Bruno", "Gael", "Iván", "Francisco", "Facundo",
            "Agustín", "Elías", "Matías", "Renato", "Ángel", "Aaron", "Ian", "Emilio", "Axel", "Dylan",
            "Aitana", "Renata", "Mía", "Zoe", "Jazmín", "Sara", "Milagros", "Luna", "Bianca", "Florencia",
            "Abril", "Ámbar", "Nicole", "Alexa", "Alma", "Noa", "Ariadna", "Violeta", "Julieta", "Josué", "Ezequiel",
            "Lautaro", "Nazareno", "Ulises", "Simón", "Lisandro", "Dante", "Ismael",
            "Tamara", "Celeste", "Brenda", "Selena", "Miriam", "Noelia", "Agostina", "Kiara", "Melina", "Delfina",
            "Federico", "Giovanni", "Ricardo", "Fernando", "Belen", "Paola", "Diana", "Victoria", "Adela",
            "Raul", "Nora", "Olga", "Marcelo", "Esteban", "Josefina", "Laura", "Miguel", "Claudia", "Santiago",
            "Luciano", "Martina", "Sofía", "Bárbara", "César", "Franco", "Diego", "Alicia", "Héctor", "Antonio", "Alba",
            "Alexandra", "Amparo", "Ana", "Antonia", "Aurelio", "Aurora", "Benito", "Blanca",
            "Caridad", "Carlos", "Carmen", "Cesar", "Clara", "Cristina", "Dalia", "Daniel", "Eduardo",
            "Eva", "Felicia", "Felipe", "Gabriela", "Gonzalo", "Guillermo", "Hector", "Hilda",
            "Javier", "Jimena", "Jonás", "Josefa", "José", "Juan", "Julia",
            "Julio", "Karla", "Lara", "Leticia", "Lidia", "Lourdes", "Luis", "Manuel", "Marcelino",
            "Mariana", "Marta", "Montserrat", "Natalia", "Nicolás", "Óscar",
            "Pablo", "Patricia", "Pedro", "Raquel", "Rocío", "Rodrigo", "Rubén", "Salvador", "Samuel",
            "Susana", "Tania", "Teresa", "Tomás", "Verónica", "Virginia", "Víctor", "Yolanda",
            "Alfredo", "Arturo", "Marina", "Tomas",
            "Adrián", "Elena", "Fabiola", "Ximena", "Lucía", "Patricio"

    };

    private static final String[] APELLIDOS = {
            "Bravo", "Reyes", "Rivas", "Del Valle", "Campos", "Benitez", "Arroyo", "Gallegos", "Zamora",
            "Palacios", "Valencia", "Correa", "Barrios", "Lagos", "León", "Villegas", "Palma", "Roldán",
            "Torres", "Franco", "Segura", "Montoya", "Espinosa", "Peralta", "Tejada", "Tapia", "Guerra",
            "Luna", "Rosales", "Vilches", "Castaño", "Mejía", "Solano", "Altamirano", "Lozano", "Bautista", "Alvarado",
            "Carrillo", "Escobar", "Figueroa", "Grimaldi", "Ibarra", "Juárez", "López", "Macías", "Narváez",
            "Olivares", "Padilla", "Quiroga", "Rentería", "Santana", "Trujillo", "Ulloa", "Vallejo", "Yáñez", "Zúñiga",
            "Arroyo", "Bustamante", "Caballero", "Cabrera", "Cano", "Carreño", "Castillo", "Cuesta", "Del Castillo",
            "Fajardo",
            "Gallego", "González", "Guzmán", "Herrera", "Jaimes", "Lara", "Lozano", "Márquez", "Martínez", "Mora",
            "Moreno", "Ortega", "Paredes", "Parra", "Pérez", "Pichardo", "Rivas", "Rocío", "Salazar", "Valdés",
            "Abarca", "Alonso", "Arias", "Arévalo", "Arriaza", "Barea", "Bermúdez", "Borbón", "Bravo", "Cabrera",
            "Calderón", "Canales", "Castro", "Ceballos", "Chavez", "Collado", "Contreras", "Cordero", "Delgado",
            "Dominguez",
            "Duarte", "Durán", "Escobar", "Escudero", "Figueroa", "Flores", "García", "Gómez", "Guerrero", "Hernández",
            "Hidalgo", "Izquierdo", "Jaramillo", "Jiménez", "López", "Mancilla", "Manrique", "Márquez", "Mena",
            "Mendoza",
            "Mercado", "Muñoz", "Naranjo", "Olivares", "Ortega", "Paredes", "Pérez", "Ramón", "Reyes", "Rivas",
            "Rivera", "Rodríguez", "Rojas", "Ruiz", "Sánchez", "Silva", "Solís", "Torres", "Vásquez", "Vega",
            "Vega", "Vidal", "Zapata", "Zúñiga", "Bautista", "Briones", "Benavides", "Caballero", "Cano", "Castro",
            "De la Cruz", "González", "Gallegos", "García", "Guerrero", "Herrera", "Luna", "Lozano", "Malagón", "Mora",
            "Moreno", "Mújica", "Ortega", "Palencia", "Pino", "Ramirez", "Romero", "Sánchez", "Santos", "Bravo",
            "Reyes", "Rivas", "Del Valle", "Campos", "Benitez", "Arroyo", "Gallegos", "Zamora",
            "Palacios", "Valencia", "Correa", "Barrios", "Lagos", "León", "Villegas", "Palma", "Roldán",
            "Torres", "Franco", "Segura", "Montoya", "Espinosa", "Peralta", "Tejada", "Tapia", "Guerra",
            "Luna", "Rosales", "Vilches", "Castaño", "Mejía", "Solano", "Altamirano", "Lozano", "Bautista", "Alvarado",
            "Carrillo", "Escobar", "Figueroa", "Grimaldi", "Ibarra", "Juárez", "López", "Macías", "Narváez",
            "Olivares", "Padilla", "Quiroga", "Rentería", "Santana", "Trujillo", "Ulloa", "Vallejo", "Yáñez", "Zúñiga",
            "Bustamante", "Caballero", "Cabrera", "Cano", "Carreño", "Castillo", "Cuesta", "Del Castillo",
            "Fajardo", "Gallego", "González", "Guzmán", "Herrera", "Jaimes", "Lara", "Márquez", "Martínez", "Mora",
            "Moreno", "Ortega", "Paredes", "Parra", "Pérez", "Pichardo", "Rocío", "Salazar", "Valdés",
            "Abarca", "Alonso", "Arias", "Arévalo", "Arriaza", "Barea", "Bermúdez", "Borbón",
            "Calderón", "Canales", "Castro", "Ceballos", "Chavez", "Collado", "Contreras", "Cordero", "Delgado",
            "Dominguez", "Duarte", "Durán", "Escudero", "Flores", "García", "Gómez", "Guerrero", "Hernández",
            "Hidalgo", "Izquierdo", "Jaramillo", "Jiménez", "Mancilla", "Manrique", "Mena",
            "Mendoza", "Mercado", "Muñoz", "Naranjo", "Ramón", "Rivera", "Rodríguez", "Rojas", "Ruiz", "Sánchez",
            "Silva", "Solís", "Vásquez", "Vega", "Vidal", "Zapata", "Benavides", "De la Cruz", "Malagón", "Mújica",
            "Palencia", "Pino", "Ramirez", "Romero", "Santos", "Santos", "Serrano", "Suárez", "Téllez",
            "Valenzuela", "Vásquez", "Villalobos", "Zapata", "Zúñiga", "Aguirre", "Alarcón", "Alfaro",
            "Alonso", "Alvarado", "Araya", "Arce", "Arrieta", "Astorga", "Ayala", "Baeza",
            "Bermúdez", "Bravo", "Briones", "Cabrera", "Cáceres", "Callejas", "Cano",
            "Carvajal", "Castillo", "Ceballos", "Cifuentes", "Cifuentes", "Cisneros", "Córdoba", "Correa",

    };

    private static final String[] EMPRESAS = {
            "CABOT", "ANDI", "OKIANUS", "TRULULU",
            "ECOPETROL", "ANAVA", "CORTEVA", "DOW",
            "AJOVER", "PUERTO DE CARTAGENA", "TRASO", "YARA",
            "ESENTTIA", "GERCO", "REFINERIA DE CARTAGENA", "TERMOCANDELARIA",
    };

    public static void main(String[] args) {
        generarUsuarios(10); // Genera 30 usuarios por defecto
    }

    public static void generarUsuarios(int cantidad) {
        try (MongoClient mongoClient = MongoClients.create(MONGODB_URI)) {
            MongoDatabase database = mongoClient.getDatabase("ProyectoDeAula");
            MongoCollection<Document> collection = database.getCollection("usuarios");

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            Random rand = new Random();

            for (int i = 0; i < cantidad; i++) {
                String nombre = NOMBRES[rand.nextInt(NOMBRES.length)] + " " +
                        APELLIDOS[rand.nextInt(APELLIDOS.length)];
                String correo = nombre.toLowerCase()
                        .replace(" ", ".")
                        .replace("á", "a")
                        .replace("é", "e")
                        .replace("í", "i")
                        .replace("ó", "o")
                        .replace("ú", "u")
                        .replace("ñ", "n") +
                        rand.nextInt(999) + "@gmail.com";

                String numero = "3" + (rand.nextInt(9) + 1) +
                        String.format("%08d", rand.nextInt(100000000));
                // Guardar la contraseña sin encriptar para mostrarla
                String passwordPlano = "Pass" + rand.nextInt(1000);
                String password = encoder.encode(passwordPlano);
                String empresa = EMPRESAS[rand.nextInt(EMPRESAS.length)];

                // Determinar el rol
                String rol = rand.nextFloat() > 0.8 ? "ADMIN" : "VOLUNTARIO";
                
                Document usuario = new Document()
                        .append("nombre", nombre)
                        .append("correo", correo)
                        .append("numero", numero)
                        .append("password", password)
                        .append("empresa", empresa)
                        .append("imagenPerfil", "/img/default-profile.png")
                        .append("rol", rol)
                        .append("fechaRegistro", LocalDateTime.now().toString())
                        .append("proyectosParticipadosIds", new ArrayList<>())
                        .append("cacheEstadisticas", new Document());

                collection.insertOne(usuario);
                System.out.println(String.format("✅ Usuario creado: %s | Correo: %s | Contraseña: %s | Rol: %s | Empresa: %s",
                        nombre, correo, passwordPlano, rol, empresa));
            }

            System.out.println("\n🎉 Se han generado " + cantidad + " usuarios exitosamente!");

            // Mostrar estadísticas
            long totalUsuarios = collection.countDocuments();
            long admins = collection.countDocuments(new Document("rol", "ADMIN"));
            long voluntarios = collection.countDocuments(new Document("rol", "VOLUNTARIO"));

            System.out.println("\n📊 Estadísticas:");
            System.out.println("Total usuarios: " + totalUsuarios);
            System.out.println("Administradores: " + admins);
            System.out.println("Voluntarios: " + voluntarios);

        } catch (Exception e) {
            System.err.println("❌ Error al generar usuarios: " + e.getMessage());
            e.printStackTrace();
        }
    }
}