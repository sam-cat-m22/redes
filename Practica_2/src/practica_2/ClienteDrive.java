
package practica_2;
import java.net.*;
import java.io.*;

/**
 *
 * @author cesar
 */

public class ClienteDrive {
    public static void main(String[] args) {
        try {
            // Datos del servidor
            String direccionServidor = "127.0.0.1";  // Localhost
            int puertoServidor = 1234;

            // Mensaje a enviar
            String mensaje = "Hola, servidor. Este es un mensaje de prueba.";
            byte[] datosMensaje = mensaje.getBytes();

            // Creación del socket UDP del cliente
            DatagramSocket socketCliente = new DatagramSocket();

            // Dirección del servidor
            InetAddress direccion = InetAddress.getByName(direccionServidor);

            // Creación del paquete a enviar al servidor
            DatagramPacket paquete = new DatagramPacket(datosMensaje, datosMensaje.length, direccion, puertoServidor);

            // Enviar el paquete
            socketCliente.send(paquete);
            System.out.println("Mensaje enviado al servidor: " + mensaje);

            // Recibir respuesta del servidor (ACK)
            byte[] bufferRespuesta = new byte[65535];
            DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length);
            socketCliente.receive(paqueteRespuesta);

            // Mostrar la respuesta
            String respuesta = new String(paqueteRespuesta.getData(), 0, paqueteRespuesta.getLength());
            System.out.println("Respuesta del servidor: " + respuesta);

            // Cerrar el socket
            socketCliente.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
