package practica_2;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author cesar
 * @author Sam
 */
public class ServidorDrive {
    private static final int PUERTO = 1234;
    private static final String DIRECTORIO_RAIZ = "C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Practica_2\\DRIVE";
    private static final int TAMANIO_VENTANA = 4;
    private static Map<Integer, byte[]> bufferRecepcion = new HashMap<>(); // Almacenar los paquetes recibidos

    public static void main(String[] args) {
        try {
            DatagramSocket socketServidor = new DatagramSocket(PUERTO);
            System.out.println("Servidor iniciado en el puerto " + PUERTO);

            while (true) {
                byte[] bufferRecibir = new byte[1024];
                DatagramPacket paqueteRecibido = new DatagramPacket(bufferRecibir, bufferRecibir.length);
                socketServidor.receive(paqueteRecibido);

                String comando = new String(paqueteRecibido.getData(), 0, paqueteRecibido.getLength());
                System.out.println("Comando recibido: " + comando);

                if (comando.startsWith("LIST_DIRECTORY")) {
                    listarContenido(comando, paqueteRecibido, socketServidor, DIRECTORIO_RAIZ);
                } else if (comando.startsWith("CREATE_FOLDER")) {
                    crearCarpeta(comando, paqueteRecibido, socketServidor, DIRECTORIO_RAIZ);
                } else if (comando.startsWith("UPLOAD_FILE")) {
                    manejarSubidaArchivo(socketServidor, paqueteRecibido, comando);
                } else if (comando.startsWith("NAVIGATE")) {
                    navegarCarpeta(comando, paqueteRecibido, socketServidor, DIRECTORIO_RAIZ);
                } else if (comando.startsWith("DELETE_FILE")) {
                    eliminarArchivo(comando, paqueteRecibido, socketServidor, DIRECTORIO_RAIZ);
                } else if (comando.startsWith("RENAME_FILE")) {
                    renombrarArchivo(comando, paqueteRecibido, socketServidor, DIRECTORIO_RAIZ);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void listarContenido(String comando, DatagramPacket paqueteRecibido, DatagramSocket socketServidor, String directorioRaiz) {
        try {
            String[] partes = comando.split(":");
            String rutaSolicitada = partes.length > 1 ? partes[1] : "/";
            String rutaAbsoluta = directorioRaiz + rutaSolicitada.replace("/", "\\");

            File carpeta = new File(rutaAbsoluta);
            StringBuilder contenido = new StringBuilder();

            if (carpeta.exists() && carpeta.isDirectory()) {
                File[] archivos = carpeta.listFiles();
                for (File archivo : archivos) {
                    if (archivo.isDirectory()) {
                        contenido.append("[C] ").append(archivo.getName()).append("\n");
                    } else if (archivo.isFile()) {
                        contenido.append("[A] ").append(archivo.getName()).append("\n");
                    }
                }
                contenido.append("[..] Regresar al nivel anterior\n"); // Agregar opción para regresar
            } else {
                contenido.append("ERROR: Ruta no válida o no encontrada.\n");
            }

            byte[] bufferRespuesta = contenido.toString().getBytes();
            InetAddress direccionCliente = paqueteRecibido.getAddress();
            int puertoCliente = paqueteRecibido.getPort();

            DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length, direccionCliente, puertoCliente);
            socketServidor.send(paqueteRespuesta);
            System.out.println("Respuesta enviada al cliente...");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void crearCarpeta(String comando, DatagramPacket paqueteRecibido, DatagramSocket socketServidor, String directorioRaiz) {
        try {
            String[] partes = comando.split(":");
            String rutaNuevaCarpeta = partes.length > 1 ? partes[1] : "";
            String rutaAbsoluta = directorioRaiz + rutaNuevaCarpeta.replace("/", "\\");

            File nuevaCarpeta = new File(rutaAbsoluta);
            String respuesta;

            if (nuevaCarpeta.mkdirs()) {
                respuesta = "Carpeta creada con éxito: " + rutaNuevaCarpeta;
            } else {
                respuesta = "ERROR: No se pudo crear la carpeta en la ruta especificada.";
            }

            byte[] bufferRespuesta = respuesta.getBytes();
            InetAddress direccionCliente = paqueteRecibido.getAddress();
            int puertoCliente = paqueteRecibido.getPort();

            DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length, direccionCliente, puertoCliente);
            socketServidor.send(paqueteRespuesta);
            System.out.println("Respuesta enviada al cliente: " + respuesta);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void manejarSubidaArchivo(DatagramSocket socketServidor, DatagramPacket paqueteRecibido, String comando) throws IOException {
        String[] partes = comando.split(":");
        String rutaArchivo = partes[1];
        long tamanioArchivo = Long.parseLong(partes[2]);

        System.out.println("Preparado para recibir archivo: " + rutaArchivo + " con tamaño: " + tamanioArchivo);
        File archivoDestino = new File(DIRECTORIO_RAIZ + rutaArchivo);
        FileOutputStream fos = new FileOutputStream(archivoDestino);

        int numeroPaqueteEsperado = 0;
        int totalPaquetes = (int) Math.ceil((double) tamanioArchivo / 1024);
        boolean archivoCompleto = false;

        while (!archivoCompleto) {
            try {
                byte[] bufferPaquete = new byte[1028]; // 1024 bytes de datos + 4 bytes para el número de secuencia
                DatagramPacket paqueteDatos = new DatagramPacket(bufferPaquete, bufferPaquete.length);
                socketServidor.receive(paqueteDatos);

                ByteArrayInputStream bais = new ByteArrayInputStream(paqueteDatos.getData());
                DataInputStream dis = new DataInputStream(bais);
                int numeroPaquete = dis.readInt();
                byte[] datos = new byte[paqueteDatos.getLength() - 4];
                dis.read(datos);

                if (numeroPaquete == numeroPaqueteEsperado) {
                    fos.write(datos);
                    fos.flush();
                    bufferRecepcion.put(numeroPaquete, datos);
                    numeroPaqueteEsperado++;
                } else {
                    System.out.println("Paquete fuera de secuencia. Esperado: " + numeroPaqueteEsperado + ", recibido: " + numeroPaquete);
                }

                // Enviar ACK para el paquete recibido
                String respuestaACK = "ACK:" + numeroPaquete;
                byte[] bufferACK = respuestaACK.getBytes();
                InetAddress direccionCliente = paqueteDatos.getAddress();
                int puertoCliente = paqueteDatos.getPort();

                DatagramPacket paqueteACK = new DatagramPacket(bufferACK, bufferACK.length, direccionCliente, puertoCliente);
                socketServidor.send(paqueteACK);
                System.out.println("ACK enviado para el paquete: " + numeroPaquete);

                // Verificar si se han recibido todos los paquetes
                if (numeroPaqueteEsperado >= totalPaquetes) {
                    archivoCompleto = true;
                    System.out.println("Archivo recibido completamente: " + rutaArchivo);
                }

            } catch (SocketTimeoutException e) {
                System.out.println("Tiempo de espera agotado, esperando retransmisión del paquete: " + numeroPaqueteEsperado);
            }
        }
        fos.close();
    }


    private static void navegarCarpeta(String comando, DatagramPacket paqueteRecibido, DatagramSocket socketServidor, String directorioRaiz) {
        try {
            String[] partes = comando.split(":");
            String rutaNavegar = partes.length > 1 ? partes[1] : "";
            String rutaAbsoluta;

            // Verificar si es para regresar al nivel anterior
            if (rutaNavegar.equals("..")) {
                int ultimaBarra = directorioRaiz.lastIndexOf("\\");
                rutaAbsoluta = (ultimaBarra > 0) ? directorioRaiz.substring(0, ultimaBarra) : directorioRaiz;
            } else {
                rutaAbsoluta = directorioRaiz + "\\" + rutaNavegar;
            }

            File carpeta = new File(rutaAbsoluta);
            StringBuilder contenido = new StringBuilder();

            if (carpeta.exists() && carpeta.isDirectory()) {
                File[] archivos = carpeta.listFiles();
                for (File archivo : archivos) {
                    if (archivo.isDirectory()) {
                        contenido.append("[C] ").append(archivo.getName()).append("\n");
                    } else if (archivo.isFile()) {
                        contenido.append("[A] ").append(archivo.getName()).append("\n");
                    }
                }
                contenido.append("[..] Regresar al nivel anterior\n"); // Agregar opción para regresar
            } else {
                contenido.append("ERROR: Ruta no válida o no encontrada.\n");
            }

            byte[] bufferRespuesta = contenido.toString().getBytes();
            InetAddress direccionCliente = paqueteRecibido.getAddress();
            int puertoCliente = paqueteRecibido.getPort();

            DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length, direccionCliente, puertoCliente);
            socketServidor.send(paqueteRespuesta);
            System.out.println("Respuesta enviada al cliente...");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void eliminarArchivo(String comando, DatagramPacket paqueteRecibido, DatagramSocket socketServidor, String directorioRaiz) {
        try {
            String[] partes = comando.split(":");
            String rutaArchivo = partes.length > 1 ? partes[1] : "";
            String rutaAbsoluta = directorioRaiz + rutaArchivo.replace("/", "\\");

            File archivo = new File(rutaAbsoluta);
            String respuesta;

            if (archivo.exists() && archivo.isFile() && archivo.delete()) {
                respuesta = "Archivo eliminado con éxito: " + rutaArchivo;
            } else {
                respuesta = "ERROR: No se pudo eliminar el archivo especificado.";
            }

            byte[] bufferRespuesta = respuesta.getBytes();
            InetAddress direccionCliente = paqueteRecibido.getAddress();
            int puertoCliente = paqueteRecibido.getPort();

            DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length, direccionCliente, puertoCliente);
            socketServidor.send(paqueteRespuesta);
            System.out.println("Respuesta enviada al cliente: " + respuesta);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void renombrarArchivo(String comando, DatagramPacket paqueteRecibido, DatagramSocket socketServidor, String directorioRaiz) {
        try {
            String[] partes = comando.split(":");
            if (partes.length < 3) {
                String errorRespuesta = "ERROR: Comando incorrecto para renombrar archivo.";
                byte[] bufferError = errorRespuesta.getBytes();
                InetAddress direccionCliente = paqueteRecibido.getAddress();
                int puertoCliente = paqueteRecibido.getPort();
                DatagramPacket paqueteError = new DatagramPacket(bufferError, bufferError.length, direccionCliente, puertoCliente);
                socketServidor.send(paqueteError);
                return;
            }

            String rutaArchivo = partes[1];
            String nuevoNombre = partes[2];
            String rutaAbsoluta = directorioRaiz + rutaArchivo.replace("/", "\\");
            File archivo = new File(rutaAbsoluta);
            String respuesta;

            if (archivo.exists() && archivo.isFile()) {
                String rutaPadre = archivo.getParent();
                File archivoRenombrado = new File(rutaPadre, nuevoNombre);
                if (archivo.renameTo(archivoRenombrado)) {
                    respuesta = "Archivo renombrado con éxito a: " + nuevoNombre;
                } else {
                    respuesta = "ERROR: No se pudo renombrar el archivo.";
                }
            } else {
                respuesta = "ERROR: Archivo no encontrado o ruta inválida.";
            }

            byte[] bufferRespuesta = respuesta.getBytes();
            InetAddress direccionCliente = paqueteRecibido.getAddress();
            int puertoCliente = paqueteRecibido.getPort();

            DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length, direccionCliente, puertoCliente);
            socketServidor.send(paqueteRespuesta);
            System.out.println("Respuesta enviada al cliente: " + respuesta);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
