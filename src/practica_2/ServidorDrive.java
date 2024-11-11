package practica_2;
import java.net.*;
import java.io.*;

/**
 *
 * @author cesar
 * @author Sam
 */
public class ServidorDrive {
    public static void main(String[] args) {
        try {
            int puerto = 1234;
            DatagramSocket socketServidor = new DatagramSocket(puerto);
            System.out.println("Servidor iniciado en el puerto " + puerto);

            String directorioRaiz = "C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Practica_2\\DRIVE";

            while (true) {
                byte[] bufferRecibir = new byte[1024];
                DatagramPacket paqueteRecibido = new DatagramPacket(bufferRecibir, bufferRecibir.length);
                socketServidor.receive(paqueteRecibido);

                String comando = new String(paqueteRecibido.getData(), 0, paqueteRecibido.getLength());
                System.out.println("Comando recibido: " + comando);

                if (comando.startsWith("LIST_DIRECTORY")) {
                    listarContenido(comando, paqueteRecibido, socketServidor, directorioRaiz);
                } else if (comando.startsWith("CREATE_FOLDER")) {
                    crearCarpeta(comando, paqueteRecibido, socketServidor, directorioRaiz);
                } else if (comando.startsWith("UPLOAD_FILE")) {
                    subirArchivo(comando, paqueteRecibido, socketServidor);
                } else if (comando.startsWith("NAVIGATE")) {
                    navegarCarpeta(comando, paqueteRecibido, socketServidor, directorioRaiz);
                } else if (comando.startsWith("DELETE_FILE")) {
                    eliminarArchivo(comando, paqueteRecibido, socketServidor, directorioRaiz);
                } else if (comando.startsWith("RENAME_FILE")) {
                    renombrarArchivo(comando, paqueteRecibido, socketServidor, directorioRaiz);
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

    private static void subirArchivo(String comando, DatagramPacket paqueteRecibido, DatagramSocket socketServidor) {
        try {
            System.out.println("Preparado para recibir archivo...");

            String respuesta = "READY_FOR_UPLOAD";
            byte[] bufferRespuesta = respuesta.getBytes();
            InetAddress direccionCliente = paqueteRecibido.getAddress();
            int puertoCliente = paqueteRecibido.getPort();

            DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length, direccionCliente, puertoCliente);
            socketServidor.send(paqueteRespuesta);
            System.out.println("Respuesta enviada al cliente para proceder con el archivo...");

        } catch (Exception e) {
            e.printStackTrace();
        }
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
