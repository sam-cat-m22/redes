package practica_2;
import java.net.*;
import java.io.*;

/**
 *
 * @author cesar
 */
public class ClienteDrive {

    private static String rutaActual = "/"; // Comenzamos en la carpeta raíz

    public static void main(String[] args) {
        try {
            // Paso 1: Crear el socket para el cliente
            int puertoServidor = 1234; // Puerto del servidor
            String direccionServidor = "127.0.0.1"; // Dirección del servidor (puede cambiarse)
            DatagramSocket socketCliente = new DatagramSocket(); // Creamos el socket del cliente
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                listarDirectorio(socketCliente, direccionServidor, puertoServidor);

                System.out.println("Elija un recurso ingresando el número correspondiente:");
                int opcionSeleccionada = Integer.parseInt(reader.readLine().trim());
                manejarSeleccion(socketCliente, direccionServidor, puertoServidor, opcionSeleccionada, reader);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void listarDirectorio(DatagramSocket socketCliente, String direccionServidor, int puertoServidor) throws IOException {
        String comando = "LIST_DIRECTORY:" + rutaActual; // Comando para solicitar la lista de la carpeta actual
        byte[] bufferComando = comando.getBytes(); // Convertimos la cadena a bytes

        InetAddress direccionIP = InetAddress.getByName(direccionServidor);
        DatagramPacket paquete = new DatagramPacket(bufferComando, bufferComando.length, direccionIP, puertoServidor);
        socketCliente.send(paquete); // Enviar el comando al servidor
        System.out.println("Comando 'LIST_DIRECTORY' enviado al servidor...");

        byte[] bufferRespuesta = new byte[1024]; // Buffer para recibir la respuesta del servidor
        DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length);
        socketCliente.receive(paqueteRespuesta); // Esperar la respuesta del servidor

        String respuesta = new String(paqueteRespuesta.getData(), 0, paqueteRespuesta.getLength());
        System.out.println("\nContenido del Drive (" + rutaActual + "):");
        System.out.println(respuesta);

        String[] recursos = respuesta.split("\n");
        for (int i = 0; i < recursos.length; i++) {
            System.out.println((i + 1) + ") " + recursos[i]);
        }

        int indiceAdicional = recursos.length + 1;
        System.out.println(indiceAdicional + ") Crear una nueva carpeta");
        System.out.println((indiceAdicional + 1) + ") Subir un archivo");
    }

    private static void manejarSeleccion(DatagramSocket socketCliente, String direccionServidor, int puertoServidor, int opcionSeleccionada, BufferedReader reader) throws IOException {
        InetAddress direccionIP = InetAddress.getByName(direccionServidor);
        String[] recursos = listarRecursos(socketCliente, direccionServidor, puertoServidor);
        int indiceAdicional = recursos.length + 1;

        if (opcionSeleccionada >= 1 && opcionSeleccionada <= recursos.length) {
            String recursoSeleccionado = recursos[opcionSeleccionada - 1];
            System.out.println("Seleccionó: " + recursoSeleccionado);

            if (recursoSeleccionado.startsWith("[C]")) {
                manejarCarpeta(socketCliente, direccionIP, puertoServidor, reader, recursoSeleccionado);
            } else if (recursoSeleccionado.startsWith("[A]")) {
                manejarArchivo(reader);
            } else if (recursoSeleccionado.startsWith("[..]")) {
                rutaActual = navegarAtras();
            }
        } else if (opcionSeleccionada == indiceAdicional) {
            crearCarpeta(socketCliente, direccionIP, puertoServidor, reader);
        } else if (opcionSeleccionada == (indiceAdicional + 1)) {
            subirArchivo(reader);
        } else {
            System.out.println("Opción no válida, intente nuevamente.");
        }
    }

    private static String[] listarRecursos(DatagramSocket socketCliente, String direccionServidor, int puertoServidor) throws IOException {
        String comando = "LIST_DIRECTORY:" + rutaActual;
        byte[] bufferComando = comando.getBytes();

        InetAddress direccionIP = InetAddress.getByName(direccionServidor);
        DatagramPacket paquete = new DatagramPacket(bufferComando, bufferComando.length, direccionIP, puertoServidor);
        socketCliente.send(paquete);

        byte[] bufferRespuesta = new byte[1024];
        DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length);
        socketCliente.receive(paqueteRespuesta);

        String respuesta = new String(paqueteRespuesta.getData(), 0, paqueteRespuesta.getLength());
        return respuesta.split("\n");
    }

    private static void manejarCarpeta(DatagramSocket socketCliente, InetAddress direccionIP, int puertoServidor, BufferedReader reader, String recursoSeleccionado) throws IOException {
        System.out.println("¿Qué desea hacer con la carpeta?");
        System.out.println("1) Abrir carpeta");
        System.out.println("2) Crear nueva subcarpeta");
        System.out.println("3) Subir archivo");
        System.out.println("4) Regresar al nivel anterior");
        System.out.println("5) Cancelar");

        int accionSeleccionada = Integer.parseInt(reader.readLine().trim());
        if (accionSeleccionada == 1) {
            String nombreCarpeta = recursoSeleccionado.substring(3).trim();
            rutaActual += nombreCarpeta + "/";
        } else if (accionSeleccionada == 2) {
            crearCarpeta(socketCliente, direccionIP, puertoServidor, reader);
        } else if (accionSeleccionada == 3) {
            subirArchivo(reader);
        } else if (accionSeleccionada == 4) {
            rutaActual = navegarAtras();
        } else {
            System.out.println("Acción cancelada.");
        }
    }

    private static void manejarArchivo(BufferedReader reader) throws IOException {
        System.out.println("¿Qué desea hacer con el archivo?");
        System.out.println("1) Cambiar nombre");
        System.out.println("2) Descargar");
        System.out.println("3) Eliminar");
        System.out.println("4) Cancelar");

        int accionSeleccionada = Integer.parseInt(reader.readLine().trim());
        System.out.println("Acción seleccionada: " + accionSeleccionada);
    }

    private static void crearCarpeta(DatagramSocket socketCliente, InetAddress direccionIP, int puertoServidor, BufferedReader reader) throws IOException {
        System.out.println("Ingrese el nombre de la nueva carpeta:");
        String nombreCarpeta = reader.readLine().trim();

        String comandoCrearCarpeta = "CREATE_FOLDER:" + rutaActual + nombreCarpeta;
        byte[] bufferComandoCrearCarpeta = comandoCrearCarpeta.getBytes();
        DatagramPacket paqueteCrearCarpeta = new DatagramPacket(bufferComandoCrearCarpeta, bufferComandoCrearCarpeta.length, direccionIP, puertoServidor);
        socketCliente.send(paqueteCrearCarpeta);
        System.out.println("Comando 'CREATE_FOLDER' enviado al servidor...");

        byte[] bufferRespuestaCrearCarpeta = new byte[1024];
        DatagramPacket paqueteRespuestaCrearCarpeta = new DatagramPacket(bufferRespuestaCrearCarpeta, bufferRespuestaCrearCarpeta.length);
        socketCliente.receive(paqueteRespuestaCrearCarpeta);

        String respuestaCrearCarpeta = new String(paqueteRespuestaCrearCarpeta.getData(), 0, paqueteRespuestaCrearCarpeta.getLength());
        System.out.println("Respuesta del servidor: " + respuestaCrearCarpeta);
    }

    private static void subirArchivo(BufferedReader reader) throws IOException {
        System.out.println("Ingrese la ruta del archivo que desea subir:");
        String rutaArchivo = reader.readLine().trim();
        System.out.println("Subiendo archivo: " + rutaArchivo);
        // Implementación para subir el archivo al servidor
    }

    private static String navegarAtras() {
        if (!rutaActual.equals("/")) {
            int ultimaBarra = rutaActual.lastIndexOf("/", rutaActual.length() - 2);
            return rutaActual.substring(0, ultimaBarra + 1);
        } else {
            System.out.println("Ya estás en la carpeta raíz, no puedes retroceder más.");
            return rutaActual;
        }
    }
}
